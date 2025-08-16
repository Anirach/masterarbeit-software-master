package de.fuh.kn.webapp.nutzerverwaltung.belegung;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den BelegungenHtmxController.
 * Testet die HTMX-basierte Suche und Filterung von Kursbelegungen.
 */
@ExtendWith(MockitoExtension.class)
class BelegungenHtmxControllerTest {

    @Mock
    private BelegungService belegungService;

    @Mock
    private KursService kursService;

    @Mock
    private Model model;

    @InjectMocks
    private BelegungenHtmxController controller;

    private KursDTO testKursDTO;
    private List<BelegungDTO> testBelegungen;

    @BeforeEach
    void setUp() {
        // Test-Objekte erstellen
        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Testkurs");

        testBelegungen = new ArrayList<>();
        
        // Aktive Belegung
        BelegungDTO belegung1 = new BelegungDTO();
        belegung1.setId(1L);
        belegung1.setKursId(1L);
        belegung1.setStudentId(1L);
        belegung1.setMatrikelnummer("12345678");
        belegung1.setStudentName("Max Mustermann");
        belegung1.setKursName("Testkurs");
        belegung1.setStartDatum(LocalDate.now().minusDays(10));
        belegung1.setEndDatum(LocalDate.now().plusDays(100));
        belegung1.setAktiv(true);
        
        // Inaktive Belegung
        BelegungDTO belegung2 = new BelegungDTO();
        belegung2.setId(2L);
        belegung2.setKursId(1L);
        belegung2.setStudentId(2L);
        belegung2.setMatrikelnummer("87654321");
        belegung2.setStudentName("Anna Schmidt");
        belegung2.setKursName("Testkurs");
        belegung2.setStartDatum(LocalDate.now().minusDays(100));
        belegung2.setEndDatum(LocalDate.now().minusDays(10));
        belegung2.setAktiv(false);
        
        testBelegungen.add(belegung1);
        testBelegungen.add(belegung2);
    }

    /**
     * Testet die Suche nach Belegungen ohne Filter, was alle Belegungen zurückgeben sollte.
     */
    @Test
    void searchBelegungen_WithoutFilters_ShouldReturnAllBelegungen() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(testBelegungen);

        // Act
        String viewName = controller.searchBelegungen(1L, null, null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        verify(model).addAttribute("belegungen", testBelegungen);
        verify(model).addAttribute("kursId", 1L);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }

    /**
     * Testet die Suche nach Belegungen mit dem Filter "nur aktive",
     * was nur aktive Belegungen zurückgeben sollte.
     */
    @Test
    void searchBelegungen_WithActiveFilter_ShouldReturnOnlyActiveBelegungen() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(testBelegungen);

        // Act
        String viewName = controller.searchBelegungen(1L, null, true, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        
        // Capture argument to verify filtering
        verify(model).addAttribute(eq("belegungen"), argThat(list -> {
            List<BelegungDTO> belegungen = (List<BelegungDTO>) list;
            return belegungen.size() == 1 && belegungen.get(0).isAktiv();
        }));
        
        verify(model).addAttribute("kursId", 1L);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }

    /**
     * Testet die Suche nach Belegungen mit einem Suchbegriff für den Studentennamen,
     * was nur Belegungen mit passenden Namen zurückgeben sollte.
     */
    @Test
    void searchBelegungen_WithNameSearch_ShouldReturnMatchingBelegungen() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(testBelegungen);

        // Act
        String viewName = controller.searchBelegungen(1L, "anna", null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        
        // Capture argument to verify filtering
        verify(model).addAttribute(eq("belegungen"), argThat(list -> {
            List<BelegungDTO> belegungen = (List<BelegungDTO>) list;
            return belegungen.size() == 1 && belegungen.get(0).getStudentName().toLowerCase().contains("anna");
        }));
        
        verify(model).addAttribute("kursId", 1L);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }

    /**
     * Testet die Suche nach Belegungen mit einem Suchbegriff für die Matrikelnummer,
     * was nur Belegungen mit passenden Matrikelnummern zurückgeben sollte.
     */
    @Test
    void searchBelegungen_WithMatrikelnummerSearch_ShouldReturnMatchingBelegungen() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(testBelegungen);

        // Act
        String viewName = controller.searchBelegungen(1L, "1234", null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        
        // Capture argument to verify filtering
        verify(model).addAttribute(eq("belegungen"), argThat(list -> {
            List<BelegungDTO> belegungen = (List<BelegungDTO>) list;
            return belegungen.size() == 1 && belegungen.get(0).getMatrikelnummer().contains("1234");
        }));
        
        verify(model).addAttribute("kursId", 1L);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }

    /**
     * Testet die Suche mit dem Statuswort "aktiv",
     * was nur aktive Belegungen zurückgeben sollte.
     */
    @Test
    void searchBelegungen_WithStatusSearchAktiv_ShouldReturnActiveBelegungen() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(testBelegungen);

