package pl.rentowne.user;

public record UserProfileDto(
        PreferredLanguage preferredLanguage,
        String image
) {
}
