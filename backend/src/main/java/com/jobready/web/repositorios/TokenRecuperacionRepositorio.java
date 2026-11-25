package com.jobready.web.repositorios;

import com.jobready.web.entidades.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepositorio extends JpaRepository<TokenRecuperacion, Integer> {
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    long countByUsuario_IdUsuarioAndFechaCreacionAfter(Integer idUsuario, LocalDateTime desde);

    /** Anula los enlaces pendientes del usuario: solo el último correo enviado sirve. */
    @Modifying
    @Query("UPDATE TokenRecuperacion t SET t.fechaUso = :ahora WHERE t.usuario.idUsuario = :idUsuario AND t.fechaUso IS NULL")
    int invalidarPendientes(@Param("idUsuario") Integer idUsuario, @Param("ahora") LocalDateTime ahora);
}
