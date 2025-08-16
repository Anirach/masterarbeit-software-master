package de.fuh.kn.webapp.nutzerverwaltung.aktivitaeten;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den AktivitaetenHtmxController.
 * Testet die HTMX-basierte Filterung und Paginierung von Aktivitäten.
 */
@ExtendWith(MockitoExtension.class)
class AktivitaetenHtmxControllerTest {

    @Mock
    private AktivitaetsService aktivitaetsService;

    @Mock
    private NutzerService nutzerService;

    @Mock
    private Model model;

    @InjectMocks
    private AktivitaetenHtmxController controller;

    private List<AktivitaetDTO> testAktivitaeten;
    private NutzerDTO testNutzer;
    private StudentDTO testStudent;
    private AktivitaetFilterDTO filter;

    @BeforeEach
    void setUp() {
        // Test-Nutzer erstellen
        testNutzer = new NutzerDTO();
        testNutzer.setId(1L);
        testNutzer.setVorname("Admin");
        testNutzer.setNachname("User");
        testNutzer.setDisplayName("Admin User");

        testStudent = new StudentDTO();
        testStudent.setId(2L);
        testStudent.setVorname("Student");
        testStudent.setNachname("Test");
        testStudent.setDisplayName("Student Test");

        // Test-Aktivitäten erstellen
        testAktivitaeten = new ArrayList<>();
        
        AktivitaetDTO aktivitaet1 = new AktivitaetDTO();
        aktivitaet1.setId(1L);
        aktivitaet1.setNutzerId(1L);
        aktivitaet1.setNutzerName("Admin User");
        aktivitaet1.setAktivitaetsTyp(AktivitaetsTyp.LOGIN);
        aktivitaet1.setBeschreibung("Admin hat sich angemeldet");
        aktivitaet1.setZeitpunkt(LocalDateTime.now().minusDays(1));
        aktivitaet1.setErfolg(true);
        
        AktivitaetDTO aktivitaet2 = new AktivitaetDTO();
        aktivitaet2.setId(2L);
        aktivitaet2.setNutzerId(2L);
        aktivitaet2.setNutzerName("Student Test");
        aktivitaet2.setAktivitaetsTyp(AktivitaetsTyp.REGISTRIEREN);
        aktivitaet2.setBeschreibung("Student hat sich registriert");
        aktivitaet2.setZeitpunkt(LocalDateTime.now().minusDays(2));
        aktivitaet2.setErfolg(true);
        
        testAktivitaeten.add(aktivitaet1);
        testAktivitaeten.add(aktivitaet2);
        
        // Standard-Filter erstellen
        filter = new AktivitaetFilterDTO();
        filter.setPage(0);
        filter.setSize(10);
    }

    /**
     * Testet die Filterung von Aktivitäten für einen Kursbetreuer ohne spezifische Filter.
     * Sollte alle Aktivitäten zurückgeben, wenn showAllUsers aktiviert ist.
     */
    @Test
    void filterAktivitaeten_AsKursbetreuer_WithShowAllUsers_ShouldReturnAllAktivitaeten() {
        // Arrange
        filter.setShowAllUsers(true);
        
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(testAktivitaeten);
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(aktivitaetsService.getFilteredAktivitaeten(
                isNull(), isNull(), eq(0), eq(10), any()))
                .thenReturn(aktivitaetenPage);
        
        // Act
        String viewName = controller.filterAktivitaeten(filter, model);
        
        // Assert
        assertEquals("fragments/aktivitaeten-htmx :: aktivitaeten-content", viewName);
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(model).addAttribute("showAllUsers", true);
        verify(model).addAttribute("showPagination", true);
        verify(aktivitaetsService).getFilteredAktivitaeten(
                isNull(), isNull(), eq(0), eq(10), any());
    }

    /**
     * Testet die Filterung von Aktivitäten für einen Kursbetreuer mit dem Filter
     * für einen bestimmten Nutzer.
     */
    @Test
    void filterAktivitaeten_AsKursbetreuer_WithNutzerId_ShouldReturnNutzerAktivitaeten() {
        // Arrange
        filter.setNutzerId(2L);
        filter.setShowAllUsers(false);
        
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(List.of(testAktivitaeten.get(1)));
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(aktivitaetsService.getFilteredAktivitaetenByNutzerId(
                eq(2L), isNull(), isNull(), eq(0), eq(10), any()))
                .thenReturn(aktivitaetenPage);
        
        // Act
        String viewName = controller.filterAktivitaeten(filter, model);
        
        // Assert
        assertEquals("fragments/aktivitaeten-htmx :: aktivitaeten-content", viewName);
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(model).addAttribute("showAllUsers", false);
        verify(model).addAttribute("nutzerId", 2L);
        verify(aktivitaetsService).getFilteredAktivitaetenByNutzerId(
                eq(2L), isNull(), isNull(), eq(0), eq(10), any());
    }

