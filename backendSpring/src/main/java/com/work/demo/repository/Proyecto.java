package com.work.demo.repository;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



import java.sql.Date;

@Entity
@Table(name="Proyectos")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProyecto; // Uso de camelCase en la entidad

    @Column(name = "nombre") // No hace falta especificar, pero puede estar para claridad
    private String nombre;

    @Column(name = "fecha_creacion") // Mapeo explícito de la columna "fecha_creacion"
    private Date fechaCreacion;
    @Column(name = "min_conf") // Mapeo explícito de la columna "fecha_creacion"
    private double minConf;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // Mapeo explícito a proyecto_id
    private Usuario usuario;
}
