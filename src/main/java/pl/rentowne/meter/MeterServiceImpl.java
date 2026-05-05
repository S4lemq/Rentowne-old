package pl.rentowne.meter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.rented_object.RentedObjectDto;
import pl.rentowne.user.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeterServiceImpl implements MeterService {

    private final MeterRepository meterRepository;
    private final UserService userService;

    @Override
    @Transactional
    public MeterDto addMeter(final MeterDto meterDto) {
        final RentedObject rentedObjectReference = new RentedObject(meterDto.getRentedObject().id());

        final Meter meter = this.convertToEntity(meterDto, rentedObjectReference);
        final Meter savedMeter = meterRepository.save(meter);
        return this.convertToDto(savedMeter);
    }

    @Override
    @Transactional
    public ApartmentMeterDto addApartmentMeter(final ApartmentMeterDto meterDto) {
        final Apartment apartmentReference = new Apartment(meterDto.apartment().id());

        final Meter meter = this.convertToEntityFromApartmentMeter(meterDto, apartmentReference);
        final Meter savedMeter = meterRepository.save(meter);
        return this.convertToApartmentMeterDto(savedMeter);
    }

    @Override
    @Transactional(readOnly = true)
    public MeterDto getMeterById(final Long id) {
        final Meter meter = meterRepository.findWithApartmentAndRentedObjectById(id).orElseThrow(() -> new RentowneNotFoundException(id));

        if (meter.getRentedObject() != null) {
            final RentedObject rentedObject = meter.getRentedObject();
            final String apartmentName = rentedObject.getApartment().getApartmentName();

            return MeterDto.builder()
                    .id(meter.getId())
                    .name(meter.getName())
                    .meterType(meter.getMeterType())
                    .rentedObject(RentedObjectDto.builder()
                            .id(rentedObject.getId())
                            .build())
                    .apartment(ApartmentDto.builder()
                            .apartmentName(apartmentName)
                            .build())
                    .meterNumber(meter.getMeterNumber())
                    .initialMeterReading(meter.getInitialMeterReading())
                    .installationDate(meter.getInstallationDate())
                    .distributeReadings(false)
                    .build();
        } else {
            final Apartment apartment = meter.getApartment();
            return MeterDto.builder()
                    .id(meter.getId())
                    .name(meter.getName())
                    .meterType(meter.getMeterType())
                    .apartment(ApartmentDto.builder()
                            .id(apartment.getId())
                            .apartmentName(apartment.getApartmentName())
                            .build())
                    .meterNumber(meter.getMeterNumber())
                    .initialMeterReading(meter.getInitialMeterReading())
                    .installationDate(meter.getInstallationDate())
                    .distributeReadings(meter.isDistributeReadings())
                    .build();
        }
    }

    @Override
    @Transactional
    public void updateMeter(final MeterDto meter) {
        final Meter meterEntity = meterRepository.findById(meter.getId())
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        meterEntity.setName(meter.getName());
        meterEntity.setMeterType(meter.getMeterType());
        meterEntity.setMeterNumber(meter.getMeterNumber());
        meterEntity.setInitialMeterReading(meter.getInitialMeterReading());
        meterEntity.setInstallationDate(meter.getInstallationDate());

        if (meter.getRentedObject() != null && meter.getRentedObject().id() != null) {
            meterEntity.setRentedObject(new RentedObject(meter.getRentedObject().id()));
        }

        meterRepository.save(meterEntity);
    }

    @Override
    @Transactional
    public void updateApartmentMeter(final ApartmentMeterDto meter) {
        final Meter meterEntity = meterRepository.findById(meter.id())
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        meterEntity.setName(meter.name());
        meterEntity.setMeterType(meter.meterType());
        meterEntity.setMeterNumber(meter.meterNumber());
        meterEntity.setInitialMeterReading(meter.initialMeterReading());
        meterEntity.setInstallationDate(meter.installationDate());
        meterEntity.setDistributeReadings(meter.distributeReadings());

        if (meter.apartment() != null && meter.apartment().id() != null) {
            meterEntity.setApartment(new Apartment(meter.apartment().id()));
        }

        meterRepository.save(meterEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApartmentMeterDto> getApartmentMeters(final Long apartmentId) {
        final List<Meter> meters = meterRepository.findAllByApartmentIdOrderByIdAsc(apartmentId);
        return meters.stream()
                .map(this::convertToApartmentMeterDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void getMeterCountByRentedObject(final Long id) {
        final List<Meter> meters = meterRepository.findAllByRentedObjectId(id);
        final boolean hasRequiredMeters = meters.stream()
                .anyMatch(meter -> meter.getMeterType() == MeterType.ELECTRIC || meter.getMeterType() == MeterType.WATER_COLD);

        if (!hasRequiredMeters) {
            throw new RentowneBusinessException(RentowneErrorCode.RENTED_OBJECT_DOES_NOT_HAVE_METERS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeterListDto> getMetersList(final Pageable pageable, final Long apartmentId, final MeterType meterType, final String searchText) {
        final Long userAccountId = userService.getLoggedUser().id();
        return meterRepository.getMetersList(pageable, userAccountId, apartmentId, meterType, searchText);
    }

    private Meter convertToEntity(final MeterDto dto, final RentedObject rentedObjectReference) {
        return Meter.builder()
                .name(dto.getName())
                .meterType(dto.getMeterType())
                .rentedObject(rentedObjectReference)
                .meterNumber(dto.getMeterNumber())
                .initialMeterReading(dto.getInitialMeterReading())
                .installationDate(dto.getInstallationDate())
                .build();
    }

    private Meter convertToEntityFromApartmentMeter(final ApartmentMeterDto dto, final Apartment apartmentReference) {
        return Meter.builder()
                .name(dto.name())
                .meterType(dto.meterType())
                .apartment(apartmentReference)
                .meterNumber(dto.meterNumber())
                .initialMeterReading(dto.initialMeterReading())
                .installationDate(dto.installationDate())
                .distributeReadings(dto.distributeReadings())
                .build();
    }

    private MeterDto convertToDto(final Meter entity) {
        return MeterDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .meterType(entity.getMeterType())
                .meterNumber(entity.getMeterNumber())
                .initialMeterReading(entity.getInitialMeterReading())
                .installationDate(entity.getInstallationDate())
                .build();
    }

    private ApartmentMeterDto convertToApartmentMeterDto(final Meter entity) {
        return new ApartmentMeterDto(
                entity.getId(),
                entity.getName(),
                entity.getMeterType(),
                null,
                entity.getMeterNumber(),
                entity.getInitialMeterReading(),
                entity.getInstallationDate(),
                entity.isDistributeReadings()
        );
    }
}
