package de.fuh.kn.webapp.nutzerverwaltung.belegung;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMapper;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.BelegungRepository;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den KursbetreuerBelegungController.
 * Testet die verschiedenen Controller-Methoden zur Verwaltung von Kursbelegungen.
 */
@ExtendWith(MockitoExtension.class)
class KursbetreuerBelegungControllerTest {

    @Mock
    private BelegungService belegungService;

    @Mock
    private KursService kursService;
    
    @Mock
    private NutzerService nutzerService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private KursRepository kursRepository;

    @Mock
    private BelegungRepository belegungRepository;

    @Mock
    private BelegungMapper belegungMapper;

    @Mock
    private KursMapper kursMapper;

    @Mock
    private Model model;
    
    @Mock
    private RedirectAttributes redirectAttributes;

    @Captor
    private ArgumentCaptor<LocalDate> startDateCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> endDateCaptor;

    @Spy
    @InjectMocks
    private KursbetreuerBelegungController controller;

    private Kurs testKurs;
    private List<Student> testStudents;
    private KursDTO testKursDTO;
    private StudentDTO testStudentDTO;
    private BelegungDTO testBelegungDTO;
    private List<StudentDTO> testStudentDTOs;
    private LocalDate fixedCurrentDate;

    @BeforeEach
    void setUp() {
        // Test-Entities
        testKurs = new Kurs();
        testKurs.setId(1L);
        testKurs.setName("Testkurs");

        testStudents = new ArrayList<>();
        Student student = new Student();
        student.setId(1L);
        student.setVorname("Max");
        student.setNachname("Mustermann");
        testStudents.add(student);
        
        // Test-DTOs
        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Testkurs");

        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setMatrikelnummer("12345678");
        testStudentDTO.setVorname("Max");
        testStudentDTO.setNachname("Mustermann");
        
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
        
        testStudentDTOs = new ArrayList<>();
        testStudentDTOs.add(testStudentDTO);
        
        // Festes Datum für Tests
        fixedCurrentDate = LocalDate.of(2023, 6, 15);
    }

    /**
     * Parametrisierter Test für die Logik zur Bestimmung des Semesterstartdatums.
     * Testet verschiedene Datumskonstellationen und prüft, ob die erwarteten Daten gesetzt werden.
     * 
     * @param currentDate Aktuelles Datum
     * @param expectedStartDate Erwartetes Startdatum
     * @param expectedEndDate Erwartetes Enddatum
     * @param scenario Beschreibung des Testszenarios
     */
    @ParameterizedTest(name = "{3}: Bei aktuellem Datum {0} sollte Start={1}, Ende={2}")
    @MethodSource("provideDateTestCases")
    void semesterDateLogic_ShouldSelectCorrectDates(
            LocalDate currentDate, 
            LocalDate expectedStartDate, 
            LocalDate expectedEndDate, 
            String scenario) {
        
        // Erstelle eine Spy-Version des Controllers, um getCurrentDate zu überschreiben
        KursbetreuerBelegungController controllerSpy = spy(new KursbetreuerBelegungController(
                belegungService, kursService, nutzerService));
        
        // Mock der getCurrentDate-Methode
        doReturn(currentDate).when(controllerSpy).getCurrentDate();

        // Mock für kursService.getKursById
        KursDTO mockKursDTO = new KursDTO();
        mockKursDTO.setId(1L);
        mockKursDTO.setName("Testkurs");
        when(kursService.getKursById(1L)).thenReturn(mockKursDTO);
        
        // Mock für belegungService.getEnrollmentsByKurs
        when(belegungService.getEnrollmentsByKurs(any(KursDTO.class))).thenReturn(Collections.emptyList());
        
        // Mock für studentService.getVerfuegbareStudenten
        List<StudentDTO> mockStudentDTOs = new ArrayList<>();
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(1L);
        studentDTO.setVorname("Max");
        studentDTO.setNachname("Mustermann");
        mockStudentDTOs.add(studentDTO);
        when(nutzerService.getAlleStudenten()).thenReturn(mockStudentDTOs);

        // Act
        String result = controllerSpy.showAddBelegungForm(1L, model);

        // Assert
        verify(model).addAttribute(eq("startDatum"), startDateCaptor.capture());
        verify(model).addAttribute(eq("endDatum"), endDateCaptor.capture());

        LocalDate capturedStartDate = startDateCaptor.getValue();
        LocalDate capturedEndDate = endDateCaptor.getValue();

        assertEquals(expectedStartDate, capturedStartDate, 
                "Startdatum sollte korrekt gesetzt werden: " + scenario);
        assertEquals(expectedEndDate, capturedEndDate,
                "Enddatum sollte korrekt gesetzt werden: " + scenario);
    }

