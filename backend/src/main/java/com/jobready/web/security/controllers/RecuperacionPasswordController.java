package com.jobready.web.security.controllers;

import com.jobready.web.security.services.RecuperacionPasswordServicio;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RecuperacionPasswordController {
    private final RecuperacionPasswordServicio recuperacion;

    public RecuperacionPasswordController(RecuperacionPasswordServicio recuperacion) {
        this.recuperacion = recuperacion;
    }

    @PostMapping("/recuperar-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperar(@RequestBody RecuperarPasswordDTO datos) {
        recuperacion.recuperar(datos.email() == null ? null : datos.email().trim());
    }

    @PostMapping("/restablecer-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@RequestBody RestablecerPasswordDTO datos) {
        recuperacion.restablecer(datos.token(), datos.password());
    }

    public record RecuperarPasswordDTO(String email) {}

    public record RestablecerPasswordDTO(String token, String password) {}
}
