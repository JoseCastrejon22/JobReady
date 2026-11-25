package com.jobready.web.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pregunta que se mostró al candidato durante la grabación: su posición y el intervalo del video
 * (en segundos desde el inicio) en que estuvo en pantalla mientras respondía.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaRespondidaDTO {
    private Integer idPregunta;
    private Integer orden;
    private Integer segundoInicio;
    private Integer segundoFin;
}
