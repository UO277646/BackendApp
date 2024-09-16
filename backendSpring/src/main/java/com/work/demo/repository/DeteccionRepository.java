package com.work.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Repository
public interface DeteccionRepository extends JpaRepository<Deteccion,Long> {

}
