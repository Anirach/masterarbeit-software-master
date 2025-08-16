package de.fuh.kn.webapp.nutzerverwaltung.controller;

import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den NutzerHtmxController.
 * Testet die HTMX-basierte Suche und Filterung von Nutzern.
 */
@ExtendWith(MockitoExtension.class)
class NutzerHtmxControllerTest {

    @Mock
    private NutzerService nutzerService;

    @Mock
    private Model model;

    @InjectMocks
    private NutzerHtmxController controller;

    private List<StudentDTO> testStudenten;
    private List<KursbetreuerDTO> testKursbetreuer;
    
    @BeforeEach
    void setUp() {
        // Test-Studenten erstellen
        testStudenten = new ArrayList<>();
        
        StudentDTO student1 = new StudentDTO();
        student1.setId(1L);
        student1.setVorname("Max");
        student1.setNachname("Mustermann");
        student1.setEmail("max.mustermann@uni.de");
        student1.setMatrikelnummer("12345678");
        student1.setIstRegistriert(true);
        
        StudentDTO student2 = new StudentDTO();
        student2.setId(2L);
        student2.setVorname("Anna");
        student2.setNachname("Schmidt");
        student2.setEmail("anna.schmidt@uni.de");
        student2.setMatrikelnummer("87654321");
        student2.setIstRegistriert(false);
        
        testStudenten.add(student1);
        testStudenten.add(student2);
        
        // Test-Kursbetreuer erstellen
        testKursbetreuer = new ArrayList<>();
        
        KursbetreuerDTO betreuer1 = new KursbetreuerDTO();
        betreuer1.setId(1L);
        betreuer1.setVorname("Thomas");
        betreuer1.setNachname("Weber");
        betreuer1.setEmail("thomas.weber@uni.de");
        betreuer1.setIstRegistriert(true);
        
        KursbetreuerDTO betreuer2 = new KursbetreuerDTO();
        betreuer2.setId(2L);
        betreuer2.setVorname("Julia");
        betreuer2.setNachname("Müller");
        betreuer2.setEmail("julia.mueller@uni.de");
        betreuer2.setIstRegistriert(true);
        
        testKursbetreuer.add(betreuer1);
        testKursbetreuer.add(betreuer2);
    }

