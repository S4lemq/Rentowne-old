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
import pl.rentowne.user.model.User;
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

    @Override
    @Transactional
    public Long addHousingServiceProvider(HousingProviderDto dto) throws RentowneBusinessException {
        return housingProviderRepository.save(this.asEntity(dto)).getId();
    }

    @Override
    public HousingProviderDto getHousingProviderById(Long id) {
        HousingProvider housingProvider = housingProviderRepository.findWithProviderFieldsById(id).orElseThrow(() -> new RentowneNotFoundException(id));
        return this.asDto(housingProvider);
    }

    @Override
    @Transactional
    public void updateHousingProvider(HousingProviderDto dto) {
        HousingProvider entity = housingProviderRepository.findWithProviderFieldsById(dto.getId()).orElseThrow(() -> new RentowneNotFoundException(dto.getId()));

        // Aktualizacja podstawowych pól
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setTax(dto.getTax());
        entity.setConversionRate(dto.getConversionRate());
        entity.setRecurringPayment(dto.isRecurringPayment());

        // Aktualizacja pól ProviderField, jeśli są dostępne
        if (dto.getProviderFieldDtos() != null) {
            updateProviderFields(dto.getProviderFieldDtos(), entity);
        }

        housingProviderRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingProviderBasicDto> getHousingProvidersForLoggedUser() {
        Long loggedUserId = userService.getLoggedUser().getId();

        List<HousingProvider> providers = housingProviderRepository.findAllByUserId(loggedUserId);

        return providers.stream()
                .map(provider -> HousingProviderBasicDto.builder()
                        .id(provider.getId())
                        .name(provider.getName())
                        .type(provider.getType())
                        .recurringPayment(provider.isRecurringPayment())
                        .build())
                .collect(Collectors.toList());
    }

    private void updateProviderFields(List<ProviderFieldDto> fieldsDto, HousingProvider housingProvider) {
        List<ProviderField> currentFields = housingProvider.getProviderFields();
        // Usuwanie, aktualizowanie istniejących i dodawanie nowych pól
        Map<Long, ProviderFieldDto> dtoMap = fieldsDto.stream()
                .filter(fieldDto -> fieldDto.getId() != null)
                .collect(Collectors.toMap(ProviderFieldDto::getId, Function.identity()));

        // Usuń istniejące ProviderField, których nie ma w dto
        List<ProviderField> fieldsToRemove = currentFields.stream()
                .filter(field -> !dtoMap.containsKey(field.getId()))
                .collect(Collectors.toList());
        currentFields.removeAll(fieldsToRemove);
        housingProvider.setProviderFields(currentFields); // Aktualizuj kolekcję w encji
        providerFieldRepository.deleteAll(fieldsToRemove); // Usuń z bazy danych

        // Aktualizuj istniejące i dodaj nowe
        fieldsDto.forEach(fieldDto -> {
            if (fieldDto.getId() != null) {
                // Aktualizuj istniejące
                ProviderField existingField = currentFields.stream()
                        .filter(field -> field.getId().equals(fieldDto.getId()))
                        .findFirst()
                        .orElse(null);
                if (existingField != null) {
                    existingField.setName(fieldDto.getName());
                    existingField.setPrice(fieldDto.getPrice());
                    existingField.setBillingMethod(fieldDto.getBillingMethod());
                }
            } else {
                // Dodaj nowe
                ProviderField newField = ProviderField.builder()
                        .name(fieldDto.getName())
                        .price(fieldDto.getPrice())
                        .billingMethod(fieldDto.getBillingMethod())
                        .housingProvider(housingProvider)
                        .build();
                currentFields.add(newField);
            }
        });

    }

    private HousingProvider asEntity(HousingProviderDto dto) throws RentowneBusinessException {
        Long loggedUserId = userService.getLoggedUser().getId();

        HousingProvider housingProvider = HousingProvider.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .tax(dto.getTax())
                .conversionRate(dto.getConversionRate())
                .recurringPayment(dto.isRecurringPayment())
                .user(new User(loggedUserId))
                .build();

        List<ProviderField> fields = dto.getProviderFieldDtos().stream()
                .map(field -> ProviderField.builder()
                        .id(field.getId())
                        .name(field.getName())
                        .price(field.getPrice())
                        .billingMethod(field.getBillingMethod())
                        .housingProvider(housingProvider)
                        .build())
                .toList();

        housingProvider.setProviderFields(fields);
        return housingProvider;
    }

    private HousingProviderDto asDto(HousingProvider entity) {
        return HousingProviderDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .tax(entity.getTax())
                .conversionRate(entity.getConversionRate())
                .recurringPayment(entity.isRecurringPayment())
                .providerFieldDtos(
                        entity.getProviderFields().stream().map(
                                field -> ProviderFieldDto.builder()
                                        .id(field.getId())
                                        .name(field.getName())
                                        .price(field.getPrice())
                                        .billingMethod(field.getBillingMethod())
                                        .build()
                        ).toList()
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderListDto> getHousingProvidersList(Pageable pageable, Long apartmentId, String searchText) {
        Long userAccountId = userService.getLoggedUser().getId();
        return housingProviderRepository.getHousingProvidersList(pageable, userAccountId, apartmentId, searchText);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderApartmentDto> getHousingProvidersForApartment(Pageable pageable, Long apartmentId) {
        Long userAccountId = userService.getLoggedUser().getId();
        return housingProviderRepository.getHousingProvidersForApartment(pageable, userAccountId, apartmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderWithAssignmentDto> getAllProvidersWithAssignmentStatus(Pageable pageable, Long apartmentId) {
        Long userAccountId = userService.getLoggedUser().getId();
        return housingProviderRepository.getAllProvidersWithAssignmentStatus(pageable, userAccountId, apartmentId);
    }

}
