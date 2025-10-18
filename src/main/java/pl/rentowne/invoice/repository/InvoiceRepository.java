package pl.rentowne.invoice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.invoice.model.Invoice;
import pl.rentowne.invoice.model.dto.InvoiceFilterDto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Repozytorium faktur
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query(
            value = """
        select i from Invoice i
        left join fetch i.housingProvider hp
        left join fetch i.apartment a
        where (:#{#invoiceFilterDto.housingProviderId} is null or hp.id = :#{#invoiceFilterDto.housingProviderId})
          and (:#{#invoiceFilterDto.apartmentId} is null or a.id = :#{#invoiceFilterDto.apartmentId})
          and (:#{#invoiceFilterDto.month} is null or cast(i.month as string) = :#{#invoiceFilterDto.month})
          and (:#{#invoiceFilterDto.payDay} is null or cast(i.payDay as string) = :#{#invoiceFilterDto.payDay})
          and (:#{#invoiceFilterDto.payDayMonth} is null or cast(i.payDayMonth as string) = :#{#invoiceFilterDto.payDayMonth})
          and (:#{#invoiceFilterDto.category} is null or hp.type = :#{#invoiceFilterDto.category})
        """,
            countQuery = """
            select count(distinct i) from Invoice i
        left join i.housingProvider hp
        left join i.apartment a
        where (:#{#invoiceFilterDto.housingProviderId} is null or hp.id = :#{#invoiceFilterDto.housingProviderId})
          and (:#{#invoiceFilterDto.apartmentId} is null or a.id = :#{#invoiceFilterDto.apartmentId})
          and (:#{#invoiceFilterDto.month} is null or cast(i.month as string) = :#{#invoiceFilterDto.month})
          and (:#{#invoiceFilterDto.payDay} is null or cast(i.payDay as string) = :#{#invoiceFilterDto.payDay})
          and (:#{#invoiceFilterDto.payDayMonth} is null or cast(i.payDayMonth as string) = :#{#invoiceFilterDto.payDayMonth})
          and (:#{#invoiceFilterDto.category} is null or hp.type = :#{#invoiceFilterDto.category})
    """
    )
    Page<Invoice> findAllWithRelations(Pageable pageable, @Param("invoiceFilterDto") InvoiceFilterDto dto);


    /**
     * Faktury zapłacone w danym miesiącu (według paymentDate)
     */
    @Query("""
            select i from Invoice i
            left join fetch i.housingProvider hp
            left join fetch i.apartment a
            where year(i.paymentDate) = :year
            and month(i.paymentDate) = :month
            """)
    Page<Invoice> findPaidInMonth(@Param("year") int year, @Param("month") int month, Pageable pageable);

    /**
     * Zbliżające się płatności (aktualna data <= payDay i brak paymentDate)
     */
    @Query("""
            select i from Invoice i
            left join fetch i.housingProvider hp
            left join fetch i.apartment a
            where :currentDate <= i.payDay
            and i.paymentDate is null
            """)
    Page<Invoice> findUpcomingPayments(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    /**
     * Przeterminowane płatności (aktualna data > payDay i brak paymentDate)
     */
    @Query("""
            select i from Invoice i
            left join fetch i.housingProvider hp
            left join fetch i.apartment a
            where :currentDate > i.payDay
            and i.paymentDate is null
            """)
    Page<Invoice> findOverduePayments(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    /**
     * Do rozliczenia w danym miesiącu (według payDayMonth)
     */
    @Query("""
            select i from Invoice i
            left join fetch i.housingProvider hp
            left join fetch i.apartment a
            where year(i.payDayMonth) = :year
            and month(i.payDayMonth) = :month
            """)
    Page<Invoice> findToSettleInMonth(@Param("year") int year, @Param("month") int month, Pageable pageable);

    /**
     * Suma zapłaconych faktur w danym miesiącu (według daty przelewu)
     */
    @Query("""
            select coalesce(sum(i.amount), 0)
            from Invoice i
            where year(i.paymentDate) = :year
            and month(i.paymentDate) = :month
            """)
    BigDecimal sumPaidInMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Suma zbliżających się płatności (aktualna data <= payDay i brak daty przelewu)
     */
    @Query("""
            select coalesce(sum(i.amount), 0)
            from Invoice i
            where :currentDate <= i.payDay
            and i.paymentDate is null
            """)
    BigDecimal sumUpcomingPayments(@Param("currentDate") LocalDate currentDate);

    /**
     * Suma przeterminowanych płatności (aktualna data > payDay i brak daty przelewu)
     */
    @Query("""
            select coalesce(sum(i.amount), 0)
            from Invoice i
            where :currentDate > i.payDay
            and i.paymentDate is null
            """)
    BigDecimal sumOverduePayments(@Param("currentDate") LocalDate currentDate);

    /**
     * Suma do rozliczenia w danym miesiącu (według payDayMonth)
     */
    @Query("""
            select coalesce(sum(i.amount), 0)
            from Invoice i
            where year(i.payDayMonth) = :year
            and month(i.payDayMonth) = :month
            """)
    BigDecimal sumToSettleInMonth(@Param("year") int year, @Param("month") int month);
}
