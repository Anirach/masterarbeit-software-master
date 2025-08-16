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
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutSuccessListenerTest {

    @Mock
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    @Mock
    private NutzerService nutzerService;

    @Mock
    private WebAuthenticationDetails webAuthenticationDetails;

    private LogoutSuccessListener logoutSuccessListener;

    @BeforeEach
    void setUp() {
        logoutSuccessListener = new LogoutSuccessListener(nutzerService, aktivitaetsProtokollierungService);
    }

    @Test
    void onLogoutSuccess_shouldLogLogoutForExistingUser() {
        // Arrange
        String username = "testuser@example.com";
        String remoteAddress = "127.0.0.1";
        String sessionId = "testSessionId";

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn(remoteAddress);
        when(webAuthenticationDetails.getSessionId()).thenReturn(sessionId);
        authentication.setDetails(webAuthenticationDetails);

        LogoutSuccessEvent event = new LogoutSuccessEvent(authentication);
        
        NutzerDTO mockNutzer = new NutzerDTO();
        mockNutzer.setEmail(username);
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        // Act
        logoutSuccessListener.onLogoutSuccess(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(mockNutzer),
                eq(AktivitaetsTyp.LOGOUT),
                eq("Abmeldung für Benutzer: " + username),
                detailsCaptor.capture(),
                eq(true),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(username, details.get("username"));
        assertEquals(remoteAddress, details.get("remoteAddress"));
        assertEquals(sessionId, details.get("sessionId"));
    }
    
    @Test
    void onLogoutSuccess_shouldLogLogoutForNonExistentUser() {
        // Arrange
        String username = "nonexistentuser@example.com";
        String remoteAddress = "127.0.0.1";
        String sessionId = "testSessionId";

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn(remoteAddress);
        when(webAuthenticationDetails.getSessionId()).thenReturn(sessionId);
        authentication.setDetails(webAuthenticationDetails);

        LogoutSuccessEvent event = new LogoutSuccessEvent(authentication);
        
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.empty());
        
        // Act
        logoutSuccessListener.onLogoutSuccess(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                isNull(), // Null nutzer
                eq(AktivitaetsTyp.LOGOUT),
                eq("Abmeldung für Benutzer: " + username),
                detailsCaptor.capture(),
                eq(true),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(username, details.get("username"));
        assertEquals(remoteAddress, details.get("remoteAddress"));
        assertEquals(sessionId, details.get("sessionId"));
    }
    
    @Test
    void onLogoutSuccess_shouldHandleExceptions() {
        // Arrange
        String username = "testuser@example.com";

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authentication.setDetails(webAuthenticationDetails);

        LogoutSuccessEvent event = new LogoutSuccessEvent(authentication);
        
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Test exception")).when(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                any(), any(), any(), any(), any(), any(), any());
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> logoutSuccessListener.onLogoutSuccess(event));
        
        // Verify that aktivitaetsService was called
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onLogoutSuccess_shouldHandleNullAuthenticationDetails() {
        // Arrange
        String username = "testuser@example.com";

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        
        // Explicitly set details to null
        authentication.setDetails(null);

        LogoutSuccessEvent event = new LogoutSuccessEvent(authentication);
        
        NutzerDTO mockNutzer = new NutzerDTO();
        mockNutzer.setEmail(username);
        when(nutzerService.findeNutzerNachEmail(username)).thenReturn(Optional.of(mockNutzer));
        
        // Act
        logoutSuccessListener.onLogoutSuccess(event);
        
        // Assert
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(mockNutzer),
                eq(AktivitaetsTyp.LOGOUT),
                eq("Abmeldung für Benutzer: " + username),
                detailsCaptor.capture(),
                eq(true),
                isNull(),
                isNull()
        );
        
        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(username, details.get("username"));
        
        // Should not contain remote address or session id
        assertFalse(details.containsKey("remoteAddress"));
        assertFalse(details.containsKey("sessionId"));
    }
}
