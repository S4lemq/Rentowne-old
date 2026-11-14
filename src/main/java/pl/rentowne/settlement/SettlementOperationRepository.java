package pl.rentowne.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementOperationRepository extends JpaRepository<SettlementOperation, SettlementOperation.Id> {
}
