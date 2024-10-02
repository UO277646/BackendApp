package com.work.demo.rest;

import com.work.demo.rest.dto.DeteccionApiDto;
import com.work.demo.service.DeteccionService;
import com.work.demo.service.dto.DeteccionServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/detecciones")
public class DeteccionController {

    @Autowired
    private DeteccionService deteccionService;
    @GetMapping("/find/foto/{fotoId}")
    public List<DeteccionApiDto> getDetectionsByFotoId(@PathVariable Date fotoId) {
        List<DeteccionServiceDto> detecciones = deteccionService.findByFotoId(fotoId);
        // Convertimos de DeteccionServiceDto a DeteccionApiDto
        return detecciones.stream()
                .map(deteccion -> new DeteccionApiDto(deteccion.getDeteccionId(),
                        deteccion.getProyectoId(),
                        deteccion.getFotoId(),
                        deteccion.getObjeto(),
                        deteccion.getX(),
                        deteccion.getY(),
                        deteccion.getWeight(),
                        deteccion.getHeight(),
                        deteccion.getConfidence()
                        ))
                .collect(Collectors.toList());
    }

    // Método para obtener detecciones por proyecto y día (fecha)
    @GetMapping("/get/{proyecto}/{dia}")
    public List<DeteccionApiDto> getDeteccionesByProyectoAndDia(@PathVariable Long proyecto, @PathVariable Date dia) {
        List<DeteccionServiceDto> detecciones = deteccionService.findByProyectoAndDia(proyecto, dia);
        return detecciones.stream()
                .map(this::convertirADeteccionApiDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/find/all")
    public List<DeteccionApiDto> getAllDetecciones() {
        List<DeteccionServiceDto> detecciones = deteccionService.findAll();
        // Convertimos de DeteccionServiceDto a DeteccionApiDto
        return detecciones.stream()
                .map(this::convertirADeteccionApiDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/find/{id}")
    public DeteccionApiDto getDeteccionById(@PathVariable Long id) {
        DeteccionServiceDto deteccion = deteccionService.obtenerDeteccionPorId(id);
        return convertirADeteccionApiDto(deteccion);
    }

    @PostMapping("/create")
    public DeteccionApiDto createDeteccion(@RequestBody DeteccionApiDto deteccionApiDto) {
        DeteccionServiceDto deteccionDto = convertirADeteccionServiceDto(deteccionApiDto);
        DeteccionServiceDto nuevaDeteccion = deteccionService.crearDeteccion(deteccionDto);
        return convertirADeteccionApiDto(nuevaDeteccion);
    }

    @PutMapping("/update/{id}")
    public DeteccionApiDto updateDeteccion(@PathVariable Long id, @RequestBody DeteccionApiDto deteccionApiDto) {
        DeteccionServiceDto deteccionDto = convertirADeteccionServiceDto(deteccionApiDto);
        DeteccionServiceDto deteccionActualizada = deteccionService.actualizarDeteccion(id, deteccionDto);
        return convertirADeteccionApiDto(deteccionActualizada);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteDeteccion(@PathVariable Long id) {
        deteccionService.eliminarDeteccion(id);
    }

    // Métodos de conversión entre DeteccionServiceDto y DeteccionApiDto
    private DeteccionApiDto convertirADeteccionApiDto(DeteccionServiceDto deteccionDto) {
        return DeteccionApiDto.builder()
                .deteccionId(deteccionDto.getDeteccionId())
                .proyectoId(deteccionDto.getProyectoId())
                .fotoId(deteccionDto.getFotoId())
                .objeto(deteccionDto.getObjeto())
                .x(deteccionDto.getX())
                .y(deteccionDto.getY())
                .weight(deteccionDto.getWeight())
                .height(deteccionDto.getHeight())
                .confidence(deteccionDto.getConfidence())
                .build();
    }

    private DeteccionServiceDto convertirADeteccionServiceDto(DeteccionApiDto deteccionApiDto) {
        return DeteccionServiceDto.builder()
                .deteccionId(deteccionApiDto.getDeteccionId())
                .proyectoId(deteccionApiDto.getProyectoId())
                .fotoId(deteccionApiDto.getFotoId())
                .objeto(deteccionApiDto.getObjeto())
                .x(deteccionApiDto.getX())
                .y(deteccionApiDto.getY())
                .weight(deteccionApiDto.getWeight())
                .height(deteccionApiDto.getHeight())
                .confidence(deteccionApiDto.getConfidence())
                .build();
    }
}

