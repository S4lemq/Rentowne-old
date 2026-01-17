package pl.rentowne.apartment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.housing_provider.model.HousingProvider;
import pl.rentowne.housing_provider.repository.HousingProviderRepository;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;
import pl.rentowne.rented_object.repository.RentedObjectRepository;
import pl.rentowne.rented_object.service.RentedObjectService;
import pl.rentowne.user.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApartmentServiceImpl implements ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final UserService userService;
    private final RentedObjectService rentedObjectService;
    private final RentedObjectRepository rentedObjectRepository;
    private final HousingProviderRepository housingProviderRepository;

    @Override
    @Transactional(readOnly = true)
    public ApartmentDto getApartment(final Long id) throws RentowneNotFoundException {
        final Apartment apartment = apartmentRepository.findById(id)
            .orElseThrow(() -> new RentowneNotFoundException(id));
        loadRentedObjects(apartment);
        return ApartmentMapper.toDto(apartment);
    }

    @Override
    @Transactional
    public Long addApartment(final ApartmentDto apartmentDto) throws RentowneBusinessException {
        validateRentedObjectNames(apartmentDto);

        final Long loggedUserId = userService.getLoggedUser().getId();
        final Long savedApartmentId = apartmentRepository.saveAndFlush(
            ApartmentMapper.toEntity(apartmentDto, null, loggedUserId)
        ).getId();

        final Set<RentedObject> rentedObjects = mapToRentedObjectEntities(apartmentDto, savedApartmentId);
        rentedObjectService.saveAll(rentedObjects);
        return savedApartmentId;
    }

    @Override
    @Transactional
    public void updateApartment(final ApartmentDto apartmentDto, final Long apartmentId) throws RentowneBusinessException {
        updateRentedObjects(apartmentDto, apartmentId);
        final Long loggedUserId = userService.getLoggedUser().getId();
        apartmentRepository.save(ApartmentMapper.toEntity(apartmentDto, apartmentId, loggedUserId));
    }

    @Override
    @Transactional
    public void deleteApartment(final Long id) {
        rentedObjectService.deleteAllByApartmentId(id);
        apartmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApartmentDto> getAllApartmentsByLoggedUserAndApartment(final Long apartmentId) throws RentowneBusinessException {
        final Long loggedUserId = userService.getLoggedUser().getId();
        return apartmentRepository.getAllApartmentsByLoggedUserAndApartment(loggedUserId, apartmentId);
    }

    @Override
    @Transactional
    public void updateRentedFlagOnApartment(final Long rentedObjectId) {
        rentedObjectRepository.updateRentedFlag(rentedObjectId, true);
        final Long apartmentId = apartmentRepository.getApartmentIdByRentedObject(rentedObjectId);
        final List<Long> ids = rentedObjectRepository.findByApartmentIdAndIsRented(apartmentId, false)
                .stream().map(RentedObject::getId).toList();
        if (ids.isEmpty()) {
            apartmentRepository.updateRentedFlag(apartmentId, true);
        }
    }

    @Override
    @Transactional
    public void addHousingProviders(final ApartmentHousingProviderRequest dto) throws RentowneNotFoundException {
        final Apartment apartment = apartmentRepository.getApartmentWithHousingProviders(dto.apartmentId());
        final List<HousingProvider> housingProvidersFromRequest = housingProviderRepository.findAllById(dto.housingProviderIds());
        final Set<HousingProvider> currentHousingProviders = apartment.getHousingProviders();

        validateNewProviders(currentHousingProviders, housingProvidersFromRequest);
        removeObsoleteProviders(apartment, currentHousingProviders, housingProvidersFromRequest);
        addNewProviders(apartment, currentHousingProviders, housingProvidersFromRequest);
    }

    @Override
    @Transactional
    public void pinApartment(final boolean isPinned, final long id) {
        apartmentRepository.pinApartment(isPinned, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApartmentDto> getAllApartmentsByLoggedUser() throws RentowneBusinessException {
        final Long loggedUserId = userService.getLoggedUser().getId();
        return apartmentRepository.findByUser_IdOrderByApartmentName(loggedUserId)
                .stream().map(ApartmentMapper::toBasicDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApartmentDto> getAllFullApartmentsByLoggedUser() throws RentowneBusinessException {
        final Long loggedUserId = userService.getLoggedUser().getId();
        final List<Apartment> apartments = apartmentRepository.findByUser_IdOrderByApartmentName(loggedUserId);
        return apartments.stream()
            .map(apartment -> {
                loadRentedObjects(apartment);
                return ApartmentMapper.toDto(apartment);
            })
            .toList();
    }


    private void loadRentedObjects(final Apartment apartment) {
        final List<RentedObject> rentedObjects = rentedObjectRepository.getAllByApartmentId(apartment.getId());
        apartment.setRentedObjects(new HashSet<>(rentedObjects));
    }

    private void validateRentedObjectNames(final ApartmentDto apartmentDto) throws RentowneBusinessException {
        final List<RentedObjectDto> rentedObjectDtos = apartmentDto.rentedObjectDtos();
        if (rentedObjectDtos == null || rentedObjectDtos.isEmpty()) {
            return;
        }

        final Set<String> uniqueNames = new HashSet<>();
        for (final RentedObjectDto dto : rentedObjectDtos) {
            if (!uniqueNames.add(dto.getRentedObjectName())) {
                throw new RentowneBusinessException(RentowneErrorCode.NON_UNIQUE_RENTED_OBJECT_NAME);
            }
        }
    }

    private void validateNewProviders(final Set<HousingProvider> currentProviders,
                                     final List<HousingProvider> newProviders) throws RentowneBusinessException {
        final List<HousingProvider> providersToAdd = newProviders.stream()
            .filter(providerFromRequest -> currentProviders.stream()
                .noneMatch(currentProvider -> currentProvider.getId().equals(providerFromRequest.getId())))
            .toList();

        for (final HousingProvider newProvider : providersToAdd) {
            final boolean existsWithType = currentProviders.stream()
                .anyMatch(currentProvider -> currentProvider.getType().equals(newProvider.getType()));

            if (existsWithType) {
                throw new RentowneBusinessException(
                    RentowneErrorCode.PROVIDER_TYPE_ALREADY_EXISTS,
                    newProvider.getType().toString()
                );
            }
        }
    }

    private void removeObsoleteProviders(final Apartment apartment,
                                        final Set<HousingProvider> currentProviders,
                                        final List<HousingProvider> requestedProviders) {
        final Set<HousingProvider> providersToRemove = currentProviders.stream()
            .filter(provider -> !requestedProviders.contains(provider))
            .collect(Collectors.toSet());

        for (final HousingProvider providerToRemove : providersToRemove) {
            providerToRemove.getApartments().remove(apartment);
            apartment.getHousingProviders().remove(providerToRemove);
        }
    }

    private void addNewProviders(final Apartment apartment,
                                final Set<HousingProvider> currentProviders,
                                final List<HousingProvider> requestedProviders) {
        for (final HousingProvider newProvider : requestedProviders) {
            if (!currentProviders.contains(newProvider)) {
                newProvider.addApartment(apartment);
            }
        }
    }

    private void updateRentedObjects(final ApartmentDto apartmentDto, final Long apartmentId) {
        if (apartmentId == null) {
            return;
        }

        final Set<RentedObject> rentedObjects = mapToRentedObjectEntities(apartmentDto, apartmentId);
        final List<RentedObject> rentedObjectsFromDB = rentedObjectService.getAllByApartmentId(apartmentId);

        final Set<String> namesFromRequest = rentedObjects.stream()
            .map(RentedObject::getRentedObjectName)
            .collect(Collectors.toSet());

        final Set<String> namesFromDb = rentedObjectsFromDB.stream()
            .map(RentedObject::getRentedObjectName)
            .collect(Collectors.toSet());

        final List<RentedObject> toDelete = rentedObjectsFromDB.stream()
            .filter(entity -> !namesFromRequest.contains(entity.getRentedObjectName()))
            .toList();

        final List<RentedObject> toAdd = rentedObjects.stream()
            .filter(entity -> !namesFromDb.contains(entity.getRentedObjectName()))
            .toList();

        rentedObjectService.deleteAll(toDelete);
        rentedObjectService.saveAll(new HashSet<>(toAdd));
    }

    private Set<RentedObject> mapToRentedObjectEntities(final ApartmentDto apartmentDto, final Long apartmentId) {
        final List<RentedObjectDto> rentedObjectDtos = apartmentDto.rentedObjectDtos();
        if (rentedObjectDtos == null || rentedObjectDtos.isEmpty()) {
            return new HashSet<>();
        }

        return rentedObjectDtos.stream()
            .map(dto -> RentedObject.builder()
                .rentedObjectName(dto.getRentedObjectName())
                .isRented(false)
                .apartment(new Apartment(apartmentId))
                .build())
            .collect(Collectors.toSet());
    }
}
