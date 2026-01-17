package pl.rentowne.rented_object.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.meter.model.Meter;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.tenant.model.Tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "RENTED_OBJECT",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_RENTED_OBJECT", columnNames = "RENTED_OBJECT_ID")
        },
        indexes = {
                @Index(name = "PK_RENTED_OBJECT", columnList = "RENTED_OBJECT_ID", unique = true)
        }
)
public class RentedObject extends BaseEntity {

    @Id
    @SequenceGenerator(name = "rented_object_seq", sequenceName = "RENTED_OBJECT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rented_object_seq")
    @Column(name = "RENTED_OBJECT_ID", nullable = false)
    private Long id;

    @Column(name = "RENTED_OBJECT_NAME", nullable = false, length = 60)
    private String rentedObjectName;

    @Column(name = "IS_RENTED", nullable = false)
    private boolean isRented;

    @Column(name = "LAST_SETTLEMENT_DATE")
    private LocalDateTime lastSettlementDate;

    @Column(name = "LAST_SETTLEMENT_TOTAL_AMOUNT")
    private BigDecimal lastSettlementTotalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APARTMENT_ID")
    private Apartment apartment;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "rentedObject")
    private Set<Meter> meters;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "rentedObject", cascade = CascadeType.PERSIST)
    private List<Settlement> settlements;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "rentedObject")
    @SQLRestriction("ACTIVE = true")
    private List<Tenant> tenants;

    public RentedObject(Long id) {
        this.id = id;
    }

    /**
     * Zwraca aktywnego tenant'a (jeśli istnieje)
     * @return aktywny tenant lub null
     */
    public Tenant getActiveTenant() {
        if (tenants == null || tenants.isEmpty()) {
            return null;
        }
        // @SQLRestriction powinno już filtrować tylko aktywnych, więc zwracamy pierwszego
        return tenants.getFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RentedObject that = (RentedObject) o;
        return Objects.equals(id, that.id) && Objects.equals(rentedObjectName, that.rentedObjectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rentedObjectName);
    }
}
