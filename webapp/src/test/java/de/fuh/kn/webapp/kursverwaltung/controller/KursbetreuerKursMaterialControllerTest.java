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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-Tests für den KursbetreuerKursMaterialController.
 * Testet die Controller-Methoden mithilfe von MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerKursMaterialControllerTest {

    @Mock
    private KursMaterialService kursMaterialService;

    @Mock
    private KursService kursService;

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private KursbetreuerKursMaterialController controller;

    private MockMvc mockMvc;

    private KursDTO testKursDTO;
    private KurseinheitDTO testKurseinheitDTO;
    private KursMaterialDTO testKursMaterialDTO;
    private MockMultipartFile testFile;

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

        // Mock-Datei für Tests erstellen
        testFile = new MockMultipartFile(
                "file",
                "test-dokument.pdf",
                "application/pdf",
                "Testinhalt".getBytes()
        );
    }

    @Test
    @DisplayName("showKursMaterialUploadForm sollte das Upload-Formular für Kursmaterial anzeigen")
    void showKursMaterialUploadForm_ShouldDisplayUploadForm() throws Exception {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/1/material/neu"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kursId", 1L))
                .andExpect(model().attribute("kursName", "Informatik Grundlagen"))
                .andExpect(model().attribute("uploadTarget", "kurs"))
                .andExpect(view().name("kursbetreuer/kursmaterial/material-upload"));

        verify(kursService, times(1)).getKursById(1L);
    }
    
    @Test
    @DisplayName("showKursMaterialUploadForm sollte zur Kursverwaltung umleiten, wenn Kurs nicht existiert")
    void showKursMaterialUploadForm_ShouldRedirectToKursverwaltung_WhenKursNotExists() throws Exception {
        // Arrange
        when(kursService.getKursById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurse/999/material/neu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kursService, times(1)).getKursById(999L);
    }

    @Test
    @DisplayName("showKurseinheitMaterialUploadForm sollte das Upload-Formular für Kurseinheitmaterial anzeigen")
    void showKurseinheitMaterialUploadForm_ShouldDisplayUploadForm() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(2L)).thenReturn(testKurseinheitDTO);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/2/material/neu"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("kurseinheitId", 2L))
                .andExpect(model().attribute("kurseinheitName", "Einführung in Java"))
                .andExpect(model().attribute("kursId", 1L))
                .andExpect(model().attribute("kursName", "Informatik Grundlagen"))
                .andExpect(model().attribute("uploadTarget", "kurseinheit"))
                .andExpect(view().name("kursbetreuer/kursmaterial/material-upload"));

        verify(kurseinheitService, times(1)).getKurseinheitById(2L);
        verify(kursService, times(1)).getKursById(1L);
    }
    
    @Test
    @DisplayName("showKurseinheitMaterialUploadForm sollte zur Kursverwaltung umleiten, wenn Kurseinheit nicht existiert")
    void showKurseinheitMaterialUploadForm_ShouldRedirectToKursverwaltung_WhenKurseinheitNotExists() throws Exception {
        // Arrange
        when(kurseinheitService.getKurseinheitById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/kursbetreuer/kurseinheiten/999/material/neu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kursverwaltung"));

        verify(kurseinheitService, times(1)).getKurseinheitById(999L);
    }

    @Test
    @DisplayName("uploadKursMaterial sollte erfolgreich Material hochladen")
    void uploadKursMaterial_ShouldUploadMaterial_Successfully() throws Exception {
        // Arrange
        when(kursMaterialService.kursDateiHochladen(eq(1L), any(MockMultipartFile.class)))
                .thenReturn(testKursMaterialDTO);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurse/1/material/upload")
                .file(testFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/1"))
                .andExpect(flash().attributeExists("fragmentSuccessMessage"));

        verify(kursMaterialService, times(1)).kursDateiHochladen(eq(1L), any(MockMultipartFile.class));
        verify(kursService, times(1)).getKursById(1L);
    }
    
    @Test
    @DisplayName("uploadKursMaterial sollte Fehler anzeigen, wenn keine Datei ausgewählt wurde")
    void uploadKursMaterial_ShouldShowError_WhenNoFileSelected() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "",
                "application/pdf",
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurse/1/material/upload")
                .file(emptyFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/1/material/neu"))
                .andExpect(flash().attribute("errorMessage", "Bitte wählen Sie eine Datei aus."));

        verify(kursMaterialService, never()).kursDateiHochladen(anyLong(), any(MockMultipartFile.class));
        verify(kursService, never()).getKursById(anyLong());
    }
    
    @Test
    @DisplayName("uploadKursMaterial sollte Fehler behandeln, wenn IOException auftritt")
    void uploadKursMaterial_ShouldHandleIOException() throws Exception {
        // Arrange
        when(kursMaterialService.kursDateiHochladen(eq(1L), any(MockMultipartFile.class)))
                .thenThrow(new IOException("Testfehler"));

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurse/1/material/upload")
                .file(testFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/1/material/neu"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(kursMaterialService, times(1)).kursDateiHochladen(eq(1L), any(MockMultipartFile.class));
        verify(kursService, never()).getKursById(anyLong());
    }
    
    @Test
    @DisplayName("uploadKursMaterial sollte Fehler behandeln, wenn IllegalArgumentException auftritt")
    void uploadKursMaterial_ShouldHandleIllegalArgumentException() throws Exception {
        // Arrange
        when(kursMaterialService.kursDateiHochladen(eq(1L), any(MockMultipartFile.class)))
                .thenThrow(new IllegalArgumentException("Dateityp nicht erlaubt"));

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurse/1/material/upload")
                .file(testFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurse/1/material/neu"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(kursMaterialService, times(1)).kursDateiHochladen(eq(1L), any(MockMultipartFile.class));
        verify(kursService, never()).getKursById(anyLong());
    }

    @Test
    @DisplayName("uploadKurseinheitMaterial sollte erfolgreich Material hochladen")
    void uploadKurseinheitMaterial_ShouldUploadMaterial_Successfully() throws Exception {
        // Arrange
        KursMaterialDTO kurseinheitMaterialDTO = new KursMaterialDTO();
        kurseinheitMaterialDTO.setId(4L);
        kurseinheitMaterialDTO.setName("Übungsblatt.pdf");
        kurseinheitMaterialDTO.setKurseinheitId(2L);
        kurseinheitMaterialDTO.setMimeType("application/pdf");
        kurseinheitMaterialDTO.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
                
        when(kursMaterialService.kurseinheitDateiHochladen(eq(2L), any(MockMultipartFile.class)))
                .thenReturn(kurseinheitMaterialDTO);
        when(kurseinheitService.getKurseinheitById(2L)).thenReturn(testKurseinheitDTO);

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurseinheiten/2/material/upload")
                .file(testFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurseinheiten/2"))
                .andExpect(flash().attributeExists("fragmentSuccessMessage"));

        verify(kursMaterialService, times(1)).kurseinheitDateiHochladen(eq(2L), any(MockMultipartFile.class));
        verify(kurseinheitService, times(1)).getKurseinheitById(2L);
    }
    
    @Test
    @DisplayName("uploadKurseinheitMaterial sollte Fehler anzeigen, wenn keine Datei ausgewählt wurde")
    void uploadKurseinheitMaterial_ShouldShowError_WhenNoFileSelected() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "",
                "application/pdf",
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurseinheiten/2/material/upload")
                .file(emptyFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurseinheiten/2/material/neu"))
                .andExpect(flash().attribute("errorMessage", "Bitte wählen Sie eine Datei aus."));

        verify(kursMaterialService, never()).kurseinheitDateiHochladen(anyLong(), any(MockMultipartFile.class));
        verify(kurseinheitService, never()).getKurseinheitById(anyLong());
    }
    
    @Test
    @DisplayName("uploadKurseinheitMaterial sollte Fehler behandeln, wenn IOException auftritt")
    void uploadKurseinheitMaterial_ShouldHandleIOException() throws Exception {
        // Arrange
        when(kursMaterialService.kurseinheitDateiHochladen(eq(2L), any(MockMultipartFile.class)))
                .thenThrow(new IOException("Testfehler"));

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurseinheiten/2/material/upload")
                .file(testFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurseinheiten/2/material/neu"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(kursMaterialService, times(1)).kurseinheitDateiHochladen(eq(2L), any(MockMultipartFile.class));
        verify(kurseinheitService, never()).getKurseinheitById(anyLong());
    }
    
    @Test
    @DisplayName("uploadKurseinheitMaterial sollte Fehler behandeln, wenn IllegalArgumentException auftritt")
    void uploadKurseinheitMaterial_ShouldHandleIllegalArgumentException() throws Exception {
        // Arrange
        when(kursMaterialService.kurseinheitDateiHochladen(eq(2L), any(MockMultipartFile.class)))
                .thenThrow(new IllegalArgumentException("Dateityp nicht erlaubt"));

        // Act & Assert
        mockMvc.perform(multipart("/kursbetreuer/kurseinheiten/2/material/upload")
                .file(testFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kursbetreuer/kurseinheiten/2/material/neu"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(kursMaterialService, times(1)).kurseinheitDateiHochladen(eq(2L), any(MockMultipartFile.class));
        verify(kurseinheitService, never()).getKurseinheitById(anyLong());
    }
    
    // Zusätzliche Tests für die Direktaufruf-Methoden
    
    @Test
    @DisplayName("showKursMaterialUploadForm sollte direkt Kurs-Attribute zum Model hinzufügen")
    void showKursMaterialUploadForm_ShouldAddKursAttributesToModel() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act
        String viewName = controller.showKursMaterialUploadForm(1L, model);

        // Assert
        assertEquals("kursbetreuer/kursmaterial/material-upload", viewName);
        verify(kursService).getKursById(1L);
        verify(model).addAttribute("kursId", 1L);
        verify(model).addAttribute("kursName", "Informatik Grundlagen");
        verify(model).addAttribute("uploadTarget", "kurs");
    }
    
    @Test
    @DisplayName("showKurseinheitMaterialUploadForm sollte direkt Kurseinheit-Attribute zum Model hinzufügen")
    void showKurseinheitMaterialUploadForm_ShouldAddKurseinheitAttributesToModel() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(2L)).thenReturn(testKurseinheitDTO);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act
        String viewName = controller.showKurseinheitMaterialUploadForm(2L, model);

        // Assert
        assertEquals("kursbetreuer/kursmaterial/material-upload", viewName);
        verify(kurseinheitService).getKurseinheitById(2L);
        verify(kursService).getKursById(1L);
        verify(model).addAttribute("kurseinheitId", 2L);
        verify(model).addAttribute("kurseinheitName", "Einführung in Java");
        verify(model).addAttribute("kursId", 1L);
        verify(model).addAttribute("kursName", "Informatik Grundlagen");
        verify(model).addAttribute("uploadTarget", "kurseinheit");
    }
    
    @Test
    @DisplayName("uploadKursMaterial sollte Erfolgsattribute setzen und auf Kursseite umleiten")
    void uploadKursMaterial_ShouldSetSuccessAttributesAndRedirectToKursPage() throws Exception {
        // Arrange
        when(kursMaterialService.kursDateiHochladen(eq(1L), any()))
                .thenReturn(testKursMaterialDTO);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act
        String redirectUrl = controller.uploadKursMaterial(1L, testFile, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/kurse/1", redirectUrl);
        verify(kursMaterialService).kursDateiHochladen(eq(1L), any());
        verify(redirectAttributes).addFlashAttribute(eq("fragmentSuccessMessage"), anyString());
        verify(kursService).getKursById(1L);
        verify(model).addAttribute("kurs", testKursDTO);
    }
    
    @Test
    @DisplayName("uploadKurseinheitMaterial sollte Erfolgsattribute setzen und auf Kurseinheitseite umleiten")
    void uploadKurseinheitMaterial_ShouldSetSuccessAttributesAndRedirectToKurseinheitPage() throws Exception {
        // Arrange
        KursMaterialDTO kurseinheitMaterialDTO = new KursMaterialDTO();
        kurseinheitMaterialDTO.setId(4L);
        kurseinheitMaterialDTO.setName("Übungsblatt.pdf");
        kurseinheitMaterialDTO.setKurseinheitId(2L);
        kurseinheitMaterialDTO.setMimeType("application/pdf");
        kurseinheitMaterialDTO.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
                
        when(kursMaterialService.kurseinheitDateiHochladen(eq(2L), any()))
                .thenReturn(kurseinheitMaterialDTO);
        when(kurseinheitService.getKurseinheitById(2L)).thenReturn(testKurseinheitDTO);

        // Act
        String redirectUrl = controller.uploadKurseinheitMaterial(2L, testFile, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/kurseinheiten/2", redirectUrl);
        verify(kursMaterialService).kurseinheitDateiHochladen(eq(2L), any());
        verify(redirectAttributes).addFlashAttribute(eq("fragmentSuccessMessage"), anyString());
        verify(kurseinheitService).getKurseinheitById(2L);
        verify(model).addAttribute("kurseinheit", testKurseinheitDTO);
    }
}
