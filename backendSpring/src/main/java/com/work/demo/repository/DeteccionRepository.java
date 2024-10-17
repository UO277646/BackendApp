package com.work.demo.repository;

import com.work.demo.service.dto.DeteccionServiceDto;
import com.work.demo.service.dto.FotoServiceDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface DeteccionRepository extends JpaRepository<Deteccion,Long> {
    @Query("SELECT new com.work.demo.service.dto.FotoServiceDto(d.fotoId, COUNT(d)) " +
            "FROM Deteccion d WHERE d.proyecto.idProyecto = :id " +
            "GROUP BY d.fotoId")
    List<FotoServiceDto> findDetectionsByProjectGroupedByDate (@Param("id")Long id);
    //@Query(value = "SELECT d.deteccion_id AS deteccionId, d.proyecto_id AS proyectoId, d.foto_id AS fotoId, d.objeto, d.cantidad, d.esquina1, d.esquina2, d.esquina3, d.esquina4, d.accuracy " +
           // "FROM detecciones d WHERE d.foto_id = :fotoId", nativeQuery = true)
    List<DeteccionServiceDto> findByFotoId (@Param("fotoId")Date fotoId);
    @Query("SELECT d FROM Deteccion d WHERE d.proyecto.idProyecto = :proyecto AND d.fotoId = :dia")
    List<Deteccion> findByProyectoIdAndFotoId (Long proyecto, Date dia);
    @Query("SELECT d FROM Deteccion d WHERE d.proyecto.idProyecto = :proyectoId")
    List<Deteccion> findDetectionsByProject (Long proyectoId);
}
