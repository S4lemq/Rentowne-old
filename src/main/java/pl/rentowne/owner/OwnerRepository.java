package pl.rentowne.owner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface OwnerRepository extends JpaRepository<Owner, Long> {
    List<Owner> findByUser_IdOrderByLastNameAscFirstNameAscIdAsc(Long userId);

    Optional<Owner> findByIdAndUser_Id(Long id, Long userId);
}
