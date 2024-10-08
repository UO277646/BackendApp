package com.work.demo.service;

import com.work.demo.repository.Proyecto;
import com.work.demo.repository.Restriccion;
import com.work.demo.repository.RestriccionRepository;
import com.work.demo.service.dto.RestriccionServiceDto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RestriccionService {

    @Autowired
    private RestriccionRepository restriccionRepository;
    @Autowired
    private ProyectoService proyectoService;
    // Método para convertir de Restriccion a RestriccionServiceDto
    private RestriccionServiceDto convertirARestriccionDto(Restriccion restriccion) {
        return RestriccionServiceDto.builder()
                .idRestriccion(restriccion.getIdRestriccion())
                //.proyecto(restriccion.getProyecto())
                .objeto(restriccion.getObjeto())
                .fechaDesde(restriccion.getFechaDesde())
                .fechaHasta(restriccion.getFechaHasta())
                .cantidadMin(restriccion.getCantidadMin())
                .cantidadMax(restriccion.getCantidadMax())
                .cumplida(restriccion.getCumplida())
                .build();
    }

    // Método para obtener todas las restricciones y convertirlas a DTOs
    public List<RestriccionServiceDto> findAll() {
        try {
            List<Restriccion> restricciones = restriccionRepository.findAll();
            return restricciones.stream()
                    .map(this::convertirARestriccionDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la lista de restricciones", e);
        }
    }

    // Método para obtener una restricción por su ID
    public RestriccionServiceDto obtenerRestriccionPorId(Long idRestriccion) {
        try {
            Optional<Restriccion> restriccion = restriccionRepository.findById(idRestriccion);
            return restriccion.map(this::convertirARestriccionDto)
                    .orElseThrow(() -> new RuntimeException("Restricción no encontrada con ID: " + idRestriccion));
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la restricción con ID: " + idRestriccion, e);
        }
    }
    @Transactional
    // Método para crear una nueva restricción
    public RestriccionServiceDto crearRestriccion(RestriccionServiceDto restriccionDto) {
        try {
            Restriccion nuevaRestriccion = new Restriccion();
            Proyecto p=proyectoService.obtenerProyectoPorIdEntidad(restriccionDto.getProyectoId());
            nuevaRestriccion.setProyecto(p);
            nuevaRestriccion.setObjeto(restriccionDto.getObjeto());
            nuevaRestriccion.setFechaDesde(restriccionDto.getFechaDesde());
            nuevaRestriccion.setFechaHasta(restriccionDto.getFechaHasta());
            nuevaRestriccion.setCantidadMin(restriccionDto.getCantidadMin());
            nuevaRestriccion.setCantidadMax(restriccionDto.getCantidadMax());
            nuevaRestriccion.setCumplida(null);
            Restriccion restriccionGuardada = restriccionRepository.save(nuevaRestriccion);
            return convertirARestriccionDto(restriccionGuardada);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear la restricción", e);
        }
    }

    // Método para actualizar una restricción existente
    public RestriccionServiceDto actualizarRestriccion(Long idRestriccion, RestriccionServiceDto restriccionActualizadaDto) {
        try {
            Restriccion restriccionExistente = restriccionRepository.findById(idRestriccion)
                    .orElseThrow(() -> new RuntimeException("Restricción no encontrada con ID: " + idRestriccion));

            //restriccionExistente.setProyecto(restriccionActualizadaDto.getProyecto());
            restriccionExistente.setObjeto(restriccionActualizadaDto.getObjeto());
            restriccionExistente.setFechaDesde(restriccionActualizadaDto.getFechaDesde());
            restriccionExistente.setFechaHasta(restriccionActualizadaDto.getFechaHasta());
            restriccionExistente.setCantidadMin(restriccionActualizadaDto.getCantidadMin());
            restriccionExistente.setCantidadMax(restriccionActualizadaDto.getCantidadMax());
            restriccionExistente.setCumplida(restriccionActualizadaDto.getCumplida());
            Restriccion restriccionActualizada = restriccionRepository.save(restriccionExistente);
            return convertirARestriccionDto(restriccionActualizada);
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar la restricción con ID: " + idRestriccion, e);
        }
    }

    // Método para eliminar una restricción por su ID
    public void eliminarRestriccion(Long idRestriccion) {
        try {
            if (restriccionRepository.existsById(idRestriccion)) {
                restriccionRepository.deleteById(idRestriccion);
            } else {
                throw new RuntimeException("No se puede eliminar, restricción no encontrada con ID: " + idRestriccion);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar la restricción con ID: " + idRestriccion, e);
        }
    }
}
