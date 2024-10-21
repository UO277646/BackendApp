package com.work.demo.service;

import com.work.demo.repository.Fallo;
import com.work.demo.repository.FallosRepository;

import com.work.demo.repository.Restriccion;
import com.work.demo.repository.RestriccionRepository;
import com.work.demo.service.dto.FallosServiceDto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FallosService {

    @Autowired
    private FallosRepository fallosRepository;
    @Autowired
    private RestriccionRepository restriccionRepository;

    // Método para convertir de Fallo a FallosServiceDto
    private FallosServiceDto convertirAFallosDto(Fallo fallo) {
        return FallosServiceDto.builder()
                .falloId(fallo.getFalloId())
                .restriccionId(fallo.getRestriccion().getIdRestriccion())
                .datos(fallo.getDatos())
                .fecha(fallo.getFecha())
                .build();
    }

    // Método para obtener todos los fallos y convertirlos a DTOs
    public List<FallosServiceDto> obtenerTodosFallos() {
        try {
            List<Fallo> fallos = fallosRepository.findAll();
            return fallos.stream()
                    .map(this::convertirAFallosDto) // Convierte cada entidad en un DTO
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la lista de fallos", e);
        }
    }

    // Método para obtener un fallo por su ID y devolver un DTO
    public FallosServiceDto obtenerFalloPorId(Long falloId) {
        try {
            Optional<Fallo> fallo = fallosRepository.findById(falloId);
            return fallo.map(this::convertirAFallosDto)
                    .orElseThrow(() -> new RuntimeException("Fallo no encontrado con ID: " + falloId));
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el fallo con ID: " + falloId, e);
        }
    }

    // Método para crear un nuevo fallo y devolver un DTO
    @Transactional
    public FallosServiceDto crearFallo(FallosServiceDto falloDto) {
        try {
            Fallo nuevoFallo = new Fallo();
            Optional<Restriccion> restriccion=restriccionRepository.findById(falloDto.getRestriccionId());
            nuevoFallo.setRestriccion(restriccion.get());
            nuevoFallo.setDatos(falloDto.getDatos());

            Fallo falloGuardado = fallosRepository.save(nuevoFallo);
            return convertirAFallosDto(falloGuardado);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear el fallo", e);
        }
    }

    // Método para actualizar un fallo existente y devolver un DTO
    public FallosServiceDto actualizarFallo(Long falloId, FallosServiceDto falloActualizadoDto) {
        try {
            Fallo falloExistente = fallosRepository.findById(falloId)
                    .orElseThrow(() -> new RuntimeException("Fallo no encontrado con ID: " + falloId));

            Optional<Restriccion> restriccion=restriccionRepository.findById(falloActualizadoDto.getRestriccionId());
            falloExistente.setRestriccion(restriccion.get());
            falloExistente.setDatos(falloActualizadoDto.getDatos());

            Fallo falloActualizado = fallosRepository.save(falloExistente);
            return convertirAFallosDto(falloActualizado);
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar el fallo con ID: " + falloId, e);
        }
    }

    // Método para eliminar un fallo por su ID
    public void eliminarFallo(Long falloId) {
        try {
            if (fallosRepository.existsById(falloId)) {
                fallosRepository.deleteById(falloId);
            } else {
                throw new RuntimeException("No se puede eliminar, fallo no encontrado con ID: " + falloId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el fallo con ID: " + falloId, e);
        }
    }

    public List<FallosServiceDto> obtenerTodosFallosRes (Long idRec) {
        if(idRec==null){
            throw new RuntimeException("Error al obtener la lista de fallos");
        }
        try {
            List<Fallo> fallos = fallosRepository.findByRestriccion(idRec);
            return fallos.stream()
                    .map(this::convertirAFallosDto) // Convierte cada entidad en un DTO
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la lista de fallos", e);
        }
    }
}
