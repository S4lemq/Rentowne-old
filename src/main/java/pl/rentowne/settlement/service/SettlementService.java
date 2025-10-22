package pl.rentowne.settlement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.rentowne.rented_object.model.dto.CalculateRequestDto;
import pl.rentowne.settlement.model.dto.SettlementListDto;

public interface SettlementService {

    /**
     * oblicza poszczególne media po licznikach i danych dostawcy
     * @param rentedObjectId id obiektu do wynajęcia
     * @param dto informacje:
     * czy rozliczyć wodę
     * czy rozliczyć energię elektryczną
     * data rozliczenia
     */
    void calculate(Long rentedObjectId, CalculateRequestDto dto);

    /**
     * usuwa wyliczone rozliczenie
     * @param id id rozliczenia
     */
    void deleteCalculate(Long id);

    /**
     * Pobiera paginowaną listę rozliczeń dla danego obiektu do wynajęcia
     *
     * @param pageable parametry paginacji i sortowania
     * @param rentedObjectId id obiektu do wynajęcia
     * @return strona z listą rozliczeń
     */
    Page<SettlementListDto> getSettlements(Pageable pageable, Long rentedObjectId);
}
