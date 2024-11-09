package com.work.demo.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.demo.rest.dto.ProyectoApiDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProyectoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetProyectosByEmail() throws Exception {
        mockMvc.perform(get("/proyectos/find/proyectos/juaneselmejor47@gmail.com/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetProyectDetections() throws Exception {
        Long idProyecto = 1L; // Cambia este valor según los datos de prueba

        mockMvc.perform(get("/proyectos/find/detecciones/" + idProyecto)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].fechaCreacion").exists())
                .andExpect(jsonPath("$[0].cantidad").exists());
    }

    @Test
    public void testGetProyectRestrictions() throws Exception {
        Long idProyecto = 1L; // Cambia este valor según los datos de prueba

        mockMvc.perform(get("/proyectos/find/restricciones/" + idProyecto)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].idRestriccion").exists())
                .andExpect(jsonPath("$[0].objeto").exists());
    }

    @Test
    public void testCreateProyecto() throws Exception {
        ProyectoApiDto proyectoApiDto = new ProyectoApiDto();
        proyectoApiDto.setNombre("Nuevo Proyecto");
        proyectoApiDto.setUser("juaneselmejor47@gmail.com");
        proyectoApiDto.setMinConf(0.8);

        String proyectoJson = objectMapper.writeValueAsString(proyectoApiDto);

        mockMvc.perform(post("/proyectos/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proyectoJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nombre").value("Nuevo Proyecto"));

    }

    @Test
    public void testUpdateProyecto() throws Exception {
        Long idProyecto = 1L; // Cambia este valor según los datos de prueba

        ProyectoApiDto proyectoApiDto = new ProyectoApiDto();
        proyectoApiDto.setNombre("Definitivo");
        proyectoApiDto.setUser("testUser@example.com");
        proyectoApiDto.setMinConf(0.85);

        String proyectoJson = objectMapper.writeValueAsString(proyectoApiDto);

        mockMvc.perform(put("/proyectos/update/" + idProyecto)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proyectoJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nombre").value("Definitivo"));

    }

    @Test
    public void testDeleteProyecto() throws Exception {
        Long idProyecto = 17L; // Cambia este valor según los datos de prueba

        mockMvc.perform(delete("/proyectos/delete/" + idProyecto))
                .andExpect(status().isOk());
    }
}