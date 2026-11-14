package pl.rentowne.apartment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.housing_provider.HousingProvider;
import pl.rentowne.housing_provider.HousingProviderRepository;
import pl.rentowne.owner.OwnerException;
import pl.rentowne.owner.OwnerService;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.rented_object.RentedObjectRepository;
import pl.rentowne.rented_object.RentedObjectService;
import pl.rentowne.user.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    private final OwnerService ownerService;

    @Override
    @Transactional(readOnly = true)
    public ApartmentDto getApartment(final Long id) throws RentowneNotFoundException {
        final Apartment apartment = apartmentRepository.findWithRentedObjectsByIdAndUser_Id(id, userService.getLoggedUser().id())
                .orElseThrow(() -> new RentowneNotFoundException(id));
        return ApartmentMapper.toDto(apartment);
    }

    @Override
    @Transactional
    public Long addApartment(final ApartmentDto apartmentDto) throws RentowneBusinessException {
        if (apartmentDto.ownerDto() != null) {
            throw new OwnerException(HttpStatus.BAD_REQUEST, "Właściciela dodaj w edycji zapisanej nieruchomości.");
        }
        validateRentedObjectNames(apartmentDto);

        final Long loggedUserId = userService.getLoggedUser().id();
        final Long savedApartmentId = apartmentRepository.saveAndFlush(ApartmentMapper.toEntity(apartmentDto, null, loggedUserId)).getId();

        final Set<RentedObject> rentedObjects = ApartmentMapper.toRentedObjectEntities(apartmentDto, savedApartmentId);
        rentedObjectService.saveAll(rentedObjects);
        return savedApartmentId;
    }

    @Override
    @Transactional
    public void updateApartment(final ApartmentDto apartmentDto, final Long apartmentId) throws RentowneBusinessException {
        final Apartment apartment = apartmentRepository.findWithRentedObjectsByIdAndUser_Id(apartmentId, userService.getLoggedUser().id())
                .orElseThrow(() -> new RentowneNotFoundException(apartmentId));
        validateRentedObjectNames(apartmentDto);
        if (apartmentDto.ownerDto() != null) {
            apartment.setOwner(ownerService.save(apartmentDto.ownerDto()));
        }
        ApartmentMapper.updateEntity(apartmentDto, apartment);
        updateRentedObjects(apartmentDto, apartmentId);
        apartmentRepository.saveAndFlush(apartment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApartmentDto> getAllApartmentsByLoggedUserAndApartment(final Long apartmentId) throws RentowneBusinessException {
        final Long loggedUserId = userService.getLoggedUser().id();
        return apartmentRepository.getAllApartmentsByLoggedUserAndApartment(loggedUserId, apartmentId).stream()
                .map(ApartmentMapper::toBasicDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateRentedFlagOnApartment(final Long rentedObjectId) {
        rentedObjectRepository.updateRentedFlag(rentedObjectId, true);
        Optional<Apartment> idByRentedObjectsId = apartmentRepository.findIdByRentedObjects_Id(rentedObjectId);
        final Long apartmentId = idByRentedObjectsId.orElseThrow(() -> new RentowneNotFoundException(rentedObjectId)).getId();
        final List<Long> ids = rentedObjectRepository.findByApartmentIdAndIsRented(apartmentId, false)
                .stream().map(RentedObject::getId).toList();
        if (ids.isEmpty()) {
            apartmentRepository.updateRentedFlag(apartmentId, true);
        }
    }

    @Override
    @Transactional
    public void addHousingProviders(final ApartmentHousingProviderRequest dto) throws RentowneNotFoundException {
        final Apartment apartment = apartmentRepository.findWithHousingProvidersById(dto.apartmentId()).orElseThrow(() -> new RentowneNotFoundException(dto.apartmentId()));
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
        final Long loggedUserId = userService.getLoggedUser().id();
        return apartmentRepository.findByUser_IdOrderByApartmentName(loggedUserId)
                .stream().map(ApartmentMapper::toBasicDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApartmentDto> getAllFullApartmentsByLoggedUser() throws RentowneBusinessException {
        final Long loggedUserId = userService.getLoggedUser().id();
        final List<Apartment> apartments = apartmentRepository.findByUser_IdOrderByApartmentName(loggedUserId);
        return apartments.stream()
            .map(apartment -> {
                loadRentedObjects(apartment);
                return ApartmentMapper.toDto(apartment);
            })
            .toList();
    }


    private void loadRentedObjects(final Apartment apartment) {
        final List<RentedObject> rentedObjects = rentedObjectRepository.findAllByApartmentId(apartment.getId());
        apartment.setRentedObjects(new HashSet<>(rentedObjects));
    }

    private void validateRentedObjectNames(final ApartmentDto apartmentDto) throws RentowneBusinessException {
        final List<RentedObjectDto> rentedObjectDtos = apartmentDto.rentedObjectDtos();
        if (rentedObjectDtos == null || rentedObjectDtos.isEmpty()) {
            return;
        }

        final Set<String> uniqueNames = new HashSet<>();
        for (final RentedObjectDto dto : rentedObjectDtos) {
            if (!uniqueNames.add(dto.rentedObjectName())) {
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
        if (apartmentId == null || apartmentDto.rentedObjectDtos() == null) {
            return;
        }

        final Set<RentedObject> rentedObjects = ApartmentMapper.toRentedObjectEntities(apartmentDto, apartmentId);
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
}
