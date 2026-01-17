package pl.rentowne.user.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.rentowne.user.model.Role;

@Getter
@AllArgsConstructor
public class UserBasicDto {
    private Long id;
    private String email;
    private Role role;
}
