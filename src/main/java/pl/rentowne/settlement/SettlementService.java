package pl.rentowne.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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
     * Pobiera paginowaną listę rozliczeń dla danego obiektu do wynajęcia i najemcy
     *
     * @param pageable parametry paginacji i sortowania
     * @param rentedObjectId id obiektu do wynajęcia
     * @param tenantId id najemcy
     * @return strona z listą rozliczeń
     */
    Page<SettlementListDto> getSettlements(Pageable pageable, Long rentedObjectId, Long tenantId);

    /**
     * Pobiera wszystkie rozliczenia bez filtrowania.
     *
     * @return lista rozliczeń
     */
    List<SettlementListDto> getAllSettlements();

    /**
     * Pobiera wszystkie rozliczenia przygotowane pod eksport do Excela.
     *
     * @return lista wierszy eksportu
     */
    List<SettlementExportRowDto> getAllSettlementsForExport();

    /**
     * Oblicza koszty za media (prąd, woda) bez zapisu w bazie
     * @param rentedObjectId id obiektu do wynajęcia
     * @return koszty i zużycie mediów
     */
    UtilityCostsDto calculateUtilityCostsOnly(Long rentedObjectId);

    /**
     * Sprawdza czy istnieją jakieś rozliczenia dla danego najemcy
     * @param tenantId id najemcy
     * @return true jeśli istnieje co najmniej jedno rozliczenie, false w przeciwnym razie
     */
    boolean existsSettlementsByTenantId(Long tenantId);
}
