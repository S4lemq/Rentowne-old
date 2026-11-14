package pl.rentowne.meter_reading;

import pl.rentowne.exception.RentowneBusinessException;

interface MeterReadingService {
    /**
     * Dodaje odczyt licznika
     * @param dto odczytu licznika
     * @throws RentowneBusinessException wyjątek biznesowy
     */
    void addMeterReading(AddMeterReadingRequest dto) throws RentowneBusinessException;

    /**
     * Pobiera aktualny i poprzedni stan licznika
     * @param meterId id licznika
     * @param id id odczytu licznika
     * @return odczyt licznika
     */
    MeterReadingDto getCurrentAndPreviousReading(Long meterId, Long id);

    /**
     * Pobiera odczyt licznika po id licznika
     * @param meterId id licznika
     * @return odczyt licznika
     */
    PrevMeterReadingDto getMeterReadingByMeter(Long meterId);

    /**
     * Usuwa odczyt licznika po id
     * @param id id odczytu licznika
     * @throws RentowneBusinessException wyjątek biznesowy
     */
    void deleteMeterReading(Long id) throws RentowneBusinessException;
}
