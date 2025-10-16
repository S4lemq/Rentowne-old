package pl.rentowne.housing_provider.service;

import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.housing_provider.model.dto.HousingProviderBasicDto;
import pl.rentowne.housing_provider.model.dto.HousingProviderDto;

import java.util.List;

public interface HousingProviderService {
    /**
     * Dodaje dostawcę świadczeń wraz z polami
     *
     * @param dto dostawca świadczeń
     * @return id dodanego dostawcy
     */
    Long addHousingServiceProvider(HousingProviderDto dto) throws RentowneBusinessException;

    /**
     * Zwraca dostawcę świadczeń po id
     * @param id id dostawcy świadczeń
     * @return dto dostawcy świadczeń wraz z jego polami
     */
    HousingProviderDto getHousingProviderById(Long id);

    /**
     * Aktualizuje dane dostawców
     * @param dto dto dostawcy
     */
    void updateHousingProvider(HousingProviderDto dto);

    /**
     * Zwraca wszystkich dostawców dla zalogowanego użytkownika
     * @return lista dostawców (id, name, type)
     */
    List<HousingProviderBasicDto> getHousingProvidersForLoggedUser();
}
