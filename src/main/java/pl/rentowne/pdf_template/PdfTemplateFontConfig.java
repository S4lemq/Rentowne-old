package pl.rentowne.pdf_template;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import pl.rentowne.pdf_template.exception.PdfTemplateErrorCode;
import pl.rentowne.pdf_template.exception.PdfTemplateException;

import java.io.IOException;

/**
 * Konfiguracja czcionek dla OpenHTMLtoPDF.
 * Ładuje czcionki Inter z zasobów aplikacji.
 */
@Slf4j
@Component
public class PdfTemplateFontConfig {

    private static final String FONTS_PATH = "fonts/";

    /**
     * Konfiguruje czcionki w builderze PDF.
     *
     * @param builder builder PDF renderera
     */
    public void configureFonts(@NonNull PdfRendererBuilder builder) {
        // Inter Regular
        addFont(builder, "Inter-Regular.ttf", "Inter", 400, FontStyle.NORMAL);

        // Inter Bold
        addFont(builder, "Inter-Bold.ttf", "Inter", 700, FontStyle.NORMAL);

        // Inter Italic
        addFont(builder, "Inter-Italic.ttf", "Inter", 400, FontStyle.ITALIC);

        // Inter Bold Italic
        addFont(builder, "Inter-BoldItalic.ttf", "Inter", 700, FontStyle.ITALIC);
    }

    private void addFont(PdfRendererBuilder builder, String fileName, String fontFamily,
                         int fontWeight, FontStyle fontStyle) {
        ClassPathResource resource = new ClassPathResource(FONTS_PATH + fileName);
        if (resource.exists()) {
            builder.useFont(() -> {
                try {
                    return resource.getInputStream();
                } catch (IOException e) {
                    throw new PdfTemplateException(PdfTemplateErrorCode.FONT_LOADING_ERROR, fileName, e);
                }
            }, fontFamily, fontWeight, fontStyle, true);
        } else {
            log.warn("Czcionka {} nie została znaleziona w ścieżce {}", fileName, FONTS_PATH);
        }
    }
}

