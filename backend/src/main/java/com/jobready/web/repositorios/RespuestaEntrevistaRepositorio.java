package com.jobready.web.repositorios;

import com.jobready.web.entidades.RespuestaEntrevista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RespuestaEntrevistaRepositorio extends JpaRepository<RespuestaEntrevista, Integer> {
    List<RespuestaEntrevista> findBySimulacion_IdSimulacion(Integer idSimulacion);

    /** Respuestas en el orden en que se mostraron las preguntas (las antiguas, sin orden, al final). */
    @Query("SELECT r FROM RespuestaEntrevista r WHERE r.simulacion.idSimulacion = :idSimulacion "
            + "ORDER BY CASE WHEN r.orden IS NULL THEN 1 ELSE 0 END, r.orden, r.idRespuesta")
    List<RespuestaEntrevista> findOrdenadasPorSimulacion(@Param("idSimulacion") Integer idSimulacion);
}
