package entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tariffs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TariffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double dailyRentFee;   // Tarifa diaria de arriendo
    private Double dailyLateFee;   // Tarifa diaria de multa
    private Double repairFee;      // Cargo por reparación leve
}
