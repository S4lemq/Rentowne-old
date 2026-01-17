package pl.rentowne.apartment;

import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneNotFoundException;

import java.util.List;

/**
 * Serwis mieszkań
 */
public interface ApartmentService {

    /**
     * Pobiera mieszkanie po id
     *
     * @param id id mieszkania
     * @return {@link ApartmentDto} dto mieszkania
     * @throws RentowneNotFoundException - nie znaleziono mieszkania
     */
    ApartmentDto getApartment(Long id) throws RentowneNotFoundException;

    /**
     * Dodaje mieszkanie
     * @param apartmentDto dto mieszkania
     * @return id zapisanego mieszkania
     */
    Long addApartment(ApartmentDto apartmentDto) throws RentowneBusinessException;

    /**
     * aktualizuje dane mieszkania
     * @param apartmentDto dto mieszkania
     * @param apartmentId id mieszkania
     * @throws RentowneBusinessException wyjątek biznesowy
     */
    void updateApartment(ApartmentDto apartmentDto, Long apartmentId) throws RentowneBusinessException;

    /**
     * Pobiera wszystkie mieszkania zalogowanego użytkownika po oraz opcjonalnie dodaje do wyników jeszcze przekazany parametr apartmentId
     * @param apartmentId id mieszkania
     * @return lista mieszkań
     */
    List<ApartmentDto> getAllApartmentsByLoggedUserAndApartment(Long apartmentId) throws RentowneBusinessException;

    /**
     * Aktualizuje flagę czy wynajęte
     * @param rentedObjectId id obiektu
     */
    void updateRentedFlagOnApartment(Long rentedObjectId);

    /**
     * Przypisuje mieszkanie do dostawcy świadczeń
     * @param dto {@link ApartmentHousingProviderRequest} dto mieszkania i idków dostawców
     * @throws RentowneNotFoundException nie znaleziono mieszkania/dostawcy
     */
    void addHousingProviders(ApartmentHousingProviderRequest dto) throws RentowneNotFoundException;

    /**
     * Służy do przypinania nieruchomości
     * @param isPinned czy przypięto
     * @param id id mieszkania
     */
    void pinApartment(boolean isPinned, long id);

    /**
     * Pobiera wszystkie mieszkania zalogowanego użytkownika
     * @return lista mieszkań
     * @throws RentowneBusinessException wyjątek biznesowy
     */
    List<ApartmentDto> getAllApartmentsByLoggedUser() throws RentowneBusinessException;

    /**
     * Pobiera wszystkie mieszkania zalogowanego użytkownika z pełnymi danymi (do generowania PDF)
     * @return lista mieszkań z pełnymi danymi
     * @throws RentowneBusinessException wyjątek biznesowy
     */
    List<ApartmentDto> getAllFullApartmentsByLoggedUser() throws RentowneBusinessException;
}
