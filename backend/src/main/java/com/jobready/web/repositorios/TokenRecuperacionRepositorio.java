package com.jobready.web.repositorios;

import com.jobready.web.entidades.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TokenRecuperacionRepositorio extends JpaRepository<TokenRecuperacion, Integer> {
    @Query("select t.idUsuario from TokenRecuperacion t where t.tokenHash = :hash")
    Optional<Integer> buscarIdUsuario(@Param("hash") String hash);
}
