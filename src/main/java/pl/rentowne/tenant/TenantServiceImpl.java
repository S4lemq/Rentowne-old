package pl.rentowne.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.apartment.ApartmentRepository;
import pl.rentowne.apartment.ApartmentService;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.meter.Meter;
import pl.rentowne.meter.MeterType;
import pl.rentowne.meter_reading.MeterReading;
import pl.rentowne.meter_reading.MeterReadingDto;
import pl.rentowne.meter_reading.MeterReadingRepository;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.rented_object.RentedObjectRepository;
import pl.rentowne.rental_payment.DepositAccountingService;
import pl.rentowne.rental_payment.RentalPaymentException;
import pl.rentowne.user.UserService;
import pl.rentowne.security.service.AuthenticationService;
import pl.rentowne.util.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final ApartmentService apartmentService;
    private final RentedObjectRepository rentedObjectRepository;
    private final ApartmentRepository apartmentRepository;
    private final AuthenticationService authenticationService;
    private final MeterReadingRepository meterReadingRepository;
    private final DepositAccountingService depositAccounting;
    private final UserService userService;

    @Override
    @Transactional
    public Long addTenantAndLeaseAgreement(TenantDto tenantDto) throws RentowneBusinessException {
        Long rentedObjectId = tenantDto.rentedObjectDto().id();
        requireOwnedObject(rentedObjectId);
        if (tenantDto.id() != null || tenantDto.leaseAgreementDto().id() != null
                || tenantDto.addressDto() != null && tenantDto.addressDto().id() != null) {
            throw new RentalPaymentException(HttpStatus.BAD_REQUEST, "Nowy najem nie może wskazywać istniejącego najemcy, umowy ani adresu.");
        }
        apartmentService.updateRentedFlagOnApartment(rentedObjectId);
        Tenant tenant = TenantDto.asEntity(tenantDto);
        depositAccounting.initializeLease(tenant.getLeaseAgreement());
        tenant.setActive(true);
        Long tenantId = tenantRepository.save(tenant).getId();
        LocalDateTime currentReadingDate = tenantDto.leaseAgreementDto().startContractDate();

        processMeterReading(tenantDto.leaseAgreementDto().initialWaterMeterReading(), MeterType.WATER_COLD, rentedObjectId, currentReadingDate);
        processMeterReading(tenantDto.leaseAgreementDto().initialEnergyMeterReading(), MeterType.ELECTRIC, rentedObjectId, currentReadingDate);
        processMeterReading(tenantDto.leaseAgreementDto().initialGasMeterReading(), MeterType.GAS, rentedObjectId, currentReadingDate);

        authenticationService.createTenantAccount(tenantDto);
        return tenantId;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TenantDto getTenantAndLeaseAgreement(Long id) throws RentowneNotFoundException {
        Tenant tenantEntity = tenantRepository.findByTenantId(id).orElseThrow(() -> new RentowneNotFoundException(id));
        requireOwnedTenant(tenantEntity);
        tenantEntity.getLeaseAgreement().setDepositAssessed(depositAccounting.isAssessed(id));
        return TenantDto.asDto(tenantEntity);
    }

    @Override
    @Transactional
    public void updateTenantAndLeaseAgreement(TenantDto dto) {
        Tenant existingTenant = tenantRepository.lockById(dto.id())
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));
        requireOwnedTenant(existingTenant);
        requireOwnedObject(dto.rentedObjectDto().id());
        if (!existingTenant.getLeaseAgreement().getId().equals(dto.leaseAgreementDto().id())
                || dto.addressDto() != null && dto.addressDto().id() != null
                && (existingTenant.getAddress() == null || !dto.addressDto().id().equals(existingTenant.getAddress().getId()))) {
            throw new RentalPaymentException(HttpStatus.BAD_REQUEST, "Edycja najmu nie może wskazywać innej umowy ani cudzego adresu.");
        }
        Long rentedObjectIdInDB = existingTenant.getRentedObject().getId();

        Tenant updatedTenant = TenantDto.asEntity(dto);
        updatedTenant.getLeaseAgreement().setActualTerminationDate(existingTenant.getLeaseAgreement().getActualTerminationDate());
        updatedTenant.getLeaseAgreement().setDepositRefundDueDate(existingTenant.getLeaseAgreement().getDepositRefundDueDate());
        depositAccounting.prepareLeaseUpdate(existingTenant, updatedTenant.getLeaseAgreement());
        updatedTenant.setActive(existingTenant.isActive());

        this.firstUpdateFlags(rentedObjectIdInDB);
        tenantRepository.save(updatedTenant);
        this.secondUpdateFlags(dto);
    }

    private void processMeterReading(BigDecimal initialReading, MeterType meterType, Long rentedObjectId, LocalDateTime currentReadingDate) {
        if (initialReading != null) {
            MeterReadingDto meterIdAndConsumption = meterReadingRepository.getMeterIdReadingValueAndDate(rentedObjectId, meterType);

            Optional.ofNullable(meterIdAndConsumption.currentReading())
                    .ifPresent(currentReading -> {
                        if (initialReading.compareTo(currentReading) < 0) {
                            throw new RentowneBusinessException(RentowneErrorCode.BAD_METER_READING_VALUES_TENANT);
                        }
                        DateUtils.compareDates(currentReadingDate, meterIdAndConsumption.readingDate(), true, meterType);
                    });

            BigDecimal consumption = Optional.ofNullable(meterIdAndConsumption.currentReading())
                    .map(initialReading::subtract)
                    .orElse(BigDecimal.ZERO);

            MeterReading meterReading = MeterReading.builder()
                    .currentReading(initialReading)
                    .readingDate(currentReadingDate)
                    .consumption(consumption)
                    .meter(Meter.builder().id(meterIdAndConsumption.meterDto().getId()).build())
                    .settled(true)
                    .build();
            meterReadingRepository.save(meterReading);
        }
    }

    private void secondUpdateFlags(TenantDto dto) {
        Long rentedObjectId = dto.rentedObjectDto().id();
        rentedObjectRepository.updateRentedFlag(rentedObjectId, true);
        Long apartmentId = rentedObjectRepository.findApartmentIdByRentedObjectId(rentedObjectId);
        List<Long> ids = rentedObjectRepository.findByApartmentIdAndIsRented(apartmentId, false)
                .stream().map(RentedObject::getId).toList();
        if (ids.isEmpty()) {
            apartmentRepository.updateRentedFlag(apartmentId, true);
        }
    }

    private void firstUpdateFlags(Long rentedObjectIdInDB) {
        rentedObjectRepository.updateRentedFlag(rentedObjectIdInDB, false);
        Optional<Apartment> idByRentedObjectsId = apartmentRepository.findIdByRentedObjects_Id(rentedObjectIdInDB);
        apartmentRepository.updateRentedFlag(
            idByRentedObjectsId.orElseThrow(() -> new RentowneNotFoundException(rentedObjectIdInDB)).getId(),
            false
        );
    }

    private void requireOwnedObject(Long rentedObjectId) {
        if (!rentedObjectRepository.existsByIdAndApartmentUserId(rentedObjectId, userService.getLoggedUser().id())) {
            throw new RentalPaymentException(HttpStatus.NOT_FOUND, "Nie znaleziono obiektu.");
        }
    }

    private void requireOwnedTenant(Tenant tenant) {
        if (tenant.getRentedObject() == null || !tenant.getRentedObject().getApartment().getUser().getId().equals(userService.getLoggedUser().id())) {
            throw new RentalPaymentException(HttpStatus.NOT_FOUND, "Nie znaleziono najmu.");
        }
    }
}
