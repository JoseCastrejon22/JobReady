package com.jobready.web.servicios;

import com.jobready.web.entidades.Simulacion;
import com.jobready.web.repositorios.SimulacionRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.Serializable;
import java.util.List;

@Service
public class SimulacionServicio implements Serializable {
    @Autowired
    private SimulacionRepositorio simulacionRepositorio;

    public Simulacion insertar(Simulacion simulacion) {
        return simulacionRepositorio.save(simulacion);
    }

    public List<Simulacion> listar() {
        return simulacionRepositorio.findAll();
    }

    @Transactional
    public Simulacion actualizar(Integer id, Simulacion datos, String username) {
        Simulacion simulacion = simulacionRepositorio.findByIdSimulacionAndUsuario_Username(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa simulación."));
        simulacion.setCarrera(datos.getCarrera());
        simulacion.setSesion(datos.getSesion());
        simulacion.setNumeroSimulacion(datos.getNumeroSimulacion());
        simulacion.setFechaInicio(datos.getFechaInicio());
        simulacion.setFechaFin(datos.getFechaFin());
        simulacion.setDuracionSegundos(datos.getDuracionSegundos());
        simulacion.setEstado(datos.getEstado());
        return simulacionRepositorio.save(simulacion);
    }

    public void eliminar(Integer id) {
        if (!simulacionRepositorio.existsById(id)) {
            throw new RuntimeException("No existe la simulacion con id " + id);
        }
        simulacionRepositorio.deleteById(id);
    }
}


