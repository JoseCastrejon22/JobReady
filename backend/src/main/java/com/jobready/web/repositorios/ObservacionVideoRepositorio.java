package com.jobready.web.repositorios;

import com.jobready.web.entidades.ObservacionVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ObservacionVideoRepositorio extends JpaRepository<ObservacionVideo, Integer> {
    List<ObservacionVideo> findByEvaluacion_IdEvaluacionOrderBySegundoAscIdObservacionAsc(Integer idEvaluacion);
}
