package pl.rentowne.pdf_template;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pl.rentowne.pdf_template.exception.PdfTemplateErrorCode;
import pl.rentowne.pdf_template.exception.PdfTemplateException;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Serwis do generowania dokumentów PDF z szablonów HTML.
 * Wykorzystuje Thymeleaf do renderowania HTML i OpenHTMLtoPDF do konwersji na PDF.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfTemplateService {

    private final TemplateEngine templateEngine;
    private final PdfTemplateFontConfig fontConfig;

    /**
     * Generuje dokument PDF z szablonu HTML.
     *
     * @param templateName nazwa szablonu (bez rozszerzenia, np. "pdf/invoice-template")
     * @param variables    zmienne do przekazania do szablonu
     * @param filename     nazwa pliku wynikowego (bez rozszerzenia .pdf)
     * @return odpowiedź z wygenerowanym PDF
     */
    @NonNull
    public PdfTemplateResponse generatePdf(@NonNull String templateName,
                                           @NonNull Map<String, Object> variables,
                                           @NonNull String filename) {
        log.debug("Generowanie PDF z szablonu: {}, nazwa pliku: {}", templateName, filename);

        try {
            // 1. Renderowanie HTML z Thymeleaf
            String html = renderHtml(templateName, variables);

            // 2. Konwersja HTML na PDF
            byte[] pdfContent = convertHtmlToPdf(html);

            // 3. Zwracanie odpowiedzi
            String finalFilename = filename.endsWith(".pdf") ? filename : filename + ".pdf";

            log.debug("PDF wygenerowany pomyślnie, rozmiar: {} bajtów", pdfContent.length);
            return new PdfTemplateResponse(pdfContent, finalFilename);

        } catch (PdfTemplateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Błąd podczas generowania PDF z szablonu: {}", templateName, e);
            throw new PdfTemplateException(PdfTemplateErrorCode.PDF_GENERATION_ERROR, e);
        }
    }

    /**
     * Generuje dokument PDF z szablonu HTML używając kontekstu Thymeleaf.
     *
     * @param templateName nazwa szablonu
     * @param context      kontekst Thymeleaf z danymi
     * @param filename     nazwa pliku wynikowego
     * @return odpowiedź z wygenerowanym PDF
     */
    @NonNull
    public PdfTemplateResponse generatePdf(@NonNull String templateName,
                                           @NonNull Context context,
                                           @NonNull String filename) {
        try {
            String html = templateEngine.process(templateName, context);
            byte[] pdfContent = convertHtmlToPdf(html);
            String finalFilename = filename.endsWith(".pdf") ? filename : filename + ".pdf";
            return new PdfTemplateResponse(pdfContent, finalFilename);
        } catch (PdfTemplateException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfTemplateException(PdfTemplateErrorCode.PDF_GENERATION_ERROR, e);
        }
    }

    /**
     * Renderuje szablon HTML z podanymi zmiennymi.
     *
     * @param templateName nazwa szablonu
     * @param variables    zmienne
     * @return wyrenderowany HTML
     */
    @NonNull
    public String renderHtml(@NonNull String templateName, @NonNull Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            throw new PdfTemplateException(PdfTemplateErrorCode.TEMPLATE_PROCESSING_ERROR, templateName, e);
        }
    }

    /**
     * Konwertuje HTML na PDF.
     *
     * @param html kod HTML
     * @return zawartość PDF jako tablica bajtów
     */
    @NonNull
    public byte[] convertHtmlToPdf(@NonNull String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Konfiguracja czcionek
            fontConfig.configureFonts(builder);

            // Konfiguracja buildera
//            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);

            // Generowanie PDF
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new PdfTemplateException(PdfTemplateErrorCode.PDF_GENERATION_ERROR, e);
        }
    }
}

