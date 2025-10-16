package pl.rentowne.invoice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.invoice.model.Invoice;

import java.util.List;

/**
 * Repozytorium faktur
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("select i from Invoice i where i.rentedObject.id = :rentedObjectId")
    List<Invoice> findByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

    @Query("select i from Invoice i where i.housingProvider.id = :housingProviderId")
    List<Invoice> findByHousingProviderId(@Param("housingProviderId") Long housingProviderId);

    @Query("select i from Invoice i where i.paid = :paid")
    List<Invoice> findByPaidStatus(@Param("paid") boolean paid);

    @Query("""
            select i from Invoice i
            left join fetch i.housingProvider hp
            left join fetch i.rentedObject ro
            where i.id = :id
            """)
    Invoice findByIdWithRelations(@Param("id") Long id);

    @Query("""
            select i from Invoice i
            left join fetch i.housingProvider hp
            left join fetch i.rentedObject ro
            """)
    Page<Invoice> findAllWithRelations(Pageable pageable);
}
