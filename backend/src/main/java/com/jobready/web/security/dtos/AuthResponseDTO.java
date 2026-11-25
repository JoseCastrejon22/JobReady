package com.jobready.web.security.dtos;

import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/** Cuerpo de /api/authenticate: el token va solo en la cabecera Authorization. */
public class AuthResponseDTO {
    private Set<String> roles;
}
