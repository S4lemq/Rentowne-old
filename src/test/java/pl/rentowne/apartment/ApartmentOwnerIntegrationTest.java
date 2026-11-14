package pl.rentowne.apartment;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.rentowne.TestSecurityConfig;
import pl.rentowne.user.Role;
import pl.rentowne.user.UserBasicDto;
import pl.rentowne.user.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ApartmentOwnerIntegrationTest {
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

    private long userId;
    private long otherUserId;
    private long apartmentId;
    private long secondApartmentId;
    private long otherApartmentId;

    @BeforeEach
    void fixtures() {
        when(auditor.getCurrentAuditor()).thenReturn(Optional.of("owners-test"));
        userId = createUser();
        otherUserId = createUser();
        apartmentId = createApartment(userId);
        secondApartmentId = createApartment(userId);
        otherApartmentId = createApartment(otherUserId);
        login(userId);
    }

    @Test
    void createsOwnerDuringEditAndPreservesPropertyIdentityAndAssociations() throws Exception {
        Long addressId = jdbc.queryForObject("SELECT address_id FROM apartment WHERE apartment_id=?", Long.class, apartmentId);
        long objectId = next("RENTED_OBJECT_SEQ");
        jdbc.update("INSERT INTO rented_object(rented_object_id,rented_object_name,is_rented,apartment_id,insert_date,insert_operator) VALUES (?,'23a',true,?,now(),'test')", objectId, apartmentId);

        Map<String, Object> request = update(owner());
        request.put("addressDto", Map.of("id", 999999, "streetName", "Nowa", "buildingNumber", "2", "apartmentNumber", "3",
                "zipCode", "60-101", "cityName", "Poznań", "voivodeship", "Wielkopolskie"));
        Map<String, Object> saved = save(apartmentId, request);
        Map<String, Object> owner = nestedOwner(saved);

        assertThat(owner.get("id")).isNotNull();
        assertThat(owner.get("version")).isEqualTo(0);
        assertThat(owner.get("firstName")).isEqualTo("Jan");
        assertThat(owner.get("accountNumber")).isEqualTo("61109010140000071219812874");
        assertThat(saved.get("pinned")).isEqualTo(true);
        assertThat(saved.get("isRented")).isEqualTo(true);
        assertThat(jdbc.queryForObject("SELECT address_id FROM apartment WHERE apartment_id=?", Long.class, apartmentId)).isEqualTo(addressId);
        assertThat(jdbc.queryForObject("SELECT user_account_id FROM apartment WHERE apartment_id=?", Long.class, apartmentId)).isEqualTo(userId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rented_object WHERE rented_object_id=? AND apartment_id=?", Long.class, objectId, apartmentId)).isEqualTo(1);

        mvc.perform(get("/api/apartments/list")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void sharesOwnerAcrossPropertiesAndReturnsUpdatedData() throws Exception {
        Map<String, Object> first = nestedOwner(save(apartmentId, update(owner())));
        Map<String, Object> second = nestedOwner(save(secondApartmentId, update(first)));
        assertThat(second.get("id")).isEqualTo(first.get("id"));

        first.put("representativeName", "Anna Nowak");
        first.put("representativeEmail", "anna@example.com");
        first.put("paymentAccountHolder", "REPRESENTATIVE");
        first.put("cityName", "Gdańsk");
        Map<String, Object> edited = nestedOwner(save(apartmentId, update(first)));
        assertThat(edited.get("version")).isEqualTo(1);
        mvc.perform(get("/api/apartments/{id}", secondApartmentId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerDto.cityName").value("Gdańsk"))
                .andExpect(jsonPath("$.ownerDto.representativeEmail").value("anna@example.com"))
                .andExpect(jsonPath("$.ownerDto.paymentAccountHolder").value("REPRESENTATIVE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM property_owner WHERE user_account_id=?", Long.class, userId)).isEqualTo(1);

        // Older clients omitting ownerDto must not remove the assignment.
        Map<String, Object> withoutOwner = update(null);
        save(secondApartmentId, withoutOwner);
        mvc.perform(get("/api/apartments/{id}", secondApartmentId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerDto.id").value(first.get("id")));
    }

    @Test
    void rejectsStaleSharedOwnerUpdatesAtomically() throws Exception {
        Map<String, Object> original = nestedOwner(save(apartmentId, update(owner())));
        Map<String, Object> changed = new HashMap<>(original);
        changed.put("lastName", "Nowak");
        save(apartmentId, update(changed));

        Map<String, Object> stale = update(original);
        stale.put("apartmentName", "Nie zapisz tej nazwy");
        mvc.perform(put("/api/apartments/{id}", apartmentId).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(stale)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
        assertThat(jdbc.queryForObject("SELECT apartment_name FROM apartment WHERE apartment_id=?", String.class, apartmentId)).isEqualTo("Zmieniona nieruchomość");
        mvc.perform(get("/api/apartments/{id}", apartmentId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerDto.lastName").value("Nowak"));
    }

    @Test
    void preventsReadingEditingAndAssigningAnotherUsersOwner() throws Exception {
        login(otherUserId);
        Map<String, Object> foreign = nestedOwner(save(otherApartmentId, update(owner())));
        login(userId);
        mvc.perform(get("/api/owners")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/apartments/{id}", otherApartmentId)).andExpect(status().isNotFound());
        mvc.perform(put("/api/apartments/{id}", otherApartmentId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(update(owner())))).andExpect(status().isNotFound());
        mvc.perform(put("/api/apartments/{id}", apartmentId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(update(foreign)))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT owner_id FROM apartment WHERE apartment_id=?", Long.class, apartmentId)).isNull();
    }

    @Test
    void validatesOwnerEmailsAccountAndRepresentativeBeforeSaving() throws Exception {
        for (Map<String, Object> invalid : java.util.List.<Map<String, Object>>of(
                Map.of("firstName", "   "), Map.of("email", "nie-email"), Map.of("zipCode", "60101"),
                Map.of("accountNumber", "61109010140000071219812875"),
                Map.of("accountNumber", "123"), Map.of("representativeName", "Anna Nowak"),
                Map.of("representativeName", "Anna Nowak", "representativeEmail", "błędny-email"),
                Map.of("paymentAccountHolder", "REPRESENTATIVE"))) {
            Map<String, Object> payload = owner();
            payload.putAll(invalid);
            mvc.perform(put("/api/apartments/{id}", apartmentId).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(update(payload))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isMap());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM property_owner WHERE user_account_id=?", Long.class, userId)).isZero();
        save(apartmentId, update(null));
    }

    @Test
    void rejectsOwnerOnApartmentCreationAndDoesNotCreateOrphanOwnerOnFailedEdit() throws Exception {
        mvc.perform(post("/api/apartments").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(update(owner())))).andExpect(status().isBadRequest());

        Map<String, Object> invalidProperty = update(owner());
        invalidProperty.put("apartmentName", "x".repeat(100));
        assertThatThrownBy(() -> mvc.perform(put("/api/apartments/{id}", apartmentId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(invalidProperty)))).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM property_owner WHERE user_account_id=?", Long.class, userId)).isZero();
    }

    private Map<String, Object> owner() {
        return new HashMap<>(Map.of("firstName", " Jan ", "lastName", "Kowalski", "email", "jan@example.com",
                "streetName", "Jasna", "buildingNumber", "12", "zipCode", "60-101", "cityName", "Poznań",
                "accountNumber", "pl61 1090 1014 0000 0712 1981 2874", "paymentAccountHolder", "OWNER"));
    }

    private Map<String, Object> update(Map<String, Object> owner) {
        Map<String, Object> request = new HashMap<>(Map.of("apartmentName", "Zmieniona nieruchomość", "leasesNumber", 1, "area", 40));
        if (owner != null) request.put("ownerDto", owner);
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedOwner(Map<String, Object> response) {
        return (Map<String, Object>) response.get("ownerDto");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> save(long id, Map<String, Object> request) throws Exception {
        String response = mvc.perform(put("/api/apartments/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readValue(response, Map.class);
    }

    private long createUser() {
        long id = next("USER_ACCOUNT_SEQ");
        jdbc.update("INSERT INTO user_account(user_account_id,first_name,last_name,email,password,role,mfa_enabled,preferred_language,insert_date,insert_operator) VALUES (?,'Test','User',?,'test','ADMIN',false,'POLISH',now(),'test')", id, "owners" + id + "@example.com");
        return id;
    }

    private long createApartment(long ownerUserId) {
        long address = next("ADDRESS_SEQ");
        long id = next("APARTMENT_SEQ");
        jdbc.update("INSERT INTO address(address_id,street_name,building_number,zip_code,city_name,voivode_ship,insert_date,insert_operator) VALUES (?,'Jasna','1','60-101','Poznań','Wielkopolskie',now(),'test')", address);
        jdbc.update("INSERT INTO apartment(apartment_id,apartment_name,leases_number,area,is_rented,pinned,address_id,user_account_id,insert_date,insert_operator) VALUES (?,'Testowa nieruchomość',1,40,true,true,?,?,now(),'test')", id, address, ownerUserId);
        return id;
    }

    private long next(String sequence) {
        return jdbc.queryForObject("SELECT nextval('" + sequence + "')", Long.class);
    }

    private void login(long id) {
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(id, "test@example.com", Role.ADMIN));
    }
}
