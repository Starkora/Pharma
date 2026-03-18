package com.pharmasys.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClienteRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String apellido;

    @Pattern(regexp = "^$|^[0-9]{8,15}$", message = "El DNI debe contener solo números")
    @Size(max = 20, message = "El DNI no puede exceder 20 caracteres")
    private String dni;

    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccion;

    @Pattern(regexp = "^$|^[0-9+\\-()\\s]{6,20}$", message = "Formato de teléfono inválido")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Email(message = "Formato de correo inválido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String email;

    private LocalDate fechaNacimiento;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;
}
