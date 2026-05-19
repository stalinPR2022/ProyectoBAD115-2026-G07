package com.proyecto_bad115.sistema_encuestas.repository;

import com.proyecto_bad115.sistema_encuestas.model.RolPrivilegio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolPrivilegioRepository extends JpaRepository<RolPrivilegio, Integer> {

    List<RolPrivilegio> findByRolIdRol(Integer idRol);
}