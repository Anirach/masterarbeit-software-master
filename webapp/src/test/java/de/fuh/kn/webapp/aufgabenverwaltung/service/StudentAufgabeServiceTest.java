package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests für StudentAufgabeService.
 * Testet die Geschäftslogik für die Verwaltung von Aufgaben aus Studentensicht.
 */
@ExtendWith(MockitoExtension.class)
class StudentAufgabeServiceTest {

    @Mock
    private AufgabeService aufgabeService;

    @Mock
    private AufgabeZugangsService aufgabeZugangsService;

    @Mock
    private LoesungsversuchService loesungsversuchService;

    @InjectMocks
    private StudentAufgabeService studentAufgabeService;

    private AufgabeDto testAufgabe;
    private TeilaufgabeDto teilaufgabe1;
    private TeilaufgabeDto teilaufgabe2;
    private TeilaufgabeDto teilaufgabe3;
    private Long studentId;

    @BeforeEach
    void setUp() {
        studentId = 1L;
        
        // Test-Teilaufgaben erstellen
        teilaufgabe1 = new TeilaufgabeDto();
        teilaufgabe1.setId(101L);
        teilaufgabe1.setReihenfolge(1);
        teilaufgabe1.setAufgabenstellungMarkdown("Aufgabe 1 Markdown");
        
        teilaufgabe2 = new TeilaufgabeDto();
        teilaufgabe2.setId(102L);
        teilaufgabe2.setReihenfolge(2);
        teilaufgabe2.setAufgabenstellungMarkdown("Aufgabe 2 Markdown");
        
        teilaufgabe3 = new TeilaufgabeDto();
        teilaufgabe3.setId(103L);
        teilaufgabe3.setReihenfolge(3);
        teilaufgabe3.setAufgabenstellungMarkdown("Aufgabe 3 Markdown");
        
        // Test-Aufgabe erstellen
        testAufgabe = new AufgabeDto();
        testAufgabe.setId(10L);
        testAufgabe.setTitel("Test Aufgabe");
        testAufgabe.setTeilaufgaben(Arrays.asList(teilaufgabe1, teilaufgabe2, teilaufgabe3));
    }

    @Test
    void hatZugangZuAufgabe_StudentHatZugang_ReturnsTrue() {
        // Arrange
        Long aufgabeId = 10L;
        when(aufgabeZugangsService.hatZugangZuAufgabe(aufgabeId, studentId)).thenReturn(true);
        
        // Act
        boolean result = studentAufgabeService.hatZugangZuAufgabe(aufgabeId, studentId);
        
        // Assert
        assertTrue(result);
        verify(aufgabeZugangsService).hatZugangZuAufgabe(aufgabeId, studentId);
    }

    @Test
    void hatZugangZuAufgabe_StudentHatKeinenZugang_ReturnsFalse() {
        // Arrange
        Long aufgabeId = 10L;
        when(aufgabeZugangsService.hatZugangZuAufgabe(aufgabeId, studentId)).thenReturn(false);
        
        // Act
        boolean result = studentAufgabeService.hatZugangZuAufgabe(aufgabeId, studentId);
        
        // Assert
        assertFalse(result);
        verify(aufgabeZugangsService).hatZugangZuAufgabe(aufgabeId, studentId);
    }

    @Test
    void ermittleAktiveTeilaufgabe_MitTeilaufgabeId_ReturnsSpecificTeilaufgabe() {
        // Arrange
        Long teilaufgabeId = 102L;
        
        // Act
        TeilaufgabeDto result = studentAufgabeService.ermittleAktiveTeilaufgabe(
            testAufgabe, teilaufgabeId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(teilaufgabe2, result);
        assertEquals(102L, result.getId());
        // Sollte nicht loesungsversuchService aufrufen, da direkt mit ID gesucht wird
        verifyNoInteractions(loesungsversuchService);
    }

    @Test
    void ermittleAktiveTeilaufgabe_OhneTeilaufgabeIdErsteNichtAbgeschlossen_ReturnsErsteTeilaufgabe() {
        // Arrange
        when(loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, 101L)).thenReturn(false);
        
        // Act
        TeilaufgabeDto result = studentAufgabeService.ermittleAktiveTeilaufgabe(
            testAufgabe, null, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(teilaufgabe1, result);
        verify(loesungsversuchService).istTeilaufgabeAbgeschlossen(studentId, 101L);
    }

    @Test
    void ermittleAktiveTeilaufgabe_ErsteAbgeschlossenZweiteNicht_ReturnsZweiteTeilaufgabe() {
        // Arrange
        when(loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, 101L)).thenReturn(true);
        when(loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, 102L)).thenReturn(false);
        
        // Act
        TeilaufgabeDto result = studentAufgabeService.ermittleAktiveTeilaufgabe(
            testAufgabe, null, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(teilaufgabe2, result);
        verify(loesungsversuchService).istTeilaufgabeAbgeschlossen(studentId, 101L);
        verify(loesungsversuchService).istTeilaufgabeAbgeschlossen(studentId, 102L);
    }

