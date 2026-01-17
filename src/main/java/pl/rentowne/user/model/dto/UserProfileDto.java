package pl.rentowne.user.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.rentowne.user.model.PreferredLanguage;

@Getter
@AllArgsConstructor
public class UserProfileDto {
    private PreferredLanguage preferredLanguage;
    private String image;
}
