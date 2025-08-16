package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchMapper;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsMapper;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsRequestDto;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsResponseDto;
import de.fuh.kn.webapp.llm.service.LlmBewertungService;
import de.fuh.kn.webapp.persistence.entity.LoesungsVersuch;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoesungsversuchServiceTest {

    @Mock
    private LoesungsVersuchRepository loesungsVersuchRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeilaufgabeRepository teilaufgabeRepository;

    @Mock
    private LoesungsVersuchMapper loesungsVersuchMapper;

    @Mock
    private BewertungsMapper bewertungsMapper;

    @Mock
    private LlmBewertungService llmBewertungService;

    @InjectMocks
    private LoesungsversuchService loesungsversuchService;

    @Captor
    private ArgumentCaptor<LoesungsVersuch> loesungsVersuchCaptor;

    private Student student;
    private Teilaufgabe teilaufgabe;
    private LoesungsVersuch uebersprungenerVersuch1;
    private LoesungsVersuch uebersprungenerVersuch2;
    private LoesungsVersuch abgeschlossenerUndUebersprungenerVersuch;
    private LoesungsVersuch nichtUebersprungenerVersuch;

    @BeforeEach
    void setUp() {
        // Testdaten erstellen
        student = new Student();
        student.setId(1L);
        student.setVorname("Max");
        student.setNachname("Mustermann");

        teilaufgabe = new Teilaufgabe();
        teilaufgabe.setId(10L);

        // Verschiedene Arten von Lösungsversuchen erstellen
        uebersprungenerVersuch1 = new LoesungsVersuch();
        uebersprungenerVersuch1.setId(101L);
        uebersprungenerVersuch1.setStudent(student);
        uebersprungenerVersuch1.setTeilaufgabe(teilaufgabe);
        uebersprungenerVersuch1.setZeitpunkt(LocalDateTime.now().minusDays(1));
        uebersprungenerVersuch1.setIstUebersprungen(true);
        uebersprungenerVersuch1.setIstAbgeschlossen(false);

        uebersprungenerVersuch2 = new LoesungsVersuch();
        uebersprungenerVersuch2.setId(102L);
        uebersprungenerVersuch2.setStudent(student);
        uebersprungenerVersuch2.setTeilaufgabe(teilaufgabe);
        uebersprungenerVersuch2.setZeitpunkt(LocalDateTime.now().minusHours(5));
        uebersprungenerVersuch2.setIstUebersprungen(true);
        uebersprungenerVersuch2.setIstAbgeschlossen(false);

        // Dieser Versuch ist abgeschlossen UND übersprungen - sollte nicht zurückgesetzt werden
        abgeschlossenerUndUebersprungenerVersuch = new LoesungsVersuch();
        abgeschlossenerUndUebersprungenerVersuch.setId(103L);
        abgeschlossenerUndUebersprungenerVersuch.setStudent(student);
        abgeschlossenerUndUebersprungenerVersuch.setTeilaufgabe(teilaufgabe);
        abgeschlossenerUndUebersprungenerVersuch.setZeitpunkt(LocalDateTime.now().minusDays(2));
        abgeschlossenerUndUebersprungenerVersuch.setIstUebersprungen(true);
        abgeschlossenerUndUebersprungenerVersuch.setIstAbgeschlossen(true);

        // Normaler Versuch, der nicht übersprungen ist
        nichtUebersprungenerVersuch = new LoesungsVersuch();
        nichtUebersprungenerVersuch.setId(104L);
        nichtUebersprungenerVersuch.setStudent(student);
        nichtUebersprungenerVersuch.setTeilaufgabe(teilaufgabe);
        nichtUebersprungenerVersuch.setZeitpunkt(LocalDateTime.now().minusHours(2));
        nichtUebersprungenerVersuch.setIstUebersprungen(false);
        nichtUebersprungenerVersuch.setIstAbgeschlossen(false);
    }

    @Test
    void setzeUebersprungeneLoesungsversucheZurueck_NurUebersprungeneWerdenZurueckgesetzt() {
        // Vorbereitung
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(loesungsVersuchRepository.findByStudentAndIstUebersprungenTrueAndIstAbgeschlossenFalse(student))
                .thenReturn(Arrays.asList(uebersprungenerVersuch1, uebersprungenerVersuch2));

        // Ausführung
        int anzahl = loesungsversuchService.setzeUebersprungeneLoesungsversucheZurueck(student.getId());

        // Überprüfung
        assertEquals(2, anzahl);
        verify(loesungsVersuchRepository, times(2)).save(loesungsVersuchCaptor.capture());

        List<LoesungsVersuch> gespeicherteVersuche = loesungsVersuchCaptor.getAllValues();
        assertEquals(2, gespeicherteVersuche.size());

        // Überprüfe, dass das übersprungen-Flag zurückgesetzt wurde
        for (LoesungsVersuch versuch : gespeicherteVersuche) {
            assertFalse(versuch.getIstUebersprungen());
        }

        // Überprüfe die IDs der gespeicherten Versuche
        assertTrue(gespeicherteVersuche.stream()
                .map(LoesungsVersuch::getId)
                .allMatch(id -> id.equals(101L) || id.equals(102L)));
    }

    @Test
    void setzeUebersprungeneLoesungsversucheZurueck_KeineUebersprungenenVersuche_ReturnZero() {
        // Vorbereitung
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(loesungsVersuchRepository.findByStudentAndIstUebersprungenTrueAndIstAbgeschlossenFalse(student))
                .thenReturn(Collections.emptyList());

        // Ausführung
        int anzahl = loesungsversuchService.setzeUebersprungeneLoesungsversucheZurueck(student.getId());

        // Überprüfung
        assertEquals(0, anzahl);
        verify(loesungsVersuchRepository, never()).save(any());
    }

    @Test
    void setzeUebersprungeneLoesungsversucheZurueck_NullStudentId_ReturnZero() {
        // Ausführung
        int anzahl = loesungsversuchService.setzeUebersprungeneLoesungsversucheZurueck(null);

        // Überprüfung
        assertEquals(0, anzahl);
        verify(studentRepository, never()).findById(any());
        verify(loesungsVersuchRepository, never()).findByStudentAndIstUebersprungenTrueAndIstAbgeschlossenFalse(any());
        verify(loesungsVersuchRepository, never()).save(any());
    }

    @Test
    void findeStudentIdByEmail_shouldDelegateToRepository() {
        // Arrange
        String email = "student@example.com";
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        // Act
        Optional<Long> result = loesungsversuchService.findeStudentIdByEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(student.getId(), result.get());
        verify(studentRepository).findByEmail(email);
    }

    @Test
    void findeStudentIdByEmail_whenNotFound_shouldReturnEmpty() {
        // Arrange
        String email = "nonexistent@example.com";
        when(studentRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Optional<Long> result = loesungsversuchService.findeStudentIdByEmail(email);

        // Assert
        assertTrue(result.isEmpty());
        verify(studentRepository).findByEmail(email);
    }

    @Test
    void setzeUebersprungeneLoesungsversucheZurueck_shouldHandleExceptions() {
        // Arrange
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(loesungsVersuchRepository.findByStudentAndIstUebersprungenTrueAndIstAbgeschlossenFalse(student))
            .thenThrow(new RuntimeException("Test exception"));

        // Act
        int result = loesungsversuchService.setzeUebersprungeneLoesungsversucheZurueck(student.getId());

        // Assert
        assertEquals(0, result);
        verify(studentRepository).findById(student.getId());
        verify(loesungsVersuchRepository).findByStudentAndIstUebersprungenTrueAndIstAbgeschlossenFalse(student);
        verify(loesungsVersuchRepository, never()).save(any());
    }

    @Test
    @DisplayName("findeNeuesterLoesungsversuch sollte den neuesten nicht zurückgesetzten Versuch finden")
    void findeNeuesterLoesungsversuch_ShouldReturnNewestNotResetAttempt() {
        // Arrange
        LoesungsVersuch neusterVersuch = new LoesungsVersuch();
        neusterVersuch.setId(200L);
        neusterVersuch.setIstZurueckGesetzt(false);

        LoesungsVersuchDTO expectedDto = new LoesungsVersuchDTO();
        expectedDto.setId(200L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.findFirstByStudentAndTeilaufgabeOrderByZeitpunktDesc(student, teilaufgabe))
                .thenReturn(Optional.of(neusterVersuch));
        when(loesungsVersuchMapper.toDto(neusterVersuch)).thenReturn(expectedDto);

        // Act
        Optional<LoesungsVersuchDTO> result = loesungsversuchService.findeNeuesterLoesungsversuch(1L, 10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedDto.getId(), result.get().getId());
        verify(loesungsVersuchMapper).toDto(neusterVersuch);
    }

    @Test
    @DisplayName("findeNeuesterLoesungsversuch sollte empty zurückgeben wenn Versuch zurückgesetzt ist")
    void findeNeuesterLoesungsversuch_ShouldReturnEmptyWhenResetted() {
        // Arrange
        LoesungsVersuch zurueckgesetzterVersuch = new LoesungsVersuch();
        zurueckgesetzterVersuch.setId(200L);
        zurueckgesetzterVersuch.setIstZurueckGesetzt(true);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.findFirstByStudentAndTeilaufgabeOrderByZeitpunktDesc(student, teilaufgabe))
                .thenReturn(Optional.of(zurueckgesetzterVersuch));

        // Act
        Optional<LoesungsVersuchDTO> result = loesungsversuchService.findeNeuesterLoesungsversuch(1L, 10L);

        // Assert
        assertFalse(result.isPresent());
        verify(loesungsVersuchMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("istTeilaufgabeAbgeschlossen sollte true zurückgeben wenn abgeschlossen")
    void istTeilaufgabeAbgeschlossen_ShouldReturnTrueWhenCompleted() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(student, teilaufgabe))
                .thenReturn(true);

        // Act
        boolean result = loesungsversuchService.istTeilaufgabeAbgeschlossen(1L, 10L);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("istTeilaufgabeUebersprungen sollte true zurückgeben wenn übersprungen")
    void istTeilaufgabeUebersprungen_ShouldReturnTrueWhenSkipped() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstUebersprungenTrue(student, teilaufgabe))
                .thenReturn(true);

        // Act
        boolean result = loesungsversuchService.istTeilaufgabeUebersprungen(1L, 10L);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("istTeilaufgabeErledigt sollte true zurückgeben wenn abgeschlossen oder übersprungen")
    void istTeilaufgabeErledigt_ShouldReturnTrueWhenCompletedOrSkipped() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(student, teilaufgabe))
                .thenReturn(false);
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstUebersprungenTrue(student, teilaufgabe))
                .thenReturn(true);

        // Act
        boolean result = loesungsversuchService.istTeilaufgabeErledigt(1L, 10L);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("markiereLoesungsversuchAbgeschlossen sollte Versuch als abgeschlossen markieren")
    void markiereLoesungsversuchAbgeschlossen_ShouldMarkAsCompleted() {
        // Arrange
        LoesungsVersuch versuch = new LoesungsVersuch();
        versuch.setId(100L);
        versuch.setStudent(student);
        versuch.setBewertungPunkte(75);
        versuch.setIstAbgeschlossen(false);

        LoesungsVersuchDTO expectedDto = new LoesungsVersuchDTO();
        expectedDto.setId(100L);
        expectedDto.setIstAbgeschlossen(true);

        when(loesungsVersuchRepository.findById(100L)).thenReturn(Optional.of(versuch));
        when(loesungsVersuchRepository.save(any(LoesungsVersuch.class))).thenReturn(versuch);
        when(loesungsVersuchMapper.toDto(any(LoesungsVersuch.class))).thenReturn(expectedDto);

        // Act
        LoesungsVersuchDTO result = loesungsversuchService.markiereLoesungsversuchAbgeschlossen(100L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDto.getId(), result.getId());
        assertTrue(result.getIstAbgeschlossen());
        verify(loesungsVersuchRepository).save(argThat(lv -> lv.getIstAbgeschlossen()));
    }

    @Test
    @DisplayName("markiereLoesungsversuchAbgeschlossen sollte Exception werfen bei zu wenig Punkten")
    void markiereLoesungsversuchAbgeschlossen_ShouldThrowExceptionWhenNotEnoughPoints() {
        // Arrange
        LoesungsVersuch versuch = new LoesungsVersuch();
        versuch.setId(100L);
        versuch.setStudent(student);
        versuch.setBewertungPunkte(40); // Zu wenig Punkte

        when(loesungsVersuchRepository.findById(100L)).thenReturn(Optional.of(versuch));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                loesungsversuchService.markiereLoesungsversuchAbgeschlossen(100L, 1L));
        verify(loesungsVersuchRepository, never()).save(any());
    }

    @Test
    @DisplayName("markiereLoesungsversuchAbgeschlossen sollte Exception werfen bei falschem Student")
    void markiereLoesungsversuchAbgeschlossen_ShouldThrowExceptionWhenWrongStudent() {
        // Arrange
        Student andererStudent = new Student();
        andererStudent.setId(2L);

        LoesungsVersuch versuch = new LoesungsVersuch();
        versuch.setId(100L);
        versuch.setStudent(andererStudent);
        versuch.setBewertungPunkte(75);

        when(loesungsVersuchRepository.findById(100L)).thenReturn(Optional.of(versuch));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                loesungsversuchService.markiereLoesungsversuchAbgeschlossen(100L, 1L));
        verify(loesungsVersuchRepository, never()).save(any());
    }

    @Test
    @DisplayName("erstelleUebersprungenenLoesungsversuch sollte übersprungenen Versuch erstellen")
    void erstelleUebersprungenenLoesungsversuch_ShouldCreateSkippedAttempt() {
        // Arrange
        LoesungsVersuchDTO expectedDto = new LoesungsVersuchDTO();
        expectedDto.setId(300L);
        expectedDto.setIstUebersprungen(true);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.save(any(LoesungsVersuch.class))).thenAnswer(invocation -> {
            LoesungsVersuch lv = invocation.getArgument(0);
            lv.setId(300L);
            return lv;
        });
        when(loesungsVersuchMapper.toDto(any(LoesungsVersuch.class))).thenReturn(expectedDto);

        // Act
        LoesungsVersuchDTO result = loesungsversuchService.erstelleUebersprungenenLoesungsversuch(1L, 10L);

        // Assert
        assertNotNull(result);
        assertEquals(300L, result.getId());
        assertTrue(result.getIstUebersprungen());
        verify(loesungsVersuchRepository).save(argThat(lv -> 
                lv.getIstUebersprungen() && 
                lv.getStudent().equals(student) && 
                lv.getTeilaufgabe().equals(teilaufgabe)));
    }

    @Test
    @DisplayName("erstelleUndBewerteLoesungsversuch sollte Versuch erstellen und bewerten")
    void erstelleUndBewerteLoesungsversuch_ShouldCreateAndEvaluateAttempt() {
        // Arrange
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("feld1", "Antwort 1");

        BewertungsRequestDto bewertungsRequest = new BewertungsRequestDto();
        BewertungsResponseDto bewertungsResponse = new BewertungsResponseDto();
        bewertungsResponse.setPunkte(85);
        bewertungsResponse.setInputToken(100);
        bewertungsResponse.setOutputToken(50);
        bewertungsResponse.setCost(new BigDecimal("0.10"));

        LoesungsVersuchDTO expectedDto = new LoesungsVersuchDTO();
        expectedDto.setId(400L);
        expectedDto.setBewertungPunkte(85);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(bewertungsMapper.createRequestDto(any(), any())).thenReturn(bewertungsRequest);
        when(llmBewertungService.evaluateSolution(bewertungsRequest)).thenReturn(bewertungsResponse);
        when(bewertungsMapper.updateLoesungsVersuch(any(), any())).thenAnswer(invocation -> {
            LoesungsVersuch lv = invocation.getArgument(0);
            lv.setBewertungPunkte(85);
            return lv;
        });
        when(loesungsVersuchRepository.save(any(LoesungsVersuch.class))).thenAnswer(invocation -> {
            LoesungsVersuch lv = invocation.getArgument(0);
            lv.setId(400L);
            return lv;
        });
        when(loesungsVersuchMapper.toDto(any(LoesungsVersuch.class))).thenReturn(expectedDto);

        // Act
        LoesungsVersuchDTO result = loesungsversuchService.erstelleUndBewerteLoesungsversuch(1L, 10L, loesungFelder);

        // Assert
        assertNotNull(result);
        assertEquals(400L, result.getId());
        assertEquals(85, result.getBewertungPunkte());
        verify(llmBewertungService).evaluateSolution(bewertungsRequest);
        verify(bewertungsMapper).updateLoesungsVersuch(any(), eq(bewertungsResponse));
    }

    @Test
    @DisplayName("hatMindestens50ProzentErreicht sollte true zurückgeben bei genügend Punkten")
    void hatMindestens50ProzentErreicht_ShouldReturnTrueWhenEnoughPoints() {
        // Arrange
        LoesungsVersuch versuchMitGenugPunkten = new LoesungsVersuch();
        versuchMitGenugPunkten.setBewertungPunkte(60);

        LoesungsVersuch versuchMitWenigPunkten = new LoesungsVersuch();
        versuchMitWenigPunkten.setBewertungPunkte(30);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.findByStudentAndTeilaufgabe(student, teilaufgabe))
                .thenReturn(Arrays.asList(versuchMitGenugPunkten, versuchMitWenigPunkten));

        // Act
        boolean result = loesungsversuchService.hatMindestens50ProzentErreicht(1L, 10L);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hatMindestens50ProzentErreicht sollte false zurückgeben bei zu wenig Punkten")
    void hatMindestens50ProzentErreicht_ShouldReturnFalseWhenNotEnoughPoints() {
        // Arrange
        LoesungsVersuch versuch1 = new LoesungsVersuch();
        versuch1.setBewertungPunkte(30);

        LoesungsVersuch versuch2 = new LoesungsVersuch();
        versuch2.setBewertungPunkte(40);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.findByStudentAndTeilaufgabe(student, teilaufgabe))
                .thenReturn(Arrays.asList(versuch1, versuch2));

        // Act
        boolean result = loesungsversuchService.hatMindestens50ProzentErreicht(1L, 10L);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("findeNeuesteLoesungsversucheFuerAlleTeilaufgaben sollte Map mit Versuchen zurückgeben")
    void findeNeuesteLoesungsversucheFuerAlleTeilaufgaben_ShouldReturnMapWithAttempts() {
        // Arrange
        List<Long> teilaufgabeIds = Arrays.asList(10L, 20L, 30L);

        LoesungsVersuchDTO dto1 = new LoesungsVersuchDTO();
        dto1.setTeilaufgabeId(10L);

        LoesungsVersuchDTO dto2 = new LoesungsVersuchDTO();
        dto2.setTeilaufgabeId(20L);

        Teilaufgabe teilaufgabe2 = new Teilaufgabe();
        teilaufgabe2.setId(20L);

        LoesungsVersuch versuch1 = new LoesungsVersuch();
        versuch1.setIstZurueckGesetzt(false);

        LoesungsVersuch versuch2 = new LoesungsVersuch();
        versuch2.setIstZurueckGesetzt(false);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(teilaufgabeRepository.findById(20L)).thenReturn(Optional.of(teilaufgabe2));
        when(teilaufgabeRepository.findById(30L)).thenReturn(Optional.of(new Teilaufgabe()));

        when(loesungsVersuchRepository.findFirstByStudentAndTeilaufgabeOrderByZeitpunktDesc(eq(student), any()))
                .thenReturn(Optional.empty());
        when(loesungsVersuchRepository.findFirstByStudentAndTeilaufgabeOrderByZeitpunktDesc(eq(student), eq(teilaufgabe)))
                .thenReturn(Optional.of(versuch1));
        when(loesungsVersuchRepository.findFirstByStudentAndTeilaufgabeOrderByZeitpunktDesc(eq(student), eq(teilaufgabe2)))
                .thenReturn(Optional.of(versuch2));


        when(loesungsVersuchMapper.toDto(versuch1)).thenReturn(dto1);
        when(loesungsVersuchMapper.toDto(versuch2)).thenReturn(dto2);

        // Act
        Map<Long, LoesungsVersuchDTO> result = loesungsversuchService.findeNeuesteLoesungsversucheFuerAlleTeilaufgaben(1L, teilaufgabeIds);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey(10L));
        assertTrue(result.containsKey(20L));
        assertFalse(result.containsKey(30L));
    }

    @Test
    @DisplayName("findeAlleLösungsversuche sollte alle Versuche sortiert zurückgeben")
    void findeAlleLösungsversuche_ShouldReturnAllAttemptsSorted() {
        // Arrange
        LoesungsVersuch versuch1 = new LoesungsVersuch();
        versuch1.setId(101L);
        versuch1.setZeitpunkt(LocalDateTime.now().minusDays(1));

        LoesungsVersuch versuch2 = new LoesungsVersuch();
        versuch2.setId(102L);
        versuch2.setZeitpunkt(LocalDateTime.now());
        versuch2.setIstZurueckGesetzt(true); // Auch zurückgesetzte sollten angezeigt werden

        LoesungsVersuchDTO dto1 = new LoesungsVersuchDTO();
        dto1.setId(101L);

        LoesungsVersuchDTO dto2 = new LoesungsVersuchDTO();
        dto2.setId(102L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.findByStudentAndTeilaufgabeOrderByZeitpunktDesc(student, teilaufgabe))
                .thenReturn(Arrays.asList(versuch2, versuch1)); // Neueste zuerst
        when(loesungsVersuchMapper.toDto(versuch2)).thenReturn(dto2);
        when(loesungsVersuchMapper.toDto(versuch1)).thenReturn(dto1);

        // Act
        List<LoesungsVersuchDTO> result = loesungsversuchService.findeAlleLösungsversuche(1L, 10L);

        // Assert
        assertEquals(2, result.size());
        assertEquals(102L, result.get(0).getId()); // Neueste zuerst
        assertEquals(101L, result.get(1).getId());
    }

    @Test
    @DisplayName("setzeLoesungsversucheZurueck sollte alle Versuche zurücksetzen")
    void setzeLoesungsversucheZurueck_ShouldResetAllAttempts() {
        // Arrange
        LoesungsVersuch versuch1 = new LoesungsVersuch();
        versuch1.setId(101L);
        versuch1.setIstZurueckGesetzt(false);

        LoesungsVersuch versuch2 = new LoesungsVersuch();
        versuch2.setId(102L);
        versuch2.setIstZurueckGesetzt(false);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(10L)).thenReturn(Optional.of(teilaufgabe));
        when(loesungsVersuchRepository.findByStudentAndTeilaufgabe(student, teilaufgabe))
                .thenReturn(Arrays.asList(versuch1, versuch2));

        // Act
        int result = loesungsversuchService.setzeLoesungsversucheZurueck(1L, 10L);

        // Assert
        assertEquals(2, result);
        verify(loesungsVersuchRepository, times(2)).save(loesungsVersuchCaptor.capture());

        List<LoesungsVersuch> gespeicherteVersuche = loesungsVersuchCaptor.getAllValues();
        assertTrue(gespeicherteVersuche.stream().allMatch(LoesungsVersuch::getIstZurueckGesetzt));
    }

    @Test
    @DisplayName("Student nicht gefunden sollte IllegalArgumentException werfen")
    void whenStudentNotFound_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                loesungsversuchService.findeNeuesterLoesungsversuch(999L, 10L));
        assertThrows(IllegalArgumentException.class, () ->
                loesungsversuchService.istTeilaufgabeAbgeschlossen(999L, 10L));
        assertThrows(IllegalArgumentException.class, () ->
                loesungsversuchService.erstelleUebersprungenenLoesungsversuch(999L, 10L));
    }

    @Test
    @DisplayName("Teilaufgabe nicht gefunden sollte IllegalArgumentException werfen")
    void whenTeilaufgabeNotFound_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                loesungsversuchService.findeNeuesterLoesungsversuch(1L, 999L));
        assertThrows(IllegalArgumentException.class, () ->
                loesungsversuchService.istTeilaufgabeAbgeschlossen(1L, 999L));
        assertThrows(IllegalArgumentException.class, () ->
                loesungsversuchService.erstelleUebersprungenenLoesungsversuch(1L, 999L));
    }
}