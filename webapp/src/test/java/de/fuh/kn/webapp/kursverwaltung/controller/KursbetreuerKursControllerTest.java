package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.kursbetreuung.dashboard.DashboardStatisticsDTO;
import de.fuh.kn.webapp.kursbetreuung.dashboard.DashboardStatisticsService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-Tests für den KursbetreuerKursController.
 * Testet die Controller-Methoden mithilfe von MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerKursControllerTest {

    @Mock
    private KursService kursService;

    @Mock
    private AktivitaetsService aktivitaetsService;
    
    @Mock
    private NutzerService nutzerService;
    
    @Mock
    private DashboardStatisticsService dashboardStatisticsService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private KursbetreuerKursController controller;

    private MockMvc mockMvc;

    private KursDTO testKursDTO1;
    private KursDTO testKursDTO2;
    private List<KursDTO> kursList;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Testdaten anlegen
        testKursDTO1 = new KursDTO();
        testKursDTO1.setId(1L);
        testKursDTO1.setName("Informatik Grundlagen");

        testKursDTO2 = new KursDTO();
        testKursDTO2.setId(2L);
        testKursDTO2.setName("Kommunikationsnetze");

        kursList = Arrays.asList(testKursDTO1, testKursDTO2);
    }

    @Test
    @DisplayName("showDashboard sollte alle Aktivitäten anzeigen")
    void showDashboard_ShouldDisplayAllCourses() throws Exception {
        // Arrange

        // Mock AktivitaetsService
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(java.util.Collections.emptyList());
        when(aktivitaetsService.getAlleAktivitaeten(anyInt(), anyInt())).thenReturn(aktivitaetenPage);
        
        // Mock NutzerService
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(1L);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        
        // Mock DashboardStatisticsService
        DashboardStatisticsDTO statistics = DashboardStatisticsDTO.builder()
                .anzahlLoesungsversuche(10)
                .anzahlChatNachrichten(20)
                .aiKostenGesamt(BigDecimal.valueOf(5.50))
                .anzahlAktiveNutzer(5)
                .build();
        when(dashboardStatisticsService.getDashboardStatistics()).thenReturn(statistics);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("aktivitaeten", aktivitaetenPage))
                .andExpect(model().attribute("nutzer", nutzerDTO))
                .andExpect(model().attribute("statistics", statistics))
                .andExpect(view().name("kursbetreuer/dashboard"));

        verify(aktivitaetsService, times(1)).getAlleAktivitaeten(anyInt(), anyInt());
        verify(nutzerService, times(1)).getAuthenticatedNutzer();
    }

    @Test
    @DisplayName("showDashboard sollte alle Aktivitäten im Model hinzufügen")
    void showDashboard_ShouldAddCoursesToModel() {
        // Arrange

        // Mock AktivitaetsService
        Page<AktivitaetDTO> aktivitaetenPage = new PageImpl<>(java.util.Collections.emptyList());
        when(aktivitaetsService.getAlleAktivitaeten(anyInt(), anyInt())).thenReturn(aktivitaetenPage);
        
        // Mock NutzerService
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(1L);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        
        // Mock DashboardStatisticsService
        DashboardStatisticsDTO statistics = DashboardStatisticsDTO.builder()
                .anzahlLoesungsversuche(10)
                .anzahlChatNachrichten(20)
                .aiKostenGesamt(BigDecimal.valueOf(5.50))
                .anzahlAktiveNutzer(5)
                .build();
        when(dashboardStatisticsService.getDashboardStatistics()).thenReturn(statistics);

        // Act
        String viewName = controller.showDashboard(model);

        // Assert
        assertEquals("kursbetreuer/dashboard", viewName);
        verify(aktivitaetsService).getAlleAktivitaeten(anyInt(), anyInt());
        verify(nutzerService).getAuthenticatedNutzer();
        verify(model).addAttribute("aktivitaeten", aktivitaetenPage);
        verify(model).addAttribute("nutzer", nutzerDTO);
        verify(model).addAttribute("statistics", statistics);
    }

    @Test
    @DisplayName("showKursDetails sollte die Kursdetails anzeigen")
    void showKursDetails_ShouldDisplayCourseDetails() throws Exception {
        // Arrange
        when(kursService.getKursByIdMitKurseinheiten(1L)).thenReturn(testKursDTO1);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurs", testKursDTO1))
                .andExpect(view().name("kursbetreuer/kurs/kurs-details"));

        verify(kursService, times(1)).getKursByIdMitKurseinheiten(1L);
    }

    @Test
    @DisplayName("showCreateKursForm sollte das Formular zum Erstellen eines Kurses anzeigen")
    void showCreateKursForm_ShouldDisplayCreateForm() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/neu"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("kurs"))
                .andExpect(model().attribute("isNew", true))
                .andExpect(view().name("kursbetreuer/kurs/kurs-form"));
    }

    @Test
    @DisplayName("showEditKursForm sollte das Formular zum Bearbeiten eines Kurses anzeigen")
    void showEditKursForm_ShouldDisplayEditForm() throws Exception {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO1);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/1/bearbeiten"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurs", testKursDTO1))
                .andExpect(model().attribute("isNew", false))
                .andExpect(view().name("kursbetreuer/kurs/kurs-form"));

        verify(kursService, times(1)).getKursById(1L);
    }

    @Test
    @DisplayName("saveKurs sollte einen neuen Kurs erstellen, wenn keine ID vorhanden ist")
    void saveKurs_ShouldCreateNewCourse_WhenNoId() throws Exception {
        // Arrange
        KursDTO newKursDTO = new KursDTO();
        newKursDTO.setName("Neuer Kurs");
        
        KursDTO savedKursDTO = new KursDTO();
        savedKursDTO.setId(3L);
        savedKursDTO.setName("Neuer Kurs");
        
        when(kursService.erstelleKurs(any(KursDTO.class))).thenReturn(savedKursDTO);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/kurse/speichern")
                .flashAttr("kurs", newKursDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/3"));

        verify(kursService, times(1)).erstelleKurs(any(KursDTO.class));
        verify(kursService, never()).aktualisiereKurs(any(KursDTO.class));
    }

    @Test
    @DisplayName("saveKurs sollte einen vorhandenen Kurs aktualisieren, wenn eine ID vorhanden ist")
    void saveKurs_ShouldUpdateExistingCourse_WhenIdPresent() throws Exception {
        // Arrange
        when(kursService.aktualisiereKurs(any(KursDTO.class))).thenReturn(testKursDTO1);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/kurse/speichern")
                .flashAttr("kurs", testKursDTO1))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/1"));

        verify(kursService, never()).erstelleKurs(any(KursDTO.class));
        verify(kursService, times(1)).aktualisiereKurs(any(KursDTO.class));
    }

    @Test
    @DisplayName("showDeleteConfirmation sollte die Löschbestätigungsseite anzeigen")
    void showDeleteConfirmation_ShouldDisplayDeleteConfirmation() throws Exception {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO1);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/1/loeschen"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurs", testKursDTO1))
                .andExpect(view().name("kursbetreuer/kurs/kurs-loeschen"));

        verify(kursService, times(1)).getKursById(1L);
    }

    @Test
    @DisplayName("deleteKursPost sollte einen Kurs löschen über POST-Methode")
    void deleteKursPost_ShouldDeleteCourse() throws Exception {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO1);
        doNothing().when(kursService).loescheKurs(1L);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/kurse/1/loeschen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kursService, times(1)).getKursById(1L);
        verify(kursService, times(1)).loescheKurs(1L);
    }

    @Test
    @DisplayName("showKursverwaltung sollte die Kursverwaltungsseite anzeigen")
    void showKursverwaltung_ShouldDisplayCourseManagement() throws Exception {
        // Arrange
        when(kursService.getAlleKurse()).thenReturn(kursList);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kursverwaltung"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurse", kursList))
                .andExpect(view().name("kursbetreuer/kurs/kursverwaltung"));

        verify(kursService, times(1)).getAlleKurse();
    }
}
