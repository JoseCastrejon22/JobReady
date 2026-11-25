package com.jobready.web.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

/**
 * Solicitud de recuperación de contraseña. Solo se guarda el hash SHA-256 del token que viaja en el
 * enlace del correo, de modo que leer la tabla no permite restablecer ninguna cuenta.
 * El token es de un solo uso y vence a los pocos minutos.
 */
@Entity
@Table(name = "tokens_recuperacion", indexes = @Index(name = "idx_tokens_recuperacion_usuario", columnList = "id_usuario"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRecuperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Integer idToken;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
    /** Momento en que se usó o se invalidó; null mientras el enlace siga disponible. */
    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;
}
