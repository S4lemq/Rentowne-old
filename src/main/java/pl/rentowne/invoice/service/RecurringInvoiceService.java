package pl.rentowne.invoice.service;

import pl.rentowne.invoice.model.dto.RecurringInvoiceDto;

import java.util.List;

public interface RecurringInvoiceService {

    /**
     * Generuje faktury cykliczne dla dostawców z flagą recurringPayment = true
     *
     * @return lista wygenerowanych faktur
     */
    List<RecurringInvoiceDto> generateRecurringInvoices();
}

