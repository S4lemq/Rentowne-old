package pl.rentowne.meter_reading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.meter.model.MeterType;
import pl.rentowne.meter_reading.model.MeterReading;
import pl.rentowne.meter_reading.model.dto.PrevMeterReadingDto;
import pl.rentowne.meter_reading.repository.custom.MeterReadingRepositoryCustom;

import java.math.BigDecimal;
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


    // IDs odczytów w mieszkaniu (dla danego typu licznika), które NIE są jeszcze zamknięte na poziomie mieszkania
    @Query("""
       select mr.id
       from MeterReading mr
       join mr.meter m
       where m.rentedObject.apartment.id = :apartmentId
         and m.meterType = :type
         and mr.apartmentSettled = false
       """)
    List<Long> findUnclosedReadingIdsInApartment(@Param("apartmentId") Long apartmentId,
                                                 @Param("type") MeterType type);

    // Suma zużycia dla RO, tylko z odczytów niezamkniętych na poziomie mieszkania (ignoruje settled)
    @Query("""
       select coalesce(sum(mr.consumption), 0)
       from MeterReading mr
       join mr.meter m
       where m.rentedObject.id = :roId
         and m.meterType = :type
         and mr.apartmentSettled = false
       """)
    BigDecimal sumConsumptionUnclosedForRentedObject(@Param("roId") Long roId,
                                                     @Param("type") MeterType type);

    // Czy zostały jeszcze ODSETTLOWANE kawalerki w mieszkaniu? (jeśli >0, to nie jesteśmy "ostatnim")
    @Query("""
       select count(mr)
       from MeterReading mr
       join mr.meter m
       where m.rentedObject.apartment.id = :apartmentId
         and m.meterType = :type
         and mr.apartmentSettled = false
         and mr.settled = false
       """)
    long countUnclosedAndUnsettledInApartment(@Param("apartmentId") Long apartmentId,
                                              @Param("type") MeterType type);

    // Ustawienie apartmentSettled = true dla listy ID (zamyka cykl na poziomie mieszkania)
    @Modifying
    @Query("update MeterReading mr set mr.apartmentSettled = true where mr.id in :ids")
    int markApartmentSettled(@Param("ids") List<Long> ids);

}
