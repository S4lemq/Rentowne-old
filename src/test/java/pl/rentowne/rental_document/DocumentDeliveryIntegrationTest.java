package pl.rentowne.rental_document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.rentowne.TestSecurityConfig;
import pl.rentowne.common.mail.AttachmentEmailService;
import pl.rentowne.common.mail.DocumentMail;
import pl.rentowne.settlement.CalculateRequestDto;
import pl.rentowne.settlement.SettlementChargeDto;
import pl.rentowne.settlement.SettlementService;
import pl.rentowne.user.Role;
import pl.rentowne.user.UserBasicDto;
import pl.rentowne.user.UserService;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the financial transaction, durable queue and actual SMTP/MIME delivery. No layout assertions. */
@SpringBootTest(properties = {"application.documents.enabled=true", "application.documents.worker-enabled=false", "spring.jpa.show-sql=false"})
@ActiveProfiles("test")
@Testcontainers
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentDeliveryIntegrationTest {
    @Container static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16.1");
    static final GreenMail SMTP = smtp();
    static final Path PDF_DIRECTORY = tempDirectory();

    @DynamicPropertySource
    static void configuration(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> SMTP.getSmtp().getPort());
        registry.add("spring.mail.properties.mail.smtp.auth", () -> false);
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> false);
        registry.add("application.documents.directory", PDF_DIRECTORY::toString);
        registry.add("application.email.sender", () -> "emailSimpleService");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired DocumentDeliveryWorker worker;
    @Autowired DocumentStore store;
    @Autowired @Qualifier("taskScheduler") TaskScheduler applicationScheduler;
    @Autowired @Qualifier("documentScheduler") TaskScheduler documentScheduler;
    @Autowired SettlementService settlements;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean UserService users;
    @MockitoBean AuditorAware<String> auditor;
    @MockitoBean(name = "documentClock") Clock clock;
    @MockitoSpyBean AttachmentEmailService transport;
    @MockitoSpyBean DocumentProperties properties;
    @MockitoSpyBean RentalDocumentPdfService pdf;
    private long userId, ownerId, apartmentId, objectId, tenantId, leaseId;

    @BeforeEach
    void fixture() throws Exception {
        jdbc.update("DELETE FROM document_delivery_attempt");
        jdbc.update("DELETE FROM document_delivery");
        jdbc.update("DELETE FROM rental_document");
        jdbc.update("DELETE FROM rental_document_counter");
        SMTP.purgeEmailFromAllMailboxes();
        when(clock.instant()).thenReturn(Instant.parse("2026-11-14T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Warsaw"));
        when(auditor.getCurrentAuditor()).thenReturn(Optional.of("documents-test"));
        userId = next("USER_ACCOUNT_SEQ");
        jdbc.update("INSERT INTO user_account(user_account_id,first_name,last_name,email,password,role,mfa_enabled,preferred_language,insert_date,insert_operator) VALUES (?,'Test','User',?,'test','ADMIN',false,'POLISH',now(),'test')", userId, "user" + userId + "@example.com");
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(userId, "test@example.com", Role.ADMIN));
        ownerId = createOwner();
        createTenancy(ownerId);
    }

    @AfterEach
    void resetMode() { ReflectionTestUtils.setField(transport, "senderMode", "emailSimpleService"); }

    @AfterAll
    static void cleanup() throws Exception {
        SMTP.stop();
        try (var files = Files.list(PDF_DIRECTORY)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
        }
        Files.deleteIfExists(PDF_DIRECTORY);
    }

    @Test
    void blockedDocumentSchedulerDoesNotBlockApplicationMaintenance() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch maintenance = new CountDownLatch(1);
        var documentTask = documentScheduler.schedule(() -> {
            started.countDown();
            try { release.await(10, TimeUnit.SECONDS); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }, Instant.now());
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            applicationScheduler.schedule(maintenance::countDown, Instant.now());
            assertThat(maintenance.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            documentTask.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void queuesThenSendsPdfToTenantAndCcWithAllManualCharges() throws Exception {
        var request = request(false, true);
        calculate(request);
        assertThat(SMTP.getReceivedMessages()).isEmpty();
        assertThat(state()).isEqualTo("PENDING");
        var snapshot = snapshot();
        assertThat(snapshot.total()).isEqualByComparingTo("3278.79");
        assertThat(snapshot.lines()).hasSize(9);
        assertThat(snapshot.payment().dueDate().toString()).isEqualTo("2026-11-10");
        assertThat(worker.processNext()).isTrue();
        assertThat(state()).isEqualTo("SENT");
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
        var message = SMTP.getReceivedMessages()[0];
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(tenantEmail());
        assertThat(message.getRecipients(Message.RecipientType.CC)).extracting(Object::toString)
                .containsExactlyInAnyOrder(ownerEmail(), representativeEmail());
        assertThat(message.getSubject()).contains("Rachunek 01/2026", "Listopad 2026");
        Part attachment = attachment(message);
        assertThat(attachment).isNotNull();
        assertThat(attachment.isMimeType("application/pdf")).isTrue();
        assertThat(attachment.getFileName()).isEqualTo("rachunek-01-2026.pdf");
        byte[] bytes = attachment.getInputStream().readAllBytes();
        assertThat(new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(bytes).isEqualTo(savedPdf());
        assertThat(worker.processNext()).isFalse();
    }

    @Test
    void requestRetryAndConcurrentWorkersDoNotDuplicateNumberOrDelivery() throws Exception {
        var request = request(false, false);
        calculate(request);
        calculate(request);
        assertThat(count("rental_document")).isEqualTo(1);
        assertThat(count("document_delivery")).isEqualTo(1);
        try (var threads = Executors.newFixedThreadPool(2)) {
            var first = threads.submit(worker::processNext);
            var second = threads.submit(worker::processNext);
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
        assertThat(count("document_delivery_attempt")).isEqualTo(1);
    }

    @Test
    void partialSettlementAlsoCreatesDocumentAndKeepsExplicitPeriod() throws Exception {
        calculate(request(true, false));
        assertThat(snapshot().period()).isEqualTo("15.11.2026 - 30.11.2026");
        assertThat(snapshot().note()).isNotBlank();
        worker.processNext();
        assertThat(state()).isEqualTo("SENT");
    }

    @Test
    void ownerSeriesIsSharedAcrossPropertiesAndIndependentAcrossYearsAndTypes() throws Exception {
        calculate(request(false, false));
        long firstObject = objectId;
        createTenancy(ownerId);
        calculate(request(false, false));
        assertThat(jdbc.queryForList("SELECT document_number FROM rental_document ORDER BY sequence_number", String.class)).containsExactly("01/2026", "02/2026");
        terminate("100");
        assertThat(jdbc.queryForObject("SELECT document_number FROM rental_document WHERE document_type='DEPOSIT_SETTLEMENT'", String.class)).isEqualTo("01/2026");
        when(clock.instant()).thenReturn(Instant.parse("2027-01-02T12:00:00Z"));
        objectId = firstObject;
        tenantId = jdbc.queryForObject("SELECT tenant_id FROM tenant WHERE rented_object_id=?", Long.class, objectId);
        calculate(request(false, false));
        assertThat(jdbc.queryForObject("SELECT document_number FROM rental_document WHERE document_year=2027", String.class)).isEqualTo("01/2027");
        ownerId = createOwner();
        createTenancy(ownerId);
        calculate(request(false, false));
        assertThat(jdbc.queryForObject("SELECT document_number FROM rental_document WHERE owner_id=?", String.class, ownerId)).isEqualTo("01/2027");
    }

    @Test
    void concurrentFinancialRequestsAllocateDistinctNumbersForSameOwner() throws Exception {
        long objectOne = objectId;
        var requestOne = request(false, false);
        createTenancy(ownerId);
        long objectTwo = objectId;
        var requestTwo = request(false, false);
        try (var threads = Executors.newFixedThreadPool(2)) {
            var one = threads.submit(() -> settlements.calculate(objectOne, requestOne));
            var two = threads.submit(() -> settlements.calculate(objectTwo, requestTwo));
            one.get(); two.get();
        }
        assertThat(jdbc.queryForList("SELECT sequence_number FROM rental_document ORDER BY sequence_number", Integer.class)).containsExactly(1, 2);
    }

    @Test
    void refusesIncompleteDataAndRollsBackFinancialChanges() throws Exception {
        jdbc.update("UPDATE apartment SET owner_id=NULL WHERE apartment_id=?", apartmentId);
        mvc.perform(post(calculateUrl()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request(false, false))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("właściciela")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement WHERE tenant_id=?", Long.class, tenantId)).isZero();
        assertThat(count("rental_document")).isZero();
        assertThat(worker.processNext()).isFalse();
    }

    @Test
    void rollbackDoesNotLeaveDocumentNumberOrMailTask() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            settlements.calculate(objectId, request(false, false));
            assertThat(count("document_delivery")).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(count("rental_document")).isZero();
        assertThat(count("rental_document_counter")).isZero();
        assertThat(worker.processNext()).isFalse();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    void failedConnectionRetriesSamePdfAndOriginalRecipientsAfterDataEdits() throws Exception {
        calculate(request(false, false));
        doThrow(new MailSendException("SMTP unavailable", new ConnectException("Connection refused"))).when(transport).send(any(DocumentMail.class));
        worker.processNext();
        assertThat(state()).isEqualTo("PENDING");
        byte[] original = savedPdf();
        jdbc.update("UPDATE property_owner SET first_name='Zmieniony',email='changed@example.com' WHERE owner_id=?", ownerId);
        jdbc.update("UPDATE tenant SET email='changed-tenant@example.com' WHERE tenant_id=?", tenantId);
        dueNow();
        doCallRealMethod().when(transport).send(any(DocumentMail.class));
        worker.processNext();
        assertThat(state()).isEqualTo("SENT");
        assertThat(savedPdf()).isEqualTo(original);
        assertThat(SMTP.getReceivedMessages()[0].getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(tenantEmail());
        assertThat(SMTP.getReceivedMessages()[0].getRecipients(Message.RecipientType.CC)).extracting(Object::toString).contains(ownerEmail());
        assertThat(count("rental_document")).isEqualTo(1);
        assertThat(count("document_delivery_attempt")).isEqualTo(2);
    }

    @Test
    void stopsAfterThreeClearlyFailedAttempts() throws Exception {
        calculate(request(false, false));
        doThrow(new MailSendException("SMTP unavailable", new ConnectException("Connection refused"))).when(transport).send(any(DocumentMail.class));
        for (int attempt = 0; attempt < 3; attempt++) { dueNow(); worker.processNext(); }
        assertThat(state()).isEqualTo("FAILED");
        assertThat(count("document_delivery_attempt")).isEqualTo(3);
        assertThat(worker.processNext()).isFalse();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    void ambiguousSmtpResultRequiresVerificationWithoutAutomaticRetry() throws Exception {
        calculate(request(false, false));
        doThrow(new MailSendException("No final SMTP response", new SocketTimeoutException("Timed out"))).when(transport).send(any(DocumentMail.class));
        worker.processNext();
        assertThat(state()).isEqualTo("UNKNOWN");
        dueNow();
        assertThat(worker.processNext()).isFalse();
        assertThat(count("document_delivery_attempt")).isEqualTo(1);
    }

    @Test
    void resumesExpiredPreparationTaskButDoesNotRepeatInterruptedSmtp() throws Exception {
        calculate(request(false, false));
        var tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> store.claim(properties));
        jdbc.update("UPDATE document_delivery SET lease_until=now()-INTERVAL '1 second'");
        worker.processNext();
        assertThat(state()).isEqualTo("SENT");
        assertThat(count("document_delivery_attempt")).isEqualTo(2);
        calculate(request(false, false));
        tx.execute(status -> store.claim(properties));
        jdbc.update("UPDATE document_delivery SET lease_until=now()-INTERVAL '1 second',smtp_started_at=now() WHERE state='PROCESSING'");
        assertThat(worker.processNext()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document_delivery WHERE state='UNKNOWN'", Long.class)).isEqualTo(1);
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
    }

    @Test
    void terminationGeneratesPdfButHoldsMailUntilDepositVerificationIsEnabled() throws Exception {
        terminate("100");
        assertThat(snapshot().result()).isEqualByComparingTo("1900");
        worker.processNext();
        assertThat(state()).isEqualTo("HELD");
        assertThat(savedPdf()).isNotEmpty();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    void enabledDepositDeliverySendsRefundAndPersistsDatesSeparatelyFromTransferDate() throws Exception {
        doReturn(true).when(properties).depositMailEnabled();
        terminate("100");
        assertThat(snapshot().payment().recipient()).isEqualTo("Anna Nowak");
        assertThat(snapshot().payment().dueDate().toString()).isEqualTo("2026-11-28");
        worker.processNext();
        assertThat(state()).isEqualTo("SENT");
        assertThat(SMTP.getReceivedMessages()[0].getSubject()).contains("Rozliczenie kaucji 01/2026");
        assertThat(jdbc.queryForObject("SELECT actual_termination_date FROM lease_agreement WHERE lease_agreement_id=?", java.sql.Date.class, leaseId).toString()).isEqualTo("2026-11-14");
        assertThat(jdbc.queryForObject("SELECT deposit_return_date FROM lease_agreement WHERE lease_agreement_id=?", java.sql.Timestamp.class, leaseId)).isNull();
        mvc.perform(post("/api/lease-agreements/{id}/terminate", leaseId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        assertThat(count("rental_document")).isEqualTo(1);
    }

    @Test
    void handlesDepositSurchargeAndZeroWithoutRequiringTenantRefundAccount() throws Exception {
        jdbc.update("UPDATE tenant SET account_number='' WHERE tenant_id=?", tenantId);
        terminate("2100");
        assertThat(snapshot().result()).isEqualByComparingTo("-100");
        assertThat(snapshot().payment().recipient()).isEqualTo("Jan Kowalski");
        createTenancy(ownerId);
        terminate("2000");
        var zero = json.readValue(jdbc.queryForObject("SELECT snapshot FROM rental_document WHERE source_key=?", String.class, "LEASE:" + leaseId), RentalDocumentData.class);
        assertThat(zero.result()).isZero();
        assertThat(zero.payment()).isNull();
    }

    @Test
    void positiveRefundRequiresTenantBankAccountAndInvalidDatesAreRejected() throws Exception {
        jdbc.update("UPDATE tenant SET account_number='' WHERE tenant_id=?", tenantId);
        mvc.perform(post("/api/lease-agreements/{id}/terminate", leaseId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("konta najemcy")));
        assertThat(jdbc.queryForObject("SELECT is_contract_active FROM lease_agreement WHERE lease_agreement_id=?", Boolean.class, leaseId)).isTrue();
        assertThat(count("rental_document")).isZero();
        mvc.perform(post("/api/lease-agreements/{id}/terminate", leaseId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"terminationDate\":\"2026-11-14\",\"refundDueDate\":\"2026-11-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancellationPreservesPdfAndNumberAndNotifiesRecipients() throws Exception {
        calculate(request(false, false));
        worker.processNext();
        byte[] original = savedPdf();
        long settlement = jdbc.queryForObject("SELECT settlement_id FROM rental_document", Long.class);
        mvc.perform(delete("/api/settlements/{id}", settlement)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT cancelled FROM rental_document", Boolean.class)).isTrue();
        assertThat(savedPdf()).isEqualTo(original);
        worker.processNext();
        assertThat(jdbc.queryForObject("SELECT state FROM document_delivery WHERE kind='CANCELLATION'", String.class)).isEqualTo("SENT");
        assertThat(SMTP.getReceivedMessages()).hasSize(6);
        assertThat(java.util.Arrays.stream(SMTP.getReceivedMessages()).filter(message -> {
            try { return message.getSubject().startsWith("Anulowanie"); } catch (Exception error) { throw new RuntimeException(error); }
        })).hasSize(3);
        calculate(request(false, false));
        assertThat(jdbc.queryForObject("SELECT MAX(sequence_number) FROM rental_document", Integer.class)).isEqualTo(2);
    }

    @Test
    void cancellationAfterInterruptedSmtpNotifiesRecipientsAndPreservesUnknownOutcome() throws Exception {
        calculate(request(false, false));
        var tx = new TransactionTemplate(transactionManager);
        var claimed = tx.execute(status -> store.claim(properties).orElseThrow());
        tx.execute(status -> store.markSmtp(claimed));
        long settlement = jdbc.queryForObject("SELECT settlement_id FROM rental_document", Long.class);

        mvc.perform(delete("/api/settlements/{id}", settlement)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT state FROM document_delivery WHERE kind='ISSUE'", String.class)).isEqualTo("UNKNOWN");
        assertThat(jdbc.queryForObject("SELECT outcome FROM document_delivery_attempt", String.class)).isEqualTo("UNKNOWN");
        // A stale worker must not overwrite the cancellation's outcome.
        tx.executeWithoutResult(status -> store.finish(claimed, "SENT", null));
        assertThat(jdbc.queryForObject("SELECT state FROM document_delivery WHERE kind='ISSUE'", String.class)).isEqualTo("UNKNOWN");
        worker.processNext();
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
        assertThat(SMTP.getReceivedMessages()[0].getSubject()).startsWith("Anulowanie");
        assertThat(worker.processNext()).isFalse();
    }

    @Test
    void cancellationDuringPreparationStopsWorkerWithoutSendingNotification() throws Exception {
        calculate(request(false, false));
        var tx = new TransactionTemplate(transactionManager);
        var claimed = tx.execute(status -> store.claim(properties).orElseThrow());
        long settlement = jdbc.queryForObject("SELECT settlement_id FROM rental_document", Long.class);

        mvc.perform(delete("/api/settlements/{id}", settlement)).andExpect(status().isOk());

        assertThat(state()).isEqualTo("CANCELLED");
        Boolean smtpMarked = tx.execute(status -> store.markSmtp(claimed));
        assertThat(smtpMarked).isFalse();
        assertThat(jdbc.queryForObject("SELECT outcome FROM document_delivery_attempt", String.class)).isEqualTo("CANCELLED");
        assertThat(worker.processNext()).isFalse();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    void cancellingSimulatedReceiptDoesNotSendRealNotificationAfterTransportSwitch() throws Exception {
        ReflectionTestUtils.setField(transport, "senderMode", "fakeEmailService");
        calculate(request(false, false));
        worker.processNext();
        ReflectionTestUtils.setField(transport, "senderMode", "emailSimpleService");
        long settlement = jdbc.queryForObject("SELECT settlement_id FROM rental_document", Long.class);

        mvc.perform(delete("/api/settlements/{id}", settlement)).andExpect(status().isOk());

        assertThat(state()).isEqualTo("SIMULATED");
        assertThat(jdbc.queryForObject("SELECT cancelled FROM rental_document", Boolean.class)).isTrue();
        assertThat(count("document_delivery")).isEqualTo(1);
        assertThat(worker.processNext()).isFalse();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    void preparationFailureKeepsFinancialOperationAndRetriesWithoutAllocatingAnotherNumber() throws Exception {
        calculate(request(false, false));
        doThrow(new IllegalStateException("Temporary PDF preparation failure")).when(pdf).generate(any(RentalDocumentData.class));
        worker.processNext();
        assertThat(state()).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement WHERE tenant_id=?", Long.class, tenantId)).isEqualTo(1);
        assertThat(SMTP.getReceivedMessages()).isEmpty();
        doCallRealMethod().when(pdf).generate(any(RentalDocumentData.class));
        dueNow();
        worker.processNext();
        assertThat(state()).isEqualTo("SENT");
        assertThat(count("rental_document")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT last_number FROM rental_document_counter", Integer.class)).isEqualTo(1);
    }

    @Test
    void missingPersistedPdfIsReportedWithoutRegeneratingAnIssuedArtifact() throws Exception {
        calculate(request(false, false));
        doThrow(new MailSendException("SMTP unavailable", new ConnectException("Connection refused"))).when(transport).send(any(DocumentMail.class));
        worker.processNext();
        String filename = jdbc.queryForObject("SELECT pdf_file_name FROM rental_document", String.class);
        Files.delete(PDF_DIRECTORY.resolve(filename));
        dueNow();
        worker.processNext();
        verify(pdf, times(1)).generate(any(RentalDocumentData.class));
        assertThat(state()).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT last_error FROM document_delivery", String.class)).contains("NoSuchFileException");
    }

    @Test
    void cancellationBeforeDeliveryPreventsSendingAndRetainsUsedNumber() throws Exception {
        calculate(request(false, false));
        long settlement = jdbc.queryForObject("SELECT settlement_id FROM rental_document", Long.class);
        mvc.perform(delete("/api/settlements/{id}", settlement)).andExpect(status().isOk());
        assertThat(state()).isEqualTo("CANCELLED");
        assertThat(worker.processNext()).isFalse();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
        calculate(request(false, false));
        assertThat(jdbc.queryForObject("SELECT MAX(sequence_number) FROM rental_document", Integer.class)).isEqualTo(2);
    }

    @Test
    void simulatedTransportIsNeverReportedAsSentAndDuplicateCcIsRemoved() throws Exception {
        ReflectionTestUtils.setField(transport, "senderMode", "fakeEmailService");
        jdbc.update("UPDATE property_owner SET representative_email=email WHERE owner_id=?", ownerId);
        calculate(request(false, false));
        assertThat(json.readTree(jdbc.queryForObject("SELECT cc_json FROM document_delivery", String.class))).hasSize(1);
        worker.processNext();
        assertThat(state()).isEqualTo("SIMULATED");
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    void tenantAddressIsExcludedFromCcIgnoringCaseAndWhitespace() throws Exception {
        jdbc.update("UPDATE property_owner SET email=?,representative_email=? WHERE owner_id=?", "  " + tenantEmail().toUpperCase(java.util.Locale.ROOT) + "  ", tenantEmail(), ownerId);
        calculate(request(false, false));
        assertThat(json.readTree(jdbc.queryForObject("SELECT cc_json FROM document_delivery", String.class))).isEmpty();
        worker.processNext();
        assertThat(state()).isEqualTo("SENT");
        assertThat(SMTP.getReceivedMessages()).hasSize(1);
        assertThat(SMTP.getReceivedMessages()[0].getRecipients(Message.RecipientType.CC)).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {451, 550})
    void distinguishesTemporaryAndPermanentExplicitSmtpRejections(int code) throws Exception {
        calculate(request(false, false));
        doThrow(new MailSendException("SMTP rejected message", new SMTPSendFailedException("DATA", code, "Rejected", null, null, null, null)))
                .when(transport).send(any(DocumentMail.class));
        worker.processNext();
        assertThat(state()).isEqualTo(code == 451 ? "PENDING" : "FAILED");
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    void previewDoesNotCreateDocumentsAndSlowTransportDoesNotBlockAnotherFinancialRequest() throws Exception {
        mvc.perform(post("/api/lease-agreements/{id}/deposit-return-preview", leaseId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        assertThat(count("rental_document")).isZero();
        calculate(request(false, false));
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(call -> {
            entered.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("SMTP test timed out");
            return call.callRealMethod();
        }).when(transport).send(any(DocumentMail.class));
        try (var threads = Executors.newSingleThreadExecutor()) {
            var sending = threads.submit(worker::processNext);
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                createTenancy(ownerId);
                calculate(request(false, false));
                assertThat(count("rental_document")).isEqualTo(2);
            } finally { release.countDown(); }
            assertThat(sending.get()).isTrue();
        }
    }

    private CalculateRequestDto request(boolean partial, boolean extras) {
        return CalculateRequestDto.builder().settlementDate(LocalDateTime.of(2026, 11, 1, 0, 0))
                .tenantId(tenantId).requestId(UUID.randomUUID()).isPartialMonth(partial)
                .periodStartDate(partial ? LocalDateTime.of(2026, 11, 15, 0, 0) : null)
                .periodEndDate(partial ? LocalDateTime.of(2026, 11, 30, 0, 0) : null).isRentProrated(partial)
                .additionalCharges(extras ? List.of(new SettlementChargeDto(SettlementChargeDto.Type.DEPOSIT, new java.math.BigDecimal("300"), "Kaucja"),
                        new SettlementChargeDto(SettlementChargeDto.Type.OTHER, new java.math.BigDecimal("44"), "Klucze"),
                        new SettlementChargeDto(SettlementChargeDto.Type.OTHER, new java.math.BigDecimal("500"), "Krzesła")) : List.of()).build();
    }

    private void calculate(CalculateRequestDto request) throws Exception {
        mvc.perform(post(calculateUrl()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request))).andExpect(status().isOk());
    }

    private void terminate(String charge) throws Exception {
        mvc.perform(post("/api/lease-agreements/{id}/terminate", leaseId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("chargeAmount", charge, "chargeDescription", "Naprawa wyposażenia",
                        "terminationDate", "2026-11-14", "refundDueDate", "2026-11-28")))).andExpect(status().isOk());
    }

    private String calculateUrl() { return "/api/rented-objects/" + objectId + "/calculate"; }
    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private String state() { return jdbc.queryForObject("SELECT state FROM document_delivery ORDER BY created_at DESC LIMIT 1", String.class); }
    private void dueNow() { jdbc.update("UPDATE document_delivery SET next_attempt_at=now()-INTERVAL '1 second'"); }
    private RentalDocumentData snapshot() throws Exception { return json.readValue(jdbc.queryForObject("SELECT snapshot FROM rental_document ORDER BY created_at DESC LIMIT 1", String.class), RentalDocumentData.class); }
    private byte[] savedPdf() throws IOException { return Files.readAllBytes(PDF_DIRECTORY.resolve(jdbc.queryForObject("SELECT pdf_file_name FROM rental_document ORDER BY created_at DESC LIMIT 1", String.class))); }
    private String tenantEmail() { return "tenant" + tenantId + "@example.com"; }
    private String ownerEmail() { return "owner" + ownerId + "@example.com"; }
    private String representativeEmail() { return "rep" + ownerId + "@example.com"; }

    private long createOwner() {
        long id = next("PROPERTY_OWNER_SEQ");
        jdbc.update("""
                INSERT INTO property_owner(owner_id,user_account_id,first_name,last_name,email,street_name,building_number,zip_code,city_name,
                representative_name,representative_email,account_number,payment_account_holder,insert_date,insert_operator)
                VALUES (?,?,'Jan','Kowalski',?,'Jasna','12','60-101','Poznań','Katarzyna Nowak',?,'61109010140000071219812874','OWNER',now(),'test')
                """, id, userId, "owner" + id + "@example.com", "rep" + id + "@example.com");
        return id;
    }

    private void createTenancy(long owner) {
        long apartmentAddress = createAddress(), tenantAddress = createAddress();
        apartmentId = next("APARTMENT_SEQ"); objectId = next("RENTED_OBJECT_SEQ");
        leaseId = next("LEASE_AGREEMENT_SEQ"); tenantId = next("TENANT_SEQ");
        jdbc.update("INSERT INTO apartment(apartment_id,apartment_name,leases_number,area,is_rented,pinned,address_id,user_account_id,owner_id,insert_date,insert_operator) VALUES (?,'Test',1,40,true,false,?,?,?,now(),'test')", apartmentId, apartmentAddress, userId, owner);
        jdbc.update("INSERT INTO rented_object(rented_object_id,rented_object_name,is_rented,apartment_id,insert_date,insert_operator) VALUES (?,'23a',true,?,now(),'test')", objectId, apartmentId);
        jdbc.update("""
                INSERT INTO lease_agreement(lease_agreement_id,start_contract_date,end_contract_date,deposit,deposit_paid,deposit_opening_paid,deposit_balance_confirmed,
                payment_due_day_of_month,rent_amount,compensation_amount,internet_fee,gas_deposit,initial_energy_meter_reading,initial_water_meter_reading,
                is_contract_active,contract_signing_date,insert_date,insert_operator)
                VALUES (?,'2026-01-01','2027-12-31',2500,2000,2000,true,10,300,2000,40,94.79,0,0,true,'2026-01-01',now(),'test')
                """, leaseId);
        jdbc.update("INSERT INTO tenant(tenant_id,first_name,last_name,email,account_number,address_id,lease_agreement_id,rented_object_id,active,insert_date,insert_operator) VALUES (?,'Anna','Nowak',?,'61109010140000071219812874',?,?,?,true,now(),'test')", tenantId, tenantEmail(), tenantAddress, leaseId, objectId);
    }

    private long createAddress() {
        long id = next("ADDRESS_SEQ");
        jdbc.update("INSERT INTO address(address_id,street_name,building_number,zip_code,city_name,voivode_ship,insert_date,insert_operator) VALUES (?,'Jasna','12','60-101','Poznań','Wielkopolskie',now(),'test')", id);
        return id;
    }

    private long next(String sequence) { return jdbc.queryForObject("SELECT nextval('" + sequence + "')", Long.class); }

    private static Part attachment(Part part) throws Exception {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return part;
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                Part found = attachment(multipart.getBodyPart(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static GreenMail smtp() {
        GreenMail server = new GreenMail(new ServerSetup(0, "127.0.0.1", "smtp"))
                .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());
        server.start();
        return server;
    }

    private static Path tempDirectory() {
        try { return Files.createTempDirectory("rentowne-documents-"); }
        catch (IOException e) { throw new java.io.UncheckedIOException(e); }
    }
}
