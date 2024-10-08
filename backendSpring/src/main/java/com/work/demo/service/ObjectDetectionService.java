package com.work.demo.service;

import ai.onnxruntime.*;
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
import java.util.*;
import java.util.List;

@Service
public class ObjectDetectionService {

    private BufferedImage imagen;
    @Autowired
    private DeteccionService deteccionService;
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
        List<ObjectDetectionResult> gruasDetections = performGruasDetection(imageFile,proyectoId);
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
/**
 public List<ObjectDetectionResult> performConeDetection (MultipartFile imageFile,Long proyectoId) {

 List<ObjectDetectionResult> results = new ArrayList<>();
 float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

 try {
 // Cargar el modelo ONNX
 OrtEnvironment env = OrtEnvironment.getEnvironment();
 OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\conesTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

 // Redimensionar la imagen
 BufferedImage image = resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);

 // Convertir la imagen a tensor
 float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
 OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
 Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);

 // Realizar la inferencia
 OrtSession.Result result = session.run(inputs);
 OnnxTensor outputTensor = (OnnxTensor) result.get("output0").get();
 float[][][] outputData = (float[][][]) outputTensor.getValue();

 // Procesar todas las detecciones
 for (int i = 0; i < outputData.length; i++) {
 for (int j=0; j < outputData[i][0].length; j++) {
 float confidence=outputData[i][4][0]; // Usualmente la confianza está en la 5ta posición
 // Solo considerar detecciones con suficiente confianza
 if (confidence > 0.5) {
 float cx = outputData[i][0][j];
 float cy = outputData[i][1][j];
 float width = outputData[i][2][j];
 float height = outputData[i][3][j];


 // Convertir coordenadas centrales a esquinas
 float x1=cx - width / 2;
 float y1=cy - height / 2;
 float x2=cx + width / 2;
 float y2=cy + height / 2;

 float[] newBox=new float[]{x1, y1, x2, y2};

 // Verificar si la caja es similar a una detección existente
 boolean esCajaDuplicada=false;
 for (ObjectDetectionResult resultDet : results) {
 // Convertir x, y, width, height de resultDet a esquinas
 float existingX1=(float) (resultDet.getX() - resultDet.getWeight() / 2);
 float existingY1=(float) (resultDet.getY() - resultDet.getHeight() / 2);
 float existingX2=(float) (resultDet.getX() + resultDet.getWeight() / 2);
 float existingY2=(float) (resultDet.getY() + resultDet.getHeight() / 2);
 float[] existingBox=new float[]{existingX1, existingY1, existingX2, existingY2};

 float iou=calcularIoU(newBox, existingBox);

 if (iou > iouThreshold) {
 esCajaDuplicada=true;
 break;
 }
 }
 System.out.println("IOU calculado para los conos");
 // Si no es una caja duplicada, agregar la nueva detección
 if (!esCajaDuplicada) {
 System.out.println("No es caja duplicada");
 ObjectDetectionResult detectionResult=new ObjectDetectionResult(cx, cy, width, height, confidence, "Cono");
 results.add(detectionResult);

 // Guardar la detección en la base de datos
 deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Cono", x1, y1, x2, y2, confidence));

 // Dibujar la detección en la imagen
 Graphics2D graphics=image.createGraphics();
 graphics.setColor(Color.RED);
 graphics.setStroke(new java.awt.BasicStroke(3));
 graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
 graphics.dispose();
 }
 }
 }
 }
 // Guardar la imagen modificada
 String outputImagePath = "C:\\Users\\user\\Desktop\\detected_image.jpg";
 File outputfile = new File(outputImagePath);
 ImageIO.write(image, "jpg", outputfile);

 // Liberar los recursos
 inputTensor.close();
 outputTensor.close();
 session.close();
 env.close();

 } catch (OrtException | IOException e) {
 e.printStackTrace();
 }

 return results;

 }
*/
/** Funciona */
    public List<ObjectDetectionResult> performConeDetection (MultipartFile imageFile,Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\conesTrain\\weights\\best.onnx", new OrtSession.SessionOptions());
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
                    if (confidence > 0.4) {
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
        }

        // Envolver los resultados en un contenedor

        return results;
    }


    public List<ObjectDetectionResult> performVehicleDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\VehiculosTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

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
                    if (confidence > 0.4) {
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
        }

        // Envolver los resultados en un contenedor
        return results;
    }


    public List<ObjectDetectionResult> performGruasDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\gruasTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

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
                    if (confidence > 0.4) {
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
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Grua");
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Grua", x1, y1, x2, y2, confidence));

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
        }

        // Envolver los resultados en un contenedor
        return results;
    }


    public List<ObjectDetectionResult> performPalletDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\palletTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

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
                    if (confidence > 0.4) {
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
        List<ObjectDetectionResult> combinedResults = new ArrayList<>();

        System.out.println("Comienzo analisis Vehiculos");
        // Realizar la detección de vehículos
        List<ObjectDetectionResult> vehicleDetections = performVehicleDetection(image,proyectId);
        combinedResults.addAll(vehicleDetections);
        System.out.println("Comienzo analisis gruas");
        // Realizar la detección de grúas
        List<ObjectDetectionResult> gruasDetections = performGruasDetection(image,proyectId);
        combinedResults.addAll(gruasDetections);
        System.out.println("Comienzo analisis pallets");
        // Realizar la detección de palets
        List<ObjectDetectionResult> palletDetections = performPalletDetection(image,proyectId);
        combinedResults.addAll(palletDetections);
        System.out.println("Comienzo analisis conos");
        List<ObjectDetectionResult> coneDetections = performConeDetection(image,proyectId);
        combinedResults.addAll(coneDetections);
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

