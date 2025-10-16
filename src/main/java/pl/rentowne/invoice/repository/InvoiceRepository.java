package pl.rentowne.invoice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.rentowne.invoice.model.Invoice;

/**
 * Repozytorium faktur
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("""
            select i from Invoice i
            left join fetch i.housingProvider hp
            left join fetch i.rentedObject ro
            """)
    Page<Invoice> findAllWithRelations(Pageable pageable);
}
