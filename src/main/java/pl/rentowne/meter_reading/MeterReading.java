package pl.rentowne.meter_reading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import pl.rentowne.meter.Meter;
import pl.rentowne.settlement.model.Settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "METER_READING",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_METER_READING", columnNames = "METER_READING_ID"),
        },
        indexes = {
                @Index(name = "PK_METER_READING", columnList = "METER_READING_ID", unique = true),
        }
)
public class MeterReading extends BaseEntity {

    @Id
    @SequenceGenerator(name = "meter_reading_seq", sequenceName = "METER_READING_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meter_reading_seq")
    @Column(name = "METER_READING_ID", nullable = false)
    private Long id;

    @Column(name = "CURRENT_READING", nullable = false, precision = 11, scale = 5)
    private BigDecimal currentReading; //bieżący odczyt w danym miesiącu

    @Column(name = "READING_DATE", nullable = false)
    private LocalDateTime readingDate; //data odczytu

    @Column(name = "CONSUMPTION", nullable = false, precision = 11, scale = 5)
    private BigDecimal consumption; //zużycie w danym miesiącu

    @Column(name = "SETTLED", nullable = false)
    private boolean settled;

    @Column(name = "APARTMENT_SETTLED", nullable = false)
    private boolean apartmentSettled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "METER_ID")
    private Meter meter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SETTLEMENT_ID")
    private Settlement settlement;

    @Column(name = "METER_READING_SETTLEMENT_IDS")
    private Set<Long> meterReadingSettlementIds;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeterReading that = (MeterReading) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
