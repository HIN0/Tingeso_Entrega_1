package dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateClientRequest(
    @NotBlank(message = "Name cannot be blank")
    String name,
    
    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "\\+?\\d{8,12}", message = "Phone must contain 8 to 12 digits")
    String phone,

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email
) {}