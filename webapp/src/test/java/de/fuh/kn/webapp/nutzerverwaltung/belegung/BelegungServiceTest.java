package de.fuh.kn.webapp.nutzerverwaltung.belegung;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.*;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import de.fuh.kn.webapp.persistence.entity.Belegung;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.BelegungRepository;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.contains;

/**
 * Testklasse für den BelegungService.
 */
@ExtendWith(MockitoExtension.class)
class BelegungServiceTest {

    @Mock
    private BelegungRepository belegungRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private KursRepository kursRepository;

    @Mock
    private BelegungMapper belegungMapper;
    
    @Mock
    private NutzerService nutzerService;
    
    @Mock
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> detailsCaptor;

    @InjectMocks
    private BelegungService belegungService;

    private Student testStudent;
    private Kurs testKurs;
    private Belegung testBelegung;
    private BelegungDTO testBelegungDTO;
    private StudentDTO testStudentDTO;
    private KursDTO testKursDTO;
    private NutzerDTO testAdminDTO;

    @BeforeEach
    void setUp() {
        // Test-Objekte erstellen
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setMatrikelnummer("12345678");
        testStudent.setVorname("Max");
        testStudent.setNachname("Mustermann");

        testKurs = new Kurs();
        testKurs.setId(1L);
        testKurs.setName("Testvorlesung");

        testBelegung = new Belegung();
        testBelegung.setId(1L);
        testBelegung.setStudent(testStudent);
        testBelegung.setKurs(testKurs);
        testBelegung.setStartDatum(LocalDate.now().minusDays(10));
        testBelegung.setEndDatum(LocalDate.now().plusDays(100));

        testBelegungDTO = new BelegungDTO();
        testBelegungDTO.setId(1L);
        testBelegungDTO.setStudentId(1L);
        testBelegungDTO.setKursId(1L);
        testBelegungDTO.setMatrikelnummer("12345678");
        testBelegungDTO.setStudentName("Max Mustermann");
        testBelegungDTO.setKursName("Testvorlesung");
        testBelegungDTO.setStartDatum(LocalDate.now().minusDays(10));
        testBelegungDTO.setEndDatum(LocalDate.now().plusDays(100));
        testBelegungDTO.setAktiv(true);
        
        // DTOs für Student und Kurs erstellen
        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setMatrikelnummer("12345678");
        testStudentDTO.setVorname("Max");
        testStudentDTO.setNachname("Mustermann");
        
        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Testvorlesung");
        
        // Admin-DTO für Aktivitätsprotokollierung
        testAdminDTO = new NutzerDTO();
        testAdminDTO.setId(99L);
        testAdminDTO.setVorname("Admin");
        testAdminDTO.setNachname("Nutzer");
    }

