package pl.rentowne.settlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.tenant.Tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "SETTLEMENT",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_SETTLEMENT", columnNames = "SETTLEMENT_ID")
        },
        indexes = {
                @Index(name = "PK_SETTLEMENT", columnList = "SETTLEMENT_ID", unique = true),
                @Index(name = "UK_SETTLEMENT_1", columnList = "HASH", unique = true),

        }
)
public class Settlement extends BaseEntity {
    @Id
    @SequenceGenerator(name = "settlement_seq", sequenceName = "SETTLEMENT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settlement_seq")
    @Column(name = "SETTLEMENT_ID", nullable = false)
    private Long id;

    @Column(name = "ELECTRICITY_AMOUNT")
    private BigDecimal electricityAmount;

    @Column(name = "DISTRIBUTED_ELECTRICITY_AMOUNT")
    private BigDecimal distributedElectricityAmount;

    @Column(name = "WATER_AMOUNT")
    private BigDecimal waterAmount;

    @Column(name = "GAS_AMOUNT")
    private BigDecimal gasAmount;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "COMPENSATION_AMOUNT", nullable = false)
    private BigDecimal compensationAmount; //kwota odstępnego

    @Column(name = "RENT_AMOUNT", nullable = false)
    private BigDecimal rentAmount; //kwota czynszu

    @Column(name = "INTERNET_FEE", nullable = false)
    private BigDecimal internetFee; //kwota za internet

    @Column(name = "GAS_DEPOSIT")
    private BigDecimal gasDeposit; //opłata za gaz

    @Column(name = "SETTLEMENT_DATE")
    private LocalDateTime settlementDate;

    @Column(name = "HASH", length = 12, unique = true)
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RENTED_OBJECT_ID")
    private RentedObject rentedObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID")
    private Tenant tenant;

    @Column(name = "STATUS", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;

    @Column(name = "PRORATION_DESCRIPTION", columnDefinition = "TEXT")
    private String prorationDescription;

}

