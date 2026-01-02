package pl.rentowne.pdf_template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO do szablonu PDF z listą mieszkań.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentListTemplateDto {
    private String title;
    private LocalDate generatedDate;
    private String generatedBy;
    private List<ApartmentItemDto> apartments;
    private SummaryDto summary;

    /**
     * DTO pojedynczego mieszkania w liście.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApartmentItemDto {
        private Integer ordinalNumber;
        private String apartmentName;
        private String address;
        private BigDecimal area;
        private Integer leasesCount;
        private String status;
        private String imageName;
    }

    /**
     * DTO podsumowania listy mieszkań.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDto {
        private Integer totalApartments;
        private Integer rentedApartments;
        private Integer availableApartments;
        private BigDecimal totalArea;
        private BigDecimal averageArea;
    }
}

