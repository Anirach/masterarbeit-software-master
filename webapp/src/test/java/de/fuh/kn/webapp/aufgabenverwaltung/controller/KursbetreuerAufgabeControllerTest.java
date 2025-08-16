package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-Tests für den KursbetreuerAufgabeController.
 * Testet die Controller-Methoden mithilfe von MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerAufgabeControllerTest {

    @Mock
    private AufgabeService aufgabeService;

    @Mock
    private TeilaufgabeService teilaufgabeService;

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private KursService kursService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private KursbetreuerAufgabeController controller;

    private MockMvc mockMvc;

    private KurseinheitDTO testKurseinheitDTO;
    private KursDTO testKursDTO;
    private AufgabeDto testAufgabeDto;
    private TeilaufgabeDto testTeilaufgabeDto;
    private List<TeilaufgabeDto> teilaufgaben;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Testdaten für Kurseinheit und Kurs
        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Programmierung");

        testKurseinheitDTO = new KurseinheitDTO();
        testKurseinheitDTO.setId(1L);
        testKurseinheitDTO.setName("Einführung in Java");
        testKurseinheitDTO.setReihenfolge(1);
        testKurseinheitDTO.setKursId(1L);

        // Testdaten für Teilaufgabe
        testTeilaufgabeDto = new TeilaufgabeDto();
        testTeilaufgabeDto.setId(1L);
        testTeilaufgabeDto.setAufgabenstellungMarkdown("Testaufgabenstellung");
        testTeilaufgabeDto.setReihenfolge(1);
        testTeilaufgabeDto.setAufgabeId(1L);

        teilaufgaben = new ArrayList<>();
        teilaufgaben.add(testTeilaufgabeDto);

        // Testdaten für Aufgabe
        testAufgabeDto = new AufgabeDto();
        testAufgabeDto.setId(1L);
        testAufgabeDto.setTitel("Testaufgabe");
        testAufgabeDto.setKurseinheitId(1L);
        testAufgabeDto.setEinfach(true);
        testAufgabeDto.setReihenfolge(1);
        testAufgabeDto.setTeilaufgaben(teilaufgaben);
    }

    @Test
    @DisplayName("createNewAufgabe sollte eine neue Aufgabe mit Platzhalter-Titel erstellen und zur Bearbeitungsseite weiterleiten")
    void createNewAufgabe_ShouldCreateAndRedirectToEdit() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO);
        
        AufgabeDto neuAufgabe = new AufgabeDto();
        neuAufgabe.setId(2L);
        neuAufgabe.setTitel("Neue Aufgabe");
        neuAufgabe.setKurseinheitId(1L);
        
        // Verify the markdown field is set
        when(aufgabeService.erstelleAufgabe(argThat(dto -> 
            dto.getTeilaufgaben() != null && 
            !dto.getTeilaufgaben().isEmpty() && 
            dto.getTeilaufgaben().get(0).getAufgabenstellungMarkdown() != null
        ))).thenReturn(neuAufgabe);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/1/aufgaben/neu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/aufgaben/2/bearbeiten"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(aufgabeService, times(1)).erstelleAufgabe(any(AufgabeDto.class));
    }

    @Test
    @DisplayName("createNewAufgabe sollte zur Kursverwaltung umleiten wenn Kurseinheit nicht existiert")
    void createNewAufgabe_ShouldRedirect_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/999/aufgaben/neu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kurseinheitService, times(1)).getKurseinheitById(999L);
        verify(aufgabeService, never()).erstelleAufgabe(any(AufgabeDto.class));
    }

    @Test
    @DisplayName("showEditAufgabeForm sollte das Bearbeitungsformular anzeigen")
    void showEditAufgabeForm_ShouldDisplayEditForm() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(1L)).thenReturn(testAufgabeDto);
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/aufgaben/1/bearbeiten"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("aufgabe", testAufgabeDto))
                .andExpect(model().attribute("kurseinheit", testKurseinheitDTO))
                .andExpect(model().attribute("kurs", testKursDTO))
                .andExpect(model().attribute("isNew", false))
                .andExpect(view().name("kursbetreuer/aufgabe/aufgabe-bearbeiten"));

        verify(aufgabeService, times(1)).getAufgabeById(1L);
        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(kursService, times(1)).getKursById(1L);
    }

    @Test
    @DisplayName("showEditAufgabeForm sollte zur Kursverwaltung umleiten wenn Aufgabe nicht existiert")
    void showEditAufgabeForm_ShouldRedirect_WhenAufgabeNotExists() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/aufgaben/999/bearbeiten"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(aufgabeService, times(1)).getAufgabeById(999L);
        verify(kurseinheitService, never()).getKurseinheitById(anyLong());
        verify(kursService, never()).getKursById(anyLong());
    }

    @Test
    @DisplayName("showEditAufgabeForm sollte zur Kursverwaltung umleiten wenn Kurseinheit nicht existiert")
    void showEditAufgabeForm_ShouldRedirect_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(1L)).thenReturn(testAufgabeDto);
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/aufgaben/1/bearbeiten"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(aufgabeService, times(1)).getAufgabeById(1L);
        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
        verify(kursService, never()).getKursById(anyLong());
    }

    @Test
    @DisplayName("saveAufgabe sollte eine neue Aufgabe erstellen")
    void saveAufgabe_ShouldCreateNewAufgabe() throws Exception {
        // Arrange
        AufgabeDto neueAufgabe = new AufgabeDto();
        neueAufgabe.setTitel("Neue Aufgabe");
        neueAufgabe.setEinfach(true);
        neueAufgabe.setKurseinheitId(1L);

        AufgabeDto gespeicherteAufgabe = new AufgabeDto();
        gespeicherteAufgabe.setId(2L);
        gespeicherteAufgabe.setTitel("Neue Aufgabe");
        gespeicherteAufgabe.setEinfach(true);
        gespeicherteAufgabe.setKurseinheitId(1L);

        when(aufgabeService.erstelleAufgabe(any(AufgabeDto.class))).thenReturn(gespeicherteAufgabe);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/aufgaben/speichern")
                .flashAttr("aufgabe", neueAufgabe))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/aufgaben/2/bearbeiten"));

        verify(aufgabeService, times(1)).erstelleAufgabe(any(AufgabeDto.class));
        verify(aufgabeService, never()).aktualisiereAufgabe(any(AufgabeDto.class));
    }

    @Test
    @DisplayName("saveAufgabe sollte eine bestehende Aufgabe aktualisieren")
    void saveAufgabe_ShouldUpdateExistingAufgabe() throws Exception {
        // Arrange
        when(aufgabeService.aktualisiereAufgabe(any(AufgabeDto.class))).thenReturn(testAufgabeDto);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/aufgaben/speichern")
                .flashAttr("aufgabe", testAufgabeDto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/aufgaben/1/bearbeiten"));

        verify(aufgabeService, never()).erstelleAufgabe(any(AufgabeDto.class));
        verify(aufgabeService, times(1)).aktualisiereAufgabe(any(AufgabeDto.class));
    }


    @Test
    @DisplayName("showDeleteConfirmation sollte die Bestätigungsseite anzeigen")
    void showDeleteConfirmation_ShouldDisplayConfirmationPage() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(1L)).thenReturn(testAufgabeDto);
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/aufgaben/1/loeschen"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("aufgabe", testAufgabeDto))
                .andExpect(model().attribute("kurseinheit", testKurseinheitDTO))
                .andExpect(view().name("kursbetreuer/aufgabe/aufgabe-loeschen"));

        verify(aufgabeService, times(1)).getAufgabeById(1L);
        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
    }

    @Test
    @DisplayName("showDeleteConfirmation sollte zur Kursverwaltung umleiten wenn Aufgabe nicht existiert")
    void showDeleteConfirmation_ShouldRedirect_WhenAufgabeNotExists() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/aufgaben/999/loeschen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(aufgabeService, times(1)).getAufgabeById(999L);
        verify(kurseinheitService, never()).getKurseinheitById(anyLong());
    }

    @Test
    @DisplayName("showDeleteConfirmation sollte zur Kursverwaltung umleiten wenn Kurseinheit nicht existiert")
    void showDeleteConfirmation_ShouldRedirect_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(1L)).thenReturn(testAufgabeDto);
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/aufgaben/1/loeschen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(aufgabeService, times(1)).getAufgabeById(1L);
        verify(kurseinheitService, times(1)).getKurseinheitById(1L);
    }

    @Test
    @DisplayName("deleteAufgabePost sollte eine Aufgabe löschen, wenn bestätigt")
    void deleteAufgabePost_ShouldDeleteAufgabe_WhenConfirmed() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(1L)).thenReturn(testAufgabeDto);
        doNothing().when(aufgabeService).loescheAufgabe(1L);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/aufgaben/1/loeschen")
                .param("confirmDelete", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurseinheiten/1"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(aufgabeService, times(1)).getAufgabeById(1L);
        verify(aufgabeService, times(1)).loescheAufgabe(1L);
    }
    
    @Test
    @DisplayName("deleteAufgabePost sollte zur Löschbestätigung umleiten, wenn nicht bestätigt")
    void deleteAufgabePost_ShouldRedirectToConfirmation_WhenNotConfirmed() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(1L)).thenReturn(testAufgabeDto);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/aufgaben/1/loeschen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/aufgaben/1/loeschen"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(aufgabeService, times(1)).getAufgabeById(1L);
        verify(aufgabeService, never()).loescheAufgabe(anyLong());
    }

    @Test
    @DisplayName("deleteAufgabePost sollte zur Kursverwaltung umleiten wenn Aufgabe nicht existiert")
    void deleteAufgabePost_ShouldRedirect_WhenAufgabeNotExists() throws Exception {
        // Arrange
        when(aufgabeService.getAufgabeById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/kursbetreuer/aufgaben/999/loeschen")
                .param("confirmDelete", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(aufgabeService, times(1)).getAufgabeById(999L);
        verify(aufgabeService, never()).loescheAufgabe(anyLong());
    }

    // Zusätzliche Tests für die direkten Methodenaufrufe

    @Test
    @DisplayName("createNewAufgabe sollte direkt eine Aufgabe erstellen und zur Bearbeitungsseite weiterleiten")
    void createNewAufgabe_ShouldCreateAndRedirectDirectly() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(1L)).thenReturn(testKurseinheitDTO);
        
        AufgabeDto neuAufgabe = new AufgabeDto();
        neuAufgabe.setId(2L);
        neuAufgabe.setTitel("Neue Aufgabe");
        neuAufgabe.setKurseinheitId(1L);
        
        // Verify the markdown field is set
        when(aufgabeService.erstelleAufgabe(argThat(dto -> 
            dto.getTeilaufgaben() != null && 
            !dto.getTeilaufgaben().isEmpty() && 
            dto.getTeilaufgaben().get(0).getAufgabenstellungMarkdown() != null
        ))).thenReturn(neuAufgabe);

        // Act
        String redirectUrl = controller.createNewAufgabe(1L, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/aufgaben/2/bearbeiten", redirectUrl);
        verify(kurseinheitService).getKurseinheitById(1L);
        verify(aufgabeService).erstelleAufgabe(any(AufgabeDto.class));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    @DisplayName("saveAufgabe sollte Erfolgsmeldung setzen und umleiten")
    void saveAufgabe_ShouldSetSuccessMessageAndRedirect() {
        // Arrange
        AufgabeDto gespeicherteAufgabe = new AufgabeDto();
        gespeicherteAufgabe.setId(1L);
        gespeicherteAufgabe.setTitel("Testaufgabe");

        when(aufgabeService.erstelleAufgabe(any(AufgabeDto.class))).thenReturn(gespeicherteAufgabe);

        AufgabeDto neueAufgabe = new AufgabeDto();
        neueAufgabe.setTitel("Testaufgabe");

        // Act
        String redirectUrl = controller.saveAufgabe(neueAufgabe, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/aufgaben/1/bearbeiten", redirectUrl);
        verify(aufgabeService).erstelleAufgabe(neueAufgabe);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

}