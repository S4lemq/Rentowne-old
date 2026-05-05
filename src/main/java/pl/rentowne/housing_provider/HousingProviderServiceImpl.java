package pl.rentowne.housing_provider;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.provider_field.ProviderField;
import pl.rentowne.provider_field.ProviderFieldDto;
import pl.rentowne.provider_field.ProviderFieldRepository;
import pl.rentowne.user.UserService;

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
    public Long addHousingServiceProvider(final HousingProviderDto dto) throws RentowneBusinessException {
        final Long userId = getLoggedUserId();
        final HousingProvider entity = HousingProviderMapper.toEntity(dto, userId);
        return housingProviderRepository.save(entity).getId();
    }

    @Override
    public HousingProviderDto getHousingProviderById(final Long id) {
        final HousingProvider housingProvider = housingProviderRepository.findWithProviderFieldsById(id)
                .orElseThrow(() -> new RentowneNotFoundException(id));
        return HousingProviderMapper.toDto(housingProvider);
    }

    @Override
    @Transactional
    public void updateHousingProvider(final HousingProviderDto dto) {
        final HousingProvider entity = housingProviderRepository.findWithProviderFieldsById(dto.id())
                .orElseThrow(() -> new RentowneNotFoundException(dto.id()));

        HousingProviderMapper.updateEntity(entity, dto);

        if (dto.providerFieldDtos() != null) {
            updateProviderFields(dto.providerFieldDtos(), entity);
        }

        housingProviderRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingProviderBasicDto> getHousingProvidersForLoggedUser() {
        final Long userId = getLoggedUserId();
        return housingProviderRepository.findAllByUserId(userId).stream()
                .map(HousingProviderMapper::toBasicDto)
                .toList();
    }

    private void updateProviderFields(final List<ProviderFieldDto> fieldsDto, final HousingProvider housingProvider) {
        final List<ProviderField> currentFields = housingProvider.getProviderFields();
        final Map<Long, ProviderFieldDto> dtoMap = fieldsDto.stream()
                .filter(fieldDto -> fieldDto.id() != null)
                .collect(Collectors.toMap(ProviderFieldDto::id, Function.identity()));

        final List<ProviderField> fieldsToRemove = currentFields.stream()
                .filter(field -> !dtoMap.containsKey(field.getId()))
                .toList();

        currentFields.removeAll(fieldsToRemove);
        providerFieldRepository.deleteAll(fieldsToRemove);

        for (final ProviderFieldDto fieldDto : fieldsDto) {
            if (fieldDto.id() != null) {
                currentFields.stream()
                        .filter(field -> field.getId().equals(fieldDto.id()))
                        .findFirst()
                        .ifPresent(existingField -> HousingProviderMapper.updateProviderFieldEntity(existingField, fieldDto));
            } else {
                final ProviderField newField = HousingProviderMapper.toProviderFieldEntity(fieldDto, housingProvider);
                currentFields.add(newField);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderListDto> getHousingProvidersList(final Pageable pageable, final Long apartmentId, final String searchText) {
        final Long userId = getLoggedUserId();
        return housingProviderRepository.getHousingProvidersList(pageable, userId, apartmentId, searchText);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderApartmentDto> getHousingProvidersForApartment(final Pageable pageable, final Long apartmentId) {
        final Long userId = getLoggedUserId();
        return housingProviderRepository.getHousingProvidersForApartment(pageable, userId, apartmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HousingProviderWithAssignmentDto> getAllProvidersWithAssignmentStatus(final Pageable pageable, final Long apartmentId) {
        final Long userId = getLoggedUserId();
        return housingProviderRepository.getAllProvidersWithAssignmentStatus(pageable, userId, apartmentId);
    }

    private Long getLoggedUserId() {
        return userService.getLoggedUser().id();
    }
}
