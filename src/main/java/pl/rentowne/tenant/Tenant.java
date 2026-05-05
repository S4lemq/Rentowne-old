package pl.rentowne.tenant;

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
import pl.rentowne.lease_agreement.LeaseAgreement;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.tenant_settlement.TenantSettlement;

import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "TENANT",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_TENANT", columnNames = "TENANT_ID")
        },
        indexes = {
                @Index(name = "PK_TENANT", columnList = "TENANT_ID", unique = true),
                @Index(name = "I_TENANT_1", columnList = "EMAIL")
        }
)
public class Tenant extends BaseEntity {

    @Id
    @SequenceGenerator(name = "tenant_seq", sequenceName = "TENANT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_seq")
    @Column(name = "TENANT_ID", nullable = false)
    private Long id;

    @Column(name = "FIRST_NAME", nullable = false, length = 50)
    private String firstname;

    @Column(name = "LAST_NAME", nullable = false, length = 50)
    private String lastname;

    @Column(name = "EMAIL", nullable = false, length = 60)
    private String email;

    @Column(name = "ACCOUNT_NUMBER", length = 28)
    private String accountNumber;

    @Column(name = "PHONE_NUMBER", length = 20)
    private String phoneNumber;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "CIF", nullable = false, length = 10)
    private String cif;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ADDRESS_ID")
    private Address address;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "LEASE_AGREEMENT_ID", unique = true)
    private LeaseAgreement leaseAgreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RENTED_OBJECT_ID")
    private RentedObject rentedObject;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tenant")
    private List<TenantSettlement> tenantSettlements;
}
