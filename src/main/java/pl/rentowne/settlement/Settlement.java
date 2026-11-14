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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.CascadeType;
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
import java.util.List;
import java.util.ArrayList;

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

    @Builder.Default
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<SettlementCharge> charges = new ArrayList<>();

    public void addCharge(SettlementChargeDto dto) {
        charges.add(new SettlementCharge(this, charges.size(), dto));
    }

    public List<SettlementChargeDto> getAdditionalCharges() {
        return charges.stream().map(SettlementCharge::toDto).toList();
    }

    @Builder.Default
    @Column(name = "DEPOSIT_TOP_UP", nullable = false)
    private BigDecimal depositTopUp = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "DEPOSIT_CARRY_IN", nullable = false)
    private BigDecimal depositCarryIn = BigDecimal.ZERO;

    @Column(name = "DEPOSIT_ASSESSMENT", nullable = false)
    private boolean depositAssessment;

    @Builder.Default
    @Column(name = "ADDITIONAL_CHARGE", nullable = false)
    private BigDecimal additionalCharge = BigDecimal.ZERO;

    @Column(name = "ADDITIONAL_CHARGE_DESCRIPTION", length = 500)
    private String additionalChargeDescription;
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

