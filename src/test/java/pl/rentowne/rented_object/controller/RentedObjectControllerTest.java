package pl.rentowne.rented_object.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.rentowne.IntegrationTest;
import pl.rentowne.TestSecurityConfig;
import pl.rentowne.WithMockRentowneUser;
import pl.rentowne.settlement.CalculateRequestDto;
import pl.rentowne.settlement.Settlement;
import pl.rentowne.settlement.SettlementRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@IntegrationTest
@Import(TestSecurityConfig.class)
class RentedObjectControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuditorAware<String> auditorAware;

    @Autowired
    private SettlementRepository settlementRepository;

    @Test
    @WithMockRentowneUser
    @Sql("classpath:sql/init_data.sql")
    void shouldCalculateRentedObjectTwiceAndSaveTwoRows() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        var dto = CalculateRequestDto.builder()
                .electricityIncluded(true)
                .waterIncluded(true)
                .settlementDate(LocalDateTime.of(2025, 10, 3, 19, 0, 0))
                .build();

        // when
        webTestClient.post()
                .uri("/api/rented-objects/1/calculate")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/api/rented-objects/2/calculate")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk();

        // then
        var settlements = settlementRepository.findAll();

        BigDecimal sum = settlements.stream()
                .map(Settlement::getElectricityAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(BigDecimal.valueOf(118.41), sum, "Suma za energię elektryczną powinna wynosić 118.41");
    }

    @Test
    @WithMockRentowneUser
    @Sql("classpath:sql/init_data.sql")
    void shouldCalculateRentedObjectThreeTimesAndSaveThreeRows() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        var dto = CalculateRequestDto.builder()
                .electricityIncluded(true)
                .waterIncluded(true)
                .settlementDate(LocalDateTime.of(2025, 10, 3, 19, 0, 0))
                .build();

        // when
        webTestClient.post()
                .uri("/api/rented-objects/6/calculate")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/api/rented-objects/7/calculate")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/api/rented-objects/8/calculate")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk();

        // then
        var settlements = settlementRepository.findAll();

        BigDecimal sum = settlements.stream()
                .map(Settlement::getElectricityAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("305.90"), sum, "Suma za energię elektryczną powinna wynosić 305.90");

//        BigDecimal expectedAmountPerSettlement = BigDecimal.valueOf(39.47);
//        settlements.forEach(settlement ->
//                assertEquals(expectedAmountPerSettlement, settlement.getElectricityAmount(),
//                        "Każde rozliczenie powinno mieć równą wartość energii elektrycznej")
//        );

    }

}
