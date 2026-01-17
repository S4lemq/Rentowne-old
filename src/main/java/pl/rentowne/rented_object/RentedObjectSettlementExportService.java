package pl.rentowne.rented_object;

import pl.rentowne.settlement.model.dto.SettlementExportDto;

import java.time.LocalDateTime;
import java.util.List;

public interface RentedObjectSettlementExportService {

    /**
     * pobiera dane do eksportu rozliczenia obiektu do pliku csv
     * @param from od podanej daty
     * @param to do podanej daty
     * @param rentedObjectId id obiektu do wynajęcia
     * @return zwraca
     */
    List<SettlementExportDto> exportSettlements(LocalDateTime from, LocalDateTime to, Long rentedObjectId);
}
