package pl.rentowne.tenant_settlement;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do powiadomień o statusie płatności
 */
interface TenantSettlementLogRepository extends JpaRepository<TenantSettlementLog, Long> {
}
