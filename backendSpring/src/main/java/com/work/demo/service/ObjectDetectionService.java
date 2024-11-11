package com.work.demo.service;

import ai.onnxruntime.*;
import com.work.demo.repository.*;
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
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ObjectDetectionService {

    private BufferedImage imagen;
    private double minConfig;
    private int idDeteccion;
    @Autowired
    private DeteccionService deteccionService;
    @Autowired
    private FallosRepository fallosRepository;
    @Autowired
    private RestriccionRepository restriccionRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;
    /*
    @Autowired
    private EmailService emailService;
    */
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



    /**
     *  Analisis de conos
     * @param imageFile
     * @param proyectoId
     * @return
     */
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Cono", x1, y1, x2, y2, confidence));
                            this.idDeteccion=deteccionService.findLastId();
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence,"Cono",this.idDeteccion );
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.RED);
                            graphics.setStroke(new java.awt.BasicStroke(3));

                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));

                            String label = String.format("%d.-%s: %.2f", this.idDeteccion, "Cono", confidence);
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);
                            FontMetrics metrics = graphics.getFontMetrics(font);

                            int textX = (int) x1 + 5;  // Margen izquierdo dentro de la caja
                            int textY = (int) (y2 - y1) + (int) y1 - 5;  // Margen inferior dentro de la caja

                            int backgroundWidth = metrics.stringWidth(label) + 10; // Ancho del fondo, un poco más grande que el texto
                            int backgroundHeight = metrics.getHeight(); // Altura del fondo del texto

                            graphics.setColor(new Color(255, 0, 0, 180)); // Rojo semi-transparente
                            graphics.fillRect(textX - 5, textY - backgroundHeight + 5, backgroundWidth, backgroundHeight);

                            graphics.setColor(Color.WHITE);
                            graphics.drawString(label, textX, textY);

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


    /**
     *  Analisis de vehiculos en general, detecta palas y todo tipo de vehiculos, lo normal cuando detecte
     *  una pala es que salte como pala y como vehicle
     * @param imageFile
     * @param proyectoId
     * @return
     */
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Vehiculo", x1, y1, x2, y2, confidence));
                            this.idDeteccion=deteccionService.findLastId();
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Vehiculo",this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar el recuadro en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.BLUE);  // Cambia a color azul para vehículos
                            graphics.setStroke(new java.awt.BasicStroke(3));

                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));

                            String label = String.format("%d.-%s: %.2f", this.idDeteccion, "Vehiculo", confidence);
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);
                            FontMetrics metrics = graphics.getFontMetrics(font);

                            int textX = (int) x1 + 5;  // Margen izquierdo dentro de la caja
                            int textY = (int) y1 + (int) (y2 - y1) - 5;  // Margen inferior dentro de la caja

                            int backgroundWidth = metrics.stringWidth(label) + 10; // Un poco más ancho que el texto
                            int backgroundHeight = metrics.getHeight(); // Altura del fondo

                            graphics.setColor(new Color(0, 0, 255, 180)); // Azul semi-transparente
                            graphics.fillRect(textX - 5, textY - backgroundHeight + 5, backgroundWidth, backgroundHeight);

                            graphics.setColor(Color.WHITE);
                            graphics.drawString(label, textX, textY);

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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Pala", x1, y1, x2, y2, confidence));
                            this.idDeteccion=deteccionService.findLastId();
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Pala",this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.CYAN);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            String label = String.format("%d.-%s: %.2f", this.idDeteccion, "Pala", confidence);

// Configura el texto y la fuente
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);
                            FontMetrics metrics = graphics.getFontMetrics(font);

// Calcula la posición para que el texto esté en la esquina inferior izquierda de la caja
                            int textX = (int) x1 + 5;  // Margen de 5 píxeles desde el borde izquierdo de la caja
                            int textY = (int) y1 + (int) (y2 - y1) - 5;  // Margen de 5 píxeles desde el borde inferior de la caja

// Dibuja el texto en la esquina inferior izquierda de la caja
                            graphics.drawString(label, textX, textY);
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Pallet", x1, y1, x2, y2, confidence));
                            this.idDeteccion=deteccionService.findLastId(); ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence,"Pallet",this.idDeteccion );
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.BLUE);  // Color azul para la caja de detección
                            graphics.setStroke(new java.awt.BasicStroke(3));

