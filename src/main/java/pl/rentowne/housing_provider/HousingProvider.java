package pl.rentowne.housing_provider;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
import pl.rentowne.apartment.Apartment;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.provider_field.ProviderField;
import pl.rentowne.user.User;

import java.math.BigDecimal;
import java.util.HashSet;
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
        name = "housing_provider",
        uniqueConstraints = {
                @UniqueConstraint(name = "pk_housing_provider", columnNames = "housing_provider_id"),
        },
        indexes = {
                @Index(name = "pk_housing_provider", columnList = "housing_provider_id", unique = true),
        }
)
public class HousingProvider extends BaseEntity {

    @Id
    @SequenceGenerator(name = "housing_provider_seq", sequenceName = "housing_provider_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "housing_provider_seq")
    @Column(name = "housing_provider_id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 45)
    private String name;

    @Column(name = "type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ProviderType type;

    @Column(name = "tax")
    private BigDecimal tax;

    @Column(name = "conversion_rate")
    private BigDecimal conversionRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id")
    private User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "housingProvider", cascade = CascadeType.ALL)
    private List<ProviderField> providerFields;

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "apartment_housing_provider",
            joinColumns = {@JoinColumn(name = "housing_provider_id")},
            inverseJoinColumns = {@JoinColumn(name = "apartment_id")}
    )
    private Set<Apartment> apartments = new HashSet<>();

    @Column(name = "recurring_payment", nullable = false)
    private boolean recurringPayment = false;

    public void addApartment(Apartment apartment) {
        apartments.add(apartment);
        apartment.getHousingProviders().add(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HousingProvider that = (HousingProvider) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
