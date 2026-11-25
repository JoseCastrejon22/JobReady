package com.jobready.web.security.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestablecerContrasenaDTO {
    private String token;
    /** Nueva contraseña; se omite al solo validar el enlace. */
    private String password;
}