// Dibuja el rectángulo de detección
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));

// Define el texto y configura la fuente
                            String label = String.format("%d.-%s: %.2f", this.idDeteccion, "Pala", confidence);
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);
                            FontMetrics metrics = graphics.getFontMetrics(font);

// Calcula la posición del texto dentro de la caja, en la esquina inferior izquierda
                            int textX = (int) x1 + 5;  // Margen izquierdo
                            int textY = (int) y1 + (int) (y2 - y1) - 5;  // Margen inferior dentro de la caja

// Calcula las dimensiones del fondo del texto
                            int backgroundWidth = metrics.stringWidth(label) + 10; // Ancho un poco mayor al del texto
                            int backgroundHeight = metrics.getHeight(); // Altura del fondo

// Dibuja el fondo en azul
                            graphics.setColor(new Color(0, 0, 255, 180)); // Azul semi-transparente
                            graphics.fillRect(textX - 5, textY - backgroundHeight + 5, backgroundWidth, backgroundHeight);

// Dibuja el texto en blanco encima del fondo azul
                            graphics.setColor(Color.WHITE);
                            graphics.drawString(label, textX, textY);

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
                    float confidence2= outputData[i][5][j];

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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Camion", x1, y1, x2, y2,this.maxConfidence(confidence,confidence2) ));
                            this.idDeteccion=deteccionService.findLastId(); ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, this.maxConfidence(confidence,confidence2), "Camion",this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.YELLOW);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            String label = String.format("%d.-%s: %.2f", this.idDeteccion, "Camion", this.maxConfidence(confidence, confidence2));

                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);
                            FontMetrics metrics = graphics.getFontMetrics(font);

                            int textX = (int) x1 + 5;  // Margen de 5 píxeles desde el borde izquierdo de la caja
                            int textY = (int) y1 + (int) (y2 - y1) - 5;  // Margen de 5 píxeles desde el borde inferior de la caja

                            graphics.drawString(label, textX, textY);

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
    public List<ObjectDetectionResult> performBarcoDetection(MultipartFile imageFile, Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/barcos/best.onnx").toURI());
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
                    boolean passesConfidenceFilter = false;
                    float confianzaMaxima=0;
                    for (int k = 4; k <= 13; k++) {
                        if (outputData[i][k][j] >= this.minConfig) {
                            passesConfidenceFilter = true;
                            confianzaMaxima=outputData[i][k][j];
                            break; // Detener el ciclo tan pronto se encuentra una confianza válida
                        }
                    }
                    // Filtrar por confianza
                    if (passesConfidenceFilter) {
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Barco", x1, y1, x2, y2,confianzaMaxima ));
                            this.idDeteccion=deteccionService.findLastId(); ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confianzaMaxima, "Camion",this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.RED);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            String label = String.format("%d.-%s: %.2f", this.idDeteccion, "Barco", confianzaMaxima);

                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);
                            FontMetrics metrics = graphics.getFontMetrics(font);

                            int textX = (int) x1 + 5;  // Margen de 5 píxeles desde el borde izquierdo de la caja
                            int textY = (int) y1 + (int) (y2 - y1) - 5;  // Margen de 5 píxeles desde el borde inferior de la caja

                            graphics.drawString(label, textX, textY);

                            graphics.dispose();
                            passesConfidenceFilter=false;
                            confianzaMaxima=0;
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_barco_image.jpg";
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
    private float maxConfidence(float conf1,float conf2){
        if(conf1>conf2){
            return conf1;
        }else{
            return conf2;
        }
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Tubo", x1, y1, x2, y2, confidence));
                            this.idDeteccion=deteccionService.findLastId(); ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Tubo",this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.yellow);
                            graphics.setStroke(new java.awt.BasicStroke(3));

                            // Dibujar el rectángulo (caja delimitadora)
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));

                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            String label = String.format("%d.-%s: %.2f",this.idDeteccion, "Tubo", confidence);

                            // Dibujar el texto sobre la imagen, justo encima de la caja
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);

                            // Obtener las métricas de texto para calcular el tamaño del fondo
                            FontMetrics metrics = graphics.getFontMetrics(font);
                            int textWidth = metrics.stringWidth(label);
                            int textHeight = metrics.getHeight();

                            // Establecer color del fondo (amarillo)
                            graphics.setColor(Color.YELLOW);

                            // Dibujar un rectángulo como fondo del texto
                            graphics.fillRect((int) x1, (int) y1 - textHeight, textWidth, textHeight);

                            // Establecer color de la letra (blanco)
                            graphics.setColor(Color.WHITE);

                            // Dibujar el texto encima del fondo amarillo
                            graphics.drawString(label, (int) x1, (int) y1 - 5);

                            // Liberar los recursos gráficos
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

    public List<ObjectDetectionResult> performTaladroDeteccion(MultipartFile imageFile, Long proyectoId){
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/taladros/best.onnx").toURI());
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
                            this.idDeteccion++;
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Taladro",this.idDeteccion);
                            results.add(detectionResult);
                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, "Taladro", x1, y1, x2, y2, confidence));

                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.yellow);
                            graphics.setStroke(new java.awt.BasicStroke(3));

                            // Dibujar el rectángulo (caja delimitadora)
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));

                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            String label = String.format("%d.-%s: %.2f",this.idDeteccion, "Taladro", confidence);

                            // Dibujar el texto sobre la imagen, justo encima de la caja
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);

                            // Obtener las métricas de texto para calcular el tamaño del fondo
                            FontMetrics metrics = graphics.getFontMetrics(font);
                            int textWidth = metrics.stringWidth(label);
                            int textHeight = metrics.getHeight();

                            // Establecer color del fondo (amarillo)
                            graphics.setColor(Color.GRAY);

                            // Dibujar un rectángulo como fondo del texto
                            graphics.fillRect((int) x1, (int) y1 - textHeight, textWidth, textHeight);

                            // Establecer color de la letra (blanco)
                            graphics.setColor(Color.black);

                            // Dibujar el texto encima del fondo amarillo
                            graphics.drawString(label, (int) x1, (int) y1 - 5);

                            // Liberar los recursos gráficos
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_taladro_image.jpg";
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

    public List<ObjectDetectionResult> performEstacionDetection (MultipartFile imageFile, Long proyectoId) {
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
            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[0][0].length; j++) {  // 8400 posibles detecciones
                    float cx = outputData[0][0][j];  // x central
                    float cy = outputData[0][1][j];  // y central
                    float width = outputData[0][2][j];  // Ancho
                    float height = outputData[0][3][j];  // Alto

                    // Iterar sobre las confidencias de clase y encontrar la más alta
                    int detectedClassIndex = -1;
                    float maxConfidence = 0;
                    for (int classIdx = 4; classIdx <= 7; classIdx++) {
                        float confidence = outputData[0][classIdx][j];
                        if (confidence > maxConfidence) {
                            maxConfidence = confidence;
                            detectedClassIndex = classIdx - 4; // Para mapear correctamente a `classLabels`
                        }
                    }

                    // Procesar si la confianza máxima es suficiente
                    if (maxConfidence >= 0.3) {
                        String classLabel = classLabels[detectedClassIndex];

                        // Convertir coordenadas de centro a esquinas
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectoId, null, classLabel, x1, y1, x2, y2, maxConfidence));
                            this.idDeteccion=deteccionService.findLastId();
                            ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, maxConfidence, classLabel,this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.CYAN);
                            graphics.setStroke(new java.awt.BasicStroke(3));
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                            String label = String.format("%d.-%s: %.2f", this.idDeteccion, classLabel, maxConfidence);

                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);
                            FontMetrics metrics = graphics.getFontMetrics(font);

                            int textX = (int) x1 + 5;  // Margen de 5 píxeles desde el borde izquierdo de la caja
                            int textY = (int) y1 + (int) (y2 - y1) - 5;  // Margen de 5 píxeles desde el borde inferior de la caja

                            graphics.drawString(label, textX, textY);
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
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
    public List<ObjectDetectionResult> performPersonaDetection (MultipartFile imagen, Long proyectId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/personas/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imagen), 640, 640);
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectId, null, "Obrero", x1, y1, x2, y2, confidence));
                            this.idDeteccion=deteccionService.findLastId(); ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Obrero",this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.RED);
                            graphics.setStroke(new java.awt.BasicStroke(3));

                            // Dibujar el rectángulo (caja delimitadora)
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));

                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            String label = String.format("%d.-%s: %.2f",this.idDeteccion, "Obrero", confidence);

                            // Dibujar el texto sobre la imagen, justo encima de la caja
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);

                            // Obtener las métricas de texto para calcular el tamaño del fondo
                            FontMetrics metrics = graphics.getFontMetrics(font);
                            int textWidth = metrics.stringWidth(label);
                            int textHeight = metrics.getHeight();

                            // Establecer color del fondo (amarillo)
                            graphics.setColor(Color.RED);

                            // Dibujar un rectángulo como fondo del texto
                            graphics.fillRect((int) x1, (int) y1 - textHeight, textWidth, textHeight);

                            // Establecer color de la letra (blanco)
                            graphics.setColor(Color.WHITE);

                            // Dibujar el texto encima del fondo amarillo
                            graphics.drawString(label, (int) x1, (int) y1 - 5);

                            // Liberar los recursos gráficos
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_person_image.jpg";
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
    public List<ObjectDetectionResult> performMontacargaDetection (MultipartFile imagen, Long proyectId) {
        List<ObjectDetectionResult> results = new ArrayList<>();
        float iouThreshold = 0.5f;  // Umbral para considerar que dos cajas representan el mismo objeto

        try {
            // Cargar el modelo ONNX
            Path modelPath = Paths.get(ClassLoader.getSystemResource("models/forklift/best.onnx").toURI());
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image=null;
            if(this.imagen==null) {
                // Leer la imagen desde el MultipartFile y redimensionarla
                image=resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imagen), 640, 640);
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

                            deteccionService.crearDeteccion(new DeteccionServiceDto(null, proyectId, null, "Montacargas", x1, y1, x2, y2, confidence));
                            this.idDeteccion=deteccionService.findLastId(); ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Montacargas",this.idDeteccion);
                            results.add(detectionResult);
                            // Dibujar la detección en la imagen
                            Graphics2D graphics = image.createGraphics();
                            graphics.setColor(Color.RED);
                            graphics.setStroke(new java.awt.BasicStroke(3));

                            // Dibujar el rectángulo (caja delimitadora)
                            graphics.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));

                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            // Crear el texto con la clase y la confianza (redondeada a dos decimales)
                            String label = String.format("%d.-%s: %.2f",this.idDeteccion, "Montacargas", confidence);

                            // Dibujar el texto sobre la imagen, justo encima de la caja
                            Font font = new Font("Arial", Font.BOLD, 16);
                            graphics.setFont(font);

                            // Obtener las métricas de texto para calcular el tamaño del fondo
                            FontMetrics metrics = graphics.getFontMetrics(font);
                            int textWidth = metrics.stringWidth(label);
                            int textHeight = metrics.getHeight();

                            // Establecer color del fondo (amarillo)
                            graphics.setColor(Color.RED);

                            // Dibujar un rectángulo como fondo del texto
                            graphics.fillRect((int) x1, (int) y1 - textHeight, textWidth, textHeight);

                            // Establecer color de la letra (blanco)
                            graphics.setColor(Color.BLACK);

                            // Dibujar el texto encima del fondo amarillo
                            graphics.drawString(label, (int) x1, (int) y1 - 5);

                            // Liberar los recursos gráficos
                            graphics.dispose();
                        }
                    }
                }
            }

            // Guardar la imagen con el recuadro en el disco
            String outputImagePath = "C:\\Users\\user\\Desktop\\detected_montacargas_image.jpg";
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
        this.idDeteccion=deteccionService.findLastId();
        List<ObjectDetectionResult> combinedResults = new ArrayList<>();
        Optional<Proyecto> p=proyectoRepository.findById(proyectId);
        this.minConfig=p.get().getMinConf();
        Usuario usuario=p.get().getUsuario();
        System.out.println("Comienzo analisis Vehiculos");
        // Realizar la detección de vehículos
        List<ObjectDetectionResult> vehicleDetections = performVehicleDetection(image,proyectId);
        combinedResults.addAll(vehicleDetections);
        System.out.println("Comienzo analisis palas");
        // Realizar la detección de grúas
        List<ObjectDetectionResult> palasDetections = performPalaDetection(image,proyectId);
        combinedResults.addAll(palasDetections);
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
        System.out.println("Comienzo analisis estaciones");
        List<ObjectDetectionResult> estaciones = performEstacionDetection(image,proyectId);
        combinedResults.addAll(estaciones);
        System.out.println("Comienzo analisis personas con chaleco");
        List<ObjectDetectionResult> personas = performPersonaDetection(image,proyectId);
        combinedResults.addAll(personas);
        System.out.println("Comienzo analisis montacargas");
        List<ObjectDetectionResult> forklifts = performMontacargaDetection(image,proyectId);
        combinedResults.addAll(forklifts);
        System.out.println("Comienzo analisis barcos");
        List<ObjectDetectionResult> barcos = performBarcoDetection(image,proyectId);
        combinedResults.addAll(barcos);
        ObjetoImagen obj=new ObjetoImagen();
        obj.setFallos(checkRestricciones(proyectId,combinedResults));

        obj.setObjetos(combinedResults);
        obj.setImage(encodeImageToBase64(this.imagen));
        this.imagen=null;
        if(combinedResults.size()>0) {
            StringBuilder cuerpoCorreo = new StringBuilder();
            cuerpoCorreo.append("Se han encontrado las siguientes detecciones en el análisis de imagen:\n\n");

            for (ObjectDetectionResult resultado : combinedResults) {
                cuerpoCorreo.append("Objeto detectado: ").append(resultado.getLabel()).append("\n");
                cuerpoCorreo.append("Confianza: ").append(resultado.getConfidence()).append("\n");
                cuerpoCorreo.append("Coordenadas: (")
                        .append("X: ").append(resultado.getX()).append(", ")
                        .append("Y: ").append(resultado.getY()).append(", ")
                        .append("Ancho: ").append(resultado.getWeight()).append(", ")
                        .append("Alto: ").append(resultado.getHeight()).append(")\n\n");
            }
            LocalDate localDate=LocalDate.now();
            //java.sql.Date fechaActual=Date.valueOf(localDate);
            //emailService.enviarCorreo(usuario.getEmail(), "Estado detecciones dia: " + fechaActual, cuerpoCorreo.toString());
            // Devolver la lista combinada de todas las detecciones
        }return obj;
    }




    private String checkRestricciones (Long proyectId, List<ObjectDetectionResult> combinedResults) {
        List<Restriccion> restricciones = restriccionRepository.findRestrictionsByProjectDaily(proyectId);
        LocalDate localDate = LocalDate.now();
        StringBuilder mensajeFallos = new StringBuilder();
        java.sql.Date fechaActual = Date.valueOf(localDate);
        restricciones.forEach(restriccion -> {
            System.out.println(restriccion);
            // Solo evaluamos las restricciones que están dentro del rango de fechas
            if (restriccion.getDiaria() &&
                    !fechaActual.before(restriccion.getFechaDesde()) &&
                    !fechaActual.after(restriccion.getFechaHasta())) {
                List<ObjectDetectionResult> deteccionesFiltradas = combinedResults.stream()
                        .filter(deteccion ->
                                (deteccion.getLabel().replace(" ", "").toLowerCase().equals(restriccion.getObjeto().replace(" ", "").toLowerCase()))
                        ).collect(Collectors.toList());
                int cantidadDetecciones = deteccionesFiltradas.size();

                // Verificamos si se cumple la cantidad mínima y máxima de detecciones
                if (cantidadDetecciones > restriccion.getCantidadMax() || cantidadDetecciones < restriccion.getCantidadMin()) {
                    Fallo nuevoFallo = Fallo.builder()
                            .restriccion(restriccion)  // Asociamos la restricción que falló
                            .datos("La restricción no se cumplió: Objeto esperado: " + restriccion.getObjeto() +", se esperaban entre "+restriccion.getCantidadMin()+" y "+
                                    restriccion.getCantidadMax()+" apariciones el dia de "+fechaActual+" y son: " + cantidadDetecciones)
                            .fecha(fechaActual)
                            .build();
                    restriccion.setCumplida(false);
                    restriccionRepository.save(restriccion);
                    // Guardamos el fallo en la base de datos
                    fallosRepository.save(nuevoFallo);
                    mensajeFallos.append("Fallo: ").append(nuevoFallo.getDatos()).append("\n");

                }
            }
        });
        String mensaje = mensajeFallos.length() > 0 ? mensajeFallos.toString() : null;
        return mensaje;
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

