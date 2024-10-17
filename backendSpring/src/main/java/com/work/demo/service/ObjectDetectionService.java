package com.work.demo.service;

import ai.onnxruntime.*;
import com.work.demo.repository.Proyecto;
import com.work.demo.repository.ProyectoRepository;
import com.work.demo.service.dto.AnalisisReturnDto;
import com.work.demo.service.dto.DeteccionServiceDto;
import com.work.demo.service.dto.ObjetoImagen;
import com.work.demo.service.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.work.demo.rest.dto.ObjectDetectionResult;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

@Service
public class ObjectDetectionService {

    private BufferedImage imagen;
    private double minConfig;
    @Autowired
    private DeteccionService deteccionService;
    @Autowired
    private ProyectoRepository proyectoRepository;

    public AnalisisReturnDto performAllDetectionsAndReturnImage(MultipartFile imageFile,Long proyectoId) {
        AnalisisReturnDto r=new AnalisisReturnDto();
        BufferedImage imageWithDetections = null;
        List<ObjectDetectionResult> combinedResults = new ArrayList<>();

        // Realizar la detección de conos
        List<ObjectDetectionResult> coneDetections = performConeDetection(imageFile,proyectoId);
        combinedResults.addAll(coneDetections);

        // Realizar la detección de vehículos
        List<ObjectDetectionResult> vehicleDetections = performVehicleDetection(imageFile,proyectoId);
        combinedResults.addAll(vehicleDetections);

        // Realizar la detección de grúas
        List<ObjectDetectionResult> gruasDetections = performPalaDetection(imageFile,proyectoId);
        combinedResults.addAll(gruasDetections);

        // Realizar la detección de palets
        List<ObjectDetectionResult> palletDetections = performPalletDetection(imageFile,proyectoId);
        combinedResults.addAll(palletDetections);
        r.setDetecciones(combinedResults);
        try {
            // Convertir la imagen con detecciones a un byte array para enviar al frontend
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(imageWithDetections, "jpg", baos);
            r.setImage(baos.toByteArray());
            return r;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    // Método para calcular la Intersección sobre la Unión (IoU)
    private float calcularIoU(float[] boxA, float[] boxB) {
        float xA = Math.max(boxA[0], boxB[0]);
        float yA = Math.max(boxA[1], boxB[1]);
        float xB = Math.min(boxA[2], boxB[2]);
        float yB = Math.min(boxA[3], boxB[3]);

        // Calcular el área de la intersección
        float interArea = Math.max(0, xB - xA) * Math.max(0, yB - yA);

        // Calcular el área de ambas cajas
        float boxAArea = (boxA[2] - boxA[0]) * (boxA[3] - boxA[1]);
        float boxBArea = (boxB[2] - boxB[0]) * (boxB[3] - boxB[1]);

        // Calcular la IoU (Intersección sobre Unión)
        return interArea / (boxAArea + boxBArea - interArea);
    }


/** Funciona */
    public List<ObjectDetectionResult> performConeDetection (MultipartFile imageFile,Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/cono/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());
            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                 image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);
            }else{
                image=this.imagen;
            }
            // Convertir la imagen a un tensor de entrada
            float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

            // Realizar la inferencia
            OrtSession.Result result = session.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
            float[][][] outputData = (float[][][]) outputTensor.getValue();
            long[] outputShape = outputTensor.getInfo().getShape();
            System.out.println("Forma del tensor de salida: " + Arrays.toString(outputShape));

