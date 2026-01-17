package pl.rentowne.meter.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.apartment.ApartmentDto;
import pl.rentowne.apartment.ApartmentMapper;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.meter.model.Meter;
import pl.rentowne.meter.model.MeterType;
import pl.rentowne.meter.model.dto.ApartmentMeterDto;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.meter.model.dto.MeterListDto;
import pl.rentowne.meter.repository.MeterRepository;
import pl.rentowne.meter.service.MeterService;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.rented_object.RentedObjectDto;
import pl.rentowne.user.service.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeterServiceImpl implements MeterService {

    private final MeterRepository meterRepository;
    private final UserService userService;

    @Override
    @Transactional
    public MeterDto addMeter(MeterDto meterDto) {
        RentedObject rentedObjectReference = new RentedObject(meterDto.getRentedObject().getId());

        Meter meter = this.convertToEntity(meterDto, rentedObjectReference);
        Meter savedMeter = meterRepository.save(meter);
        return this.convertToDto(savedMeter);
    }

    @Override
    @Transactional
    public ApartmentMeterDto addApartmentMeter(ApartmentMeterDto meterDto) {
        Apartment apartmentReference = new Apartment(meterDto.apartment().id());

        Meter meter = this.convertToEntityFromApartmentMeter(meterDto, apartmentReference);
        Meter savedMeter = meterRepository.save(meter);
        return this.convertToApartmentMeterDto(savedMeter);
    }

    @Override
    @Transactional(readOnly = true)
    public MeterDto getMeterById(Long id) {
        Meter meter = meterRepository.getMeterDtoById(id);

        if (meter.getRentedObject() != null) {
            Long rentedObjectId = meter.getRentedObject().getId();
            String apartmentName = meterRepository.getApartmentNameByRentedObjectId(rentedObjectId);

            return MeterDto.builder()
                    .id(meter.getId())
                    .name(meter.getName())
                    .meterType(meter.getMeterType())
                    .rentedObject(RentedObjectDto.builder()
                            .id(rentedObjectId)
                            .build())
                    .apartment(ApartmentDto.builder()
                            .id(null)
                            .apartmentName(apartmentName)
                            .leasesNumber(null)
                            .isRented(false)
                            .area(null)
                            .image(null)
                            .pinned(false)
                            .cityName(null)
                            .streetName(null)
                            .buildingNumber(null)
                            .apartmentNumber(null)
                            .addressDto(null)
                            .rentedObjectDtos(null)
                            .build())
                    .meterNumber(meter.getMeterNumber())
                    .initialMeterReading(meter.getInitialMeterReading())
                    .installationDate(meter.getInstallationDate())
                    .distributeReadings(false)
                    .build();
        } else {
            Apartment apartment = meter.getApartment();
            return MeterDto.builder()
                    .id(meter.getId())
                    .name(meter.getName())
                    .meterType(meter.getMeterType())
                    .apartment(ApartmentMapper.toBasicDto(apartment))
                    .meterNumber(meter.getMeterNumber())
                    .initialMeterReading(meter.getInitialMeterReading())
                    .installationDate(meter.getInstallationDate())
                    .distributeReadings(meter.isDistributeReadings())
                    .build();
        }
    }

    @Override
    @Transactional
    public void updateMeter(MeterDto meter) {
        Meter meterEntity = meterRepository.findById(meter.getId())
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        meterEntity.setName(meter.getName());
        meterEntity.setMeterType(meter.getMeterType());
        meterEntity.setMeterNumber(meter.getMeterNumber());
        meterEntity.setInitialMeterReading(meter.getInitialMeterReading());
        meterEntity.setInstallationDate(meter.getInstallationDate());

        if (meter.getRentedObject() != null && meter.getRentedObject().getId() != null) {
            meterEntity.setRentedObject(new RentedObject(meter.getRentedObject().getId()));
        }

        meterRepository.save(meterEntity);
    }

    @Override
    @Transactional
    public void updateApartmentMeter(ApartmentMeterDto meter) {
        Meter meterEntity = meterRepository.findById(meter.id())
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
    public List<ApartmentMeterDto> getApartmentMeters(Long apartmentId) {
        List<Meter> meters = meterRepository.findByApartmentId(apartmentId);
        return meters.stream()
                .map(this::convertToApartmentMeterDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void getMeterCountByRentedObject(Long id) {
        List<Long> ids = meterRepository.findMeterCountByRentedObject(id);
        if (ids.isEmpty()) {
            throw new RentowneBusinessException(RentowneErrorCode.RENTED_OBJECT_DOES_NOT_HAVE_METERS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeterListDto> getMetersList(Pageable pageable, Long apartmentId, MeterType meterType, String searchText) {
        Long userAccountId = userService.getLoggedUser().getId();
        return meterRepository.getMetersList(pageable, userAccountId, apartmentId, meterType, searchText);
    }

    private Meter convertToEntity(MeterDto dto, RentedObject rentedObjectReference) {
        return Meter.builder()
                .name(dto.getName())
                .meterType(dto.getMeterType())
                .rentedObject(rentedObjectReference)
                .meterNumber(dto.getMeterNumber())
                .initialMeterReading(dto.getInitialMeterReading())
                .installationDate(dto.getInstallationDate())
                .build();
    }

    private Meter convertToEntityFromApartmentMeter(ApartmentMeterDto dto, Apartment apartmentReference) {
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

    private MeterDto convertToDto(Meter entity) {
        return MeterDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .meterType(entity.getMeterType())
                .meterNumber(entity.getMeterNumber())
                .initialMeterReading(entity.getInitialMeterReading())
                .installationDate(entity.getInstallationDate())
                .build();
    }

    private ApartmentMeterDto convertToApartmentMeterDto(Meter entity) {
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
