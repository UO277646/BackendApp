package com.work.demo;

import com.work.demo.exceptions.InvalidParameterException;
import com.work.demo.repository.*;
import com.work.demo.service.ProyectoService;
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
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ProyectosTest {
    @InjectMocks
    private ProyectoService proyectoService;

    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private DeteccionRepository deteccionRepository;

    @Mock
    private RestriccionRepository restriccionRepository;
    @Mock
    private FallosRepository fallosRepository;
    @Mock
    private UsuarioService usuarioService;

    private Proyecto proyecto;
    private ProyectoServiceDto proyectoDto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configuración inicial para las pruebas
        usuario = new Usuario();
        usuario.setUserId(1L);

        proyecto = new Proyecto();
        proyecto.setIdProyecto(1L);
        proyecto.setNombre("Proyecto de prueba");
        proyecto.setFechaCreacion(Date.valueOf(LocalDate.now()));
        proyecto.setMinConf(0.5);
        proyecto.setUsuario(usuario);

        proyectoDto = new ProyectoServiceDto();
        proyectoDto.setIdProyecto(1L);
        proyectoDto.setNombre("Proyecto de prueba");
        proyectoDto.setFechaCreacion(Date.valueOf(LocalDate.now()));
        proyectoDto.setMinConf(0.5);
    }

    @Test
    void testObtenerTodosProyectos() {
        List<Proyecto> proyectos = new ArrayList<>();
        proyectos.add(proyecto);
        when(proyectoRepository.findAll()).thenReturn(proyectos);

        List<ProyectoServiceDto> result = proyectoService.obtenerTodosProyectos();

        assertEquals(1, result.size());
        assertEquals(proyecto.getNombre(), result.get(0).getNombre());
        verify(proyectoRepository, times(1)).findAll();
    }

    @Test
    void testObtenerProyectoPorId() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        ProyectoServiceDto result = proyectoService.obtenerProyectoPorId(1L);

        assertNotNull(result);
        assertEquals(proyecto.getNombre(), result.getNombre());
        verify(proyectoRepository, times(1)).findById(1L);
    }

    @Test
    void testCrearProyecto() {
        when(usuarioService.findByEmail(any())).thenReturn(usuario);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        ProyectoServiceDto result = proyectoService.crearProyecto(proyectoDto);

        assertNotNull(result);
        assertEquals(proyecto.getNombre(), result.getNombre());
        verify(proyectoRepository, times(1)).save(any(Proyecto.class));
    }
    @Test
    void testCrearProyectoNombreNull() {
        // Configura el proyectoDto con nombre null
        proyectoDto.setNombre(null);
//        when(usuarioService.findByEmail(any())).thenReturn(usuario);

        // Verifica que se lanza RuntimeException cuando el nombre es null
        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.crearProyecto(proyectoDto);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido
        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }
    @Test
    void testCrearProyectoConfianzaNegative() {
        // Configura el proyectoDto con nombre null
        proyectoDto.setMinConf(-1);
//        when(usuarioService.findByEmail(any())).thenReturn(usuario);

        // Verifica que se lanza RuntimeException cuando el nombre es null
        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.crearProyecto(proyectoDto);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido
        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }
    @Test
    void testCrearProyectoConfianzaMayorUno() {
        // Configura el proyectoDto con nombre null
        proyectoDto.setMinConf(2);
//        when(usuarioService.findByEmail(any())).thenReturn(usuario);

        // Verifica que se lanza RuntimeException cuando el nombre es null
        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.crearProyecto(proyectoDto);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido
        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }

    @Test
    void testActualizarProyecto() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        ProyectoServiceDto result = proyectoService.actualizarProyecto(1L, proyectoDto);

        assertNotNull(result);
        assertEquals(proyecto.getNombre(), result.getNombre());
        verify(proyectoRepository, times(1)).findById(1L);
        verify(proyectoRepository, times(1)).save(any(Proyecto.class));
    }
    @Test
    void testActualizarProyectoConfianzaMayorUno() {
        // Configura el proyectoDto con nombre null
        proyectoDto.setMinConf(2);
//        when(usuarioService.findByEmail(any())).thenReturn(usuario);

        // Verifica que se lanza RuntimeException cuando el nombre es null
        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.actualizarProyecto(1L,proyectoDto);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido
        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }
    @Test
    void testActualizarProyectoConfianzaMenorCero() {
        // Configura el proyectoDto con nombre null
        proyectoDto.setMinConf(-1);
//        when(usuarioService.findByEmail(any())).thenReturn(usuario);

        // Verifica que se lanza RuntimeException cuando el nombre es null
        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.actualizarProyecto(1L,proyectoDto);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido
        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }
    @Test
    void testActualizarProyectoNombreNull() {
        // Configura el proyectoDto con nombre null
        proyectoDto.setNombre(null);
//        when(usuarioService.findByEmail(any())).thenReturn(usuario);

        // Verifica que se lanza RuntimeException cuando el nombre es null
        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.actualizarProyecto(1L,proyectoDto);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido
        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }
    @Test
    void testActualizarProyectoIdNull() {
        // Configura el proyectoDto con nombre null
        proyectoDto.setNombre(null);
//        when(usuarioService.findByEmail(any())).thenReturn(usuario);

        // Verifica que se lanza RuntimeException cuando el nombre es null
        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.actualizarProyecto(null,proyectoDto);
        });



    }
    @Test
    void testEliminarProyecto() {
        when(proyectoRepository.existsById(1L)).thenReturn(true);
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        proyectoService.eliminarProyecto(1L);

        verify(proyectoRepository, times(1)).existsById(1L);
        verify(proyectoRepository, times(1)).findById(1L);
        verify(proyectoRepository, times(1)).save(any(Proyecto.class));
    }
    @Test
    void testEliminarProyectoNulo() {
        when(proyectoRepository.existsById(1L)).thenReturn(true);
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.eliminarProyecto(null);
        });

        // Verifica que no se llama al método save del repositorio, porque el proyecto es inválido
        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }
    @Test
    void testFindRestrict() {
        Restriccion restriccion = new Restriccion();
        restriccion.setFechaDesde(Date.valueOf(LocalDate.now().minusDays(5)));
        restriccion.setFechaHasta(Date.valueOf(LocalDate.now().minusDays(1)));
        restriccion.setObjeto("Objeto de prueba");
        restriccion.setCantidadMin(1);
        restriccion.setCantidadMax(5);
        restriccion.setDiaria(false);
        restriccion.setProyecto(proyecto);
        List<Restriccion> restricciones = List.of(restriccion);

        Deteccion deteccion = new Deteccion();
        deteccion.setObjeto("Objeto de prueba");
        deteccion.setFotoId(Date.valueOf(LocalDate.now().minusDays(3)));
        List<Deteccion> detecciones = List.of(deteccion);

        when(restriccionRepository.findRestrictionsByProjectEntity(anyLong())).thenReturn(restricciones);
        when(deteccionRepository.findDetectionsByProject(anyLong())).thenReturn(detecciones);

        List<RestriccionServiceDto> result = proyectoService.findRestrict(1L);

        assertNotNull(result);
        verify(restriccionRepository, times(1)).findRestrictionsByProjectEntity(anyLong());
        verify(deteccionRepository, times(1)).findDetectionsByProject(anyLong());
    }

    @Test
    void testFindByEmail() {
        when(usuarioService.findOrCreateUser(anyString(), anyString())).thenReturn(1L);
        when(proyectoRepository.findByUsuarioUserIdAndBorradoFalse(anyLong())).thenReturn(List.of(proyecto));

        List<ProyectoServiceDto> result = proyectoService.findByEmail("email@example.com", "User Name");

        assertEquals(1, result.size());
        verify(proyectoRepository, times(1)).findByUsuarioUserIdAndBorradoFalse(anyLong());
    }
    @Test
    void testFindByEmailNull() {
        assertThrows(InvalidParameterException.class, () -> {
            List<ProyectoServiceDto> result = proyectoService.findByEmail(null, "User Name");
        });
    }
    @Test
    void testCheckProject() {
        when(usuarioService.findByEmail(anyString())).thenReturn(usuario);
        when(proyectoRepository.findById(anyLong())).thenReturn(Optional.of(proyecto));

        Boolean result = proyectoService.checkProject(1L, "email@example.com");

        assertTrue(result);
        verify(proyectoRepository, times(1)).findById(anyLong());
    }
    @Test
    void testCheckProjectWrongUser() {

        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.checkProject(1L, "em@example.com");
        });

    }
    @Test
    void testCheckProjectNullUser() {

        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.checkProject(1L, null);
        });

    }
    @Test
    void testCheckProjectEmptyUser() {

        assertThrows(InvalidParameterException.class, () -> {
            proyectoService.checkProject(1L, "");
        });

    }
}
