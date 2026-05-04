package pl.rentowne.meter_reading;

import lombok.Builder;
import pl.rentowne.meter.Meter;
import pl.rentowne.meter.MeterDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record MeterReadingDto(
        Long id,
        BigDecimal currentReading,
        LocalDateTime readingDate,
        BigDecimal consumption,
        MeterDto meterDto,
        BigDecimal previousReading,
        LocalDateTime previousReadingDate,
        String meterType
) {

    public MeterReadingDto(Long id, BigDecimal consumption) {
        this(id, null, null, consumption, null, null, null, null);
    }

    public MeterReadingDto(Long id, BigDecimal consumption, LocalDateTime readingDate) {
        this(id, null, readingDate, consumption, null, null, null, null);
    }

    public MeterReadingDto(BigDecimal currentReading, MeterDto meterDto, LocalDateTime readingDate) {
        this(null, currentReading, readingDate, null, meterDto, null, null, null);
    }

    public static MeterReading asEntity(MeterReadingDto dto) {
        BigDecimal previousReading = dto.previousReading() != null ?
                dto.previousReading() : BigDecimal.ZERO;
        BigDecimal currentReading = dto.currentReading() != null ?
                dto.currentReading() : BigDecimal.ZERO;

        return MeterReading.builder()
                .id(dto.id())
                .currentReading(currentReading)
                .readingDate(dto.readingDate())
                .consumption(currentReading.subtract(previousReading))
                .meter(Meter.builder()
                        .id(dto.meterDto().getId())
                        .build())
                .build();
    }

}
