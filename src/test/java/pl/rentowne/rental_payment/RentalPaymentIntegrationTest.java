package pl.rentowne.rental_payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private String url;

    @BeforeEach
    void tenancy() {
        when(auditor.getCurrentAuditor()).thenReturn(Optional.of("payments-test"));
        long userId = next("USER_ACCOUNT_SEQ");
        long addressId = next("ADDRESS_SEQ");
        long apartmentId = next("APARTMENT_SEQ");
        objectId = next("RENTED_OBJECT_SEQ");
        long leaseId = next("LEASE_AGREEMENT_SEQ");
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
