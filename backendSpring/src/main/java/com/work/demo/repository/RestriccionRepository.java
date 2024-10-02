package com.work.demo.repository;

import com.work.demo.service.dto.RestriccionServiceDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestriccionRepository extends JpaRepository<Restriccion,Long> {
    @Query("SELECT new com.work.demo.service.dto.RestriccionServiceDto(r.idRestriccion, r.proyecto.idProyecto, r.objeto, r.fechaDesde, r.fechaHasta, r.cantidad) " +
            "FROM Restriccion r WHERE r.proyecto.idProyecto = :id")
    List<RestriccionServiceDto> findRestrictionsByProject (@Param("id")Long id);
}
