package pl.rentowne.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pl.rentowne.user.Role;
import pl.rentowne.user.UserBasicDto;
import pl.rentowne.user.UserService;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pl.rentowne.analytics.AnalyticsDto.*;
import static pl.rentowne.analytics.FinancialAnalyticsRepository.*;

class FinancialAnalyticsServiceTest {
    private final FinancialAnalyticsRepository repository=mock(FinancialAnalyticsRepository.class);
    private final UserService users=mock(UserService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2027-02-15T12:00:00Z"),ZoneId.of("Europe/Warsaw"));
    private final FinancialAnalyticsService service=new FinancialAnalyticsService(repository,users,clock);

    @Test void keepsTheYearBoundaryMissingMonthsAndPropertyTotalsConsistent() {
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(7L,"owner@example.com",Role.ADMIN));
        when(repository.apartments(7)).thenReturn(List.of(new Apartment(1,"A"),new Apartment(2,"B")));
        when(repository.entries(eq(7L),isNull(),eq(Basis.ACCRUAL),any(),any())).thenReturn(List.of(
                new Entry(1,LocalDate.of(2026,12,1),"REVENUE",new BigDecimal("1000"),new BigDecimal("600")),
                new Entry(1,LocalDate.of(2026,12,1),"WATER",new BigDecimal("100"),BigDecimal.ZERO),
                new Entry(2,LocalDate.of(2026,12,1),"REVENUE",new BigDecimal("200"),new BigDecimal("100"))));
        var report=service.overview(LocalDate.of(2026,12,1),LocalDate.of(2027,2,1),null,Basis.ACCRUAL);
        assertThat(report.months()).hasSize(3);
        assertThat(report.months().get(1).hasRecords()).isFalse();
        assertThat(report.months().get(2).inProgress()).isTrue();
        assertThat(report.summary().result()).isEqualByComparingTo("1100.00");
        assertThat(report.summary().estimatedTax()).isEqualByComparingTo("59.50");
        assertThat(report.months().get(2).cumulativeResult()).isEqualByComparingTo("1100.00");
        assertThat(report.costs().getFirst().key()).isEqualTo("WATER");
        assertThat(FinancialAnalyticsService.sum(report.properties().stream().map(Property::totals).toList(),Basis.ACCRUAL)).isEqualTo(report.summary());
    }

    @Test void rejectsAnotherOwnersPropertyBeforeLoadingFinancialData() {
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(7L,"owner@example.com",Role.ADMIN));
        when(repository.apartments(7)).thenReturn(List.of(new Apartment(1,"A")));
        assertThatThrownBy(()->service.overview(null,null,99L,Basis.CASH)).isInstanceOf(ResponseStatusException.class);
        verify(repository,never()).entries(anyLong(),any(),any(),any(),any());
    }

    @Test void cashModeHasNoFictitiousTaxAndCapsReceiptsAtToday() {
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(7L,"owner@example.com",Role.ADMIN));
        when(repository.apartments(7)).thenReturn(List.of());
        var report=service.overview(LocalDate.of(2027,1,1),null,null,Basis.CASH);
        assertThat(report.summary().estimatedTax()).isNull();
        assertThat(report.summary().margin()).isNull();
        verify(repository).entries(7L,null,Basis.CASH,LocalDate.of(2026,1,1),LocalDate.of(2027,2,16));
    }
}
