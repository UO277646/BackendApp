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

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/find/all")
    public List<RestriccionApiDto> getAllRestricciones() {
        List<RestriccionServiceDto> restricciones = restriccionService.findAll();
        return restricciones.stream()
                .map(this::convertirARestriccionApiDto)
                .collect(Collectors.toList());
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/find/{id}")
    public RestriccionApiDto getRestriccionById(@PathVariable Long id) {
        RestriccionServiceDto restriccion = restriccionService.obtenerRestriccionPorId(id);
        return convertirARestriccionApiDto(restriccion);
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/create")
    public RestriccionApiDto createRestriccion(@RequestBody RestriccionApiDto restriccionApiDto) {
        RestriccionServiceDto restriccionDto = convertirARestriccionServiceDto(restriccionApiDto);
        RestriccionServiceDto nuevaRestriccion = restriccionService.crearRestriccion(restriccionDto);
        return convertirARestriccionApiDto(nuevaRestriccion);
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PutMapping("/update/{id}")
    public RestriccionApiDto updateRestriccion(@PathVariable Long id, @RequestBody RestriccionApiDto restriccionApiDto) {
        RestriccionServiceDto restriccionDto = convertirARestriccionServiceDto(restriccionApiDto);
        RestriccionServiceDto restriccionActualizada = restriccionService.actualizarRestriccion(id, restriccionDto);
        return convertirARestriccionApiDto(restriccionActualizada);
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @DeleteMapping("/delete/{id}")
    public void deleteRestriccion(@PathVariable Long id) {
        restriccionService.eliminarRestriccion(id);
    }

    // Métodos de conversión entre RestriccionServiceDto y RestriccionApiDto
    private RestriccionApiDto convertirARestriccionApiDto(RestriccionServiceDto restriccionDto) {
        return RestriccionApiDto.builder()
                .idRestriccion(restriccionDto.getIdRestriccion())
                .proyecto(restriccionDto.getProyecto())
                .objeto(restriccionDto.getObjeto())
                .fechaDesde(restriccionDto.getFechaDesde())
                .fechaHasta(restriccionDto.getFechaHasta())
                .cantidad(restriccionDto.getCantidad())
                .build();
    }

    private RestriccionServiceDto convertirARestriccionServiceDto(RestriccionApiDto restriccionApiDto) {
        return RestriccionServiceDto.builder()
                .idRestriccion(restriccionApiDto.getIdRestriccion())
                .proyecto(restriccionApiDto.getProyecto())
                .objeto(restriccionApiDto.getObjeto())
                .fechaDesde(restriccionApiDto.getFechaDesde())
                .fechaHasta(restriccionApiDto.getFechaHasta())
                .cantidad(restriccionApiDto.getCantidad())
                .build();
    }
}
