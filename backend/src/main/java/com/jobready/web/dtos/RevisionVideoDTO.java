package com.jobready.web.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RevisionVideoDTO(List<Video> videos) {
    public record Entrevista(Integer idSimulacion, String candidato, String carrera,
                             LocalDateTime fechaInicio, String estado) {}

    public record Video(Integer idArchivo, String url, List<Observacion> observaciones) {}

    public record Observacion(Integer idObservacion, BigDecimal segundo, String comentario) {}

    public record NuevaObservacion(Integer idArchivo, BigDecimal segundo, String comentario) {}
}
