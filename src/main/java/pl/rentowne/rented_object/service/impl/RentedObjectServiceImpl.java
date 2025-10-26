package pl.rentowne.rented_object.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.rented_object.model.dto.RentedObjectDetailsDto;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;
import pl.rentowne.rented_object.repository.RentedObjectRepository;
import pl.rentowne.rented_object.service.RentedObjectService;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.repository.SettlementRepository;
import pl.rentowne.user.service.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.rentowne.util.DateUtils.formatPolishDate;

@Service
@RequiredArgsConstructor
public class RentedObjectServiceImpl implements RentedObjectService {

    private final RentedObjectRepository rentedObjectRepository;
    private final SettlementRepository settlementRepository;
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
    public List<RentedObjectDetailsDto> getRentedObjectsDetails(Long apartmentId) {
        Long userId = userService.getLoggedUser().getId();
        List<RentedObject> rentedObjects = rentedObjectRepository.findRentedObjectsWithDetailsForUser(userId, apartmentId);

        return rentedObjects.stream()
                .map(this::mapToDetailsDto)
                .collect(Collectors.toList());
    }

    private RentedObjectDetailsDto mapToDetailsDto(RentedObject rentedObject) {
        Long tenantId = rentedObject.getTenant().getId();
        Settlement latestSettlement = settlementRepository.findLatestByRentedObjectId(rentedObject.getId(), tenantId);

        String tenantFullName = rentedObject.getTenant().getFirstname() + " " + rentedObject.getTenant().getLastname();

        BigDecimal rentAmount = latestSettlement != null ? latestSettlement.getRentAmount() : null;
        BigDecimal internetFee = latestSettlement != null ? latestSettlement.getInternetFee() : null;
        BigDecimal gasDeposit = latestSettlement != null ? latestSettlement.getGasDeposit() : null;
        BigDecimal electricityAmount = latestSettlement != null ? latestSettlement.getElectricityAmount() : null;
        BigDecimal waterAmount = latestSettlement != null ? latestSettlement.getWaterAmount() : null;
        BigDecimal compensationAmount = latestSettlement != null ? latestSettlement.getCompensationAmount() : null;

        // Oblicz totalAmount jako sumę wszystkich składników
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (electricityAmount != null) totalAmount = totalAmount.add(electricityAmount);
        if (waterAmount != null) totalAmount = totalAmount.add(waterAmount);
        if (compensationAmount != null) totalAmount = totalAmount.add(compensationAmount);
        if (internetFee != null) totalAmount = totalAmount.add(internetFee);
        if (rentAmount != null) totalAmount = totalAmount.add(rentAmount);

        // Suma = czynsz + internet + gaz + prąd + woda
        BigDecimal sum = BigDecimal.ZERO;
        if (rentAmount != null) sum = sum.add(rentAmount);
        if (internetFee != null) sum = sum.add(internetFee);
        if (gasDeposit != null) sum = sum.add(gasDeposit);
        if (electricityAmount != null) sum = sum.add(electricityAmount);
        if (waterAmount != null) sum = sum.add(waterAmount);

        // Zysk = odstępne * 0.915
        BigDecimal profit = null;
        if (compensationAmount != null) {
            profit = compensationAmount.multiply(new BigDecimal("0.915")).setScale(2, RoundingMode.HALF_UP);
        }

        // Sprawdź czy istnieje rozliczenie z bieżącego miesiąca
        boolean hasCurrentMonthSettlement = false;
        if (latestSettlement != null && latestSettlement.getSettlementDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime settlementDate = latestSettlement.getSettlementDate();
            hasCurrentMonthSettlement = settlementDate.getYear() == now.getYear()
                    && settlementDate.getMonthValue() == now.getMonthValue();
        }

        return new RentedObjectDetailsDto(
                rentedObject.getId(),
                rentedObject.getRentedObjectName(),
                tenantFullName,
                tenantId,
                formatPolishDate(rentedObject.getLastSettlementDate()),
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

    private List<RentedObjectDto> mapToDtos(List<RentedObject> rentedObjects) {
        return rentedObjects.stream()
                .map(ro -> {
                    List<MeterDto> meterDtos = ro.getMeters().stream()
                            .map(meter -> MeterDto.builder()
                                    .id(meter.getId())
                                    .name(meter.getName())
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
