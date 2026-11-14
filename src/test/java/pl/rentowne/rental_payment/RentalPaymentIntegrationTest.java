package pl.rentowne.rental_payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.rentowne.TestSecurityConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.rentowne.user.Role;
import pl.rentowne.user.UserBasicDto;
import pl.rentowne.user.UserService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class RentalPaymentIntegrationTest {
    // Existing integration fixtures reset shared sequences; these scenarios own their database.
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16.1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean UserService users;
    @MockitoBean AuditorAware<String> auditor;
    private long tenantId;
    private long objectId;
    private long leaseId;
    private String url;

    @BeforeEach
    void tenancy() {
        when(auditor.getCurrentAuditor()).thenReturn(Optional.of("payments-test"));
        long userId = next("USER_ACCOUNT_SEQ");
        long addressId = next("ADDRESS_SEQ");
        long apartmentId = next("APARTMENT_SEQ");
        objectId = next("RENTED_OBJECT_SEQ");
        leaseId = next("LEASE_AGREEMENT_SEQ");
        tenantId = next("TENANT_SEQ");
        jdbc.update("INSERT INTO address(address_id,street_name,building_number,zip_code,city_name,voivode_ship,insert_date,insert_operator) VALUES (?, 'Testowa','1','00-001','Warszawa','Mazowieckie',now(),'test')", addressId);
        jdbc.update("INSERT INTO user_account(user_account_id,first_name,last_name,email,password,role,mfa_enabled,preferred_language,insert_date,insert_operator) VALUES (?, 'Test','Owner',?,'test','ADMIN',false,'POLISH',now(),'test')", userId, "owner" + userId + "@test.local");
        jdbc.update("INSERT INTO apartment(apartment_id,apartment_name,leases_number,area,is_rented,pinned,address_id,user_account_id,insert_date,insert_operator) VALUES (?,'Test',1,40,true,false,?,?,now(),'test')", apartmentId, addressId, userId);
        jdbc.update("INSERT INTO rented_object(rented_object_id,rented_object_name,is_rented,apartment_id,insert_date,insert_operator) VALUES (?,'23a',true,?,now(),'test')", objectId, apartmentId);
        jdbc.update("""
                INSERT INTO lease_agreement(lease_agreement_id,start_contract_date,end_contract_date,deposit,deposit_paid,
                payment_due_day_of_month,rent_amount,compensation_amount,internet_fee,gas_deposit,initial_energy_meter_reading,
                initial_water_meter_reading,is_contract_active,contract_signing_date,insert_date,insert_operator)
                VALUES (?, '2026-01-01','2027-12-31',0,0,10,300,2000,40,160,0,0,true,'2026-01-01',now(),'test')
                """, leaseId);
        jdbc.update("INSERT INTO tenant(tenant_id,first_name,last_name,email,account_number,lease_agreement_id,rented_object_id,active,insert_date,insert_operator) VALUES (?,'Anna','Kowalska',?,'123',?,?,true,now(),'test')", tenantId, "tenant" + tenantId + "@test.local", leaseId, objectId);
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(userId, "test@test.local", Role.ADMIN));
        url = "/api/rented-objects/" + objectId + "/tenants/" + tenantId + "/payments";
    }

    @Test
    void shouldHandlePartialFullAndExcessPaymentsAcrossMonthlyCharges() throws Exception {
        assertBalance("0", "0", "PAID");
        settle("2539.89");
        assertBalance("-2539.89", "0", "DUE");
        create("1000");
        create("800");
        assertBalance("-739.89", "1800", "PARTIALLY_PAID");
        settle("2600");
        assertBalance("-3339.89", "1800", "PARTIALLY_PAID");
        create("3400");
        assertBalance("60.11", "5200", "OVERPAID");
        settle("2500");
        assertBalance("-2439.89", "5200", "PARTIALLY_PAID");
        var full = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(2439.89)).andReturn();
        assertBalance("0", "7639.89", "PAID");
        mvc.perform(get(url)).andExpect(jsonPath("$.payments.length()").value(4));
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        settle("100");
        assertBalance("-100", "7639.89", "PARTIALLY_PAID");
        assertThat(json.readTree(full.getResponse().getContentAsString()).get("id").asLong()).isPositive();
    }

    @Test
    void shouldRecalculateAfterCorrectionsAndKeepReceiptsWhenSettlementIsDeleted() throws Exception {
        long settlement = settle("2539.89");
        var payment = create("1000");
        long id = payment.get("id").asLong();
        String update = json.writeValueAsString(Map.of("amount", 2700, "paymentDate", "2026-05-10", "description", "Korekta", "version", 0));
        mvc.perform(put(url + "/" + id).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk());
        assertBalance("160.11", "2700", "OVERPAID");
        mvc.perform(put(url + "/" + id).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isConflict());
        jdbc.update("UPDATE settlement SET total_amount = 2600 WHERE settlement_id = ?", settlement);
        assertBalance("100", "2700", "OVERPAID");
        mvc.perform(delete("/api/settlements/" + settlement)).andExpect(status().isOk());
        assertBalance("2700", "2700", "OVERPAID");
        mvc.perform(get(url)).andExpect(jsonPath("$.payments.length()").value(1));
        mvc.perform(delete(url + "/" + id).param("version", "0")).andExpect(status().isConflict());
        mvc.perform(delete(url + "/" + id).param("version", "1")).andExpect(status().isNoContent());
        assertBalance("0", "0", "PAID");
    }

    @Test
    void shouldApplyOpeningCoverageWithoutCountingItAsARealReceipt() throws Exception {
        long old = settle("800");
        jdbc.update("INSERT INTO settlement_opening_credit(settlement_id,amount) VALUES (?,800)", old);
        assertBalance("0", "0", "PAID");
        jdbc.update("UPDATE settlement SET total_amount = 850 WHERE settlement_id = ?", old);
        assertBalance("-50", "0", "DUE");
        mvc.perform(delete("/api/settlements/" + old)).andExpect(status().isOk());
        assertBalance("0", "0", "PAID");
        settle("100");
        assertBalance("-100", "0", "DUE");
    }

    @Test
    void shouldDeduplicateRetriesAndRejectChangedPayloadAndDeletedReplay() throws Exception {
        var key = UUID.randomUUID();
        String request = customRequest("500", key);
        var first = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated()).andReturn();
        var again = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated()).andReturn();
        assertThat(again.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertBalance("500", "500", "OVERPAID");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(customRequest("600", key))).andExpect(status().isConflict());
        long id = json.readTree(first.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete(url + "/" + id).param("version", "0")).andExpect(status().isNoContent());
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isConflict());
        assertBalance("0", "0", "PAID");
    }

    @Test
    void shouldSerializeConcurrentFullPaymentAndConcurrentRetries() throws Exception {
        settle("2539.89");
        var key = UUID.randomUUID();
        String request = fullRequest(key);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(request)).andReturn());
            var second = executor.submit(() -> mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(request)).andReturn());
            assertThat(first.get().getResponse().getStatus()).isEqualTo(201);
            assertThat(second.get().getResponse().getStatus()).isEqualTo(201);
        }
        assertBalance("0", "2539.89", "PAID");
        settle("100");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID()))).andReturn());
            var second = executor.submit(() -> mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID()))).andReturn());
            assertThat(java.util.List.of(first.get().getResponse().getStatus(), second.get().getResponse().getStatus())).containsExactlyInAnyOrder(201, 400);
        }
        assertBalance("0", "2639.89", "PAID");
    }

    @Test
    void shouldValidateMoneyAndScopeAllOperationsToTheOwnerAndTenancy() throws Exception {
        for (String amount : new String[]{"0", "-10", "1.001", "1000000000000"}) {
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(customRequest(amount, UUID.randomUUID()))).andExpect(status().isBadRequest());
        }
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"CUSTOM\",\"paymentDate\":\"2026-05-01\"}"))
                .andExpect(status().isBadRequest());
        var payment = create("100");
        var owner = users.getLoggedUser();
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(-1L, "other@test.local", Role.ADMIN));
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(customRequest("1", UUID.randomUUID()))).andExpect(status().isNotFound());
        mvc.perform(put(url + "/" + payment.get("id").asLong()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1,\"paymentDate\":\"2026-05-01\",\"version\":0}")).andExpect(status().isNotFound());
        mvc.perform(delete(url + "/" + payment.get("id").asLong()).param("version", "0")).andExpect(status().isNotFound());
        when(users.getLoggedUser()).thenReturn(owner);
        mvc.perform(get("/api/rented-objects/999999/tenants/" + tenantId + "/payments")).andExpect(status().isNotFound());
        assertBalance("100", "100", "OVERPAID");
    }

    @Test
    void shouldChargeTheNextActualMonthlyCalculationAgainstExistingCredit() throws Exception {
        create("2700");
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"waterIncluded\":false,\"electricityIncluded\":false,\"settlementDate\":\"2026-06-01T00:00:00\"}"))
                .andExpect(status().isOk());
        assertBalance("200", "2700", "OVERPAID");
        mvc.perform(get("/api/rented-objects/details").param("active", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tenantId == " + tenantId + ")].paymentSummary.balance").value(200.0));
    }

    @ParameterizedTest
    @CsvSource({"2509.79,0,500,500,2000", "300,100,2709.79,400,2100", "500,500,2509.79,0,2500", "3009.79,500,0,0,2500"})
    void shouldMeetDepositAcceptanceExamples(String amount, String deposit, String due, String depositDue, String paid) throws Exception {
        depositLease("2000", true);
        JsonNode before = history();
        assertThat(before.at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("500");
        assertThat(before.at("/summary/amountDue").decimalValue()).isZero();
        long settlementId = calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        assertDepositState("3009.79", "500", "2000");
        assertThat(history().get("payments")).isEmpty();
        JsonNode payment = createSplit(amount, deposit);
        assertThat(payment.get("depositAmount").decimalValue()).isEqualByComparingTo(deposit);
        assertDepositState(due, depositDue, paid);
        assertThat(jdbc.queryForObject("SELECT total_amount FROM settlement WHERE settlement_id=?", BigDecimal.class, settlementId)).isEqualByComparingTo("3009.79");
        JsonNode card = json.readTree(mvc.perform(get("/api/rented-objects/details").param("active", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get(0);
        assertThat(card.get("totalAmount").decimalValue()).isEqualByComparingTo("3009.79");
        assertThat(card.get("sum").decimalValue()).isEqualByComparingTo("434.79");
        assertThat(card.get("profit").decimalValue()).isEqualByComparingTo("1830");
        assertThat(card.at("/paymentSummary/depositDue").decimalValue()).isEqualByComparingTo(depositDue);
        assertThat(card.at("/paymentSummary/depositRequired").decimalValue()).isEqualByComparingTo("2500");
        assertThat(card.at("/paymentSummary/depositPaid").decimalValue()).isEqualByComparingTo(paid);
        mvc.perform(get("/api/settlements").param("rentedObjectId", "" + objectId).param("tenantId", "" + tenantId))
                .andExpect(jsonPath("$.content[0].depositTopUp").value(500))
                .andExpect(jsonPath("$.content[0].additionalCharge").value(75))
                .andExpect(jsonPath("$.content[0].additionalChargeDescription").value("Dorobienie klucza"));
    }

    @Test
    void shouldBookTheRemainingTotalAndCarryPaidDepositAcrossMonths() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        create("2509.79");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.depositAmount").value(500));
        assertDepositState("0", "0", "2500");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        assertDepositState("2434.79", "0", "2500");
    }

    @ParameterizedTest
    @CsvSource({"300,0,2709.79,500,2000", "2509.79,0,500,500,2000", "2709.79,200,300,300,2200",
            "3009.79,500,0,0,2500", "3100,500,0,0,2500"})
    void shouldBookOneAmountAndAccountForItsComponentsWithoutAnExplicitSplit(
            String amount, String deposit, String due, String depositDue, String paid) throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");

        JsonNode receipt = create(amount);

        assertThat(receipt.get("amount").decimalValue()).isEqualByComparingTo(amount);
        assertThat(receipt.get("depositAmount").decimalValue()).isEqualByComparingTo(deposit);
        assertDepositState(due, depositDue, paid);
        JsonNode state = history();
        assertThat(state.get("payments")).hasSize(1);
        assertThat(state.at("/summary/totalPayments").decimalValue()).isEqualByComparingTo(amount);
        assertThat(state.at("/summary/overpayment").decimalValue())
                .isEqualByComparingTo(new BigDecimal(amount).subtract(new BigDecimal("3009.79")).max(BigDecimal.ZERO));
    }

    @Test
    void shouldKeepHistoricalAllocationWhenCorrectingOnlyTheTotalAmountOrDescription() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        JsonNode receipt = createSplit("300", "100");
        String paymentUrl = url + "/" + receipt.get("id").asLong();

        mvc.perform(put(paymentUrl).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":300,\"paymentDate\":\"2026-05-13\",\"description\":\"Poprawiony opis\",\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.depositAmount").value(100));
        assertDepositState("2709.79", "400", "2100");

        mvc.perform(put(paymentUrl).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50,\"paymentDate\":\"2026-05-13\",\"description\":\"Mniejsza wpłata\",\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.depositAmount").value(50));
        assertDepositState("2959.79", "450", "2050");

        mvc.perform(delete(paymentUrl).param("version", "2")).andExpect(status().isNoContent());
        assertDepositState("3009.79", "500", "2000");
    }

    @Test
    void shouldKeepUnpaidDepositAcrossMonthsWithoutAssessingItAgain() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        createSplit("2509.79", "0");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        createSplit("2434.79", "0");
        assertDepositState("500", "500", "2000");
        assertThat(jdbc.queryForObject("SELECT SUM(deposit_top_up) FROM settlement WHERE tenant_id=?", BigDecimal.class, tenantId)).isEqualByComparingTo("500");
    }

    @Test
    void shouldReverseSplitOnCorrectionAndDeletionAndDeduplicateRetries() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        UUID key = UUID.randomUUID();
        String body = splitRequest("300", "100", key);
        JsonNode receipt = json.readTree(mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        assertDepositState("2709.79", "400", "2100");
        mvc.perform(put(url + "/" + receipt.get("id").asLong()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":400,\"depositAmount\":200,\"paymentDate\":\"2026-05-12\",\"version\":0}"))
                .andExpect(status().isOk());
        assertDepositState("2609.79", "300", "2200");
        mvc.perform(delete(url + "/" + receipt.get("id").asLong()).param("version", "1")).andExpect(status().isNoContent());
        assertDepositState("3009.79", "500", "2000");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
    }

    @Test
    void shouldReserveReceivedDepositWhenReversingAndReplacingASettlement() throws Exception {
        depositLease("2000", true);
        UUID key = UUID.randomUUID();
        long original = calculateWithDepositAndExtra(key, false, "75");
        JsonNode receipt = createSplit("300", "100");
        mvc.perform(delete("/api/settlements/" + original)).andExpect(status().isOk());
        assertBalance("200", "300", "OVERPAID");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("400");
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(calculationRequest(key, false, "75"))).andExpect(status().isConflict());
        long replacement = calculateWithDepositAndExtra(UUID.randomUUID(), false, "400", "75");
        assertThat(jdbc.queryForObject("SELECT deposit_top_up FROM settlement WHERE settlement_id=?", BigDecimal.class, replacement)).isEqualByComparingTo("400");
        assertDepositState("2709.79", "400", "2100");
        mvc.perform(delete(url + "/" + receipt.get("id").asLong()).param("version", "0")).andExpect(status().isNoContent());
        assertDepositState("3009.79", "500", "2000");
    }

    @Test
    void shouldAssignExistingOverpaymentToDepositAndReverseItWithItsReceipt() throws Exception {
        depositLease("2000", true);
        JsonNode receipt = create("3300");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        assertBalance("290.21", "3300", "OVERPAID");
        assertDepositState("0", "0", "2500");
        assertThat(history().at("/payments/0/depositAmount").decimalValue()).isEqualByComparingTo("500");
        mvc.perform(delete(url + "/" + receipt.get("id").asLong()).param("version", "1")).andExpect(status().isNoContent());
        assertDepositState("3009.79", "500", "2000");
    }

    @ParameterizedTest
    @CsvSource({"0,true,2500", "2500,true,0", "2000,false,0"})
    void shouldOnlyRemindForZeroFullAndUnconfirmedOpeningDeposit(String opening, boolean confirmed, String expected) throws Exception {
        depositLease(opening, confirmed);
        long id = calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        assertThat(jdbc.queryForObject("SELECT deposit_top_up FROM settlement WHERE settlement_id=?", BigDecimal.class, id)).isZero();
        if (confirmed) assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo(expected);
        else assertThat(history().at("/summary/depositReminder").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT deposit_paid FROM lease_agreement WHERE lease_agreement_id=?", BigDecimal.class, leaseId)).isEqualByComparingTo(opening);
    }

    @Test
    void shouldAddOneOffChargesInFullEvenForAPartialMonthAndKeepTaxBaseUnchanged() throws Exception {
        depositLease("2000", true);
        long id = calculateWithDepositAndExtra(UUID.randomUUID(), true, "75");
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM settlement WHERE settlement_id=?", id);
        assertThat((BigDecimal) row.get("deposit_top_up")).isEqualByComparingTo("500");
        assertThat((BigDecimal) row.get("additional_charge")).isEqualByComparingTo("75");
        BigDecimal compensation = (BigDecimal) row.get("compensation_amount");
        BigDecimal total = (BigDecimal) row.get("total_amount");
        mvc.perform(delete("/api/settlements/" + id)).andExpect(status().isOk());
        long withoutExtra = calculateWithDepositAndExtra(UUID.randomUUID(), true, "0");
        assertThat(jdbc.queryForObject("SELECT compensation_amount FROM settlement WHERE settlement_id=?", BigDecimal.class, withoutExtra)).isEqualByComparingTo(compensation);
        assertThat(total.subtract(jdbc.queryForObject("SELECT total_amount FROM settlement WHERE settlement_id=?", BigDecimal.class, withoutExtra))).isEqualByComparingTo("75");
    }

    @Test
    void shouldDeduplicateConcurrentSettlementRetriesAndValidateNewMoneyFields() throws Exception {
        depositLease("2000", true);
        UUID key = UUID.randomUUID();
        String request = calculationRequest(key, false, "75");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON).content(request)).andReturn());
            var second = executor.submit(() -> mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON).content(request)).andReturn());
            assertThat(first.get().getResponse().getStatus()).isEqualTo(200);
            assertThat(second.get().getResponse().getStatus()).isEqualTo(200);
        }
        assertDepositState("3009.79", "500", "2000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement WHERE tenant_id=?", Integer.class, tenantId)).isEqualTo(1);
        for (String extra : new String[]{"-1", "1.001"}) {
            mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                    .content(calculationRequest(UUID.randomUUID(), false, extra))).andExpect(status().isBadRequest());
        }
        for (String deposit : new String[]{"-1", "300.01", "501", "1.001"}) {
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(splitRequest("300", deposit, UUID.randomUUID()))).andExpect(status().isBadRequest());
        }
        assertDepositState("3009.79", "500", "2000");
    }

    @Test
    void shouldConfirmUnknownLegacyDepositAndApplyAnAdditionalReceiptBeforeAssessmentOnlyOnce() throws Exception {
        depositLease("0", false);
        jdbc.update("UPDATE lease_agreement SET deposit_paid=NULL, deposit_opening_paid=NULL WHERE lease_agreement_id=?", leaseId);
        long historical = settle("2500");
        jdbc.update("INSERT INTO settlement_opening_credit(settlement_id,amount) VALUES (?,2500)", historical);
        assertThat(history().at("/summary/depositReminder").isNull()).isTrue();
        assertThat(history().at("/summary/depositRequired").decimalValue()).isEqualByComparingTo("2500");
        assertThat(history().at("/summary/depositPaid").isNull()).isTrue();
        var tenant = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(mvc.perform(get("/api/tenants/" + tenantId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var lease = (com.fasterxml.jackson.databind.node.ObjectNode) tenant.get("leaseAgreementDto");
        lease.put("depositBalanceConfirmed", true);
        mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tenant)))
                .andExpect(status().isBadRequest());
        lease.put("depositPaid", 2000);
        mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tenant)))
                .andExpect(status().isOk());
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("500");
        lease.put("depositPaid", 2200);
        mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tenant)))
                .andExpect(status().isOk());
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("300");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "300", "75");
        assertDepositState("2809.79", "300", "2200");
        // Ordinary agreement edits preserve the opening amount and machine allocation.
        mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tenant)))
                .andExpect(status().isOk());
        assertDepositState("2809.79", "300", "2200");
        lease.put("depositPaid", 2300);
        mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tenant)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT total_amount FROM settlement WHERE settlement_id=?", BigDecimal.class, historical)).isEqualByComparingTo("2500");
    }

    @Test
    void shouldKeepDepositPendingAfterValidationFailureAndRejectChangedRetryPayload() throws Exception {
        depositLease("2000", true);
        UUID key = UUID.randomUUID();
        var staleTenantRequest = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(calculationRequest(key, false, "75"));
        staleTenantRequest.put("tenantId", -1);
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(staleTenantRequest))).andExpect(status().isConflict());
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(calculationRequest(key, false, "-75"))).andExpect(status().isBadRequest());
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("500");
        long id = calculateWithDepositAndExtra(key, false, "75");
        assertThat(calculateWithDepositAndExtra(key, false, "75")).isEqualTo(id);
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(calculationRequest(key, false, "80"))).andExpect(status().isConflict());
        assertDepositState("3009.79", "500", "2000");
        JsonNode receipt = createSplit("4000", "0");
        assertThat(receipt.get("depositAmount").decimalValue()).isEqualByComparingTo("500");
        assertBalance("990.21", "4000", "OVERPAID");
        assertDepositState("0", "0", "2500");
    }

    @Test
    void shouldChargeDepositForFullMonthWithUtilitiesEnabledAsWellAsInitialAndPartialTypes() throws Exception {
        depositLease("2000", true);
        var request = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(calculationRequest(UUID.randomUUID(), false, "75"));
        request.put("waterIncluded", true).put("electricityIncluded", true);
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andExpect(status().isOk());
        assertDepositState("3009.79", "500", "2000");
    }

    @Test
    void shouldUseRemainingOverpaymentForDepositAfterDeletingADepositReceipt() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        JsonNode deposit = createSplit("500", "500");
        createSplit("3000", "0");
        mvc.perform(delete(url + "/" + deposit.get("id").asLong()).param("version", "0")).andExpect(status().isNoContent());
        assertDepositState("9.79", "9.79", "2490.21");
        assertThat(history().at("/payments/0/depositAmount").decimalValue()).isEqualByComparingTo("490.21");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(9.79))
                .andExpect(jsonPath("$.depositAmount").value(9.79));
        assertDepositState("0", "0", "2500");
    }

    @Test
    void shouldReallocateCreditOnReceiptCorrectionWithoutRequiringMoreThanTheTransfer() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        JsonNode deposit = createSplit("500", "500");
        createSplit("3000", "0");
        mvc.perform(put(url + "/" + deposit.get("id").asLong()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"depositAmount\":100,\"paymentDate\":\"2026-05-12\",\"version\":0}"))
                .andExpect(status().isOk());
        assertBalance("90.21", "3100", "OVERPAID");
        assertDepositState("0", "0", "2500");
    }

    @Test
    void shouldApplyCreditReleasedByDeletingAnotherMonthlyChargeToDeposit() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        long otherMonth = calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        createSplit("4944.58", "0");
        assertDepositState("500", "500", "2000");
        mvc.perform(delete("/api/settlements/" + otherMonth)).andExpect(status().isOk());
        assertBalance("1934.79", "4944.58", "OVERPAID");
        assertDepositState("0", "0", "2500");
    }

    @Test
    void shouldScopeSettlementMutationsHistoryAndExportToItsOwner() throws Exception {
        depositLease("2000", true);
        long id = calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        String tenantBody = mvc.perform(get("/api/tenants/" + tenantId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var owner = users.getLoggedUser();
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(-1L, "other@test.local", Role.ADMIN));
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(calculationRequest(UUID.randomUUID(), false, "75"))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/settlements/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/settlements").param("rentedObjectId", "" + objectId).param("tenantId", "" + tenantId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/settlements/tenant/" + tenantId + "/exists")).andExpect(status().isNotFound());
        mvc.perform(get("/api/tenants/" + tenantId)).andExpect(status().isNotFound());
        mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(tenantBody)).andExpect(status().isNotFound());
        byte[] export = mvc.perform(get("/api/settlements/export/excel")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(export))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
        }
        mvc.perform(get("/api/lease-agreements/" + leaseId + "/termination-info")).andExpect(status().isNotFound());
        mvc.perform(post("/api/lease-agreements/" + leaseId + "/terminate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        when(users.getLoggedUser()).thenReturn(owner);
        assertDepositState("3009.79", "500", "2000");
        byte[] ownExport = mvc.perform(get("/api/settlements/export/excel")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(ownExport))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1);
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(4).getNumericCellValue()).isEqualTo(id);
        }
    }

    @Test
    void shouldNotTreatUnknownReceivedDepositAsZeroOnTermination() throws Exception {
        jdbc.update("UPDATE lease_agreement SET deposit_paid=NULL, deposit_opening_paid=NULL WHERE lease_agreement_id=?", leaseId);
        String leaseUrl = "/api/lease-agreements/" + leaseId;
        mvc.perform(post(leaseUrl + "/deposit-return-preview").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(leaseUrl + "/terminate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT is_contract_active FROM lease_agreement WHERE lease_agreement_id=?", Boolean.class, leaseId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement WHERE tenant_id=?", Integer.class, tenantId)).isZero();
        jdbc.update("UPDATE lease_agreement SET deposit_paid=0, deposit_opening_paid=0 WHERE lease_agreement_id=?", leaseId);
        mvc.perform(post(leaseUrl + "/deposit-return-preview").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post(leaseUrl + "/terminate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post(leaseUrl + "/terminate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT returned_deposit_amount FROM lease_agreement WHERE lease_agreement_id=?", BigDecimal.class, leaseId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement WHERE tenant_id=? AND deposit_assessment=true", Integer.class, tenantId)).isZero();
    }

    @Test
    void shouldShowNewestSettlementWhenTwoHaveTheSameDate() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "75");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        JsonNode card = json.readTree(mvc.perform(get("/api/rented-objects/details").param("active", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get(0);
        assertThat(card.get("totalAmount").decimalValue()).isEqualByComparingTo("2434.79");
        assertThat(card.get("depositTopUp").decimalValue()).isZero();
        assertThat(card.at("/paymentSummary/depositDue").decimalValue()).isEqualByComparingTo("500");
    }

    @Test
    void shouldRejectDepositAmountsOutsideTheLedgerPrecisionBeforeSaving() throws Exception {
        var tenant = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(mvc.perform(get("/api/tenants/" + tenantId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var lease = (com.fasterxml.jackson.databind.node.ObjectNode) tenant.get("leaseAgreementDto");
        for (String invalid : new String[]{"-1", "1.001", "100000", "1000000000000"}) {
            lease.put("depositPaid", new BigDecimal(invalid));
            mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tenant)))
                    .andExpect(status().isBadRequest());
        }
        lease.put("depositPaid", 0).put("id", leaseId + 1000000);
        mvc.perform(put("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tenant)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT deposit_paid FROM lease_agreement WHERE lease_agreement_id=?", BigDecimal.class, leaseId)).isZero();
    }

    @Test
    void shouldRejectInvalidPartialPeriodsWithoutAssessingDeposit() throws Exception {
        depositLease("2000", true);
        for (String start : new String[]{"2026-06-30T00:00:00", "2026-05-15T00:00:00", ""}) {
            var request = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(calculationRequest(UUID.randomUUID(), true, "75"));
            request.put("periodStartDate", start).put("periodEndDate", "2026-06-15T00:00:00");
            mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement WHERE tenant_id=?", Integer.class, tenantId)).isZero();
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("500");
    }

    @Test
    void shouldOnlyRemindUntilDepositIsManuallyAddedAndNeverIncludeItInFullPayment() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        assertDepositState("2434.79", "0", "2000");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("500");
        assertThat(history().at("/summary/depositToCharge").decimalValue()).isEqualByComparingTo("500");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(2434.79)).andExpect(jsonPath("$.depositAmount").value(0));
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        assertDepositState("2434.79", "0", "2000");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("500");
        assertThat(jdbc.queryForObject("SELECT SUM(deposit_top_up) FROM settlement WHERE tenant_id=?", BigDecimal.class, tenantId)).isZero();
    }

    @Test
    void shouldAllowManualDepositInstalmentsAndRemindAboutTheUnbilledRemainderNextMonth() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "300", "30");
        assertDepositState("2764.79", "300", "2000");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.depositAmount").value(300));
        assertDepositState("0", "0", "2300");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        assertDepositState("2434.79", "0", "2300");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("200");
        assertThat(history().at("/summary/depositToCharge").decimalValue()).isEqualByComparingTo("200");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "200", "0");
        assertDepositState("5069.58", "200", "2300");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.depositAmount").value(200));
        assertDepositState("0", "0", "2500");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isZero();
    }

    @Test
    void shouldRemindAboutPartlyPaidBilledDepositWithoutChargingItAgain() throws Exception {
        depositLease("2000", true);
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "500", "0");
        createSplit("300", "300");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("200");
        assertThat(history().at("/summary/depositToCharge").decimalValue()).isZero();
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "0", "0");
        assertDepositState("5069.58", "200", "2300");
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(calculationRequest(UUID.randomUUID(), false, "200", "0"))).andExpect(status().isBadRequest());
        assertDepositState("5069.58", "200", "2300");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("200");
    }

    @Test
    void shouldStoreEveryPositionAndExportItsTypeAmountAndDescription() throws Exception {
        depositLease("2000", true);
        var request = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(calculationRequest(UUID.randomUUID(), false, "0", "0"));
        request.set("additionalCharges", json.valueToTree(java.util.List.of(
                Map.of("type", "DEPOSIT", "amount", 100, "description", "Kaucja - część 1"),
                Map.of("type", "DEPOSIT", "amount", 100, "description", "Kaucja - część 2"),
                Map.of("type", "OTHER", "amount", 30, "description", "Klucze"),
                Map.of("type", "OTHER", "amount", 75, "description", "Krzesło"))));
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andExpect(status().isOk());
        assertDepositState("2739.79", "200", "2000");
        mvc.perform(get("/api/settlements").param("rentedObjectId", "" + objectId).param("tenantId", "" + tenantId))
                .andExpect(jsonPath("$.content[0].additionalCharges.length()").value(4))
                .andExpect(jsonPath("$.content[0].additionalCharges[2].description").value("Klucze"))
                .andExpect(jsonPath("$.content[0].additionalCharges[3].amount").value(75));
        mvc.perform(get("/api/rented-objects/details").param("active", "true"))
                .andExpect(jsonPath("$[0].additionalCharges.length()").value(4));
        byte[] exported = mvc.perform(get("/api/settlements/export/excel")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(exported))) {
            var positions = workbook.getSheet("Pozycje naliczeń");
            assertThat(positions.getLastRowNum()).isEqualTo(4);
            assertThat(positions.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Kaucja");
            assertThat(positions.getRow(3).getCell(2).getNumericCellValue()).isEqualTo(30);
            assertThat(positions.getRow(4).getCell(3).getStringCellValue()).isEqualTo("Krzesło");
        }
        // Validate the combined deposit amount, not just each individual position.
        ((com.fasterxml.jackson.databind.node.ArrayNode) request.get("additionalCharges")).removeAll()
                .add(json.valueToTree(Map.of("type", "DEPOSIT", "amount", 200)))
                .add(json.valueToTree(Map.of("type", "DEPOSIT", "amount", 200)));
        request.put("requestId", UUID.randomUUID().toString());
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        assertDepositState("2739.79", "200", "2000");
    }

    @Test
    void shouldPreserveDepositHeldWhenOneOfSeveralManualBillsIsDeleted() throws Exception {
        depositLease("2000", true);
        long first = calculateWithDepositAndExtra(UUID.randomUUID(), false, "300", "0");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "200", "0");
        createSplit("300", "300");
        mvc.perform(delete("/api/settlements/" + first)).andExpect(status().isOk());
        assertDepositState("2434.79", "0", "2300");
        assertThat(history().at("/summary/depositToCharge").decimalValue()).isEqualByComparingTo("200");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, "200", "0");
        assertDepositState("5069.58", "200", "2300");
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(fullRequest(UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.depositAmount").value(200));
        assertDepositState("0", "0", "2500");
    }

    @Test
    void shouldKeepLegacySingleChargeOrdinaryEvenIfItsDescriptionSaysDeposit() throws Exception {
        depositLease("2000", true);
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"settlementDate\":\"2026-06-01T00:00:00\",\"additionalCharge\":200,\"additionalChargeDescription\":\"Kaucja\"}"))
                .andExpect(status().isOk());
        assertDepositState("2634.79", "0", "2000");
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo("500");
    }

    @ParameterizedTest
    @CsvSource({"0,865.21,2000,500", "200,665.21,2200,300"})
    void shouldUseOverpaymentOnlyForManuallyRequestedDeposit(String deposit, String credit, String received, String reminder) throws Exception {
        depositLease("2000", true);
        create("3300");
        calculateWithDepositAndExtra(UUID.randomUUID(), false, deposit, "0");
        assertBalance(credit, "3300", "OVERPAID");
        assertDepositState("0", "0", received);
        assertThat(history().at("/summary/depositReminder").decimalValue()).isEqualByComparingTo(reminder);
        assertThat(history().at("/payments/0/depositAmount").decimalValue()).isEqualByComparingTo(deposit);
    }

    private void depositLease(String opening, boolean confirmed) {
        jdbc.update("UPDATE lease_agreement SET deposit=2500, deposit_paid=?, deposit_opening_paid=?, deposit_balance_confirmed=?, gas_deposit=94.79 WHERE lease_agreement_id=?",
                new BigDecimal(opening), new BigDecimal(opening), confirmed, leaseId);
    }
    private JsonNode history() throws Exception {
        return json.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private void assertDepositState(String due, String depositDue, String paid) throws Exception {
        JsonNode state = history();
        assertThat(state.at("/summary/amountDue").decimalValue()).isEqualByComparingTo(due);
        assertThat(state.at("/summary/depositDue").decimalValue()).isEqualByComparingTo(depositDue);
        assertThat(state.at("/summary/depositRequired").decimalValue()).isEqualByComparingTo("2500");
        assertThat(state.at("/summary/depositPaid").decimalValue()).isEqualByComparingTo(paid);
        assertThat(jdbc.queryForObject("SELECT deposit_paid FROM lease_agreement WHERE lease_agreement_id=?", BigDecimal.class, leaseId)).isEqualByComparingTo(paid);
    }
    private JsonNode createSplit(String amount, String deposit) throws Exception {
        return json.readTree(mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(splitRequest(amount, deposit, UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private String splitRequest(String amount, String deposit, UUID key) throws Exception {
        return json.writeValueAsString(Map.of("requestId", key, "mode", "CUSTOM", "amount", new BigDecimal(amount), "depositAmount", new BigDecimal(deposit), "paymentDate", "2026-05-12"));
    }
    private long calculateWithDepositAndExtra(UUID key, boolean partial, String extra) throws Exception {
        return calculateWithDepositAndExtra(key, partial, "500", extra);
    }
    private long calculateWithDepositAndExtra(UUID key, boolean partial, String deposit, String extra) throws Exception {
        mvc.perform(post("/api/rented-objects/" + objectId + "/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(calculationRequest(key, partial, deposit, extra))).andExpect(status().isOk());
        return jdbc.queryForObject("SELECT MAX(settlement_id) FROM settlement WHERE tenant_id=?", Long.class, tenantId);
    }
    private String calculationRequest(UUID key, boolean partial, String extra) throws Exception {
        return calculationRequest(key, partial, "500", extra);
    }
    private String calculationRequest(UUID key, boolean partial, String deposit, String extra) throws Exception {
        return json.writeValueAsString(Map.of("requestId", key, "settlementDate", "2026-06-01T00:00:00", "isPartialMonth", partial,
                "periodStartDate", "2026-06-15T00:00:00", "periodEndDate", "2026-06-30T00:00:00", "isRentProrated", partial,
                "additionalCharges", java.util.List.of(
                        Map.of("type", "DEPOSIT", "amount", new BigDecimal(deposit), "description", "Kaucja"),
                        Map.of("type", "OTHER", "amount", new BigDecimal(extra), "description", "Dorobienie klucza"))));
    }

    private long next(String sequence) { return jdbc.queryForObject("SELECT nextval('" + sequence + "')", Long.class); }
    private long settle(String amount) {
        long id = next("SETTLEMENT_SEQ");
        jdbc.update("""
                INSERT INTO settlement(settlement_id,rented_object_id,tenant_id,total_amount,compensation_amount,rent_amount,internet_fee,
                gas_deposit,settlement_date,status,insert_date,insert_operator)
                VALUES (?,?,?, ?,2000,300,40,160,'2026-04-01','TO_PAY',now(),'test')
                """, id, objectId, tenantId, new BigDecimal(amount));
        return id;
    }
    private JsonNode create(String amount) throws Exception {
        return json.readTree(mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(customRequest(amount, UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private String customRequest(String amount, UUID key) throws Exception {
        return json.writeValueAsString(Map.of("requestId", key, "mode", "CUSTOM", "amount", new BigDecimal(amount), "paymentDate", "2026-05-12", "description", "Przelew"));
    }
    private String fullRequest(UUID key) throws Exception {
        return json.writeValueAsString(Map.of("requestId", key, "mode", "FULL", "paymentDate", "2026-05-12"));
    }
    private void assertBalance(String balance, String payments, String statusName) throws Exception {
        JsonNode result = json.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(result.at("/summary/balance").decimalValue()).isEqualByComparingTo(balance);
        assertThat(result.at("/summary/totalPayments").decimalValue()).isEqualByComparingTo(payments);
        assertThat(result.at("/summary/status").asText()).isEqualTo(statusName);
    }
}
