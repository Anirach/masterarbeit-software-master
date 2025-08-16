package de.fuh.kn.webapp.uebung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.StudentAufgabenService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import de.fuh.kn.webapp.uebung.dto.AufgabeFortschrittDto;
import de.fuh.kn.webapp.uebung.dto.KursFortschrittDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FortschrittServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeilaufgabeRepository teilaufgabeRepository;

    @Mock
    private LoesungsVersuchRepository loesungsVersuchRepository;

    @Mock
    private AufgabeService aufgabeService;

    @Mock
    private StudentAufgabenService studentAufgabenService;

    @InjectMocks
    private FortschrittService fortschrittService;

    private StudentDTO studentDTO;
    private Student studentEntity;
    private KursDTO kursDTO;
    private List<KurseinheitDTO> kurseinheiten;
    private List<AufgabeDto> aufgaben1;
    private List<AufgabeDto> aufgaben2;

    @BeforeEach
    void setUp() {
        studentDTO = new StudentDTO();
        studentDTO.setId(1L);
        studentDTO.setVorname("Test");
        studentDTO.setNachname("Student");

        studentEntity = new Student();
        studentEntity.setId(1L);
        studentEntity.setVorname("Test");
        studentEntity.setNachname("Student");

        // Erstelle Kurs mit Kurseinheiten
        kursDTO = new KursDTO();
        kursDTO.setId(1L);
        kursDTO.setName("Test Kurs");

        // Erstelle Kurseinheiten
        KurseinheitDTO kurseinheit1 = new KurseinheitDTO();
        kurseinheit1.setId(10L);
        kurseinheit1.setName("Kurseinheit 1");

        KurseinheitDTO kurseinheit2 = new KurseinheitDTO();
        kurseinheit2.setId(20L);
        kurseinheit2.setName("Kurseinheit 2");

        kurseinheiten = Arrays.asList(kurseinheit1, kurseinheit2);
        kursDTO.setKurseinheiten(kurseinheiten);

        // Erstelle Aufgaben für Kurseinheit 1
        AufgabeDto aufgabe1 = new AufgabeDto();
        aufgabe1.setId(100L);
        aufgabe1.setTitel("Aufgabe 1");
        List<TeilaufgabeDto> teilaufgaben1 = new ArrayList<>();
        TeilaufgabeDto teilaufgabe1 = new TeilaufgabeDto();
        teilaufgabe1.setId(1001L);
        TeilaufgabeDto teilaufgabe2 = new TeilaufgabeDto();
        teilaufgabe2.setId(1002L);
        teilaufgaben1.add(teilaufgabe1);
        teilaufgaben1.add(teilaufgabe2);
        aufgabe1.setTeilaufgaben(teilaufgaben1);

        AufgabeDto aufgabe2 = new AufgabeDto();
        aufgabe2.setId(200L);
        aufgabe2.setTitel("Aufgabe 2");
        List<TeilaufgabeDto> teilaufgaben2 = new ArrayList<>();
        TeilaufgabeDto teilaufgabe3 = new TeilaufgabeDto();
        teilaufgabe3.setId(2001L);
        teilaufgaben2.add(teilaufgabe3);
        aufgabe2.setTeilaufgaben(teilaufgaben2);

        aufgaben1 = Arrays.asList(aufgabe1, aufgabe2);

        // Erstelle Aufgaben für Kurseinheit 2
        AufgabeDto aufgabe3 = new AufgabeDto();
        aufgabe3.setId(300L);
        aufgabe3.setTitel("Aufgabe 3");
        List<TeilaufgabeDto> teilaufgaben3 = new ArrayList<>();
        TeilaufgabeDto teilaufgabe4 = new TeilaufgabeDto();
        teilaufgabe4.setId(3001L);
        teilaufgaben3.add(teilaufgabe4);
        aufgabe3.setTeilaufgaben(teilaufgaben3);

        aufgaben2 = List.of(aufgabe3);
    }

    @Test
    void berechneFortschrittShouldCalculateCorrectProgressWhenAufgabenExist() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentEntity));
        when(teilaufgabeRepository.countTeilaufgabenByKursId(1L)).thenReturn(20L);
        when(loesungsVersuchRepository.countByStudentIdAndKursIdAndKorrekt(1L, 1L, true)).thenReturn(5L);

        // Act
        KursFortschrittDTO result = fortschrittService.berechneFortschritt(studentDTO, kursDTO, true);

        // Assert
        assertNotNull(result);
        assertEquals(kursDTO, result.getKurs());
        assertEquals(25, result.getFortschrittProzent()); // 5/20 = 25%
        assertEquals(5L, result.getAbgeschlosseneTeilaufgaben());
        assertEquals(20L, result.getGesamtTeilaufgaben());
        assertTrue(result.isIstAktiv());
    }

    @Test
    void berechneFortschrittShouldReturnZeroProgressWhenNoAufgabenExist() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentEntity));
        when(teilaufgabeRepository.countTeilaufgabenByKursId(1L)).thenReturn(0L);

        // Act
        KursFortschrittDTO result = fortschrittService.berechneFortschritt(studentDTO, kursDTO, false);

        // Assert
        assertNotNull(result);
        assertEquals(kursDTO, result.getKurs());
        assertEquals(0, result.getFortschrittProzent());
        assertEquals(0L, result.getGesamtTeilaufgaben());
        assertFalse(result.isIstAktiv());
    }

    @Test
    void berechneFortschrittFuerKurseShouldCalculateProgressForMultipleCourses() {
        // Arrange
        KursDTO kursDTO2 = new KursDTO();
        kursDTO2.setId(2L);
        kursDTO2.setName("Test Kurs 2");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentEntity));
        when(teilaufgabeRepository.countTeilaufgabenByKursId(1L)).thenReturn(20L);
        when(teilaufgabeRepository.countTeilaufgabenByKursId(2L)).thenReturn(10L);
        when(loesungsVersuchRepository.countByStudentIdAndKursIdAndKorrekt(1L, 1L, true)).thenReturn(5L);
        when(loesungsVersuchRepository.countByStudentIdAndKursIdAndKorrekt(1L, 2L, true)).thenReturn(5L);

        List<KursDTO> kurse = Arrays.asList(kursDTO, kursDTO2);

        // Act
        List<KursFortschrittDTO> results = fortschrittService.berechneFortschrittFuerKurse(studentDTO, kurse, true);

        // Assert
        assertEquals(2, results.size());
        
        // First course
        assertEquals(kursDTO, results.get(0).getKurs());
        assertEquals(25, results.get(0).getFortschrittProzent());
        
        // Second course
        assertEquals(kursDTO2, results.get(1).getKurs());
        assertEquals(50, results.get(1).getFortschrittProzent()); // 5/10 = 50%
    }

    @Test
    void zaehleSolvedAssignmentsShouldReturnCorrectCount() {
        // Arrange
        when(loesungsVersuchRepository.countByStudentIdAndKorrekt(1L, true)).thenReturn(5L);

        // Act
        Long result = fortschrittService.zaehleSolvedAssignments(1L);

        // Assert
        assertEquals(5L, result);
    }

    @Test
    void erstelleAufgabenFortschritteMapShouldCreateMapOfProgressObjects() {
        // Arrange
        // Kombiniere alle Aufgaben
        List<AufgabeDto> alleAufgaben = new ArrayList<>();
        alleAufgaben.addAll(aufgaben1);
        alleAufgaben.addAll(aufgaben2);

        // Setup für aufgabeService mock - wir rufen KurseinheitDTO.getKurseinheiten() im Stream ab
        when(aufgabeService.getAufgabenByKurseinheitId(10L)).thenReturn(aufgaben1);
        when(aufgabeService.getAufgabenByKurseinheitId(20L)).thenReturn(aufgaben2);

        // Setup für Zugriffsstatus
        Map<Long, Boolean> zugaenglich = new HashMap<>();
        zugaenglich.put(100L, true);
        zugaenglich.put(200L, true);
        zugaenglich.put(300L, false);
        when(studentAufgabenService.bestimmeAufgabenZugangsstatus(studentDTO, alleAufgaben)).thenReturn(zugaenglich);

        // Setup für die batch-verarbeiteten abgeschlossenen Teilaufgaben
        Map<Long, Integer> abgeschlosseneTeilaufgaben = new HashMap<>();
        abgeschlosseneTeilaufgaben.put(100L, 1);
        abgeschlosseneTeilaufgaben.put(200L, 0);
        abgeschlosseneTeilaufgaben.put(300L, 0);
        when(studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(alleAufgaben, 1L))
            .thenReturn(abgeschlosseneTeilaufgaben);

        // Setup für Punktestand (wird immer noch einzeln abgerufen)
        when(studentAufgabenService.berechneAufgabenPunktestand(aufgaben1.get(0), 1L)).thenReturn(75);
        when(studentAufgabenService.berechneAufgabenPunktestand(aufgaben1.get(1), 1L)).thenReturn(0);
        when(studentAufgabenService.berechneAufgabenPunktestand(aufgaben2.get(0), 1L)).thenReturn(0);

        // Act
        Map<Long, AufgabeFortschrittDto> result = fortschrittService.erstelleAufgabenFortschritteMap(kursDTO, studentDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsKey(100L));
        assertTrue(result.containsKey(200L));
        assertTrue(result.containsKey(300L));

        // Überprüfe einzelne Fortschritte
        AufgabeFortschrittDto aufgabe1Fortschritt = result.get(100L);
        assertEquals("Aufgabe 1", aufgabe1Fortschritt.getAufgabe().getTitel());
        assertEquals(2, aufgabe1Fortschritt.getAufgabe().getTeilaufgaben().size());
        assertTrue(aufgabe1Fortschritt.isFreigeschaltet());
        assertEquals(1, aufgabe1Fortschritt.getAbgeschlosseneTeilaufgaben());
        assertEquals(75, aufgabe1Fortschritt.getDurchschnittlichePunktzahl());
    }

    @Test
    void erstelleAufgabenFortschritteProKurseinheitMapShouldGroupByKurseinheit() {
        // Arrange
        // Kombiniere alle Aufgaben
        List<AufgabeDto> alleAufgaben = new ArrayList<>();
        alleAufgaben.addAll(aufgaben1);
        alleAufgaben.addAll(aufgaben2);

        // Setup für aufgabeService mock
        when(aufgabeService.getAufgabenByKurseinheitId(10L)).thenReturn(aufgaben1);
        when(aufgabeService.getAufgabenByKurseinheitId(20L)).thenReturn(aufgaben2);

        // Setup für Zugriffsstatus
        Map<Long, Boolean> zugaenglich = new HashMap<>();
        zugaenglich.put(100L, true);
        zugaenglich.put(200L, true);
        zugaenglich.put(300L, false);
        when(studentAufgabenService.bestimmeAufgabenZugangsstatus(studentDTO, alleAufgaben)).thenReturn(zugaenglich);

        // Setup für die batch-verarbeiteten abgeschlossenen Teilaufgaben
        Map<Long, Integer> abgeschlosseneTeilaufgaben = new HashMap<>();
        abgeschlosseneTeilaufgaben.put(100L, 1);
        abgeschlosseneTeilaufgaben.put(200L, 0);
        abgeschlosseneTeilaufgaben.put(300L, 0);
        when(studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(alleAufgaben, 1L))
            .thenReturn(abgeschlosseneTeilaufgaben);

        // Setup für Punktestand (wird immer noch einzeln abgerufen)
        when(studentAufgabenService.berechneAufgabenPunktestand(aufgaben1.get(0), 1L)).thenReturn(75);
        when(studentAufgabenService.berechneAufgabenPunktestand(aufgaben1.get(1), 1L)).thenReturn(0);
        when(studentAufgabenService.berechneAufgabenPunktestand(aufgaben2.get(0), 1L)).thenReturn(0);

        // Act
        Map<Long, List<AufgabeFortschrittDto>> result =
                fortschrittService.erstelleAufgabenFortschritteProKurseinheitMap(kursDTO, studentDTO);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(10L));
        assertTrue(result.containsKey(20L));

        // Überprüfe Anzahl der Aufgaben pro Kurseinheit
        assertEquals(2, result.get(10L).size());
        assertEquals(1, result.get(20L).size());

        // Überprüfe einzelne Fortschritte
        List<AufgabeFortschrittDto> kurseinheit1Fortschritte = result.get(10L);
        assertEquals("Aufgabe 1", kurseinheit1Fortschritte.get(0).getAufgabe().getTitel());
        assertEquals(2, kurseinheit1Fortschritte.get(0).getAufgabe().getTeilaufgaben().size());
        assertTrue(kurseinheit1Fortschritte.get(0).isFreigeschaltet());
        assertEquals(1, kurseinheit1Fortschritte.get(0).getAbgeschlosseneTeilaufgaben());
        assertEquals(75, kurseinheit1Fortschritte.get(0).getDurchschnittlichePunktzahl());

        List<AufgabeFortschrittDto> kurseinheit2Fortschritte = result.get(20L);
        assertEquals("Aufgabe 3", kurseinheit2Fortschritte.get(0).getAufgabe().getTitel());
        assertEquals(1, kurseinheit2Fortschritte.get(0).getAufgabe().getTeilaufgaben().size());
        assertFalse(kurseinheit2Fortschritte.get(0).isFreigeschaltet());
        assertEquals(0, kurseinheit2Fortschritte.get(0).getAbgeschlosseneTeilaufgaben());
        assertEquals(0, kurseinheit2Fortschritte.get(0).getDurchschnittlichePunktzahl());
    }
}