package de.fuh.kn.webapp.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockKursbetreuerSecurityContextFactory.class)
public @interface WithMockKursbetreuer {
    String email() default "test@kursbetreuer.de";
    String vorname() default "Test";
    String nachname() default "Kursbetreuer";
}
