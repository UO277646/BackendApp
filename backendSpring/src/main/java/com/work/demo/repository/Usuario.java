package com.work.demo.repository;

import jakarta.persistence.*;
import lombok.*;

@Table(name="Usuarios")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId; // CamelCase
    @Column(name = "nombre") // No hace falta especificar, pero puede estar para claridad
    private String nombre; // CamelCase
    @Column(name = "email") // No hace falta especificar, pero puede estar para claridad
    private String email; // CamelCase
}
