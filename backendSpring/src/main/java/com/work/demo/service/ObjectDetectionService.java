package com.work.demo.service;

import ai.onnxruntime.*;
import com.work.demo.service.utils.ImageUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.work.demo.rest.dto.ObjectDetectionContainer;
import com.work.demo.rest.dto.ObjectDetectionResult;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Service
public class ObjectDetectionService {

/**

 List<ObjectDetectionContainer> detectionContainers = new ArrayList<>();

 try {
 // Guardar el archivo MultipartFile a una ubicación temporal
 File tempFile = File.createTempFile("uploaded", ".jpg");
 imageFile.transferTo(tempFile);

 // Cargar la imagen desde el archivo temporal
 Mat image = Imgcodecs.imread(tempFile.getAbsolutePath());

 // Cargar el modelo ONNX
 String modelPath = "C:/Users/eii/Desktop/prueba/best.onnx";
 Net net = Dnn.readNetFromONNX(modelPath);

 // Preprocesar la imagen
 Mat blob = Dnn.blobFromImage(image, 1.0 / 255.0, new Size(640, 640), new Scalar(0, 0, 0), true, false);

 // Establecer el blob como input de la red
 net.setInput(blob);

 // Realizar la detección
 Mat output = net.forward();

 // Procesar las salidas
 for (int i = 0; i < output.size(2); i++) {
 double confidence = output.get(0, i)[4];  // Confianza de la detección
 if (confidence > 0.5) {
 double[] data = output.get(0, i);

 // Las coordenadas de la caja delimitadora
 double x = data[0] * image.cols();
 double y = data[1] * image.rows();
 double width = data[2] * image.cols();
 double height = data[3] * image.rows();

 // Crear un contenedor de detección
 ObjectDetectionResult detectionResult = new ObjectDetectionResult(x, y, width, height, confidence);
 ObjectDetectionContainer container = new ObjectDetectionContainer();
 container.addObjectDetectionResult(detectionResult);
 detectionContainers.add(container);

 // Dibujar la caja delimitadora en la imagen
 Point topLeft = new Point(x - width / 2, y - height / 2);
 Point bottomRight = new Point(x + width / 2, y + height / 2);
 Imgproc.rectangle(image, topLeft, bottomRight, new Scalar(0, 255, 0), 2);

 // (Opcional) Guardar la imagen con las detecciones para verificación
 Imgcodecs.imwrite("C:/Users/eii/Desktop/prueba/a_out.jpg", image);
 }
 }

 // Eliminar el archivo temporal
 tempFile.delete();

 } catch (IOException e) {
 e.printStackTrace();
 }

 return detectionContainers;
 **/
    public List<ObjectDetectionContainer> performObjectDetection(MultipartFile imageFile) {
        List<ObjectDetectionResult> results = new ArrayList<>();

        try {
            // Cargar el modelo ONNX
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\wsPagWeb\\trainsExitosos\\conesTrain\\weights\\best.onnx", new OrtSession.SessionOptions());

            // Leer la imagen desde el MultipartFile
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
                        bestBox[0], bestBox[1], bestBox[2], bestBox[3], bestConfidence,"Cono"
                );
                results.add(detectionResult);
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

