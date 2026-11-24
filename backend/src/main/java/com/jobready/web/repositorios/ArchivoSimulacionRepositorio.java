package com.jobready.web.repositorios;

import com.jobready.web.entidades.ArchivoSimulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArchivoSimulacionRepositorio extends JpaRepository<ArchivoSimulacion, Integer> {
    List<ArchivoSimulacion> findBySimulacion_IdSimulacionOrderByIdArchivo(Integer idSimulacion);
    Optional<ArchivoSimulacion> findByIdArchivoAndSimulacion_Usuario_Username(Integer idArchivo, String username);

    @Query("select a from ArchivoSimulacion a where exists (select e from EvaluacionSimulacion e "
            + "where e.simulacion = a.simulacion and e.especialista.username = :username)")
    List<ArchivoSimulacion> listarAsignados(@Param("username") String username);
}


