package com.jobready.web.servicios;

import com.jobready.web.entidades.EvaluacionSimulacion;
import com.jobready.web.repositorios.EvaluacionSimulacionRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.Serializable;
import java.util.List;
import com.jobready.web.dtos.ResultadoSimulacionDTO;


@Service
public class EvaluacionSimulacionServicio implements Serializable {
    @Autowired
    private EvaluacionSimulacionRepositorio evaluacionSimulacionRepositorio;

    public EvaluacionSimulacion insertar(EvaluacionSimulacion evaluacionSimulacion) {
        return evaluacionSimulacionRepositorio.save(evaluacionSimulacion);
    }

    public List<EvaluacionSimulacion> listar() {
        return evaluacionSimulacionRepositorio.findAll();
    }

    public ResultadoSimulacionDTO obtenerResultado(Integer idSimulacion, String username) {
        EvaluacionSimulacion evaluacion =
                evaluacionSimulacionRepositorio
                        .findBySimulacion_IdSimulacionAndSimulacion_Usuario_Username(idSimulacion, username)
                        .orElseThrow(this::evaluacionInaccesible);
        String nombreEspecialista = null;
        if (evaluacion.getEspecialista() != null) {
            nombreEspecialista = evaluacion.getEspecialista().getNombreCompleto();
        }
        return new ResultadoSimulacionDTO(
                evaluacion.getSimulacion().getIdSimulacion(),
                evaluacion.getPuntajeGeneral(),
                evaluacion.getNivelComunicacion(),
                evaluacion.getTiempoPromedioRespuesta(),
                evaluacion.getSeguridadCalificacion(),
                evaluacion.getFeedbackResumen(),
                nombreEspecialista
        );
    }

    @Transactional
    public EvaluacionSimulacion actualizar(Integer id, EvaluacionSimulacion datos, String username) {
        EvaluacionSimulacion evaluacion = buscarAsignada(id, username);
        evaluacion.setPuntajeGeneral(datos.getPuntajeGeneral());
        evaluacion.setNivelComunicacion(datos.getNivelComunicacion());
        evaluacion.setTiempoPromedioRespuesta(datos.getTiempoPromedioRespuesta());
        evaluacion.setSeguridadCalificacion(datos.getSeguridadCalificacion());
        evaluacion.setFeedbackResumen(datos.getFeedbackResumen());
        evaluacion.setEvaluadoPorEspecialista(datos.getEvaluadoPorEspecialista());
        return evaluacionSimulacionRepositorio.save(evaluacion);
    }

    @Transactional
    public void eliminar(Integer id, String username, boolean administrador) {
        EvaluacionSimulacion evaluacion = administrador
                ? evaluacionSimulacionRepositorio.findById(id).orElseThrow(this::evaluacionInaccesible)
                : buscarAsignada(id, username);
        evaluacionSimulacionRepositorio.delete(evaluacion);
    }

    private EvaluacionSimulacion buscarAsignada(Integer id, String username) {
        return evaluacionSimulacionRepositorio.findByIdEvaluacionAndEspecialista_Username(id, username)
                .orElseThrow(this::evaluacionInaccesible);
    }

    private ResponseStatusException evaluacionInaccesible() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una evaluación disponible.");
    }
}


