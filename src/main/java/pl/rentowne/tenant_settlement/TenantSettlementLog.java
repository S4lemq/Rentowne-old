package pl.rentowne.tenant_settlement;

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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "TENANT_SETTLEMENT_LOG",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_TENANT_SETTLEMENT_LOG", columnNames = "TENANT_SETTLEMENT_LOG_ID")
        },
        indexes = {
                @Index(name = "PK_TENANT_SETTLEMENT_LOG", columnList = "TENANT_SETTLEMENT_LOG_ID", unique = true)
        }
)
class TenantSettlementLog {
    @Id
    @SequenceGenerator(name = "tenant_settlement_log_seq", sequenceName = "TENANT_SETTLEMENT_LOG_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_settlement_log_seq")
    @Column(name = "TENANT_SETTLEMENT_LOG_ID", nullable = false)
    private Long id;

    @Column(name = "TENANT_SETTLEMENT_ID")
    private Long tenantSettlementId;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED")
    private LocalDateTime created;
}
