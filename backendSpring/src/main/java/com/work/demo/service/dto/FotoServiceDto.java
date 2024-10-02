package com.work.demo.service.dto;

import lombok.*;

import java.sql.Date;
@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FotoServiceDto {
    private Date fechaCreacion;
    private long cantidad;

}
