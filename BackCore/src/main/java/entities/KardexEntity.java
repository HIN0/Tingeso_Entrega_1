package entities;

import entities.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kardex")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KardexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tool_id")
    private ToolEntity tool;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    @Column(name = "movement_date")
    private LocalDateTime date;

    private Integer quantity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
