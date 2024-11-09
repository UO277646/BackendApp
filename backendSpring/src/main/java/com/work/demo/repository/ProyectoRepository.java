package com.work.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto,Long> {

    List<Proyecto> findByUsuarioUserIdAndBorradoFalse(Long usuarioId);
}
