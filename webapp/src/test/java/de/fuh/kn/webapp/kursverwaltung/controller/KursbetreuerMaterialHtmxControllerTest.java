package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-Tests für den KursbetreuerMaterialHtmxController.
 * Testet die Controller-Methoden mithilfe von MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerMaterialHtmxControllerTest {

    @Mock
    private KursMaterialService kursMaterialService;

    @Mock
    private KursService kursService;

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private Model model;

    @InjectMocks
    private KursbetreuerMaterialHtmxController controller;

    private MockMvc mockMvc;

    private KursDTO testKursDTO;
    private KurseinheitDTO testKurseinheitDTO;
    private KursMaterialDTO testKursMaterialDTO;
    private KursMaterialDTO testKurseinheitMaterialDTO;

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

        testKursMaterialDTO = new KursMaterialDTO();
        testKursMaterialDTO.setId(3L);
        testKursMaterialDTO.setName("Skript-1.pdf");
        testKursMaterialDTO.setKursId(1L);
        testKursMaterialDTO.setMimeType("application/pdf");
        testKursMaterialDTO.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
                
        testKurseinheitMaterialDTO = new KursMaterialDTO();
        testKurseinheitMaterialDTO.setId(4L);
        testKurseinheitMaterialDTO.setName("Übungsblatt.pdf");
        testKurseinheitMaterialDTO.setKurseinheitId(2L);
        testKurseinheitMaterialDTO.setMimeType("application/pdf");
        testKurseinheitMaterialDTO.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
    }

    @Test
    @DisplayName("deleteKursMaterial sollte Material löschen und Kursmaterial-Baum zurückgeben")
    void deleteKursMaterial_ShouldDeleteMaterialAndReturnTree_WhenMaterialBelongsToKurs() throws Exception {
        // Arrange
        when(kursMaterialService.getKursMaterialById(3L)).thenReturn(testKursMaterialDTO);
        doNothing().when(kursMaterialService).loescheKursMaterial(3L);
        when(kursService.getKursByIdMitKurseinheiten(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(delete("/kursbetreuer/htmx/kursmaterial/3"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("fragmentSuccessMessage"))
                .andExpect(model().attribute("kurs", testKursDTO))
                .andExpect(view().name("fragments/kursbetreuer/kursmaterial-tree :: kursmaterial-tree"));

        verify(kursMaterialService, times(1)).getKursMaterialById(3L);
        verify(kursMaterialService, times(1)).loescheKursMaterial(3L);
        verify(kursService, times(1)).getKursByIdMitKurseinheiten(1L);
    }
    
    @Test
    @DisplayName("deleteKursMaterial sollte Material löschen und Kursmaterial-Baum zurückgeben für Kurseinheit-Material")
    void deleteKursMaterial_ShouldDeleteMaterialAndReturnTree_WhenMaterialBelongsToKurseinheit() throws Exception {
        // Arrange
        when(kursMaterialService.getKursMaterialById(4L)).thenReturn(testKurseinheitMaterialDTO);
        doNothing().when(kursMaterialService).loescheKursMaterial(4L);
        when(kurseinheitService.getKurseinheitById(2L)).thenReturn(testKurseinheitDTO);
        when(kursService.getKursByIdMitKurseinheiten(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(delete("/kursbetreuer/htmx/kursmaterial/4"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("fragmentSuccessMessage"))
                .andExpect(model().attribute("kurs", testKursDTO))
                .andExpect(view().name("fragments/kursbetreuer/kursmaterial-tree :: kursmaterial-tree"));

        verify(kursMaterialService, times(1)).getKursMaterialById(4L);
        verify(kursMaterialService, times(1)).loescheKursMaterial(4L);
        verify(kurseinheitService, times(1)).getKurseinheitById(2L);
        verify(kursService, times(1)).getKursByIdMitKurseinheiten(1L);
    }
    
    @Test
    @DisplayName("deleteKursMaterial sollte nur Erfolgsmeldung zurückgeben, wenn Material keinem Kurs/keiner Kurseinheit zugeordnet ist")
    void deleteKursMaterial_ShouldReturnOnlySuccessMessage_WhenMaterialHasNoAssociation() throws Exception {
        // Arrange
        KursMaterialDTO materialOhneZuordnung = new KursMaterialDTO();
        materialOhneZuordnung.setId(5L);
        materialOhneZuordnung.setName("Ohne-Zuordnung.pdf");
        materialOhneZuordnung.setMimeType("application/pdf");
        materialOhneZuordnung.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
                
        when(kursMaterialService.getKursMaterialById(5L)).thenReturn(materialOhneZuordnung);
        doNothing().when(kursMaterialService).loescheKursMaterial(5L);

        // Act & Assert
        mockMvc.perform(delete("/kursbetreuer/htmx/kursmaterial/5"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("fragmentSuccessMessage"))
                .andExpect(view().name("fragments/messages :: successMessage"));

        verify(kursMaterialService, times(1)).getKursMaterialById(5L);
        verify(kursMaterialService, times(1)).loescheKursMaterial(5L);
        verify(kursService, never()).getKursByIdMitKurseinheiten(anyLong());
        verify(kurseinheitService, never()).getKurseinheitById(anyLong());
    }
    
    @Test
    @DisplayName("deleteKursMaterial sollte Fehlermeldung zurückgeben, wenn eine Exception auftritt")
    void deleteKursMaterial_ShouldReturnErrorMessage_WhenExceptionOccurs() throws Exception {
        // Arrange
        when(kursMaterialService.getKursMaterialById(3L)).thenReturn(testKursMaterialDTO);
        doThrow(new IllegalArgumentException("Testfehler")).when(kursMaterialService).loescheKursMaterial(3L);

        // Act & Assert
        mockMvc.perform(delete("/kursbetreuer/htmx/kursmaterial/3"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(view().name("fragments/messages :: errorMessage"));

        verify(kursMaterialService, times(1)).getKursMaterialById(3L);
        verify(kursMaterialService, times(1)).loescheKursMaterial(3L);
    }
    
    @Test
    @DisplayName("deleteKursMaterial direkt über Controller-Methode aufrufen sollte den korrekten View zurückgeben")
    void deleteKursMaterial_DirectCall_ShouldReturnCorrectView() {
        // Arrange
        when(kursMaterialService.getKursMaterialById(3L)).thenReturn(testKursMaterialDTO);
        doNothing().when(kursMaterialService).loescheKursMaterial(3L);
        when(kursService.getKursByIdMitKurseinheiten(1L)).thenReturn(testKursDTO);

        // Act
        String viewName = controller.deleteKursMaterial(3L, model);

        // Assert
        assertEquals("fragments/kursbetreuer/kursmaterial-tree :: kursmaterial-tree", viewName);
        verify(kursMaterialService).getKursMaterialById(3L);
        verify(kursMaterialService).loescheKursMaterial(3L);
        verify(kursService).getKursByIdMitKurseinheiten(1L);
        verify(model).addAttribute(eq("fragmentSuccessMessage"), anyString());
        verify(model).addAttribute("kurs", testKursDTO);
    }
    
    @Test
    @DisplayName("deleteKurseinheitMaterial sollte Material löschen und Kurseinheit-Material-Liste zurückgeben")
    void deleteKurseinheitMaterial_ShouldDeleteMaterialAndReturnList() throws Exception {
        // Arrange
        when(kursMaterialService.getKursMaterialById(4L)).thenReturn(testKurseinheitMaterialDTO);
        doNothing().when(kursMaterialService).loescheKursMaterial(4L);
        when(kurseinheitService.getKurseinheitById(2L)).thenReturn(testKurseinheitDTO);

        // Act & Assert
        mockMvc.perform(delete("/kursbetreuer/htmx/kurseinheit-material/4"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("fragmentSuccessMessage"))
                .andExpect(model().attribute("kurseinheit", testKurseinheitDTO))
                .andExpect(view().name("fragments/kursbetreuer/kurseinheit-material-list :: kurseinheit-material-list"));

        verify(kursMaterialService, times(1)).getKursMaterialById(4L);
        verify(kursMaterialService, times(1)).loescheKursMaterial(4L);
        verify(kurseinheitService, times(1)).getKurseinheitById(2L);
    }
    
    @Test
    @DisplayName("deleteKurseinheitMaterial sollte nur Erfolgsmeldung zurückgeben, wenn keine Kurseinheit gefunden wurde")
    void deleteKurseinheitMaterial_ShouldReturnOnlySuccessMessage_WhenNoKurseinheitFound() throws Exception {
        // Arrange
        KursMaterialDTO materialOhneKurseinheit = new KursMaterialDTO();
        materialOhneKurseinheit.setId(6L);
        materialOhneKurseinheit.setName("Ohne-Kurseinheit.pdf");
        materialOhneKurseinheit.setMimeType("application/pdf");
        materialOhneKurseinheit.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
                
        when(kursMaterialService.getKursMaterialById(6L)).thenReturn(materialOhneKurseinheit);
        doNothing().when(kursMaterialService).loescheKursMaterial(6L);

        // Act & Assert
        mockMvc.perform(delete("/kursbetreuer/htmx/kurseinheit-material/6"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("fragmentSuccessMessage"))
                .andExpect(view().name("fragments/messages :: successMessage"));

        verify(kursMaterialService, times(1)).getKursMaterialById(6L);
        verify(kursMaterialService, times(1)).loescheKursMaterial(6L);
        verify(kurseinheitService, never()).getKurseinheitById(anyLong());
    }
    
    @Test
    @DisplayName("deleteKurseinheitMaterial sollte Fehlermeldung zurückgeben, wenn eine Exception auftritt")
    void deleteKurseinheitMaterial_ShouldReturnErrorMessage_WhenExceptionOccurs() throws Exception {
        // Arrange
        when(kursMaterialService.getKursMaterialById(4L)).thenReturn(testKurseinheitMaterialDTO);
        doThrow(new IllegalArgumentException("Testfehler")).when(kursMaterialService).loescheKursMaterial(4L);

        // Act & Assert
        mockMvc.perform(delete("/kursbetreuer/htmx/kurseinheit-material/4"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(view().name("fragments/messages :: errorMessage"));

        verify(kursMaterialService, times(1)).getKursMaterialById(4L);
        verify(kursMaterialService, times(1)).loescheKursMaterial(4L);
    }
    
    @Test
    @DisplayName("deleteKurseinheitMaterial direkt über Controller-Methode aufrufen sollte den korrekten View zurückgeben")
    void deleteKurseinheitMaterial_DirectCall_ShouldReturnCorrectView() {
        // Arrange
        when(kursMaterialService.getKursMaterialById(4L)).thenReturn(testKurseinheitMaterialDTO);
        doNothing().when(kursMaterialService).loescheKursMaterial(4L);
        when(kurseinheitService.getKurseinheitById(2L)).thenReturn(testKurseinheitDTO);

        // Act
        String viewName = controller.deleteKurseinheitMaterial(4L, model);

        // Assert
        assertEquals("fragments/kursbetreuer/kurseinheit-material-list :: kurseinheit-material-list", viewName);
        verify(kursMaterialService).getKursMaterialById(4L);
        verify(kursMaterialService).loescheKursMaterial(4L);
        verify(kurseinheitService).getKurseinheitById(2L);
        verify(model).addAttribute(eq("fragmentSuccessMessage"), anyString());
        verify(model).addAttribute("kurseinheit", testKurseinheitDTO);
    }
}
