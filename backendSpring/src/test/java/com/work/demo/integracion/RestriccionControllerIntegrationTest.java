package com.work.demo.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.demo.rest.dto.RestriccionApiDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.time.LocalDate;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RestriccionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllRestricciones() throws Exception {
        mockMvc.perform(get("/restricciones/find/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetRestriccionById() throws Exception {
        Long id = 5L; // Adjust according to your test data

        mockMvc.perform(get("/restricciones/find/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idRestriccion").exists())
                .andExpect(jsonPath("$.objeto").exists());
    }

    @Test
    public void testCreateRestriccion() throws Exception {
        RestriccionApiDto restriccionApiDto = RestriccionApiDto.builder()
                .objeto("Test Object")
                .fechaDesde(Date.valueOf(LocalDate.now()))
                .fechaHasta(Date.valueOf(LocalDate.now().plusDays(1)))
                .cantidadMin(1)
                .cantidadMax(10)
                .proyectoId(1L)
                .diaria(true)
                .build();

        String restriccionJson = objectMapper.writeValueAsString(restriccionApiDto);

        mockMvc.perform(post("/restricciones/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restriccionJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.objeto").value("Test Object"));
    }

    @Test
    public void testUpdateRestriccion() throws Exception {
        Long id = 15L; // Adjust according to your test data

        RestriccionApiDto restriccionApiDto = RestriccionApiDto.builder()
                .objeto("Updated Object")
                .fechaDesde(Date.valueOf(LocalDate.now()))
                .fechaHasta(Date.valueOf(LocalDate.now().plusDays(1)))
                .cantidadMin(2)
                .cantidadMax(20)
                .proyectoId(1L)
                .diaria(true)
                .build();

        String restriccionJson = objectMapper.writeValueAsString(restriccionApiDto);

        mockMvc.perform(put("/restricciones/update/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restriccionJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.objeto").value("Updated Object"));
    }

    @Test
    public void testDeleteRestriccion() throws Exception {
        Long id = 15L; // Adjust according to your test data
        mockMvc.perform(delete("/restricciones/delete/" + id))
                .andExpect(status().isOk());
    }
}
