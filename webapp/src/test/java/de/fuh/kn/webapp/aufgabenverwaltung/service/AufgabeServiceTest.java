package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeMapper;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import de.fuh.kn.webapp.persistence.repository.AufgabeRepository;
import de.fuh.kn.webapp.persistence.repository.KurseinheitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den AufgabeService.
 * Testet die Funktionalität zur Verwaltung von Aufgaben.
 */
@ExtendWith(MockitoExtension.class)
class AufgabeServiceTest {

    @Mock
    private AufgabeRepository aufgabeRepository;

    @Mock
    private KurseinheitRepository kurseinheitRepository;

    @Mock
    private AufgabeMapper aufgabeMapper;

    @Mock
    private TeilaufgabeService teilaufgabeService;

    @InjectMocks
    private AufgabeService aufgabeService;

    private Kurseinheit kurseinheit;
    private Aufgabe aufgabe1;
    private Aufgabe aufgabe2;
    private AufgabeDto aufgabeDto1;
    private AufgabeDto aufgabeDto2;

    @BeforeEach
    void setup() {
        // Testdaten initialisieren
        kurseinheit = new Kurseinheit();
        kurseinheit.setId(1L);
        kurseinheit.setName("Testeinheit");
        kurseinheit.setReihenfolge(1);

        aufgabe1 = new Aufgabe();
        aufgabe1.setId(1L);
        aufgabe1.setTitel("Testaufgabe 1");
        aufgabe1.setReihenfolge(1);
        aufgabe1.setKurseinheit(kurseinheit);
        
        // Eine Teilaufgabe hinzufügen, um "Einfach" zu machen
        Teilaufgabe teilaufgabe1 = new Teilaufgabe();
        teilaufgabe1.setId(1L);
        teilaufgabe1.setAufgabe(aufgabe1);
        List<Teilaufgabe> teilaufgaben1 = new ArrayList<>();
        teilaufgaben1.add(teilaufgabe1);
        aufgabe1.setTeilaufgaben(teilaufgaben1);

        aufgabe2 = new Aufgabe();
        aufgabe2.setId(2L);
        aufgabe2.setTitel("Testaufgabe 2");
        aufgabe2.setAufgabenText("Allgemeiner Aufgabentext");
        aufgabe2.setReihenfolge(2);
        aufgabe2.setKurseinheit(kurseinheit);
        
        // Mehrere Teilaufgaben hinzufügen, um "Nicht Einfach" zu machen
        Teilaufgabe teilaufgabe2a = new Teilaufgabe();
        teilaufgabe2a.setId(2L);
        teilaufgabe2a.setAufgabe(aufgabe2);
        Teilaufgabe teilaufgabe2b = new Teilaufgabe();
        teilaufgabe2b.setId(3L);
        teilaufgabe2b.setAufgabe(aufgabe2);
        List<Teilaufgabe> teilaufgaben2 = new ArrayList<>();
        teilaufgaben2.add(teilaufgabe2a);
        teilaufgaben2.add(teilaufgabe2b);
        aufgabe2.setTeilaufgaben(teilaufgaben2);

        aufgabeDto1 = new AufgabeDto();
        aufgabeDto1.setId(1L);
        aufgabeDto1.setTitel("Testaufgabe 1");
        aufgabeDto1.setReihenfolge(1);
        aufgabeDto1.setKurseinheitId(1L);
        aufgabeDto1.setEinfach(true);

        aufgabeDto2 = new AufgabeDto();
        aufgabeDto2.setId(2L);
        aufgabeDto2.setTitel("Testaufgabe 2");
        aufgabeDto2.setAufgabenText("Allgemeiner Aufgabentext");
        aufgabeDto2.setReihenfolge(2);
        aufgabeDto2.setKurseinheitId(1L);
        aufgabeDto2.setEinfach(false);
    }

