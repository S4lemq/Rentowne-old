package pl.rentowne.owner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import pl.rentowne.user.User;
import pl.rentowne.user.UserService;

import java.util.List;
import java.util.Objects;

@Service
@Validated
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnerService {
    private final OwnerRepository repository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<OwnerDto> getOwners() {
        return repository.findByUser_IdOrderByLastNameAscFirstNameAscIdAsc(userService.getLoggedUser().id())
                .stream().map(OwnerMapper::toDto).toList();
    }

    @Transactional
    public Owner save(@NotNull @Valid OwnerDto dto) {
        Long userId = userService.getLoggedUser().id();
        Owner owner;
        if (dto.id() == null) {
            owner = new Owner();
            owner.setUser(new User(userId));
        } else {
            owner = repository.findByIdAndUser_Id(dto.id(), userId)
                    .orElseThrow(() -> new OwnerException(HttpStatus.NOT_FOUND, "Nie znaleziono właściciela."));
            if (!Objects.equals(dto.version(), owner.getVersion())) {
                throw new OwnerException(HttpStatus.CONFLICT,
                        "Dane właściciela zostały zmienione. Odśwież nieruchomość przed zapisem.");
            }
        }
        OwnerMapper.update(owner, dto);
        return repository.saveAndFlush(owner);
    }
}
