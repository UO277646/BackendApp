package com.work.demo.rest.dto;

import lombok.*;

import java.sql.Date;
@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FotoApiDto {
    private Date fechaCreacion;
    private long cantidad;
}
