package com.work.demo.rest;
import com.work.demo.rest.dto.FotoApiDto;
import com.work.demo.rest.dto.ProyectoApiDto;
import com.work.demo.rest.dto.RestriccionApiDto;
import com.work.demo.service.ProyectoService;
import com.work.demo.service.dto.FotoServiceDto;
import com.work.demo.service.dto.ProyectoServiceDto;
import com.work.demo.service.dto.RestriccionServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/proyectos")
public class ProyectoController {

    @Autowired
    private ProyectoService proyectoService;

    @GetMapping("/find/all")
    public List<ProyectoApiDto> getAllProyectos() {
        List<ProyectoServiceDto> proyectos = proyectoService.findAll();
        // Convertimos de ProyectoServiceDto a ProyectoApiDto
        return proyectos.stream()
                .map(this::convertirAProyectoApiDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/find/detecciones/{id}")
    public List<FotoApiDto> getProyectDetections(@PathVariable Long id) {
        List<FotoServiceDto> fotos = proyectoService.findDetect(id);
        return fotos.stream()
                .map(fotoServiceDto -> new FotoApiDto(fotoServiceDto.getFechaCreacion(), fotoServiceDto.getCantidad()))
                .collect(Collectors.toList());
    }
    @GetMapping("/find/restricciones/{id}")
    public List<RestriccionApiDto> getProyectRestrictions(@PathVariable Long id) {
        List<RestriccionServiceDto> restricciones = proyectoService.findRestrict(id);
        // Convertimos de RestriccionServiceDto a RestriccionApiDto
        return restricciones.stream()
                .map(restriccion -> new RestriccionApiDto(restriccion.getIdRestriccion(),
                        restriccion.getObjeto(),
                        restriccion.getFechaDesde(),
                        restriccion.getFechaHasta(),restriccion.getCantidad()))
                .collect(Collectors.toList());
    }

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

    @PutMapping("/update/{id}")
    public ProyectoApiDto updateProyecto(@PathVariable Long id, @RequestBody ProyectoApiDto proyectoApiDto) {
        ProyectoServiceDto proyectoDto = convertirAProyectoServiceDto(proyectoApiDto);
        ProyectoServiceDto proyectoActualizado = proyectoService.actualizarProyecto(id, proyectoDto);
        return convertirAProyectoApiDto(proyectoActualizado);
    }

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
