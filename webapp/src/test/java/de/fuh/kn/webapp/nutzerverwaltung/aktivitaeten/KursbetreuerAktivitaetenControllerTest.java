package de.fuh.kn.webapp.nutzerverwaltung.aktivitaeten;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testklasse für den KursbetreuerAktivitaetenController.
 * Testet die Controller-Methoden zur Anzeige von Aktivitäten für Kursbetreuer.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerAktivitaetenControllerTest {

    @Mock
    private AktivitaetsService aktivitaetsService;

    @Mock
    private Model model;

    @InjectMocks
    private KursbetreuerAktivitaetenController controller;

    // Testdaten
    private List<AktivitaetDTO> testAktivitaeten;
    private Page<AktivitaetDTO> testAktivitaetenPage;
    private AktivitaetFilterDTO testFilter;

    @BeforeEach
    void setUp() {
        // Erstellen der Test-Aktivitäten
        testAktivitaeten = new ArrayList<>();
        
        AktivitaetDTO aktivitaet1 = new AktivitaetDTO();
        aktivitaet1.setId(1L);
        aktivitaet1.setNutzerId(1L);
        aktivitaet1.setNutzerName("Max Mustermann");
        aktivitaet1.setAktivitaetsTyp(de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp.LOGIN);
        aktivitaet1.setBeschreibung("Benutzer hat sich eingeloggt");
        aktivitaet1.setZeitpunkt(LocalDateTime.now().minusDays(1));
        aktivitaet1.setErfolg(true);
        
        AktivitaetDTO aktivitaet2 = new AktivitaetDTO();
        aktivitaet2.setId(2L);
        aktivitaet2.setNutzerId(2L);
        aktivitaet2.setNutzerName("Maria Musterfrau");
        aktivitaet2.setAktivitaetsTyp(de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp.KURS_BEARBEITEN);
        aktivitaet2.setBeschreibung("Kurs wurde bearbeitet");
        aktivitaet2.setZeitpunkt(LocalDateTime.now().minusHours(2));
        aktivitaet2.setErfolg(true);
        aktivitaet2.setReferenzTyp("Kurs");
        aktivitaet2.setReferenzId(5L);
        aktivitaet2.setReferenzName("Programmierung 1");
        
        testAktivitaeten.add(aktivitaet1);
        testAktivitaeten.add(aktivitaet2);
        
        // Paginierte Aktivitäten erstellen
        testAktivitaetenPage = new PageImpl<>(testAktivitaeten, 
                                            PageRequest.of(0, 10), 
                                            testAktivitaeten.size());
        
        // Test-Filter erstellen
        testFilter = new AktivitaetFilterDTO();
        testFilter.setShowAllUsers(true);
        testFilter.setPage(0);
        testFilter.setSize(10);
    }

    /**
     * Testet die Anzeige aller Aktivitäten im System.
     * Prüft, ob die Methode den richtigen View zurückgibt und die erwarteten Attribute
     * zum Model hinzugefügt werden.
     */
    @Test
    void zeigeAlleAktivitaeten_ShouldReturnCorrectViewAndAddAttributes() {
        // Arrange
        when(aktivitaetsService.getAlleAktivitaeten(anyInt(), anyInt())).thenReturn(testAktivitaetenPage);

        // Act
        String viewName = controller.zeigeAlleAktivitaeten(testFilter, model);

        // Assert
        assertEquals("kursbetreuer/aktivitaeten/aktivitaeten", viewName);
        verify(model).addAttribute("aktivitaeten", testAktivitaetenPage);
        verify(model).addAttribute("showAllUsers", true);
        verify(model).addAttribute("nutzerId", null);
        verify(model).addAttribute("filter", testFilter);
        verify(aktivitaetsService).getAlleAktivitaeten(testFilter.getPage(), testFilter.getSize());
    }

    /**
     * Testet, dass beim Aufruf ohne Filter ein neuer Filter erstellt wird
     * mit korrekten Standardwerten.
     */
    @Test
    void zeigeAlleAktivitaeten_WithNullFilter_ShouldCreateDefaultFilter() {
        // Arrange
        when(aktivitaetsService.getAlleAktivitaeten(anyInt(), anyInt())).thenReturn(testAktivitaetenPage);

        // Act
        String viewName = controller.zeigeAlleAktivitaeten(null, model);

        // Assert
        assertEquals("kursbetreuer/aktivitaeten/aktivitaeten", viewName);
        
        // Capture und verifiziere, dass ein neuer Filter mit Standardwerten erstellt wurde
        verify(model).addAttribute(eq("filter"), any(AktivitaetFilterDTO.class));
        verify(model).addAttribute("showAllUsers", true);
        verify(model).addAttribute("nutzerId", null);
        verify(aktivitaetsService).getAlleAktivitaeten(0, 10); // Standardwerte
    }

    /**
     * Testet, dass der übergebene Filter korrekt verwendet wird.
     */
    @Test
    void zeigeAlleAktivitaeten_WithCustomFilter_ShouldUseProvidedFilter() {
        // Arrange
        // Angepasste Filter-Werte
        AktivitaetFilterDTO customFilter = new AktivitaetFilterDTO();
        customFilter.setShowAllUsers(true);
        customFilter.setPage(2);
        customFilter.setSize(5);
        customFilter.setSuchbegriff("login");
        customFilter.setAktivitaetsTyp("LOGIN");
        
        when(aktivitaetsService.getAlleAktivitaeten(eq(2), eq(5))).thenReturn(testAktivitaetenPage);

        // Act
        String viewName = controller.zeigeAlleAktivitaeten(customFilter, model);

        // Assert
        assertEquals("kursbetreuer/aktivitaeten/aktivitaeten", viewName);
        verify(model).addAttribute("filter", customFilter);
        verify(aktivitaetsService).getAlleAktivitaeten(2, 5); // Benutzerdefinierte Werte
    }
}