package com.work.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestriccionRepository extends JpaRepository<Restriccion,Long> {
}
