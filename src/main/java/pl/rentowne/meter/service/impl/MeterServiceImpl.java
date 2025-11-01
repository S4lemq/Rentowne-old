package pl.rentowne.meter.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.model.Apartment;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.meter.model.Meter;
import pl.rentowne.meter.model.dto.ApartmentMeterDto;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.meter.model.dto.MeterListDto;
import pl.rentowne.meter.repository.MeterRepository;
import pl.rentowne.meter.service.MeterService;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;
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
        Apartment apartmentReference = new Apartment(meterDto.apartment().getId());

        Meter meter = this.convertToEntityFromApartmentMeter(meterDto, apartmentReference);
        Meter savedMeter = meterRepository.save(meter);
        return this.convertToApartmentMeterDto(savedMeter);
    }

    @Override
    @Transactional(readOnly = true)
    public MeterDto getMeterById(Long id) {
        Meter meterDto = meterRepository.getMeterDtoById(id);
        RentedObject rentedObject = meterDto.getRentedObject();

        return MeterDto.builder()
                .id(meterDto.getId())
                .name(meterDto.getName())
                .meterType(meterDto.getMeterType())
                .rentedObject(RentedObjectDto.builder()
                        .id(rentedObject.getId())
                        .build())
                .meterNumber(meterDto.getMeterNumber())
                .initialMeterReading(meterDto.getInitialMeterReading())
                .installationDate(meterDto.getInstallationDate())
                .build();
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

        if (meter.apartment() != null && meter.apartment().getId() != null) {
            meterEntity.setApartment(new Apartment(meter.apartment().getId()));
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
    public Page<MeterListDto> getMetersList(Pageable pageable, Long apartmentId, String searchText) {
        Long userAccountId = userService.getLoggedUser().getId();
        return meterRepository.getMetersList(pageable, userAccountId, apartmentId, searchText);
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
                entity.getInstallationDate()
        );
    }
}
