package com.work.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FallosRepository extends JpaRepository<Fallo,Long> {
    @Query("SELECT f FROM Fallo f WHERE f.restriccion.idRestriccion = :idRec")
    List<Fallo> findByRestriccion(Long idRec);
}
