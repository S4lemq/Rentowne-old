package pl.rentowne.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.rentowne.user.Role;
import pl.rentowne.user.UserBasicDto;
import pl.rentowne.user.UserService;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pl.rentowne.analytics.AnalyticsDto.*;
import static pl.rentowne.analytics.FinancialAnalyticsRepository.*;

class ForecastServiceTest {
    private final FinancialAnalyticsRepository repository=mock(FinancialAnalyticsRepository.class);
    private final FinancialAnalyticsService analytics=mock(FinancialAnalyticsService.class);
    private final UserService users=mock(UserService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2027-01-15T12:00:00Z"),ZoneId.of("Europe/Warsaw"));
    private final ForecastService service=new ForecastService(repository,analytics,users,clock);
    private final List<SettlementMonth> records=new ArrayList<>();

    @BeforeEach void prepare() {
        when(users.getLoggedUser()).thenReturn(new UserBasicDto(1L,"test@example.com",Role.ADMIN));
        when(analytics.ownedApartments(1,null)).thenReturn(List.of(new Apartment(1,"A")));
        when(repository.leases(1,null)).thenReturn(List.of(new Lease(1,1,1,"A","Pokój","Najemca",LocalDate.of(2026,1,1),LocalDate.of(2027,2,14),null,new BigDecimal("1000"),new BigDecimal("600"))));
        for(int month=10;month<=12;month++) records.add(new SettlementMonth(1,1,1,LocalDate.of(2026,month,1),new BigDecimal("1000"),ZERO,new BigDecimal("600")));
        when(repository.settlementMonths(eq(1L),isNull(),any(),any())).thenReturn(records);
        when(repository.providers(1,null)).thenReturn(List.of(new Provider(1,1,"Dostawca",true,new BigDecimal("100"))));
        when(repository.invoiceMonths(eq(1L),isNull(),any(),any())).thenReturn(List.of());
        when(analytics.overview(any(),any(),isNull(),eq(Basis.ACCRUAL))).thenReturn(new Overview(LocalDate.now(clock),null,null,null,Basis.ACCRUAL,List.of(),null,List.of(),List.of(),List.of(),null));
    }

    @Test void proratesTheLastMonthAndDoesNotAutomaticallyRenew() {
        var result=service.forecast(3,null,false,ZERO);
        assertThat(result.months().getFirst().revenue()).isEqualByComparingTo("500.00");
        assertThat(result.months().getFirst().result()).isEqualByComparingTo("400.00");
        assertThat(result.months().get(1).revenue()).isZero();
        assertThat(result.months().get(1).result()).isEqualByComparingTo("-100.00");
        assertThat(service.forecast(3,null,true,ZERO).months().getFirst().revenue()).isEqualByComparingTo("1000.00");
    }

    @Test void recordedFutureEntriesReplaceEstimatesAndAreNotInflated() {
        records.add(new SettlementMonth(1,1,1,LocalDate.of(2027,2,1),new BigDecimal("1200"),ZERO,new BigDecimal("600")));
        when(repository.invoiceMonths(eq(1L),isNull(),any(),any())).thenReturn(List.of(new InvoiceMonth(1,1,LocalDate.of(2027,2,1),new BigDecimal("150"))));
        var month=service.forecast(3,null,true,new BigDecimal("10")).months().getFirst();
        assertThat(month.revenue()).isEqualByComparingTo("1200.00");
        assertThat(month.costs()).isEqualByComparingTo("150.00");
        assertThat(month.result()).isEqualByComparingTo("1050.00");
    }

    @Test void missingHistoryDoesNotBecomeAZeroCostOrCertainResult() {
        records.clear();
        var month=service.forecast(3,null,false,ZERO).months().getFirst();
        assertThat(month.complete()).isFalse();
        assertThat(month.result()).isNull();
        assertThat(month.afterTax()).isNull();
        assertThat(month.missingData()).isNotEmpty();
    }
}
