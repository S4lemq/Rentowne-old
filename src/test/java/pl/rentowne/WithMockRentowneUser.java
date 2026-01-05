package pl.rentowne;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockRentowneUserSecurityContextFactory.class)
public @interface WithMockRentowneUser {
    String username() default "peter.lubowicz@gmail.com";
    String[] roles() default {"ADMIN"};
    long userId() default 1L;
}
