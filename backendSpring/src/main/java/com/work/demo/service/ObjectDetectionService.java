package com.work.demo.service;

import ai.onnxruntime.*;
import com.work.demo.service.dto.AnalisisReturnDto;
import com.work.demo.service.utils.ImageUtils;
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

    public AnalisisReturnDto performAllDetectionsAndReturnImage(MultipartFile imageFile) {
        AnalisisReturnDto r=new AnalisisReturnDto();
        BufferedImage imageWithDetections = null;
        List<ObjectDetectionContainer> combinedResults = new ArrayList<>();

        // Realizar la detección de conos
        List<ObjectDetectionContainer> coneDetections = performConeDetection(imageFile);
        combinedResults.addAll(coneDetections);

        // Realizar la detección de vehículos
        List<ObjectDetectionContainer> vehicleDetections = performVehicleDetection(imageFile);
        combinedResults.addAll(vehicleDetections);

        // Realizar la detección de grúas
        List<ObjectDetectionContainer> gruasDetections = performGruasDetection(imageFile);
        combinedResults.addAll(gruasDetections);

        // Realizar la detección de palets
        List<ObjectDetectionContainer> palletDetections = performPalletDetection(imageFile);
        combinedResults.addAll(palletDetections);
        r.setDetecciones(combinedResults);
        try {
            // Realizar todas las detecciones (Conos, Vehículos, Gruas, etc.)
            performConeDetection(imageFile);
            performVehicleDetection(imageFile);
            performGruasDetection(imageFile);
            performPalletDetection(imageFile);

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

    public List<ObjectDetectionContainer> performConeDetection (MultipartFile imageFile) {
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

    public List<ObjectDetectionContainer> performVehicleDetection (MultipartFile imageFile) {
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
        ObjectDetectionContainer cont = new ObjectDetectionContainer();
        cont.setObjects(results);
        List<ObjectDetectionContainer> l = new ArrayList<>();
        l.add(cont);
        return l;
    }

    public List<ObjectDetectionContainer> performGruasDetection (MultipartFile imageFile) {
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
        return l;
    }

    public List<ObjectDetectionContainer> performPalletDetection (MultipartFile imageFile) {
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
        ObjectDetectionContainer cont = new ObjectDetectionContainer();
        cont.setObjects(results);
        List<ObjectDetectionContainer> l = new ArrayList<>();
        l.add(cont);
        return l;
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

