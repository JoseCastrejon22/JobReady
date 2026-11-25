package com.jobready.web.security.services;

import com.jobready.web.entidades.TokenRecuperacion;
import com.jobready.web.entidades.Usuario;
import com.jobready.web.repositorios.TokenRecuperacionRepositorio;
import com.jobready.web.repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RecuperacionPasswordServicio {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final UsuarioRepositorio usuarios;
    private final TokenRecuperacionRepositorio tokens;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> correo;
    private final String remitente;
    private final String frontendUrl;

    public RecuperacionPasswordServicio(UsuarioRepositorio usuarios, TokenRecuperacionRepositorio tokens,
                                       PasswordEncoder passwordEncoder, ObjectProvider<JavaMailSender> correo,
                                       @Value("${jobready.mail.from:${spring.mail.username:}}") String remitente,
                                       @Value("${jobready.recuperacion.frontend-url:${ip.frontend}}") String frontendUrl) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.remitente = remitente;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void recuperar(String email) {
        if (email == null || email.length() > 120 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un correo electrónico válido.");
        }
        Usuario usuario = usuarios.findByEmail(email).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No existe ninguna cuenta asociada a este correo electrónico"));
        JavaMailSender mailSender = correo.getIfAvailable();
        if (mailSender == null || remitente.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El correo de recuperación no está disponible.");
        }

        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String secreto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenRecuperacion token = tokens.findById(usuario.getIdUsuario()).orElseGet(TokenRecuperacion::new);
        token.setUsuario(usuario);
        token.setTokenHash(hash(secreto));
        token.setVencimiento(Instant.now().plus(15, ChronoUnit.MINUTES));
        token.setUsado(false);
        tokens.saveAndFlush(token);

        String enlace = UriComponentsBuilder.fromUriString(frontendUrl)
                .pathSegment("restablecer-password").fragment(secreto).build().toUriString();
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(usuario.getEmail());
        mensaje.setSubject("Restablece tu contraseña de JobReady");
        mensaje.setText("Abre este enlace para restablecer tu contraseña. Es válido durante 15 minutos.\n\n"
                + enlace + "\n\nSi no solicitaste este cambio, puedes ignorar este correo.");
        try {
            mailSender.send(mensaje);
        } catch (MailException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo enviar el correo de recuperación.", ex);
        }
    }

    @Transactional
    public void restablecer(String secreto, String password) {
        if (password == null || password.length() < 8 || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La contraseña debe tener al menos 8 caracteres y ocupar hasta 72 bytes.");
        }
        if (secreto == null || !secreto.matches("[A-Za-z0-9_-]{43}")) {
            throw enlaceInvalido();
        }

        String tokenHash = hash(secreto);
        Integer idUsuario = tokens.buscarIdUsuario(tokenHash).orElseThrow(this::enlaceInvalido);
        Usuario usuario = usuarios.buscarParaActualizar(idUsuario).orElseThrow(this::enlaceInvalido);
        TokenRecuperacion token = tokens.findById(idUsuario).orElseThrow(this::enlaceInvalido);
        if (!tokenHash.equals(token.getTokenHash()) || token.isUsado()
                || !token.getVencimiento().isAfter(Instant.now())) {
            throw enlaceInvalido();
        }
        usuario.setPasswordHash(passwordEncoder.encode(password));
        token.setUsado(true);
    }

    private ResponseStatusException enlaceInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace es inválido, ha vencido o ya fue usado.");
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }
}
