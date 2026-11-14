package pl.rentowne.settlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "SETTLEMENT_CHARGE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SettlementCharge {
    @Id
    @SequenceGenerator(name = "settlement_charge_seq", sequenceName = "SETTLEMENT_CHARGE_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settlement_charge_seq")
    @Column(name = "SETTLEMENT_CHARGE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SETTLEMENT_ID", nullable = false)
    private Settlement settlement;

    @Column(name = "POSITION", nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private SettlementChargeDto.Type type;

    @Column(name = "AMOUNT", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    SettlementCharge(Settlement settlement, int position, SettlementChargeDto dto) {
        this.settlement = settlement;
        this.position = position;
        this.type = dto.type();
        this.amount = dto.amount();
        this.description = dto.description() == null ? "" : dto.description().strip();
    }

    SettlementChargeDto toDto() {
        return new SettlementChargeDto(type, amount, description);
    }
}
