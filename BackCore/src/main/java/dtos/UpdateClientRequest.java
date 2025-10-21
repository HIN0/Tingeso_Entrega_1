package dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateClientRequest(
    @NotBlank(message = "Name cannot be blank")
    String name,

    // @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone format") // Ejemplo de validación opcional
    @NotBlank(message = "Phone cannot be blank")
    String phone,

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email
) {}