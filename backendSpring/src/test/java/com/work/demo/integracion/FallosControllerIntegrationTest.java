package com.work.demo.integracion;

import com.work.demo.service.FallosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FallosControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FallosService fallosService;

    @BeforeEach
    void setUp() {

    }

    @Test
    public void testGetFallosByRestriccion() throws Exception {
        Long idRec = 14L;  // ID de restricción para la prueba

        mockMvc.perform(get("/fallos/find/fallos/{idRec}", idRec)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

    }

    @Test
    public void testDeleteFallo() throws Exception {
        Long falloId = 59L;  // ID del fallo a eliminar

        mockMvc.perform(delete("/fallos/delete/{falloId}", falloId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));
    }
}
