package pl.rentowne.common.mail;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import pl.rentowne.rental_document.DocumentProperties;

import java.nio.charset.StandardCharsets;

/** Synchronous transport invoked only by an explicitly confirmed document send operation. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentEmailService {
    public enum Result { SENT, SIMULATED }

    private final JavaMailSender sender;
    private final DocumentProperties properties;
    @Value("${application.email.sender:emailSimpleService}")
    private String senderMode;

    public Result send(DocumentMail mail) {
        if ("fakeEmailService".equals(senderMode)) {
            log.info("Document email simulated: {}", mail.subject());
            return Result.SIMULATED;
        }
        var message = sender.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setValidateAddresses(true);
            helper.setFrom(properties.from());
            helper.setReplyTo(properties.replyTo());
            helper.setTo(mail.to());
            if (!mail.cc().isEmpty()) helper.setCc(mail.cc().toArray(String[]::new));
            helper.setSubject(mail.subject());
            helper.setText(mail.text(), "<html><body><div style=\"font-family:Arial,sans-serif;color:#1f2b3a;line-height:1.6;max-width:640px\">"
                    + HtmlUtils.htmlEscape(mail.text()).replace("\n", "<br>") + "</div></body></html>");
            if (mail.pdf() != null) helper.addAttachment(mail.filename(), new ByteArrayResource(mail.pdf()), "application/pdf");
        } catch (MessagingException e) {
            throw new MailPreparationException("Nie można przygotować wiadomości z dokumentem", e);
        }
        sender.send(message);
        return Result.SENT;
    }
}
