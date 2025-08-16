package de.fuh.kn.webapp.security;

import de.fuh.kn.webapp.nutzerverwaltung.auth.KursbetreuerUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockKursbetreuerSecurityContextFactory implements WithSecurityContextFactory<WithMockKursbetreuer> {

    @Override
    public SecurityContext createSecurityContext(WithMockKursbetreuer annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        // Create a Kursbetreuer
        KursbetreuerDTO kursbetreuer = new KursbetreuerDTO();
        kursbetreuer.setId(1L);
        kursbetreuer.setEmail(annotation.email());
        kursbetreuer.setVorname(annotation.vorname());
        kursbetreuer.setNachname(annotation.nachname());

        
        // Create the UserDetails object
        KursbetreuerUserDetails userDetails = new KursbetreuerUserDetails(kursbetreuer, "password");
        
        // Create the authentication token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        context.setAuthentication(authentication);
        return context;
    }
}
