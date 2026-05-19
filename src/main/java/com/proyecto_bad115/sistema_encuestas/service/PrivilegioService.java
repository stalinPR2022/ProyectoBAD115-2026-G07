package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.model.Privilegio;
import com.proyecto_bad115.sistema_encuestas.repository.PrivilegioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrivilegioService {

    private final PrivilegioRepository privilegioRepository;

    public PrivilegioService(PrivilegioRepository privilegioRepository) {
        this.privilegioRepository = privilegioRepository;
    }

    public List<Privilegio> listarPrivilegios() {
        return privilegioRepository.findAll();
    }

    public Optional<Privilegio> buscarPorId(Integer id) {
        return privilegioRepository.findById(id);
    }

    public Privilegio guardarPrivilegio(Privilegio privilegio) {
        return privilegioRepository.save(privilegio);
    }

    public void eliminarPrivilegio(Integer id) {
        privilegioRepository.deleteById(id);
    }
}