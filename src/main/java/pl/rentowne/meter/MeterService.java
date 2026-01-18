package pl.rentowne.meter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Serwis liczników
 */
public interface MeterService {
    /**
     * Dodaje licznik
     * @param meter dto licznika {@link MeterDto}
     * @return zapisane dto licznika
     */
    MeterDto addMeter(MeterDto meter);

    /**
     * Dodaje licznik przypisany do mieszkania
     * @param meter dto licznika {@link ApartmentMeterDto}
     * @return zapisane dto licznika
     */
    ApartmentMeterDto addApartmentMeter(ApartmentMeterDto meter);

    /**
     * Pobiera dto licznika po jego id
     * @param id id licznika
     * @return dto licznika
     */
    MeterDto getMeterById(Long id);

    /**
     * Pobiera listę liczników przypisanych do mieszkania
     * @param apartmentId id mieszkania
     * @return lista liczników mieszkania
     */
    List<ApartmentMeterDto> getApartmentMeters(Long apartmentId);

    /**
     * Aktualizuje dane licznika
     * @param meter dto licznika
     */
    void updateMeter(MeterDto meter);

    /**
     * Aktualizuje licznik przypisany do mieszkania
     * @param meter dto licznika mieszkania
     */
    void updateApartmentMeter(ApartmentMeterDto meter);

    /**
     *
     * Pobiera ilość wymaganych liczników dla danego obiektu
     * @param id obiektu
     */
    void getMeterCountByRentedObject(Long id);

    /**
     * Pobiera paginowaną listę liczników dla zalogowanego użytkownika
     * @param pageable parametry paginacji i sortowania
     * @param apartmentId opcjonalne id mieszkania do filtrowania
     * @param meterType opcjonalny typ licznika do filtrowania
     * @param searchText opcjonalny tekst do filtrowania po apartmentName i rentedObjectName
     * @return strona z listą liczników
     */
    Page<MeterListDto> getMetersList(Pageable pageable, Long apartmentId, MeterType meterType, String searchText);
}
