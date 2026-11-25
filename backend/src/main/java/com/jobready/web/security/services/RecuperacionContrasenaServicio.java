package com.jobready.web.security.services;

import com.jobready.web.entidades.TokenRecuperacion;
import com.jobready.web.entidades.Usuario;
import com.jobready.web.repositorios.TokenRecuperacionRepositorio;
import com.jobready.web.repositorios.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperación de contraseña por correo: el usuario indica su email, recibe un enlace con un token
 * aleatorio de un solo uso y, desde ese enlace, define una contraseña nueva.
 */
@Service
public class RecuperacionContrasenaServicio {
    private static final Logger log = LoggerFactory.getLogger(RecuperacionContrasenaServicio.class);
    public static final int MINUTOS_VALIDEZ = 30;
    /** Máximo de correos por cuenta en la ventana indicada, para que no se use para saturar un buzón. */
    private static final int MAX_SOLICITUDES = 3;
    private static final int VENTANA_MINUTOS = 15;
    private static final int LONGITUD_MINIMA_PASSWORD = 8;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepositorio usuarioRepositorio;
    private final TokenRecuperacionRepositorio tokenRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final CorreoServicio correoServicio;
    private final String urlFrontend;

    public RecuperacionContrasenaServicio(UsuarioRepositorio usuarioRepositorio,
                                          TokenRecuperacionRepositorio tokenRepositorio,
                                          PasswordEncoder passwordEncoder,
                                          CorreoServicio correoServicio,
                                          @Value("${ip.frontend}") String urlFrontend) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.tokenRepositorio = tokenRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.correoServicio = correoServicio;
        this.urlFrontend = urlFrontend.endsWith("/") ? urlFrontend.substring(0, urlFrontend.length() - 1) : urlFrontend;
    }

    /**
     * Si el correo pertenece a una cuenta activa, genera un enlace nuevo (anulando los anteriores) y lo envía.
     * Nunca informa si el correo existe: quien llama siempre recibe la misma respuesta.
     */
    @Transactional
    public void solicitar(String email) {
        if (email == null || email.isBlank() || email.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un correo válido");
        }
        Usuario usuario = usuarioRepositorio.findFirstByEmailIgnoreCase(email.trim()).orElse(null);
        if (usuario == null || !"activo".equalsIgnoreCase(usuario.getEstado())) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (tokenRepositorio.countByUsuario_IdUsuarioAndFechaCreacionAfter(
                usuario.getIdUsuario(), ahora.minusMinutes(VENTANA_MINUTOS)) >= MAX_SOLICITUDES) {
            log.info("Recuperación de contraseña omitida para el usuario {}: demasiadas solicitudes", usuario.getIdUsuario());
            return;
        }
        tokenRepositorio.invalidarPendientes(usuario.getIdUsuario(), ahora);

        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenRecuperacion registro = new TokenRecuperacion();
        registro.setUsuario(usuario);
        registro.setTokenHash(hash(token));
        registro.setFechaCreacion(ahora);
        registro.setFechaExpiracion(ahora.plusMinutes(MINUTOS_VALIDEZ));
        tokenRepositorio.save(registro);

        String enlace = urlFrontend + "/restablecer-contrasena?token=" + token;
        correoServicio.enviarRecuperacionContrasena(usuario.getEmail(), usuario.getNombreCompleto(), enlace, MINUTOS_VALIDEZ);
    }

    /** Comprueba que el enlace siga vigente, para avisar antes de que el usuario escriba la contraseña. */
    @Transactional(readOnly = true)
    public void validar(String token) {
        buscarVigente(token, LocalDateTime.now());
    }

    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < LONGITUD_MINIMA_PASSWORD || nuevaPassword.length() > 72) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La contraseña debe tener entre 8 y 72 caracteres");
        }
        LocalDateTime ahora = LocalDateTime.now();
        TokenRecuperacion registro = buscarVigente(token, ahora);
        Usuario usuario = registro.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepositorio.save(usuario);
        // Marca este enlace y cualquier otro pendiente como usados.
        tokenRepositorio.invalidarPendientes(usuario.getIdUsuario(), ahora);
        log.info("Contraseña restablecida para el usuario {}", usuario.getIdUsuario());
    }

    private TokenRecuperacion buscarVigente(String token, LocalDateTime ahora) {
        if (token == null || token.isBlank() || token.length() > 100) {
            throw enlaceInvalido();
        }
        TokenRecuperacion registro = tokenRepositorio.findByTokenHash(hash(token.trim())).orElseThrow(this::enlaceInvalido);
        if (registro.getFechaUso() != null || registro.getFechaExpiracion().isBefore(ahora)
                || !"activo".equalsIgnoreCase(registro.getUsuario().getEstado())) {
            throw enlaceInvalido();
        }
        return registro;
    }

    private ResponseStatusException enlaceInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace no es válido o ya expiró");
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
