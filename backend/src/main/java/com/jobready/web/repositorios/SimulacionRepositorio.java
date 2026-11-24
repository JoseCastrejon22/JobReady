package com.jobready.web.repositorios;

import com.jobready.web.entidades.Simulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SimulacionRepositorio extends JpaRepository<Simulacion, Integer> {
    Optional<Simulacion> findByIdSimulacionAndUsuario_Username(Integer idSimulacion, String username);
}


