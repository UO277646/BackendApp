package com.work.demo.service;

import com.work.demo.repository.Deteccion;
import com.work.demo.repository.DeteccionRepository;
import com.work.demo.repository.Proyecto;
import com.work.demo.service.dto.DeteccionServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeteccionService {
    @Autowired
    private ProyectoService proyectoService;
    @Autowired
    private DeteccionRepository deteccionRepository;

    // Método para convertir de Deteccion a DeteccionServiceDto
    private DeteccionServiceDto convertirADeteccionDto(Deteccion deteccion) {
        return DeteccionServiceDto.builder()
                .deteccionId(deteccion.getDeteccionId())
                .proyectoId(deteccion.getProyecto().getIdProyecto())
                .fotoId(deteccion.getFotoId())
                .objeto(deteccion.getObjeto())
                .x(deteccion.getX())
                .y(deteccion.getY())
                .weight(deteccion.getWeight())
                .height(deteccion.getHeight())
                .confidence(deteccion.getConfidence())
                .build();
    }

    // Método para obtener todas las detecciones y convertirlas a DTOs
    public List<DeteccionServiceDto> obtenerTodasDetecciones() {
        try {
            List<Deteccion> detecciones = deteccionRepository.findAll();
            return detecciones.stream()
                    .map(this::convertirADeteccionDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la lista de detecciones", e);
        }
    }

    // Método findAll que retorna todas las detecciones
    public List<DeteccionServiceDto> findAll() {
        try {
            List<Deteccion> detecciones = deteccionRepository.findAll();
            return detecciones.stream()
                    .map(this::convertirADeteccionDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener todas las detecciones", e);
        }
    }

    // Método para obtener una detección por su ID y devolver un DTO
    public DeteccionServiceDto obtenerDeteccionPorId(Long deteccionId) {
        try {
            Optional<Deteccion> deteccion = deteccionRepository.findById(deteccionId);
            return deteccion.map(this::convertirADeteccionDto)
                    .orElseThrow(() -> new RuntimeException("Detección no encontrada con ID: " + deteccionId));
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la detección con ID: " + deteccionId, e);
        }
    }

    // Método para crear una nueva detección y devolver un DTO
    public DeteccionServiceDto crearDeteccion(DeteccionServiceDto deteccionDto) {
        try {
            Proyecto p=proyectoService.obtenerProyectoPorIdEntidad(deteccionDto.getProyectoId());
            Deteccion nuevaDeteccion = Deteccion.builder()
                    .proyecto(p)
                    .fotoId(new Date(System.currentTimeMillis()))
                    .objeto(deteccionDto.getObjeto())
                    .x(deteccionDto.getX())
                    .y(deteccionDto.getY())
                    .weight(deteccionDto.getWeight())
                    .height(deteccionDto.getHeight())
                    .confidence(deteccionDto.getConfidence())
                    .build();

            Deteccion deteccionGuardada = deteccionRepository.save(nuevaDeteccion);
            return convertirADeteccionDto(deteccionGuardada);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear la detección", e);
        }
    }

    // Método para actualizar una detección existente y devolver un DTO
    public DeteccionServiceDto actualizarDeteccion(Long deteccionId, DeteccionServiceDto deteccionActualizadaDto) {
        try {
            Proyecto p=proyectoService.obtenerProyectoPorIdEntidad(deteccionActualizadaDto.getProyectoId());
            Deteccion deteccionExistente = deteccionRepository.findById(deteccionId)
                    .orElseThrow(() -> new RuntimeException("Detección no encontrada con ID: " + deteccionId));

            deteccionExistente.setProyecto(p);
            deteccionExistente.setFotoId(deteccionActualizadaDto.getFotoId());
            deteccionExistente.setObjeto(deteccionActualizadaDto.getObjeto());
            deteccionExistente.setX(deteccionActualizadaDto.getX());
            deteccionExistente.setY(deteccionActualizadaDto.getY());
            deteccionExistente.setHeight(deteccionActualizadaDto.getHeight());
            deteccionExistente.setWeight(deteccionActualizadaDto.getWeight());
            deteccionExistente.setConfidence(deteccionActualizadaDto.getConfidence());

            Deteccion deteccionActualizada = deteccionRepository.save(deteccionExistente);
            return convertirADeteccionDto(deteccionActualizada);
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar la detección con ID: " + deteccionId, e);
        }
    }

    // Método para eliminar una detección por su ID
    public void eliminarDeteccion(Long deteccionId) {
        try {
            if (deteccionRepository.existsById(deteccionId)) {
                deteccionRepository.deleteById(deteccionId);
            } else {
                throw new RuntimeException("No se puede eliminar, detección no encontrada con ID: " + deteccionId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar la detección con ID: " + deteccionId, e);
        }
    }

    public boolean checkToday () {
        try {
            // Obtener la fecha actual
            LocalDate today = LocalDate.now();

            // Obtener todas las detecciones
            List<Deteccion> detecciones = deteccionRepository.findAll();

            // Comprobar si alguna detección tiene un 'fotoId' con la fecha de hoy
            return detecciones.stream().anyMatch(deteccion -> {
                // Convertir el campo 'fotoId' de Date a LocalDate
                LocalDate fotoDate = deteccion.getFotoId().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return fotoDate.equals(today);
            });
        } catch (Exception e) {
            throw new RuntimeException("Error al comprobar las detecciones de hoy", e);
        }
    }

    public List<DeteccionServiceDto> findByFotoId (Date fotoId) {
        // Llama al repositorio para buscar las detecciones por fotoId
        if(fotoId==null){
            throw new RuntimeException("Foto null");
        }
        return deteccionRepository.findByFotoId(fotoId);
    }

    public List<DeteccionServiceDto> findByProyectoAndDia (Long proyecto, Date dia) {
        if(proyecto==null || dia==null){
            throw new RuntimeException("Proyecto o dia vacio");
        }
        List<Deteccion> detecciones = deteccionRepository.findByProyectoIdAndFotoId(proyecto, dia);

        // Convertimos la entidad Deteccion a DeteccionServiceDto
        return detecciones.stream()
                .map(this::convertirADeteccionDto)
                .collect(Collectors.toList());
    }
}
