package com.template.auth.dto;

import com.template.auth.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    @Email
    @Pattern(regexp = "(?i)^[a-z0-9._%+\\-]+@utp\\.edu\\.pe$", message = "Utiliza tu correo institucional @utp.edu.pe.")
    private String email;

    @NotBlank
    @Size(min = 8)
    @Pattern(
            regexp = PasswordPolicy.REGEX,
            message = PasswordPolicy.MESSAGE
    )
    private String password;
}