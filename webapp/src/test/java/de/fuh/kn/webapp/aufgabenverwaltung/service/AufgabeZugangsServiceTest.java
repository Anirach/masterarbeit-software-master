package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.service.StudentService;
import de.fuh.kn.webapp.persistence.entity.*;
import de.fuh.kn.webapp.persistence.repository.AufgabeRepository;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Use Lenient mode for Mockito to allow stubbing with different arguments
@MockitoSettings(strictness = Strictness.LENIENT)
class AufgabeZugangsServiceTest {

    @Mock
    private AufgabeRepository aufgabeRepository;

    @Mock
    private LoesungsVersuchRepository loesungsVersuchRepository;
    
    @Mock
    private StudentService studentService;

    @InjectMocks
    private AufgabeZugangsService aufgabeZugangsService;

    private Kurs kurs;
    private Kurseinheit kurseinheit1;
    private Kurseinheit kurseinheit2;
    private Aufgabe aufgabe1;
    private Aufgabe aufgabe2;
    private Aufgabe aufgabe3;
    private Aufgabe aufgabe4;
    private Student student;
    private Teilaufgabe teilaufgabe1;
    private Teilaufgabe teilaufgabe2;
    private Teilaufgabe teilaufgabe3;
    private Teilaufgabe teilaufgabe4;
    private LoesungsVersuch abgeschlossenerVersuch;
    private LoesungsVersuch uebersprungenerVersuch;
    private LoesungsVersuch unvollstaendigerVersuch;

