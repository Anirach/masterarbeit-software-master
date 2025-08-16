package de.fuh.kn.webapp.security;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.uebung.service.FortschrittService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für die Sicherheitskonfiguration (SecurityConfig).
 * Diese Tests stellen sicher, dass die Zugriffsbeschränkungen korrekt konfiguriert sind:
 * - Öffentliche Endpunkte (Login, Registrierung, statische Ressourcen) für jeden zugänglich
 * - Rollenspezifische Endpunkte nur für Benutzer mit entsprechenden Rollen zugänglich
 * - Dashboard für angemeldete Nutzer mit beliebiger Rolle erreichbar
 * - Nicht definierte Endpunkte für niemanden zugänglich
 * 
 * Die Tests verwenden MockMvc und benutzerdefinierte Security-Annotations (@WithMockStudent,
 * @WithMockKursbetreuer), um verschiedene Benutzerrollen zu simulieren.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NutzerService nutzerService;

    @MockitoBean
    private KursService kursService;

    @MockitoBean
    private BelegungService belegungService;

    @MockitoBean
    private FortschrittService fortschrittService;

    /**
     * Tests für Endpunkte, die öffentlich zugänglich sein sollten.
     * Diese Tests prüfen, dass nicht angemeldete Benutzer Zugriff auf die 
     * öffentlichen Seiten und statische Ressourcen haben.
     */
    @Nested
    @DisplayName("Tests für öffentlich zugängliche Endpunkte")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Öffentliche Seiten sollten für nicht angemeldete Benutzer zugänglich sein")
        @WithAnonymousUser
        void publicPagesAccessibleToAnonymousUsers() throws Exception {
            // Hauptseite - sollte zur Login-Seite weiterleiten
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));

            // Login-Seite - sollte direkt zugänglich sein
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(unauthenticated());

            // Registrierungsseite - sollte direkt zugänglich sein
            mockMvc.perform(get("/registration"))
                    .andExpect(status().isOk())
                    .andExpect(unauthenticated());

            // Informationsseite - sollte direkt zugänglich sein
            mockMvc.perform(get("/information"))
                    .andExpect(status().isOk())
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Statische Ressourcen sollten für nicht angemeldete Benutzer zugänglich sein")
        @WithAnonymousUser
        void staticResourcesAccessibleToAnonymousUsers() throws Exception {
            // robots.txt - sollte für Crawler zugänglich sein
            mockMvc.perform(get("/robots.txt"))
                    .andExpect(status().isOk())
                    .andExpect(unauthenticated());

            // Bild-Ressourcen - sollten für alle zugänglich sein
            mockMvc.perform(get("/images/knlogo.png"))
                    .andExpect(status().isOk())
                    .andExpect(unauthenticated());
        }
    }

    /**
     * Tests für das Dashboard, das für angemeldete Benutzer zugänglich sein sollte.
     * Diese Tests prüfen, dass das allgemeine Dashboard-Mapping ("/dashboard") 
     * Benutzer je nach Rolle zum passenden rollenspezifischen Dashboard weiterleitet.
     */
    @Nested
    @DisplayName("Tests für das Dashboard")
    class DashboardTests {

        @Test
        @DisplayName("Dashboard sollte für angemeldete Studenten zugänglich sein")
        @WithMockStudent
        void dashboardAccessibleToStudent() throws Exception {
            // Mock-Konfiguration für den NutzerService
            when(nutzerService.findeNutzerNachEmail(anyString()))
                    .thenReturn(Optional.of(new NutzerDTO()));

            // Prüft, dass Studenten zum Studenten-Dashboard weitergeleitet werden
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/student/dashboard"))
                    .andExpect(authenticated().withRoles("STUDENT"));
        }

        @Test
        @DisplayName("Dashboard sollte für angemeldete Kursbetreuer zugänglich sein")
        @WithMockKursbetreuer
        void dashboardAccessibleToKursbetreuer() throws Exception {
            // Mock-Konfiguration für den NutzerService
            when(nutzerService.findeNutzerNachEmail(anyString()))
                    .thenReturn(Optional.of(new NutzerDTO()));

            // Prüft, dass Kursbetreuer zum Kursbetreuer-Dashboard weitergeleitet werden
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/kursbetreuer/dashboard"))
                    .andExpect(authenticated().withRoles("KURSBETREUER"));
        }

        @Test
        @DisplayName("Dashboard sollte für nicht angemeldete Benutzer nicht zugänglich sein")
        @WithAnonymousUser
        void dashboardNotAccessibleToAnonymousUsers() throws Exception {
            // Prüft, dass anonyme Benutzer nicht auf das Dashboard zugreifen können
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login")) // Weiterleitung zur Login-Seite
                    .andExpect(unauthenticated());
        }
    }

    /**
     * Tests für studentenspezifische Endpunkte.
     * Diese Tests prüfen, dass Endpunkte unter "/student/**" nur für 
     * angemeldete Benutzer mit der Rolle STUDENT zugänglich sind.
     */
    @Nested
    @DisplayName("Tests für Studenten-Endpunkte")
    class StudentEndpointsTests {

        @Test
        @DisplayName("Student-Endpunkte sollten für Studenten zugänglich sein")
        @WithMockStudent
        void studentEndpointsAccessibleToStudents() throws Exception {
            // Mock für NutzerService für getAuthenticatedNutzer erstellen
            NutzerDTO mockNutzerDTO = new NutzerDTO();
            mockNutzerDTO.setId(1L); // Verwenden Sie die gleiche ID wie in WithMockStudentSecurityContextFactory
            mockNutzerDTO.setVorname("Test");
            mockNutzerDTO.setNachname("Student");
            when(nutzerService.getAuthenticatedNutzer()).thenReturn(mockNutzerDTO);

            // Mock für getStudentById erstellen - dies ist die kritische Ergänzung
            StudentDTO mockStudentDTO = new StudentDTO();
            mockStudentDTO.setId(1L);
            mockStudentDTO.setVorname("Test");
            mockStudentDTO.setNachname("Student");
            mockStudentDTO.setEmail("test@student.de");
            when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));

            // Mocks für FortschrittService und BelegungService - damit keine weiteren Fehler auftreten
            when(kursService.getKursById(anyLong())).thenReturn(new KursDTO());

            // Minimum mocking für BelegungService
            when(belegungService.getAllEnrollmentsByStudent(mockStudentDTO)).thenReturn(Collections.emptyList());
            when(belegungService.getActiveEnrollmentsByStudent(mockStudentDTO)).thenReturn(Collections.emptyList());

            // Minimum mocking für FortschrittService
            when(fortschrittService.berechneFortschrittFuerKurse(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(Collections.emptyList());
            when(fortschrittService.zaehleSolvedAssignments(mockStudentDTO.getId())).thenReturn(0L);

            // Prüft, dass Studenten auf ihr Dashboard zugreifen können
            mockMvc.perform(get("/student/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(authenticated().withRoles("STUDENT"));
        }

        @Test
        @DisplayName("Student-Endpunkte sollten für Kursbetreuer nicht zugänglich sein")
        @WithMockKursbetreuer
        void studentEndpointsNotAccessibleToKursbetreuer() throws Exception {
            // Prüft, dass Kursbetreuer keinen Zugriff auf Studenten-Bereiche haben
            mockMvc.perform(get("/student/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student-Endpunkte sollten für nicht angemeldete Benutzer nicht zugänglich sein")
        @WithAnonymousUser
        void studentEndpointsNotAccessibleToAnonymousUsers() throws Exception {
            // Prüft, dass anonyme Benutzer keinen Zugriff auf Studenten-Bereiche haben
            mockMvc.perform(get("/student/dashboard"))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * Tests für kursbetreuersspezifische Endpunkte.
     * Diese Tests prüfen, dass Endpunkte unter "/kursbetreuer/**" nur für 
     * angemeldete Benutzer mit der Rolle KURSBETREUER zugänglich sind.
     */
    @Nested
    @DisplayName("Tests für Kursbetreuer-Endpunkte")
    class KursbetreuerEndpointsTests {

        @Test
        @DisplayName("Kursbetreuer-Endpunkte sollten für Kursbetreuer zugänglich sein")
        @WithMockKursbetreuer
        void kursbetreuerEndpointsAccessibleToKursbetreuer() throws Exception {
            // Mock für KursService erstellen, um einen existierenden Kurs zurückzugeben
            KursDTO mockKursDTO = new KursDTO();
            mockKursDTO.setId(1L);
            mockKursDTO.setName("Test Kurs");
            
            // KursService so konfigurieren, dass er für ID 1 einen Kurs zurückgibt
            when(kursService.getKursById(1L)).thenReturn(mockKursDTO);
            
            // Mock für NutzerService für getAuthenticatedNutzer erstellen
            NutzerDTO mockNutzerDTO = new NutzerDTO();
            mockNutzerDTO.setId(1L);
            mockNutzerDTO.setVorname("Test");
            mockNutzerDTO.setNachname("Kursbetreuer");
            when(nutzerService.getAuthenticatedNutzer()).thenReturn(mockNutzerDTO);
            
            // Prüft, dass Kursbetreuer auf ihr Dashboard zugreifen können
            mockMvc.perform(get("/kursbetreuer/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(authenticated().withRoles("KURSBETREUER"));

            // Auskommentierter Test für spezifischen Kurs-Zugriff
            // mockMvc.perform(get("/kursbetreuer/kurse/5"))
            //         .andExpect(status().isOk())
            //         .andExpect(authenticated().withRoles("KURSBETREUER"));

            // Prüft, dass Kursbetreuer auf die Kursverwaltung zugreifen können
            mockMvc.perform(get("/kursbetreuer/kursverwaltung"))
                    .andExpect(status().isOk())
                    .andExpect(authenticated().withRoles("KURSBETREUER"));
                    
            // Teste Zugriff auf Kurseinheit-Formular-Endpunkte
            mockMvc.perform(get("/kursbetreuer/kurse/1/kurseinheiten/neu"))
                    .andExpect(status().isOk())
                    .andExpect(authenticated().withRoles("KURSBETREUER"));
                    
            // Auskommentierter Test für Kurseinheit-Bearbeitung
            // mockMvc.perform(get("/kursbetreuer/kurseinheiten/1/bearbeiten"))
            //         .andExpect(status().isOk())
            //         .andExpect(authenticated().withRoles("KURSBETREUER"));
        }

        @Test
        @DisplayName("Kursbetreuer-Endpunkte sollten für Studenten nicht zugänglich sein")
        @WithMockStudent
        void kursbetreuerEndpointsNotAccessibleToStudents() throws Exception {
            // Prüft, dass Studenten keinen Zugriff auf Kursbetreuer-Dashboard haben
            mockMvc.perform(get("/kursbetreuer/dashboard"))
                    .andExpect(status().isForbidden());

            // Prüft, dass Studenten keinen Zugriff auf Kursdetails haben
            mockMvc.perform(get("/kursbetreuer/kurse/5"))
                    .andExpect(status().isForbidden());

            // Prüft, dass Studenten keinen Zugriff auf die Kursverwaltung haben
            mockMvc.perform(get("/kursbetreuer/kursverwaltung"))
                    .andExpect(status().isForbidden());
                    
            // Prüft, dass Studenten keinen Zugriff auf Kurseinheit-Erstellung haben
            mockMvc.perform(get("/kursbetreuer/kurse/1/kurseinheiten/neu"))
                    .andExpect(status().isForbidden());
                    
            // Prüft, dass Studenten keinen Zugriff auf Kurseinheit-Bearbeitung haben
            mockMvc.perform(get("/kursbetreuer/kurseinheiten/1/bearbeiten"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Kursbetreuer-Endpunkte sollten für nicht angemeldete Benutzer nicht zugänglich sein")
        @WithAnonymousUser
        void kursbetreuerEndpointsNotAccessibleToAnonymousUsers() throws Exception {
            // Prüft, dass anonyme Benutzer keinen Zugriff auf Kursbetreuer-Dashboard haben
            mockMvc.perform(get("/kursbetreuer/dashboard"))
                    .andExpect(status().isForbidden());

            // Prüft, dass anonyme Benutzer keinen Zugriff auf Kursdetails haben
            mockMvc.perform(get("/kursbetreuer/kurse/5"))
                    .andExpect(status().isForbidden());

            // Prüft, dass anonyme Benutzer keinen Zugriff auf die Kursverwaltung haben
            mockMvc.perform(get("/kursbetreuer/kursverwaltung"))
                    .andExpect(status().isForbidden());
                    
            // Prüft, dass anonyme Benutzer keinen Zugriff auf Kurseinheit-Erstellung haben
            mockMvc.perform(get("/kursbetreuer/kurse/1/kurseinheiten/neu"))
                    .andExpect(status().isForbidden());
                    
            // Prüft, dass anonyme Benutzer keinen Zugriff auf Kurseinheit-Bearbeitung haben
            mockMvc.perform(get("/kursbetreuer/kurseinheiten/1/bearbeiten"))
                    .andExpect(status().isForbidden());
                    
            // Prüft, dass anonyme Benutzer keinen Zugriff auf Kurseinheit-Löschung haben
            mockMvc.perform(get("/kursbetreuer/kurseinheiten/1/loeschen"))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * Tests für nicht definierte Endpunkte, die für niemanden zugänglich sein sollten.
     * Diese Tests prüfen, dass Endpunkte, die nicht explizit in der SecurityConfig
     * definiert wurden, für alle Benutzer unzugänglich sind (denyAll-Konfiguration).
     */
    @Nested
    @DisplayName("Tests für nicht definierte Endpunkte")
    class UndefinedEndpointsTests {

        @Test
        @DisplayName("Nicht definierte Endpunkte sollten für anonyme Benutzer nicht zugänglich sein")
        @WithAnonymousUser
        void undefinedEndpointsNotAccessibleToAnonymousUsers() throws Exception {
            // Prüft, dass anonyme Benutzer zur Login-Seite weitergeleitet werden
            mockMvc.perform(get("/undefined/endpoint"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("Nicht definierte Endpunkte sollten für Studenten nicht zugänglich sein")
        @WithMockStudent
        void undefinedEndpointsNotAccessibleToStudents() throws Exception {
            // Prüft, dass Studenten keinen Zugriff auf undefinierte Endpunkte haben
            mockMvc.perform(get("/undefined/endpoint"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Nicht definierte Endpunkte sollten für Kursbetreuer nicht zugänglich sein")
        @WithMockKursbetreuer
        void undefinedEndpointsNotAccessibleToKursbetreuer() throws Exception {
            // Prüft, dass Kursbetreuer keinen Zugriff auf undefinierte Endpunkte haben
            mockMvc.perform(get("/undefined/endpoint"))
                    .andExpect(status().isForbidden());
        }
    }
}