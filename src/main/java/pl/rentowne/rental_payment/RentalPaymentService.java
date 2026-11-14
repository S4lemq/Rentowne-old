package pl.rentowne.rental_payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import pl.rentowne.tenant.Tenant;
import pl.rentowne.tenant.TenantRepository;
import pl.rentowne.user.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalPaymentService {
    private final RentalPaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;
    private final UserService userService;
    private final DepositAccountingService deposits;

    public Map<Long, RentalBalanceDto> getBalances(Collection<Long> tenantIds) {
        if (tenantIds.isEmpty()) return Map.of();
        // Filter by owner even for the batch used by finance cards.
        List<Long> ownedIds = tenantRepository.findOwnedTenantIds(tenantIds, userService.getLoggedUser().id());
        if (ownedIds.isEmpty()) return Map.of();
        return paymentRepository.findBalances(ownedIds).stream().collect(Collectors.toMap(
                RentalPaymentRepository.BalanceTotals::getTenantId,
                RentalBalanceDto::from));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    RentalPaymentHistoryDto getHistory(Long tenantId, Long rentedObjectId) {
        requireTenancy(tenantId, rentedObjectId, false);
        return new RentalPaymentHistoryDto(balance(tenantId), paymentRepository
                .findByTenantIdAndRentedObjectIdAndDeletedFalseOrderByPaymentDateDescIdDesc(tenantId, rentedObjectId)
                .stream().map(RentalPaymentDto::from).toList());
    }

    @Transactional
    RentalPaymentDto create(Long tenantId, Long rentedObjectId, CreateRentalPaymentRequest request) {
        Tenant tenant = requireTenancy(tenantId, rentedObjectId, true);
        var previous = paymentRepository.findByTenantIdAndRequestId(tenantId, request.requestId());
        if (previous.isPresent()) {
            RentalPayment payment = previous.get();
            if (payment.isDeleted() || !payment.getRentedObject().getId().equals(rentedObjectId)
                    || !payment.getRequestPayload().equals(request.payload())) {
                throw new RentalPaymentException(HttpStatus.CONFLICT, "Ta operacja została już użyta. Odśwież historię wpłat.");
            }
            return RentalPaymentDto.from(payment);
        }

        RentalBalanceDto before = balance(tenantId);
        BigDecimal amount = request.mode() == CreateRentalPaymentRequest.Mode.FULL
                ? before.amountDue() : request.amount();
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new RentalPaymentException(HttpStatus.BAD_REQUEST,
                    "Podaj dodatnią kwotę z maksymalnie dwoma miejscami po przecinku. Pełna wpłata wymaga zadłużenia.");
        }
        RentalPayment payment = new RentalPayment();
        payment.setTenant(tenant);
        payment.setRentedObject(tenant.getRentedObject());
        payment.setAmount(amount.setScale(2, RoundingMode.UNNECESSARY));
        payment.setDepositAmount(allocateDeposit(before, amount, request.depositAmount(), BigDecimal.ZERO,
                request.mode() == CreateRentalPaymentRequest.Mode.FULL));
        payment.setPaymentDate(request.paymentDate());
        payment.setDescription(request.normalizedDescription());
        payment.setRequestId(request.requestId());
        payment.setRequestPayload(request.payload());
        paymentRepository.saveAndFlush(payment);
        deposits.reconcileDeposit(tenant);
        return RentalPaymentDto.from(payment);
    }

    @Transactional
    RentalPaymentDto update(Long tenantId, Long rentedObjectId, Long id, UpdateRentalPaymentRequest request) {
        Tenant tenant = requireTenancy(tenantId, rentedObjectId, true);
        RentalPayment payment = requirePayment(tenantId, rentedObjectId, id);
        checkVersion(payment, request.version());
        var totals = paymentRepository.findBalances(List.of(tenantId)).getFirst();
        RentalBalanceDto before = RentalBalanceDto.excluding(totals, payment.getAmount(), payment.getDepositAmount());
        payment.setDepositAmount(allocateDeposit(before, request.amount(), request.depositAmount(), payment.getDepositAmount(), false));
        payment.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        payment.setPaymentDate(request.paymentDate());
        payment.setDescription(request.description() == null ? "" : request.description().strip());
        paymentRepository.saveAndFlush(payment);
        deposits.reconcileDeposit(tenant);
        return RentalPaymentDto.from(payment);
    }

    @Transactional
    void delete(Long tenantId, Long rentedObjectId, Long id, Long version) {
        Tenant tenant = requireTenancy(tenantId, rentedObjectId, true);
        RentalPayment payment = requirePayment(tenantId, rentedObjectId, id);
        checkVersion(payment, version);
        payment.setDeleted(true);
        paymentRepository.saveAndFlush(payment);
        deposits.reconcileDeposit(tenant);
    }

    private RentalBalanceDto balance(Long tenantId) {
        var totals = paymentRepository.findBalances(List.of(tenantId)).getFirst();
        return RentalBalanceDto.from(totals);
    }

    private BigDecimal allocateDeposit(RentalBalanceDto before, BigDecimal amount, BigDecimal requested,
                                       BigDecimal previousDeposit, boolean full) {
        BigDecimal due = before.depositDue();
        if (!before.depositAssessed()) {
            BigDecimal deposit = requested == null ? previousDeposit.min(amount) : requested;
            if (deposit.signum() < 0 || deposit.compareTo(previousDeposit.min(amount)) > 0) {
                throw new RentalPaymentException(HttpStatus.BAD_REQUEST, "Brak naliczonej kaucji. Potwierdź otrzymaną kwotę w umowie.");
            }
            return deposit;
        }
        BigDecimal uncoveredDeposit = due.min(before.amountDue());
        BigDecimal minimum = amount.subtract(before.amountDue().subtract(uncoveredDeposit).max(BigDecimal.ZERO))
                .max(BigDecimal.ZERO).min(uncoveredDeposit);
        if (requested != null && (requested.signum() < 0 || requested.scale() > 2 || requested.compareTo(amount.min(due)) > 0)) {
            throw new RentalPaymentException(HttpStatus.BAD_REQUEST, "Część na kaucję nie może przekraczać kwoty przelewu ani zaległej kaucji.");
        }
        // The current UI books a total amount only. Retain an existing assignment on
        // corrections, bounded by the corrected transfer and the outstanding component.
        BigDecimal deposit = requested == null ? previousDeposit.min(amount).min(due).max(minimum) : requested;
        if (full) {
            deposit = uncoveredDeposit;
        } else if (amount.compareTo(before.amountDue()) >= 0) {
            deposit = deposit.max(uncoveredDeposit).min(amount).min(due);
        }
        if (deposit.signum() < 0 || deposit.scale() > 2 || deposit.compareTo(amount.min(due)) > 0 || deposit.compareTo(minimum) < 0) {
            throw new RentalPaymentException(HttpStatus.BAD_REQUEST,
                    "Część na kaucję musi mieścić się w przelewie i zaległej kaucji oraz pokrywać nadwyżkę ponad pozostałe należności.");
        }
        return deposit;
    }

    private Tenant requireTenancy(Long tenantId, Long rentedObjectId, boolean lock) {
        Tenant tenant = (lock ? tenantRepository.lockById(tenantId) : tenantRepository.findById(tenantId))
                .orElseThrow(() -> new RentalPaymentException(HttpStatus.NOT_FOUND, "Nie znaleziono najmu."));
        if (tenant.getRentedObject() == null || !tenant.getRentedObject().getId().equals(rentedObjectId)
                || !tenant.getRentedObject().getApartment().getUser().getId().equals(userService.getLoggedUser().id())) {
            throw new RentalPaymentException(HttpStatus.NOT_FOUND, "Nie znaleziono najmu.");
        }
        return tenant;
    }

    private RentalPayment requirePayment(Long tenantId, Long rentedObjectId, Long id) {
        return paymentRepository.findByIdAndTenantIdAndRentedObjectIdAndDeletedFalse(id, tenantId, rentedObjectId)
                .orElseThrow(() -> new RentalPaymentException(HttpStatus.NOT_FOUND, "Nie znaleziono wpłaty. Odśwież historię."));
    }

    private void checkVersion(RentalPayment payment, Long version) {
        if (!Objects.equals(payment.getVersion(), version)) {
            throw new RentalPaymentException(HttpStatus.CONFLICT, "Wpłata została już zmieniona. Odśwież historię przed korektą.");
        }
    }

    record RentalPaymentHistoryDto(RentalBalanceDto summary, List<RentalPaymentDto> payments) {}
}
