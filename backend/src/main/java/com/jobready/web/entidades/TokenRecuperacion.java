package com.jobready.web.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "tokens_recuperacion")
@Getter
@Setter
public class TokenRecuperacion {
    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant vencimiento;

    @Column(nullable = false)
    private boolean usado;
}
