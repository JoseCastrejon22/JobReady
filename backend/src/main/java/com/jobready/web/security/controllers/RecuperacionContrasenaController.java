package com.jobready.web.security.controllers;

import com.jobready.web.security.dtos.RestablecerContrasenaDTO;
import com.jobready.web.security.dtos.SolicitudRecuperacionDTO;
import com.jobready.web.security.services.RecuperacionContrasenaServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Endpoints públicos para recuperar la contraseña mediante un enlace enviado al correo. */
@RestController
@RequestMapping("/api/recuperar-contrasena")
public class RecuperacionContrasenaController {
    private final RecuperacionContrasenaServicio servicio;

    public RecuperacionContrasenaController(RecuperacionContrasenaServicio servicio) {
        this.servicio = servicio;
    }

    /** Siempre responde lo mismo, exista o no la cuenta, para no revelar qué correos están registrados. */
    @PostMapping
    public Map<String, String> solicitar(@RequestBody SolicitudRecuperacionDTO solicitud) {
        servicio.solicitar(solicitud.getEmail());
        return Map.of("mensaje", "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña.");
    }

    @PostMapping("/validar")
    public ResponseEntity<Void> validar(@RequestBody RestablecerContrasenaDTO datos) {
        servicio.validar(datos.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecer(@RequestBody RestablecerContrasenaDTO datos) {
        servicio.restablecer(datos.getToken(), datos.getPassword());
        return ResponseEntity.noContent().build();
    }
}