    @Test
    void ermittleAktiveTeilaufgabe_AlleAbgeschlossen_ReturnsErsteTeilaufgabe() {
        // Arrange
        when(loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, 101L)).thenReturn(true);
        when(loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, 102L)).thenReturn(true);
        when(loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, 103L)).thenReturn(true);
        
        // Act
        TeilaufgabeDto result = studentAufgabeService.ermittleAktiveTeilaufgabe(
            testAufgabe, null, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(teilaufgabe1, result);
        verify(loesungsversuchService, times(3)).istTeilaufgabeAbgeschlossen(any(), any());
    }

    @Test
    void ermittleAktiveTeilaufgabe_KeineTeilaufgaben_ReturnsNull() {
        // Arrange
        AufgabeDto leerAufgabe = new AufgabeDto();
        leerAufgabe.setTeilaufgaben(Collections.emptyList());
        
        // Act
        TeilaufgabeDto result = studentAufgabeService.ermittleAktiveTeilaufgabe(
            leerAufgabe, null, studentId);
        
        // Assert
        assertNull(result);
    }

    @Test
    void ermittleAktiveTeilaufgabe_UngueltigeTeilaufgabeIdUndErsteNichtAbgeschlossen_ReturnsErsteTeilaufgabe() {
        // Arrange
        Long ungueltigeTeilaufgabeId = 999L;
        when(loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, 101L)).thenReturn(false);
        
        // Act
        TeilaufgabeDto result = studentAufgabeService.ermittleAktiveTeilaufgabe(
            testAufgabe, ungueltigeTeilaufgabeId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(teilaufgabe1, result);
        verify(loesungsversuchService).istTeilaufgabeAbgeschlossen(studentId, 101L);
    }

    @Test
    void findeNaechsteTeilaufgabe_ZweiteIstNaechste_ReturnsZweiteTeilaufgabe() {
        // Arrange
        Long aktuelleTeillaufgabeId = 101L;
        when(loesungsversuchService.istTeilaufgabeErledigt(studentId, 102L)).thenReturn(false);
        
        // Act
        Optional<TeilaufgabeDto> result = studentAufgabeService.findeNaechsteTeilaufgabe(
            testAufgabe, aktuelleTeillaufgabeId, studentId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(teilaufgabe2, result.get());
        verify(loesungsversuchService).istTeilaufgabeErledigt(studentId, 102L);
    }

    @Test
    void findeNaechsteTeilaufgabe_ZweiteErledigtDritteNicht_ReturnsDritteTeilaufgabe() {
        // Arrange
        Long aktuelleTeillaufgabeId = 101L;
        when(loesungsversuchService.istTeilaufgabeErledigt(studentId, 102L)).thenReturn(true);
        when(loesungsversuchService.istTeilaufgabeErledigt(studentId, 103L)).thenReturn(false);
        
        // Act
        Optional<TeilaufgabeDto> result = studentAufgabeService.findeNaechsteTeilaufgabe(
            testAufgabe, aktuelleTeillaufgabeId, studentId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(teilaufgabe3, result.get());
        verify(loesungsversuchService).istTeilaufgabeErledigt(studentId, 102L);
        verify(loesungsversuchService).istTeilaufgabeErledigt(studentId, 103L);
    }

    @Test
    void findeNaechsteTeilaufgabe_AlleNachfolgendenErledigt_ReturnsEmpty() {
        // Arrange
        Long aktuelleTeillaufgabeId = 101L;
        when(loesungsversuchService.istTeilaufgabeErledigt(studentId, 102L)).thenReturn(true);
        when(loesungsversuchService.istTeilaufgabeErledigt(studentId, 103L)).thenReturn(true);
        
        // Act
        Optional<TeilaufgabeDto> result = studentAufgabeService.findeNaechsteTeilaufgabe(
            testAufgabe, aktuelleTeillaufgabeId, studentId);
        
        // Assert
        assertFalse(result.isPresent());
        verify(loesungsversuchService).istTeilaufgabeErledigt(studentId, 102L);
        verify(loesungsversuchService).istTeilaufgabeErledigt(studentId, 103L);
    }

    @Test
    void findeNaechsteTeilaufgabe_LetzteTeilaufgabe_ReturnsEmpty() {
        // Arrange
        Long aktuelleTeillaufgabeId = 103L;
        
        // Act
        Optional<TeilaufgabeDto> result = studentAufgabeService.findeNaechsteTeilaufgabe(
            testAufgabe, aktuelleTeillaufgabeId, studentId);
        
        // Assert
        assertFalse(result.isPresent());
        // Keine Aufrufe an loesungsversuchService, da keine nachfolgenden Teilaufgaben
        verifyNoInteractions(loesungsversuchService);
    }

    @Test
    void findeNaechsteTeilaufgabe_UngueltigeTeilaufgabeId_ReturnsEmpty() {
        // Arrange
        Long ungueltigeTeilaufgabeId = 999L;
        
        // Act
        Optional<TeilaufgabeDto> result = studentAufgabeService.findeNaechsteTeilaufgabe(
            testAufgabe, ungueltigeTeilaufgabeId, studentId);
        
        // Assert
        assertFalse(result.isPresent());
        // Keine Aufrufe, da die aktuelle Teilaufgabe nicht gefunden wurde
        verifyNoInteractions(loesungsversuchService);
    }

    @Test
    void findeNaechsteTeilaufgabe_KeineTeilaufgaben_ReturnsEmpty() {
        // Arrange
        AufgabeDto leerAufgabe = new AufgabeDto();
        leerAufgabe.setTeilaufgaben(Collections.emptyList());
        Long aktuelleTeillaufgabeId = 101L;
        
        // Act
        Optional<TeilaufgabeDto> result = studentAufgabeService.findeNaechsteTeilaufgabe(
            leerAufgabe, aktuelleTeillaufgabeId, studentId);
        
        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(loesungsversuchService);
    }
}