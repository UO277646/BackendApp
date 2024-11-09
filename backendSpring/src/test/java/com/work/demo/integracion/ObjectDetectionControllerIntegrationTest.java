package com.work.demo.integracion;


import com.work.demo.rest.ObjectDetectionController;
import com.work.demo.service.ObjectDetectionService;
import com.work.demo.service.DeteccionService;
import com.work.demo.service.dto.ObjetoImagen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ObjectDetectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectDetectionService objectDetectionService;

    @Autowired
    private DeteccionService deteccionService;

    @BeforeEach
    void setUp() {
        // Configura el comportamiento del servicio de detección

    }

    @Test
    public void testDetectObjectsEndpoint() throws Exception {
        // Cargar la imagen cono.jpg desde resources
        ClassPathResource imgFile = new ClassPathResource("imagenes/cono.jpg");
        MockMultipartFile mockImageFile = new MockMultipartFile(
                "image",
                "cono.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                imgFile.getInputStream()
        );

        Long proyectId = 1L;  // ID del proyecto a usar en la prueba

        mockMvc.perform(MockMvcRequestBuilders.multipart("/detect")
                        .file(mockImageFile)
                        .param("proyectId", String.valueOf(proyectId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objetos", is(not(empty()))));  // Verifica que la lista de objetos no esté vacía
    }
}
