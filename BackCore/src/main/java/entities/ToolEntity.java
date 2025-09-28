package entities;

import entities.enums.ToolStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    @Enumerated(EnumType.STRING)
    private ToolStatus status;

    private Integer stock;

    @Column(name = "replacement_value")
    private Integer replacementValue;
}
