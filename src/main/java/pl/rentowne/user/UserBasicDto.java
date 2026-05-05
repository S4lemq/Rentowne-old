package pl.rentowne.user;

public record UserBasicDto(
        Long id,
        String email,
        Role role
) {
}
