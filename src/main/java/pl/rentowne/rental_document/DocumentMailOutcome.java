package pl.rentowne.rental_document;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class DocumentMailOutcome {
    private DocumentMailOutcome() {}
    static String outcome(MailException error) {
        if (error instanceof MailAuthenticationException || error instanceof MailPreparationException) return "FAILED";
        var pending = new ArrayDeque<Throwable>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(error);
        boolean retry = false, rejected = false;
        while (!pending.isEmpty()) {
            Throwable item = pending.removeFirst();
            if (!seen.add(item)) continue;
            if (item instanceof SendFailedException failed && failed.getValidSentAddresses() != null
                    && failed.getValidSentAddresses().length > 0) return "UNKNOWN";
            if (item instanceof ConnectException || item instanceof UnknownHostException) retry = true;
            int code = item instanceof SMTPSendFailedException failed ? failed.getReturnCode()
                    : item instanceof SMTPAddressFailedException failed ? failed.getReturnCode() : 0;
            if (code >= 400 && code < 500) retry = true;
            if (code >= 500 && code < 600) rejected = true;
            if (item.getCause() != null) pending.add(item.getCause());
            if (item instanceof MessagingException messaging && messaging.getNextException() != null) pending.add(messaging.getNextException());
            if (item instanceof MailSendException send) Collections.addAll(pending, send.getMessageExceptions());
        }
        return rejected || retry ? "FAILED" : "UNKNOWN";
    }

}
