package pl.rentowne.meter.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.meter.model.dto.MeterListDto;

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
     * Pobiera dto licznika po jego id
     * @param id id licznika
     * @return dto licznika
     */
    MeterDto getMeterById(Long id);

    /**
     * Aktualizuje dane licznika
     * @param meter dto licznika
     */
    void updateMeter(MeterDto meter);

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
     * @param searchText opcjonalny tekst do filtrowania po apartmentName i rentedObjectName
     * @return strona z listą liczników
     */
    Page<MeterListDto> getMetersList(Pageable pageable, Long apartmentId, String searchText);
}
