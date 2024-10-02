package com.work.demo.service;

import com.work.demo.repository.DeteccionRepository;
import com.work.demo.repository.Proyecto;
import com.work.demo.repository.ProyectoRepository;
import com.work.demo.repository.RestriccionRepository;
import com.work.demo.service.dto.FotoServiceDto;
import com.work.demo.service.dto.ProyectoServiceDto;
import com.work.demo.service.dto.RestriccionServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private DeteccionRepository deteccionRepository;
    @Autowired
    private RestriccionRepository restriccionRepository;

    // Método para convertir de Proyecto a ProyectoServiceDto
    private ProyectoServiceDto convertirAProyectoDto(Proyecto proyecto) {
        ProyectoServiceDto dto = new ProyectoServiceDto();
        dto.setIdProyecto(proyecto.getIdProyecto());
        dto.setNombre(proyecto.getNombre());
        dto.setFechaCreacion(proyecto.getFechaCreacion());
        return dto;
    }

    // Método para obtener todos los proyectos y convertirlos a DTOs
    public List<ProyectoServiceDto> obtenerTodosProyectos() {
        try {
            List<Proyecto> proyectos = proyectoRepository.findAll();
            return proyectos.stream()
                    .map(this::convertirAProyectoDto) // Convierte cada entidad en un DTO
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la lista de proyectos", e);
        }
    }

    // Método findAll que retorna todos los proyectos
    public List<ProyectoServiceDto> findAll() {
        try {
            List<Proyecto> proyectos = proyectoRepository.findAll();
            return proyectos.stream()
                    .map(this::convertirAProyectoDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener todos los proyectos", e);
        }
    }

    // Método para obtener un proyecto por su ID y devolver un DTO
    public ProyectoServiceDto obtenerProyectoPorId(Long id_proyecto) {
        try {
            Optional<Proyecto> proyecto = proyectoRepository.findById(id_proyecto);
            return proyecto.map(this::convertirAProyectoDto)
                    .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + id_proyecto));
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el proyecto con ID: " + id_proyecto, e);
        }
    }
    public Proyecto obtenerProyectoPorIdEntidad(Long id_proyecto) {
        try {

            return  proyectoRepository.findById(id_proyecto).get();
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el proyecto con ID: " + id_proyecto, e);
        }
    }

    // Método para crear un nuevo proyecto y devolver un DTO
    public ProyectoServiceDto crearProyecto(ProyectoServiceDto proyectoDto) {
        try {
            Proyecto nuevoProyecto = new Proyecto();
            nuevoProyecto.setNombre(proyectoDto.getNombre());
            nuevoProyecto.setFechaCreacion(Date.valueOf(LocalDate.now()));
            Proyecto proyectoGuardado = proyectoRepository.save(nuevoProyecto);
            return convertirAProyectoDto(proyectoGuardado);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear el proyecto", e);
        }
    }

    // Método para actualizar un proyecto existente y devolver un DTO
    public ProyectoServiceDto actualizarProyecto(Long id_proyecto, ProyectoServiceDto proyectoActualizadoDto) {
        try {
            Proyecto proyectoExistente = proyectoRepository.findById(id_proyecto)
                    .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + id_proyecto));

            proyectoExistente.setNombre(proyectoActualizadoDto.getNombre());
            proyectoExistente.setFechaCreacion(proyectoActualizadoDto.getFechaCreacion());
            Proyecto proyectoActualizado = proyectoRepository.save(proyectoExistente);
            return convertirAProyectoDto(proyectoActualizado);
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar el proyecto con ID: " + id_proyecto, e);
        }
    }

    // Método para eliminar un proyecto por su ID
    public void eliminarProyecto(Long id_proyecto) {
        try {
            if (proyectoRepository.existsById(id_proyecto)) {
                proyectoRepository.deleteById(id_proyecto);
            } else {
                throw new RuntimeException("No se puede eliminar, proyecto no encontrado con ID: " + id_proyecto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el proyecto con ID: " + id_proyecto, e);
        }
    }

    public List<RestriccionServiceDto> findRestrict (Long id) {
        return restriccionRepository.findRestrictionsByProject(id);
    }

    public List<FotoServiceDto> findDetect (Long id) {
        return deteccionRepository.findDetectionsByProjectGroupedByDate(id);
    }
}

