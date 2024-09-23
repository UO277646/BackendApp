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
                        deteccion.getProyecto(),
                        deteccion.getFotoId(),
                        deteccion.getObjeto(),
                        deteccion.getCantidad(),
                        deteccion.getEsquina1(),
                        deteccion.getEsquina2(),
                        deteccion.getEsquina3(),
                        deteccion.getEsquina4(),
                        deteccion.getAccuracy()))
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
    }

    private DeteccionServiceDto convertirADeteccionServiceDto(DeteccionApiDto deteccionApiDto) {
        return DeteccionServiceDto.builder()
                .deteccionId(deteccionApiDto.getDeteccionId())
                .proyecto(deteccionApiDto.getProyecto())
                .fotoId(deteccionApiDto.getFotoId())
                .objeto(deteccionApiDto.getObjeto())
                .cantidad(deteccionApiDto.getCantidad())
                .esquina1(deteccionApiDto.getEsquina1())
                .esquina2(deteccionApiDto.getEsquina2())
                .esquina3(deteccionApiDto.getEsquina3())
                .esquina4(deteccionApiDto.getEsquina4())
                .accuracy(deteccionApiDto.getAccuracy())
                .build();
    }
}

