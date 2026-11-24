package com.jobready.web.servicios;

import com.jobready.web.dtos.RevisionVideoDTO;
import com.jobready.web.entidades.ArchivoSimulacion;
import com.jobready.web.entidades.EvaluacionSimulacion;
import com.jobready.web.entidades.ObservacionVideo;
import com.jobready.web.entidades.Simulacion;
import com.jobready.web.repositorios.ArchivoSimulacionRepositorio;
import com.jobready.web.repositorios.EvaluacionSimulacionRepositorio;
import com.jobready.web.repositorios.ObservacionVideoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RevisionVideoServicio {
    private final EvaluacionSimulacionRepositorio evaluaciones;
    private final ArchivoSimulacionRepositorio archivos;
    private final ObservacionVideoRepositorio observaciones;

    public RevisionVideoServicio(EvaluacionSimulacionRepositorio evaluaciones, ArchivoSimulacionRepositorio archivos,
                                 ObservacionVideoRepositorio observaciones) {
        this.evaluaciones = evaluaciones;
        this.archivos = archivos;
        this.observaciones = observaciones;
    }

    public List<RevisionVideoDTO.Entrevista> listar(String username, boolean evaluador) {
        var lista = evaluador ? evaluaciones.findByEspecialista_UsernameOrderBySimulacion_FechaInicioDesc(username)
                : evaluaciones.findBySimulacion_Usuario_UsernameOrderBySimulacion_FechaInicioDesc(username);
        return lista.stream().map(evaluacion -> {
            Simulacion simulacion = evaluacion.getSimulacion();
            return new RevisionVideoDTO.Entrevista(simulacion.getIdSimulacion(),
                    simulacion.getUsuario().getNombreCompleto(), simulacion.getCarrera().getNombre(),
                    simulacion.getFechaInicio(), simulacion.getEstado());
        }).toList();
    }

    public RevisionVideoDTO obtener(Integer idSimulacion, String username, boolean evaluador) {
        var evaluacion = evaluador ? evaluacionAsignada(idSimulacion, username)
                : evaluaciones.findBySimulacion_IdSimulacionAndSimulacion_Usuario_Username(idSimulacion, username)
                .orElseThrow(this::revisionInaccesible);
        var notasPorArchivo = observaciones.findByEvaluacion_IdEvaluacionOrderBySegundoAscIdObservacionAsc(
                evaluacion.getIdEvaluacion()).stream().collect(Collectors.groupingBy(
                nota -> nota.getArchivo().getIdArchivo(),
                Collectors.mapping(this::convertir, Collectors.toList())));
        return new RevisionVideoDTO(archivos.findBySimulacion_IdSimulacionOrderByIdArchivo(idSimulacion)
                .stream().filter(this::esVideo).map(archivo -> new RevisionVideoDTO.Video(
                        archivo.getIdArchivo(), archivo.getUrlAlmacenamiento(),
                        notasPorArchivo.getOrDefault(archivo.getIdArchivo(), List.of()))).toList());
    }

    @Transactional
    public RevisionVideoDTO.Observacion guardar(Integer idSimulacion, String username,
                                               RevisionVideoDTO.NuevaObservacion datos) {
        EvaluacionSimulacion evaluacion = evaluacionAsignada(idSimulacion, username);
        String comentario = datos.comentario() == null ? "" : datos.comentario().strip();
        BigDecimal segundo = datos.segundo();
        if (datos.idArchivo() == null || comentario.isEmpty() || comentario.length() > 1000
                || segundo == null || segundo.signum() < 0 || segundo.scale() > 3
                || segundo.compareTo(new BigDecimal("999999999.999")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa el comentario y la marca de tiempo.");
        }
        ArchivoSimulacion archivo = archivos.findById(datos.idArchivo())
                .filter(video -> video.getSimulacion().getIdSimulacion().equals(idSimulacion) && esVideo(video))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe ese video en la entrevista."));
        ObservacionVideo nota = new ObservacionVideo();
        nota.setEvaluacion(evaluacion);
        nota.setArchivo(archivo);
        nota.setSegundo(segundo);
        nota.setComentario(comentario);
        return convertir(observaciones.save(nota));
    }

    private EvaluacionSimulacion evaluacionAsignada(Integer idSimulacion, String username) {
        return evaluaciones.findBySimulacion_IdSimulacionAndEspecialista_Username(idSimulacion, username)
                .orElseThrow(this::revisionInaccesible);
    }

    private ResponseStatusException revisionInaccesible() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una evaluación disponible para esta entrevista.");
    }

    private boolean esVideo(ArchivoSimulacion archivo) {
        String tipo = archivo.getTipoArchivo().toLowerCase(Locale.ROOT);
        return tipo.equals("video") || tipo.startsWith("video/");
    }

    private RevisionVideoDTO.Observacion convertir(ObservacionVideo nota) {
        return new RevisionVideoDTO.Observacion(nota.getIdObservacion(), nota.getSegundo(), nota.getComentario());
    }
}
