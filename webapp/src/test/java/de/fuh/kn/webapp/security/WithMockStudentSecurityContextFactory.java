package de.fuh.kn.webapp.security;

import de.fuh.kn.webapp.nutzerverwaltung.auth.StudentUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockStudentSecurityContextFactory implements WithSecurityContextFactory<WithMockStudent> {

    @Override
    public SecurityContext createSecurityContext(WithMockStudent annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        // Create a Kursbetreuer
        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setEmail(annotation.email());
        student.setVorname(annotation.vorname());
        student.setNachname(annotation.nachname());

        // Create the UserDetails object
        StudentUserDetails userDetails = new StudentUserDetails(student, "password");
        
        // Create the authentication token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        context.setAuthentication(authentication);
        return context;
    }
}
