package pl.rentowne.meter_reading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.rentowne.meter_reading.model.MeterReading;
import pl.rentowne.meter_reading.model.dto.PrevMeterReadingDto;
import pl.rentowne.meter_reading.repository.custom.MeterReadingRepositoryCustom;

import java.util.List;

/**
 * Repozytorium odczytów stanu licznika
 */
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long>, MeterReadingRepositoryCustom {

    @Modifying
    @Query("update MeterReading m set m.settled = false, m.settlement.id = null where m.settlement.id = :id")
    int updateSettledToFalseBySettlementId(Long id);

    @Query("select new pl.rentowne.meter_reading.model.dto.PrevMeterReadingDto(" +
            "mr.id, mr.currentReading, mr.readingDate, " +
            "concat(t.firstname, ' ', t.lastname), ro.rentedObjectName, m.meterType)" +
            "from MeterReading mr " +
            "join mr.meter m " +
            "join m.rentedObject ro " +
            "join ro.tenant t " +
            "where m.id = :meterId " +
            "order by mr.readingDate desc")
    List<PrevMeterReadingDto> findPrevMeterReading(Long meterId);
}
