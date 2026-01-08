package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

import java.util.List;

/**
 * Interfejs dla obiektów, które mogą być przekształcone na wiersz tabeli PDF.
 * Rekordy lub klasy implementujące ten interfejs mogą być bezpośrednio
 * przekazywane do metody {@link PdfDocumentBuilder#createTable(PdfTableConfig, List)}.
 */
public interface PdfTableRow {

    /**
     * Konwertuje obiekt na listę wartości tekstowych reprezentujących wiersz tabeli.
     *
     * @return lista wartości komórek wiersza
     */
    @NonNull
    List<String> toTableRow();
}

