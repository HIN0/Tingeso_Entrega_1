package dtos;

import entities.ToolEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Only the fields a user may set when registering a tool (status and units in repair are decided by the system)
public record CreateToolRequest(
    @NotBlank(message = "Name cannot be blank")
    String name,

    @NotBlank(message = "Category cannot be blank")
    String category,

    @NotNull(message = "Stock cannot be null")
    @Min(value = 0, message = "Stock cannot be negative")
    Integer stock,

    @NotNull(message = "Replacement value cannot be null")
    @Min(value = 1000, message = "Replacement value must be at least 1000")
    Integer replacementValue
) {
    public ToolEntity toEntity() {
        return ToolEntity.builder()
                .name(name.trim())
                .category(category.trim())
                .stock(stock)
                .replacementValue(replacementValue)
                .build();
    }
}