    @Test
    void testGetAufgabenByKurseinheitId() {
        // Arrange
        List<Aufgabe> aufgaben = Arrays.asList(aufgabe1, aufgabe2);
        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(kurseinheit));
        when(aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit)).thenReturn(aufgaben);
        when(aufgabeMapper.toDto(aufgabe1)).thenReturn(aufgabeDto1);
        when(aufgabeMapper.toDto(aufgabe2)).thenReturn(aufgabeDto2);

        // Act
        List<AufgabeDto> result = aufgabeService.getAufgabenByKurseinheitId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Testaufgabe 1", result.get(0).getTitel());
        assertEquals("Testaufgabe 2", result.get(1).getTitel());

        // Verify
        verify(kurseinheitRepository).findById(1L);
        verify(aufgabeRepository).findByKurseinheitOrderByReihenfolgeAsc(kurseinheit);
        verify(aufgabeMapper, times(2)).toDto(any(Aufgabe.class));
    }

    @Test
    void testGetAufgabenByKurseinheitId_NotFound() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> aufgabeService.getAufgabenByKurseinheitId(999L)
        );
        assertEquals("Kurseinheit mit ID 999 existiert nicht.", exception.getMessage());

        // Verify
        verify(kurseinheitRepository).findById(999L);
        verify(aufgabeRepository, never()).findByKurseinheitOrderByReihenfolgeAsc(any());
    }

    @Test
    void testGetAufgabeById() {
        // Arrange
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        when(aufgabeMapper.toDto(aufgabe1)).thenReturn(aufgabeDto1);

        // Act
        AufgabeDto result = aufgabeService.getAufgabeById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Testaufgabe 1", result.getTitel());

        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(aufgabeMapper).toDto(aufgabe1);
    }

    @Test
    void testGetAufgabeById_NotFound() {
        // Arrange
        when(aufgabeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        AufgabeDto result = aufgabeService.getAufgabeById(999L);

        // Assert
        assertNull(result);

        // Verify
        verify(aufgabeRepository).findById(999L);
        verify(aufgabeMapper, never()).toDto(any());
    }

    @Test
    void testErstelleAufgabe() {
        // Arrange
        AufgabeDto neueAufgabeDto = new AufgabeDto();
        neueAufgabeDto.setTitel("Neue Aufgabe");
        neueAufgabeDto.setEinfach(true);
        neueAufgabeDto.setKurseinheitId(1L);

        Aufgabe neueAufgabe = new Aufgabe();
        neueAufgabe.setTitel("Neue Aufgabe");

        Aufgabe gespeicherteAufgabe = new Aufgabe();
        gespeicherteAufgabe.setId(3L);
        gespeicherteAufgabe.setTitel("Neue Aufgabe");
        gespeicherteAufgabe.setReihenfolge(3);
        gespeicherteAufgabe.setKurseinheit(kurseinheit);

        AufgabeDto gespeicherteAufgabeDto = new AufgabeDto();
        gespeicherteAufgabeDto.setId(3L);
        gespeicherteAufgabeDto.setTitel("Neue Aufgabe");
        gespeicherteAufgabeDto.setReihenfolge(3);
        gespeicherteAufgabeDto.setKurseinheitId(1L);
        gespeicherteAufgabeDto.setEinfach(true);

        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(kurseinheit));
        when(aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit))
                .thenReturn(Arrays.asList(aufgabe1, aufgabe2));
        when(aufgabeMapper.toEntity(neueAufgabeDto)).thenReturn(neueAufgabe);
        when(aufgabeRepository.save(any(Aufgabe.class))).thenReturn(gespeicherteAufgabe);
        when(aufgabeMapper.toDto(gespeicherteAufgabe)).thenReturn(gespeicherteAufgabeDto);

        // Act
        AufgabeDto result = aufgabeService.erstelleAufgabe(neueAufgabeDto);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Neue Aufgabe", result.getTitel());
        assertEquals(3, result.getReihenfolge());

        // Verify
        verify(kurseinheitRepository).findById(1L);
        verify(aufgabeRepository).findByKurseinheitOrderByReihenfolgeAsc(kurseinheit);
        verify(aufgabeMapper).toEntity(neueAufgabeDto);
        verify(aufgabeRepository).save(any(Aufgabe.class));
        verify(aufgabeMapper).toDto(gespeicherteAufgabe);
    }

    @Test
    void testErstelleAufgabe_WithReihenfolge() {
        // Arrange
        AufgabeDto neueAufgabeDto = new AufgabeDto();
        neueAufgabeDto.setTitel("Neue Aufgabe");
        neueAufgabeDto.setEinfach(true);
        neueAufgabeDto.setKurseinheitId(1L);
        neueAufgabeDto.setReihenfolge(5);

        Aufgabe neueAufgabe = new Aufgabe();
        neueAufgabe.setTitel("Neue Aufgabe");
        neueAufgabe.setReihenfolge(5);

        Aufgabe gespeicherteAufgabe = new Aufgabe();
        gespeicherteAufgabe.setId(3L);
        gespeicherteAufgabe.setTitel("Neue Aufgabe");
        gespeicherteAufgabe.setReihenfolge(5);
        gespeicherteAufgabe.setKurseinheit(kurseinheit);

        AufgabeDto gespeicherteAufgabeDto = new AufgabeDto();
        gespeicherteAufgabeDto.setId(3L);
        gespeicherteAufgabeDto.setTitel("Neue Aufgabe");
        gespeicherteAufgabeDto.setReihenfolge(5);
        gespeicherteAufgabeDto.setKurseinheitId(1L);
        gespeicherteAufgabeDto.setEinfach(true);

        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(kurseinheit));
        when(aufgabeMapper.toEntity(neueAufgabeDto)).thenReturn(neueAufgabe);
        when(aufgabeRepository.save(any(Aufgabe.class))).thenReturn(gespeicherteAufgabe);
        when(aufgabeMapper.toDto(gespeicherteAufgabe)).thenReturn(gespeicherteAufgabeDto);

        // Act
        AufgabeDto result = aufgabeService.erstelleAufgabe(neueAufgabeDto);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Neue Aufgabe", result.getTitel());
        assertEquals(5, result.getReihenfolge());

        // Verify
        verify(kurseinheitRepository).findById(1L);
        verify(aufgabeRepository, never()).findByKurseinheitOrderByReihenfolgeAsc(any());
        verify(aufgabeMapper).toEntity(neueAufgabeDto);
        verify(aufgabeRepository).save(any(Aufgabe.class));
        verify(aufgabeMapper).toDto(gespeicherteAufgabe);
    }

    @Test
    void testErstelleAufgabe_KurseinheitNotFound() {
        // Arrange
        AufgabeDto neueAufgabeDto = new AufgabeDto();
        neueAufgabeDto.setTitel("Neue Aufgabe");
        neueAufgabeDto.setEinfach(true);
        neueAufgabeDto.setKurseinheitId(999L);

        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> aufgabeService.erstelleAufgabe(neueAufgabeDto)
        );
        assertEquals("Kurseinheit mit ID 999 existiert nicht.", exception.getMessage());

        // Verify
        verify(kurseinheitRepository).findById(999L);
        verify(aufgabeRepository, never()).save(any());
    }

    @Test
    void testAktualisiereAufgabe() {
        // Arrange
        AufgabeDto aktualisierteAufgabeDto = new AufgabeDto();
        aktualisierteAufgabeDto.setId(1L);
        aktualisierteAufgabeDto.setTitel("Aktualisierte Aufgabe");
        aktualisierteAufgabeDto.setEinfach(true);
        aktualisierteAufgabeDto.setKurseinheitId(1L);
        aktualisierteAufgabeDto.setReihenfolge(3);

        Aufgabe gespeicherteAufgabe = new Aufgabe();
        gespeicherteAufgabe.setId(1L);
        gespeicherteAufgabe.setTitel("Aktualisierte Aufgabe");
        gespeicherteAufgabe.setReihenfolge(3);
        gespeicherteAufgabe.setKurseinheit(kurseinheit);

        AufgabeDto gespeicherteAufgabeDto = new AufgabeDto();
        gespeicherteAufgabeDto.setId(1L);
        gespeicherteAufgabeDto.setTitel("Aktualisierte Aufgabe");
        gespeicherteAufgabeDto.setReihenfolge(3);
        gespeicherteAufgabeDto.setKurseinheitId(1L);
        gespeicherteAufgabeDto.setEinfach(true);

        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(kurseinheit));
        when(aufgabeRepository.save(any(Aufgabe.class))).thenReturn(gespeicherteAufgabe);
        when(aufgabeMapper.toDto(gespeicherteAufgabe)).thenReturn(gespeicherteAufgabeDto);

        // Act
        AufgabeDto result = aufgabeService.aktualisiereAufgabe(aktualisierteAufgabeDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Aktualisierte Aufgabe", result.getTitel());
        assertEquals(3, result.getReihenfolge());

        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(kurseinheitRepository).findById(1L);
        verify(aufgabeRepository).save(any(Aufgabe.class));
        verify(aufgabeMapper).toDto(gespeicherteAufgabe);
    }

    @Test
    void testAktualisiereAufgabe_WithChangedKurseinheit() {
        // Arrange
        Kurseinheit neueKurseinheit = new Kurseinheit();
        neueKurseinheit.setId(2L);
        neueKurseinheit.setName("Neue Kurseinheit");

        AufgabeDto aktualisierteAufgabeDto = new AufgabeDto();
        aktualisierteAufgabeDto.setId(1L);
        aktualisierteAufgabeDto.setTitel("Aktualisierte Aufgabe");
        aktualisierteAufgabeDto.setEinfach(true);
        aktualisierteAufgabeDto.setKurseinheitId(2L);

        List<Aufgabe> neueKurseinheitAufgaben = Arrays.asList(
                new Aufgabe() {{ setReihenfolge(1); }},
                new Aufgabe() {{ setReihenfolge(2); }}
        );

        Aufgabe gespeicherteAufgabe = new Aufgabe();
        gespeicherteAufgabe.setId(1L);
        gespeicherteAufgabe.setTitel("Aktualisierte Aufgabe");
        gespeicherteAufgabe.setReihenfolge(3);
        gespeicherteAufgabe.setKurseinheit(neueKurseinheit);

        AufgabeDto gespeicherteAufgabeDto = new AufgabeDto();
        gespeicherteAufgabeDto.setId(1L);
        gespeicherteAufgabeDto.setTitel("Aktualisierte Aufgabe");
        gespeicherteAufgabeDto.setReihenfolge(3);
        gespeicherteAufgabeDto.setKurseinheitId(2L);
        gespeicherteAufgabeDto.setEinfach(true);

        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        when(kurseinheitRepository.findById(2L)).thenReturn(Optional.of(neueKurseinheit));
        when(aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(neueKurseinheit))
                .thenReturn(neueKurseinheitAufgaben);
        when(aufgabeRepository.save(any(Aufgabe.class))).thenReturn(gespeicherteAufgabe);
        when(aufgabeMapper.toDto(gespeicherteAufgabe)).thenReturn(gespeicherteAufgabeDto);

        // Act
        AufgabeDto result = aufgabeService.aktualisiereAufgabe(aktualisierteAufgabeDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Aktualisierte Aufgabe", result.getTitel());
        assertEquals(3, result.getReihenfolge());
        assertEquals(2L, result.getKurseinheitId());

        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(kurseinheitRepository).findById(2L);
        verify(aufgabeRepository).findByKurseinheitOrderByReihenfolgeAsc(neueKurseinheit);
        verify(aufgabeRepository).save(any(Aufgabe.class));
        verify(aufgabeMapper).toDto(gespeicherteAufgabe);
    }

    @Test
    void testAktualisiereAufgabe_AufgabeNotFound() {
        // Arrange
        AufgabeDto aktualisierteAufgabeDto = new AufgabeDto();
        aktualisierteAufgabeDto.setId(999L);
        aktualisierteAufgabeDto.setTitel("Aktualisierte Aufgabe");
        aktualisierteAufgabeDto.setKurseinheitId(1L);

        when(aufgabeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> aufgabeService.aktualisiereAufgabe(aktualisierteAufgabeDto)
        );
        assertEquals("Aufgabe mit ID 999 existiert nicht.", exception.getMessage());

        // Verify
        verify(aufgabeRepository).findById(999L);
        verify(aufgabeRepository, never()).save(any());
    }

    @Test
    void testAktualisiereAufgabe_KurseinheitNotFound() {
        // Arrange
        AufgabeDto aktualisierteAufgabeDto = new AufgabeDto();
        aktualisierteAufgabeDto.setId(1L);
        aktualisierteAufgabeDto.setTitel("Aktualisierte Aufgabe");
        aktualisierteAufgabeDto.setKurseinheitId(999L);

        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> aufgabeService.aktualisiereAufgabe(aktualisierteAufgabeDto)
        );
        assertEquals("Kurseinheit mit ID 999 existiert nicht.", exception.getMessage());

        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(kurseinheitRepository).findById(999L);
        verify(aufgabeRepository, never()).save(any());
    }

    @Test
    void testLoescheAufgabe() {
        // Arrange
        when(aufgabeRepository.existsById(1L)).thenReturn(true);

        // Act
        aufgabeService.loescheAufgabe(1L);

        // Verify
        verify(aufgabeRepository).existsById(1L);
        verify(aufgabeRepository).deleteById(1L);
    }

    @Test
    void testLoescheAufgabe_NotFound() {
        // Arrange
        when(aufgabeRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> aufgabeService.loescheAufgabe(999L)
        );
        assertEquals("Aufgabe mit ID 999 existiert nicht.", exception.getMessage());

        // Verify
        verify(aufgabeRepository).existsById(999L);
        verify(aufgabeRepository, never()).deleteById(any());
    }

    @Test
    void testAktualisiereAufgabenReihenfolge() {
        // Arrange
        List<Long> neueSortierung = Arrays.asList(2L, 1L);

        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(kurseinheit));
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe1));
        when(aufgabeRepository.findById(2L)).thenReturn(Optional.of(aufgabe2));
        when(aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit))
                .thenReturn(Arrays.asList(aufgabe2, aufgabe1)); // Umgekehrte Reihenfolge nach Update
        when(aufgabeMapper.toDto(aufgabe2)).thenReturn(aufgabeDto2);
        when(aufgabeMapper.toDto(aufgabe1)).thenReturn(aufgabeDto1);

        // Act
        List<AufgabeDto> result = aufgabeService.aktualisiereAufgabenReihenfolge(1L, neueSortierung);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());

        // Verify
        verify(kurseinheitRepository).findById(1L);
        verify(aufgabeRepository).findById(1L);
        verify(aufgabeRepository).findById(2L);
        verify(aufgabeRepository, times(2)).save(any(Aufgabe.class));
        verify(aufgabeRepository).findByKurseinheitOrderByReihenfolgeAsc(kurseinheit);
        verify(aufgabeMapper, times(2)).toDto(any(Aufgabe.class));
    }

    @Test
    void testAktualisiereAufgabenReihenfolge_KurseinheitNotFound() {
        // Arrange
        List<Long> neueSortierung = Arrays.asList(1L, 2L);

        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> aufgabeService.aktualisiereAufgabenReihenfolge(999L, neueSortierung)
        );
        assertEquals("Kurseinheit mit ID 999 existiert nicht.", exception.getMessage());

        // Verify
        verify(kurseinheitRepository).findById(999L);
        verify(aufgabeRepository, never()).findById(any());
        verify(aufgabeRepository, never()).save(any());
    }
}