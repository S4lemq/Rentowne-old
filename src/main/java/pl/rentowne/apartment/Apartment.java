package pl.rentowne.apartment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.address.Address;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.housing_provider.HousingProvider;
import pl.rentowne.meter.Meter;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.user.User;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "apartment",
        uniqueConstraints = {
                @UniqueConstraint(name = "pk_apartment", columnNames = "apartment_id"),
                @UniqueConstraint(name = "uk_apartment_1", columnNames = "address_id")
        },
        indexes = {
                @Index(name = "pk_apartment", columnList = "apartment_id", unique = true),
                @Index(name = "uk_apartment_1", columnList = "address_id", unique = true),
                @Index(name = "i_apartment_1", columnList = "apartment_name"),
        }
)
public class Apartment extends BaseEntity {

    @Id
    @SequenceGenerator(name = "apartment_seq", sequenceName = "apartment_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_seq")
    @Column(name = "apartment_id", nullable = false)
    private Long id;

    @Column(name = "apartment_name", nullable = false, length = 60)
    private String apartmentName;

    @Column(name = "leases_number", nullable = false)
    private BigInteger leasesNumber;

    @Column(name = "area", nullable = false)
    private BigDecimal area;

    @Column(name = "is_rented", nullable = false)
    private boolean isRented;

    @Column(name = "image", length = 128)
    private String image;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", unique = true)
    private Address address;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "apartment")
    private Set<RentedObject> rentedObjects;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "apartment")
    private Set<Meter> meters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id")
    private User user;

    @ManyToMany(mappedBy = "apartments", fetch = FetchType.LAZY)
    private Set<HousingProvider> housingProviders = new HashSet<>();

    public Apartment(Long id) {
        this.id = id;
    }

    //set nie zadziała, tzn gdy dodajesz nowe obiekty, wtedy one mają id = null, jak trzeba dodaj UUID lub nowy uniqueContraint z polem
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Apartment apartment = (Apartment) o;
        return Objects.equals(id, apartment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
