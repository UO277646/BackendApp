package com.work.demo.rest.dto;

import jakarta.persistence.Column;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FallosApiDto {
    private Long falloId; // CamelCase
    private Long restriccionId; // CamelCase
    private String datos; // CamelCase
}
