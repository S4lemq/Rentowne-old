package pl.rentowne.tenant_settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repozytorium rozliczeń najemcy
 */
interface TenantSettlementRepository extends JpaRepository<TenantSettlement, Long> {
    Optional<TenantSettlement> findByOrderHash(String orderHash);
}
