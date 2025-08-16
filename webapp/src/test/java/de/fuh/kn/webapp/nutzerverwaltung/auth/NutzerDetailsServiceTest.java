package de.fuh.kn.webapp.nutzerverwaltung.auth;

import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test für den NutzerDetailsService.
 * Testet die korrekte Konvertierung von DTOs zu UserDetails-Objekten.
 */
@ExtendWith(MockitoExtension.class)
public class NutzerDetailsServiceTest {

    @Mock
    private NutzerService nutzerService;

    @InjectMocks
    private NutzerDetailsService nutzerDetailsService;

    private StudentDTO testStudentDTO;
    private KursbetreuerDTO testKursbetreuerDTO;
    private NutzerDTO testUnknownDTO;

    @BeforeEach
    void setUp() {
        // StudentDTO
        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setEmail("student@test.de");
        testStudentDTO.setVorname("Max");
        testStudentDTO.setNachname("Mustermann");
        testStudentDTO.setMatrikelnummer("123456");
        testStudentDTO.setIstRegistriert(true);
        testStudentDTO.setDisplayName("Max Mustermann");

        // KursbetreuerDTO
        testKursbetreuerDTO = new KursbetreuerDTO();
        testKursbetreuerDTO.setId(2L);
        testKursbetreuerDTO.setEmail("betreuer@test.de");
        testKursbetreuerDTO.setVorname("Erika");
        testKursbetreuerDTO.setNachname("Musterfrau");
        testKursbetreuerDTO.setIstRegistriert(true);
        testKursbetreuerDTO.setDisplayName("Erika Musterfrau");
        
        // Unbekanntes DTO (für Fehlertests)
        testUnknownDTO = new NutzerDTO();
        testUnknownDTO.setId(3L);
        testUnknownDTO.setEmail("unknown@test.de");
        testUnknownDTO.setVorname("Unbekannt");
        testUnknownDTO.setNachname("Typ");
        testUnknownDTO.setIstRegistriert(true);
        testUnknownDTO.setDisplayName("Unbekannt Typ");
    }

    @Test
    void testLoadUserByUsernameForStudent() {
        // Mock: Service gibt StudentDTO zurück
        when(nutzerService.findeNutzerNachEmail("student@test.de"))
            .thenReturn(Optional.of(testStudentDTO));
        when(nutzerService.findePasswortNachEmail("student@test.de"))
            .thenReturn(Optional.of("encodedPassword123"));

        // UserDetails laden
        UserDetails userDetails = nutzerDetailsService.loadUserByUsername("student@test.de");

        // Überprüfungen
        assertInstanceOf(StudentUserDetails.class, userDetails);
        assertEquals("student@test.de", userDetails.getUsername());
        assertEquals("encodedPassword123", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        
        // Rollenprüfung
        boolean hasStudentRole = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> role.equals("ROLE_STUDENT"));
        assertTrue(hasStudentRole);
        
        // Spezifische StudentUserDetails-Prüfungen
        StudentUserDetails studentDetails = (StudentUserDetails) userDetails;
        assertEquals(1L, studentDetails.getId());
        assertEquals("Max", studentDetails.getVorname());
        assertEquals("Mustermann", studentDetails.getNachname());
        assertEquals("123456", studentDetails.getMatrikelnummer());
        
        // Verify service methods were called
        verify(nutzerService).findeNutzerNachEmail("student@test.de");
        verify(nutzerService).findePasswortNachEmail("student@test.de");
    }