    /**
     * Liefert Testfälle für die Semesterdaten-Logik.
     * Jeder Testfall enthält:
     * - aktuelles Datum
     * - erwartetes Startdatum
     * - erwartetes Enddatum
     * - Beschreibung des Szenarios
     */
    private static Stream<Arguments> provideDateTestCases() {
        int year = 2025;
        return Stream.of(
            // Standard-Szenarien für verschiedene Monate
            Arguments.of(
                LocalDate.of(year, 1, 15),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Januar: Sollte April dieses Jahres wählen"),
                
            Arguments.of(
                LocalDate.of(year, 5, 15),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Mai: Sollte April dieses Jahres wählen (auch wenn in Vergangenheit)"),
                
            Arguments.of(
                LocalDate.of(year, 8, 15),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "August: Sollte Oktober dieses Jahres wählen"),
                
            Arguments.of(
                LocalDate.of(year, 11, 15),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "November: Sollte Oktober dieses Jahres wählen (auch wenn in Vergangenheit)"),
                
            Arguments.of(
                LocalDate.of(year, 12, 31),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "Dezember: Sollte Oktober dieses Jahres wählen (auch wenn in Vergangenheit)"),
                
            // Grenzfälle
            Arguments.of(
                LocalDate.of(year, 3, 31),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Grenzfall 31. März: Tag vor Semesterbeginn"),
                
            Arguments.of(
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Grenzfall 1. April: Genau am Semesterbeginn"),
                
            Arguments.of(
                LocalDate.of(year, 9, 30),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "Grenzfall 30. September: Genau am Semesterende"),
                
            Arguments.of(
                LocalDate.of(year, 10, 1),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "Grenzfall 1. Oktober: Genau am Semesterbeginn"),
                
            Arguments.of(
                LocalDate.of(year + 1, 3, 31),
                LocalDate.of(year + 1, 4, 1),
                LocalDate.of(year + 1, 9, 30),
                "Grenzfall 31. März Folgejahr: Genau am Semesterende")
        );
    }
    
    /**
     * Testet das Anzeigen von Belegungen für einen bestimmten Kurs.
     */
    @Test
    void showBelegungByKurs_ShouldDisplayKursBelegungen() {
        // Arrange
        List<BelegungDTO> belegungen = Collections.singletonList(testBelegungDTO);
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKursWithProgress(testKursDTO)).thenReturn(belegungen);
        when(nutzerService.getAlleStudenten()).thenReturn(testStudentDTOs);

        // Act
        String viewName = controller.showBelegungByKurs(1L, model);

        // Assert
        assertEquals("kursbetreuer/kursbelegung/belegungen-kurs", viewName);
        verify(model).addAttribute("kurs", testKursDTO);
        verify(model).addAttribute("belegungen", belegungen);
        verify(model).addAttribute("allStudents", testStudentDTOs);
        verify(model).addAttribute(eq("heute"), any(LocalDate.class));
    }
    
    /**
     * Testet das Anzeigen eines Formulars zum Hinzufügen einer neuen Belegung.
     */
    @Test
    void showAddBelegungForm_ShouldDisplayAddForm() {
        // Arrange
        // Datum fixieren für reproduzierbare Tests
        doReturn(fixedCurrentDate).when(controller).getCurrentDate();
        
        List<BelegungDTO> existingBelegungen = Collections.emptyList();
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.getEnrollmentsByKurs(testKursDTO)).thenReturn(existingBelegungen);
        when(nutzerService.getAlleStudenten()).thenReturn(testStudentDTOs);

        // Act
        String viewName = controller.showAddBelegungForm(1L, model);

        // Assert
        assertEquals("kursbetreuer/kursbelegung/belegung-add", viewName);
        verify(model).addAttribute("kurs", testKursDTO);
        verify(model).addAttribute("availableStudents", testStudentDTOs);
        verify(model).addAttribute(eq("startDatum"), any(LocalDate.class));
        verify(model).addAttribute(eq("endDatum"), any(LocalDate.class));
    }
    
    /**
     * Testet das Hinzufügen einer neuen Belegung über die Student-ID.
     */
    @Test
    void addBelegung_WithStudentId_ShouldAddBelegungAndRedirect() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        when(belegungService.addStudentToKurs(
                eq(testStudentDTO), 
                eq(testKursDTO), 
                any(LocalDate.class), 
                any(LocalDate.class)
        )).thenReturn(testBelegungDTO);

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);

        // Act
        String redirectUrl = controller.addBelegung(
                1L, 1L, null, null,
                startDatum, endDatum, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(belegungService).addStudentToKurs(
                eq(testStudentDTO), 
                eq(testKursDTO), 
                eq(startDatum), 
                eq(endDatum));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Hinzufügen einer neuen Belegung über die Matrikelnummer.
     */
    @Test
    void addBelegung_WithMatrikelnummer_ShouldAddBelegungAndRedirect() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(belegungService.addStudentByMatrikelnummerToKurs(
                eq("12345678"), 
                eq(1L), 
                any(LocalDate.class), 
                any(LocalDate.class)
        )).thenReturn(testBelegungDTO);

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);

        // Act
        String redirectUrl = controller.addBelegung(
                1L, null, "12345678", null,
                startDatum, endDatum, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(belegungService).addStudentByMatrikelnummerToKurs(
                eq("12345678"), 
                eq(1L), 
                eq(startDatum), 
                eq(endDatum));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Hinzufügen mehrerer Belegungen über eine Liste von Matrikelnummern.
     */
    @Test
    void addBelegung_WithMatrikelnummernListe_ShouldAddMultipleBelegungenAndRedirect() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        
        List<BelegungDTO> erstellteBelegungen = Collections.singletonList(testBelegungDTO);
        when(belegungService.addMultipleStudentsByMatrikelnummerToKurs(
                any(), 
                eq(1L), 
                any(LocalDate.class), 
                any(LocalDate.class)
        )).thenReturn(erstellteBelegungen);

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);
        String matrikelnummernListe = "12345678\n87654321";

        // Act
        String redirectUrl = controller.addBelegung(
                1L, null, null, matrikelnummernListe,
                startDatum, endDatum, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(belegungService).addMultipleStudentsByMatrikelnummerToKurs(
                eq(List.of("12345678", "87654321")),
                eq(1L), 
                eq(startDatum), 
                eq(endDatum));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Hinzufügen einer Belegung, wenn keine Student-ID oder Matrikelnummer angegeben ist.
     */
    @Test
    void addBelegung_WithNoStudentIdOrMatrikelnummer_ShouldShowErrorAndRedirect() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);

        // Act
        String redirectUrl = controller.addBelegung(
                1L, null, null, null,
                startDatum, endDatum, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
        verify(belegungService, never()).addStudentToKurs(any(), any(), any(), any());
        verify(belegungService, never()).addStudentByMatrikelnummerToKurs(anyString(), anyLong(), any(), any());
    }
    
    /**
     * Testet das Hinzufügen einer Belegung, wenn eine Exception auftritt.
     */
    @Test
    void addBelegung_WhenExceptionOccurs_ShouldShowErrorAndRedirect() {
        // Arrange
        when(kursService.getKursById(1L)).thenReturn(testKursDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudentDTO));
        when(belegungService.addStudentToKurs(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Student ist bereits eingeschrieben"));

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);

        // Act
        String redirectUrl = controller.addBelegung(
                1L, 1L, null, null,
                startDatum, endDatum, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Anzeigen eines Formulars zum Bearbeiten einer bestehenden Belegung mit redirectTo="kurs".
     */
    @Test
    void showEditBelegungForm_WithKursRedirect_ShouldDisplayEditForm() {
        // Arrange
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        when(kursService.getKursById(testBelegungDTO.getKursId())).thenReturn(testKursDTO);

        // Act
        String viewName = controller.showEditBelegungForm(1L, "kurs", model);

        // Assert
        assertEquals("kursbetreuer/kursbelegung/belegung-edit", viewName);
        verify(model).addAttribute("belegung", testBelegungDTO);
        verify(model).addAttribute("kursId", testBelegungDTO.getKursId());
        verify(model).addAttribute("kursName", testKursDTO.getName());
        verify(model).addAttribute("returnTo", "kurs");
        verify(model).addAttribute("studentId", testBelegungDTO.getStudentId());
    }
    
    /**
     * Testet das Anzeigen eines Formulars zum Bearbeiten einer bestehenden Belegung mit redirectTo="student".
     */
    @Test
    void showEditBelegungForm_WithStudentRedirect_ShouldDisplayEditForm() {
        // Arrange
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        when(kursService.getKursById(testBelegungDTO.getKursId())).thenReturn(testKursDTO);

        // Act
        String viewName = controller.showEditBelegungForm(1L, "student", model);

        // Assert
        assertEquals("kursbetreuer/kursbelegung/belegung-edit", viewName);
        verify(model).addAttribute("belegung", testBelegungDTO);
        verify(model).addAttribute("kursId", testBelegungDTO.getKursId());
        verify(model).addAttribute("kursName", testKursDTO.getName());
        verify(model).addAttribute("returnTo", "student");
        verify(model).addAttribute("studentId", testBelegungDTO.getStudentId());
    }
    
    /**
     * Testet das Anzeigen eines Formulars zum Bearbeiten einer nicht existierenden Belegung.
     */
    @Test
    void showEditBelegungForm_WhenBelegungNotFound_ShouldReturnErrorView() {
        // Arrange
        when(belegungService.getBelegungById(99L))
                .thenThrow(new NoSuchElementException("Belegung nicht gefunden"));

        // Act
        String viewName = controller.showEditBelegungForm(99L, "kurs", model);

        // Assert
        assertEquals("error", viewName);
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Aktualisieren einer bestehenden Belegung mit redirectTo="kurs".
     */
    @Test
    void updateBelegung_WithKursRedirect_ShouldUpdateDatesAndRedirectToKurs() {
        // Arrange
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        
        BelegungDTO updatedBelegungDTO = new BelegungDTO();
        updatedBelegungDTO.setId(1L);
        updatedBelegungDTO.setKursId(1L);
        updatedBelegungDTO.setStudentId(1L);
        updatedBelegungDTO.setStudentName("Max Mustermann");
        
        when(belegungService.updateBelegungDates(
                eq(testBelegungDTO),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(updatedBelegungDTO);

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);

        // Act
        String redirectUrl = controller.updateBelegung(1L, startDatum, endDatum, "kurs", redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(belegungService).updateBelegungDates(
                eq(testBelegungDTO),
                eq(startDatum),
                eq(endDatum));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Aktualisieren einer bestehenden Belegung mit redirectTo="student".
     */
    @Test
    void updateBelegung_WithStudentRedirect_ShouldUpdateDatesAndRedirectToStudent() {
        // Arrange
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        
        BelegungDTO updatedBelegungDTO = new BelegungDTO();
        updatedBelegungDTO.setId(1L);
        updatedBelegungDTO.setKursId(1L);
        updatedBelegungDTO.setStudentId(1L);
        updatedBelegungDTO.setStudentName("Max Mustermann");
        
        when(belegungService.updateBelegungDates(
                eq(testBelegungDTO),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(updatedBelegungDTO);

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);

        // Act
        String redirectUrl = controller.updateBelegung(1L, startDatum, endDatum, "student", redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/student/1", redirectUrl);
        verify(belegungService).updateBelegungDates(
                eq(testBelegungDTO),
                eq(startDatum),
                eq(endDatum));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Aktualisieren einer nicht existierenden Belegung.
     */
    @Test
    void updateBelegung_WhenBelegungNotFound_ShouldShowErrorAndRedirect() {
        // Arrange
        when(belegungService.getBelegungById(99L))
                .thenThrow(new NoSuchElementException("Belegung nicht gefunden"));

        LocalDate startDatum = LocalDate.now();
        LocalDate endDatum = LocalDate.now().plusMonths(6);

        // Act
        String redirectUrl = controller.updateBelegung(99L, startDatum, endDatum, "kurs", redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/kursverwaltung", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Löschen einer bestehenden Belegung mit redirectTo="kurs".
     */
    @Test
    void deleteBelegung_WithKursRedirect_ShouldRemoveBelegungAndRedirectToKurs() {
        // Arrange
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        doNothing().when(belegungService).removeBelegung(testBelegungDTO);

        // Act
        String redirectUrl = controller.deleteBelegung(1L, "kurs", redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(belegungService).removeBelegung(testBelegungDTO);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Löschen einer bestehenden Belegung mit redirectTo="student".
     */
    @Test
    void deleteBelegung_WithStudentRedirect_ShouldRemoveBelegungAndRedirectToStudent() {
        // Arrange
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        doNothing().when(belegungService).removeBelegung(testBelegungDTO);

        // Act
        String redirectUrl = controller.deleteBelegung(1L, "student", redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/nutzer/student/1", redirectUrl);
        verify(belegungService).removeBelegung(testBelegungDTO);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Löschen einer nicht existierenden Belegung.
     */
    @Test
    void deleteBelegung_WhenBelegungNotFound_ShouldShowErrorAndRedirect() {
        // Arrange
        when(belegungService.getBelegungById(99L))
                .thenThrow(new NoSuchElementException("Belegung nicht gefunden"));

        // Act
        String redirectUrl = controller.deleteBelegung(99L, "kurs", redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/kursverwaltung", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
    }
    
    /**
     * Testet das Löschen mehrerer Belegungen gleichzeitig.
     */
    @Test
    void massDeleteBelegungen_ShouldRemoveMultipleBelegungenAndRedirect() {
        // Arrange
        List<Long> belegungIds = Collections.singletonList(1L);
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        when(belegungService.removeBelegungen(any())).thenReturn(belegungIds);

        // Act
        String redirectUrl = controller.massDeleteBelegungen(belegungIds, 1L, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(belegungService).removeBelegungen(any());
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
    
    /**
     * Testet das Löschen mehrerer Belegungen, wenn keine Belegungen ausgewählt wurden.
     */
    @Test
    void massDeleteBelegungen_WhenNoBelegungenSelected_ShouldShowErrorAndRedirect() {
        // Arrange
        List<Long> belegungIds = Collections.emptyList();

        // Act
        String redirectUrl = controller.massDeleteBelegungen(belegungIds, 1L, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
        verify(belegungService, never()).removeBelegungen(any());
    }
    
    /**
     * Testet das Löschen mehrerer Belegungen, wenn eine Exception auftritt.
     */
    @Test
    void massDeleteBelegungen_WhenExceptionOccurs_ShouldShowErrorAndRedirect() {
        // Arrange
        List<Long> belegungIds = Collections.singletonList(1L);
        when(belegungService.getBelegungById(1L)).thenReturn(testBelegungDTO);
        when(belegungService.removeBelegungen(any()))
                .thenThrow(new RuntimeException("Fehler beim Löschen"));

        // Act
        String redirectUrl = controller.massDeleteBelegungen(belegungIds, 1L, redirectAttributes);

        // Assert
        assertEquals("redirect:/kursbetreuer/belegungen/kurs/1", redirectUrl);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
    }
}