package pl.rentowne.rented_object.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.lease_agreement.model.LeaseAgreement;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.rented_object.model.dto.ProrationCheckResponseDto;
import pl.rentowne.rented_object.model.dto.RentedObjectDetailsDto;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;
import pl.rentowne.rented_object.repository.RentedObjectRepository;
import pl.rentowne.rented_object.service.RentedObjectService;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.repository.SettlementRepository;
import pl.rentowne.tenant.model.Tenant;
import pl.rentowne.tenant.repository.TenantRepository;
import pl.rentowne.user.service.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.rentowne.util.DateUtils.formatPolishDate;

@Service
@RequiredArgsConstructor
public class RentedObjectServiceImpl implements RentedObjectService {

    private final RentedObjectRepository rentedObjectRepository;
    private final SettlementRepository settlementRepository;
    private final TenantRepository tenantRepository;
    private final UserService userService;

    @Override
    public void saveAll(Set<RentedObject> rentedObjects) {
        rentedObjectRepository.saveAll(rentedObjects);
    }

    @Override
    public List<RentedObject> getAllByApartmentId(Long apartmentId) {
        return rentedObjectRepository.getAllByApartmentId(apartmentId);
    }

    @Override
    public void deleteAll(List<RentedObject> toDelete) {
        rentedObjectRepository.deleteAll(toDelete);
    }

    @Override
    public void deleteAllByApartmentId(Long apartmentId) {
        rentedObjectRepository.deleteAllByApartmentId(apartmentId);
    }

    @Override
    public List<RentedObjectDto> getAllRentedObjectDtosWithMeters(Long apartmentId) {
        List<RentedObject> rentedObjects = rentedObjectRepository.findByApartmentIdWithMeters(apartmentId);
        return this.mapToDtos(rentedObjects);
    }

    @Override
    public List<RentedObjectDto> getAllRentedObjectDtos() throws RentowneBusinessException {
        Long userId = userService.getLoggedUser().getId();
        return rentedObjectRepository.getAllRentedObjectDtos(userId);
    }

    @Override
    public List<RentedObjectDto> getAllByApartmentAndOptionalRentedObject(Long apartmentId, Long rentedObjectId) throws RentowneBusinessException {
        return rentedObjectRepository.getAllByApartmentAndOptionalRentedObject(apartmentId, rentedObjectId);
    }

    @Override
    public List<RentedObjectDetailsDto> getRentedObjectsDetails(Long apartmentId, Boolean active) {
        Long userId = userService.getLoggedUser().getId();
        List<Tenant> tenants = tenantRepository.findTenantsWithDetailsForUser(userId, apartmentId, active);

        return tenants.stream()
                .map(this::mapToDetailsDto)
                .collect(Collectors.toList());
    }

    private RentedObjectDetailsDto mapToDetailsDto(Tenant tenant) {
        Long tenantId = tenant.getId();
        Long rentedObjectId = tenant.getRentedObject().getId();
        Settlement latestSettlement = settlementRepository.findLatestByRentedObjectId(rentedObjectId, tenantId);

        String tenantFullName = tenant.getFirstname() + " " + tenant.getLastname();
        String rentedObjectName = tenant.getRentedObject().getRentedObjectName();

        BigDecimal rentAmount = latestSettlement != null ? latestSettlement.getRentAmount() : null;
        BigDecimal internetFee = latestSettlement != null ? latestSettlement.getInternetFee() : null;
        BigDecimal gasDeposit = latestSettlement != null ? latestSettlement.getGasDeposit() : null;
        BigDecimal electricityAmount = latestSettlement != null ? latestSettlement.getElectricityAmount() : null;
        BigDecimal waterAmount = latestSettlement != null ? latestSettlement.getWaterAmount() : null;
        BigDecimal compensationAmount = latestSettlement != null ? latestSettlement.getCompensationAmount() : null;
        LocalDateTime lastSettlementDate = latestSettlement != null ? latestSettlement.getSettlementDate() : null;

        BigDecimal totalAmount = sumAmounts(electricityAmount, waterAmount, compensationAmount, internetFee, rentAmount, gasDeposit);
        BigDecimal sum = sumAmounts(rentAmount, internetFee, gasDeposit, electricityAmount, waterAmount);
        BigDecimal profit = calculateProfit(compensationAmount);
        boolean hasCurrentMonthSettlement = checkCurrentMonthSettlement(latestSettlement);

        return new RentedObjectDetailsDto(
                rentedObjectId,
                rentedObjectName,
                tenantFullName,
                tenantId,
                formatPolishDate(lastSettlementDate),
                totalAmount.compareTo(BigDecimal.ZERO) > 0 ? totalAmount : null,
                compensationAmount,
                sum.compareTo(BigDecimal.ZERO) > 0 ? sum : null,
                rentAmount,
                internetFee,
                electricityAmount,
                gasDeposit,
                waterAmount,
                profit,
                hasCurrentMonthSettlement
        );
    }

