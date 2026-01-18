package pl.rentowne.rented_object;

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
import pl.rentowne.meter.Meter;
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
        name = "rented_object",
        uniqueConstraints = {
                @UniqueConstraint(name = "pk_rented_object", columnNames = "rented_object_id")
        },
        indexes = {
                @Index(name = "pk_rented_object", columnList = "rented_object_id", unique = true)
        }
)
public class RentedObject extends BaseEntity {

    @Id
    @SequenceGenerator(name = "rented_object_seq", sequenceName = "rented_object_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rented_object_seq")
    @Column(name = "rented_object_id", nullable = false)
    private Long id;

    @Column(name = "rented_object_name", nullable = false, length = 60)
    private String rentedObjectName;

    @Column(name = "is_rented", nullable = false)
    private boolean isRented;

    @Column(name = "last_settlement_date")
    private LocalDateTime lastSettlementDate;

    @Column(name = "last_settlement_total_amount")
    private BigDecimal lastSettlementTotalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
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
