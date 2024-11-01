package com.work.demo;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ai.onnxruntime.*;
import com.work.demo.rest.dto.ObjectDetectionResult;
import com.work.demo.service.DeteccionService;
import com.work.demo.service.ObjectDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ResourceUtils;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

class ObjectDetectionServiceTest {

    @Mock
    private DeteccionService deteccionService;

    @InjectMocks
    private ObjectDetectionService detectionService;

    private MockMultipartFile mockConeImageFile;
    private MockMultipartFile mockCamionImageFile;
    private MockMultipartFile mockEstacionImageFile;
    private MockMultipartFile mockMontacargasImageFile;
    private MockMultipartFile mockObreroImageFile;
    private MockMultipartFile mockPalletImageFile;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        Field minConfigField = ObjectDetectionService.class.getDeclaredField("minConfig");
        minConfigField.setAccessible(true);
        minConfigField.set(detectionService, 0.8);
        mockConeImageFile = loadMockMultipartFile("imagenes/cono.jpg");
        mockCamionImageFile = loadMockMultipartFile("imagenes/camion.jpg");
        mockEstacionImageFile = loadMockMultipartFile("imagenes/estacion.jpg");
        mockMontacargasImageFile = loadMockMultipartFile("imagenes/montacargas.jpg");
        mockObreroImageFile = loadMockMultipartFile("imagenes/obrero.jpg");
        mockPalletImageFile = loadMockMultipartFile("imagenes/pallet.jpg");
    }

    private MockMultipartFile loadMockMultipartFile(String filePath) throws IOException {
        try (InputStream imageStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            Objects.requireNonNull(imageStream, "No se encontró el archivo: " + filePath);
            byte[] imageBytes = imageStream.readAllBytes();
            return new MockMultipartFile("imageFile", filePath, "image/jpeg", imageBytes);
        }
    }
    @Test
    void testPerformConeDetection() throws IOException, OrtException, URISyntaxException {
        // Configurar mocks
        BufferedImage testImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
        when(deteccionService.findLastId()).thenReturn(1);

        // Simulación del entorno y sesión ONNX
        OrtEnvironment mockEnv = mock(OrtEnvironment.class);
        OrtSession mockSession = mock(OrtSession.class);
        when(mockEnv.createSession(anyString(), any())).thenReturn(mockSession);

        // Mock de salida del modelo ONNX
        float[][][] mockOutputData = {{{0.5f, 0.5f, 0.3f, 0.3f, 0.8f}}}; // Datos de ejemplo con una detección de confianza 0.8
        OnnxTensor mockTensor = mock(OnnxTensor.class);
        when(mockTensor.getValue()).thenReturn(mockOutputData);

        OnnxTensorLike mockTensorLike = mock(OnnxTensorLike.class);
        OrtSession.Result mockResult = mock(OrtSession.Result.class);
        when(mockResult.get(anyString())).thenReturn(java.util.Optional.of(mockTensorLike));
        when(mockSession.run(any())).thenReturn(mockResult);

        // Ejecutar el método
        List<ObjectDetectionResult> results = detectionService.performConeDetection(mockConeImageFile, 1L);

        // Verificar resultados
        assertNotNull(results);
        assertEquals(1, results.size());  // Verificar que haya una detección en los resultados
        assertEquals("Cono", results.get(0).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertTrue(results.get(0).getConfidence() > 0.8f, "La confianza de la detección debe ser mayor que 0.8");
        // Verificar interacciones
         verify(deteccionService, times(1)).findLastId();
    }
    @Test
    void testPerformVehicleDetection() throws IOException, OrtException, URISyntaxException {
        // Configurar mocks
        BufferedImage testImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
        when(deteccionService.findLastId()).thenReturn(1);

        // Simulación del entorno y sesión ONNX
        OrtEnvironment mockEnv = mock(OrtEnvironment.class);
        OrtSession mockSession = mock(OrtSession.class);
        when(mockEnv.createSession(anyString(), any())).thenReturn(mockSession);

        // Mock de salida del modelo ONNX
        float[][][] mockOutputData = {{{0.5f, 0.5f, 0.3f, 0.3f, 0.8f}}}; // Datos de ejemplo con una detección de confianza 0.8
        OnnxTensor mockTensor = mock(OnnxTensor.class);
        when(mockTensor.getValue()).thenReturn(mockOutputData);

        OnnxTensorLike mockTensorLike = mock(OnnxTensorLike.class);
        OrtSession.Result mockResult = mock(OrtSession.Result.class);
        when(mockResult.get(anyString())).thenReturn(java.util.Optional.of(mockTensorLike));
        when(mockSession.run(any())).thenReturn(mockResult);

        // Ejecutar el método
        List<ObjectDetectionResult> results = detectionService.performCamionDetection(mockCamionImageFile, 1L);

        // Verificar resultados
        assertNotNull(results);
        assertEquals(1, results.size());  // Verificar que haya una detección en los resultados
        assertEquals("Camion", results.get(0).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertTrue(results.get(0).getConfidence() > 0.8f, "La confianza de la detección debe ser mayor que 0.8");
        // Verificar interacciones
        verify(deteccionService, times(1)).findLastId();
    }
    @Test
    void testPerformForkLiftDetection() throws IOException, OrtException, URISyntaxException {
        // Configurar mocks
        BufferedImage testImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
        when(deteccionService.findLastId()).thenReturn(1);

        // Simulación del entorno y sesión ONNX
        OrtEnvironment mockEnv = mock(OrtEnvironment.class);
        OrtSession mockSession = mock(OrtSession.class);
        when(mockEnv.createSession(anyString(), any())).thenReturn(mockSession);

        // Mock de salida del modelo ONNX
        float[][][] mockOutputData = {{{0.5f, 0.5f, 0.3f, 0.3f, 0.8f}}}; // Datos de ejemplo con una detección de confianza 0.8
        OnnxTensor mockTensor = mock(OnnxTensor.class);
        when(mockTensor.getValue()).thenReturn(mockOutputData);

        OnnxTensorLike mockTensorLike = mock(OnnxTensorLike.class);
        OrtSession.Result mockResult = mock(OrtSession.Result.class);
        when(mockResult.get(anyString())).thenReturn(java.util.Optional.of(mockTensorLike));
        when(mockSession.run(any())).thenReturn(mockResult);

        // Ejecutar el método
        List<ObjectDetectionResult> results = detectionService.performMontacargaDetection(mockMontacargasImageFile, 1L);

        // Verificar resultados
        assertNotNull(results);
        assertEquals(2, results.size());  // Verificar que haya una detección en los resultados
        assertEquals("Montacargas", results.get(0).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertEquals("Montacargas", results.get(1).getLabel()); // Verificar que la etiqueta sea "Cono"

        assertTrue(results.get(0).getConfidence() > 0.8f, "La confianza de la detección debe ser mayor que 0.8");
        assertTrue(results.get(1).getConfidence() > 0.8f, "La confianza de la detección debe ser mayor que 0.8");
        // Verificar interacciones
        verify(deteccionService, times(2)).findLastId();
    }
    @Test
    void testPerformObreroDetection() throws IOException, OrtException, URISyntaxException {
        // Configurar mocks
        BufferedImage testImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
        when(deteccionService.findLastId()).thenReturn(1);

        // Simulación del entorno y sesión ONNX
        OrtEnvironment mockEnv = mock(OrtEnvironment.class);
        OrtSession mockSession = mock(OrtSession.class);
        when(mockEnv.createSession(anyString(), any())).thenReturn(mockSession);

        // Mock de salida del modelo ONNX
        float[][][] mockOutputData = {{{0.5f, 0.5f, 0.3f, 0.3f, 0.8f}}}; // Datos de ejemplo con una detección de confianza 0.8
        OnnxTensor mockTensor = mock(OnnxTensor.class);
        when(mockTensor.getValue()).thenReturn(mockOutputData);

        OnnxTensorLike mockTensorLike = mock(OnnxTensorLike.class);
        OrtSession.Result mockResult = mock(OrtSession.Result.class);
        when(mockResult.get(anyString())).thenReturn(java.util.Optional.of(mockTensorLike));
        when(mockSession.run(any())).thenReturn(mockResult);

        // Ejecutar el método
        List<ObjectDetectionResult> results = detectionService.performPersonaDetection(mockObreroImageFile, 1L);

        // Verificar resultados
        assertNotNull(results);
        assertEquals(1, results.size());  // Verificar que haya una detección en los resultados
        assertEquals("Obrero", results.get(0).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertTrue(results.get(0).getConfidence() > 0.8f, "La confianza de la detección debe ser mayor que 0.8");
        // Verificar interacciones
        verify(deteccionService, times(1)).findLastId();
    }
    @Test
    void testPerformPalletDetection() throws IOException, OrtException, URISyntaxException, NoSuchFieldException, IllegalAccessException {
        Field minConfigField = ObjectDetectionService.class.getDeclaredField("minConfig");
        minConfigField.setAccessible(true);
        minConfigField.set(detectionService, 0.6);
        // Configurar mocks
        BufferedImage testImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
        when(deteccionService.findLastId()).thenReturn(1);

        // Simulación del entorno y sesión ONNX
        OrtEnvironment mockEnv = mock(OrtEnvironment.class);
        OrtSession mockSession = mock(OrtSession.class);
        when(mockEnv.createSession(anyString(), any())).thenReturn(mockSession);

        // Mock de salida del modelo ONNX
        float[][][] mockOutputData = {{{0.5f, 0.5f, 0.3f, 0.3f, 0.8f}}}; // Datos de ejemplo con una detección de confianza 0.8
        OnnxTensor mockTensor = mock(OnnxTensor.class);
        when(mockTensor.getValue()).thenReturn(mockOutputData);

        OnnxTensorLike mockTensorLike = mock(OnnxTensorLike.class);
        OrtSession.Result mockResult = mock(OrtSession.Result.class);
        when(mockResult.get(anyString())).thenReturn(java.util.Optional.of(mockTensorLike));
        when(mockSession.run(any())).thenReturn(mockResult);

        // Ejecutar el método
        List<ObjectDetectionResult> results = detectionService.performPalletDetection(mockPalletImageFile, 1L);

        // Verificar resultados
        assertNotNull(results);
        assertEquals(1, results.size());  // Verificar que haya una detección en los resultados
        assertEquals("Pallet", results.get(0).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertTrue(results.get(0).getConfidence() > 0.6f, "La confianza de la detección debe ser mayor que 0.6");
        // Verificar interacciones
        verify(deteccionService, times(1)).findLastId();
    }
    @Test
    void testPerformEstacionDetection() throws  OrtException {

        // Configurar mocks
        BufferedImage testImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
        when(deteccionService.findLastId()).thenReturn(1);

        // Simulación del entorno y sesión ONNX
        OrtEnvironment mockEnv = mock(OrtEnvironment.class);
        OrtSession mockSession = mock(OrtSession.class);
        when(mockEnv.createSession(anyString(), any())).thenReturn(mockSession);

        // Mock de salida del modelo ONNX
        float[][][] mockOutputData = {{{0.5f, 0.5f, 0.3f, 0.3f, 0.8f}}}; // Datos de ejemplo con una detección de confianza 0.8
        OnnxTensor mockTensor = mock(OnnxTensor.class);
        when(mockTensor.getValue()).thenReturn(mockOutputData);

        OnnxTensorLike mockTensorLike = mock(OnnxTensorLike.class);
        OrtSession.Result mockResult = mock(OrtSession.Result.class);
        when(mockResult.get(anyString())).thenReturn(java.util.Optional.of(mockTensorLike));
        when(mockSession.run(any())).thenReturn(mockResult);

        // Ejecutar el método
        List<ObjectDetectionResult> results = detectionService.performEstacionDetection(mockEstacionImageFile, 1L);

        // Verificar resultados
        assertNotNull(results);
        assertEquals(6, results.size());  // Verificar que haya una detección en los resultados
        assertEquals("Conexion", results.get(0).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertEquals("Conexion", results.get(1).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertEquals("Conexion", results.get(2).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertEquals("Tanque", results.get(3).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertEquals("Radiador", results.get(4).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertEquals("Estacion", results.get(5).getLabel()); // Verificar que la etiqueta sea "Cono"
        assertTrue(results.get(0).getConfidence() > 0.3f, "La confianza de la detección debe ser mayor que 0.6");
        assertTrue(results.get(1).getConfidence() > 0.3f, "La confianza de la detección debe ser mayor que 0.6");
        assertTrue(results.get(2).getConfidence() > 0.3f, "La confianza de la detección debe ser mayor que 0.6");
        assertTrue(results.get(3).getConfidence() > 0.3f, "La confianza de la detección debe ser mayor que 0.6");
        assertTrue(results.get(4).getConfidence() > 0.3f, "La confianza de la detección debe ser mayor que 0.6");
        assertTrue(results.get(5).getConfidence() > 0.3f, "La confianza de la detección debe ser mayor que 0.6");
        // Verificar interacciones
        verify(deteccionService, times(6)).findLastId();
    }
}
