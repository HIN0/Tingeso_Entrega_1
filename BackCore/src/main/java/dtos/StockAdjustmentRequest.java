package dtos;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
    @NotNull(message = "Quantity change cannot be null")
    Integer quantityChange // positive (add units) or negative (remove units)
) {}