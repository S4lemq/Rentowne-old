package pl.rentowne.rental_payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pl.rentowne.lease_agreement.LeaseAgreement;
import pl.rentowne.settlement.CalculateRequestDto;
import pl.rentowne.settlement.Settlement;
import pl.rentowne.settlement.SettlementChargeDto;
import pl.rentowne.settlement.SettlementRepository;
import pl.rentowne.tenant.Tenant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Deposit receipts are part of a transfer, never a second transfer. */
@Service
@RequiredArgsConstructor
public class DepositAccountingService {
    private final RentalPaymentRepository payments;
    private final SettlementRepository settlements;

    public boolean isAssessed(Long tenantId) {
        return payments.findBalances(List.of(tenantId)).getFirst().getDepositAssessed();
    }

    public void initializeLease(LeaseAgreement lease) {
        validatePaid(lease);
        lease.setDepositOpeningPaid(lease.getDepositPaid());
        lease.setDepositBalanceConfirmed(lease.getDepositPaid() != null);
    }

    public void prepareLeaseUpdate(Tenant existing, LeaseAgreement updated) {
        validatePaid(updated);
        LeaseAgreement old = existing.getLeaseAgreement();
        var totals = payments.findBalances(List.of(existing.getId())).getFirst();
        if (totals.getDepositAssessed() && !same(old.getDepositPaid(), updated.getDepositPaid())) {
            throw badRequest("Po naliczeniu dopłaty zmień wpłaconą kaucję przez korektę lub zaksięgowanie wpłaty.");
        }
        BigDecimal received = payments.sumDepositPayments(existing.getId());
        BigDecimal opening = updated.getDepositPaid() == null ? null : updated.getDepositPaid().subtract(received);
        if (opening != null && opening.signum() < 0) {
            throw badRequest("Wpłacona kaucja nie może być mniejsza od zaksięgowanych wpłat na kaucję. Skoryguj historię wpłat.");
        }
        updated.setDepositOpeningPaid(opening);
        updated.setDepositBalanceConfirmed(old.isDepositBalanceConfirmed() || updated.isDepositBalanceConfirmed());
        if (updated.isDepositBalanceConfirmed() && updated.getDepositPaid() == null) {
            throw badRequest("Potwierdź otrzymaną kaucję, wpisując kwotę (również 0 zł).");
        }
    }

