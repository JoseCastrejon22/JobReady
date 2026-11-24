package com.jobready.web.servicios;

import com.jobready.web.entidades.ArchivoSimulacion;
import com.jobready.web.repositorios.ArchivoSimulacionRepositorio;
import com.jobready.web.repositorios.EvaluacionSimulacionRepositorio;
import com.jobready.web.repositorios.SimulacionRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.Serializable;
import java.util.List;

@Service
public class ArchivoSimulacionServicio implements Serializable {
    @Autowired
    private ArchivoSimulacionRepositorio archivoSimulacionRepositorio;
    @Autowired
    private SimulacionRepositorio simulaciones;
    @Autowired
    private EvaluacionSimulacionRepositorio evaluaciones;

    @Transactional
    public ArchivoSimulacion insertar(ArchivoSimulacion archivoSimulacion, String username, boolean evaluador) {
        Integer id = archivoSimulacion.getSimulacion().getIdSimulacion();
        var simulacion = evaluador
                ? evaluaciones.findBySimulacion_IdSimulacionAndEspecialista_Username(id, username)
                    .map(evaluacion -> evaluacion.getSimulacion())
                : simulaciones.findByIdSimulacionAndUsuario_Username(id, username);
        archivoSimulacion.setSimulacion(simulacion.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa simulación.")));
        return archivoSimulacionRepositorio.save(archivoSimulacion);
    }

    public List<ArchivoSimulacion> listar(String username, boolean administrador) {
        return administrador ? archivoSimulacionRepositorio.findAll() : archivoSimulacionRepositorio.listarAsignados(username);
    }

    @Transactional
    public void eliminar(Integer id, String username, boolean administrador) {
        var archivo = administrador ? archivoSimulacionRepositorio.findById(id)
                : archivoSimulacionRepositorio.findByIdArchivoAndSimulacion_Usuario_Username(id, username);
        archivoSimulacionRepositorio.delete(archivo.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe ese archivo.")));
    }
}


