package com.work.demo;
import com.work.demo.exceptions.InvalidParameterException;
import com.work.demo.repository.*;
import com.work.demo.service.ProyectoService;
import com.work.demo.service.RestriccionService;
import com.work.demo.service.UsuarioService;
import com.work.demo.service.dto.ProyectoServiceDto;
import com.work.demo.service.dto.RestriccionServiceDto;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestriccionServiceTest {

    @Mock
    private RestriccionRepository restriccionRepository;

    @Mock
    private ProyectoService proyectoService;

    @InjectMocks
    private RestriccionService restriccionService;

    private Restriccion restriccion;
    private RestriccionServiceDto restriccionDto;
    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        proyecto = new Proyecto();
        proyecto.setIdProyecto(1L);

        restriccion = new Restriccion();
        restriccion.setIdRestriccion(1L);
        restriccion.setProyecto(proyecto);
        restriccion.setObjeto("objeto");
        restriccion.setFechaDesde(Date.valueOf(LocalDate.now()));
        restriccion.setFechaHasta(Date.valueOf(LocalDate.now()));
        restriccion.setCantidadMin(1);
        restriccion.setCantidadMax(5);
        restriccion.setCumplida(false);

        restriccionDto = RestriccionServiceDto.builder()
                .idRestriccion(1L)
                .proyectoId(1L)
                .objeto("objeto")
                .fechaDesde(Date.valueOf(LocalDate.now()))
                .fechaHasta(Date.valueOf(LocalDate.now()))
                .cantidadMin(1)
                .cantidadMax(5)
                .cumplida(false)
                .build();
    }

    @Test
    void findAll_ReturnsRestricciones() {
        // Given
        when(restriccionRepository.findAll()).thenReturn(Arrays.asList(restriccion));

        // When
        List<RestriccionServiceDto> result = restriccionService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(restriccionRepository, times(1)).findAll();
    }

    @Test
    void obtenerRestriccionPorId_ReturnsRestriccion() {
        // Given
        when(restriccionRepository.findById(1L)).thenReturn(Optional.of(restriccion));

        // When
        RestriccionServiceDto result = restriccionService.obtenerRestriccionPorId(1L);

        // Then
        assertNotNull(result);
        assertEquals(restriccion.getIdRestriccion(), result.getIdRestriccion());
        verify(restriccionRepository, times(1)).findById(1L);
    }
    @Test
    void obtenerRestriccionNull() {
        // Given
        when(restriccionRepository.findById(1L)).thenReturn(Optional.of(restriccion));

        // When
        assertThrows(InvalidParameterException.class, () -> {
            restriccionService.obtenerRestriccionPorId(null);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido

    }
    @Test
    void crearRestriccion_ReturnsCreatedRestriccion() {
        // Given
        when(proyectoService.obtenerProyectoPorIdEntidad(1L)).thenReturn(proyecto);
        when(restriccionRepository.save(any(Restriccion.class))).thenReturn(restriccion);

        // When
        RestriccionServiceDto result = restriccionService.crearRestriccion(restriccionDto);

        // Then
        assertNotNull(result);
        assertEquals(restriccion.getIdRestriccion(), result.getIdRestriccion());
        verify(restriccionRepository, times(1)).save(any(Restriccion.class));
    }
    @Test
    void crearRestriccionNullDto() {
        // Given
        RestriccionServiceDto restriccionDto = null;

        // When & Then
        assertThrows(InvalidParameterException.class, () ->
                        restriccionService.crearRestriccion(restriccionDto),
                "Debería lanzar InvalidParameterException cuando restriccionDto es null");
    }

    @Test
    void crearRestriccionCantidadMinNegativa() {
        // Given
        RestriccionServiceDto restriccionDto = new RestriccionServiceDto();
        restriccionDto.setCantidadMin(-1); // Valor negativo
        restriccionDto.setCantidadMax(10);

        // When & Then
        assertThrows(InvalidParameterException.class, () ->
                        restriccionService.crearRestriccion(restriccionDto),
                "Debería lanzar InvalidParameterException cuando cantidadMin es negativo");
    }

    @Test
    void crearRestriccionCantidadMaxMenorQueCantidadMin() {
        // Given
        RestriccionServiceDto restriccionDto = new RestriccionServiceDto();
        restriccionDto.setCantidadMin(5);
        restriccionDto.setCantidadMax(3); // cantidadMax menor que cantidadMin

        // When & Then
        assertThrows(InvalidParameterException.class, () ->
                        restriccionService.crearRestriccion(restriccionDto),
                "Debería lanzar InvalidParameterException cuando cantidadMax es menor que cantidadMin");
    }
    @Test
    void actualizarRestriccionReturnsUpdatedRestriccion() {
        // Given
        when(restriccionRepository.findById(1L)).thenReturn(Optional.of(restriccion));
        when(restriccionRepository.save(any(Restriccion.class))).thenReturn(restriccion);

        // When
        RestriccionServiceDto result = restriccionService.actualizarRestriccion(1L, restriccionDto);

        // Then
        assertNotNull(result);
        assertEquals(restriccion.getIdRestriccion(), result.getIdRestriccion());
        verify(restriccionRepository, times(1)).save(any(Restriccion.class));
    }

    @Test
    void eliminarRestriccionSetsBorradoToTrue() {
        // Given
        when(restriccionRepository.findById(1L)).thenReturn(Optional.of(restriccion));
        when(restriccionRepository.existsById(1L)).thenReturn(true);

        // When
        restriccionService.eliminarRestriccion(1L);

        // Then
        assertTrue(restriccion.isBorrado());
        verify(restriccionRepository, times(1)).save(any(Restriccion.class));
    }
    @Test
    void actualizarRestriccionNullDto() {
        // Given
        RestriccionServiceDto restriccionActualizadaDto = null;

        // When & Then
        assertThrows(InvalidParameterException.class, () ->
                        restriccionService.actualizarRestriccion(1L, restriccionActualizadaDto),
                "Debería lanzar InvalidParameterException cuando restriccionActualizadaDto es null");
    }

    @Test
    void actualizarRestriccionCantidadMinNegativa() {
        // Given
        RestriccionServiceDto restriccionActualizadaDto = new RestriccionServiceDto();
        restriccionActualizadaDto.setCantidadMin(-1); // Valor negativo
        restriccionActualizadaDto.setCantidadMax(10);

        // When & Then
        assertThrows(InvalidParameterException.class, () ->
                        restriccionService.actualizarRestriccion(1L, restriccionActualizadaDto),
                "Debería lanzar InvalidParameterException cuando cantidadMin es negativo");
    }

    @Test
    void actualizarRestriccionCantidadMaxMenorQueCantidadMin() {
        // Given
        RestriccionServiceDto restriccionActualizadaDto = new RestriccionServiceDto();
        restriccionActualizadaDto.setCantidadMin(5);
        restriccionActualizadaDto.setCantidadMax(3); // cantidadMax menor que cantidadMin

        // When & Then
        assertThrows(InvalidParameterException.class, () ->
                        restriccionService.actualizarRestriccion(1L, restriccionActualizadaDto),
                "Debería lanzar InvalidParameterException cuando cantidadMax es menor que cantidadMin");
    }
}