            // Procesar la salida del tensor
            float bestConfidence = 0.0f;


            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza y buscar la detección con mayor confianza
                    if (confidence >= this.minConfig) {
                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Actualizar la mejor caja delimitadora
                        float[]  newBox = new float[]{x1, y1, x2, y2};
                        boolean esCajaDuplicada = false;
                        for (ObjectDetectionResult resultDet : results) {
                            float existingX1 = (float) (resultDet.getX() - resultDet.getWeight() / 2);
                            float existingY1 = (float) (resultDet.getY() - resultDet.getHeight() / 2);
                            float existingX2 = (float) (resultDet.getX() + resultDet.getWeight() / 2);
                            float existingY2 = (float) (resultDet.getY() + resultDet.getHeight() / 2);
                            float[] existingBox = new float[]{existingX1, existingY1, existingX2, existingY2};

                            float iou = calcularIoU(newBox, existingBox);
                            if (iou > iouThreshold) {
                                esCajaDuplicada = true;
                                break;
                            }
                        }
                        if(!esCajaDuplicada){
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Cono");
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Cono", x1, y1, x2, y2, confidence));

                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.RED);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            graphics.dispose();
                        }
                    }
                }
            }

            // Almacenar la mejor detección si tiene suficiente confianza


                // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_cone_image.jpg";
            this.imagen=image;
            File outputfile = new File(outputImagePath);
            ImageIO.write(image, "jpg", outputfile);
            System.out.println("Imagen guardada en: " + outputImagePath);
            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Envolver los resultados en un contenedor

        return results;
    }


    public List<ObjectDetectionResult> performVehicleDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/vehiculo/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);
            }else{
                image=this.imagen;
            }
            // Convertir la imagen a un tensor de entrada
            float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

            // Realizar la inferencia
            OrtSession.Result result = session.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
            float[][][] outputData = (float[][][]) outputTensor.getValue();
            long[] outputShape = outputTensor.getInfo().getShape();
            System.out.println("Forma del tensor de salida: " + Arrays.toString(outputShape));

            // Procesar la salida del tensor
            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza
                    if (confidence >= this.minConfig) {
                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Control de duplicados con IoU
                        float[] newBox = new float[]{x1, y1, x2, y2};
                        boolean esCajaDuplicada = false;
                        for (ObjectDetectionResult resultDet : results) {
                            float existingX1 = (float) (resultDet.getX() - resultDet.getWeight() / 2);
                            float existingY1 = (float) (resultDet.getY() - resultDet.getHeight() / 2);
                            float existingX2 = (float) (resultDet.getX() + resultDet.getWeight() / 2);
                            float existingY2 = (float) (resultDet.getY() + resultDet.getHeight() / 2);
                            float[] existingBox = new float[]{existingX1, existingY1, existingX2, existingY2};

                            float iou = calcularIoU(newBox, existingBox);
                            if (iou > iouThreshold) {
                                esCajaDuplicada = true;
                                break;
                            }
                        }

                        if (!esCajaDuplicada) {
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Vehicle");
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Vehicle", x1, y1, x2, y2, confidence));

                            // Dibujar el recuadro en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.BLUE);  // Cambia a color azul para vehículos
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con las detecciones en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_vehicle_image.jpg";
            this.imagen=image;
            File outputfile = new File(outputImagePath);
            ImageIO.write(image, "jpg", outputfile);
            System.out.println("Imagen guardada en: " + outputImagePath);

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Envolver los resultados en un contenedor
        return results;
    }


    public List<ObjectDetectionResult> performPalaDetection (MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/pala/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);
            }else{
                image=this.imagen;
            }
            // Convertir la imagen a un tensor de entrada
            float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

            // Realizar la inferencia
            OrtSession.Result result = session.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
            float[][][] outputData = (float[][][]) outputTensor.getValue();
            long[] outputShape = outputTensor.getInfo().getShape();
            System.out.println("Forma del tensor de salida: " + Arrays.toString(outputShape));

            // Procesar la salida del tensor
            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza
                    if (confidence >= this.minConfig) {
                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Actualizar la mejor caja delimitadora
                        float[] newBox = new float[]{x1, y1, x2, y2};
                        boolean esCajaDuplicada = false;
                        for (ObjectDetectionResult resultDet : results) {
                            float existingX1 = (float) (resultDet.getX() - resultDet.getWeight() / 2);
                            float existingY1 = (float) (resultDet.getY() - resultDet.getHeight() / 2);
                            float existingX2 = (float) (resultDet.getX() + resultDet.getWeight() / 2);
                            float existingY2 = (float) (resultDet.getY() + resultDet.getHeight() / 2);
                            float[] existingBox = new float[]{existingX1, existingY1, existingX2, existingY2};

                            float iou = calcularIoU(newBox, existingBox);
                            if (iou > iouThreshold) {
                                esCajaDuplicada = true;
                                break;
                            }
                        }
                        if (!esCajaDuplicada) {
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Pala");
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Pala", x1, y1, x2, y2, confidence));

                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.CYAN);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_grua_image.jpg";
            this.imagen=image;
            File outputfile = new File(outputImagePath);
            ImageIO.write(image, "jpg", outputfile);
            System.out.println("Imagen guardada en: " + outputImagePath);

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Envolver los resultados en un contenedor
        return results;
    }


    public List<ObjectDetectionResult> performPalletDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/pallet/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);
            }else{
                image=this.imagen;
            }
            // Convertir la imagen a un tensor de entrada
            float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

            // Realizar la inferencia
            OrtSession.Result result = session.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
            float[][][] outputData = (float[][][]) outputTensor.getValue();
            long[] outputShape = outputTensor.getInfo().getShape();
            System.out.println("Forma del tensor de salida: " + Arrays.toString(outputShape));

            // Procesar la salida del tensor
            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza
                    if (confidence >= this.minConfig) {
                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Crear la nueva caja delimitadora
                        float[] newBox = new float[]{x1, y1, x2, y2};
                        boolean esCajaDuplicada = false;
                        for (ObjectDetectionResult resultDet : results) {
                            float existingX1 = (float) (resultDet.getX() - resultDet.getWeight() / 2);
                            float existingY1 = (float) (resultDet.getY() - resultDet.getHeight() / 2);
                            float existingX2 = (float) (resultDet.getX() + resultDet.getWeight() / 2);
                            float existingY2 = (float) (resultDet.getY() + resultDet.getHeight() / 2);
                            float[] existingBox = new float[]{existingX1, existingY1, existingX2, existingY2};

                            // Calcular IoU y verificar duplicados
                            float iou = calcularIoU(newBox, existingBox);
                            if (iou > iouThreshold) {
                                esCajaDuplicada = true;
                                break;
                            }
                        }

                        // Si no es duplicada, añadir la detección
                        if (!esCajaDuplicada) {
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Pallet");
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Pallet", x1, y1, x2, y2, confidence));

                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.BLUE);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_pallet_image.jpg";
            this.imagen=image;
            File outputfile = new File(outputImagePath);
            ImageIO.write(image, "jpg", outputfile);
            System.out.println("Imagen guardada en: " + outputImagePath);

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Retornar los resultados
        return results;
    }
    public List<ObjectDetectionResult> performCamionDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/camiones/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);
            }else{
                image=this.imagen;
            }
            // Convertir la imagen a un tensor de entrada
            float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

            // Realizar la inferencia
            OrtSession.Result result = session.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
            float[][][] outputData = (float[][][]) outputTensor.getValue();
            long[] outputShape = outputTensor.getInfo().getShape();
            System.out.println("Forma del tensor de salida: " + Arrays.toString(outputShape));

            // Procesar la salida del tensor
            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza
                    if (confidence >= this.minConfig) {
                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Crear la nueva caja delimitadora
                        float[] newBox = new float[]{x1, y1, x2, y2};
                        boolean esCajaDuplicada = false;
                        for (ObjectDetectionResult resultDet : results) {
                            float existingX1 = (float) (resultDet.getX() - resultDet.getWeight() / 2);
                            float existingY1 = (float) (resultDet.getY() - resultDet.getHeight() / 2);
                            float existingX2 = (float) (resultDet.getX() + resultDet.getWeight() / 2);
                            float existingY2 = (float) (resultDet.getY() + resultDet.getHeight() / 2);
                            float[] existingBox = new float[]{existingX1, existingY1, existingX2, existingY2};

                            // Calcular IoU y verificar duplicados
                            float iou = calcularIoU(newBox, existingBox);
                            if (iou > iouThreshold) {
                                esCajaDuplicada = true;
                                break;
                            }
                        }

                        // Si no es duplicada, añadir la detección
                        if (!esCajaDuplicada) {
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Camion");
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Camion", x1, y1, x2, y2, confidence));

                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.yellow);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_camion_image.jpg";
            this.imagen=image;
            File outputfile = new File(outputImagePath);
            ImageIO.write(image, "jpg", outputfile);
            System.out.println("Imagen guardada en: " + outputImagePath);

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Retornar los resultados
        return results;
    }

    public List<ObjectDetectionResult> performTuboDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/tubos/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);
            }else{
                image=this.imagen;
            }
            // Convertir la imagen a un tensor de entrada
            float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

            // Realizar la inferencia
            OrtSession.Result result = session.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
            float[][][] outputData = (float[][][]) outputTensor.getValue();
            long[] outputShape = outputTensor.getInfo().getShape();
            System.out.println("Forma del tensor de salida: " + Arrays.toString(outputShape));

            // Procesar la salida del tensor
            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza
                    if (confidence >= this.minConfig) {
                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Crear la nueva caja delimitadora
                        float[] newBox = new float[]{x1, y1, x2, y2};
                        boolean esCajaDuplicada = false;
                        for (ObjectDetectionResult resultDet : results) {
                            float existingX1 = (float) (resultDet.getX() - resultDet.getWeight() / 2);
                            float existingY1 = (float) (resultDet.getY() - resultDet.getHeight() / 2);
                            float existingX2 = (float) (resultDet.getX() + resultDet.getWeight() / 2);
                            float existingY2 = (float) (resultDet.getY() + resultDet.getHeight() / 2);
                            float[] existingBox = new float[]{existingX1, existingY1, existingX2, existingY2};

                            // Calcular IoU y verificar duplicados
                            float iou = calcularIoU(newBox, existingBox);
                            if (iou > iouThreshold) {
                                esCajaDuplicada = true;
                                break;
                            }
                        }

                        // Si no es duplicada, añadir la detección
                        if (!esCajaDuplicada) {
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Tubo");
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Tubo", x1, y1, x2, y2, confidence));

                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.yellow);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_tubo_image.jpg";
            this.imagen=image;
            File outputfile = new File(outputImagePath);
            ImageIO.write(image, "jpg", outputfile);
            System.out.println("Imagen guardada en: " + outputImagePath);

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Retornar los resultados
        return results;
    }
    public List<ObjectDetectionResult> performPersonaDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        // Lista de clases según el índice (ejemplo)
        String[] classLabels = {"Estacion", "Conexion", "Tanque", "Radiador"};

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/estaciones/best.onnx").toURI());
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image = this.imagen == null
                    ? resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640)
                    : this.imagen;

            // Convertir la imagen a un tensor de entrada
            float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

            // Realizar la inferencia
            OrtSession.Result result = session.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
            float[][][] outputData = (float[][][]) outputTensor.getValue();
            long[] outputShape = outputTensor.getInfo().getShape();
            System.out.println("Forma del tensor de salida: " + Arrays.toString(outputShape));  // 1,20,8400

            // Procesar la salida del tensor
            for (int classIndex = 0; classIndex < outputData[0].length; classIndex++) {  // Recorrer las 20 clases
                String classLabel = classLabels[classIndex];  // Asignar la etiqueta de la clase actual
                for (int j = 0; j < outputData[0][classIndex].length; j++) {  // 8400 posibles detecciones

                    // Descomponer coordenadas y confianza de cada detección
                    float cx = outputData[classIndex][0][j];  // x central
                    float cy = outputData[classIndex][1][j];  // y central
                    float width = outputData[classIndex][2][j];  // Ancho
                    float height = outputData[classIndex][3][j];  // Alto
                    float confidence = outputData[classIndex][4][j];  // Confianza

                    // Procesar si la confianza es suficiente
                    if (confidence >= this.minConfig) {
                        // Convertir coordenadas de centro a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Comprobar si la caja ya existe usando IoU
                        float[] newBox = new float[]{x1, y1, x2, y2};
                        boolean esCajaDuplicada = false;
                        for (ObjectDetectionResult resultDet : results) {
                            float[] existingBox = new float[]{
                                    (float) (resultDet.getX() - resultDet.getWeight() / 2),
                                    (float) (resultDet.getY() - resultDet.getHeight() / 2),
                                    (float) (resultDet.getX() + resultDet.getWeight() / 2),
                                    (float) (resultDet.getY() + resultDet.getHeight() / 2)
                            };

                            // Calcular IoU y evitar duplicados
                            float iou = calcularIoU(newBox, existingBox);
                            if (iou > iouThreshold) {
                                esCajaDuplicada = true;
                                break;
                            }
                        }

                        // Si no es duplicada, crear la detección en BBDD
                        if (!esCajaDuplicada) {
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, classLabel);
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(
                                    null, proyectoId, null, classLabel, x1, y1, x2, y2, confidence));

                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.yellow);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con los recuadros en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_tubo_image.jpg";
            this.imagen = image;
            File outputfile = new File(outputImagePath);
            ImageIO.write(image, "jpg", outputfile);
            System.out.println("Imagen guardada en: " + outputImagePath);

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Retornar los resultados
        return results;
    }

    // Método para encontrar el índice del valor máximo en un array
    public static int argMax(float[] scores) {
        int maxIndex = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    public ObjetoImagen performAllDetections (MultipartFile image, Long proyectId) {
        if(image==null || proyectId==null){
            throw new RuntimeException("Error al detectar imagenes");
        }
        List<ObjectDetectionResult> combinedResults = new ArrayList<>();
        Optional<Proyecto> p=proyectoRepository.findById(proyectId);
        this.minConfig=p.get().getMinConf();

        System.out.println("Comienzo analisis Vehiculos");
        // Realizar la detección de vehículos
        List<ObjectDetectionResult> vehicleDetections = performVehicleDetection(image,proyectId);
        combinedResults.addAll(vehicleDetections);
        System.out.println("Comienzo analisis gruas");
        // Realizar la detección de grúas
        List<ObjectDetectionResult> gruasDetections = performPalaDetection(image,proyectId);
        combinedResults.addAll(gruasDetections);
        System.out.println("Comienzo analisis pallets");
        // Realizar la detección de palets
        List<ObjectDetectionResult> palletDetections = performPalletDetection(image,proyectId);
        combinedResults.addAll(palletDetections);
        System.out.println("Comienzo analisis conos");
        List<ObjectDetectionResult> coneDetections = performConeDetection(image,proyectId);
        combinedResults.addAll(coneDetections);
        System.out.println("Comienzo analisis camiones");
        List<ObjectDetectionResult> camionDetections = performCamionDetection(image,proyectId);
        combinedResults.addAll(camionDetections);
        System.out.println("Comienzo analisis tubos");
        List<ObjectDetectionResult> tubosDetections = performTuboDetection(image,proyectId);
        combinedResults.addAll(tubosDetections);
        System.out.println("Comienzo analisis personas");
        List<ObjectDetectionResult> pesonasDetect = performPersonaDetection(image,proyectId);
        combinedResults.addAll(pesonasDetect);
        ObjetoImagen obj=new ObjetoImagen();
        obj.setObjetos(combinedResults);
        obj.setImage(encodeImageToBase64(this.imagen));
        this.imagen=null;
        // Devolver la lista combinada de todas las detecciones
        return obj;
    }
    public static String encodeImageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);  // Cambia el formato según corresponda
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

