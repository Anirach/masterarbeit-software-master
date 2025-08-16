package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeMapper;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import de.fuh.kn.webapp.persistence.repository.AufgabeRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den TeilaufgabeService.
 * Testet die Funktionalität zur Verwaltung von Teilaufgaben.
 */
@ExtendWith(MockitoExtension.class)
class TeilaufgabeServiceTest {

    @Mock
    private TeilaufgabeRepository teilaufgabeRepository;

    @Mock
    private AufgabeRepository aufgabeRepository;

    @Mock
    private TeilaufgabeMapper teilaufgabeMapper;

    @InjectMocks
    private TeilaufgabeService teilaufgabeService;

    private Aufgabe aufgabe;
    private Teilaufgabe teilaufgabe1;
    private Teilaufgabe teilaufgabe2;
    private TeilaufgabeDto teilaufgabeDto1;
    private TeilaufgabeDto teilaufgabeDto2;

    @BeforeEach
    void setUp() {
        // Initialisierung der Test-Objekte
        aufgabe = new Aufgabe();
        aufgabe.setId(1L);
        aufgabe.setTitel("Test Aufgabe");
        
        teilaufgabe1 = new Teilaufgabe();
        teilaufgabe1.setId(1L);
        teilaufgabe1.setReihenfolge(1);
        teilaufgabe1.setAufgabenstellungMarkdown("Aufgabe 1");
        teilaufgabe1.setAufgabe(aufgabe);
        
        teilaufgabe2 = new Teilaufgabe();
        teilaufgabe2.setId(2L);
        teilaufgabe2.setReihenfolge(2);
        teilaufgabe2.setAufgabenstellungMarkdown("Aufgabe 2");
        teilaufgabe2.setAufgabe(aufgabe);
        
        teilaufgabeDto1 = new TeilaufgabeDto();
        teilaufgabeDto1.setId(1L);
        teilaufgabeDto1.setReihenfolge(1);
        teilaufgabeDto1.setAufgabenstellungMarkdown("Aufgabe 1");
        teilaufgabeDto1.setAufgabeId(1L);
        
        teilaufgabeDto2 = new TeilaufgabeDto();
        teilaufgabeDto2.setId(2L);
        teilaufgabeDto2.setReihenfolge(2);
        teilaufgabeDto2.setAufgabenstellungMarkdown("Aufgabe 2");
        teilaufgabeDto2.setAufgabeId(1L);
    }

