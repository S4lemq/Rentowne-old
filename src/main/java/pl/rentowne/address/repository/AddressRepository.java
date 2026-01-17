package pl.rentowne.address.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.rentowne.address.model.Address;

/**
 * repozytorium adresów
 */
public interface AddressRepository extends JpaRepository<Address, Long> {
}
