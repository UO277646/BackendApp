package com.work.demo.rest;

import com.work.demo.rest.dto.FallosApiDto;
import com.work.demo.rest.dto.ProyectoApiDto;
import com.work.demo.service.FallosService;
import com.work.demo.service.dto.FallosServiceDto;
import com.work.demo.service.dto.ProyectoServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/fallos")
public class FallosController {
    @Autowired
    private FallosService fallosService;
    @GetMapping("/find/fallos/{idRec}")
    public List<FallosApiDto> getProyectosByEmail(@PathVariable Long idRec) {
        List<FallosServiceDto> fallosServiceDtos = fallosService.obtenerTodosFallosRes(idRec);
        return fallosServiceDtos.stream()
                .map(this::convertirApiDto)
                .collect(Collectors.toList());
    }

    private FallosApiDto convertirApiDto (FallosServiceDto fallosServiceDto) {
            FallosApiDto f=new FallosApiDto();
            f.setFalloId(fallosServiceDto.getFalloId());
            f.setRestriccionId(fallosServiceDto.getRestriccionId());
            f.setDatos(fallosServiceDto.getDatos());
            return f;
    }
}
