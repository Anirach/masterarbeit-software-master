package de.fuh.kn.webapp.nutzerverwaltung.controller;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.auth.KursbetreuerUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den KursbetreuerNutzerController.
 * Testet die verschiedenen Controller-Methoden zur Verwaltung von Nutzern (Studenten und Kursbetreuer).
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerNutzerControllerTest {

    @Mock
    private NutzerService nutzerService;

    @Mock
    private BelegungService belegungService;

    @Mock
    private KursService kursService;
    
    @Mock
    private AktivitaetsService aktivitaetsService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private UserDetails userDetails;

    @Mock
    private KursbetreuerUserDetails kursbetreuerUserDetails;

    @InjectMocks
    private KursbetreuerNutzerController controller;

    @Captor
    private ArgumentCaptor<LocalDate> startDateCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> endDateCaptor;

    // Testdaten
    private StudentDTO testStudentDTO;
    private KursbetreuerDTO testKursbetreuerDTO;
    private List<StudentDTO> testStudenten;
    private List<KursbetreuerDTO> testKursbetreuer;
    private BelegungDTO testBelegungDTO;
    private KursDTO testKursDTO;
    private List<BelegungDTO> testBelegungen;
    private List<KursDTO> testKurse;

    @BeforeEach
    void setUp() {
        // Student-Testdaten
        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setMatrikelnummer("12345678");
        testStudentDTO.setVorname("Max");
        testStudentDTO.setNachname("Mustermann");
        testStudentDTO.setEmail("max.mustermann@example.com");

        // Kursbetreuer-Testdaten
        testKursbetreuerDTO = new KursbetreuerDTO();
        testKursbetreuerDTO.setId(2L);
        testKursbetreuerDTO.setVorname("Maria");
        testKursbetreuerDTO.setNachname("Musterfrau");
        testKursbetreuerDTO.setEmail("maria.musterfrau@example.com");
        testKursbetreuerDTO.setKlartext_passwort("initialPassword");

        // Listen vorbereiten
        testStudenten = new ArrayList<>();
        testStudenten.add(testStudentDTO);

        testKursbetreuer = new ArrayList<>();
        testKursbetreuer.add(testKursbetreuerDTO);

        // Kurs-Testdaten
        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Testkurs");

        // Belegungs-Testdaten
        testBelegungDTO = new BelegungDTO();
        testBelegungDTO.setId(1L);
        testBelegungDTO.setKursId(1L);
        testBelegungDTO.setStudentId(1L);
        testBelegungDTO.setMatrikelnummer("12345678");
        testBelegungDTO.setStudentName("Max Mustermann");
        testBelegungDTO.setKursName("Testkurs");
        testBelegungDTO.setStartDatum(LocalDate.now().minusDays(10));
        testBelegungDTO.setEndDatum(LocalDate.now().plusDays(100));
        testBelegungDTO.setAktiv(true);

        testBelegungen = new ArrayList<>();
        testBelegungen.add(testBelegungDTO);

        testKurse = new ArrayList<>();
        testKurse.add(testKursDTO);

        // Kein Stubbing im Setup, stattdessen in den spezifischen Tests
    }

    /**
     * Testet die Anzeige der Übersicht aller Nutzer.
     */
    @Test
    void showNutzerverwaltung_ShouldDisplayAllUsers() {
        // Arrange
        when(nutzerService.getAlleStudenten()).thenReturn(testStudenten);
        when(nutzerService.getAlleKursbetreuer()).thenReturn(testKursbetreuer);

        // Act
        String viewName = controller.showNutzerverwaltung(model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/nutzerverwaltung", viewName);
        verify(model).addAttribute("studenten", testStudenten);
        verify(model).addAttribute("kursbetreuer", testKursbetreuer);
    }
    
    /**
     * Testet die Anzeige der Studentendetails.
     */
    @Test
    void showStudentDetails_WhenStudentExists_ShouldShowStudentDetails() {
        // Arrange
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        when(belegungService.getAllEnrollmentsByStudent(testStudentDTO)).thenReturn(testBelegungen);
        when(aktivitaetsService.findeAktivitaetenFuerNutzerPaged(testStudentDTO, 0, 10)).thenReturn(Page.empty());

        // Act
        String viewName = controller.showStudentDetails(1L, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/student-details", viewName);
        verify(model).addAttribute("student", testStudentDTO);
        verify(model).addAttribute("belegungen", testBelegungen);
        verify(model).addAttribute(eq("aktivitaeten"), any(Page.class));
    }
    
    /**
     * Testet die Anzeige der Studentendetails, wenn der Student nicht existiert.
     */
    @Test
    void showStudentDetails_WhenStudentNotFound_ShouldShowErrorPage() {
        // Arrange
        when(nutzerService.getStudentById(99L)).thenReturn(Optional.empty());

        // Act
        String viewName = controller.showStudentDetails(99L, model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Anzeige des Bearbeitungsformulars für Studenten.
     */
    @Test
    void showEditStudentForm_WhenStudentExists_ShouldShowEditForm() {
        // Arrange
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));

        // Act
        String viewName = controller.showEditStudentForm(1L, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/student-edit", viewName);
        verify(model).addAttribute("student", testStudentDTO);
    }
    
    /**
     * Testet die Anzeige des Bearbeitungsformulars für Studenten, wenn der Student nicht existiert.
     */
    @Test
    void showEditStudentForm_WhenStudentNotFound_ShouldShowErrorPage() {
        // Arrange
        when(nutzerService.getStudentById(99L)).thenReturn(Optional.empty());

        // Act
        String viewName = controller.showEditStudentForm(99L, model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Speichern von bearbeiteten Studentendaten.
     */
    @Test
    void saveStudent_WhenValidData_ShouldUpdateAndRedirect() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(nutzerService.aktualisiereStudent(eq(1L), any(StudentDTO.class))).thenReturn(testStudentDTO);

        // Act
        String redirectUrl = controller.saveStudent(1L, testStudentDTO, bindingResult, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/student/1", redirectUrl);
        verify(nutzerService).aktualisiereStudent(eq(1L), any(StudentDTO.class));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Speichern von Studentendaten mit Validierungsfehlern.
     */
    @Test
    void saveStudent_WhenValidationErrors_ShouldReturnToEditForm() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);

        // Act
        String viewName = controller.saveStudent(1L, testStudentDTO, bindingResult, redirectAttributes);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/student-edit", viewName);
        verify(nutzerService, never()).aktualisiereStudent(anyLong(), any(StudentDTO.class));
    }
    
    /**
     * Testet das Zurücksetzen eines Passwortes für einen Studenten.
     */
    @Test
    void resetPassword_ForStudent_ShouldResetPasswordAndRedirect() {
        // Arrange
        when(nutzerService.setzePasswortZurueck(1L)).thenReturn("neuesPasswort");
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));

        // Act
        String redirectUrl = controller.resetPassword(1L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/student/1", redirectUrl);
        verify(nutzerService).setzePasswortZurueck(1L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("student", testStudentDTO);
    }
    
    /**
     * Testet das Zurücksetzen eines Passwortes für einen Kursbetreuer.
     */
    @Test
    void resetPassword_ForKursbetreuer_ShouldResetPasswordAndRedirect() {
        // Arrange
        when(nutzerService.setzePasswortZurueck(2L)).thenReturn("neuesPasswort");
        when(nutzerService.getStudentById(2L)).thenReturn(Optional.empty());
        when(nutzerService.getKursbetreuerById(2L)).thenReturn(Optional.of(testKursbetreuerDTO));

        // Act
        String redirectUrl = controller.resetPassword(2L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/kursbetreuer/2", redirectUrl);
        verify(nutzerService).setzePasswortZurueck(2L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("kursbetreuer", testKursbetreuerDTO);
    }
    
    /**
     * Testet das Zurücksetzen eines Passwortes, wenn der Nutzer nicht existiert.
     */
    @Test
    void resetPassword_WhenUserNotFound_ShouldRedirectToOverview() {
        // Arrange
        when(nutzerService.setzePasswortZurueck(99L)).thenReturn("neuesPasswort");
        when(nutzerService.getStudentById(99L)).thenReturn(Optional.empty());
        when(nutzerService.getKursbetreuerById(99L)).thenReturn(Optional.empty());

        // Act
        String redirectUrl = controller.resetPassword(99L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer", redirectUrl);
        verify(nutzerService).setzePasswortZurueck(99L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        // Es sollte keine Attribute zum Model hinzugefügt werden, da kein Nutzer gefunden wurde
        verify(model, never()).addAttribute(eq("student"), any());
        verify(model, never()).addAttribute(eq("kursbetreuer"), any());
    }
    
    /**
     * Testet die Anzeige der Löschbestätigung für einen Studenten.
     */
    @Test
    void showDeleteStudentConfirmation_WhenStudentExists_ShouldShowConfirmation() {
        // Arrange
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));

        // Act
        String viewName = controller.showDeleteStudentConfirmation(1L, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/student-loeschen", viewName);
        verify(model).addAttribute("student", testStudentDTO);
    }
    
    /**
     * Testet die Anzeige der Löschbestätigung für einen Studenten, wenn der Student nicht existiert.
     */
    @Test
    void showDeleteStudentConfirmation_WhenStudentNotFound_ShouldShowErrorPage() {
        // Arrange
        when(nutzerService.getStudentById(99L)).thenReturn(Optional.empty());

        // Act
        String viewName = controller.showDeleteStudentConfirmation(99L, model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Löschen eines Studenten.
     */
    @Test
    void deleteStudent_WhenStudentExists_ShouldDeleteAndRedirect() {
        // Arrange
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        doNothing().when(nutzerService).loescheStudent(1L);

        // Act
        String redirectUrl = controller.deleteStudent(1L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer", redirectUrl);
        verify(nutzerService).loescheStudent(1L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("student", testStudentDTO);
    }
    
    /**
     * Testet das Löschen eines Studenten, wenn der Student nicht existiert.
     */
    @Test
    void deleteStudent_WhenStudentNotFound_ShouldShowErrorAndRedirect() {
        // Arrange
        when(nutzerService.getStudentById(99L)).thenReturn(Optional.empty());

        // Act
        String redirectUrl = controller.deleteStudent(99L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
        verify(nutzerService, never()).loescheStudent(anyLong());
        verify(model, never()).addAttribute(eq("student"), any());
    }
    
    /**
     * Testet die Anzeige des Formulars zum Erstellen eines neuen Kursbetreuers.
     */
    @Test
    void showCreateKursbetreuerForm_ShouldDisplayCreateForm() {
        // Act
        String viewName = controller.showCreateKursbetreuerForm(model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/kursbetreuer-create", viewName);
        verify(model).addAttribute(eq("kursbetreuer"), any(KursbetreuerDTO.class));
    }
    
    /**
     * Testet das Speichern eines neuen Kursbetreuers.
     */
    @Test
    void saveKursbetreuer_WhenValidData_ShouldCreateAndRedirect() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(nutzerService.erstelleKursbetreuer(any(KursbetreuerDTO.class))).thenReturn(testKursbetreuerDTO);

        // Act
        String redirectUrl = controller.saveKursbetreuer(testKursbetreuerDTO, bindingResult, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/kursbetreuer/2", redirectUrl);
        verify(nutzerService).erstelleKursbetreuer(any(KursbetreuerDTO.class));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("kursbetreuer", testKursbetreuerDTO);
    }
    
    /**
     * Testet das Speichern eines Kursbetreuers mit Validierungsfehlern.
     */
    @Test
    void saveKursbetreuer_WhenValidationErrors_ShouldReturnToCreateForm() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);

        // Act
        String viewName = controller.saveKursbetreuer(testKursbetreuerDTO, bindingResult, redirectAttributes, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/kursbetreuer-create", viewName);
        verify(nutzerService, never()).erstelleKursbetreuer(any(KursbetreuerDTO.class));
        verify(model, never()).addAttribute(eq("kursbetreuer"), any());
    }
    
    /**
     * Testet die Anzeige der Kursbetreuerdetails.
     */
    @Test
    void showKursbetreuerDetails_WhenKursbetreuerExists_ShouldShowDetails() {
        // Arrange
        when(nutzerService.getKursbetreuerById(2L)).thenReturn(Optional.of(testKursbetreuerDTO));
        when(aktivitaetsService.findeAktivitaetenFuerNutzerPaged(testKursbetreuerDTO, 0, 10)).thenReturn(Page.empty());

        // Act
        String viewName = controller.showKursbetreuerDetails(2L, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/kursbetreuer-details", viewName);
        verify(model).addAttribute("kursbetreuer", testKursbetreuerDTO);
        verify(model).addAttribute(eq("aktivitaeten"), any(Page.class));
    }
    
    /**
     * Testet die Anzeige der Kursbetreuerdetails, wenn der Kursbetreuer nicht existiert.
     */
    @Test
    void showKursbetreuerDetails_WhenKursbetreuerNotFound_ShouldShowErrorPage() {
        // Arrange
        when(nutzerService.getKursbetreuerById(99L)).thenReturn(Optional.empty());

        // Act
        String viewName = controller.showKursbetreuerDetails(99L, model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Anzeige des Bearbeitungsformulars für Kursbetreuer.
     */
    @Test
    void showEditKursbetreuerForm_WhenKursbetreuerExists_ShouldShowEditForm() {
        // Arrange
        when(nutzerService.getKursbetreuerById(2L)).thenReturn(Optional.of(testKursbetreuerDTO));

        // Act
        String viewName = controller.showEditKursbetreuerForm(2L, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/kursbetreuer-edit", viewName);
        verify(model).addAttribute("kursbetreuer", testKursbetreuerDTO);
    }
    
    /**
     * Testet die Anzeige des Bearbeitungsformulars für Kursbetreuer, wenn der Kursbetreuer nicht existiert.
     */
    @Test
    void showEditKursbetreuerForm_WhenKursbetreuerNotFound_ShouldShowErrorPage() {
        // Arrange
        when(nutzerService.getKursbetreuerById(99L)).thenReturn(Optional.empty());

        // Act
        String viewName = controller.showEditKursbetreuerForm(99L, model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Speichern von bearbeiteten Kursbetreuer-Daten.
     */
    @Test
    void saveKursbetreuer_WithID_WhenValidData_ShouldUpdateAndRedirect() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(nutzerService.aktualisiereKursbetreuer(eq(2L), any(KursbetreuerDTO.class))).thenReturn(testKursbetreuerDTO);

        // Act
        String redirectUrl = controller.saveKursbetreuer(2L, testKursbetreuerDTO, bindingResult, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/kursbetreuer/2", redirectUrl);
        verify(nutzerService).aktualisiereKursbetreuer(eq(2L), any(KursbetreuerDTO.class));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("kursbetreuer", testKursbetreuerDTO);
    }
    
    /**
     * Testet das Speichern von Kursbetreuer-Daten mit Validierungsfehlern.
     */
    @Test
    void saveKursbetreuer_WithID_WhenValidationErrors_ShouldReturnToEditForm() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);

        // Act
        String viewName = controller.saveKursbetreuer(2L, testKursbetreuerDTO, bindingResult, redirectAttributes, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/kursbetreuer-edit", viewName);
        verify(nutzerService, never()).aktualisiereKursbetreuer(anyLong(), any(KursbetreuerDTO.class));
        verify(model, never()).addAttribute(eq("kursbetreuer"), any());
    }
    
    /**
     * Testet die Anzeige der Löschbestätigung für einen Kursbetreuer.
     */
    @Test
    void showDeleteKursbetreuerConfirmation_WhenKursbetreuerExists_ShouldShowConfirmation() {
        // Arrange
        when(nutzerService.getKursbetreuerById(2L)).thenReturn(Optional.of(testKursbetreuerDTO));

        // Act
        String viewName = controller.showDeleteKursbetreuerConfirmation(2L, model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/kursbetreuer-loeschen", viewName);
        verify(model).addAttribute("kursbetreuer", testKursbetreuerDTO);
    }
    
    /**
     * Testet die Anzeige der Löschbestätigung für einen Kursbetreuer, wenn der Kursbetreuer nicht existiert.
     */
    @Test
    void showDeleteKursbetreuerConfirmation_WhenKursbetreuerNotFound_ShouldShowErrorPage() {
        // Arrange
        when(nutzerService.getKursbetreuerById(99L)).thenReturn(Optional.empty());

        // Act
        String viewName = controller.showDeleteKursbetreuerConfirmation(99L, model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Löschen eines Kursbetreuers.
     */
    @Test
    void deleteKursbetreuer_WhenKursbetreuerExists_ShouldDeleteAndRedirect() {
        // Arrange
        when(kursbetreuerUserDetails.getId()).thenReturn(3L); // anderer als der zu löschende
        when(nutzerService.getKursbetreuerById(2L)).thenReturn(Optional.of(testKursbetreuerDTO));
        doNothing().when(nutzerService).loescheKursbetreuer(2L);

        // Act
        String redirectUrl = controller.deleteKursbetreuer(2L, redirectAttributes, kursbetreuerUserDetails, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer", redirectUrl);
        verify(nutzerService).loescheKursbetreuer(2L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(model).addAttribute("kursbetreuer", testKursbetreuerDTO);
    }
    
    /**
     * Testet das Löschen eines Kursbetreuers, wenn der Kursbetreuer nicht existiert.
     */
    @Test
    void deleteKursbetreuer_WhenKursbetreuerNotFound_ShouldShowErrorAndRedirect() {
        // Arrange
        when(kursbetreuerUserDetails.getId()).thenReturn(3L); // anderer als der zu löschende
        when(nutzerService.getKursbetreuerById(99L)).thenReturn(Optional.empty());

        // Act
        String redirectUrl = controller.deleteKursbetreuer(99L, redirectAttributes, kursbetreuerUserDetails, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
        verify(nutzerService, never()).loescheKursbetreuer(anyLong());
        verify(model, never()).addAttribute(eq("kursbetreuer"), any());
    }
    
    /**
     * Testet, dass ein Kursbetreuer sich nicht selbst löschen kann.
     */
    @Test
    void deleteKursbetreuer_WhenSelfDeletion_ShouldShowErrorAndRedirect() {
        // Arrange
        when(kursbetreuerUserDetails.getId()).thenReturn(2L); // Der gleiche, der gelöscht werden soll

        // Act
        String redirectUrl = controller.deleteKursbetreuer(2L, redirectAttributes, kursbetreuerUserDetails, model);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/kursbetreuer/2", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
        verify(nutzerService, never()).loescheKursbetreuer(anyLong());
        verify(model, never()).addAttribute(anyString(), any());
    }
    
    /**
     * Testet die Anzeige des Formulars zum Hinzufügen einer Belegung für einen Studenten.
     */
    @Test
    void showAddBelegungForm_WhenStudentExists_ShouldShowAddForm() {
        // Arrange
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        when(belegungService.getAllEnrollmentsByStudent(testStudentDTO)).thenReturn(testBelegungen);
        when(kursService.getAlleKurse()).thenReturn(testKurse);

        // Act
        String viewName = controller.showAddBelegungForm(1L, null, "student", model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/student-belegung-add", viewName);
        verify(model).addAttribute("student", testStudentDTO);
        verify(model).addAttribute(eq("verfuegbareKurse"), any());
        verify(model).addAttribute(eq("startDatum"), any(LocalDate.class));
        verify(model).addAttribute(eq("endDatum"), any(LocalDate.class));
        verify(model).addAttribute("returnTo", "student");
    }
    
    /**
     * Testet die Anzeige des Formulars zum Hinzufügen einer Belegung, wenn der Student nicht existiert.
     */
    @Test
    void showAddBelegungForm_WhenStudentNotFound_ShouldShowErrorPage() {
        // Arrange
        when(nutzerService.getStudentById(99L)).thenReturn(Optional.empty());

        // Act
        String viewName = controller.showAddBelegungForm(99L, null, "student", model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet die Anzeige des Formulars zum Hinzufügen einer Belegung mit vorausgewähltem Kurs.
     */
    @Test
    void showAddBelegungForm_WithPreselectedKurs_ShouldShowAddFormWithPreselectedKurs() {
        // Arrange
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        when(belegungService.getAllEnrollmentsByStudent(testStudentDTO)).thenReturn(Collections.emptyList());
        when(kursService.getAlleKurse()).thenReturn(testKurse);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        // Act
        String viewName = controller.showAddBelegungForm(1L, 1L, "kurs", model);

        // Assert
        assertEquals("kursbetreuer/nutzerverwaltung/student-belegung-add", viewName);
        verify(model).addAttribute("student", testStudentDTO);
        verify(model).addAttribute(eq("verfuegbareKurse"), any());
        verify(model).addAttribute(eq("startDatum"), any(LocalDate.class));
        verify(model).addAttribute(eq("endDatum"), any(LocalDate.class));
        verify(model).addAttribute("preselectedKurs", testKursDTO);
        verify(model).addAttribute("returnTo", "kurs");
    }
    
    /**
     * Testet das Hinzufügen einer Belegung für einen Studenten.
     */
    @Test
    void addBelegungToStudent_WhenSuccessful_ShouldAddBelegungAndRedirect() {
        // Arrange
        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);
        
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.addStudentToKurs(
                eq(testStudentDTO),
                eq(testKursDTO),
                eq(startDatum),
                eq(endDatum)
        )).thenReturn(testBelegungDTO);

        // Act
        String redirectUrl = controller.addBelegungToStudent(
                1L, 1L, startDatum, endDatum, "student", redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/student/1", redirectUrl);
        verify(belegungService).addStudentToKurs(
                eq(testStudentDTO),
                eq(testKursDTO),
                eq(startDatum),
                eq(endDatum));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Hinzufügen einer Belegung für einen Studenten, wenn ein Fehler auftritt.
     */
    @Test
    void addBelegungToStudent_WhenExceptionOccurs_ShouldShowErrorAndRedirect() {
        // Arrange
        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);
        
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.addStudentToKurs(
                any(StudentDTO.class),
                any(KursDTO.class),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenThrow(new IllegalStateException("Student ist bereits für diesen Kurs eingeschrieben"));

        // Act
        String redirectUrl = controller.addBelegungToStudent(
                1L, 1L, startDatum, endDatum, "student", redirectAttributes);

        // Assert
        String expectedRedirect = "redirect:/kursbetreuer/nutzer/student/1/belegung/add?returnTo=student";
        assertEquals(expectedRedirect, redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
    }


}