    @Test
    void getTeilaufgabenByAufgabeId_ShouldReturnAllTeilaufgaben() {
        // Arrange
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe));
        when(teilaufgabeRepository.findByAufgabeOrderByReihenfolgeAsc(aufgabe))
                .thenReturn(List.of(teilaufgabe1, teilaufgabe2));
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(teilaufgabeDto1);
        when(teilaufgabeMapper.toDto(teilaufgabe2)).thenReturn(teilaufgabeDto2);
        
        // Act
        List<TeilaufgabeDto> result = teilaufgabeService.getTeilaufgabenByAufgabeId(1L);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(teilaufgabeDto1, result.get(0));
        assertEquals(teilaufgabeDto2, result.get(1));
        
        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findByAufgabeOrderByReihenfolgeAsc(aufgabe);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
        verify(teilaufgabeMapper).toDto(teilaufgabe2);
    }
    
    @Test
    void getTeilaufgabeById_ShouldReturnTeilaufgabe_WhenExists() {
        // Arrange
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(teilaufgabeDto1);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.getTeilaufgabeById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(teilaufgabeDto1, result);
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void getTeilaufgabeById_ShouldReturnNull_WhenDoesNotExist() {
        // Arrange
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.getTeilaufgabeById(999L);
        
        // Assert
        assertNull(result);
        
        // Verify
        verify(teilaufgabeRepository).findById(999L);
        verify(teilaufgabeMapper, never()).toDto(any());
    }
    
    @Test
    void erstelleTeilaufgabe_ShouldCreateTeilaufgabe_WithGivenReihenfolge() {
        // Arrange
        TeilaufgabeDto newDto = new TeilaufgabeDto();
        newDto.setAufgabenstellungMarkdown("Neue Aufgabe");
        newDto.setAufgabeId(1L);
        newDto.setReihenfolge(3);
        
        Teilaufgabe newEntity = new Teilaufgabe();
        newEntity.setAufgabenstellungMarkdown("Neue Aufgabe");
        
        Teilaufgabe savedEntity = new Teilaufgabe();
        savedEntity.setId(3L);
        savedEntity.setAufgabenstellungMarkdown("Neue Aufgabe");
        savedEntity.setReihenfolge(3);
        savedEntity.setAufgabe(aufgabe);
        
        TeilaufgabeDto savedDto = new TeilaufgabeDto();
        savedDto.setId(3L);
        savedDto.setAufgabenstellungMarkdown("Neue Aufgabe");
        savedDto.setReihenfolge(3);
        savedDto.setAufgabeId(1L);
        
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe));
        when(teilaufgabeMapper.toEntity(newDto)).thenReturn(newEntity);
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(savedEntity);
        when(teilaufgabeMapper.toDto(savedEntity)).thenReturn(savedDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.erstelleTeilaufgabe(newDto);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.getReihenfolge());
        assertEquals("Neue Aufgabe", result.getAufgabenstellungMarkdown());
        
        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(teilaufgabeMapper).toEntity(newDto);
        verify(teilaufgabeRepository).save(any(Teilaufgabe.class));
        verify(teilaufgabeMapper).toDto(savedEntity);
    }
    
    @Test
    void erstelleTeilaufgabe_ShouldSetNextReihenfolge_WhenReihenfolgeIsNull() {
        // Arrange
        TeilaufgabeDto newDto = new TeilaufgabeDto();
        newDto.setAufgabenstellungMarkdown("Neue Aufgabe");
        newDto.setAufgabeId(1L);
        newDto.setReihenfolge(null);
        
        Teilaufgabe newEntity = new Teilaufgabe();
        newEntity.setAufgabenstellungMarkdown("Neue Aufgabe");
        
        Teilaufgabe savedEntity = new Teilaufgabe();
        savedEntity.setId(3L);
        savedEntity.setAufgabenstellungMarkdown("Neue Aufgabe");
        savedEntity.setReihenfolge(3);
        savedEntity.setAufgabe(aufgabe);
        
        TeilaufgabeDto savedDto = new TeilaufgabeDto();
        savedDto.setId(3L);
        savedDto.setAufgabenstellungMarkdown("Neue Aufgabe");
        savedDto.setReihenfolge(3);
        savedDto.setAufgabeId(1L);
        
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe));
        when(teilaufgabeRepository.findByAufgabeOrderByReihenfolgeAsc(aufgabe))
                .thenReturn(List.of(teilaufgabe1, teilaufgabe2));
        when(teilaufgabeMapper.toEntity(newDto)).thenReturn(newEntity);
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(savedEntity);
        when(teilaufgabeMapper.toDto(savedEntity)).thenReturn(savedDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.erstelleTeilaufgabe(newDto);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.getReihenfolge());
        
        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findByAufgabeOrderByReihenfolgeAsc(aufgabe);
        verify(teilaufgabeMapper).toEntity(newDto);
        verify(teilaufgabeRepository).save(any(Teilaufgabe.class));
        verify(teilaufgabeMapper).toDto(savedEntity);
    }
    
    @Test
    void erstelleTeilaufgabe_ShouldThrowException_WhenAufgabeDoesNotExist() {
        // Arrange
        TeilaufgabeDto newDto = new TeilaufgabeDto();
        newDto.setAufgabenstellungMarkdown("Neue Aufgabe");
        newDto.setAufgabeId(999L);
        newDto.setReihenfolge(1);
        
        when(aufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.erstelleTeilaufgabe(newDto);
        });
        
        assertTrue(exception.getMessage().contains("Aufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(aufgabeRepository).findById(999L);
        verify(teilaufgabeMapper, never()).toEntity(any());
        verify(teilaufgabeRepository, never()).save(any());
    }
    
    @Test
    void aktualisiereTeilaufgabe_ShouldUpdateTeilaufgabe_WhenExists() {
        // Arrange
        TeilaufgabeDto updateDto = new TeilaufgabeDto();
        updateDto.setId(1L);
        updateDto.setAufgabenstellungMarkdown("Aktualisierte Aufgabe");
        updateDto.setAufgabeId(1L);
        updateDto.setReihenfolge(1);
        Map<String, String> musterloesungen = new HashMap<>();
        musterloesungen.put("feld1", "antwort1");
        updateDto.setMusterloesungFelder(musterloesungen);
        updateDto.setMusterloesungBewertungshinweise("Neue Hinweise");
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe));
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(teilaufgabe1);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(updateDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.aktualisiereTeilaufgabe(updateDto);
        
        // Assert
        assertNotNull(result);
        assertEquals("Aktualisierte Aufgabe", result.getAufgabenstellungMarkdown());
        assertEquals("Neue Hinweise", result.getMusterloesungBewertungshinweise());
        assertEquals(1, result.getMusterloesungFelder().size());
        assertEquals("antwort1", result.getMusterloesungFelder().get("feld1"));
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(aufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).save(teilaufgabe1);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void aktualisiereTeilaufgabe_ShouldHandleAufgabeWechsel() {
        // Arrange
        Aufgabe neueAufgabe = new Aufgabe();
        neueAufgabe.setId(2L);
        neueAufgabe.setTitel("Neue Aufgabe");
        
        TeilaufgabeDto updateDto = new TeilaufgabeDto();
        updateDto.setId(1L);
        updateDto.setAufgabenstellungMarkdown("Aktualisierte Aufgabe");
        updateDto.setAufgabeId(2L); // Andere Aufgabe ID
        updateDto.setReihenfolge(1);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(aufgabeRepository.findById(2L)).thenReturn(Optional.of(neueAufgabe));
        when(teilaufgabeRepository.findByAufgabeOrderByReihenfolgeAsc(neueAufgabe))
                .thenReturn(new ArrayList<>());
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(teilaufgabe1);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(updateDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.aktualisiereTeilaufgabe(updateDto);
        
        // Assert
        assertNotNull(result);
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(aufgabeRepository).findById(2L);
        verify(teilaufgabeRepository).findByAufgabeOrderByReihenfolgeAsc(neueAufgabe);
        verify(teilaufgabeRepository).save(any(Teilaufgabe.class));
        verify(teilaufgabeMapper).toDto(any(Teilaufgabe.class));
    }
    
    @Test
    void aktualisiereTeilaufgabe_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        TeilaufgabeDto updateDto = new TeilaufgabeDto();
        updateDto.setId(999L);
        updateDto.setAufgabenstellungMarkdown("Aktualisierte Aufgabe");
        updateDto.setAufgabeId(1L);
        
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.aktualisiereTeilaufgabe(updateDto);
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).findById(999L);
        verify(aufgabeRepository, never()).findById(any());
        verify(teilaufgabeRepository, never()).save(any());
    }
    
    @Test
    void aktualisiereTeilaufgabe_ShouldThrowException_WhenAufgabeDoesNotExist() {
        // Arrange
        TeilaufgabeDto updateDto = new TeilaufgabeDto();
        updateDto.setId(1L);
        updateDto.setAufgabenstellungMarkdown("Aktualisierte Aufgabe");
        updateDto.setAufgabeId(999L);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(aufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.aktualisiereTeilaufgabe(updateDto);
        });
        
        assertTrue(exception.getMessage().contains("Aufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(aufgabeRepository).findById(999L);
        verify(teilaufgabeRepository, never()).save(any());
    }
    
    @Test
    void loescheTeilaufgabe_ShouldDeleteTeilaufgabe_WhenExists() {
        // Arrange
        when(teilaufgabeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(teilaufgabeRepository).deleteById(1L);
        
        // Act
        teilaufgabeService.loescheTeilaufgabe(1L);
        
        // Verify
        verify(teilaufgabeRepository).existsById(1L);
        verify(teilaufgabeRepository).deleteById(1L);
    }
    
    @Test
    void loescheTeilaufgabe_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        when(teilaufgabeRepository.existsById(999L)).thenReturn(false);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.loescheTeilaufgabe(999L);
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).existsById(999L);
        verify(teilaufgabeRepository, never()).deleteById(any());
    }
    
    @Test
    void aktualisiereTeilaufgabenReihenfolge_ShouldUpdateOrder_WhenAllTeilaufgabenExist() {
        // Arrange
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe));
        
        // Mock für die erste Teilaufgabe
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        
        // Mock für die zweite Teilaufgabe
        when(teilaufgabeRepository.findById(2L)).thenReturn(Optional.of(teilaufgabe2));
        
        // Rückgabewert für speichernde Operationen und findByAufgabeOrderByReihenfolgeAsc simulieren
        List<Teilaufgabe> updatedTeilaufgaben = List.of(teilaufgabe2, teilaufgabe1); // Geänderte Reihenfolge!
        when(teilaufgabeRepository.findByAufgabeOrderByReihenfolgeAsc(aufgabe)).thenReturn(updatedTeilaufgaben);
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // DTOs in umgekehrter Reihenfolge
        when(teilaufgabeMapper.toDto(teilaufgabe2)).thenReturn(teilaufgabeDto2);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(teilaufgabeDto1);
        
        // Act
        List<TeilaufgabeDto> result = teilaufgabeService.aktualisiereTeilaufgabenReihenfolge(1L, List.of(2L, 1L));
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(teilaufgabeDto2, result.get(0)); // Teilaufgabe 2 ist jetzt an erster Stelle
        assertEquals(teilaufgabeDto1, result.get(1)); // Teilaufgabe 1 ist jetzt an zweiter Stelle
        
        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findById(2L);
        verify(teilaufgabeRepository, times(2)).save(any(Teilaufgabe.class));
        verify(teilaufgabeRepository).findByAufgabeOrderByReihenfolgeAsc(aufgabe);
        verify(teilaufgabeMapper).toDto(teilaufgabe2);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void aktualisiereTeilaufgabenReihenfolge_ShouldThrowException_WhenAufgabeDoesNotExist() {
        // Arrange
        when(aufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.aktualisiereTeilaufgabenReihenfolge(999L, List.of(1L, 2L));
        });
        
        assertTrue(exception.getMessage().contains("Aufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(aufgabeRepository).findById(999L);
        verify(teilaufgabeRepository, never()).findById(any());
    }
    
    @Test
    void aktualisiereTeilaufgabenReihenfolge_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe));
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.aktualisiereTeilaufgabenReihenfolge(1L, List.of(1L, 999L));
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findById(999L);
    }
    
    @Test
    void aktualisiereTeilaufgabenReihenfolge_ShouldThrowException_WhenTeilaufgabeNotBelongsToAufgabe() {
        // Arrange
        Aufgabe andereAufgabe = new Aufgabe();
        andereAufgabe.setId(2L);
        
        Teilaufgabe teilaufgabeVonAndererAufgabe = new Teilaufgabe();
        teilaufgabeVonAndererAufgabe.setId(3L);
        teilaufgabeVonAndererAufgabe.setAufgabe(andereAufgabe);
        
        when(aufgabeRepository.findById(1L)).thenReturn(Optional.of(aufgabe));
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeRepository.findById(3L)).thenReturn(Optional.of(teilaufgabeVonAndererAufgabe));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.aktualisiereTeilaufgabenReihenfolge(1L, List.of(1L, 3L));
        });
        
        assertTrue(exception.getMessage().contains("gehört nicht zur Aufgabe mit ID 1"));
        
        // Verify
        verify(aufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).findById(3L);
    }
    
    @Test
    void aktualisiereMusterloesung_ShouldUpdateMusterloesung_WhenTeilaufgabeExists() {
        // Arrange
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("feld1", "antwort1");
        musterloesungFelder.put("feld2", "antwort2");
        
        String bewertungshinweise = "Bewertungshinweise für Musterlösung";
        
        TeilaufgabeDto expectedDto = new TeilaufgabeDto();
        expectedDto.setId(1L);
        expectedDto.setMusterloesungFelder(musterloesungFelder);
        expectedDto.setMusterloesungBewertungshinweise(bewertungshinweise);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(teilaufgabe1);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(expectedDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.aktualisiereMusterloesung(1L, musterloesungFelder, bewertungshinweise);
        
        // Assert
        assertNotNull(result);
        assertEquals(musterloesungFelder, result.getMusterloesungFelder());
        assertEquals(bewertungshinweise, result.getMusterloesungBewertungshinweise());
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).save(teilaufgabe1);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void aktualisiereMusterloesung_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("feld1", "antwort1");
        
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.aktualisiereMusterloesung(999L, musterloesungFelder, "Hinweise");
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).findById(999L);
        verify(teilaufgabeRepository, never()).save(any());
    }
    
    @Test
    void getMusterloesungFelder_ShouldReturnFields_WhenTeilaufgabeExists() {
        // Arrange
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("feld1", "antwort1");
        musterloesungFelder.put("feld2", "antwort2");
        
        teilaufgabe1.setMusterloesungFelder(musterloesungFelder);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        
        // Act
        Map<String, String> result = teilaufgabeService.getMusterloesungFelder(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("antwort1", result.get("feld1"));
        assertEquals("antwort2", result.get("feld2"));
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
    }
    
    @Test
    void getMusterloesungFelder_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.getMusterloesungFelder(999L);
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).findById(999L);
    }
    
    @Test
    void getMusterloesungBewertungshinweise_ShouldReturnHinweise_WhenTeilaufgabeExists() {
        // Arrange
        String bewertungshinweise = "Bewertungshinweise für Musterlösung";
        teilaufgabe1.setMusterloesungBewertungshinweise(bewertungshinweise);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        
        // Act
        String result = teilaufgabeService.getMusterloesungBewertungshinweise(1L);
        
        // Assert
        assertEquals(bewertungshinweise, result);
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
    }
    
    @Test
    void getMusterloesungBewertungshinweise_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.getMusterloesungBewertungshinweise(999L);
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).findById(999L);
    }
    
    @Test
    void fuegeMusterloesungFeldHinzu_ShouldAddNewField_WhenTeilaufgabeExists() {
        // Arrange
        Map<String, String> initialFelder = new HashMap<>();
        initialFelder.put("feld1", "antwort1");
        teilaufgabe1.setMusterloesungFelder(initialFelder);
        
        Map<String, String> expectedFelder = new HashMap<>(initialFelder);
        expectedFelder.put("feld2", "antwort2");
        
        TeilaufgabeDto expectedDto = new TeilaufgabeDto();
        expectedDto.setId(1L);
        expectedDto.setMusterloesungFelder(expectedFelder);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(teilaufgabe1);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(expectedDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.fuegeMusterloesungFeldHinzu(1L, "feld2", "antwort2");
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getMusterloesungFelder().size());
        assertEquals("antwort2", result.getMusterloesungFelder().get("feld2"));
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).save(teilaufgabe1);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void fuegeMusterloesungFeldHinzu_ShouldUpdateExistingField_WhenFieldExists() {
        // Arrange
        Map<String, String> initialFelder = new HashMap<>();
        initialFelder.put("feld1", "alteAntwort");
        teilaufgabe1.setMusterloesungFelder(initialFelder);
        
        Map<String, String> expectedFelder = new HashMap<>();
        expectedFelder.put("feld1", "neueAntwort");
        
        TeilaufgabeDto expectedDto = new TeilaufgabeDto();
        expectedDto.setId(1L);
        expectedDto.setMusterloesungFelder(expectedFelder);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(teilaufgabe1);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(expectedDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.fuegeMusterloesungFeldHinzu(1L, "feld1", "neueAntwort");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getMusterloesungFelder().size());
        assertEquals("neueAntwort", result.getMusterloesungFelder().get("feld1"));
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).save(teilaufgabe1);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void fuegeMusterloesungFeldHinzu_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.fuegeMusterloesungFeldHinzu(999L, "feld", "antwort");
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).findById(999L);
        verify(teilaufgabeRepository, never()).save(any());
    }
    
    @Test
    void entferneMusterloesungFeld_ShouldRemoveField_WhenFieldExists() {
        // Arrange
        Map<String, String> initialFelder = new HashMap<>();
        initialFelder.put("feld1", "antwort1");
        initialFelder.put("feld2", "antwort2");
        teilaufgabe1.setMusterloesungFelder(initialFelder);
        
        Map<String, String> expectedFelder = new HashMap<>();
        expectedFelder.put("feld2", "antwort2");
        
        TeilaufgabeDto expectedDto = new TeilaufgabeDto();
        expectedDto.setId(1L);
        expectedDto.setMusterloesungFelder(expectedFelder);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(teilaufgabe1);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(expectedDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.entferneMusterloesungFeld(1L, "feld1");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getMusterloesungFelder().size());
        assertFalse(result.getMusterloesungFelder().containsKey("feld1"));
        assertEquals("antwort2", result.getMusterloesungFelder().get("feld2"));
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).save(teilaufgabe1);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void entferneMusterloesungFeld_ShouldNotChangeMap_WhenFieldDoesNotExist() {
        // Arrange
        Map<String, String> initialFelder = new HashMap<>();
        initialFelder.put("feld1", "antwort1");
        teilaufgabe1.setMusterloesungFelder(initialFelder);
        
        Map<String, String> expectedFelder = new HashMap<>();
        expectedFelder.put("feld1", "antwort1");
        
        TeilaufgabeDto expectedDto = new TeilaufgabeDto();
        expectedDto.setId(1L);
        expectedDto.setMusterloesungFelder(expectedFelder);
        
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(teilaufgabe1));
        when(teilaufgabeRepository.save(any(Teilaufgabe.class))).thenReturn(teilaufgabe1);
        when(teilaufgabeMapper.toDto(teilaufgabe1)).thenReturn(expectedDto);
        
        // Act
        TeilaufgabeDto result = teilaufgabeService.entferneMusterloesungFeld(1L, "nichtExistierendesFeld");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getMusterloesungFelder().size());
        assertEquals("antwort1", result.getMusterloesungFelder().get("feld1"));
        
        // Verify
        verify(teilaufgabeRepository).findById(1L);
        verify(teilaufgabeRepository).save(teilaufgabe1);
        verify(teilaufgabeMapper).toDto(teilaufgabe1);
    }
    
    @Test
    void entferneMusterloesungFeld_ShouldThrowException_WhenTeilaufgabeDoesNotExist() {
        // Arrange
        when(teilaufgabeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teilaufgabeService.entferneMusterloesungFeld(999L, "feld");
        });
        
        assertTrue(exception.getMessage().contains("Teilaufgabe mit ID 999 existiert nicht"));
        
        // Verify
        verify(teilaufgabeRepository).findById(999L);
        verify(teilaufgabeRepository, never()).save(any());
    }
}
