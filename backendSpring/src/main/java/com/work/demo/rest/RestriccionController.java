package com.work.demo.rest;

import com.work.demo.rest.dto.RestriccionApiDto;
import com.work.demo.service.RestriccionService;
import com.work.demo.service.dto.RestriccionServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/restricciones")
public class RestriccionController {

    @Autowired
    private RestriccionService restriccionService;

    @GetMapping("/find/all")
    public List<RestriccionApiDto> getAllRestricciones() {
        List<RestriccionServiceDto> restricciones = restriccionService.findAll();
        return restricciones.stream()
                .map(this::convertirARestriccionApiDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/find/{id}")
    public RestriccionApiDto getRestriccionById(@PathVariable Long id) {
        RestriccionServiceDto restriccion = restriccionService.obtenerRestriccionPorId(id);
        return convertirARestriccionApiDto(restriccion);
    }

    @PostMapping("/create")
    public RestriccionApiDto createRestriccion(@RequestBody RestriccionApiDto restriccionApiDto) {
        RestriccionServiceDto restriccionDto = convertirARestriccionServiceDto(restriccionApiDto);
        System.out.println(restriccionApiDto);
        RestriccionServiceDto nuevaRestriccion = restriccionService.crearRestriccion(restriccionDto);
        return convertirARestriccionApiDto(nuevaRestriccion);
    }

    @PutMapping("/update/{id}")
    public RestriccionApiDto updateRestriccion(@PathVariable Long id, @RequestBody RestriccionApiDto restriccionApiDto) {
        RestriccionServiceDto restriccionDto = convertirARestriccionServiceDto(restriccionApiDto);
        RestriccionServiceDto restriccionActualizada = restriccionService.actualizarRestriccion(id, restriccionDto);
        return convertirARestriccionApiDto(restriccionActualizada);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteRestriccion(@PathVariable Long id) {
        restriccionService.eliminarRestriccion(id);
    }

    // Métodos de conversión entre RestriccionServiceDto y RestriccionApiDto
    private RestriccionApiDto convertirARestriccionApiDto(RestriccionServiceDto restriccionDto) {
        return RestriccionApiDto.builder()
                .idRestriccion(restriccionDto.getIdRestriccion())
                .objeto(restriccionDto.getObjeto())
                .fechaDesde(restriccionDto.getFechaDesde())
                .fechaHasta(restriccionDto.getFechaHasta())
                .cantidadMin(restriccionDto.getCantidadMin())
                .cantidadMax(restriccionDto.getCantidadMax())
                .diaria(restriccionDto.getDiaria())
                .build();
    }

    private RestriccionServiceDto convertirARestriccionServiceDto(RestriccionApiDto restriccionApiDto) {
        return RestriccionServiceDto.builder()
                .idRestriccion(restriccionApiDto.getIdRestriccion())
                .objeto(restriccionApiDto.getObjeto())
                .fechaDesde(restriccionApiDto.getFechaDesde())
                .fechaHasta(restriccionApiDto.getFechaHasta())
                .cantidadMin(restriccionApiDto.getCantidadMin())
                .cantidadMax(restriccionApiDto.getCantidadMax())
                .proyectoId(restriccionApiDto.getProyectoId())
                .diaria(restriccionApiDto.getDiaria())
                .build();
    }
}
