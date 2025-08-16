package de.fuh.kn.webapp.nutzerverwaltung.auth;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.nutzerverwaltung.service.RegistrierungDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.RegistrierungMapper;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests für den NutzerController.
 * Überprüft die Darstellung von Login- und Registrierungsformularen und die 
 * Verarbeitung der Registrierung von Nutzern.
 */
@ExtendWith(MockitoExtension.class)
public class LoginRegistrationControllerTest {

    @Mock
    private NutzerService nutzerService;
    
    @Mock
    private RegistrierungMapper registrierungMapper;
    
    @Mock
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private LoginRegistrationController loginRegistrationController;

    private Student testStudent;
    private RegistrierungDTO testRegistrierungDTO;

    @BeforeEach
    void setUp() {
        // Student-Testdaten
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setEmail("student@test.de");
        testStudent.setPasswort("encodedPassword123");
        testStudent.setVorname("Max");
        testStudent.setNachname("Mustermann");
        testStudent.setMatrikelnummer("123456");
        
        // RegistrierungDTO-Testdaten
        testRegistrierungDTO = new RegistrierungDTO();
        testRegistrierungDTO.setEmail("neu@student.de");
        testRegistrierungDTO.setVorname("Neuer");
        testRegistrierungDTO.setNachname("Student");
        testRegistrierungDTO.setMatrikelnummer("654321");
        testRegistrierungDTO.setPasswort("Passwort123");
        testRegistrierungDTO.setPasswortBestaetigung("Passwort123");
    }

    @Test
    void testShowLoginForm() {
        // Methode aufrufen
        String viewName = loginRegistrationController.showLoginForm();
        
        // Überprüfen, dass die korrekte View zurückgegeben wird
        assertEquals("login", viewName);
    }

    @Test
    void testShowRegistrationForm() {
        // Methode aufrufen
        String viewName = loginRegistrationController.showRegistrationForm(model);
        
        // Überprüfen, dass das Model korrekt befüllt wurde
        verify(model).addAttribute(eq("registrierung"), any(RegistrierungDTO.class));
        
        // Überprüfen, dass die korrekte View zurückgegeben wird
        assertEquals("registration", viewName);
    }

    @Test
    void testRegisterStudent_Success() {
        // Mock für Dummy-Student-Prüfung
        when(nutzerService.existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer())).thenReturn(true);
        
