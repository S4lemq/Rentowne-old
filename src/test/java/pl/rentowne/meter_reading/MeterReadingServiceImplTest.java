package pl.rentowne.meter_reading;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.meter.Meter;
import pl.rentowne.meter.MeterRepository;
import pl.rentowne.meter.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingServiceImplTest {
    @Mock MeterReadingRepository readings;
    @Mock MeterRepository meters;
    @InjectMocks MeterReadingServiceImpl service;

    private final LocalDateTime previousDate = LocalDateTime.of(2026, 9, 30, 0, 0);
    private Meter meter;

    @BeforeEach
    void meter() {
        meter = Meter.builder().id(1L).meterType(MeterType.WATER_COLD).build();
        when(meters.lockById(1L)).thenReturn(Optional.of(meter));
    }

    @Test
    void shouldCalculateConsumptionFromLatestDatabaseReading() {
        previous("125.12345");
        service.addMeterReading(request("150.54321", previousDate.plusMonths(1)));

        var captured = ArgumentCaptor.forClass(MeterReading.class);
        verify(readings).save(captured.capture());
        assertThat(captured.getValue().getId()).isNull();
        assertThat(captured.getValue().getMeter()).isSameAs(meter);
        assertThat(captured.getValue().getConsumption()).isEqualByComparingTo("25.41976");
    }

    @Test
    void shouldRejectValueNotGreaterThanLatestDatabaseReading() {
        previous("150");
        assertThatThrownBy(() -> service.addMeterReading(request("140", previousDate.plusMonths(1))))
                .isInstanceOf(RentowneBusinessException.class);
        assertThatThrownBy(() -> service.addMeterReading(request("150", previousDate.plusMonths(1))))
                .isInstanceOf(RentowneBusinessException.class);
        verify(readings, never()).save(any());
    }

    @Test
    void shouldRejectDateNotAfterLatestDatabaseReading() {
        previous("100");
        assertThatThrownBy(() -> service.addMeterReading(request("150", previousDate)))
                .isInstanceOf(RentowneBusinessException.class);
        assertThatThrownBy(() -> service.addMeterReading(request("150", previousDate.minusDays(1))))
                .isInstanceOf(RentowneBusinessException.class);
        verify(readings, never()).save(any());
    }

    @Test
    void shouldUseInitialMeterStateWhenThereAreNoReadings() {
        meter.setInitialMeterReading(new BigDecimal("80"));
        meter.setInstallationDate(previousDate);
        service.addMeterReading(request("100", previousDate.plusDays(1)));

        var captured = ArgumentCaptor.forClass(MeterReading.class);
        verify(readings).save(captured.capture());
        assertThat(captured.getValue().getConsumption()).isEqualByComparingTo("20");
    }

    @Test
    void shouldUseZeroWhenNeitherHistoricalNorInitialReadingExists() {
        service.addMeterReading(request("100", previousDate));
        var captured = ArgumentCaptor.forClass(MeterReading.class);
        verify(readings).save(captured.capture());
        assertThat(captured.getValue().getConsumption()).isEqualByComparingTo("100");
    }

    @Test
    void shouldRejectUnknownMeter() {
        when(meters.lockById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addMeterReading(request("100", previousDate)))
                .isInstanceOf(RentowneNotFoundException.class);
        verify(readings, never()).save(any());
    }

    private void previous(String value) {
        when(readings.getMeterReadingByMeter(1L)).thenReturn(MeterReading.builder()
                .currentReading(new BigDecimal(value)).readingDate(previousDate).build());
    }

    private AddMeterReadingRequest request(String value, LocalDateTime date) {
        return new AddMeterReadingRequest(1L, new BigDecimal(value), date);
    }
}
