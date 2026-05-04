package pl.rentowne.invoice;

import java.util.List;

interface RecurringInvoiceService {

    /**
     * Generuje faktury cykliczne dla dostawców z flagą recurringPayment = true
     *
     * @return lista wygenerowanych faktur
     */
    List<RecurringInvoiceDto> generateRecurringInvoices();
}