        // Mock für Prüfung auf existierenden registrierten Studenten
        when(nutzerService.existiertRegistrierterStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer())).thenReturn(false);
        
        // Mock des Service für Registrierung
        StudentDTO registeredStudentDTO = new StudentDTO();
        registeredStudentDTO.setId(1L);
        registeredStudentDTO.setEmail(testRegistrierungDTO.getEmail());
        registeredStudentDTO.setMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        when(nutzerService.registriereStudent(any(RegistrierungDTO.class))).thenReturn(registeredStudentDTO);
        
        // Mock des BindingResult
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // Mock für die Aktivitätsprotokollierung
        AktivitaetDTO mockAktivitaetDTO = new AktivitaetDTO();
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            eq(registeredStudentDTO), 
            eq(AktivitaetsTyp.REGISTRIEREN), 
            anyString(), 
            anyMap(), 
            eq(true),
            eq(null),
            eq(null)
        )).thenReturn(mockAktivitaetDTO);
        
        // Methode aufrufen
        String viewName = loginRegistrationController.registerStudent(testRegistrierungDTO, bindingResult, model);
        
        // Verifizieren, dass alle erwarteten Methodenaufrufe erfolgten
        verify(nutzerService).existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        verify(nutzerService).existiertRegistrierterStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        verify(nutzerService).registriereStudent(testRegistrierungDTO);
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            eq(registeredStudentDTO), 
            eq(AktivitaetsTyp.REGISTRIEREN), 
            anyString(), 
            anyMap(), 
            eq(true),
            eq(null),
            eq(null)
        );
        
        // Überprüfen, dass die Weiterleitung zur Login-Seite erfolgt
        assertEquals("redirect:/login?registered=true", viewName);
    }
    
    @Test
    void testRegisterStudent_PasswortMismatch() {
        // Test-Setup für nicht übereinstimmende Passwörter
        RegistrierungDTO registrierungMitFalscherBestaetigung = new RegistrierungDTO();
        registrierungMitFalscherBestaetigung.setEmail("neu@student.de");
        registrierungMitFalscherBestaetigung.setVorname("Neuer");
        registrierungMitFalscherBestaetigung.setNachname("Student");
        registrierungMitFalscherBestaetigung.setMatrikelnummer("654321");
        registrierungMitFalscherBestaetigung.setPasswort("Passwort123");
        registrierungMitFalscherBestaetigung.setPasswortBestaetigung("Passwort456"); // Falsche Bestätigung

        when(bindingResult.hasErrors()).thenReturn(true);

        // Methode aufrufen
        String viewName = loginRegistrationController.registerStudent(registrierungMitFalscherBestaetigung, bindingResult, model);

        // Überprüfen, dass der Fehler im BindingResult registriert wurde
        verify(bindingResult).rejectValue(
            eq("passwortBestaetigung"), 
            eq("error.passwort"), 
            eq("Die Passwörter stimmen nicht überein")
        );
        
        // Überprüfen, dass die Darstellung wieder auf der Registrierungsseite bleibt
        assertEquals("registration", viewName);
        
        // Überprüfen, dass die Service-Methoden NICHT aufgerufen wurden
        verify(nutzerService, never()).existiertDummyStudentMitMatrikelnummer(any());
        verify(nutzerService, never()).existiertRegistrierterStudentMitMatrikelnummer(any());
        verify(nutzerService, never()).registriereStudent(any(RegistrierungDTO.class));
    }
    
    @Test
    void testRegisterStudent_ValidationErrors() {
        // Mock des BindingResult - hat Fehler
        when(bindingResult.hasErrors()).thenReturn(true);
        
        // Methode aufrufen
        String viewName = loginRegistrationController.registerStudent(testRegistrierungDTO, bindingResult, model);
        
        // Überprüfen, dass die Service-Methoden NICHT aufgerufen wurden
        verify(nutzerService, never()).existiertDummyStudentMitMatrikelnummer(any());
        verify(nutzerService, never()).existiertRegistrierterStudentMitMatrikelnummer(any());
        verify(nutzerService, never()).registriereStudent(any(RegistrierungDTO.class));
        
        // Überprüfen, dass die Darstellung wieder auf der Registrierungsseite bleibt
        assertEquals("registration", viewName);
    }
    
    @Test
    void testRegisterStudent_EmailExistsError() {
        // Mock für Dummy-Student-Prüfung
        when(nutzerService.existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer())).thenReturn(true);
        
        // Mock für Prüfung auf existierenden registrierten Studenten
        when(nutzerService.existiertRegistrierterStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer())).thenReturn(false);
        
        // Mock des BindingResult
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // Mock des Service - wirft Exception bei Registrierung
        when(nutzerService.registriereStudent(any(RegistrierungDTO.class)))
            .thenThrow(new IllegalArgumentException("Ein Nutzer mit dieser E-Mail-Adresse existiert bereits"));
        
        // Methode aufrufen
        String viewName = loginRegistrationController.registerStudent(testRegistrierungDTO, bindingResult, model);
        
        // Überprüfen, dass alle erwarteten Methodenaufrufe erfolgten
        verify(nutzerService).existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        verify(nutzerService).existiertRegistrierterStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        verify(nutzerService).registriereStudent(testRegistrierungDTO);
        
        // Überprüfen, dass die Fehlermeldung im Model gesetzt wurde
        verify(model).addAttribute("registrationError", "Ein Nutzer mit dieser E-Mail-Adresse existiert bereits");
        
        // Überprüfen, dass die Darstellung wieder auf der Registrierungsseite bleibt
        assertEquals("registration", viewName);
    }
    
    @Test
    void testRegisterStudent_NoDummyStudentExists() {
        // Mock für Dummy-Student-Prüfung - gibt false zurück (kein Dummy-Student)
        when(nutzerService.existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer())).thenReturn(false);
        
        // Mock des BindingResult
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // Methode aufrufen
        String viewName = loginRegistrationController.registerStudent(testRegistrierungDTO, bindingResult, model);
        
        // Überprüfen, dass nur die Dummy-Prüfung aufgerufen wurde
        verify(nutzerService).existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        verify(nutzerService, never()).existiertRegistrierterStudentMitMatrikelnummer(any());
        verify(nutzerService, never()).registriereStudent(any());
        
        // Überprüfen, dass die Fehlermeldung im Model gesetzt wurde
        verify(model).addAttribute(
            eq("registrationError"),
            eq("Es existiert kein Student mit dieser Matrikelnummer im System. Bitte wenden Sie sich an Ihren Kursbetreuer oder versuchen Sie es in einigen Tagen erneut.")
        );
        
        // Überprüfen, dass die Darstellung wieder auf der Registrierungsseite bleibt
        assertEquals("registration", viewName);
    }
    
    @Test
    void testRegisterStudent_AlreadyRegisteredStudent() {
        // Mock für Dummy-Student-Prüfung
        when(nutzerService.existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer())).thenReturn(true);
        
        // Mock für Prüfung auf existierenden registrierten Studenten - gibt true zurück (bereits registriert)
        when(nutzerService.existiertRegistrierterStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer())).thenReturn(true);
        
        // Mock des BindingResult
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // Methode aufrufen
        String viewName = loginRegistrationController.registerStudent(testRegistrierungDTO, bindingResult, model);
        
        // Überprüfen, dass beide Prüfungen aufgerufen wurden
        verify(nutzerService).existiertDummyStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        verify(nutzerService).existiertRegistrierterStudentMitMatrikelnummer(testRegistrierungDTO.getMatrikelnummer());
        verify(nutzerService, never()).registriereStudent(any());
        
        // Überprüfen, dass die Fehlermeldung im Model gesetzt wurde
        verify(model).addAttribute(
            eq("registrationError"),
            eq("Ein Student mit dieser Matrikelnummer ist bereits registriert.")
        );
        
        // Überprüfen, dass die Darstellung wieder auf der Registrierungsseite bleibt
        assertEquals("registration", viewName);
    }
}
