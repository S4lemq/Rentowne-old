package pl.rentowne.meter_reading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.meter.model.MeterType;
import pl.rentowne.meter_reading.model.MeterReading;
import pl.rentowne.meter_reading.model.dto.MeterReadingDto;
import pl.rentowne.meter_reading.model.dto.PrevMeterReadingDto;
import pl.rentowne.meter_reading.repository.custom.MeterReadingRepositoryCustom;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Repozytorium odczytów stanu licznika
 */
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long>, MeterReadingRepositoryCustom {

    @Modifying
    @Query("update MeterReading m set m.settled = false, m.settlement.id = null where m.settlement.id = :id")
    int updateSettledToFalseBySettlementId(Long id);

    @Query("""
            select new pl.rentowne.meter_reading.model.dto.PrevMeterReadingDto(
                mr.id, mr.currentReading, mr.readingDate,
                concat(t.firstname, ' ', t.lastname), ro.rentedObjectName, m.meterType)
            from MeterReading mr
            join mr.meter m
            left join m.rentedObject ro
            left join ro.tenant t on t.active = true
            where m.id = :meterId
            order by mr.readingDate desc
            """)
    List<PrevMeterReadingDto> findPrevMeterReading(@Param("meterId") Long meterId);


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

    // Pobieranie odczytów z datami dla danego RO i typu licznika (nierozliczone)
    @Query("""
       select new pl.rentowne.meter_reading.model.dto.MeterReadingDto(mr.id, mr.consumption, mr.readingDate)
       from MeterReading mr
       join mr.meter m
       where m.rentedObject.id = :rentedObjectId
         and m.meterType = :meterType
         and mr.settled = false
       order by mr.readingDate asc
       """)
    List<MeterReadingDto> findReadingsWithDatesByRentedObjectAndMeterType(@Param("rentedObjectId") Long rentedObjectId,
                                                                           @Param("meterType") MeterType meterType);

    // Pobieranie dwóch ostatnich odczytów z datami dla danego RO i typu licznika (bez względu na settled)
    @Query("""
       select new pl.rentowne.meter_reading.model.dto.MeterReadingDto(mr.id, mr.consumption, mr.readingDate)
       from MeterReading mr
       join mr.meter m
       where m.rentedObject.id = :rentedObjectId
         and m.meterType = :meterType
       order by mr.readingDate desc
       limit 2
       """)
    List<MeterReadingDto> findLastTwoReadingsWithDatesByRentedObjectAndMeterType(@Param("rentedObjectId") Long rentedObjectId,
                                                                                 @Param("meterType") MeterType meterType);

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
    int markApartmentSettled(@Param("ids") Set<Long> ids);


    // Cofnięcie apartmentSettled dla WSZYSTKICH odczytów w cyklu (po typie licznika – elektryka)
    @Modifying
    @Query("""
           update MeterReading m
              set m.apartmentSettled = false
            where m.settlement.id in :settlementIds
              and m.meter.meterType = :type
           """)
    int unsetApartmentSettledBySettlementIdsAndType(@Param("settlementIds") List<Long> settlementIds,
                                                    @Param("type") MeterType type);

    // Pobiera ID nierozliczonych odczytów dla danego obiektu i typu licznika
    @Query("""
           select mr.id
           from MeterReading mr
           join mr.meter m
           where m.rentedObject.id = :rentedObjectId
             and m.meterType = :type
             and mr.settled = false
           """)
    List<Long> findUnsettledReadingIdsByRentedObjectAndType(@Param("rentedObjectId") Long rentedObjectId,
                                                             @Param("type") MeterType type);

    // Pobiera nierozliczone odczyty z licznika mieszkania (distributeReadings = true)
    @Query("""
           select new pl.rentowne.meter_reading.model.dto.MeterReadingDto(mr.id, mr.consumption, mr.readingDate)
           from MeterReading mr
           join mr.meter m
           where m.apartment.id = :apartmentId
             and m.meterType = :meterType
             and m.distributeReadings = true
             and mr.settled = false
           order by mr.readingDate asc
           """)
    List<MeterReadingDto> findUnsettledDistributedReadingsByApartmentAndType(@Param("apartmentId") Long apartmentId,
                                                                              @Param("meterType") MeterType meterType);


    @Modifying
    @Query("""
        UPDATE MeterReading mr
        SET mr.settled = true, mr.apartmentSettled = true, mr.meterReadingSettlementIds = :notDistributeReadings
        WHERE mr.id IN :distributeReadings
    """)
    void markDistributedReadingsSettled(List<Long> distributeReadings, Set<Long> notDistributeReadings);


    @Query("""
        select mr.id
        from MeterReading mr
        where mr.settlement.id in :cycleSettlementIds
    """)
    List<Long> findMeterReadings(List<Long> cycleSettlementIds);

    List<MeterReading> findAllBySettledTrueAndApartmentSettledTrueAndMeter_DistributeReadingsTrue();

    @Modifying
    @Query("""
           update MeterReading m
              set m.apartmentSettled = false, m.settled = false, m.meterReadingSettlementIds = null
            where m.id in :meterReadingIds
    """)
    void unsetDistributedReadingsSettled(List<Long> meterReadingIds);
}
