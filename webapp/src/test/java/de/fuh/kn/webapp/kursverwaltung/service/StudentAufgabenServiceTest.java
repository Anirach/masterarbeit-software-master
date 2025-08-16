package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeZugangsService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.StudentService;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAufgabenServiceTest {

    @Mock
    private LoesungsVersuchRepository loesungsVersuchRepository;
    
    @Mock
    private TeilaufgabeRepository teilaufgabeRepository;
    
    @Mock
    private StudentService studentService;
    
    @Mock
    private AufgabeZugangsService aufgabeZugangsService;
    
    @Mock
    private AufgabeService aufgabeService;
    
    private StudentAufgabenService studentAufgabenService;
    
    private StudentDTO testStudent;
    private AufgabeDto testAufgabe;
    private TeilaufgabeDto testTeilaufgabe1;
    private TeilaufgabeDto testTeilaufgabe2;
    
    @BeforeEach
    void setUp() {
        studentAufgabenService = new StudentAufgabenService(
            loesungsVersuchRepository,
            teilaufgabeRepository,
            studentService,
            aufgabeZugangsService,
            aufgabeService
        );
        
        // Test-Daten erstellen
        testStudent = new StudentDTO();
        testStudent.setId(1L);
        testStudent.setEmail("student@test.com");
        
        testTeilaufgabe1 = new TeilaufgabeDto();
        testTeilaufgabe1.setId(10L);
        testTeilaufgabe1.setReihenfolge(1);
        
        testTeilaufgabe2 = new TeilaufgabeDto();
        testTeilaufgabe2.setId(20L);
        testTeilaufgabe2.setReihenfolge(2);
        
        testAufgabe = new AufgabeDto();
        testAufgabe.setId(100L);
        testAufgabe.setTitel("Test Aufgabe");
        testAufgabe.setTeilaufgaben(Arrays.asList(testTeilaufgabe1, testTeilaufgabe2));
    }
    
    @Test
    @DisplayName("bestimmeAufgabenZugangsstatus sollte leere Map zurückgeben für null Aufgaben")
    void testBestimmeAufgabenZugangsstatus_ShouldReturnEmptyMap_WhenAufgabenIsNull() {
        // Act
        Map<Long, Boolean> result = studentAufgabenService.bestimmeAufgabenZugangsstatus(testStudent, null);
        
        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(aufgabeZugangsService);
    }
    
    @Test
    @DisplayName("bestimmeAufgabenZugangsstatus sollte leere Map zurückgeben für leere Aufgaben-Liste")
    void testBestimmeAufgabenZugangsstatus_ShouldReturnEmptyMap_WhenAufgabenIsEmpty() {
        // Act
        Map<Long, Boolean> result = studentAufgabenService.bestimmeAufgabenZugangsstatus(testStudent, Collections.emptyList());
        
        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(aufgabeZugangsService);
    }
    
    @Test
    @DisplayName("bestimmeAufgabenZugangsstatus sollte Zugangsstatus für Aufgaben zurückgeben")
    void testBestimmeAufgabenZugangsstatus_ShouldReturnAccessStatus_WhenAufgabenExist() {
        // Arrange
        List<AufgabeDto> aufgaben = Arrays.asList(testAufgabe);
        when(aufgabeZugangsService.hatZugangZuAufgabe(testAufgabe.getId(), testStudent.getId())).thenReturn(true);
        
        // Act
        Map<Long, Boolean> result = studentAufgabenService.bestimmeAufgabenZugangsstatus(testStudent, aufgaben);
        
        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(testAufgabe.getId()));
        verify(aufgabeZugangsService).hatZugangZuAufgabe(testAufgabe.getId(), testStudent.getId());
    }
    
    @Test
    @DisplayName("bestimmeAufgabenZugangsstatus sollte mehrere Aufgaben verarbeiten")
    void testBestimmeAufgabenZugangsstatus_ShouldProcessMultipleAufgaben() {
        // Arrange
        AufgabeDto aufgabe2 = new AufgabeDto();
        aufgabe2.setId(200L);
        List<AufgabeDto> aufgaben = Arrays.asList(testAufgabe, aufgabe2);
        
        when(aufgabeZugangsService.hatZugangZuAufgabe(testAufgabe.getId(), testStudent.getId())).thenReturn(true);
        when(aufgabeZugangsService.hatZugangZuAufgabe(aufgabe2.getId(), testStudent.getId())).thenReturn(false);
        
        // Act
        Map<Long, Boolean> result = studentAufgabenService.bestimmeAufgabenZugangsstatus(testStudent, aufgaben);
        
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.get(testAufgabe.getId()));
        assertFalse(result.get(aufgabe2.getId()));
    }
    
    @Test
    @DisplayName("zaehleAbgeschlosseneTeilaufgaben sollte 0 zurückgeben für null Aufgabe")
    void testZaehleAbgeschlosseneTeilaufgaben_ShouldReturn0_WhenAufgabeIsNull() {
        // Act
        int result = studentAufgabenService.zaehleAbgeschlosseneTeilaufgaben(null, 1L);
        
        // Assert
        assertEquals(0, result);
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("zaehleAbgeschlosseneTeilaufgaben sollte 0 zurückgeben für Aufgabe ohne Teilaufgaben")
    void testZaehleAbgeschlosseneTeilaufgaben_ShouldReturn0_WhenNoTeilaufgaben() {
        // Arrange
        testAufgabe.setTeilaufgaben(Collections.emptyList());
        
        // Act
        int result = studentAufgabenService.zaehleAbgeschlosseneTeilaufgaben(testAufgabe, 1L);
        
        // Assert
        assertEquals(0, result);
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("zaehleAbgeschlosseneTeilaufgaben sollte Anzahl abgeschlossener Teilaufgaben zurückgeben")
    void testZaehleAbgeschlosseneTeilaufgaben_ShouldReturnCompletedCount() {
        // Arrange
        Long studentId = 1L;
        List<Long> teilaufgabenIds = Arrays.asList(10L, 20L);
        List<Long> abgeschlosseneTeilaufgabenIds = Arrays.asList(10L); // Nur eine abgeschlossen
        
        when(loesungsVersuchRepository.findAbgeschlosseneTeilaufgabenIdsByStudentId(studentId, teilaufgabenIds))
            .thenReturn(abgeschlosseneTeilaufgabenIds);
        
        // Act
        int result = studentAufgabenService.zaehleAbgeschlosseneTeilaufgaben(testAufgabe, studentId);
        
        // Assert
        assertEquals(1, result);
        verify(loesungsVersuchRepository).findAbgeschlosseneTeilaufgabenIdsByStudentId(studentId, teilaufgabenIds);
    }
    
    @Test
    @DisplayName("berechneAufgabenPunktestand sollte 0 zurückgeben für null Aufgabe")
    void testBerechneAufgabenPunktestand_ShouldReturn0_WhenAufgabeIsNull() {
        // Act
        int result = studentAufgabenService.berechneAufgabenPunktestand(null, 1L);
        
        // Assert
        assertEquals(0, result);
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("berechneAufgabenPunktestand sollte 0 zurückgeben für Aufgabe ohne Teilaufgaben")
    void testBerechneAufgabenPunktestand_ShouldReturn0_WhenNoTeilaufgaben() {
        // Arrange
        testAufgabe.setTeilaufgaben(null);
        
        // Act
        int result = studentAufgabenService.berechneAufgabenPunktestand(testAufgabe, 1L);
        
        // Assert
        assertEquals(0, result);
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("berechneDurchschnittlichePunktzahl sollte 0 zurückgeben für leere Teilaufgaben-Liste")
    void testBerechneDurchschnittlichePunktzahl_ShouldReturn0_WhenTeilaufgabenIdsIsEmpty() {
        // Act
        int result = studentAufgabenService.berechneDurchschnittlichePunktzahl(1L, Collections.emptyList());
        
        // Assert
        assertEquals(0, result);
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("berechneDurchschnittlichePunktzahl sollte 0 zurückgeben für null Teilaufgaben-Liste")
    void testBerechneDurchschnittlichePunktzahl_ShouldReturn0_WhenTeilaufgabenIdsIsNull() {
        // Act
        int result = studentAufgabenService.berechneDurchschnittlichePunktzahl(1L, null);
        
        // Assert
        assertEquals(0, result);
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("berechneDurchschnittlichePunktzahl sollte Durchschnitt berechnen")
    void testBerechneDurchschnittlichePunktzahl_ShouldCalculateAverage() {
        // Arrange
        Long studentId = 1L;
        List<Long> teilaufgabenIds = Arrays.asList(10L, 20L);
        
        // Mock für erste Teilaufgabe: 80 Punkte
        when(loesungsVersuchRepository.findBewertungspunkteByStudentIdAndTeilaufgabeId(studentId, 10L))
            .thenReturn(Arrays.asList(80));
        
        // Mock für zweite Teilaufgabe: 90 Punkte
        when(loesungsVersuchRepository.findBewertungspunkteByStudentIdAndTeilaufgabeId(studentId, 20L))
            .thenReturn(Arrays.asList(90));
        
        // Act
        int result = studentAufgabenService.berechneDurchschnittlichePunktzahl(studentId, teilaufgabenIds);
        
        // Assert
        assertEquals(85, result); // (80 + 90) / 2 = 85
        verify(loesungsVersuchRepository).findBewertungspunkteByStudentIdAndTeilaufgabeId(studentId, 10L);
        verify(loesungsVersuchRepository).findBewertungspunkteByStudentIdAndTeilaufgabeId(studentId, 20L);
    }
    
    @Test
    @DisplayName("berechneDurchschnittlichePunktzahl sollte nur bewertete Teilaufgaben berücksichtigen")
    void testBerechneDurchschnittlichePunktzahl_ShouldIgnoreUnratedTeilaufgaben() {
        // Arrange
        Long studentId = 1L;
        List<Long> teilaufgabenIds = Arrays.asList(10L, 20L, 30L);
        
        // Mock für erste Teilaufgabe: 80 Punkte
        when(loesungsVersuchRepository.findBewertungspunkteByStudentIdAndTeilaufgabeId(studentId, 10L))
            .thenReturn(Arrays.asList(80));
        
        // Mock für zweite Teilaufgabe: keine Bewertung
        when(loesungsVersuchRepository.findBewertungspunkteByStudentIdAndTeilaufgabeId(studentId, 20L))
            .thenReturn(Collections.emptyList());
        
        // Mock für dritte Teilaufgabe: 100 Punkte
        when(loesungsVersuchRepository.findBewertungspunkteByStudentIdAndTeilaufgabeId(studentId, 30L))
            .thenReturn(Arrays.asList(100));
        
        // Act
        int result = studentAufgabenService.berechneDurchschnittlichePunktzahl(studentId, teilaufgabenIds);
        
        // Assert
        assertEquals(90, result); // (80 + 100) / 2 = 90 (unbewertete Teilaufgabe wird ignoriert)
    }
    
    @Test
    @DisplayName("berechneAbgeschlosseneTeilaufgabenMap sollte leere Map zurückgeben für null Aufgaben")
    void testBerechneAbgeschlosseneTeilaufgabenMap_ShouldReturnEmptyMap_WhenAufgabenIsNull() {
        // Act
        Map<Long, Integer> result = studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(null, 1L);
        
        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("berechneAbgeschlosseneTeilaufgabenMap sollte leere Map zurückgeben für leere Aufgaben-Liste")
    void testBerechneAbgeschlosseneTeilaufgabenMap_ShouldReturnEmptyMap_WhenAufgabenIsEmpty() {
        // Act
        Map<Long, Integer> result = studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(Collections.emptyList(), 1L);
        
        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(loesungsVersuchRepository);
    }
    
    @Test
    @DisplayName("berechneAbgeschlosseneTeilaufgabenMap sollte Map mit abgeschlossenen Teilaufgaben zurückgeben")
    void testBerechneAbgeschlosseneTeilaufgabenMap_ShouldReturnCompletedSubtasksMap() {
        // Arrange
        Long studentId = 1L;
        
        // Zweite Aufgabe erstellen
        TeilaufgabeDto teilaufgabe3 = new TeilaufgabeDto();
        teilaufgabe3.setId(30L);
        
        AufgabeDto aufgabe2 = new AufgabeDto();
        aufgabe2.setId(200L);
        aufgabe2.setTeilaufgaben(Arrays.asList(teilaufgabe3));
        
        List<AufgabeDto> aufgaben = Arrays.asList(testAufgabe, aufgabe2);
        
        // Mock: Student hat Teilaufgabe 10L und 30L abgeschlossen
        List<Long> alleTeilaufgabenIds = Arrays.asList(10L, 20L, 30L);
        List<Long> abgeschlosseneTeilaufgabenIds = Arrays.asList(10L, 30L);
        
        when(loesungsVersuchRepository.findAbgeschlosseneTeilaufgabenIdsByStudentId(eq(studentId), anyList()))
            .thenReturn(abgeschlosseneTeilaufgabenIds);
        
        // Act
        Map<Long, Integer> result = studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(aufgaben, studentId);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(1, result.get(testAufgabe.getId())); // 1 von 2 Teilaufgaben abgeschlossen
        assertEquals(1, result.get(aufgabe2.getId())); // 1 von 1 Teilaufgabe abgeschlossen
        
        verify(loesungsVersuchRepository).findAbgeschlosseneTeilaufgabenIdsByStudentId(eq(studentId), anyList());
    }
    
    @Test
    @DisplayName("berechneAbgeschlosseneTeilaufgabenMap sollte Aufgaben ohne Teilaufgaben korrekt behandeln")
    void testBerechneAbgeschlosseneTeilaufgabenMap_ShouldHandleAufgabenWithoutTeilaufgaben() {
        // Arrange
        Long studentId = 1L;
        
        AufgabeDto aufgabeOhneTeilaufgaben = new AufgabeDto();
        aufgabeOhneTeilaufgaben.setId(300L);
        aufgabeOhneTeilaufgaben.setTeilaufgaben(Collections.emptyList());
        
        List<AufgabeDto> aufgaben = Arrays.asList(testAufgabe, aufgabeOhneTeilaufgaben);
        
        List<Long> abgeschlosseneTeilaufgabenIds = Arrays.asList(10L);
        when(loesungsVersuchRepository.findAbgeschlosseneTeilaufgabenIdsByStudentId(eq(studentId), anyList()))
            .thenReturn(abgeschlosseneTeilaufgabenIds);
        
        // Act
        Map<Long, Integer> result = studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(aufgaben, studentId);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(1, result.get(testAufgabe.getId())); // 1 von 2 Teilaufgaben abgeschlossen
        assertEquals(0, result.get(aufgabeOhneTeilaufgaben.getId())); // 0 Teilaufgaben
    }
}