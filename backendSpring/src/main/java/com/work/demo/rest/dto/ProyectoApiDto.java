package com.work.demo.rest.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Data
@Setter
@Getter
public class ProyectoApiDto {
    private Long idProyecto;

    private String nombre;
    private Date fechaCreacion;

    private double minConf;
    private String user;
}
