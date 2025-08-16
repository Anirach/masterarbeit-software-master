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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFailureListenerTest {

    @Mock
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    @Mock
    private NutzerService nutzerService;

    @Mock
    private WebAuthenticationDetails webAuthenticationDetails;

    private AuthenticationFailureListener authenticationFailureListener;

    @BeforeEach
    void setUp() {
        authenticationFailureListener = new AuthenticationFailureListener(nutzerService, aktivitaetsProtokollierungService);
    }

    @Test
    void onAuthenticationFailure_shouldLogFailedLoginAttemptWithoutUser() {
        // Arrange
        String username = "testuser@example.com";
        String remoteAddress = "127.0.0.1";
        String sessionId = "testSessionId";
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, "wrongpassword");
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn(remoteAddress);
        when(webAuthenticationDetails.getSessionId()).thenReturn(sessionId);
        authentication.setDetails(webAuthenticationDetails);

        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                authentication, exception);
        
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.empty());
        
        // Act
        authenticationFailureListener.onAuthenticationFailure(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                isNull(),
                eq(AktivitaetsTyp.LOGIN),
                eq("Fehlgeschlagene Anmeldung für Benutzer: " + username),
                detailsCaptor.capture(),
                eq(false),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(username, details.get("username"));
        assertEquals("BadCredentialsException", details.get("fehlerklasse"));
        assertEquals("Bad credentials", details.get("fehlermeldung"));
        assertEquals(remoteAddress, details.get("remoteAddress"));
        assertEquals(sessionId, details.get("sessionId"));
    }
    
    @Test
    void onAuthenticationFailure_shouldLogFailedLoginAttemptWithExistingUser() {
        // Arrange
        String username = "existinguser@example.com";
        String remoteAddress = "192.168.1.1";
        String sessionId = "existingUserSessionId";
        BadCredentialsException exception = new BadCredentialsException("Invalid password");
        
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, "wrongpassword");
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn(remoteAddress);
        when(webAuthenticationDetails.getSessionId()).thenReturn(sessionId);
        authentication.setDetails(webAuthenticationDetails);
        
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                authentication, exception);
        
        NutzerDTO mockNutzer = new NutzerDTO();
        mockNutzer.setId(1L);
        mockNutzer.setEmail(username);
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        // Act
        authenticationFailureListener.onAuthenticationFailure(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(mockNutzer),
                eq(AktivitaetsTyp.LOGIN),
                eq("Fehlgeschlagene Anmeldung für Benutzer: " + username),
                detailsCaptor.capture(),
                eq(false),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(username, details.get("username"));
        assertEquals("BadCredentialsException", details.get("fehlerklasse"));
        assertEquals("Invalid password", details.get("fehlermeldung"));
        assertEquals(remoteAddress, details.get("remoteAddress"));
        assertEquals(sessionId, details.get("sessionId"));
    }
    
    @Test
    void onAuthenticationFailure_shouldHandleExceptions() {
        // Arrange
        String username = "testuser@example.com";
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, "wrongpassword");
        authentication.setDetails(webAuthenticationDetails);

        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                authentication, exception);
        
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Test exception")).when(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                any(), any(), any(), any(), any(), any(), any());
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> authenticationFailureListener.onAuthenticationFailure(event));
        
        // Verify that aktivitaetsService was called
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                any(), any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void onAuthenticationFailure_shouldHandleNullAuthenticationDetails() {
        // Arrange
        String username = "testuser@example.com";
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, "wrongpassword");
        // Explicitly set details to null
        authentication.setDetails(null);
        
        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(
                authentication, exception);
        
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.empty());
        
        // Act
        authenticationFailureListener.onAuthenticationFailure(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                isNull(),
                eq(AktivitaetsTyp.LOGIN),
                eq("Fehlgeschlagene Anmeldung für Benutzer: " + username),
                detailsCaptor.capture(),
                eq(false),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(username, details.get("username"));
        assertEquals("BadCredentialsException", details.get("fehlerklasse"));
        assertEquals("Bad credentials", details.get("fehlermeldung"));
        
        // Should not contain remote address or session id
        assertFalse(details.containsKey("remoteAddress"));
        assertFalse(details.containsKey("sessionId"));
    }
}
