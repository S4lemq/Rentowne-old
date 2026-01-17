package pl.rentowne.tenant_settlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.rentowne.tenant_settlement.model.TenantSettlement;

import java.util.Optional;

/**
 * Repozytorium rozliczeń najemcy
 */
public interface TenantSettlementRepository extends JpaRepository<TenantSettlement, Long> {
    Optional<TenantSettlement> findByOrderHash(String orderHash);
}
