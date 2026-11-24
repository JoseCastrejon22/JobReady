package com.jobready.web.controladores;

import com.jobready.web.dtos.RevisionVideoDTO;
import com.jobready.web.servicios.RevisionVideoServicio;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RevisionVideoController {
    private final RevisionVideoServicio revisionVideoServicio;

    public RevisionVideoController(RevisionVideoServicio revisionVideoServicio) {
        this.revisionVideoServicio = revisionVideoServicio;
    }

    @GetMapping("/evaluaciones-simulacion/revisiones")
    @PreAuthorize("hasAnyRole('CANDIDATO', 'EVALUADOR')")
    public List<RevisionVideoDTO.Entrevista> listar(Authentication usuario) {
        return revisionVideoServicio.listar(usuario.getName(), esEvaluador(usuario));
    }

    @GetMapping("/simulacion/{id}/revision-video")
    @PreAuthorize("hasAnyRole('CANDIDATO', 'EVALUADOR')")
    public RevisionVideoDTO obtener(@PathVariable Integer id, Authentication usuario) {
        return revisionVideoServicio.obtener(id, usuario.getName(), esEvaluador(usuario));
    }

    @PostMapping("/simulacion/{id}/observaciones-video")
    @PreAuthorize("hasRole('EVALUADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public RevisionVideoDTO.Observacion guardar(@PathVariable Integer id, Authentication usuario,
                                                @RequestBody RevisionVideoDTO.NuevaObservacion datos) {
        return revisionVideoServicio.guardar(id, usuario.getName(), datos);
    }

    private boolean esEvaluador(Authentication usuario) {
        return usuario.getAuthorities().stream().anyMatch(rol -> rol.getAuthority().equals("ROLE_EVALUADOR"));
    }
}
