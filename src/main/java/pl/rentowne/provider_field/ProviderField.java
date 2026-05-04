package pl.rentowne.provider_field;

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
import pl.rentowne.housing_provider.HousingProvider;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "PROVIDER_FIELD",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_PROVIDER_FIELD", columnNames = "PROVIDER_FIELD_ID"),
        },
        indexes = {
                @Index(name = "PK_PROVIDER_FIELD", columnList = "PROVIDER_FIELD_ID", unique = true),
        }
)
public class ProviderField extends BaseEntity {

    @Id
    @SequenceGenerator(name = "provider_field_seq", sequenceName = "PROVIDER_FIELD_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "provider_field_seq")
    @Column(name = "PROVIDER_FIELD_ID", nullable = false)
    private Long id;

    @Column(name = "NAME", length = 60)
    private String name;

    @Column(name = "PRICE", nullable = false)
    private BigDecimal price;

    @Column(name = "BILLING_METHOD", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private BillingMethod billingMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HOUSING_PROVIDER_ID")
    private HousingProvider housingProvider;
}