    @Test
    void addStudentToKurs_WithDTOs_ShouldCreateNewBelegungAndLogActivity() {
        // Arrange
        when(nutzerService.getStudentByMatrikelnummer(testStudentDTO.getMatrikelnummer())).thenReturn(Optional.of(testStudentDTO));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.empty());
        when(belegungRepository.save(any(Belegung.class))).thenReturn(testBelegung);
        when(belegungMapper.toDto(testBelegung)).thenReturn(testBelegungDTO);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testAdminDTO);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(any(), any(), any(), any(), anyBoolean(), any(), anyLong())).thenReturn(new AktivitaetDTO());

        // Act
        BelegungDTO result = belegungService.addStudentToKurs(
                testStudentDTO, testKursDTO, LocalDate.now().minusDays(10), LocalDate.now().plusDays(100));

        // Assert
        assertNotNull(result);
        assertEquals(testBelegungDTO.getId(), result.getId());
        verify(belegungRepository).save(any(Belegung.class));
        
        // Verify activity logging for course enrollment
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(testAdminDTO),
                eq(AktivitaetsTyp.BELEGUNG_ERSTELLEN),
                contains("Student zu Kurs hinzugefügt: Testvorlesung"),
                detailsCaptor.capture(),
                eq(true),
                eq("Student"),
                eq(testStudent.getId())
        );
        
        // Verify details in the activity log
        Map<String, Object> capturedDetails = detailsCaptor.getValue();
        assertNotNull(capturedDetails);
        assertEquals(testKurs.getId(), capturedDetails.get("kursId"));
        assertEquals(testKurs.getName(), capturedDetails.get("kursName"));
        assertEquals(testStudent.getId(), capturedDetails.get("studentId"));
        assertEquals(testStudentDTO.getMatrikelnummer(), capturedDetails.get("matrikelnummer"));
    }

    @Test
    void addStudentToKurs_ShouldThrowException_WhenStudentAlreadyEnrolled() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(nutzerService.getStudentByMatrikelnummer(testStudentDTO.getMatrikelnummer())).thenReturn(Optional.of(testStudentDTO));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.of(testBelegung));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                belegungService.addStudentToKurs(
                        testStudentDTO, testKursDTO, LocalDate.now().minusDays(10), LocalDate.now().plusDays(100)));
        
        verify(belegungRepository, never()).save(any(Belegung.class));
        // Verify no activity log was created
        verify(aktivitaetsProtokollierungService, never()).protokolliereAktivitaet(
                any(), eq(AktivitaetsTyp.BELEGUNG_ERSTELLEN), any(), any(), anyBoolean(), any(), anyLong());
    }

    @Test
    void removeStudentFromKurs_WithDTOs_ShouldDeleteBelegungAndLogActivity() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.of(testBelegung));
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testAdminDTO);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(any(), any(), any(), any(), anyBoolean(), any(), anyLong())).thenReturn(new AktivitaetDTO());
        
        // Make sure the testBelegung has the student and kurs set correctly
        testBelegung.setStudent(testStudent);
        testBelegung.setKurs(testKurs);

        // Act
        belegungService.removeStudentFromKurs(testStudentDTO, testKursDTO);

        // Assert
        verify(belegungRepository).delete(testBelegung);
        
        // Verify activity logging for enrollment deletion
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(testAdminDTO),
                eq(AktivitaetsTyp.BELEGUNG_LOESCHEN),
                contains("Student aus Kurs entfernt: Testvorlesung"),
                detailsCaptor.capture(),
                eq(true),
                eq("Student"),
                eq(testStudent.getId())
        );
        
        // Verify details in the activity log
        Map<String, Object> capturedDetails = detailsCaptor.getValue();
        assertNotNull(capturedDetails);
        assertEquals(testKurs.getId(), capturedDetails.get("kursId"));
        assertEquals(testKurs.getName(), capturedDetails.get("kursName"));
        assertEquals(testStudent.getId(), capturedDetails.get("studentId"));
        assertEquals(testStudent.getMatrikelnummer(), capturedDetails.get("matrikelnummer"));
    }

    @Test
    void removeStudentFromKurs_ShouldThrowException_WhenBelegungNotFound() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
                belegungService.removeStudentFromKurs(testStudentDTO, testKursDTO));
        
        verify(belegungRepository, never()).delete(any(Belegung.class));
        // Verify no activity log was created
        verify(aktivitaetsProtokollierungService, never()).protokolliereAktivitaet(
                any(), eq(AktivitaetsTyp.BELEGUNG_LOESCHEN), any(), any(), anyBoolean(), any(), anyLong());
    }

    @Test
    void removeBelegung_ShouldDeleteBelegungAndLogActivity() {
        // Arrange
        when(belegungRepository.findById(1L)).thenReturn(Optional.of(testBelegung));
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testAdminDTO);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(any(), any(), any(), any(), anyBoolean(), any(), anyLong())).thenReturn(new AktivitaetDTO());
        
        // Make sure the testBelegung has the student and kurs set correctly
        testBelegung.setStudent(testStudent);
        testBelegung.setKurs(testKurs);

        // Act
        belegungService.removeBelegung(testBelegungDTO);

        // Assert
        verify(belegungRepository).delete(testBelegung);
        
        // Verify activity logging for enrollment deletion
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(testAdminDTO),
                eq(AktivitaetsTyp.BELEGUNG_LOESCHEN),
                contains("Student aus Kurs entfernt: Testvorlesung"),
                detailsCaptor.capture(),
                eq(true),
                eq("Student"),
                eq(testStudent.getId())
        );
        
        // Verify details in the activity log
        Map<String, Object> capturedDetails = detailsCaptor.getValue();
        assertNotNull(capturedDetails);
        assertEquals(testKurs.getId(), capturedDetails.get("kursId"));
        assertEquals(testKurs.getName(), capturedDetails.get("kursName"));
        assertEquals(testStudent.getId(), capturedDetails.get("studentId"));
        assertEquals(testStudent.getMatrikelnummer(), capturedDetails.get("matrikelnummer"));
    }
    
    @Test
    void updateBelegungDates_WithDTO_ShouldUpdateDates() {
        // Arrange
        LocalDate newStartDate = LocalDate.now().minusDays(5);
        LocalDate newEndDate = LocalDate.now().plusDays(50);
        
        Belegung updatedBelegung = new Belegung();
        updatedBelegung.setId(1L);
        updatedBelegung.setStudent(testStudent);
        updatedBelegung.setKurs(testKurs);
        updatedBelegung.setStartDatum(newStartDate);
        updatedBelegung.setEndDatum(newEndDate);
        
        BelegungDTO updatedDTO = new BelegungDTO();
        updatedDTO.setId(1L);
        updatedDTO.setStartDatum(newStartDate);
        updatedDTO.setEndDatum(newEndDate);
        
        when(belegungRepository.findById(1L)).thenReturn(Optional.of(testBelegung));
        when(belegungRepository.save(testBelegung)).thenReturn(updatedBelegung);
        when(belegungMapper.toDto(updatedBelegung)).thenReturn(updatedDTO);

        // Act
        BelegungDTO result = belegungService.updateBelegungDates(testBelegungDTO, newStartDate, newEndDate);

        // Assert
        assertNotNull(result);
        assertEquals(newStartDate, result.getStartDatum());
        assertEquals(newEndDate, result.getEndDatum());
        verify(belegungRepository).save(testBelegung);
    }

    @Test
    void getActiveEnrollmentsByStudent_WithDTO_ShouldReturnActiveEnrollments() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(belegungRepository.findActiveByStudent(eq(testStudent), any(LocalDate.class)))
                .thenReturn(List.of(testBelegung));
        when(belegungMapper.toDtoList(List.of(testBelegung))).thenReturn(List.of(testBelegungDTO));

        // Act
        List<BelegungDTO> result = belegungService.getActiveEnrollmentsByStudent(testStudentDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBelegungDTO.getId(), result.get(0).getId());
    }

    @Test
    void getAllEnrollmentsByStudent_WithDTO_ShouldReturnAllEnrollments() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(belegungRepository.findByStudent(testStudent)).thenReturn(List.of(testBelegung));
        when(belegungMapper.toDtoList(List.of(testBelegung))).thenReturn(List.of(testBelegungDTO));

        // Act
        List<BelegungDTO> result = belegungService.getAllEnrollmentsByStudent(testStudentDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBelegungDTO.getId(), result.get(0).getId());
    }

    @Test
    void getEnrollmentsByKurs_WithDTO_ShouldReturnKursEnrollments() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByKurs(testKurs)).thenReturn(List.of(testBelegung));
        when(belegungMapper.toDtoList(List.of(testBelegung))).thenReturn(List.of(testBelegungDTO));

        // Act
        List<BelegungDTO> result = belegungService.getEnrollmentsByKurs(testKursDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBelegungDTO.getId(), result.get(0).getId());
    }

    @Test
    void getActiveEnrollmentsByKurs_WithDTO_ShouldReturnActiveEnrollments() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByKurs(testKurs)).thenReturn(List.of(testBelegung));
        when(belegungMapper.toDtoList(any())).thenReturn(List.of(testBelegungDTO));

        // Act
        List<BelegungDTO> result = belegungService.getActiveEnrollmentsByKurs(testKursDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBelegungDTO.getId(), result.get(0).getId());
    }

    @Test
    void isStudentEnrolledInKurs_WithDTOs_ShouldReturnTrue_WhenEnrolledAndActive() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.of(testBelegung));

        // Act
        boolean result = belegungService.isStudentEnrolledInKurs(testStudentDTO, testKursDTO);

        // Assert
        assertTrue(result);
    }

    @Test
    void isStudentEnrolledInKurs_ShouldReturnFalse_WhenNotEnrolled() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.empty());

        // Act
        boolean result = belegungService.isStudentEnrolledInKurs(testStudentDTO, testKursDTO);

        // Assert
        assertFalse(result);
    }

    @Test
    void isStudentEnrolledInKurs_ShouldReturnFalse_WhenEnrolledButNotActive() {
        // Arrange
        Belegung expiredBelegung = new Belegung();
        expiredBelegung.setId(2L);
        expiredBelegung.setStudent(testStudent);
        expiredBelegung.setKurs(testKurs);
        expiredBelegung.setStartDatum(LocalDate.now().minusDays(100));
        expiredBelegung.setEndDatum(LocalDate.now().minusDays(10)); // Bereits abgelaufen

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.of(expiredBelegung));

        // Act
        boolean result = belegungService.isStudentEnrolledInKurs(testStudentDTO, testKursDTO);

        // Assert
        assertFalse(result);
    }

    @Test
    void getBelegungByStudentAndKurs_WithDTOs_ShouldReturnBelegung() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.of(testBelegung));
        when(belegungMapper.toDto(testBelegung)).thenReturn(testBelegungDTO);

        // Act
        BelegungDTO result = belegungService.getBelegungByStudentAndKurs(testStudentDTO, testKursDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testBelegungDTO.getId(), result.getId());
    }
    
    @Test
    void removeBelegungen_WithDTOs_ShouldDeleteBelegungenAndLogActivities() {
        // Arrange
        List<BelegungDTO> belegungDTOs = List.of(testBelegungDTO);
        when(belegungRepository.findById(1L)).thenReturn(Optional.of(testBelegung));
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testAdminDTO);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(any(), any(), any(), any(), anyBoolean(), any(), anyLong())).thenReturn(new AktivitaetDTO());
        
        // Make sure the testBelegung has the student and kurs set correctly
        testBelegung.setStudent(testStudent);
        testBelegung.setKurs(testKurs);
        
        // Act
        List<Long> result = belegungService.removeBelegungen(belegungDTOs);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
        verify(belegungRepository).delete(testBelegung);
        
        // Verify activity logging for enrollment deletion
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(testAdminDTO),
                eq(AktivitaetsTyp.BELEGUNG_LOESCHEN),
                contains("Student aus Kurs entfernt: Testvorlesung"),
                detailsCaptor.capture(),
                eq(true),
                eq("Student"),
                eq(testStudent.getId())
        );
        
        // Verify details in the activity log including massenLoeschung flag
        Map<String, Object> capturedDetails = detailsCaptor.getValue();
        assertNotNull(capturedDetails);
        assertEquals(testKurs.getId(), capturedDetails.get("kursId"));
        assertEquals(testKurs.getName(), capturedDetails.get("kursName"));
        assertEquals(testStudent.getId(), capturedDetails.get("studentId"));
        assertEquals(testStudent.getMatrikelnummer(), capturedDetails.get("matrikelnummer"));
    }

    @Test
    void getBelegungByStudentAndKurs_ShouldThrowException_WhenBelegungNotFound() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
                belegungService.getBelegungByStudentAndKurs(testStudentDTO, testKursDTO));
    }
    
    @Test
    void getBelegungById_ShouldReturnBelegung() {
        // Arrange
        when(belegungRepository.findById(1L)).thenReturn(Optional.of(testBelegung));
        when(belegungMapper.toDto(testBelegung)).thenReturn(testBelegungDTO);

        // Act
        BelegungDTO result = belegungService.getBelegungById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testBelegungDTO.getId(), result.getId());
    }

    @Test
    void getBelegungById_ShouldThrowException_WhenBelegungNotFound() {
        // Arrange
        when(belegungRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
                belegungService.getBelegungById(1L));
    }
    
    @Test
    void addStudentByMatrikelnummerToKurs_ShouldCreateNewBelegungAndLogActivities() {
        // Arrange
        String matrikelnummer = "12345678";
        when(nutzerService.getStudentByMatrikelnummer(matrikelnummer)).thenReturn(Optional.empty());
        when(nutzerService.erstelleDummyStudent(matrikelnummer)).thenReturn(testStudentDTO);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.empty());
        when(belegungRepository.save(any(Belegung.class))).thenReturn(testBelegung);
        when(belegungMapper.toDto(testBelegung)).thenReturn(testBelegungDTO);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testAdminDTO);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(any(), any(), any(), any(), anyBoolean(), any(), anyLong())).thenReturn(new AktivitaetDTO());

        // Act
        BelegungDTO result = belegungService.addStudentByMatrikelnummerToKurs(
                matrikelnummer, 1L, LocalDate.now().minusDays(10), LocalDate.now().plusDays(100));

        // Assert
        assertNotNull(result);
        assertEquals(testBelegungDTO.getId(), result.getId());
        verify(belegungRepository).save(any(Belegung.class));
        
        // Verify activity logging for new student creation
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(testAdminDTO),
                eq(AktivitaetsTyp.NUTZER_ERSTELLT),
                eq("Neuer Nutzer erstellt"),
                eq(null),
                eq(true),
                eq("Student"),
                eq(testStudentDTO.getId())
        );
        
        // Verify activity logging for course enrollment
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
                eq(testAdminDTO),
                eq(AktivitaetsTyp.BELEGUNG_ERSTELLEN),
                contains("Student zu Kurs hinzugefügt: Testvorlesung"),
                detailsCaptor.capture(),
                eq(true),
                eq("Student"),
                eq(testStudent.getId())
        );
        
        // Verify details in the activity log
        Map<String, Object> capturedDetails = detailsCaptor.getValue();
        assertNotNull(capturedDetails);
        assertEquals(testKurs.getId(), capturedDetails.get("kursId"));
        assertEquals(testKurs.getName(), capturedDetails.get("kursName"));
        assertEquals(testStudent.getId(), capturedDetails.get("studentId"));
        assertEquals(matrikelnummer, capturedDetails.get("matrikelnummer"));
    }
    
    @Test
    void addMultipleStudentsByMatrikelnummerToKurs_ShouldCreateNewBelegungen() {
        // Arrange
        List<String> matrikelnummern = List.of("12345678", "87654321");
        
        when(nutzerService.getStudentByMatrikelnummer("12345678")).thenReturn(Optional.empty());
        when(nutzerService.erstelleDummyStudent("12345678")).thenReturn(testStudentDTO);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        
        Student testStudent2 = new Student();
        testStudent2.setId(2L);
        testStudent2.setMatrikelnummer("87654321");
        testStudent2.setVorname("Anna");
        testStudent2.setNachname("Beispiel");

        StudentDTO testStudentDTO2 = new StudentDTO();
        testStudentDTO2.setId(2L);
        testStudentDTO2.setMatrikelnummer("87654321");
        testStudentDTO2.setVorname("Anna");
        testStudentDTO2.setNachname("Beispiel");

        when(nutzerService.getStudentByMatrikelnummer("87654321")).thenReturn(Optional.empty());
        when(nutzerService.erstelleDummyStudent("87654321")).thenReturn(testStudentDTO2);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(testStudent2));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(any(Student.class), eq(testKurs))).thenReturn(Optional.empty());
        when(belegungRepository.save(any(Belegung.class))).thenReturn(testBelegung);
        when(belegungMapper.toDto(any(Belegung.class))).thenReturn(testBelegungDTO);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(null);

        // Act
        List<BelegungDTO> results = belegungService.addMultipleStudentsByMatrikelnummerToKurs(
                matrikelnummern, 1L, LocalDate.now().minusDays(10), LocalDate.now().plusDays(100));

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(belegungRepository, times(2)).save(any(Belegung.class));
        verify(aktivitaetsProtokollierungService, times(2)).protokolliereAktivitaet(
                eq(null),
                eq(AktivitaetsTyp.NUTZER_ERSTELLT),
                eq("Neuer Nutzer erstellt"),
                eq(null),
                eq(true),
                eq("Student"),
                any(Long.class)
        );
    }
}