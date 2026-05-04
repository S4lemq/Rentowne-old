package pl.rentowne.tenant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import pl.rentowne.security.service.AuthenticationService;
import pl.rentowne.tenant.model.Tenant;
import pl.rentowne.tenant.model.dto.TenantDto;
import pl.rentowne.tenant.repository.TenantRepository;
import pl.rentowne.tenant.service.CifGeneratorService;
import pl.rentowne.tenant.service.TenantService;
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
    private final CifGeneratorService cifGeneratorService;

    @Override
    @Transactional
    public Long addTenantAndLeaseAgreement(TenantDto tenantDto) throws RentowneBusinessException {
        Long rentedObjectId = tenantDto.getRentedObjectDto().id();
        apartmentService.updateRentedFlagOnApartment(rentedObjectId);
        Tenant tenant = TenantDto.asEntity(tenantDto);
        tenant.setActive(true);
        tenant.setCif(cifGeneratorService.generateCif());
        Long tenantId = tenantRepository.save(tenant).getId();
        LocalDateTime currentReadingDate = tenantDto.getLeaseAgreementDto().startContractDate();

        processMeterReading(tenantDto.getLeaseAgreementDto().initialWaterMeterReading(), MeterType.WATER_COLD, rentedObjectId, currentReadingDate);
        processMeterReading(tenantDto.getLeaseAgreementDto().initialEnergyMeterReading(), MeterType.ELECTRIC, rentedObjectId, currentReadingDate);
        processMeterReading(tenantDto.getLeaseAgreementDto().initialGasMeterReading(), MeterType.GAS, rentedObjectId, currentReadingDate);

        authenticationService.createTenantAccount(tenantDto);
        return tenantId;
    }

    @Override
    public TenantDto getTenantAndLeaseAgreement(Long id) throws RentowneNotFoundException {
        Tenant tenantEntity = tenantRepository.findByTenantId(id).orElseThrow(() -> new RentowneNotFoundException(id));
        return TenantDto.asDto(tenantEntity);
    }

    @Override
    @Transactional
    public void updateTenantAndLeaseAgreement(TenantDto dto) {
        Long rentedObjectIdInDB = tenantRepository.getRentedObjectId(dto.getId());
        this.firstUpdateFlags(rentedObjectIdInDB);

        Tenant existingTenant = tenantRepository.findById(dto.getId())
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        Tenant updatedTenant = TenantDto.asEntity(dto);
        updatedTenant.setCif(existingTenant.getCif());
        updatedTenant.setActive(existingTenant.isActive());

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
        Long rentedObjectId = dto.getRentedObjectDto().id();
        rentedObjectRepository.updateRentedFlag(rentedObjectId, true);
        Long apartmentId = dto.getApartment().id();
        List<Long> ids = rentedObjectRepository.findByApartmentIdAndIsRented(apartmentId, false)
                .stream().map(RentedObject::getId).toList();
        if (ids.isEmpty()) {
            apartmentRepository.updateRentedFlag(apartmentId, true);
        }
    }

    private void firstUpdateFlags(Long rentedObjectIdInDB) {
        rentedObjectRepository.updateRentedFlag(rentedObjectIdInDB, false);
        Long apartmentId = apartmentRepository.findIdByRentedObjects_Id(rentedObjectIdInDB);
        apartmentRepository.updateRentedFlag(apartmentId, false);
    }
}
