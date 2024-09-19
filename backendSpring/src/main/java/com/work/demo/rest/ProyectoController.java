package com.work.demo.rest;
import com.work.demo.rest.dto.ProyectoApiDto;
import com.work.demo.service.ProyectoService;
import com.work.demo.service.dto.ProyectoServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/proyectos")
public class ProyectoController {

    @Autowired
    private ProyectoService proyectoService;

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/find/all")
    public List<ProyectoApiDto> getAllProyectos() {
        List<ProyectoServiceDto> proyectos = proyectoService.findAll();
        // Convertimos de ProyectoServiceDto a ProyectoApiDto
        return proyectos.stream()
                .map(this::convertirAProyectoApiDto)
                .collect(Collectors.toList());
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/find/{id}")
    public ProyectoApiDto getProyectoById(@PathVariable Long id) {
        ProyectoServiceDto proyecto = proyectoService.obtenerProyectoPorId(id);
        return convertirAProyectoApiDto(proyecto);
    }

    //@CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/create")
    public ProyectoApiDto createProyecto(@RequestBody ProyectoApiDto proyectoApiDto) {
        ProyectoServiceDto proyectoDto = convertirAProyectoServiceDto(proyectoApiDto);
        ProyectoServiceDto nuevoProyecto = proyectoService.crearProyecto(proyectoDto);
        return convertirAProyectoApiDto(nuevoProyecto);
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PutMapping("/update/{id}")
    public ProyectoApiDto updateProyecto(@PathVariable Long id, @RequestBody ProyectoApiDto proyectoApiDto) {
        ProyectoServiceDto proyectoDto = convertirAProyectoServiceDto(proyectoApiDto);
        ProyectoServiceDto proyectoActualizado = proyectoService.actualizarProyecto(id, proyectoDto);
        return convertirAProyectoApiDto(proyectoActualizado);
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @DeleteMapping("/delete/{id}")
    public void deleteProyecto(@PathVariable Long id) {
        proyectoService.eliminarProyecto(id);
    }

    // Métodos de conversión entre ProyectoServiceDto y ProyectoApiDto
    private ProyectoApiDto convertirAProyectoApiDto(ProyectoServiceDto proyectoDto) {
        ProyectoApiDto apiDto = new ProyectoApiDto();
        apiDto.setIdProyecto(proyectoDto.getIdProyecto());
        apiDto.setNombre(proyectoDto.getNombre());
        apiDto.setFechaCreacion(proyectoDto.getFechaCreacion());
        return apiDto;
    }

    private ProyectoServiceDto convertirAProyectoServiceDto(ProyectoApiDto apiDto) {
        ProyectoServiceDto serviceDto = new ProyectoServiceDto();
        serviceDto.setIdProyecto(apiDto.getIdProyecto());
        serviceDto.setNombre(apiDto.getNombre());
        serviceDto.setFechaCreacion(apiDto.getFechaCreacion());
        return serviceDto;
    }
}