    public void applyCharges(Settlement settlement, CalculateRequestDto request) {
        List<SettlementChargeDto> charges = requestedCharges(request);
        BigDecimal deposit = charges.stream().filter(c -> c.type() == SettlementChargeDto.Type.DEPOSIT)
                .map(SettlementChargeDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal extra = charges.stream().filter(c -> c.type() == SettlementChargeDto.Type.OTHER)
                .map(SettlementChargeDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (settlement.getTotalAmount().add(extra).add(deposit).compareTo(new BigDecimal("999999.99")) > 0) {
            throw badRequest("Kwota całkowita rozliczenia przekracza maksymalną kwotę 999 999,99 zł.");
        }
        Tenant tenant = settlement.getTenant();
        LeaseAgreement lease = tenant.getLeaseAgreement();
        var totals = payments.findBalances(List.of(tenant.getId())).getFirst();
        RentalBalanceDto before = RentalBalanceDto.from(totals);
        if (deposit.signum() > 0) {
            if (!lease.isContractActive() || !lease.isDepositBalanceConfirmed() || lease.getDepositPaid() == null) {
                throw badRequest("Przed naliczeniem kaucji potwierdź w aktywnej umowie kwotę faktycznie otrzymanej kaucji.");
            }
            if (deposit.compareTo(before.depositToCharge()) > 0) {
                throw badRequest("Kaucja przekracza kwotę pozostałą do naliczenia. Już naliczona, niezapłacona kaucja pozostaje w saldzie.");
            }
            settlement.setDepositAssessment(true);
            settlement.setDepositTopUp(deposit);
            if (!totals.getDepositAssessed()) settlement.setDepositCarryIn(totals.getDepositPayments());
        }
        charges.forEach(settlement::addCharge);
        settlement.setAdditionalCharge(extra);
        String description = charges.stream().filter(c -> c.type() == SettlementChargeDto.Type.OTHER)
                .map(SettlementChargeDto::description).filter(Objects::nonNull).map(String::strip)
                .filter(s -> !s.isEmpty()).collect(Collectors.joining("; "));
        settlement.setAdditionalChargeDescription(description.length() > 500 ? description.substring(0, 497) + "..." : description);
        settlement.setTotalAmount(settlement.getTotalAmount().add(extra));
        // Only a manually added deposit position can use available credit.
        BigDecimal credit = before.overpayment().subtract(settlement.getTotalAmount()).max(BigDecimal.ZERO).min(deposit);
        if (credit.signum() > 0) allocateExistingCredit(tenant, credit);
        synchronizePaidDeposit(tenant);
        settlement.setTotalAmount(settlement.getTotalAmount().add(deposit));
    }

    private List<SettlementChargeDto> requestedCharges(CalculateRequestDto request) {
        List<SettlementChargeDto> charges = request.additionalCharges();
        if (charges != null && request.additionalCharge() != null && request.additionalCharge().signum() != 0) {
            throw badRequest("Prześlij listę naliczeń albo pojedyncze naliczenie, nie obie wartości jednocześnie.");
        }
        if (charges == null) {
            // Older clients may still send one ordinary charge; this never implies deposit.
            charges = List.of(new SettlementChargeDto(SettlementChargeDto.Type.OTHER,
                    request.additionalCharge(), request.additionalChargeDescription()));
        }
        for (SettlementChargeDto charge : charges) {
            if (charge == null || charge.type() == null || charge.amount() != null && (charge.amount().signum() < 0
                    || charge.amount().scale() > 2 || charge.amount().compareTo(new BigDecimal("999999999999.99")) > 0)
                    || charge.description() != null && charge.description().length() > 500) {
                throw badRequest("Podaj rodzaj i nieujemną kwotę naliczenia z maksymalnie dwoma miejscami po przecinku.");
            }
        }
        return charges.stream().filter(c -> c.amount() != null && c.amount().signum() > 0).toList();
    }

    public void reconcileDeposit(Tenant tenant) {
        var totals = payments.findBalances(List.of(tenant.getId())).getFirst();
        BigDecimal receivedBeyondRemainingBills = totals.getDepositPayments()
                .subtract(totals.getDepositCharges()).subtract(totals.getDepositCarryIn()).max(BigDecimal.ZERO);
        if (totals.getDepositAssessed() && receivedBeyondRemainingBills.signum() > 0) {
            // Removing one of several manual deposit bills must not free money already held
            // as deposit. Preserve it on a remaining bill before another instalment is added.
            Settlement remaining = settlements.findFirstByTenantIdAndRentedObjectIdAndDepositAssessmentTrueOrderByIdAsc(
                    tenant.getId(), tenant.getRentedObject().getId()).orElseThrow();
            remaining.setDepositCarryIn(remaining.getDepositCarryIn().add(receivedBeyondRemainingBills));
            settlements.flush();
            totals = payments.findBalances(List.of(tenant.getId())).getFirst();
        }
        RentalBalanceDto balance = RentalBalanceDto.from(totals);
        // A correction or deletion can release ordinary credit while deposit is still due.
        // Attribute that existing money before proposing the next full transfer.
        BigDecimal credit = balance.depositDue().subtract(balance.amountDue()).max(BigDecimal.ZERO);
        if (balance.depositAssessed() && credit.signum() > 0) allocateExistingCredit(tenant, credit);
        synchronizePaidDeposit(tenant);
    }

    private void allocateExistingCredit(Tenant tenant, BigDecimal credit) {
        var receipts = payments.findByTenantIdAndRentedObjectIdAndDeletedFalseOrderByPaymentDateDescIdDesc(
                tenant.getId(), tenant.getRentedObject().getId());
        for (RentalPayment receipt : receipts.reversed()) {
            BigDecimal allocated = receipt.getAmount().subtract(receipt.getDepositAmount()).min(credit);
            if (allocated.signum() > 0) receipt.setDepositAmount(receipt.getDepositAmount().add(allocated));
            credit = credit.subtract(allocated);
            if (credit.signum() == 0) break;
        }
        payments.flush();
    }

    private void synchronizePaidDeposit(Tenant tenant) {
        LeaseAgreement lease = tenant.getLeaseAgreement();
        BigDecimal opening = lease.getDepositOpeningPaid();
        BigDecimal received = payments.sumDepositPayments(tenant.getId());
        if (opening != null || received.signum() > 0) {
            lease.setDepositPaid((opening == null ? BigDecimal.ZERO : opening).add(received));
        }
    }

    private void validatePaid(LeaseAgreement lease) {
        if (!validMoney(lease.getDeposit()) || lease.getDepositPaid() != null && !validMoney(lease.getDepositPaid())) {
            throw badRequest("Podaj nieujemną kwotę kaucji: maksymalnie 99 999,99 zł i 2 miejsca po przecinku; puste pole wpłaty oznacza brak potwierdzenia.");
        }
    }

    private boolean validMoney(BigDecimal amount) {
        return amount != null && amount.signum() >= 0 && amount.scale() <= 2
                && amount.compareTo(new BigDecimal("99999.99")) <= 0;
    }

    private boolean same(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private RentalPaymentException badRequest(String message) {
        return new RentalPaymentException(HttpStatus.BAD_REQUEST, message);
    }
}
