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

    public Map<Long, RentalBalanceDto> getBalances(Collection<Long> tenantIds) {
        if (tenantIds.isEmpty()) return Map.of();
        // Filter by owner even for the batch used by finance cards.
        List<Long> ownedIds = tenantRepository.findOwnedTenantIds(tenantIds, userService.getLoggedUser().id());
        if (ownedIds.isEmpty()) return Map.of();
        return paymentRepository.findBalances(ownedIds).stream().collect(Collectors.toMap(
                RentalPaymentRepository.BalanceTotals::getTenantId,
                t -> RentalBalanceDto.from(t.getTotalCharges(), t.getHistoricalCredit(), t.getTotalPayments())));
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

        BigDecimal amount = request.mode() == CreateRentalPaymentRequest.Mode.FULL
                ? balance(tenantId).amountDue() : request.amount();
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new RentalPaymentException(HttpStatus.BAD_REQUEST,
                    "Podaj dodatnią kwotę z maksymalnie dwoma miejscami po przecinku. Pełna wpłata wymaga zadłużenia.");
        }
        RentalPayment payment = new RentalPayment();
        payment.setTenant(tenant);
        payment.setRentedObject(tenant.getRentedObject());
        payment.setAmount(amount.setScale(2, RoundingMode.UNNECESSARY));
        payment.setPaymentDate(request.paymentDate());
        payment.setDescription(request.normalizedDescription());
        payment.setRequestId(request.requestId());
        payment.setRequestPayload(request.payload());
        return RentalPaymentDto.from(paymentRepository.saveAndFlush(payment));
    }

    @Transactional
    RentalPaymentDto update(Long tenantId, Long rentedObjectId, Long id, UpdateRentalPaymentRequest request) {
        requireTenancy(tenantId, rentedObjectId, true);
        RentalPayment payment = requirePayment(tenantId, rentedObjectId, id);
        checkVersion(payment, request.version());
        payment.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        payment.setPaymentDate(request.paymentDate());
        payment.setDescription(request.description() == null ? "" : request.description().strip());
        return RentalPaymentDto.from(paymentRepository.saveAndFlush(payment));
    }

    @Transactional
    void delete(Long tenantId, Long rentedObjectId, Long id, Long version) {
        requireTenancy(tenantId, rentedObjectId, true);
        RentalPayment payment = requirePayment(tenantId, rentedObjectId, id);
        checkVersion(payment, version);
        payment.setDeleted(true);
        paymentRepository.saveAndFlush(payment);
    }

    private RentalBalanceDto balance(Long tenantId) {
        var totals = paymentRepository.findBalances(List.of(tenantId)).getFirst();
        return RentalBalanceDto.from(totals.getTotalCharges(), totals.getHistoricalCredit(), totals.getTotalPayments());
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
