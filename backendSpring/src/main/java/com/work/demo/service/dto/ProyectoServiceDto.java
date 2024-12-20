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
    private double minConf;
    private String user;

    @Override
    public String toString () {
        return "ProyectoServiceDto{" +
                "idProyecto=" + idProyecto +
                ", nombre='" + nombre + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", minConf=" + minConf +
                ", user='" + user + '\'' +
                '}';
    }
}