    /**
     * Testet die Suche nach Studenten ohne Filter, was alle Studenten zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithoutFilters_ShouldReturnAllStudents() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        
        // Act
        String viewName = controller.searchStudenten(null, null, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        verify(model).addAttribute("studenten", testStudenten);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche nach Studenten mit dem Filter "nur registrierte",
     * was nur registrierte Studenten zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithRegisteredFilter_ShouldReturnOnlyRegisteredStudents() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        
        // Act
        String viewName = controller.searchStudenten(null, true, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        
        // Verify that only registered students are returned
        verify(model).addAttribute(eq("studenten"), argThat(list -> {
            List<StudentDTO> students = (List<StudentDTO>) list;
            return students.size() == 1 && students.get(0).getIstRegistriert();
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche nach Studenten mit einem Suchbegriff für den Namen,
     * was nur Studenten mit passenden Namen zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithNameSearch_ShouldReturnMatchingStudents() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        
        // Act
        String viewName = controller.searchStudenten("mann", null, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        
        // Verify that only students with matching names are returned
        verify(model).addAttribute(eq("studenten"), argThat(list -> {
            List<StudentDTO> students = (List<StudentDTO>) list;
            return students.size() == 1 && 
                   students.get(0).getNachname().toLowerCase().contains("mann");
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche nach Studenten mit einem Suchbegriff für die Matrikelnummer,
     * was nur Studenten mit passenden Matrikelnummern zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithMatrikelnummerSearch_ShouldReturnMatchingStudents() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        
        // Act
        String viewName = controller.searchStudenten("1234", null, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        
        // Verify that only students with matching Matrikelnummer are returned
        verify(model).addAttribute(eq("studenten"), argThat(list -> {
            List<StudentDTO> students = (List<StudentDTO>) list;
            return students.size() == 1 && 
                   students.get(0).getMatrikelnummer().contains("1234");
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche nach Studenten mit einem Suchbegriff für die E-Mail-Adresse,
     * was nur Studenten mit passenden E-Mail-Adressen zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithEmailSearch_ShouldReturnMatchingStudents() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        
        // Act
        String viewName = controller.searchStudenten("anna", null, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        
        // Verify that only students with matching email are returned
        verify(model).addAttribute(eq("studenten"), argThat(list -> {
            List<StudentDTO> students = (List<StudentDTO>) list;
            return students.size() == 1 && 
                   students.get(0).getEmail().toLowerCase().contains("anna");
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche mit dem Statuswort "registriert",
     * was nur registrierte Studenten zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithStatusSearchRegistriert_ShouldReturnRegisteredStudents() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        
        // Act
        String viewName = controller.searchStudenten("registriert", null, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        
        // Verify that only registered students are returned
        verify(model).addAttribute(eq("studenten"), argThat(list -> {
            List<StudentDTO> students = (List<StudentDTO>) list;
            return students.size() == 1 && students.get(0).getIstRegistriert();
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche mit dem Statuswort "nicht registriert",
     * was nur nicht registrierte Studenten zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithStatusSearchNichtRegistriert_ShouldReturnUnregisteredStudents() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        
        // Act
        String viewName = controller.searchStudenten("nicht registriert", null, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        
        // Verify that only unregistered students are returned
        verify(model).addAttribute(eq("studenten"), argThat(list -> {
            List<StudentDTO> students = (List<StudentDTO>) list;
            return students.size() == 1 && !students.get(0).getIstRegistriert();
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche mit kombiniertem Filter für registrierte Studenten und Suchbegriff,
     * was nur registrierte Studenten mit passendem Namen zurückgeben sollte.
     */
    @Test
    void searchStudenten_WithRegisteredFilterAndSearch_ShouldCombineBothFilters() {
        // Arrange
        // Füge einen weiteren registrierten Studenten mit anderem Namen hinzu
        StudentDTO student3 = new StudentDTO();
        student3.setId(3L);
        student3.setVorname("Hans");
        student3.setNachname("Müller");
        student3.setEmail("hans.mueller@uni.de");
        student3.setMatrikelnummer("11223344");
        student3.setIstRegistriert(true);
        
        List<StudentDTO> erweiterteTestStudenten = new ArrayList<>(testStudenten);
        erweiterteTestStudenten.add(student3);
        
        when(nutzerService.getAlleStudenten()).thenReturn(erweiterteTestStudenten);

        // Act
        String viewName = controller.searchStudenten("mann", true, model);

        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        
        // Verify that only registered students matching the search term are returned
        verify(model).addAttribute(eq("studenten"), argThat(list -> {
            List<StudentDTO> students = (List<StudentDTO>) list;
            return students.size() == 1 && 
                   students.get(0).getIstRegistriert() &&
                   students.get(0).getNachname().toLowerCase().contains("mann");
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Fehlerbehandlung bei der Suche nach Studenten,
     * wenn eine Exception auftritt.
     */
    @Test
    void searchStudenten_WhenExceptionOccurs_ShouldHandleExceptionAndReturnEmptyList() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenThrow(new RuntimeException("Testfehler"));

        // Act
        String viewName = controller.searchStudenten(null, null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table", viewName);
        verify(model).addAttribute("studenten", Collections.emptyList());
        verify(model).addAttribute(eq("errorMessage"), contains("Fehler bei der Suche"));
    }
    
    /**
     * Testet die Suche nach Kursbetreuern ohne Filter,
     * was alle Kursbetreuer zurückgeben sollte.
     */
    @Test
    void searchKursbetreuer_WithoutFilters_ShouldReturnAllKursbetreuer() {
        // Arrange
        when(nutzerService.getAlleKursbetreuer()).thenReturn(testKursbetreuer);
        
        // Act
        String viewName = controller.searchKursbetreuer(null, model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/kursbetreuer-table :: .kursbetreuer-table", viewName);
        verify(model).addAttribute("kursbetreuer", testKursbetreuer);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche nach Kursbetreuern mit einem Suchbegriff für den Namen,
     * was nur Kursbetreuer mit passenden Namen zurückgeben sollte.
     */
    @Test
    void searchKursbetreuer_WithNameSearch_ShouldReturnMatchingKursbetreuer() {
        // Arrange
        when(nutzerService.getAlleKursbetreuer()).thenReturn(testKursbetreuer);
        
        // Act
        String viewName = controller.searchKursbetreuer("weber", model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/kursbetreuer-table :: .kursbetreuer-table", viewName);
        
        // Verify that only kursbetreuer with matching names are returned
        verify(model).addAttribute(eq("kursbetreuer"), argThat(list -> {
            List<KursbetreuerDTO> kursbetreuer = (List<KursbetreuerDTO>) list;
            return kursbetreuer.size() == 1 && 
                   kursbetreuer.get(0).getNachname().toLowerCase().contains("weber");
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Suche nach Kursbetreuern mit einem Suchbegriff für die E-Mail-Adresse,
     * was nur Kursbetreuer mit passenden E-Mail-Adressen zurückgeben sollte.
     */
    @Test
    void searchKursbetreuer_WithEmailSearch_ShouldReturnMatchingKursbetreuer() {
        // Arrange
        when(nutzerService.getAlleKursbetreuer()).thenReturn(testKursbetreuer);
        
        // Act
        String viewName = controller.searchKursbetreuer("julia", model);
        
        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/kursbetreuer-table :: .kursbetreuer-table", viewName);
        
        // Verify that only kursbetreuer with matching email are returned
        verify(model).addAttribute(eq("kursbetreuer"), argThat(list -> {
            List<KursbetreuerDTO> kursbetreuer = (List<KursbetreuerDTO>) list;
            return kursbetreuer.size() == 1 && 
                   kursbetreuer.get(0).getEmail().toLowerCase().contains("julia");
        }));
        
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Fehlerbehandlung bei der Suche nach Kursbetreuern,
     * wenn eine Exception auftritt.
     */
    @Test
    void searchKursbetreuer_WhenExceptionOccurs_ShouldHandleExceptionAndReturnEmptyList() {
        // Arrange
        when(nutzerService.getAlleKursbetreuer()).thenThrow(new RuntimeException("Testfehler"));

        // Act
        String viewName = controller.searchKursbetreuer(null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/nutzerverwaltung/kursbetreuer-table :: .kursbetreuer-table", viewName);
        verify(model).addAttribute("kursbetreuer", Collections.emptyList());
        verify(model).addAttribute(eq("errorMessage"), contains("Fehler bei der Suche"));
    }
}
