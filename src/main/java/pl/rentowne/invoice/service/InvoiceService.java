package pl.rentowne.invoice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.rentowne.invoice.model.InvoiceFilterType;
import pl.rentowne.invoice.model.dto.InvoiceDto;
import pl.rentowne.invoice.model.dto.InvoiceListDto;
import pl.rentowne.invoice.model.dto.InvoiceSummaryDto;

/**
 * Serwis obsługi faktur
 */
public interface InvoiceService {

    /**
     * Dodaje nową fakturę
     *
     * @param dto dane faktury
     * @return id dodanej faktury
     */
    Long addInvoice(InvoiceDto dto);

    /**
     * Pobiera fakturę po id
     *
     * @param id id faktury
     * @return dto faktury
     */
    InvoiceDto getInvoiceById(Long id);

    /**
     * Aktualizuje dane faktury
     *
     * @param dto dane faktury do aktualizacji
     */
    void updateInvoice(InvoiceDto dto);

    /**
     * Usuwa fakturę
     *
     * @param id id faktury do usunięcia
     */
    void deleteInvoice(Long id);

    /**
     * Pobiera paginowaną listę faktur
     *
     * @param pageable parametry paginacji i sortowania
     * @return strona z listą faktur
     */
    Page<InvoiceListDto> getInvoices(Pageable pageable);

    /**
     * Pobiera paginowaną listę faktur z opcjonalnym filtrowaniem
     *
     * @param pageable parametry paginacji i sortowania
     * @param filterType opcjonalny typ filtra
     * @return strona z listą faktur
     */
    Page<InvoiceListDto> getInvoices(Pageable pageable, InvoiceFilterType filterType);

    /**
     * Pobiera podsumowanie faktur
     *
     * @return podsumowanie zawierające sumy płatności
     */
    InvoiceSummaryDto getInvoiceSummary();
}
