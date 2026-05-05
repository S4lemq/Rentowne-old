package pl.rentowne.tenant_settlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.common.model.BaseEntity;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "PAYMENT",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_PAYMENT", columnNames = "PAYMENT_ID")
        },
        indexes = {
                @Index(name = "PK_PAYMENT", columnList = "PAYMENT_ID", unique = true)
        }
)
public class Payment extends BaseEntity {
    @Id
    @SequenceGenerator(name = "payment_seq", sequenceName = "PAYMENT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    @Column(name = "PAYMENT_ID", nullable = false)
    private Long id;

    @Column(name = "NAME", length = 64, nullable = false)
    private String name;

    @Column(name = "TYPE", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType type;

    @Column(name = "DEFAULT_PAYMENT", nullable = false)
    private boolean defaultPayment;

    @Column(name = "NOTE", nullable = false)
    private String note;

}
