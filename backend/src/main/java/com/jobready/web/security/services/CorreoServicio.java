package com.jobready.web.security.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Envía los correos de la plataforma. Se ejecuta en segundo plano: la respuesta al usuario no espera
 * al servidor SMTP ni deja ver, por su demora, si el correo pertenece a una cuenta.
 */
@Service
public class CorreoServicio {
    private static final Logger log = LoggerFactory.getLogger(CorreoServicio.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;
    private final String usuarioSmtp;

    public CorreoServicio(ObjectProvider<JavaMailSender> mailSender,
                          @Value("${jobready.mail.from:}") String remitente,
                          @Value("${spring.mail.username:}") String usuarioSmtp) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.usuarioSmtp = usuarioSmtp;
    }

    @Async
    public void enviarRecuperacionContrasena(String destinatario, String nombre, String enlace, int minutosValidez) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || usuarioSmtp.isBlank()) {
            // Sin SMTP configurado (desarrollo local) el enlace solo se registra en el log del servidor.
            log.warn("Correo no configurado (MAIL_USERNAME/MAIL_PASSWORD). Enlace de recuperación para {}: {}",
                    destinatario, enlace);
            return;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente.isBlank() ? usuarioSmtp : remitente);
            helper.setTo(destinatario);
            helper.setSubject("JobReady: restablece tu contraseña");
            helper.setText(textoPlano(nombre, enlace, minutosValidez), html(nombre, enlace, minutosValidez));
            sender.send(mensaje);
            log.info("Correo de recuperación enviado a {}", destinatario);
        } catch (MessagingException | MailException e) {
            log.error("No se pudo enviar el correo de recuperación a {}: {}", destinatario, e.getMessage());
        }
    }

    private String textoPlano(String nombre, String enlace, int minutos) {
        return "Hola " + nombre + ",\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de JobReady.\n"
                + "Abre este enlace para elegir una nueva contraseña (vence en " + minutos + " minutos):\n\n"
                + enlace + "\n\n"
                + "Si no solicitaste el cambio, ignora este correo: tu contraseña actual sigue siendo válida.\n";
    }

    private String html(String nombre, String enlace, int minutos) {
        String n = HtmlUtils.htmlEscape(nombre);
        String e = HtmlUtils.htmlEscape(enlace);
        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#1a1a1a">
                  <h2 style="margin:0 0 16px"><span style="color:#111">Job</span><span style="color:#2b77f2">Ready</span></h2>
                  <p>Hola %s,</p>
                  <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta.</p>
                  <p style="text-align:center;margin:28px 0">
                    <a href="%s" style="background:#1e50df;color:#fff;padding:12px 26px;border-radius:999px;text-decoration:none;font-weight:bold">Restablecer contraseña</a>
                  </p>
                  <p style="font-size:13px;color:#555">El enlace vence en %d minutos y solo puede usarse una vez.
                  Si el botón no funciona, copia esta dirección en tu navegador:<br><a href="%s">%s</a></p>
                  <p style="font-size:13px;color:#555">Si no solicitaste el cambio, ignora este correo: tu contraseña actual sigue siendo válida.</p>
                </div>
                """.formatted(n, e, minutos, e, e);
    }
}
