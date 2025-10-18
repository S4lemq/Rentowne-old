package pl.rentowne.invoice.service;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Serwis obsługi plików PDF faktur
 */
public interface InvoicePdfService {

    /**
     * Wgrywa plik PDF faktury
     *
     * @param filename nazwa pliku
     * @param inputStream strumień danych pliku
     * @return nazwa zapisanego pliku
     */
    String uploadInvoicePdf(String filename, InputStream inputStream);

    /**
     * Pobiera plik PDF faktury
     *
     * @param filename nazwa pliku
     * @return zasób pliku
     */
    Resource getInvoicePdf(String filename);

    /**
     * Usuwa plik PDF faktury
     *
     * @param filename nazwa pliku
     */
    void deleteInvoicePdf(String filename);
}

