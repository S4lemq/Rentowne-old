package pl.rentowne.pdf;

import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.model.EventSeverity;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;
import org.apache.xmlgraphics.util.MimeConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public final class FopPdfRenderer {
    private final FopFactory fop;
    private final Map<String, Templates> templates;

    public FopPdfRenderer() {
        var resources = new ClasspathResources();
        try (var configuration = new ClassPathResource("pdf/fop.xconf").getInputStream()) {
            fop = new FopConfParser(configuration, URI.create("classpath:/"), resources).getFopFactoryBuilder()
                    .setComplexScriptFeatures(false).build();
            TransformerFactory transformer = TransformerFactory.newDefaultInstance();
            transformer.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformer.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformer.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformer.setURIResolver((href, base) -> {
                URI uri = URI.create(base).resolve(href);
                try { return new StreamSource(resources.getResource(uri), uri.toString()); }
                catch (IOException error) { throw new javax.xml.transform.TransformerException(error); }
            });
            Map<String, Templates> loaded = new HashMap<>();
            for (String name : new String[]{"receipt", "deposit-settlement"}) {
                String resource = "pdf/" + name + ".xsl";
                try (var source = new ClassPathResource(resource).getInputStream()) {
                    loaded.put(name, transformer.newTemplates(new StreamSource(source, "classpath:/" + resource)));
                }
            }
            templates = Map.copyOf(loaded);
        } catch (IOException | org.xml.sax.SAXException | javax.xml.transform.TransformerConfigurationException error) {
            throw new PdfRenderingException("Nie można załadować konfiguracji lub szablonów PDF", error);
        }
    }

    public byte[] render(String template, String title, Document data) {
        Templates layout = templates.get(template);
        if (layout == null) throw new PdfRenderingException("Nieznany szablon PDF: " + template);
        try (var output = new ByteArrayOutputStream()) {
            var agent = fop.newFOUserAgent();
            agent.setTitle(title);
            agent.setAuthor("Rentowne Nieruchomości");
            agent.setCreator("Rentowne");
            agent.getEventBroadcaster().addEventListener(event -> {
                String id = event.getEventID().toLowerCase(java.util.Locale.ROOT);
                if (event.getSeverity() == EventSeverity.ERROR || event.getSeverity() == EventSeverity.FATAL
                        || id.contains("overflow") || id.contains("fontsubstituted") || id.contains("glyphnotavailable")) {
                    throw new PdfRenderingException("Niepoprawny układ lub zasób PDF: " + org.apache.fop.events.EventFormatter.format(event));
                }
            });
            var renderer = fop.newFop(MimeConstants.MIME_PDF, agent, output);
            layout.newTransformer().transform(new DOMSource(data), new SAXResult(renderer.getDefaultHandler()));
            return output.toByteArray();
        } catch (IOException | javax.xml.transform.TransformerException | org.apache.fop.apps.FOPException error) {
            throw new PdfRenderingException("Nie można wygenerować dokumentu PDF", error);
        }
    }

    /** Resources are read from the application classpath, also when running an executable JAR. */
    private static final class ClasspathResources implements ResourceResolver {
        @Override
        public Resource getResource(URI uri) throws IOException {
            URI normalized = uri.normalize();
            String path = normalized.getPath();
            if (!"classpath".equals(normalized.getScheme()) || path == null
                    || !(path.startsWith("/pdf/") || path.startsWith("/fonts/") || path.equals("/image/logo_rentowne.png"))) {
                throw new IOException("PDF może korzystać wyłącznie z zasobów aplikacji: " + normalized);
            }
            return new Resource(new ClassPathResource(path.substring(1)).getInputStream());
        }

        @Override
        public OutputStream getOutputStream(URI uri) throws IOException {
            throw new IOException("Zasoby szablonów PDF są tylko do odczytu");
        }
    }
}
