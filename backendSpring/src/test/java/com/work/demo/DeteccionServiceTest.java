package com.work.demo;
import com.work.demo.repository.*;
import com.work.demo.service.DeteccionService;
import com.work.demo.service.ProyectoService;

import com.work.demo.service.dto.DeteccionServiceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class DeteccionServiceTest {

    @InjectMocks
    private DeteccionService deteccionService;

    @Mock
    private DeteccionRepository deteccionRepository;

    @Mock
    private ProyectoService proyectoService;

    private Deteccion deteccion;
    private DeteccionServiceDto deteccionDto;
    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configuración inicial para las pruebas
        proyecto = new Proyecto();
        proyecto.setIdProyecto(1L);

        deteccion = new Deteccion();
        deteccion.setDeteccionId(1L);
        deteccion.setProyecto(proyecto);
        deteccion.setFotoId(Date.valueOf(LocalDate.now()));
        deteccion.setObjeto("Objeto de prueba");
        deteccion.setX(0.1);
        deteccion.setY(0.2);
        deteccion.setWeight(0.3);
        deteccion.setHeight(0.4);
        deteccion.setConfidence(0.9);

        deteccionDto = DeteccionServiceDto.builder()
                .deteccionId(1L)
                .proyectoId(1L)
                .fotoId(Date.valueOf(LocalDate.now()))
                .objeto("Objeto de prueba")
                .x(0.1)
                .y(0.2)
                .weight(0.3)
                .height(0.4)
                .confidence(0.9)
                .build();
    }

    @Test
    void testObtenerTodasDetecciones() {
        List<Deteccion> detecciones = new ArrayList<>();
        detecciones.add(deteccion);
        when(deteccionRepository.findAll()).thenReturn(detecciones);

        List<DeteccionServiceDto> result = deteccionService.obtenerTodasDetecciones();

        assertEquals(1, result.size());
        assertEquals(deteccion.getObjeto(), result.get(0).getObjeto());
        verify(deteccionRepository, times(1)).findAll();
    }

    @Test
    void testObtenerDeteccionPorId() {
        when(deteccionRepository.findById(1L)).thenReturn(Optional.of(deteccion));

        DeteccionServiceDto result = deteccionService.obtenerDeteccionPorId(1L);

        assertNotNull(result);
        assertEquals(deteccion.getObjeto(), result.getObjeto());
        verify(deteccionRepository, times(1)).findById(1L);
    }

    @Test
    void testCrearDeteccion() {
        when(proyectoService.obtenerProyectoPorIdEntidad(anyLong())).thenReturn(proyecto);
        when(deteccionRepository.save(any(Deteccion.class))).thenReturn(deteccion);

        DeteccionServiceDto result = deteccionService.crearDeteccion(deteccionDto);

        assertNotNull(result);
        assertEquals(deteccion.getObjeto(), result.getObjeto());
        verify(deteccionRepository, times(1)).save(any(Deteccion.class));
    }

    @Test
    void testActualizarDeteccion() {
        when(proyectoService.obtenerProyectoPorIdEntidad(anyLong())).thenReturn(proyecto);
        when(deteccionRepository.findById(1L)).thenReturn(Optional.of(deteccion));
        when(deteccionRepository.save(any(Deteccion.class))).thenReturn(deteccion);

        DeteccionServiceDto result = deteccionService.actualizarDeteccion(1L, deteccionDto);

        assertNotNull(result);
        assertEquals(deteccion.getObjeto(), result.getObjeto());
        verify(deteccionRepository, times(1)).findById(1L);
        verify(deteccionRepository, times(1)).save(any(Deteccion.class));
    }

    @Test
    void testEliminarDeteccion() {
        when(deteccionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(deteccionRepository).deleteById(1L);

        deteccionService.eliminarDeteccion(1L);

        verify(deteccionRepository, times(1)).existsById(1L);
        verify(deteccionRepository, times(1)).deleteById(1L);
    }
}
