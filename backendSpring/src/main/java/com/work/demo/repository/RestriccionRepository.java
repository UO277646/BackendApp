package com.work.demo.repository;

import com.work.demo.service.dto.RestriccionServiceDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestriccionRepository extends JpaRepository<Restriccion,Long> {
    @Query("SELECT new com.work.demo.service.dto.RestriccionServiceDto(r.idRestriccion, r.proyecto.idProyecto, r.objeto, r.fechaDesde, r.fechaHasta,r.cantidadMin, r.cantidadMax,r.cumplida,r.diaria) " +
            "FROM Restriccion r WHERE r.proyecto.idProyecto = :id AND r.borrado=false")
    List<RestriccionServiceDto> findRestrictionsByProject (@Param("id")Long id);
    @Query("SELECT r FROM Restriccion r WHERE r.proyecto.idProyecto = :proyectoId AND r.borrado=false" )
    List<Restriccion> findRestrictionsByProjectEntity (Long proyectoId);
    @Query("SELECT r FROM Restriccion r WHERE r.proyecto.idProyecto = :proyectoId AND r.diaria=true AND r.borrado=false" )
    List<Restriccion> findRestrictionsByProjectDaily (Long proyectoId);
}
