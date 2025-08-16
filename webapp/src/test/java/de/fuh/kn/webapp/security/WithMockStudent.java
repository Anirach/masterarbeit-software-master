package de.fuh.kn.webapp.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockStudentSecurityContextFactory.class)
public @interface WithMockStudent {
    String email() default "test@student.de";
    String vorname() default "Test";
    String nachname() default "Student";
}
