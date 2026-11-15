package pl.rentowne.rental_document;

import org.springframework.data.jpa.repository.JpaRepository;

interface RentalDocumentCounterRepository
        extends JpaRepository<RentalDocumentCounter, RentalDocumentCounter.Key> {}
