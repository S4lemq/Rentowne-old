package pl.rentowne.meter_reading;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.meter.Meter;
import pl.rentowne.meter.MeterRepository;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.util.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;

    @Transactional
    @Override
    public void addMeterReading(AddMeterReadingRequest dto) throws RentowneBusinessException {
        // Lock the meter before reading its latest state so concurrent additions use a fresh baseline.
        Meter meter = meterRepository.lockById(dto.meterId())
                .orElseThrow(() -> new RentowneNotFoundException(dto.meterId()));
        MeterReading previous = meterReadingRepository.getMeterReadingByMeter(meter.getId());
        BigDecimal previousValue = previous != null ? previous.getCurrentReading() : meter.getInitialMeterReading();
        LocalDateTime previousDate = previous != null ? previous.getReadingDate() : meter.getInstallationDate();

        if (previousValue != null && dto.currentReading().compareTo(previousValue) <= 0) {
            throw new RentowneBusinessException(RentowneErrorCode.BAD_METER_READING_VALUES);
        }
        if (previousDate != null) {
            DateUtils.compareDates(dto.readingDate(), previousDate, false, meter.getMeterType());
        }

        BigDecimal baseline = previousValue != null ? previousValue : BigDecimal.ZERO;
        meterReadingRepository.save(MeterReading.builder()
                .meter(meter)
                .currentReading(dto.currentReading())
                .readingDate(dto.readingDate())
                .consumption(dto.currentReading().subtract(baseline))
                .build());
    }

    @Override
    public MeterReadingDto getCurrentAndPreviousReading(Long meterId, Long id) {

        MeterReading currentReading = meterReadingRepository.getMeterReadingById(id);
        MeterReading previousReading = meterReadingRepository.getPreviousMeterReading(currentReading.getReadingDate(), meterId);

        return MeterReadingDto.builder()
                .id(currentReading.getId())
                .currentReading(currentReading.getCurrentReading())
                .readingDate(currentReading.getReadingDate())
                .consumption(currentReading.getCurrentReading().subtract(previousReading.getCurrentReading()))
                .previousReading(previousReading.getCurrentReading())
                .previousReadingDate(previousReading.getReadingDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PrevMeterReadingDto getMeterReadingByMeter(Long meterId) {
        return meterReadingRepository.findPrevMeterReading(meterId).stream().findFirst().orElse(null);
    }

    @Transactional
    @Override
    public void deleteMeterReading(Long id) throws RentowneBusinessException {
        if (!meterReadingRepository.existsById(id)) {
            throw new RentowneBusinessException(RentowneErrorCode.METER_READING_NOT_FOUND);
        }

        MeterReading meterReading = meterReadingRepository.findById(id)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.METER_READING_NOT_FOUND));

        if (meterReading.isSettled() && meterReading.isApartmentSettled()) {
            throw new RentowneBusinessException(RentowneErrorCode.METER_READING_ALREADY_SETTLED);
        }

        meterReadingRepository.deleteById(id);
    }

}
