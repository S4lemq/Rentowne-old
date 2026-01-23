package pl.rentowne.security.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    @Schema(description = "email", example = "karedon00@gmail.com", required = true)
    private String email;
    @Schema(description = "hasło", example = "m1u0r5MK@", required = true)
    private String password;
}