package pl.rentowne.rented_object;

import lombok.Builder;

@Builder
record ProrationCheckResponseDto(
    boolean shouldSuggestProration,
    String message,
    String contractStartDate,
    String settlementDate
) {}
