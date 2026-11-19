package dtos;

import entities.ClientEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Only the fields a user may set when registering a client (id and status are decided by the system)
public record CreateClientRequest(
    @NotBlank(message = "Name cannot be blank")
    String name,

    @NotBlank(message = "RUT cannot be blank")
    String rut,

    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "\\+?\\d{8,12}", message = "Phone must contain 8 to 12 digits")
    String phone,

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email
) {
    public ClientEntity toEntity() {
        return ClientEntity.builder()
                .name(name.trim())
                .rut(rut)
                .phone(phone)
                .email(email.trim())
                .build();
    }
}
