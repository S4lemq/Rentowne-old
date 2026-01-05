package pl.rentowne;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WithMockRentowneUserSecurityContextFactory implements WithSecurityContextFactory<WithMockRentowneUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockRentowneUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        var authorities = Arrays.stream(annotation.roles())
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        var principal = new UserPrincipal(annotation.userId(), annotation.username(), authorities);

        var auth = new UsernamePasswordAuthenticationToken(
                principal, "password", authorities);

        context.setAuthentication(auth);
        return context;
    }
}
