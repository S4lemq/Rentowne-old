package pl.rentowne.pdf;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Predefiniowane rozmiary czcionek.
 */
@Getter
@RequiredArgsConstructor
public enum FontSize {
    SMALL(8f),
    NORMAL(10f),
    MEDIUM(12f),
    LARGE(14f),
    EXTRA_LARGE(18f);

    private final float size;
}

