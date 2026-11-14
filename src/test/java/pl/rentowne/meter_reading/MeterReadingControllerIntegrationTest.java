package pl.rentowne.meter_reading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.rentowne.TestSecurityConfig;
import pl.rentowne.meter.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class MeterReadingControllerIntegrationTest {
    // A separate database avoids collisions with existing tests that reset shared sequences.
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
    @Autowired MeterReadingRepository readings;
    @MockitoBean AuditorAware<String> auditor;

    private static final LocalDateTime PREVIOUS_DATE = LocalDateTime.of(2026, 9, 30, 0, 0);
    private long meterId;

    @BeforeEach
    void createMeter() {
        when(auditor.getCurrentAuditor()).thenReturn(Optional.of("meter-reading-test"));
        meterId = jdbc.queryForObject("SELECT nextval('meter_seq')", Long.class);
        jdbc.update("""
                INSERT INTO meter(meter_id,name,type,distribute_readings,insert_date,insert_operator)
                VALUES (?,'Testowy licznik','WATER_COLD',false,now(),'test')
                """, meterId);
    }

    @Test
    void shouldUseLatestReadingByDateFromTheSelectedMeterAndPreservePrecision() throws Exception {
        seedReading("125.12345", PREVIOUS_DATE);
        // Insert the older reading later: the baseline is chosen by date, not by its ID.
        seedReading("90", PREVIOUS_DATE.minusMonths(1));
        long otherMeterId = jdbc.queryForObject("SELECT nextval('meter_seq')", Long.class);
        jdbc.update("INSERT INTO meter(meter_id,name,type,distribute_readings,insert_date,insert_operator) VALUES (?,'Inny','GAS',false,now(),'test')", otherMeterId);
        seedReading(otherMeterId, "900", PREVIOUS_DATE.plusDays(1));

        submit(request("150.54321", PREVIOUS_DATE.plusMonths(1))).andExpect(status().isCreated());

        MeterReading saved = latest();
        assertThat(saved.getCurrentReading()).isEqualByComparingTo("150.54321");
        assertThat(saved.getConsumption()).isEqualByComparingTo("25.41976");
        assertThat(saved.getReadingDate()).isEqualTo(PREVIOUS_DATE.plusMonths(1));
        assertThat(saved.isSettled()).isFalse();
        assertThat(saved.isApartmentSettled()).isFalse();
        assertThat(saved.getInsertOperator()).isEqualTo("meter-reading-test");
        assertThat(count()).isEqualTo(3);
    }

    @Test
    void shouldIgnoreClientSuppliedHistoryConsumptionIdAndSettlementFlags() throws Exception {
        long existingId = seedReading("110", PREVIOUS_DATE);
        ObjectNode body = request("150", PREVIOUS_DATE.plusMonths(1));
        body.put("id", existingId);
        body.put("previousReading", 0);
        body.put("previousReadingDate", "2000-01-01T00:00:00");
        body.put("consumption", 999);
        body.put("meterType", "UNTRUSTED_CLIENT_TYPE");
        body.put("settled", true);
        body.put("apartmentSettled", true);
        body.putObject("meterDto").put("id", -1);

        submit(body).andExpect(status().isCreated());

        assertThat(latest().getId()).isNotEqualTo(existingId);
        assertThat(latest().getConsumption()).isEqualByComparingTo("40");
        assertThat(latest().isSettled()).isFalse();
        assertThat(latest().isApartmentSettled()).isFalse();
        assertThat(readings.findById(existingId).orElseThrow().getCurrentReading()).isEqualByComparingTo("110");
        assertThat(count()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"99", "100"})
    void shouldRejectValueNotGreaterThanDatabaseReadingEvenWithForgedClientHistory(String value) throws Exception {
        seedReading("100", PREVIOUS_DATE);
        ObjectNode body = request(value, PREVIOUS_DATE.plusMonths(1));
        body.put("previousReading", 0);
        body.put("previousReadingDate", "2000-01-01T00:00:00");

        submit(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_METER_READING_VALUES"));
        assertThat(count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void shouldRejectDateNotAfterDatabaseReading(int dayOffset) throws Exception {
        seedReading("100", PREVIOUS_DATE);
        ObjectNode body = request("150", PREVIOUS_DATE.plusDays(dayOffset));
        body.put("previousReadingDate", "2000-01-01T00:00:00");

        submit(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_METER_READING_DATE"));
        assertThat(count()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(MeterType.class)
    void shouldUseInitialStateFromDatabaseForFirstReadingWithoutClientMeterType(MeterType type) throws Exception {
        jdbc.update("UPDATE meter SET type=?,initial_meter_reading=80.12345,installation_date=? WHERE meter_id=?",
                type.name(), PREVIOUS_DATE, meterId);

        submit(request("100.54321", PREVIOUS_DATE.plusDays(1))).andExpect(status().isCreated());

        assertThat(latest().getConsumption()).isEqualByComparingTo("20.41976");
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void shouldValidateAgainstInitialReadingAndInstallationDate() throws Exception {
        jdbc.update("UPDATE meter SET initial_meter_reading=80,installation_date=? WHERE meter_id=?", PREVIOUS_DATE, meterId);
        submit(request("79", PREVIOUS_DATE.plusDays(1))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_METER_READING_VALUES"));
        submit(request("90", PREVIOUS_DATE)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_METER_READING_DATE"));
        assertThat(count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "99999"})
    void shouldAcceptFirstReadingWithoutHistoryOrInitialState(String value) throws Exception {
        submit(request(value, PREVIOUS_DATE)).andExpect(status().isCreated());
        assertThat(latest().getConsumption()).isEqualByComparingTo(value);
    }

    @Test
    void shouldReloadHistoryForEveryFollowingRequest() throws Exception {
        seedReading("100", PREVIOUS_DATE);
        submit(request("150", PREVIOUS_DATE.plusMonths(1))).andExpect(status().isCreated());
        submit(request("180", PREVIOUS_DATE.plusMonths(2))).andExpect(status().isCreated());
        assertThat(latest().getConsumption()).isEqualByComparingTo("30");
        assertThat(count()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"meterId", "currentReading", "readingDate"})
    void shouldRejectMissingRequiredFields(String field) throws Exception {
        ObjectNode body = request("150", PREVIOUS_DATE);
        body.remove(field);
        submit(body).andExpect(status().isBadRequest());
        assertThat(count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"meterId", "currentReading", "readingDate"})
    void shouldRejectNullRequiredFields(String field) throws Exception {
        ObjectNode body = request("150", PREVIOUS_DATE);
        body.putNull(field);
        submit(body).andExpect(status().isBadRequest());
        assertThat(count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.00001", "100000", "1.123456"})
    void shouldRejectInvalidReadingValues(String value) throws Exception {
        submit(request(value, PREVIOUS_DATE)).andExpect(status().isBadRequest());
        assertThat(count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void shouldRejectInvalidMeterId(long invalidId) throws Exception {
        ObjectNode body = request("100", PREVIOUS_DATE).put("meterId", invalidId);
        submit(body).andExpect(status().isBadRequest());
        assertThat(count()).isZero();
    }

    @Test
    void shouldRejectInvalidDateFormat() throws Exception {
        submit(request("100", PREVIOUS_DATE).put("readingDate", "not-a-date")).andExpect(status().isBadRequest());
        assertThat(count()).isZero();
    }

    @Test
    void shouldReturnNotFoundForUnknownMeter() throws Exception {
        submit(request("100", PREVIOUS_DATE).put("meterId", Long.MAX_VALUE)).andExpect(status().isNotFound());
        assertThat(count()).isZero();
    }

    @Test
    void shouldRejectOldRequestShapeInsteadOfTakingMeterIdFromNestedDto() throws Exception {
        ObjectNode body = request("100", PREVIOUS_DATE);
        body.remove("meterId");
        body.putObject("meterDto").put("id", meterId);
        submit(body).andExpect(status().isBadRequest());
        assertThat(count()).isZero();
    }

    @Test
    void shouldSaveOnlyOneOfTwoConcurrentIdenticalReadings() throws Exception {
        seedReading("100", PREVIOUS_DATE);
        String body = request("150", PREVIOUS_DATE.plusMonths(1)).toString();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> submitTogether = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Requests did not start together");
                return mvc.perform(post("/api/meter-reading").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(submitTogether);
            var second = executor.submit(submitTogether);
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 400);
        }
        assertThat(count()).isEqualTo(2);
        assertThat(latest().getConsumption()).isEqualByComparingTo("50");
    }

    private ObjectNode request(String value, LocalDateTime date) {
        return json.createObjectNode().put("meterId", meterId)
                .put("currentReading", new BigDecimal(value)).put("readingDate", date.toString());
    }

    private ResultActions submit(ObjectNode body) throws Exception {
        return mvc.perform(post("/api/meter-reading").contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    private long seedReading(String value, LocalDateTime date) {
        return seedReading(meterId, value, date);
    }

    private long seedReading(long targetMeterId, String value, LocalDateTime date) {
        Long id = jdbc.queryForObject("SELECT nextval('meter_reading_seq')", Long.class);
        jdbc.update("""
                INSERT INTO meter_reading(meter_reading_id,meter_id,current_reading,reading_date,consumption,
                settled,apartment_settled,insert_date,insert_operator) VALUES (?,?,?,?,0,false,false,now(),'test')
                """, id, targetMeterId, new BigDecimal(value), date);
        return id;
    }

    private MeterReading latest() {
        return readings.getMeterReadingByMeter(meterId);
    }

    private int count() {
        return jdbc.queryForObject("SELECT count(*) FROM meter_reading WHERE meter_id=?", Integer.class, meterId);
    }
}
