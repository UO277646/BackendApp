package com.work.demo.service.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UsuarioServiceDto {
    private Long userId; // CamelCase
    private String nombre; // CamelCase
    private String email; // CamelCase
}
