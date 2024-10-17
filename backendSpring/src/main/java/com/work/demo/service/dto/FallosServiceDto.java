package com.work.demo.service.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FallosServiceDto {
    private Long falloId; // CamelCase
    private Long restriccionId; // CamelCase
    private String datos; // CamelCase
}
