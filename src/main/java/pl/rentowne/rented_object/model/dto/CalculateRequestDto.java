package pl.rentowne.rented_object.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CalculateRequestDto {
    private boolean waterIncluded;
    private boolean electricityIncluded;
    private LocalDateTime settlementDate;
}
