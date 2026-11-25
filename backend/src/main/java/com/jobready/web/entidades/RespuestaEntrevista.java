package com.jobready.web.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "respuestas_entrevista",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_respuesta_simulacion_pregunta",
                columnNames = {"id_simulacion", "id_pregunta"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEntrevista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Integer idRespuesta;

    @ManyToOne
    @JoinColumn(name = "id_simulacion", nullable = false)
    private Simulacion simulacion;

    @ManyToOne
    @JoinColumn(name = "id_pregunta", nullable = false)
    private PreguntaEntrevista pregunta;

    @ManyToOne
    @JoinColumn(name = "id_archivo")
    private ArchivoSimulacion archivo;

    @Column(name = "texto_respuesta", columnDefinition = "TEXT")
    private String textoRespuesta;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(length = 20)
    private String estado;

    /** Posición (1, 2, 3…) en que la pregunta se mostró al candidato durante la grabación. */
    @Column(name = "orden")
    private Integer orden;

    /** Segundo del video en que se mostró la pregunta (inicio de la respuesta). */
    @Column(name = "segundo_inicio")
    private Integer segundoInicio;

    /** Segundo del video en que el candidato pasó a la siguiente pregunta o terminó la grabación. */
    @Column(name = "segundo_fin")
    private Integer segundoFin;

    @PrePersist
    public void asignarValoresIniciales() {
        if (fechaRespuesta == null) fechaRespuesta = LocalDateTime.now();
        if (estado == null) estado = "enviada";
    }
}
