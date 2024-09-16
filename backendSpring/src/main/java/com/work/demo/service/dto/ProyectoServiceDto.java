package com.work.demo.service.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Data
@Setter
@Getter
public class ProyectoServiceDto {
    private Long idProyecto;

    private String nombre;
    private Date fechaCreacion;

}
