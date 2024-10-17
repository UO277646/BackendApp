package com.work.demo.rest.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.sql.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UsuarioApiDto {
    private Long userId; // CamelCase
    private String nombre; // CamelCase
    private String email; // CamelCase
}
