package pl.rentowne.address;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "address",
        uniqueConstraints = {
                @UniqueConstraint(name = "pk_address", columnNames = "address_id")
        },
        indexes = {
                @Index(name = "pk_address", columnList = "address_id", unique = true)
        }
)
public class Address extends BaseEntity {

    @Id
    @SequenceGenerator(name = "address_seq", sequenceName = "ADDRESS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_seq")
    @Column(name = "address_id", nullable = false)
    private Long id;

    @Column(name = "street_name", length = 100)
    private String streetName;

    @Column(name = "building_number", nullable = false, length = 10)
    private String buildingNumber;

    @Column(name = "apartment_number", length = 10)
    private String apartmentNumber;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(name = "city_name", nullable = false, length = 60)
    private String cityName;

    @Column(name = "voivode_ship", nullable = false, length = 60)
    private String voivodeship;
}
