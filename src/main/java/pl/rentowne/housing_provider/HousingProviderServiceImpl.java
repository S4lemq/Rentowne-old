package pl.rentowne.housing_provider;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.provider_field.model.dto.ProviderFieldDto;
import pl.rentowne.provider_field.repository.ProviderFieldRepository;
import pl.rentowne.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HousingProviderServiceImpl implements HousingProviderService {

    private final HousingProviderRepository housingProviderRepository;
    private final ProviderFieldRepository providerFieldRepository;
    private final UserService userService;
    private final HousingProviderMapper mapper;

    @Override
    @Transactional
    public Long addHousingServiceProvider(final HousingProviderDto dto) throws RentowneBusinessException {
        final Long userId = userService.getLoggedUser().getId();
        return housingProviderRepository.save(mapper.toEntity(dto, userId)).getId();
    }

    @Override
    public HousingProviderDto getHousingProviderById(final Long id) {
        final HousingProvider housingProvider = housingProviderRepository.findWithProviderFieldsById(id).orElseThrow(() -> new RentowneNotFoundException(id));
        return mapper.toDto(housingProvider);
    }

    @Override
    @Transactional
    public void updateHousingProvider(final HousingProviderDto dto) {
        final HousingProvider entity = housingProviderRepository.findWithProviderFieldsById(dto.id()).orElseThrow(() -> new RentowneNotFoundException(dto.id()));

        // Aktualizacja podstawowych pól
        entity.setName(dto.name());
        entity.setType(dto.type());
        entity.setTax(dto.tax());
        entity.setConversionRate(dto.conversionRate());
        entity.setRecurringPayment(dto.recurringPayment());

        // Aktualizacja pól ProviderField, jeśli są dostępne
        if (dto.providerFieldDtos() != null) {
            updateProviderFields(dto.providerFieldDtos(), entity);
        }

        housingProviderRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingProviderBasicDto> getHousingProvidersForLoggedUser() {
        final Long loggedUserId = userService.getLoggedUser().getId();
        final List<HousingProvider> providers = housingProviderRepository.findAllByUserId(loggedUserId);
        return providers.stream()
                .map(mapper::toBasicDto)
                .collect(Collectors.toList());
    }

    private void updateProviderFields(final List<ProviderFieldDto> fieldsDto, final HousingProvider housingProvider) {
        final List<ProviderField> currentFields = housingProvider.getProviderFields();
        // Usuwanie, aktualizowanie istniejących i dodawanie nowych pól
        final Map<Long, ProviderFieldDto> dtoMap = fieldsDto.stream()
                .filter(fieldDto -> fieldDto.id() != null)
                .collect(Collectors.toMap(ProviderFieldDto::id, Function.identity()));

        // Usuń istniejące ProviderField, których nie ma w dto
        final List<ProviderField> fieldsToRemove = currentFields.stream()
                .filter(field -> !dtoMap.containsKey(field.getId()))
                .collect(Collectors.toList());
        currentFields.removeAll(fieldsToRemove);
        housingProvider.setProviderFields(currentFields); // Aktualizuj kolekcję w encji
        providerFieldRepository.deleteAll(fieldsToRemove); // Usuń z bazy danych

        // Aktualizuj istniejące i dodaj nowe
        for (final ProviderFieldDto fieldDto : fieldsDto) {
            if (fieldDto.id() != null) {
                // Aktualizuj istniejące
                final ProviderField existingField = currentFields.stream()
                        .filter(field -> field.getId().equals(fieldDto.id()))
                        .findFirst()
                        .orElse(null);
                if (existingField != null) {
                    existingField.setName(fieldDto.name());
                    existingField.setPrice(fieldDto.price());
                    existingField.setBillingMethod(fieldDto.billingMethod());
                }
            } else {
                // Dodaj nowe
                final ProviderField newField = ProviderField.builder()
                        .name(fieldDto.name())
                        .price(fieldDto.price())
                        .billingMethod(fieldDto.billingMethod())
                        .housingProvider(housingProvider)
                        .build();
                currentFields.add(newField);
            }
        }

    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderListDto> getHousingProvidersList(final Pageable pageable, final Long apartmentId, final String searchText) {
        final Long userAccountId = userService.getLoggedUser().getId();
        return housingProviderRepository.getHousingProvidersList(pageable, userAccountId, apartmentId, searchText);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderApartmentDto> getHousingProvidersForApartment(final Pageable pageable, final Long apartmentId) {
        final Long userAccountId = userService.getLoggedUser().getId();
        return housingProviderRepository.getHousingProvidersForApartment(pageable, userAccountId, apartmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderWithAssignmentDto> getAllProvidersWithAssignmentStatus(final Pageable pageable, final Long apartmentId) {
        final Long userAccountId = userService.getLoggedUser().getId();
        return housingProviderRepository.getAllProvidersWithAssignmentStatus(pageable, userAccountId, apartmentId);
    }

}
