package com.jobready.web.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.math.BigDecimal;

@Entity
@Table(name = "observaciones_video", indexes = @Index(
        name = "idx_observaciones_evaluacion_video", columnList = "id_evaluacion,id_archivo,segundo"))
@Getter
@Setter
public class ObservacionVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idObservacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EvaluacionSimulacion evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_archivo", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ArchivoSimulacion archivo;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal segundo;

    @Column(nullable = false, length = 1000)
    private String comentario;
}