        // Act
        String viewName = controller.searchBelegungen(1L, "aktiv", null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        
        // Capture argument to verify filtering
        verify(model).addAttribute(eq("belegungen"), argThat(list -> {
            List<BelegungDTO> belegungen = (List<BelegungDTO>) list;
            return belegungen.size() == 1 && belegungen.get(0).isAktiv();
        }));
        
        verify(model).addAttribute("kursId", 1L);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }

    /**
     * Testet die Suche mit dem Statuswort "inaktiv",
     * was nur inaktive Belegungen zurückgeben sollte.
     */
    @Test
    void searchBelegungen_WithStatusSearchInaktiv_ShouldReturnInactiveBelegungen() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(testBelegungen);

        // Act
        String viewName = controller.searchBelegungen(1L, "inaktiv", null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        
        // Capture argument to verify filtering
        verify(model).addAttribute(eq("belegungen"), argThat(list -> {
            List<BelegungDTO> belegungen = (List<BelegungDTO>) list;
            return belegungen.size() == 1 && !belegungen.get(0).isAktiv();
        }));
        
        verify(model).addAttribute("kursId", 1L);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }

    /**
     * Testet die Suche mit kombiniertem Filter für aktive Belegungen und Suchbegriff,
     * was nur aktive Belegungen mit passendem Namen zurückgeben sollte.
     */
    @Test
    void searchBelegungen_WithActiveFilterAndSearch_ShouldCombineBothFilters() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(testBelegungen);

        // Füge eine weitere inaktive Belegung hinzu, um die Kombination der Filter zu testen
        BelegungDTO belegung3 = new BelegungDTO();
        belegung3.setId(3L);
        belegung3.setKursId(1L);
        belegung3.setStudentId(3L);
        belegung3.setMatrikelnummer("98765432");
        belegung3.setStudentName("Max Weber");
        belegung3.setKursName("Testkurs");
        belegung3.setStartDatum(LocalDate.now().minusDays(100));
        belegung3.setEndDatum(LocalDate.now().minusDays(10));
        belegung3.setAktiv(false);
        
        List<BelegungDTO> erweiterteTestBelegungen = new ArrayList<>(testBelegungen);
        erweiterteTestBelegungen.add(belegung3);
        
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(erweiterteTestBelegungen);

        // Act - Suche nach "Max" und nur aktive Belegungen
        String viewName = controller.searchBelegungen(1L, "Max", true, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        
        // Capture argument to verify filtering
        verify(model).addAttribute(eq("belegungen"), argThat(list -> {
            List<BelegungDTO> belegungen = (List<BelegungDTO>) list;
            return belegungen.size() == 1 && // Zwei Max-Einträge, aber nur einer ist aktiv
                   belegungen.stream().allMatch(b -> b.isAktiv() && b.getStudentName().contains("Max"));
        }));
        
        verify(model).addAttribute("kursId", 1L);
        verify(model, never()).addAttribute(eq("errorMessage"), anyString());
    }

    /**
     * Testet die Suche nach Belegungen, wenn kein Kurs gefunden wird,
     * was zu einer Exception führen sollte.
     */
    @Test
    void searchBelegungen_WhenKursNotFound_ShouldHandleExceptionAndReturnEmptyList() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(null);

        // Act
        String viewName = controller.searchBelegungen(1L, null, null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        verify(model).addAttribute("belegungen", Collections.emptyList());
        verify(model).addAttribute("kursId", 1L);
        verify(model).addAttribute(eq("errorMessage"), contains("Kurs mit ID 1 nicht gefunden"));
    }

    /**
     * Testet die Suche nach Belegungen, wenn ein unerwarteter Fehler auftritt,
     * was zu einer Exception führen sollte.
     */
    @Test
    void searchBelegungen_WhenGenericExceptionOccurs_ShouldHandleExceptionAndReturnEmptyList() {
        // Arrange
        when(kursService.getKursById(1L)).thenThrow(new RuntimeException("Testfehler"));

        // Act
        String viewName = controller.searchBelegungen(1L, null, null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        verify(model).addAttribute("belegungen", Collections.emptyList());
        verify(model).addAttribute("kursId", 1L);
        verify(model).addAttribute(eq("errorMessage"), contains("Fehler bei der Suche"));
    }

    /**
     * Testet die Suche nach Belegungen, wenn der BelegungService eine Exception wirft,
     * was zu einer Exception führen sollte.
     */
    @Test
    void searchBelegungen_WhenBelegungServiceThrowsException_ShouldHandleException() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenThrow(new NoSuchElementException("Test-Exception"));

        // Act
        String viewName = controller.searchBelegungen(1L, null, null, model);

        // Assert
        assertEquals("fragments/kursbetreuer/belegungen-table :: .belegungen-table", viewName);
        verify(model).addAttribute("belegungen", Collections.emptyList());
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
}