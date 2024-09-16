package com.work.demo.service;

import com.work.demo.repository.Deteccion;
import com.work.demo.repository.DeteccionRepository;
import com.work.demo.service.dto.DeteccionServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeteccionService {

    @Autowired
    private DeteccionRepository deteccionRepository;

    // Método para convertir de Deteccion a DeteccionServiceDto
    private DeteccionServiceDto convertirADeteccionDto(Deteccion deteccion) {
        return DeteccionServiceDto.builder()
                .deteccionId(deteccion.getDeteccionId())
                .proyecto(deteccion.getProyecto())
                .fotoId(deteccion.getFotoId())
                .objeto(deteccion.getObjeto())
                .cantidad(deteccion.getCantidad())
                .esquina1(deteccion.getEsquina1())
                .esquina2(deteccion.getEsquina2())
                .esquina3(deteccion.getEsquina3())
                .esquina4(deteccion.getEsquina4())
                .accuracy(deteccion.getAccuracy())
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
            Deteccion nuevaDeteccion = Deteccion.builder()
                    .proyecto(deteccionDto.getProyecto())
                    .fotoId(deteccionDto.getFotoId())
                    .objeto(deteccionDto.getObjeto())
                    .cantidad(deteccionDto.getCantidad())
                    .esquina1(deteccionDto.getEsquina1())
                    .esquina2(deteccionDto.getEsquina2())
                    .esquina3(deteccionDto.getEsquina3())
                    .esquina4(deteccionDto.getEsquina4())
                    .accuracy(deteccionDto.getAccuracy())
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
            Deteccion deteccionExistente = deteccionRepository.findById(deteccionId)
                    .orElseThrow(() -> new RuntimeException("Detección no encontrada con ID: " + deteccionId));

            deteccionExistente.setProyecto(deteccionActualizadaDto.getProyecto());
            deteccionExistente.setFotoId(deteccionActualizadaDto.getFotoId());
            deteccionExistente.setObjeto(deteccionActualizadaDto.getObjeto());
            deteccionExistente.setCantidad(deteccionActualizadaDto.getCantidad());
            deteccionExistente.setEsquina1(deteccionActualizadaDto.getEsquina1());
            deteccionExistente.setEsquina2(deteccionActualizadaDto.getEsquina2());
            deteccionExistente.setEsquina3(deteccionActualizadaDto.getEsquina3());
            deteccionExistente.setEsquina4(deteccionActualizadaDto.getEsquina4());
            deteccionExistente.setAccuracy(deteccionActualizadaDto.getAccuracy());

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
}
