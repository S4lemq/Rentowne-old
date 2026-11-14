package pl.rentowne.owner;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.user.User;

@Entity
@Table(name = "PROPERTY_OWNER")
@Getter
@Setter
@NoArgsConstructor
public class Owner extends BaseEntity {
    @Id
    @SequenceGenerator(name = "property_owner_seq", sequenceName = "PROPERTY_OWNER_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "property_owner_seq")
    @Column(name = "OWNER_ID")
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ACCOUNT_ID", nullable = false)
    private User user;

    @Column(name = "FIRST_NAME", nullable = false, length = 50)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 50)
    private String lastName;

    @Column(name = "EMAIL", nullable = false, length = 254)
    private String email;

    @Column(name = "STREET_NAME", length = 100)
    private String streetName;

    @Column(name = "BUILDING_NUMBER", nullable = false, length = 10)
    private String buildingNumber;

    @Column(name = "APARTMENT_NUMBER", length = 10)
    private String apartmentNumber;

    @Column(name = "ZIP_CODE", nullable = false, length = 6)
    private String zipCode;

    @Column(name = "CITY_NAME", nullable = false, length = 60)
    private String cityName;

    @Column(name = "REPRESENTATIVE_NAME", length = 120)
    private String representativeName;

    @Column(name = "REPRESENTATIVE_EMAIL", length = 254)
    private String representativeEmail;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, length = 26)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_ACCOUNT_HOLDER", nullable = false, length = 20)
    private PaymentAccountHolder paymentAccountHolder;
}
