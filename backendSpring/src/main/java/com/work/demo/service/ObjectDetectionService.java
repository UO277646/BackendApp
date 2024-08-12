package com.work.demo.service;
import ai.onnxruntime.*;
import ch.qos.logback.core.net.SyslogOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.work.demo.rest.dto.ObjectDetectionContainer;
import com.work.demo.rest.dto.ObjectDetectionResult;
import com.work.demo.service.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

@Service
public class ObjectDetectionService {

/**
 * public List<ObjectDetectionContainer> performObjectDetection(MultipartFile imageFile) {
 *     List<ObjectDetectionResult> results=new ArrayList<>();
 *
 *     try {
 *         // Cargar el modelo ONNX
 *         OrtEnvironment env=OrtEnvironment.getEnvironment();
 *         String modelPath="C:\\Users\\user\\Desktop\\pruebasYolo\\RESULTS\\VehiclesTest\\weights.onnx";
 *         OrtSession.SessionOptions opts=new OrtSession.SessionOptions();
 *         OrtSession session=env.createSession(modelPath, opts);
 *
 *         // Leer la imagen desde el MultipartFile
 *         BufferedImage image=ImageUtils.convertMultipartFileToBufferedImage(imageFile);
 *
 *         // Preprocesar la imagen según sea necesario
 *
 *         // Convertir la imagen a un tensor de entrada
 *         float[][][] inputData=ImageUtils.convertImageTo3DFloatArray(image);
 *         long[] inputShape=new long[]{1, 3, image.getHeight(), image.getWidth()}; // Forma del tensor de entrada
 *         OnnxTensor inputTensor=OnnxTensor.createTensor(env, inputData, inputShape);
 *
 *         // Crear el mapa de entradas para el modelo
 *         Map<String, OnnxTensor> inputs=Collections.singletonMap("input", inputTensor);
 *
 *         // Ejecutar la sesión y obtener todas las salidas
 *         OrtSession.Result result=session.run(inputs);
 *
 *         // Procesar los resultados de detección de objetos
 *         for (Map.Entry<String, OnnxValue> entry : result) {
 *             if (entry.getValue() instanceof OnnxTensor) {
 *                 OnnxTensor outputTensor=(OnnxTensor) entry.getValue();
 *                 float[][][] outputData=(float[][][]) outputTensor.getValue();
 *
 *                 // Convertir los resultados a la estructura ObjectDetectionResult
 *                 // y añadirlos a la lista results
 *             }
 *         }
 *
 *     } catch (OrtException e) {
 *         System.err.println("Error al cargar el modelo ONNX: " + e.getMessage());
 *     } catch (IOException e) {
 *         System.err.println("Error al leer la")
 *     }
 * }
 *

 **/
public List<ObjectDetectionContainer> performObjectDetection(MultipartFile imageFile) {
    List<ObjectDetectionResult> results = new ArrayList<>();

    try {
        // Cargar el modelo ONNX
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        OrtSession session = env.createSession("C:\\Users\\user\\Desktop\\pruebasYolo\\RESULTS\\VehiclesTest\\weights\\best.onnx", new OrtSession.SessionOptions());

        // Leer la imagen desde el MultipartFile
        BufferedImage image = ImageUtils.convertMultipartFileToBufferedImage(imageFile);

        // Preprocesar la imagen según sea necesario

        // Convertir la imagen a un tensor de entrada
        float[][][][] inputData = ImageUtils.convertImageTo4DFloatArray(image);
        System.out.println(inputData);
        //long[] inputShape = new long[]{1, 3, image.getHeight(), image.getWidth()}; // Forma del tensor de entrada

        // Crear el input tensor
        OnnxTensor inputTensor =OnnxTensor.createTensor(env, inputData);
        Map<String, OnnxTensorLike> inputs = Collections.singletonMap("images", inputTensor);
        Set<String> outputNames = new HashSet<>(); // Vacío para obtener todas las salidas
        // Realizar la inferencia
        OrtSession.Result result = session.run(inputs );//,outputNames
        OnnxTensor outputTensor =(OnnxTensor) result.get("output0").get();
        float[][][] outputData = (float[][][]) outputTensor.getValue();
        for (float[][] detection : outputData) {
            float[] box = detection[0]; // Coordenadas de la caja [x1, y1, x2, y2]
            float[] scores = detection[1]; // Puntuaciones para cada clase
            int classId = argMax(scores); // Clase con mayor puntuación
            float confidence = scores[classId]; // Confianza de la clase

            if (confidence > 0.5) { // Umbral de confianza
                System.out.println("Detectado vehículo con confianza: " + confidence);
                System.out.println("Caja delimitadora: " + Arrays.toString(box));
            }
        }
        //float[][][][] outputData = (float[][][][]) outputTensor.getValue();
        for (int i = 0; i < outputData.length; i++) {  // outputData.length debería ser 1 (el tamaño del lote)
            for (int j = 0; j < outputData[i].length; j++) {  // outputData[i].length debería ser 8400 (el número de detecciones)
                float[] detection = outputData[i][j];  // Cada detección tiene 5 características

                // Suponiendo que detection[0:3] son las coordenadas y detection[4] es la confianza
                float confidence = detection[4]; // La confianza de la detección

                if (confidence > 0.5) { // Filtrar detecciones de baja confianza
                    // Interpretar las coordenadas del cuadro delimitador
                    float x = detection[0];
                    float y = detection[1];
                    float width = detection[2];
                    float height = detection[3];

                    // Crear un objeto resultado de detección
                    ObjectDetectionResult detectionResult = new ObjectDetectionResult(x, y, width, height, confidence);
                    results.add(detectionResult);
                }
            }
        }
        // Liberar los recursos
        inputTensor.close();
        outputTensor.close();
        session.close();
        env.close();
        //  float[][] outputData = (float[][]) result.get(0).getValue();

        // Postprocesamiento de los resultados
        // Aquí deberías interpretar las salidas del modelo y procesar los resultados según sea necesario
        // Por ejemplo, iterar sobre las salidas y crear objetos ObjectDetectionResult con las etiquetas y confianzas

        //   for (float[] detection : outputData) {
            //   String label = "Label"; // Reemplazar con la lógica real para obtener la etiqueta del objeto
            //     float confidence = detection[0]; // Por ejemplo, supongamos que la primera salida es la confianza
            // results.add(new ObjectDetectionResult(label, confidence));
        //}

    } catch (OrtException | IOException e) {
        e.printStackTrace();
    }
    ObjectDetectionContainer cont=new ObjectDetectionContainer();
    cont.setObjects(results);
    List<ObjectDetectionContainer> l=new ArrayList<>();
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
}