    @Test
    void testLoadUserByUsernameForKursbetreuer() {
        // Mock: Service gibt KursbetreuerDTO zurück
        when(nutzerService.findeNutzerNachEmail("betreuer@test.de"))
            .thenReturn(Optional.of(testKursbetreuerDTO));
        when(nutzerService.findePasswortNachEmail("betreuer@test.de"))
            .thenReturn(Optional.of("encodedPassword456"));

        // UserDetails laden
        UserDetails userDetails = nutzerDetailsService.loadUserByUsername("betreuer@test.de");

        // Überprüfungen
        assertInstanceOf(KursbetreuerUserDetails.class, userDetails);
        assertEquals("betreuer@test.de", userDetails.getUsername());
        assertEquals("encodedPassword456", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        
        // Rollenprüfung
        boolean hasKursbetreuerRole = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> role.equals("ROLE_KURSBETREUER"));
        assertTrue(hasKursbetreuerRole);
        
        // Spezifische KursbetreuerUserDetails-Prüfungen
        KursbetreuerUserDetails betreuerDetails = (KursbetreuerUserDetails) userDetails;
        assertEquals(2L, betreuerDetails.getId());
        assertEquals("Erika", betreuerDetails.getVorname());
        assertEquals("Musterfrau", betreuerDetails.getNachname());
        
        // Verify service methods were called
        verify(nutzerService).findeNutzerNachEmail("betreuer@test.de");
        verify(nutzerService).findePasswortNachEmail("betreuer@test.de");
    }

    @Test
    void testLoadUserByUsernameNotFound() {
        // Mock: Service gibt keinen Nutzer zurück
        when(nutzerService.findeNutzerNachEmail("nichtvorhanden@test.de"))
            .thenReturn(Optional.empty());

        // Überprüfen, dass Exception geworfen wird
        assertThrows(UsernameNotFoundException.class, () -> 
            nutzerDetailsService.loadUserByUsername("nichtvorhanden@test.de"));
            
        // Verify service method was called
        verify(nutzerService).findeNutzerNachEmail("nichtvorhanden@test.de");
    }

    @Test
    void testLoadUserByUsernameUnknownType() {
        // Mock: Unbekannter Nutzertyp (weder Student noch Kursbetreuer)
        when(nutzerService.findeNutzerNachEmail("unknown@test.de"))
            .thenReturn(Optional.of(testUnknownDTO));
        when(nutzerService.findePasswortNachEmail("unknown@test.de"))
            .thenReturn(Optional.of("encodedPassword789"));

        // Überprüfen, dass Exception geworfen wird
        assertThrows(IllegalArgumentException.class, () -> 
            nutzerDetailsService.loadUserByUsername("unknown@test.de"));
            
        // Verify service methods were called
        verify(nutzerService).findeNutzerNachEmail("unknown@test.de");
        verify(nutzerService).findePasswortNachEmail("unknown@test.de");
    }
    
    @Test
    void testLoadUserByUsernamePasswordNotFound() {
        // Mock: Service gibt Nutzer zurück, aber kein Passwort
        when(nutzerService.findeNutzerNachEmail("student@test.de"))
            .thenReturn(Optional.of(testStudentDTO));
        when(nutzerService.findePasswortNachEmail("student@test.de"))
            .thenReturn(Optional.empty());

        // Überprüfen, dass Exception geworfen wird
        assertThrows(UsernameNotFoundException.class, () -> 
            nutzerDetailsService.loadUserByUsername("student@test.de"));
            
        // Verify service methods were called
        verify(nutzerService).findeNutzerNachEmail("student@test.de");
        verify(nutzerService).findePasswortNachEmail("student@test.de");
    }
    
    @Test
    void testLoadUserByUsernameNotRegistered() {
        // Mock: Nutzer ist nicht registriert
        StudentDTO notRegisteredStudentDTO = new StudentDTO();
        notRegisteredStudentDTO.setId(4L);
        notRegisteredStudentDTO.setEmail("not-registered@test.de");
        notRegisteredStudentDTO.setIstRegistriert(false);
        
        when(nutzerService.findeNutzerNachEmail("not-registered@test.de"))
            .thenReturn(Optional.of(notRegisteredStudentDTO));

        // Überprüfen, dass Exception geworfen wird
        assertThrows(UsernameNotFoundException.class, () -> 
            nutzerDetailsService.loadUserByUsername("not-registered@test.de"));
            
        // Verify service method was called
        verify(nutzerService).findeNutzerNachEmail("not-registered@test.de");
        verify(nutzerService, never()).findePasswortNachEmail(anyString());
    }
}