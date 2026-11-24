package com.jobready.web.controladores;

import com.jobready.web.entidades.ArchivoSimulacion;
import com.jobready.web.dtos.ArchivoSimulacionDTO;
import com.jobready.web.servicios.ArchivoSimulacionServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ArchivoSimulacionController {
    @Autowired
    private ArchivoSimulacionServicio archivoSimulacionServicio;

    @PostMapping("/archivo-simulacion")
    @PreAuthorize("hasAnyRole('CANDIDATO', 'EVALUADOR')")
    public ArchivoSimulacion insertar(@RequestBody ArchivoSimulacionDTO archivoSimulacionDTO, Authentication usuario) {
        return archivoSimulacionServicio.insertar(archivoSimulacionDTO.convertirAEntidad(),
                usuario.getName(), tieneRol(usuario, "ROLE_EVALUADOR"));
    }

    @GetMapping("/archivos-simulacion")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public List<ArchivoSimulacion> listar(Authentication usuario) {
        return archivoSimulacionServicio.listar(usuario.getName(), tieneRol(usuario, "ROLE_ADMIN"));
    }

    @DeleteMapping("/archivo-simulacion/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATO')")
    public void eliminar(@PathVariable Integer id, Authentication usuario) {
        archivoSimulacionServicio.eliminar(id, usuario.getName(), tieneRol(usuario, "ROLE_ADMIN"));
    }

    private boolean tieneRol(Authentication usuario, String autoridad) {
        return usuario.getAuthorities().stream().anyMatch(rol -> rol.getAuthority().equals(autoridad));
    }
}
