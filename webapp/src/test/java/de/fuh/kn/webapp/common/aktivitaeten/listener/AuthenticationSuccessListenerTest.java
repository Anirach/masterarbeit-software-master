package de.fuh.kn.webapp.common.aktivitaeten.listener;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationSuccessListenerTest {

    @Mock
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    @Mock
    private NutzerService nutzerService;

    @Mock
    private WebAuthenticationDetails webAuthenticationDetails;

    private AuthenticationSuccessListener authenticationSuccessListener;

    @BeforeEach
    void setUp() {
        authenticationSuccessListener = new AuthenticationSuccessListener(nutzerService, aktivitaetsProtokollierungService);
    }

    @Test
    void onAuthenticationSuccess_shouldLogSuccessfulLoginForExistingUser() {
        // Arrange
        String username = "testuser@example.com";
        String remoteAddress = "127.0.0.1";
        String sessionId = "testSessionId";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        UserDetails userDetails = new User(username, "password", authorities);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "password", authorities);
        
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn(remoteAddress);
        when(webAuthenticationDetails.getSessionId()).thenReturn(sessionId);
        authentication.setDetails(webAuthenticationDetails);
        
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        NutzerDTO mockNutzer = new NutzerDTO();
        mockNutzer.setVorname("Test");
        mockNutzer.setNachname("User");
        mockNutzer.setEmail(username);
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        // Act
        authenticationSuccessListener.onAuthenticationSuccess(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(mockNutzer),
                eq(AktivitaetsTyp.LOGIN),
                eq("Anmeldung als Test User"),
                detailsCaptor.capture(),
                eq(true),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(remoteAddress, details.get("remoteAddress"));
        assertEquals(sessionId, details.get("sessionId"));
        assertEquals(mockNutzer.getClass().getSimpleName(), details.get("nutzerTyp"));
        assertEquals(authorities, details.get("authorities"));
    }
    
    @Test
    void onAuthenticationSuccess_shouldUseEmailAsIdentificationWhenNameNotAvailable() {
        // Arrange
        String username = "testuser@example.com";
        String remoteAddress = "127.0.0.1";
        String sessionId = "testSessionId";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        UserDetails userDetails = new User(username, "password", authorities);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "password", authorities);
        
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn(remoteAddress);
        when(webAuthenticationDetails.getSessionId()).thenReturn(sessionId);
        authentication.setDetails(webAuthenticationDetails);

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        NutzerDTO mockNutzer = new NutzerDTO();
        // No first name or last name set
        mockNutzer.setVorname(null);
        mockNutzer.setNachname(null);
        mockNutzer.setEmail(username);
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        // Act
        authenticationSuccessListener.onAuthenticationSuccess(event);
        
        // Assert
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(mockNutzer),
                eq(AktivitaetsTyp.LOGIN),
                eq("Anmeldung als " + username),
                any(),
                eq(true),
                isNull(),
                isNull()
        );
    }
    
    @Test
    void onAuthenticationSuccess_shouldHandleExceptions() {
        // Arrange
        String username = "testuser@example.com";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        UserDetails userDetails = new User(username, "password", authorities);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "password", authorities);
        authentication.setDetails(webAuthenticationDetails);

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        NutzerDTO mockNutzer = mock(NutzerDTO.class);
        when(mockNutzer.getEmail()).thenReturn(username);
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        doThrow(new RuntimeException("Test exception")).when(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                any(), any(), any(), any(), any(), any(), any());
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> authenticationSuccessListener.onAuthenticationSuccess(event));
    }
    
    @Test
    void onAuthenticationSuccess_shouldHandleNonUserDetailsPrincipal() {
        // Arrange
        String username = "testuser@example.com";
        
        // Use a String principal instead of UserDetails
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "Not a UserDetails object", "password");
        
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        // Act
        authenticationSuccessListener.onAuthenticationSuccess(event);
        
        // Assert - should not call aktivitaetsService or nutzerService
        verifyNoInteractions(aktivitaetsProtokollierungService);
        verifyNoInteractions(nutzerService);
    }
    
    @Test
    void onAuthenticationSuccess_shouldHandleUserNotFound() {
        // Arrange
        String username = "nonexistentuser@example.com";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        UserDetails userDetails = new User(username, "password", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "password", authorities);
        
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.empty());
        
        // Act
        authenticationSuccessListener.onAuthenticationSuccess(event);
        
        // Assert - should not call aktivitaetsProtokollierungService.protokolliereAktivitaet
        verify(nutzerService).findeNutzerNachEmail(username);
        verifyNoInteractions(aktivitaetsProtokollierungService);
    }
    
    @Test
    void onAuthenticationSuccess_shouldHandleNullAuthenticationDetails() {
        // Arrange
        String username = "testuser@example.com";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        UserDetails userDetails = new User(username, "password", authorities);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "password", authorities);
        
        // Explicitly set details to null
        authentication.setDetails(null);

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        NutzerDTO mockNutzer = mock(NutzerDTO.class);
        when(mockNutzer.getVorname()).thenReturn("Test");
        when(mockNutzer.getNachname()).thenReturn("User");
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        // Act
        authenticationSuccessListener.onAuthenticationSuccess(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(mockNutzer),
                eq(AktivitaetsTyp.LOGIN),
                eq("Anmeldung als Test User"),
                detailsCaptor.capture(),
                eq(true),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        // Should not contain remote address or session id
        assertFalse(details.containsKey("remoteAddress"));
        assertFalse(details.containsKey("sessionId"));
        assertEquals(mockNutzer.getClass().getSimpleName(), details.get("nutzerTyp"));
        assertEquals(authorities, details.get("authorities"));
    }
    
    @Test
    void getNutzerIdentifikation_shouldReturnIdWhenEmailIsNull() {
        // Arrange
        NutzerDTO mockNutzer = new NutzerDTO();
        mockNutzer.setId(123L);
        mockNutzer.setVorname(null);
        mockNutzer.setNachname(null);
        mockNutzer.setEmail(null);

        String username = "testuser@example.com";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        UserDetails userDetails = new User(username, "password", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "password", authorities);
        
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        // Act
        authenticationSuccessListener.onAuthenticationSuccess(event);
        
        // Assert
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(mockNutzer),
                eq(AktivitaetsTyp.LOGIN),
                eq("Anmeldung als Nutzer ID: 123"),
                any(),
                eq(true),
                isNull(),
                isNull()
        );
    }
}
