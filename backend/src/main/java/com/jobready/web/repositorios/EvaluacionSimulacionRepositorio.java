package com.jobready.web.repositorios;

import com.jobready.web.entidades.EvaluacionSimulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionSimulacionRepositorio extends JpaRepository<EvaluacionSimulacion, Integer> {
    Optional<EvaluacionSimulacion> findByIdEvaluacionAndEspecialista_Username(Integer idEvaluacion, String username);
    Optional<EvaluacionSimulacion> findBySimulacion_IdSimulacionAndEspecialista_Username(
            Integer idSimulacion, String username);
    Optional<EvaluacionSimulacion> findBySimulacion_IdSimulacionAndSimulacion_Usuario_Username(
            Integer idSimulacion, String username);
    List<EvaluacionSimulacion> findByEspecialista_UsernameOrderBySimulacion_FechaInicioDesc(String username);
    List<EvaluacionSimulacion> findBySimulacion_Usuario_UsernameOrderBySimulacion_FechaInicioDesc(String username);
}


