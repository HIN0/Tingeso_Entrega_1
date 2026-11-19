package dtos;

import entities.TariffEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Daily tariffs configured by the administrator (Epic 4)
public record TariffRequest(
    @NotNull(message = "Daily rent fee is required")
    @Min(value = 1, message = "Daily rent fee must be at least 1")
    Integer dailyRentFee,

    @NotNull(message = "Daily late fee is required")
    @Min(value = 0, message = "Daily late fee cannot be negative")
    Integer dailyLateFee,

    @NotNull(message = "Repair fee is required")
    @Min(value = 0, message = "Repair fee cannot be negative")
    Integer repairFee
) {
    public TariffEntity toEntity() {
        return TariffEntity.builder()
                .dailyRentFee(dailyRentFee)
                .dailyLateFee(dailyLateFee)
                .repairFee(repairFee)
                .build();
    }
}
