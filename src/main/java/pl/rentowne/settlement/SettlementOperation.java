package pl.rentowne.settlement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "SETTLEMENT_OPERATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SettlementOperation {
    @EmbeddedId
    private Id id;

    @Column(name = "PAYLOAD", nullable = false, columnDefinition = "TEXT")
    private String payload;

    // The database clears this association on settlement deletion, retaining the request ID.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SETTLEMENT_ID")
    private Settlement settlement;

    SettlementOperation(Long tenantId, UUID requestId, String payload, Settlement settlement) {
        this.id = new Id(tenantId, requestId);
        this.payload = payload;
        this.settlement = settlement;
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Id implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "TENANT_ID", nullable = false)
        private Long tenantId;

        @Column(name = "REQUEST_ID", nullable = false)
        private UUID requestId;
    }
}
