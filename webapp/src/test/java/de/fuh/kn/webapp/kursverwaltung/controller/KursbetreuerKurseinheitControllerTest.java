package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-Tests für den KursbetreuerKurseinheitController.
 * Testet die Controller-Methoden mithilfe von MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerKurseinheitControllerTest {

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private KursService kursService;
    
    @Mock
    private AufgabeService aufgabeService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private KursbetreuerKurseinheitController controller;

    private MockMvc mockMvc;

    private KursDTO testKursDTO;
    private KurseinheitDTO testKurseinheitDTO1;
    private KurseinheitDTO testKurseinheitDTO2;
    private List<KurseinheitDTO> testKurseinheiten;
    private List<KursMaterialDTO> testKursMaterialien;
    private List<AufgabeDto> testAufgaben;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Testdaten für einen Kurs mit zwei Kurseinheiten
        testKurseinheitDTO1 = new KurseinheitDTO();
        testKurseinheitDTO1.setId(1L);
        testKurseinheitDTO1.setName("Einführung in Java");
        testKurseinheitDTO1.setReihenfolge(1);
        testKurseinheitDTO1.setKursId(1L);

        testKurseinheitDTO2 = new KurseinheitDTO();
        testKurseinheitDTO2.setId(2L);
        testKurseinheitDTO2.setName("Fortgeschrittene Java-Konzepte");
        testKurseinheitDTO2.setReihenfolge(2);
        testKurseinheitDTO2.setKursId(1L);

        testKurseinheiten = Arrays.asList(testKurseinheitDTO1, testKurseinheitDTO2);

        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Programmierung");
        testKursDTO.setKurseinheiten(testKurseinheiten);

        // Testdaten für Kursmaterialien
        KursMaterialDTO material1 = new KursMaterialDTO();
        material1.setId(1L);
        material1.setName("Java-Grundlagen.pdf");
        material1.setMimeType("application/pdf");
        material1.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
        material1.setKurseinheitId(1L);

        KursMaterialDTO material2 = new KursMaterialDTO();
        material2.setId(2L);
        material2.setName("Java-Logo.png");
        material2.setMimeType("image/png");
        material2.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);
        material2.setKurseinheitId(1L);

        testKursMaterialien = Arrays.asList(material1, material2);
        
        // Testdaten für Aufgaben
        AufgabeDto aufgabe1 = new AufgabeDto();
        aufgabe1.setId(1L);
        aufgabe1.setTitel("Testaufgabe 1");
        aufgabe1.setKurseinheitId(1L);
        aufgabe1.setReihenfolge(1);
        
        AufgabeDto aufgabe2 = new AufgabeDto();
        aufgabe2.setId(2L);
        aufgabe2.setTitel("Testaufgabe 2");
        aufgabe2.setKurseinheitId(1L);
        aufgabe2.setReihenfolge(2);
        
        testAufgaben = Arrays.asList(aufgabe1, aufgabe2);
    }

    @Test
    @DisplayName("showCreateKurseinheitForm sollte das Formular für eine neue Kurseinheit anzeigen")
    void showCreateKurseinheitForm_ShouldDisplayNewForm() throws Exception {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/1/kurseinheiten/neu"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("kurseinheit"))
                .andExpect(model().attribute("kursName", "Programmierung"))
                .andExpect(model().attribute("isNew", true))
                .andExpect(view().name("kursbetreuer/kurseinheit/kurseinheit-form"));

        verify(kursService, times(1)).getKursById(1L);
    }

    @Test
    @DisplayName("showCreateKurseinheitForm sollte zur Kursverwaltung umleiten wenn Kurs nicht existiert")
    void showCreateKurseinheitForm_ShouldRedirect_WhenKursNotExists() throws Exception {
        // Arrange
        when(kursService.getKursById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/999/kurseinheiten/neu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kursService, times(1)).getKursById(999L);
    }

    @Test
    @DisplayName("showEditKurseinheitForm sollte das Bearbeitungsformular anzeigen")
    void showEditKurseinheitForm_ShouldDisplayEditForm() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        when(kurseinheitService.getKursNameByKurseinheitId(1L)).thenReturn("Programmierung");

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/1/bearbeiten"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurseinheit", testKurseinheitDTO1))
                .andExpect(model().attribute("kursName", "Programmierung"))
                .andExpect(model().attribute("isNew", false))
                .andExpect(view().name("kursbetreuer/kurseinheit/kurseinheit-form"));

        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(kurseinheitService, times(1)).getKursNameByKurseinheitId(1L);
    }

    @Test
    @DisplayName("showEditKurseinheitForm sollte zur Kursverwaltung umleiten wenn Kurseinheit nicht existiert")
    void showEditKurseinheitForm_ShouldRedirect_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/999/bearbeiten"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kurseinheitService, times(1)).getKurseinheitById(999L);
        verify(kurseinheitService, never()).getKursNameByKurseinheitId(anyLong());
    }

    @Test
    @DisplayName("saveKurseinheit sollte eine neue Kurseinheit erstellen")
    void saveKurseinheit_ShouldCreateNewKurseinheit() throws Exception {
        // Arrange
        KurseinheitDTO neueKurseinheit = new KurseinheitDTO();
        neueKurseinheit.setName("Neue Kurseinheit");
        neueKurseinheit.setReihenfolge(3);
        neueKurseinheit.setKursId(1L);

        when(kurseinheitService.erstelleKurseinheit(any(KurseinheitDTO.class))).thenReturn(neueKurseinheit);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/kurseinheiten/speichern")
                .flashAttr("kurseinheit", neueKurseinheit))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/1"));

        verify(kurseinheitService, times(1)).erstelleKurseinheit(any(KurseinheitDTO.class));
        verify(kurseinheitService, never()).aktualisiereKurseinheit(any(KurseinheitDTO.class));
    }

    @Test
    @DisplayName("saveKurseinheit sollte eine bestehende Kurseinheit aktualisieren")
    void saveKurseinheit_ShouldUpdateExistingKurseinheit() throws Exception {
        // Arrange
        KurseinheitDTO aktualisiertesDTO = new KurseinheitDTO();
        aktualisiertesDTO.setId(1L);
        aktualisiertesDTO.setName("Aktualisierte Kurseinheit");
        aktualisiertesDTO.setReihenfolge(1);
        aktualisiertesDTO.setKursId(1L);

        when(kurseinheitService.aktualisiereKurseinheit(any(KurseinheitDTO.class))).thenReturn(aktualisiertesDTO);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/kurseinheiten/speichern")
                .flashAttr("kurseinheit", aktualisiertesDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurseinheiten/1"));

        verify(kurseinheitService, never()).erstelleKurseinheit(any(KurseinheitDTO.class));
        verify(kurseinheitService, times(1)).aktualisiereKurseinheit(any(KurseinheitDTO.class));
    }

    @Test
    @DisplayName("showDeleteConfirmation sollte die Bestätigungsseite anzeigen")
    void showDeleteConfirmation_ShouldDisplayConfirmationPage() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        when(kurseinheitService.getKursNameByKurseinheitId(1L)).thenReturn("Programmierung");

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/1/loeschen"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurseinheit", testKurseinheitDTO1))
                .andExpect(model().attribute("kursName", "Programmierung"))
                .andExpect(view().name("kursbetreuer/kurseinheit/kurseinheit-loeschen"));

        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(kurseinheitService, times(1)).getKursNameByKurseinheitId(1L);
    }

    @Test
    @DisplayName("showDeleteConfirmation sollte zur Kursverwaltung umleiten wenn Kurseinheit nicht existiert")
    void showDeleteConfirmation_ShouldRedirect_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/999/loeschen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kurseinheitService, times(1)).getKurseinheitById(999L);
        verify(kurseinheitService, never()).getKursNameByKurseinheitId(anyLong());
    }

    @Test
    @DisplayName("deleteKurseinheitPost sollte eine Kurseinheit löschen")
    void deleteKurseinheitPost_ShouldDeleteKurseinheit() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        doNothing().when(kurseinheitService).loescheKurseinheit(1L);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/kurseinheiten/1/loeschen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/1"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(kurseinheitService, times(1)).loescheKurseinheit(1L);
    }

    @Test
    @DisplayName("deleteKurseinheitPost sollte zur Kursverwaltung umleiten wenn Kurseinheit nicht existiert")
    void deleteKurseinheitPost_ShouldRedirect_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/kurseinheiten/999/loeschen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kurseinheitService, times(1)).getKurseinheitById(999L);
        verify(kurseinheitService, never()).loescheKurseinheit(anyLong());
    }

    @Test
    @DisplayName("showKurseinheitDetails sollte die Detailansicht anzeigen")
    void showKurseinheitDetails_ShouldDisplayDetailsPage() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(kurseinheitService.getKursMaterialienByKurseinheitId(1L)).thenReturn(testKursMaterialien);
        when(aufgabeService.getAufgabenByKurseinheitId(1L)).thenReturn(testAufgaben);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurseinheit", testKurseinheitDTO1))
                .andExpect(model().attribute("kurs", testKursDTO))
                .andExpect(model().attribute("kursMaterialien", testKursMaterialien))
                .andExpect(model().attribute("aufgaben", testAufgaben))
                .andExpect(view().name("kursbetreuer/kurseinheit/kurseinheit-details"));

        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(kursService, times(1)).getKursById(1L);
        verify(kurseinheitService, times(1)).getKursMaterialienByKurseinheitId(1L);
        verify(aufgabeService, times(1)).getAufgabenByKurseinheitId(1L);
    }

    @Test
    @DisplayName("showKurseinheitDetails sollte zur Kursverwaltung umleiten wenn Kurseinheit nicht existiert")
    void showKurseinheitDetails_ShouldRedirect_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kurseinheitService, times(1)).getKurseinheitById(999L);
        verify(kursService, never()).getKursById(anyLong());
        verify(kurseinheitService, never()).getKursMaterialienByKurseinheitId(anyLong());
    }

    @Test
    @DisplayName("showKurseinheitDetails sollte zur Kursverwaltung umleiten wenn Kurs nicht existiert")
    void showKurseinheitDetails_ShouldRedirect_WhenKursNotExists() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        when(kursService.getKursById(1L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(kursService, times(1)).getKursById(1L);
        verify(kurseinheitService, never()).getKursMaterialienByKurseinheitId(anyLong());
    }

    // Zusätzliche Tests für die direkten Methodenaufrufe

    @Test
    @DisplayName("showCreateKurseinheitForm sollte direkt Attribute zum Model hinzufügen")
    void showCreateKurseinheitForm_ShouldAddAttributesToModel() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act
        String viewName = controller.showCreateKurseinheitForm(1L, model);

        // Assert
        assertEquals("kursbetreuer/kurseinheit/kurseinheit-form", viewName);
        verify(kursService).getKursById(1L);
        verify(model).addAttribute(eq("kurseinheit"), any(KurseinheitDTO.class));
        verify(model).addAttribute("kursName", "Programmierung");
        verify(model).addAttribute("isNew", true);
    }

    @Test
    @DisplayName("showEditKurseinheitForm sollte direkt Attribute zum Model hinzufügen")
    void showEditKurseinheitForm_ShouldAddAttributesToModel() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        when(kurseinheitService.getKursNameByKurseinheitId(1L)).thenReturn("Programmierung");

        // Act
        String viewName = controller.showEditKurseinheitForm(1L, model);

        // Assert
        assertEquals("kursbetreuer/kurseinheit/kurseinheit-form", viewName);
        verify(kurseinheitService).getKurseinheitById(1L);
        verify(kurseinheitService).getKursNameByKurseinheitId(1L);
        verify(model).addAttribute("kurseinheit", testKurseinheitDTO1);
        verify(model).addAttribute("kursName", "Programmierung");
        verify(model).addAttribute("isNew", false);
    }

    @Test
    @DisplayName("saveKurseinheit sollte bei neuer Kurseinheit auf Kursseite umleiten")
    void saveKurseinheit_ShouldRedirectToKursPage_WhenNewKurseinheit() {
        // Arrange
        KurseinheitDTO neueKurseinheit = new KurseinheitDTO();
        neueKurseinheit.setName("Neue Kurseinheit");
        neueKurseinheit.setReihenfolge(3);
        neueKurseinheit.setKursId(1L);

        KurseinheitDTO erstellteKurseinheit = new KurseinheitDTO();
        erstellteKurseinheit.setId(3L);
        erstellteKurseinheit.setName("Neue Kurseinheit");
        erstellteKurseinheit.setReihenfolge(3);
        erstellteKurseinheit.setKursId(1L);

        when(kurseinheitService.erstelleKurseinheit(neueKurseinheit)).thenReturn(erstellteKurseinheit);

        // Act
        String redirectUrl = controller.saveKurseinheit(neueKurseinheit, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/kurse/1", redirectUrl);
        verify(kurseinheitService).erstelleKurseinheit(neueKurseinheit);
        verify(kurseinheitService, never()).aktualisiereKurseinheit(any());
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("kurseinheit", erstellteKurseinheit);
    }

    @Test
    @DisplayName("saveKurseinheit sollte bei existierender Kurseinheit auf Kurseinheitseite umleiten")
    void saveKurseinheit_ShouldRedirectToKurseinheitPage_WhenExistingKurseinheit() {
        // Arrange
        KurseinheitDTO aktualisierteKurseinheit = new KurseinheitDTO();
        aktualisierteKurseinheit.setId(1L);
        aktualisierteKurseinheit.setName("Aktualisierte Kurseinheit");
        aktualisierteKurseinheit.setReihenfolge(1);
        aktualisierteKurseinheit.setKursId(1L);

        when(kurseinheitService.aktualisiereKurseinheit(aktualisierteKurseinheit)).thenReturn(aktualisierteKurseinheit);

        // Act
        String redirectUrl = controller.saveKurseinheit(aktualisierteKurseinheit, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/kurseinheiten/1", redirectUrl);
        verify(kurseinheitService, never()).erstelleKurseinheit(any());
        verify(kurseinheitService).aktualisiereKurseinheit(aktualisierteKurseinheit);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    @DisplayName("showDeleteConfirmation sollte direkt Attribute zum Model hinzufügen")
    void showDeleteConfirmation_ShouldAddAttributesToModel() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        when(kurseinheitService.getKursNameByKurseinheitId(1L)).thenReturn("Programmierung");

        // Act
        String viewName = controller.showDeleteConfirmation(1L, model);

        // Assert
        assertEquals("kursbetreuer/kurseinheit/kurseinheit-loeschen", viewName);
        verify(kurseinheitService).getKurseinheitById(1L);
        verify(kurseinheitService).getKursNameByKurseinheitId(1L);
        verify(model).addAttribute("kurseinheit", testKurseinheitDTO1);
        verify(model).addAttribute("kursName", "Programmierung");
    }

    @Test
    @DisplayName("deleteKurseinheitPost sollte Erfolgsmeldung setzen und zur Kursseite umleiten")
    void deleteKurseinheitPost_ShouldSetSuccessMessageAndRedirectToKursPage() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        doNothing().when(kurseinheitService).loescheKurseinheit(1L);

        // Act
        String redirectUrl = controller.deleteKurseinheitPost(1L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/kurse/1", redirectUrl);
        verify(kurseinheitService).getKurseinheitById(1L);
        verify(kurseinheitService).loescheKurseinheit(1L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("kurseinheit", testKurseinheitDTO1);
    }

    @Test
    @DisplayName("showKurseinheitDetails sollte direkt Attribute zum Model hinzufügen")
    void showKurseinheitDetails_ShouldAddAttributesToModel() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO1);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(kurseinheitService.getKursMaterialienByKurseinheitId(1L)).thenReturn(testKursMaterialien);
        when(aufgabeService.getAufgabenByKurseinheitId(1L)).thenReturn(testAufgaben);

        // Act
        String viewName = controller.showKurseinheitDetails(1L, model);

        // Assert
        assertEquals("kursbetreuer/kurseinheit/kurseinheit-details", viewName);
        verify(kurseinheitService).getKurseinheitById(1L);
        verify(kursService).getKursById(1L);
        verify(kurseinheitService).getKursMaterialienByKurseinheitId(1L);
        verify(aufgabeService).getAufgabenByKurseinheitId(1L);
        verify(model).addAttribute("kurseinheit", testKurseinheitDTO1);
        verify(model).addAttribute("kurs", testKursDTO);
        verify(model).addAttribute("kursMaterialien", testKursMaterialien);
        verify(model).addAttribute("aufgaben", testAufgaben);
    }
}