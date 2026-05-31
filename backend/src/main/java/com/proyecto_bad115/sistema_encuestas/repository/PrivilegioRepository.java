package com.proyecto_bad115.sistema_encuestas.repository;

import com.proyecto_bad115.sistema_encuestas.model.Privilegio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivilegioRepository extends JpaRepository<Privilegio, Integer> {

    boolean existsByNombrePrivilegio(String nombrePrivilegio);

    Optional<Privilegio> findByNombrePrivilegio(String nombrePrivilegio);
}
