package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-Tests für die Drag & Drop Reorder-Funktionalität des KursbetreuerKurseinheitHtmxControllers.
 * Testet die Controller-Methoden zur Neuordnung von Kurseinheiten mithilfe von MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerKurseinheitHtmxControllerTest {

    @Mock
    private KursService kursService;

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private Model model;

    @InjectMocks
    private KursbetreuerKurseinheitHtmxController controller;

    private MockMvc mockMvc;

    private KursDTO testKursDTO;
    private KurseinheitDTO testKurseinheitDTO;
    private List<Long> testKurseinheitIds;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Testdaten anlegen
        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Informatik Grundlagen");

        testKurseinheitDTO = new KurseinheitDTO();
        testKurseinheitDTO.setId(2L);
        testKurseinheitDTO.setKursId(1L);
        testKurseinheitDTO.setName("Einführung in Java");
                
        testKurseinheitIds = Arrays.asList(5L, 2L, 7L, 3L);
    }

    @Test
    @DisplayName("reorderKurseinheiten sollte die Reihenfolge aktualisieren und Fragment zurückgeben")
    void reorderKurseinheiten_ShouldUpdateOrderAndReturnFragment() throws Exception {
        // Arrange
        List<KurseinheitDTO> aktualisierteKurseinheiten = Collections.singletonList(testKurseinheitDTO);
        when(kurseinheitService.aktualisiereKurseinheitenReihenfolge(eq(1L), anyList())).thenReturn(aktualisierteKurseinheiten);
        when(kursService.getKursByIdMitKurseinheiten(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/htmx/kurse/1/kurseinheiten/reorder")
                .param("ids", "5", "2", "7", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("successMessage"))
                .andExpect(model().attribute("kurs", testKursDTO))
                .andExpect(view().name("fragments/kursbetreuer/kurseinheiten-table :: kurseinheiten-table"));

        verify(kurseinheitService, times(1)).aktualisiereKurseinheitenReihenfolge(eq(1L), anyList());
        verify(kursService, times(1)).getKursByIdMitKurseinheiten(1L);
    }
    
    @Test
    @DisplayName("reorderKurseinheiten sollte Fehlermeldung zurückgeben, wenn keine IDs vorhanden sind")
    void reorderKurseinheiten_ShouldReturnErrorMessage_WhenNoIdsProvided() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/htmx/kurse/1/kurseinheiten/reorder")
                .param("ids", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(view().name("fragments/messages :: errorMessage"));

        verify(kurseinheitService, never()).aktualisiereKurseinheitenReihenfolge(anyLong(), anyList());
        verify(kursService, never()).getKursByIdMitKurseinheiten(anyLong());
    }
    
    @Test
    @DisplayName("reorderKurseinheiten sollte Fehlermeldung zurückgeben, wenn eine Exception auftritt")
    void reorderKurseinheiten_ShouldReturnErrorMessage_WhenExceptionOccurs() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Testfehler")).when(kurseinheitService).aktualisiereKurseinheitenReihenfolge(eq(1L), anyList());

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/htmx/kurse/1/kurseinheiten/reorder")
                .param("ids", "5", "2", "7", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(view().name("fragments/messages :: errorMessage"));

        verify(kurseinheitService, times(1)).aktualisiereKurseinheitenReihenfolge(eq(1L), anyList());
        verify(kursService, never()).getKursByIdMitKurseinheiten(anyLong());
    }
    
    @Test
    @DisplayName("reorderKurseinheiten direkt über Controller-Methode aufrufen sollte den korrekten View zurückgeben")
    void reorderKurseinheiten_DirectCall_ShouldReturnCorrectView() {
        // Arrange
        List<KurseinheitDTO> aktualisierteKurseinheiten = Collections.singletonList(testKurseinheitDTO);
        when(kurseinheitService.aktualisiereKurseinheitenReihenfolge(eq(1L), anyList())).thenReturn(aktualisierteKurseinheiten);
        when(kursService.getKursByIdMitKurseinheiten(1L)).thenReturn(testKursDTO);

        // Act
        String viewName = controller.reorderKurseinheiten(1L, testKurseinheitIds, model);

        // Assert
        assertEquals("fragments/kursbetreuer/kurseinheiten-table :: kurseinheiten-table", viewName);
        verify(kurseinheitService).aktualisiereKurseinheitenReihenfolge(eq(1L), anyList());
        verify(kursService).getKursByIdMitKurseinheiten(1L);
        verify(model).addAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("kurs", testKursDTO);
    }
}