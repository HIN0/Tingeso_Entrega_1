package dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReturnLoanRequest(
    @NotNull Long toolId,
    @NotNull Boolean damaged,
    @NotNull Boolean irreparable,
    // opcional, si quieres registrar la fecha exacta de devolución
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate returnDate
) {}
