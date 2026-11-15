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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
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

/** Exercises financial transactions, manual document actions and SMTP/MIME delivery. No layout assertions. */
@SpringBootTest(properties = {"application.documents.enabled=true", "spring.jpa.show-sql=false"})
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
    @Autowired DocumentManagementService management;
    @Autowired DocumentSendService sending;
    @Autowired DocumentNumberService numbers;
    @Autowired RentalDocumentRepository documentRepository;
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
    void generatesReceiptWithComponentsButNeverQueuesOrSendsMail() throws Exception {
        calculate(request(false,true));
        assertThat(savedPdf()).isNotEmpty();
        assertThat(count("document_delivery")).isZero();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
        assertThat(snapshot().lines()).extracting(RentalDocumentData.Line::description).contains("Dodatkowa opłata - Klucze","Dodatkowa opłata - Krzesła");
        mvc.perform(get("/api/rental-documents")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/rental-documents/apartments")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(apartmentId));
        mvc.perform(get("/api/rental-documents").param("from","2026-11-01").param("to","2026-11-30")
                .param("search","Anna").param("apartmentId",String.valueOf(apartmentId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void manualSendUsesExactSavedAttachmentAndDeduplicatesRecipients() throws Exception {
        calculate(request(false,false));
        var detail=management.detail(documentId());
        var send=new DocumentSendRequest(UUID.randomUUID(),detail.revision(),detail.checksum(),tenantEmail(),
                List.of(ownerEmail(),ownerEmail().toUpperCase(),representativeEmail(),tenantEmail()),true,false,false);
        management.send(detail.id(),send);
        assertThat(state()).isEqualTo("SENT");
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
        assertThat(attachment(SMTP.getReceivedMessages()[0]).getInputStream().readAllBytes()).isEqualTo(savedPdf());
        assertThat(SMTP.getReceivedMessages()[0].getRecipients(Message.RecipientType.CC)).hasSize(2);
        management.send(detail.id(),send);
        assertThat(count("document_delivery")).isEqualTo(1);
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
    }

    @Test
    void deletionAfterSendingCancelsDocumentWithoutSendingAnythingAndPreservesOriginal() throws Exception {
        calculate(request(false,false));
        var id=documentId();
        management.send(id,sendRequest(id));
        byte[] original=savedPdf();
        long settlement=jdbc.queryForObject("SELECT settlement_id FROM rental_document",Long.class);
        mvc.perform(delete("/api/settlements/{id}",settlement)).andExpect(status().isOk());
        var cancelled=management.detail(id);
        assertThat(cancelled.status()).isEqualTo("Anulowany — wcześniej wysłany");
        assertThat(cancelled.history().getFirst().state()).isEqualTo("SENT");
        assertThat(savedPdf()).isEqualTo(original);
        assertThat(count("document_delivery")).isEqualTo(1);
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
        assertThat(cancelled.canSend()).isFalse();
        mvc.perform(post("/api/rental-documents/{id}/regenerate",id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"revision\":"+cancelled.revision()+"}")).andExpect(status().isConflict());
        calculate(request(false,false));
        assertThat(jdbc.queryForObject("SELECT max(sequence_number) FROM rental_document",Integer.class)).isEqualTo(2);
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
        mvc.perform(get("/api/rental-documents").param("tab","CANCELLED")).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deletingUnsentSettlementCancelsWithoutMailAndAllowsDeletingItsFile() throws Exception {
        calculate(request(false,false));
        var id=documentId();
        long settlement=management.detail(id).settlementId();
        mvc.perform(delete("/api/settlements/{id}",settlement)).andExpect(status().isOk());
        var d=management.detail(id);
        assertThat(d.status()).isEqualTo("Anulowany");
        assertThat(d.canRegenerate()).isFalse();
        management.deletePdf(id,d.revision());
        assertThat(management.detail(id).hasPdf()).isFalse();
        assertThat(count("document_delivery")).isZero();
    }

    @Test
    void regeneratesFromDatabaseAndKeepsNumberWhileStaleSendIsRejected() throws Exception {
        calculate(request(true,false));
        var id=documentId();var before=management.detail(id);var stale=sendRequest(id);
        jdbc.update("UPDATE settlement SET electricity_amount=electricity_amount+17,total_amount=total_amount+17 WHERE settlement_id=?",before.settlementId());
        jdbc.update("UPDATE tenant SET first_name='Maria',email='new-address@example.com' WHERE tenant_id=?",tenantId);
        var after=management.regenerate(id,before.revision());
        assertThat(after.data().number()).isEqualTo(before.data().number());
        assertThat(after.data().period()).isEqualTo("15.11.2026 - 30.11.2026");
        assertThat(after.data().tenant().name()).isEqualTo("Maria Nowak");
        assertThat(after.to()).isEqualTo("new-address@example.com");
        assertThat(after.data().total()).isEqualByComparingTo(before.data().total().add(new java.math.BigDecimal("17")));
        assertThat(after.checksum()).isNotEqualTo(before.checksum());
        mvc.perform(post("/api/rental-documents/{id}/send",id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(stale)))
                .andExpect(status().isConflict());
        assertThat(count("document_delivery")).isZero();
    }

    @Test
    void deletingFileKeepsFinancialSourceAndCanGenerateAgain() throws Exception {
        calculate(request(false,false));var id=documentId();var before=management.detail(id);
        String filename=jdbc.queryForObject("SELECT pdf_file_name FROM rental_document",String.class);
        var empty=management.deletePdf(id,before.revision());
        assertThat(Files.exists(PDF_DIRECTORY.resolve(filename))).isFalse();
        assertThat(empty.hasPdf()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM settlement WHERE settlement_id=?",Long.class,before.settlementId())).isEqualTo(1);
        assertThat(management.regenerate(id,empty.revision()).data().number()).isEqualTo(before.data().number());
    }

    @Test
    void failureOfPdfDoesNotRollBackFinancialOperation() throws Exception {
        doThrow(new IllegalStateException("PDF unavailable")).when(pdf).generate(any(RentalDocumentData.class));
        calculate(request(false,false));var id=documentId();var d=management.detail(id);
        assertThat(d.hasPdf()).isFalse();assertThat(d.generationError()).isNotBlank();
        assertThat(d.settlementId()).isNotNull();assertThat(count("document_delivery")).isZero();
        doCallRealMethod().when(pdf).generate(any(RentalDocumentData.class));
        assertThat(management.regenerate(id,d.revision()).hasPdf()).isTrue();
    }

    @Test
    void failedRegenerationPreservesOriginalFileAndSnapshot() throws Exception {
        calculate(request(false,false));var id=documentId();var before=management.detail(id);byte[] original=savedPdf();
        doThrow(new IllegalStateException("PDF unavailable")).when(pdf).generate(any(RentalDocumentData.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(()->management.regenerate(id,before.revision())).isInstanceOf(IllegalStateException.class);
        assertThat(management.detail(id).checksum()).isEqualTo(before.checksum());assertThat(savedPdf()).isEqualTo(original);
    }

    @Test
    void smtpFailureNeverRetriesAndUnknownProtectsDocument() throws Exception {
        calculate(request(false,false));var id=documentId();var request=sendRequest(id);
        doThrow(new MailSendException("SMTP unavailable",new ConnectException())).when(transport).send(any(DocumentMail.class));
        assertThat(management.send(id,request).history().getFirst().state()).isEqualTo("FAILED");
        management.send(id,request);verify(transport,times(1)).send(any(DocumentMail.class));
        doThrow(new MailSendException("Ambiguous",new SocketTimeoutException())).when(transport).send(any(DocumentMail.class));
        var unknown=management.send(id,sendRequest(id));
        assertThat(unknown.history().getFirst().state()).isEqualTo("UNKNOWN");
        assertThat(unknown.canRegenerate()).isFalse();assertThat(unknown.canDelete()).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(()->management.send(id,sendRequest(id))).isInstanceOf(pl.rentowne.rental_payment.RentalPaymentException.class);
        assertThat(count("document_delivery")).isEqualTo(2);
    }

    @Test
    void depositIsSingleFinalPdfAndCanBeSentManually() throws Exception {
        terminate("100");var id=documentId();var d=management.detail(id);
        assertThat(d.data().result()).isEqualByComparingTo("1900");assertThat(savedPdf()).isNotEmpty();
        assertThat(count("document_delivery")).isZero();
        management.regenerate(id,d.revision());assertThat(count("rental_document")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM settlement WHERE tenant_id=?",Long.class,tenantId)).isEqualTo(1);
        management.send(id,sendRequest(id));assertThat(SMTP.getReceivedMessages()).hasSize(3);
    }

    @Test
    void crossAccountAccessAndMutationsAreRejected() throws Exception {
        calculate(request(false,false));var id=documentId();var request=sendRequest(id);
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(userId+10000,"other@example.com",Role.ADMIN));
        mvc.perform(get("/api/rental-documents/{id}",id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/rental-documents/{id}/pdf",id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/rental-documents/{id}/pdf",id).param("revision","1")).andExpect(status().isNotFound());
        mvc.perform(post("/api/rental-documents/{id}/send",id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request))).andExpect(status().isNotFound());
        mvc.perform(get("/api/rental-documents")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void cancellationNotificationRequiresExplicitAction() throws Exception {
        calculate(request(false,false));var id=documentId();management.send(id,sendRequest(id));
        mvc.perform(delete("/api/settlements/{id}",management.detail(id).settlementId())).andExpect(status().isOk());
        assertThat(SMTP.getReceivedMessages()).hasSize(3);var d=management.detail(id);
        management.send(id,new DocumentSendRequest(UUID.randomUUID(),d.revision(),d.checksum(),tenantEmail(),List.of(ownerEmail(),representativeEmail()),true,false,true));
        assertThat(SMTP.getReceivedMessages()).hasSize(6);
        assertThat(attachment(SMTP.getReceivedMessages()[3])).isNull();
    }

    @Test
    void restartMarksInterruptedSendingUnknownWithoutSending() throws Exception {
        calculate(request(false,false));var id=documentId();management.send(id,sendRequest(id));
        jdbc.update("UPDATE document_delivery SET state='PROCESSING'");
        sending.recoverInterruptedSends();assertThat(state()).isEqualTo("UNKNOWN");
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
    }

    @Test
    void transactionRollbackAndRequestRetryDoNotCreateDuplicateDocuments() throws Exception {
        var request=request(false,false);calculate(request);calculate(request);
        assertThat(count("rental_document")).isEqualTo(1);
        new TransactionTemplate(transactionManager).executeWithoutResult(status->{settlements.calculate(objectId,request(false,false));status.setRollbackOnly();});
        assertThat(count("rental_document")).isEqualTo(1);assertThat(count("document_delivery")).isZero();
    }

    @Test
    void firstCounterCreationIsSerializedAndRollbackDoesNotConsumeNumber() throws Exception {
        var tx=new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status->{
            assertThat(numbers.next(ownerId,RentalDocumentType.RENT_RECEIPT,2030)).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(count("rental_document_counter")).isZero();
        CountDownLatch start=new CountDownLatch(1);
        try(var threads=Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> issue=()->{
                start.await();
                return tx.execute(status->numbers.next(ownerId,RentalDocumentType.RENT_RECEIPT,2030));
            };
            var first=threads.submit(issue);var second=threads.submit(issue);start.countDown();
            assertThat(List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(1,2);
        }
        assertThat(count("rental_document_counter")).isEqualTo(1);
    }

    @Test
    void optimisticVersionRejectsDetachedStaleEntity() throws Exception {
        calculate(request(false,false));var id=documentId();
        var first=documentRepository.findById(id).orElseThrow();
        var stale=documentRepository.findById(id).orElseThrow();
        first.generationFailed("First change");documentRepository.saveAndFlush(first);
        stale.generationFailed("Stale change");
        org.assertj.core.api.Assertions.assertThatThrownBy(()->documentRepository.saveAndFlush(stale))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        assertThat(documentRepository.findById(id).orElseThrow().getError()).isEqualTo("First change");
    }

    @Test
    void paginatesBeforeLoadingHistoryAndFiltersLatestDeliveryWithTimestampTies() throws Exception {
        calculate(request(false,false));var first=documentId();
        calculate(request(false,false));var second=documentId();
        jdbc.update("""
                INSERT INTO document_delivery(delivery_id,document_id,kind,to_address,cc_json,send_enabled,state,created_at,finished_at)
                VALUES ('00000000-0000-0000-0000-000000000001',?,'ISSUE','test@example.com','[]',true,'FAILED','2026-11-15','2026-11-15'),
                ('00000000-0000-0000-0000-000000000002',?,'ISSUE','test@example.com','[]',true,'SENT','2026-11-15','2026-11-15')
                """,first,first);
        mvc.perform(get("/api/rental-documents").param("size","1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/rental-documents").param("mailState","SENT")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(first.toString()));
        mvc.perform(get("/api/rental-documents").param("mailState","FAILED")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/rental-documents").param("mailState","NOT_SENT")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(second.toString()));
        mvc.perform(get("/api/rental-documents").param("type","DEPOSIT_SETTLEMENT")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/rental-documents").param("search","jasna").param("tenantId",String.valueOf(tenantId))
                .param("leaseId",String.valueOf(leaseId))).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void migrationBackfillsExistingSnapshotsWithoutChangingPdfOrHistory() throws Exception {
        calculate(request(false,false));var id=documentId();management.send(id,sendRequest(id));
        var before=management.detail(id);byte[] original=savedPdf();
        new TransactionTemplate(transactionManager).executeWithoutResult(status->{
            jdbc.execute("ALTER TABLE rental_document DROP COLUMN issue_date, DROP COLUMN tenant_name, DROP COLUMN property_address");
            jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>)connection->{
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                        new org.springframework.core.io.ClassPathResource("db/changelog/ddl-scripts/073-document-jpa-search.sql"));
                return null;
            });
        });
        var after=documentRepository.findById(id).orElseThrow();
        assertThat(after.getIssueDate()).isEqualTo(before.data().issueDate());
        assertThat(after.getTenantName()).isEqualTo(before.data().tenant().name());
        assertThat(after.getPropertyAddress()).isEqualTo(before.data().propertyAddress());
        assertThat(savedPdf()).isEqualTo(original);
        assertThat(management.detail(id).history()).isEqualTo(before.history());
    }

    @Test
    void cancellationWaitsForSendingAndPreservesCommittedOutcome() throws Exception {
        calculate(request(false,false));var id=documentId();var request=sendRequest(id);
        long settlement=management.detail(id).settlementId();byte[] original=savedPdf();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),deleting=new CountDownLatch(1);
        doAnswer(call->{entered.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Test timeout");return call.callRealMethod();})
                .when(transport).send(any(DocumentMail.class));
        try(var threads=Executors.newFixedThreadPool(2)) {
            var send=threads.submit(()->management.send(id,request));
            try {
                assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
                var cancel=threads.submit(()->{deleting.countDown();settlements.deleteCalculate(settlement);});
                assertThat(deleting.await(5,TimeUnit.SECONDS)).isTrue();
                org.assertj.core.api.Assertions.assertThatThrownBy(()->cancel.get(200,TimeUnit.MILLISECONDS))
                        .isInstanceOf(java.util.concurrent.TimeoutException.class);
                release.countDown();send.get(10,TimeUnit.SECONDS);cancel.get(10,TimeUnit.SECONDS);
            } finally {release.countDown();}
        }
        assertThat(management.detail(id).status()).isEqualTo("Anulowany — wcześniej wysłany");
        assertThat(savedPdf()).isEqualTo(original);assertThat(count("document_delivery")).isEqualTo(1);
    }

    @Test
    void settlementRowsContainDocumentStateAndSupportOlderRowsWithoutDocuments() throws Exception {
        calculate(request(false,false));var document=documentId();
        long withDocument=management.detail(document).settlementId();
        management.send(document,sendRequest(document));
        calculate(request(false,false));var unsent=documentId();
        long withoutDocument=management.detail(unsent).settlementId();
        jdbc.update("DELETE FROM rental_document WHERE document_id=?",unsent);
        var result=mvc.perform(get("/api/settlements").param("rentedObjectId",String.valueOf(objectId))
                .param("tenantId",String.valueOf(tenantId))).andExpect(status().isOk()).andReturn();
        var rows=json.readTree(result.getResponse().getContentAsString()).get("content");
        for(var row:rows) {
            if(row.get("id").asLong()==withDocument) {
                assertThat(row.at("/document/id").asText()).isEqualTo(document.toString());
                assertThat(row.at("/document/status").asText()).isEqualTo("Wysłany");
                assertThat(row.at("/document/mailState").asText()).isEqualTo("SENT");
            } else if(row.get("id").asLong()==withoutDocument) assertThat(row.get("document").isNull()).isTrue();
        }
        assertThat(rows.size()).isEqualTo(2);
    }

    private UUID documentId(){return jdbc.queryForObject("SELECT document_id FROM rental_document ORDER BY created_at DESC LIMIT 1",UUID.class);}
    @Test
    void concurrentDuplicateClickSendsOnlyOnce() throws Exception {
        calculate(request(false,false));var id=documentId();var request=sendRequest(id);
        CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        doAnswer(call->{entered.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Test timeout");return call.callRealMethod();})
                .when(transport).send(any(DocumentMail.class));
        try(var threads=Executors.newFixedThreadPool(2)) {
            var first=threads.submit(()->management.send(id,request));
            try {
                assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
                var second=threads.submit(()->management.send(id,request));
                release.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
            } finally {release.countDown();}
        }
        assertThat(count("document_delivery")).isEqualTo(1);
        assertThat(SMTP.getReceivedMessages()).hasSize(3);
    }

    @Test
    void sentFileCannotBeDeletedOrRegenerated() throws Exception {
        calculate(request(false,false));var id=documentId();var d=management.send(id,sendRequest(id));
        mvc.perform(delete("/api/rental-documents/{id}/pdf",id).param("revision",String.valueOf(d.revision()))).andExpect(status().isConflict());
        mvc.perform(post("/api/rental-documents/{id}/regenerate",id).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":"+d.revision()+"}")).andExpect(status().isConflict());
        assertThat(savedPdf()).isNotEmpty();
    }

    @Test
    void ownerCountersStayIndependentAcrossDocumentTypesAndYears() throws Exception {
        calculate(request(false,false));assertThat(snapshot().number()).isEqualTo("01/2026");
        createTenancy(ownerId);calculate(request(false,false));assertThat(snapshot().number()).isEqualTo("02/2026");
        terminate("100");assertThat(snapshot().number()).isEqualTo("01/2026");
        when(clock.instant()).thenReturn(Instant.parse("2027-01-02T12:00:00Z"));
        createTenancy(ownerId);calculate(request(false,false));assertThat(snapshot().number()).isEqualTo("01/2027");
    }

    private DocumentSendRequest sendRequest(UUID id){var d=management.detail(id);return new DocumentSendRequest(UUID.randomUUID(),d.revision(),d.checksum(),tenantEmail(),List.of(ownerEmail(),representativeEmail()),true,false,false);}

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
