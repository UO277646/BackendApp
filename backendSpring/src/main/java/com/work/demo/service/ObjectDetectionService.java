package com.work.demo.service;

import ai.onnxruntime.*;
import com.work.demo.service.dto.AnalisisReturnDto;
import com.work.demo.service.dto.DeteccionServiceDto;
import com.work.demo.service.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.work.demo.rest.dto.ObjectDetectionContainer;
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
                float confidence = outputData[i][4][0]; // Usualmente la confianza está en la 5ta posición

                // Solo considerar detecciones con suficiente confianza
                if (confidence > 0.5) {
                    float cx = outputData[i][0][0];
                    float cy = outputData[i][1][0];
                    float width = outputData[i][2][0];
                    float height = outputData[i][3][0];

                    // Convertir coordenadas centrales a esquinas
                    float x1 = cx - width / 2;
                    float y1 = cy - height / 2;
                    float x2 = cx + width / 2;
                    float y2 = cy + height / 2;

                    float[] newBox = new float[]{x1, y1, x2, y2};

                    // Verificar si la caja es similar a una detección existente
                    boolean esCajaDuplicada = false;
                    for (ObjectDetectionResult resultDet : results) {
                        // Convertir x, y, width, height de resultDet a esquinas
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

                    // Si no es una caja duplicada, agregar la nueva detección
                    if (!esCajaDuplicada) {
                        ObjectDetectionResult detectionResult = new ObjectDetectionResult(cx, cy, width, height, confidence, "Cono");
                        results.add(detectionResult);

                        // Guardar la detección en la base de datos
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

/** Funciona
    public List<ObjectDetectionContainer> performConeDetection (MultipartFile imageFile,Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\conesTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image = resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);

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
            float[] bestBox = null;

            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza y buscar la detección con mayor confianza
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence;

                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Actualizar la mejor caja delimitadora
                        bestBox = new float[]{x1, y1, x2, y2};
                    }
                }
            }

            // Almacenar la mejor detección si tiene suficiente confianza
            if (bestConfidence > 0.5) {
                System.out.println("Mejor detección con confianza: " + bestConfidence);
                System.out.println("Caja delimitadora: " + Arrays.toString(bestBox));

                ObjectDetectionResult detectionResult = new ObjectDetectionResult(
                        bestBox[0], bestBox[1], bestBox[2], bestBox[3], bestConfidence, "Cono"
                );
                results.add(detectionResult);
                deteccionService.crearDeteccion(new DeteccionServiceDto(null,proyectoId,null,"Cono",bestBox[0], bestBox[1], bestBox[2], bestBox[3], bestConfidence));
                // Dibujar el recuadro en la imagen
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.RED);
                graphics.setStroke(new java.awt.BasicStroke(3));
                graphics.drawRect((int) bestBox[0], (int) bestBox[1], (int) (bestBox[2] - bestBox[0]), (int) (bestBox[3] - bestBox[1]));
                graphics.dispose();

                // Guardar la imagen con el recuadro en el disco
                String outputImagePath = "C:\\Users\\user\\Desktop\\detected_image.jpg";
                File outputfile = new File(outputImagePath);
                ImageIO.write(image, "jpg", outputfile);

                System.out.println("Imagen guardada en: " + outputImagePath);
            }

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        }

        // Envolver los resultados en un contenedor
        ObjectDetectionContainer cont = new ObjectDetectionContainer();
        cont.setObjects(results);
        List<ObjectDetectionContainer> l = new ArrayList<>();
        l.add(cont);
        return l;
    }

*/
    public List<ObjectDetectionResult> performVehicleDetection (MultipartFile imageFile,Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\VehiculosTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image = resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);

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
            float[] bestBox = null;

            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza y buscar la detección con mayor confianza
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence;

                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Actualizar la mejor caja delimitadora
                        bestBox = new float[]{x1, y1, x2, y2};
                    }
                }
            }

            // Almacenar la mejor detección si tiene suficiente confianza
            if (bestConfidence > 0.5) {
                System.out.println("Mejor detección con confianza: " + bestConfidence);
                System.out.println("Caja delimitadora: " + Arrays.toString(bestBox));

                ObjectDetectionResult detectionResult = new ObjectDetectionResult(
                        bestBox[0], bestBox[1], bestBox[2], bestBox[3], bestConfidence, "Vehicle"
                );
                results.add(detectionResult);

                // Dibujar el recuadro en la imagen
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.RED);
                graphics.setStroke(new java.awt.BasicStroke(3));
                graphics.drawRect((int) bestBox[0], (int) bestBox[1], (int) (bestBox[2] - bestBox[0]), (int) (bestBox[3] - bestBox[1]));
                graphics.dispose();

                // Guardar la imagen con el recuadro en el disco
                String outputImagePath = "C:\\Users\\user\\Desktop\\detected_image.jpg";
                File outputfile = new File(outputImagePath);
                ImageIO.write(image, "jpg", outputfile);

                System.out.println("Imagen guardada en: " + outputImagePath);
            }

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

    public List<ObjectDetectionResult> performGruasDetection (MultipartFile imageFile,Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\gruasTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image = resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);

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
            float[] bestBox = null;

            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza y buscar la detección con mayor confianza
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence;

                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Actualizar la mejor caja delimitadora
                        bestBox = new float[]{x1, y1, x2, y2};
                    }
                }
            }

            // Almacenar la mejor detección si tiene suficiente confianza
            if (bestConfidence > 0.5) {
                System.out.println("Mejor detección con confianza: " + bestConfidence);
                System.out.println("Caja delimitadora: " + Arrays.toString(bestBox));

                ObjectDetectionResult detectionResult = new ObjectDetectionResult(
                        bestBox[0], bestBox[1], bestBox[2], bestBox[3], bestConfidence, "Grua"
                );
                results.add(detectionResult);

                // Dibujar el recuadro en la imagen
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.RED);
                graphics.setStroke(new java.awt.BasicStroke(3));
                graphics.drawRect((int) bestBox[0], (int) bestBox[1], (int) (bestBox[2] - bestBox[0]), (int) (bestBox[3] - bestBox[1]));
                graphics.dispose();

                // Guardar la imagen con el recuadro en el disco
                String outputImagePath = "C:\\Users\\user\\Desktop\\detected_image.jpg";
                File outputfile = new File(outputImagePath);
                ImageIO.write(image, "jpg", outputfile);

                System.out.println("Imagen guardada en: " + outputImagePath);
            }

            // Liberar los recursos
            inputTensor.close();
            outputTensor.close();
            session.close();
            env.close();

        } catch (OrtException | IOException e) {
            e.printStackTrace();
        }

        // Envolver los resultados en un contenedor
        ObjectDetectionContainer cont = new ObjectDetectionContainer();
        cont.setObjects(results);
        List<ObjectDetectionContainer> l = new ArrayList<>();
        l.add(cont);
        return results;
    }

    public List<ObjectDetectionResult> performPalletDetection (MultipartFile imageFile,Long proyectoId) {
        List<ObjectDetectionResult> results = new ArrayList<>();

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\palletTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile y redimensionarla
            BufferedImage image = resizeImage(ImageUtils.convertMultipartFileToBufferedImage(imageFile), 640, 640);

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
            float[] bestBox = null;

            for (int i = 0; i < outputData.length; i++) {
                for (int j = 0; j < outputData[i][0].length; j++) {
                    // Descomponemos las 5 características
                    float cx = outputData[i][0][j];
                    float cy = outputData[i][1][j];
                    float width = outputData[i][2][j];
                    float height = outputData[i][3][j];
                    float confidence = outputData[i][4][j];

                    // Filtrar por confianza y buscar la detección con mayor confianza
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence;

                        // Convertir de coordenadas centrales a esquinas
                        float x1 = cx - width / 2;
                        float y1 = cy - height / 2;
                        float x2 = cx + width / 2;
                        float y2 = cy + height / 2;

                        // Actualizar la mejor caja delimitadora
                        bestBox = new float[]{x1, y1, x2, y2};
                    }
                }
            }

            // Almacenar la mejor detección si tiene suficiente confianza
            if (bestConfidence > 0.5) {
                System.out.println("Mejor detección con confianza: " + bestConfidence);
                System.out.println("Caja delimitadora: " + Arrays.toString(bestBox));

                ObjectDetectionResult detectionResult = new ObjectDetectionResult(
                        bestBox[0], bestBox[1], bestBox[2], bestBox[3], bestConfidence, "Vehicle"
                );
                results.add(detectionResult);

                // Dibujar el recuadro en la imagen
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.RED);
                graphics.setStroke(new java.awt.BasicStroke(3));
                graphics.drawRect((int) bestBox[0], (int) bestBox[1], (int) (bestBox[2] - bestBox[0]), (int) (bestBox[3] - bestBox[1]));
                graphics.dispose();

                // Guardar la imagen con el recuadro en el disco
                String outputImagePath = "C:\\Users\\user\\Desktop\\detected_image.jpg";
                File outputfile = new File(outputImagePath);
                ImageIO.write(image, "jpg", outputfile);

                System.out.println("Imagen guardada en: " + outputImagePath);
            }

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
}

