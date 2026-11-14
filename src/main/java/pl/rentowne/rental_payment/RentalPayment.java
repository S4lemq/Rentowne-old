package pl.rentowne.rental_payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.tenant.Tenant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "RENTAL_PAYMENT")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RentalPayment extends BaseEntity {

    @Id
    @SequenceGenerator(name = "rental_payment_seq", sequenceName = "RENTAL_PAYMENT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rental_payment_seq")
    @Column(name = "RENTAL_PAYMENT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RENTED_OBJECT_ID", nullable = false)
    private RentedObject rentedObject;

    @Column(name = "AMOUNT", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "PAYMENT_DATE", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "REQUEST_ID", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "REQUEST_PAYLOAD", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