    private BigDecimal sumAmounts(BigDecimal... amounts) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            if (amount != null) {
                sum = sum.add(amount);
            }
        }
        return sum;
    }

    private BigDecimal calculateProfit(BigDecimal compensationAmount) {
        if (compensationAmount == null) {
            return null;
        }
        return compensationAmount.multiply(new BigDecimal("0.915")).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean checkCurrentMonthSettlement(Settlement latestSettlement) {
        if (latestSettlement == null || latestSettlement.getSettlementDate() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime settlementDate = latestSettlement.getSettlementDate();
        return settlementDate.getYear() == now.getYear()
                && settlementDate.getMonthValue() == now.getMonthValue();
    }

    @Override
    public boolean hasGasDeposit(Long rentedObjectId) {
        // Pobierz aktywnego tenanta bez lazy loading całego RentedObject (unikamy problemu z duplikatami w OneToOne)
        Tenant tenant = tenantRepository.getTenantByRentedObjectId(rentedObjectId);
        if (tenant == null) {
            return false;
        }

        Long tenantId = tenant.getId();
        Settlement latestSettlement = settlementRepository.findLatestByRentedObjectId(rentedObjectId, tenantId);

        return latestSettlement != null
                && latestSettlement.getGasDeposit() != null
                && latestSettlement.getGasDeposit().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public ProrationCheckResponseDto checkProrationSuggestion(Long rentedObjectId, String settlementDate) {
        // Znajdź aktywną umowę najmu dla danego obiektu
        Tenant tenant = tenantRepository.getTenantByRentedObjectId(rentedObjectId);

        if (tenant == null || tenant.getLeaseAgreement() == null) {
            return ProrationCheckResponseDto.builder()
                    .shouldSuggestProration(false)
                    .message(null)
                    .contractStartDate(null)
                    .settlementDate(settlementDate)
                    .build();
        }

        LeaseAgreement leaseAgreement = tenant.getLeaseAgreement();

        // Sprawdź czy umowa jest aktywna
        if (!leaseAgreement.isContractActive()) {
            return ProrationCheckResponseDto.builder()
                    .shouldSuggestProration(false)
                    .message(null)
                    .contractStartDate(null)
                    .settlementDate(settlementDate)
                    .build();
        }

        LocalDateTime contractStartDate = leaseAgreement.getStartContractDate();
        if (contractStartDate == null) {
            return ProrationCheckResponseDto.builder()
                    .shouldSuggestProration(false)
                    .message(null)
                    .contractStartDate(null)
                    .settlementDate(settlementDate)
                    .build();
        }

        // Parsuj datę rozliczenia
        LocalDate settlementLocalDate;
        try {
            settlementLocalDate = LocalDate.parse(settlementDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return ProrationCheckResponseDto.builder()
                    .shouldSuggestProration(false)
                    .message(null)
                    .contractStartDate(contractStartDate.toLocalDate().toString())
                    .settlementDate(settlementDate)
                    .build();
        }

        LocalDate contractStartLocalDate = contractStartDate.toLocalDate();

        // Sprawdź czy start_contract_date jest w tym samym miesiącu co settlementDate
        boolean sameMonth = contractStartLocalDate.getYear() == settlementLocalDate.getYear()
                && contractStartLocalDate.getMonthValue() == settlementLocalDate.getMonthValue();

        // Sprawdź czy start_contract_date nie jest pierwszym dniem miesiąca
        boolean notFirstDay = contractStartLocalDate.getDayOfMonth() > 1;

        // Jeśli oba warunki spełnione, zwróć sugestię
        boolean shouldSuggest = sameMonth && notFirstDay;

        String message = null;
        if (shouldSuggest) {
            int lastDayOfMonth = contractStartLocalDate.lengthOfMonth();
            message = String.format(
                    "Umowa najmu rozpoczyna się od %d %s %d. Rozważ rozliczenie niepełnego miesiąca (%d-%d %s).",
                    contractStartLocalDate.getDayOfMonth(),
                    getPolishMonthName(contractStartLocalDate.getMonthValue()),
                    contractStartLocalDate.getYear(),
                    contractStartLocalDate.getDayOfMonth(),
                    lastDayOfMonth,
                    getPolishMonthName(contractStartLocalDate.getMonthValue())
            );
        }

        return ProrationCheckResponseDto.builder()
                .shouldSuggestProration(shouldSuggest)
                .message(message)
                .contractStartDate(contractStartLocalDate.toString())
                .settlementDate(settlementDate)
                .build();
    }

    private String getPolishMonthName(int month) {
        return switch (month) {
            case 1 -> "stycznia";
            case 2 -> "lutego";
            case 3 -> "marca";
            case 4 -> "kwietnia";
            case 5 -> "maja";
            case 6 -> "czerwca";
            case 7 -> "lipca";
            case 8 -> "sierpnia";
            case 9 -> "września";
            case 10 -> "października";
            case 11 -> "listopada";
            case 12 -> "grudnia";
            default -> "";
        };
    }

    private List<RentedObjectDto> mapToDtos(List<RentedObject> rentedObjects) {
        return rentedObjects.stream()
                .map(ro -> {
                    List<MeterDto> meterDtos = ro.getMeters().stream()
                            .map(meter -> MeterDto.builder()
                                    .id(meter.getId())
                                    .name(meter.getName())
                                    .meterNumber(meter.getMeterNumber())
                                    .meterType(meter.getMeterType())
                                    .build())
                            .collect(Collectors.toList());
                    return RentedObjectDto.builder()
                            .id(ro.getId())
                            .rentedObjectName(ro.getRentedObjectName())
                            .meters(meterDtos)
                            .build();
                })
                .collect(Collectors.toList());
    }

}
