package pl.rentowne.meter_reading.model.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.rentowne.meter.model.Meter;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.meter_reading.model.MeterReading;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeterReadingDto {
    private Long id;
    private BigDecimal currentReading;
    private LocalDateTime readingDate;
    private BigDecimal consumption;
    private MeterDto meterDto;
    private BigDecimal previousReading;
    private LocalDateTime previousReadingDate;
    private String meterType;

    @QueryProjection
    public MeterReadingDto(Long id, BigDecimal consumption) {
        this.id = id;
        this.consumption = consumption;
    }

    public MeterReadingDto(Long id, BigDecimal consumption, LocalDateTime readingDate) {
        this.id = id;
        this.consumption = consumption;
        this.readingDate = readingDate;
    }

    @QueryProjection
    public MeterReadingDto(BigDecimal currentReading, MeterDto meterDto, LocalDateTime readingDate) {
        this.currentReading = currentReading;
        this.meterDto = meterDto;
        this.readingDate = readingDate;
    }

    public static MeterReading asEntity(MeterReadingDto dto) {
        BigDecimal previousReading = dto.getPreviousReading() != null ?
                dto.getPreviousReading() : BigDecimal.ZERO;
        BigDecimal currentReading = dto.getCurrentReading() != null ?
                dto.getCurrentReading() : BigDecimal.ZERO;

        return MeterReading.builder()
                .id(dto.getId())
                .currentReading(currentReading)
                .readingDate(dto.getReadingDate())
                .consumption(currentReading.subtract(previousReading))
                .meter(Meter.builder()
                        .id(dto.getMeterDto().getId())
                        .build())
                .build();
    }

}
