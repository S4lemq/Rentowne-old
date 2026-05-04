package pl.rentowne.meter_reading;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.meter.MeterType;
import pl.rentowne.util.DateUtils;

@Service
@RequiredArgsConstructor
class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;

    @Transactional
    @Override
    public void addMeterReading(MeterReadingDto dto) throws RentowneBusinessException {
        if (dto.previousReading() != null && dto.previousReadingDate() != null) {
            if (dto.currentReading().compareTo(dto.previousReading()) <= 0) {
                throw new RentowneBusinessException(RentowneErrorCode.BAD_METER_READING_VALUES);
            }
            DateUtils.compareDates(dto.readingDate(), dto.previousReadingDate(), false, MeterType.fromString(dto.meterType()));
        }

        meterReadingRepository.save(MeterReadingDto.asEntity(dto));
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