    /**
     * Testet die Filterung von Aktivitäten mit einem Aktivitätstyp.
     */
    @Test
    void filterAktivitaeten_WithAktivitaetsTyp_ShouldFilterByType() {
        // Arrange
        filter.setAktivitaetsTyp(AktivitaetsTyp.LOGIN.name());
        filter.setShowAllUsers(true);
        
        // Filter würde nur die LOGIN-Aktivität zurückgeben
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(List.of(testAktivitaeten.get(0)));
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(aktivitaetsService.getFilteredAktivitaeten(
                eq(AktivitaetsTyp.LOGIN), isNull(), eq(0), eq(10), any()))
                .thenReturn(aktivitaetenPage);
        
        // Act
        String viewName = controller.filterAktivitaeten(filter, model);
        
        // Assert
        assertEquals("fragments/aktivitaeten-htmx :: aktivitaeten-content", viewName);
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(aktivitaetsService).getFilteredAktivitaeten(
                eq(AktivitaetsTyp.LOGIN), isNull(), eq(0), eq(10), any());
    }

    /**
     * Testet die Filterung von Aktivitäten mit einem Datum-Filter.
     */
    @Test
    void filterAktivitaeten_WithDateFilter_ShouldApplyDateSpecification() {
        // Arrange
        filter.setStartDatum(LocalDate.now().minusDays(3).toString());
        filter.setEndDatum(LocalDate.now().toString());
        filter.setShowAllUsers(true);
        
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(testAktivitaeten);
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(aktivitaetsService.getFilteredAktivitaeten(
                isNull(), isNull(), eq(0), eq(10), any()))
                .thenReturn(aktivitaetenPage);
        
        // Act
        String viewName = controller.filterAktivitaeten(filter, model);
        
        // Assert
        assertEquals("fragments/aktivitaeten-htmx :: aktivitaeten-content", viewName);
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(aktivitaetsService).getFilteredAktivitaeten(
                isNull(), isNull(), eq(0), eq(10), any());
    }

    /**
     * Testet die Filterung von Aktivitäten für einen Studenten,
     * der versucht, Aktivitäten von anderen Nutzern zu sehen.
     * Sollte zu einer AccessDeniedException führen.
     */
    @Test
    void filterAktivitaeten_AsStudent_TryingToAccessOtherUserData_ShouldThrowAccessDeniedException() {
        // Arrange
        filter.setShowAllUsers(true);  // Student versucht alle Nutzer zu sehen
        filter.setNutzerId(1L);        // Student will Daten von Nutzer 1 sehen, ist aber selbst Nutzer 2
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        
        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            controller.filterAktivitaeten(filter, model);
        });
        
        verify(nutzerService).getAuthenticatedNutzer();
        verifyNoInteractions(aktivitaetsService);
    }

    /**
     * Testet die Filterung von Aktivitäten für einen Studenten,
     * der nur seine eigenen Aktivitäten abfragt (erlaubt).
     */
    @Test
    void filterAktivitaeten_AsStudent_AccessingOwnData_ShouldSucceed() {
        // Arrange
        filter.setShowAllUsers(false);
        filter.setNutzerId(2L);  // ID des Test-Studenten
        
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(List.of(testAktivitaeten.get(1)));
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(aktivitaetsService.getFilteredAktivitaetenByNutzerId(
                eq(2L), isNull(), isNull(), eq(0), eq(10), any()))
                .thenReturn(aktivitaetenPage);
        
        // Act
        String viewName = controller.filterAktivitaeten(filter, model);
        
        // Assert
        assertEquals("fragments/aktivitaeten-htmx :: aktivitaeten-content", viewName);
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(model).addAttribute("showAllUsers", false);
        verify(model).addAttribute("nutzerId", 2L);
        verify(aktivitaetsService).getFilteredAktivitaetenByNutzerId(
                eq(2L), isNull(), isNull(), eq(0), eq(10), any());
    }

    /**
     * Testet die Filterung von Aktivitäten mit Suchbegriff.
     */
    @Test
    void filterAktivitaeten_WithSearchTerm_ShouldApplySearch() {
        // Arrange
        filter.setSuchbegriff("Admin");
        filter.setShowAllUsers(true);
        
        // Filter würde nur die Aktivität mit "Admin" zurückgeben
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(List.of(testAktivitaeten.get(0)));
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(aktivitaetsService.getFilteredAktivitaeten(
                isNull(), eq("Admin"), eq(0), eq(10), any()))
                .thenReturn(aktivitaetenPage);
        
        // Act
        String viewName = controller.filterAktivitaeten(filter, model);
        
        // Assert
        assertEquals("fragments/aktivitaeten-htmx :: aktivitaeten-content", viewName);
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(aktivitaetsService).getFilteredAktivitaeten(
                isNull(), eq("Admin"), eq(0), eq(10), any());
    }

    /**
     * Testet die Paginierung von Aktivitäten.
     */
    @Test
    void filterAktivitaeten_WithPagination_ShouldUseCorrectPageAndSize() {
        // Arrange
        filter.setPage(1);
        filter.setSize(5);
        filter.setShowAllUsers(true);
        
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(testAktivitaeten);
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(aktivitaetsService.getFilteredAktivitaeten(
                isNull(), isNull(), eq(1), eq(5), any()))
                .thenReturn(aktivitaetenPage);
        
        // Act
        String viewName = controller.filterAktivitaeten(filter, model);
        
        // Assert
        assertEquals("fragments/aktivitaeten-htmx :: aktivitaeten-content", viewName);
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(aktivitaetsService).getFilteredAktivitaeten(
                isNull(), isNull(), eq(1), eq(5), any());
    }
}