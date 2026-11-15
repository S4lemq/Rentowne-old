package pl.rentowne.rental_document;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import pl.rentowne.owner.Owner;

import java.util.Optional;

/**
 * Serializes counter initialization as well as increments, including the first document of a year.
 */
interface DocumentOwnerRepository extends Repository<Owner, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Owner o where o.id=:id")
    Optional<Owner> lockById(@Param("id") Long id);
}
