package entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tariffs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false)
    private Double dailyRate; // tarifa diaria de arriendo

    @Column(nullable = false)
    private Double lateFeeRate; // tarifa diaria de multa por atraso

    @Column
    private Double repairFee; // opcional, cargos por reparación leve
}
