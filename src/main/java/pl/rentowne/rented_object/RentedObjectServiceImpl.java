package pl.rentowne.rented_object;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.lease_agreement.LeaseAgreement;
import pl.rentowne.settlement.Settlement;
import pl.rentowne.settlement.SettlementRepository;
import pl.rentowne.tenant.model.Tenant;
import pl.rentowne.tenant.repository.TenantRepository;
import pl.rentowne.user.service.UserService;
import pl.rentowne.util.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentedObjectServiceImpl implements RentedObjectService {

    private final RentedObjectRepository rentedObjectRepository;
    private final SettlementRepository settlementRepository;
    private final TenantRepository tenantRepository;
    private final UserService userService;

    @Override
    public void saveAll(final Set<RentedObject> rentedObjects) {
        rentedObjectRepository.saveAll(rentedObjects);
    }

    @Override
    public List<RentedObject> getAllByApartmentId(final Long apartmentId) {
        return rentedObjectRepository.findAllByApartmentId(apartmentId);
    }

    @Override
    public void deleteAll(final List<RentedObject> toDelete) {
        rentedObjectRepository.deleteAll(toDelete);
    }

    @Override
    public List<RentedObjectDto> getAllRentedObjectDtosWithMeters(final Long apartmentId) {
        final List<RentedObject> rentedObjects = rentedObjectRepository.findByApartmentId(apartmentId);
        return rentedObjects.stream()
            .map(RentedObjectMapper::toDtoWithMeters)
            .collect(Collectors.toList());
    }

    @Override
    public List<RentedObjectDto> getAllRentedObjectDtos() throws RentowneBusinessException {
        final Long userId = userService.getLoggedUser().getId();
        return rentedObjectRepository.findAllByLoggedUser(userId).stream().map(RentedObjectMapper::toDto).toList();
    }

    @Override
    public List<RentedObjectDto> getAllByApartmentAndOptionalRentedObject(final Long apartmentId, final Long rentedObjectId) throws RentowneBusinessException {
        return rentedObjectRepository.getAllByApartmentAndOptionalRentedObject(apartmentId, rentedObjectId).stream().map(RentedObjectMapper::toDto).toList();
    }

    @Override
    public List<RentedObjectDetailsDto> getRentedObjectsDetails(final Long apartmentId, final Boolean active) {
        final Long userId = userService.getLoggedUser().getId();
        final List<Tenant> tenants = tenantRepository.findTenantsWithDetailsForUser(userId, apartmentId, active);

        return tenants.stream()
                .map((final Tenant tenant) -> {
                    final Long tenantId = tenant.getId();
                    final Long rentedObjectId = tenant.getRentedObject().getId();
                    final Settlement latestSettlement = settlementRepository.findLatestByRentedObjectId(rentedObjectId, tenantId);
                    return RentedObjectMapper.toDetailsDto(tenant, latestSettlement);
                })
                .toList();
    }

    @Override
    public boolean hasGasDeposit(final Long rentedObjectId) {
        final Tenant tenant = tenantRepository.getTenantByRentedObjectId(rentedObjectId);
        if (tenant == null) {
            return false;
        }

        final Long tenantId = tenant.getId();
        final Settlement latestSettlement = settlementRepository.findLatestByRentedObjectId(rentedObjectId, tenantId);

        return latestSettlement != null
                && latestSettlement.getGasDeposit() != null
                && latestSettlement.getGasDeposit().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public ProrationCheckResponseDto checkProrationSuggestion(final Long rentedObjectId, final String settlementDate) {
        final Tenant tenant = tenantRepository.getTenantByRentedObjectId(rentedObjectId);

        if (tenant == null || tenant.getLeaseAgreement() == null) {
            return buildDefaultResponse(settlementDate, null);
        }

        final LeaseAgreement leaseAgreement = tenant.getLeaseAgreement();

        if (!leaseAgreement.isContractActive()) {
            return buildDefaultResponse(settlementDate, null);
        }

        final LocalDateTime contractStartDate = leaseAgreement.getStartContractDate();
        if (contractStartDate == null) {
            return buildDefaultResponse(settlementDate, null);
        }

        final LocalDate settlementLocalDate = parseSettlementDate(settlementDate);
        if (settlementLocalDate == null) {
            return buildDefaultResponse(settlementDate, contractStartDate.toLocalDate().toString());
        }

        final LocalDate contractStartLocalDate = contractStartDate.toLocalDate();
        final boolean shouldSuggest = isInSameMonthAndNotFirstDay(contractStartLocalDate, settlementLocalDate);
        final String message = shouldSuggest ? buildProrationMessage(contractStartLocalDate) : null;

        return ProrationCheckResponseDto.builder()
                .shouldSuggestProration(shouldSuggest)
                .message(message)
                .contractStartDate(contractStartLocalDate.toString())
                .settlementDate(settlementDate)
                .build();
    }

    private ProrationCheckResponseDto buildDefaultResponse(final String settlementDate, final String contractStartDate) {
        return ProrationCheckResponseDto.builder()
                .shouldSuggestProration(false)
                .message(null)
                .contractStartDate(contractStartDate)
                .settlementDate(settlementDate)
                .build();
    }

    private LocalDate parseSettlementDate(final String settlementDate) {
        try {
            return LocalDate.parse(settlementDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (final Exception e) {
            return null;
        }
    }

    private boolean isInSameMonthAndNotFirstDay(final LocalDate contractStartDate, final LocalDate settlementDate) {
        final boolean sameMonth = contractStartDate.getYear() == settlementDate.getYear()
                && contractStartDate.getMonthValue() == settlementDate.getMonthValue();
        final boolean notFirstDay = contractStartDate.getDayOfMonth() > 1;
        return sameMonth && notFirstDay;
    }

    private String buildProrationMessage(final LocalDate contractStartDate) {
        final int lastDayOfMonth = contractStartDate.lengthOfMonth();
        return String.format(
                "Umowa najmu rozpoczyna się od %d %s %d. Rozważ rozliczenie niepełnego miesiąca (%d-%d %s).",
                contractStartDate.getDayOfMonth(),
                DateUtils.getPolishMonthName(contractStartDate.getMonthValue()),
                contractStartDate.getYear(),
                contractStartDate.getDayOfMonth(),
                lastDayOfMonth,
                DateUtils.getPolishMonthName(contractStartDate.getMonthValue())
        );
    }

}
