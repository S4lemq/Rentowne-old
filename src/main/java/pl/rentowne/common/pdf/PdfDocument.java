package pl.rentowne.common.pdf;

import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Model reprezentujący dokument PDF do wygenerowania.
 * Zawiera metadane oraz zawartość dokumentu w postaci elementów.
 */
@Getter
@Builder
public final class PdfDocument {

    @NonNull
    @Builder.Default
    private final String title = "Dokument PDF";

    @Nullable
    private final String author;

    @Nullable
    private final String subject;

    @NonNull
    @Builder.Default
    private final List<PdfElement> elements = new ArrayList<>();

    /**
     * Zwraca opcjonalnego autora dokumentu.
     *
     * @return Optional zawierający autora lub pusty
     */
    @NonNull
    public Optional<String> getAuthor() {
        return Optional.ofNullable(author);
    }

    /**
     * Zwraca opcjonalny temat dokumentu.
     *
     * @return Optional zawierający temat lub pusty
     */
    @NonNull
    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Zwraca niemodyfikowalną listę elementów dokumentu.
     *
     * @return lista elementów PDF
     */
    @NonNull
    public List<PdfElement> getElements() {
        return Collections.unmodifiableList(elements);
    }
}