    @BeforeEach
    void setUp() {
        // Set default behavior for methods that might be called with different arguments
        lenient().when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(anyLong(), anyLong()))
                .thenReturn(false);
        lenient().when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(anyLong(), anyLong()))
                .thenReturn(false);

        // Testdaten erstellen
        kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Testkurs");

        // Zwei Kurseinheiten
        kurseinheit1 = new Kurseinheit();
        kurseinheit1.setId(1L);
        kurseinheit1.setName("Kurseinheit 1");
        kurseinheit1.setReihenfolge(1);
        kurseinheit1.setKurs(kurs);

        kurseinheit2 = new Kurseinheit();
        kurseinheit2.setId(2L);
        kurseinheit2.setName("Kurseinheit 2");
        kurseinheit2.setReihenfolge(2);
        kurseinheit2.setKurs(kurs);

        // Aufgaben
        aufgabe1 = new Aufgabe();
        aufgabe1.setId(1L);
        aufgabe1.setTitel("Aufgabe 1");
        aufgabe1.setReihenfolge(1);
        aufgabe1.setKurseinheit(kurseinheit1);

        aufgabe2 = new Aufgabe();
        aufgabe2.setId(2L);
        aufgabe2.setTitel("Aufgabe 2");
        aufgabe2.setReihenfolge(2);
        aufgabe2.setKurseinheit(kurseinheit1);

        aufgabe3 = new Aufgabe();
        aufgabe3.setId(3L);
        aufgabe3.setTitel("Aufgabe 3");
        aufgabe3.setReihenfolge(1);
        aufgabe3.setKurseinheit(kurseinheit2);

        aufgabe4 = new Aufgabe();
        aufgabe4.setId(4L);
        aufgabe4.setTitel("Aufgabe 4");
        aufgabe4.setReihenfolge(2);
        aufgabe4.setKurseinheit(kurseinheit2);

        // Student
        student = new Student();
        student.setId(1L);
        student.setVorname("Max");
        student.setNachname("Mustermann");

        // Teilaufgaben
        teilaufgabe1 = new Teilaufgabe();
        teilaufgabe1.setId(1L);
        teilaufgabe1.setAufgabe(aufgabe1);
        teilaufgabe1.setReihenfolge(1);

        teilaufgabe2 = new Teilaufgabe();
        teilaufgabe2.setId(2L);
        teilaufgabe2.setAufgabe(aufgabe2);
        teilaufgabe2.setReihenfolge(1);

        teilaufgabe3 = new Teilaufgabe();
        teilaufgabe3.setId(3L);
        teilaufgabe3.setAufgabe(aufgabe3);
        teilaufgabe3.setReihenfolge(1);

        teilaufgabe4 = new Teilaufgabe();
        teilaufgabe4.setId(4L);
        teilaufgabe4.setAufgabe(aufgabe4);
        teilaufgabe4.setReihenfolge(1);

        // Lösungsversuche
        abgeschlossenerVersuch = new LoesungsVersuch();
        abgeschlossenerVersuch.setId(1L);
        abgeschlossenerVersuch.setStudent(student);
        abgeschlossenerVersuch.setTeilaufgabe(teilaufgabe1);
        abgeschlossenerVersuch.setZeitpunkt(LocalDateTime.now());
        abgeschlossenerVersuch.setIstAbgeschlossen(true);
        abgeschlossenerVersuch.setIstUebersprungen(false);

        uebersprungenerVersuch = new LoesungsVersuch();
        uebersprungenerVersuch.setId(2L);
        uebersprungenerVersuch.setStudent(student);
        uebersprungenerVersuch.setTeilaufgabe(teilaufgabe2);
        uebersprungenerVersuch.setZeitpunkt(LocalDateTime.now());
        uebersprungenerVersuch.setIstAbgeschlossen(false);
        uebersprungenerVersuch.setIstUebersprungen(true);

        unvollstaendigerVersuch = new LoesungsVersuch();
        unvollstaendigerVersuch.setId(3L);
        unvollstaendigerVersuch.setStudent(student);
        unvollstaendigerVersuch.setTeilaufgabe(teilaufgabe3);
        unvollstaendigerVersuch.setZeitpunkt(LocalDateTime.now());
        unvollstaendigerVersuch.setIstAbgeschlossen(false);
        unvollstaendigerVersuch.setIstUebersprungen(false);

        // Verknüpfungen erstellen
        List<Teilaufgabe> teilaufgaben1 = new ArrayList<>();
        teilaufgaben1.add(teilaufgabe1);
        aufgabe1.setTeilaufgaben(teilaufgaben1);

        List<Teilaufgabe> teilaufgaben2 = new ArrayList<>();
        teilaufgaben2.add(teilaufgabe2);
        aufgabe2.setTeilaufgaben(teilaufgaben2);

        List<Teilaufgabe> teilaufgaben3 = new ArrayList<>();
        teilaufgaben3.add(teilaufgabe3);
        aufgabe3.setTeilaufgaben(teilaufgaben3);

        List<Teilaufgabe> teilaufgaben4 = new ArrayList<>();
        teilaufgaben4.add(teilaufgabe4);
        aufgabe4.setTeilaufgaben(teilaufgaben4);
    }

    @Test
    void hatZugangZuAufgabe_ErsteAufgabeErsteKurseinheit_ReturnsTrue() {
        // Vorbereitung
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        // The service now uses existsById instead of fetching the entire entity
        when(studentService.existsById(1L)).thenReturn(true);

        // Ausführung
        boolean result = aufgabeZugangsService.hatZugangZuAufgabe(1L, 1L);

        // Überprüfung
        assertTrue(result);
        verify(aufgabeRepository, times(1)).findById(1L);
        verify(studentService, times(1)).existsById(1L);
        verifyNoMoreInteractions(loesungsVersuchRepository);
    }

    @Test
    void hatZugangZuAufgabe_ZweiteAufgabeErsteKurseinheit_ReturnsTrue() {
        // Vorbereitung
        when(aufgabeRepository.findById(2L)).thenReturn(Optional.of(aufgabe2));
        when(studentService.existsById(1L)).thenReturn(true);
        when(aufgabeRepository.findAll()).thenReturn(List.of(aufgabe1, aufgabe2, aufgabe3, aufgabe4));
        when(aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit1))
                .thenReturn(List.of(aufgabe1, aufgabe2));
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L))
                .thenReturn(true);

        // Ausführung
        boolean result = aufgabeZugangsService.hatZugangZuAufgabe(2L, 1L);

        // Überprüfung
        assertTrue(result);
        verify(aufgabeRepository, times(1)).findById(2L);
        verify(studentService, times(1)).existsById(1L);
        verify(aufgabeRepository, times(1)).findAll();
        verify(aufgabeRepository, times(1)).findByKurseinheitOrderByReihenfolgeAsc(kurseinheit1);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L);
    }

    @Test
    void hatZugangZuAufgabe_ErsteAufgabeZweiteKurseinheit_VorherigeAbgeschlossen_ReturnsTrue() {
        // Vorbereitung
        when(aufgabeRepository.findById(3L)).thenReturn(Optional.of(aufgabe3));
        when(studentService.existsById(1L)).thenReturn(true);
        when(aufgabeRepository.findAll()).thenReturn(List.of(aufgabe1, aufgabe2, aufgabe3, aufgabe4));
        when(aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit1))
                .thenReturn(List.of(aufgabe1, aufgabe2));
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L))
                .thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 2L))
                .thenReturn(true);

        // Ausführung
        boolean result = aufgabeZugangsService.hatZugangZuAufgabe(3L, 1L);

        // Überprüfung
        assertTrue(result);
        verify(aufgabeRepository, times(1)).findById(3L);
        verify(studentService, times(1)).existsById(1L);
        verify(aufgabeRepository, times(1)).findAll();
        verify(aufgabeRepository, times(1)).findByKurseinheitOrderByReihenfolgeAsc(kurseinheit1);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 2L);
    }

    @Test
    void hatZugangZuAufgabe_ErsteAufgabeZweiteKurseinheit_VorherigeNichtAbgeschlossen_ReturnsFalse() {
        // Vorbereitung
        when(aufgabeRepository.findById(3L)).thenReturn(Optional.of(aufgabe3));
        when(studentService.existsById(1L)).thenReturn(true);
        when(aufgabeRepository.findAll()).thenReturn(List.of(aufgabe1, aufgabe2, aufgabe3, aufgabe4));
        when(aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit1))
                .thenReturn(List.of(aufgabe1, aufgabe2));
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L))
                .thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 2L))
                .thenReturn(false);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 2L))
                .thenReturn(false);

        // Ausführung
        boolean result = aufgabeZugangsService.hatZugangZuAufgabe(3L, 1L);

        // Überprüfung
        assertFalse(result);
        verify(aufgabeRepository, times(1)).findById(3L);
        verify(studentService, times(1)).existsById(1L);
        verify(aufgabeRepository, times(1)).findAll();
        verify(aufgabeRepository, times(1)).findByKurseinheitOrderByReihenfolgeAsc(kurseinheit1);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 2L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 2L);
    }

    @Test
    void hatAufgabeAbgeschlossen_AlleAbgeschlossen_ReturnsTrue() {
        // Vorbereitung
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        when(studentService.existsById(1L)).thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L))
                .thenReturn(true);

        // Ausführung
        boolean result = aufgabeZugangsService.hatAufgabeAbgeschlossen(1L, 1L);

        // Überprüfung
        assertTrue(result);
        verify(aufgabeRepository, times(1)).findById(1L);
        verify(studentService, times(1)).existsById(1L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L);
    }

    @Test
    void hatAufgabeAbgeschlossen_AlleUebersprungen_ReturnsTrue() {
        // Vorbereitung
        when(aufgabeRepository.findById(2L)).thenReturn(Optional.of(aufgabe2));
        when(studentService.existsById(1L)).thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 2L))
                .thenReturn(false);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 2L))
                .thenReturn(true);

        // Ausführung
        boolean result = aufgabeZugangsService.hatAufgabeAbgeschlossen(2L, 1L);

        // Überprüfung
        assertTrue(result);
        verify(aufgabeRepository, times(1)).findById(2L);
        verify(studentService, times(1)).existsById(1L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 2L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 2L);
    }

    @Test
    void hatAufgabeAbgeschlossen_NichtAlleAbgeschlossen_ReturnsFalse() {
        // Vorbereitung
        when(aufgabeRepository.findById(3L)).thenReturn(Optional.of(aufgabe3));
        when(studentService.existsById(1L)).thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 3L))
                .thenReturn(false);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 3L))
                .thenReturn(false);

        // Ausführung
        boolean result = aufgabeZugangsService.hatAufgabeAbgeschlossen(3L, 1L);

        // Überprüfung
        assertFalse(result);
        verify(aufgabeRepository, times(1)).findById(3L);
        verify(studentService, times(1)).existsById(1L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 3L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 3L);
    }

    @Test
    void darfZurNaechstenAufgabe_AktuelleAufgabeAbgeschlossen_ReturnsTrue() {
        // Vorbereitung
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        when(studentService.existsById(1L)).thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L))
                .thenReturn(true);

        // Ausführung
        boolean result = aufgabeZugangsService.darfZurNaechstenAufgabe(1L, 1L, 1L);

        // Überprüfung
        assertTrue(result);
        verify(aufgabeRepository, times(1)).findById(1L);
        verify(studentService, times(1)).existsById(1L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 1L);
    }

    @Test
    void darfZurNaechstenAufgabe_AktuelleAufgabeNichtAbgeschlossen_ReturnsFalse() {
        // Vorbereitung
        when(aufgabeRepository.findById(3L)).thenReturn(Optional.of(aufgabe3));
        when(studentService.existsById(1L)).thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 3L))
                .thenReturn(false);
        when(loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 3L))
                .thenReturn(false);

        // Ausführung
        boolean result = aufgabeZugangsService.darfZurNaechstenAufgabe(1L, 3L, 1L);

        // Überprüfung
        assertFalse(result);
        verify(aufgabeRepository, times(1)).findById(3L);
        verify(studentService, times(1)).existsById(1L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(1L, 3L);
        verify(loesungsVersuchRepository, times(1)).existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(1L, 3L);
    }
}