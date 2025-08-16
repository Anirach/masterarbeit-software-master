package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMapper;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für die KursService-Klasse.
 * Testet die Geschäftslogik des Services unabhängig von anderen Komponenten.
 */
@ExtendWith(MockitoExtension.class)
class KursServiceTest {

    @Mock
    private KursRepository kursRepository;

    @Mock
    private KursMapper kursMapper;

    @InjectMocks
    private KursService kursService;

    private Kurs testKurs1;
    private Kurs testKurs2;
    private KursDTO testKursDTO1;
    private KursDTO testKursDTO2;

    @BeforeEach
    void setUp() {
        // Testdaten anlegen
        testKurs1 = new Kurs();
        testKurs1.setId(1L);
        testKurs1.setName("Informatik Grundlagen");

        testKurs2 = new Kurs();
        testKurs2.setId(2L);
        testKurs2.setName("Kommunikationsnetze");

        testKursDTO1 = new KursDTO();
        testKursDTO1.setId(1L);
        testKursDTO1.setName("Informatik Grundlagen");

        testKursDTO2 = new KursDTO();
        testKursDTO2.setId(2L);
        testKursDTO2.setName("Kommunikationsnetze");
    }

    @Test
    @DisplayName("getAlleKurse sollte alle Kurse zurückgeben")
    void getAlleKurse_ShouldReturnAllCourses() {
        // Arrange
        List<Kurs> kurse = Arrays.asList(testKurs1, testKurs2);
        List<KursDTO> kursDTOs = Arrays.asList(testKursDTO1, testKursDTO2);
        
        when(kursRepository.findAll()).thenReturn(kurse);
        when(kursMapper.toDto(testKurs1)).thenReturn(testKursDTO1);
        when(kursMapper.toDto(testKurs2)).thenReturn(testKursDTO2);

        // Act
        List<KursDTO> result = kursService.getAlleKurse();

        // Assert
        assertThat(result).hasSize(2);
        assertEquals(kursDTOs, result);
        verify(kursRepository, times(1)).findAll();
        verify(kursMapper, times(2)).toDto(any(Kurs.class));
    }

    @Test
    @DisplayName("getKursById sollte einen Kurs zurückgeben, wenn er existiert")
    void getKursById_ShouldReturnCourse_WhenExists() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs1));
        when(kursMapper.toDto(testKurs1)).thenReturn(testKursDTO1);

        // Act
        KursDTO result = kursService.getKursById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testKursDTO1, result);
        verify(kursRepository, times(1)).findById(1L);
        verify(kursMapper, times(1)).toDto(testKurs1);
    }

    @Test
    @DisplayName("getKursById sollte null zurückgeben, wenn Kurs nicht existiert")
    void getKursById_ShouldReturnNull_WhenNotExists() {
        // Arrange
        when(kursRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        KursDTO result = kursService.getKursById(999L);

        // Assert
        assertNull(result);
        verify(kursRepository, times(1)).findById(999L);
        verify(kursMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("getKursByIdMitKurseinheiten sollte einen Kurs mit Kurseinheiten zurückgeben")
    void getKursByIdMitKurseinheiten_ShouldReturnCourseWithUnits() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs1));
        when(kursMapper.toDto(testKurs1)).thenReturn(testKursDTO1);

        // Act
        KursDTO result = kursService.getKursByIdMitKurseinheiten(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testKursDTO1, result);
        verify(kursRepository, times(1)).findById(1L);
        verify(kursMapper, times(1)).toDto(testKurs1);
    }

    @Test
    @DisplayName("erstelleKurs sollte einen neuen Kurs erstellen")
    void erstelleKurs_ShouldCreateNewCourse() {
        // Arrange
        when(kursMapper.toEntity(testKursDTO1)).thenReturn(testKurs1);
        when(kursRepository.save(testKurs1)).thenReturn(testKurs1);
        when(kursMapper.toDto(testKurs1)).thenReturn(testKursDTO1);

        // Act
        KursDTO result = kursService.erstelleKurs(testKursDTO1);

        // Assert
        assertNotNull(result);
        assertEquals(testKursDTO1, result);
        verify(kursMapper, times(1)).toEntity(testKursDTO1);
        verify(kursRepository, times(1)).save(testKurs1);
        verify(kursMapper, times(1)).toDto(testKurs1);
    }

    @Test
    @DisplayName("aktualisiereKurs sollte einen vorhandenen Kurs aktualisieren")
    void aktualisiereKurs_ShouldUpdateExistingCourse() {
        // Arrange
        KursDTO updateDTO = new KursDTO();
        updateDTO.setId(1L);
        updateDTO.setName("Aktualisierter Kursname");
        
        Kurs existingKurs = new Kurs();
        existingKurs.setId(1L);
        existingKurs.setName("Informatik Grundlagen");
        
        Kurs updatedKurs = new Kurs();
        updatedKurs.setId(1L);
        updatedKurs.setName("Aktualisierter Kursname");
        
        when(kursRepository.existsById(1L)).thenReturn(true);
        when(kursRepository.findById(1L)).thenReturn(Optional.of(existingKurs));
        when(kursRepository.save(any(Kurs.class))).thenReturn(updatedKurs);
        when(kursMapper.toDto(updatedKurs)).thenReturn(updateDTO);

        // Act
        KursDTO result = kursService.aktualisiereKurs(updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Aktualisierter Kursname", result.getName());
        verify(kursRepository, times(1)).existsById(1L);
        verify(kursRepository, times(1)).findById(1L);
        verify(kursRepository, times(1)).save(any(Kurs.class));
        verify(kursMapper, times(1)).toDto(updatedKurs);
    }

    @Test
    @DisplayName("aktualisiereKurs sollte eine Exception werfen, wenn der Kurs nicht existiert")
    void aktualisiereKurs_ShouldThrowException_WhenCourseNotExists() {
        // Arrange
        KursDTO updateDTO = new KursDTO();
        updateDTO.setId(999L);
        updateDTO.setName("Nicht existierender Kurs");
        
        when(kursRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kursService.aktualisiereKurs(updateDTO)
        );
        
        assertThat(exception.getMessage()).contains("existiert nicht");
        verify(kursRepository, times(1)).existsById(999L);
        verify(kursRepository, never()).save(any());
    }

    @Test
    @DisplayName("loescheKurs sollte einen Kurs löschen, wenn er existiert")
    void loescheKurs_ShouldDeleteCourse_WhenExists() {
        // Arrange
        when(kursRepository.existsById(1L)).thenReturn(true);
        doNothing().when(kursRepository).deleteById(1L);

        // Act
        kursService.loescheKurs(1L);

        // Assert
        verify(kursRepository, times(1)).existsById(1L);
        verify(kursRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("loescheKurs sollte eine Exception werfen, wenn der Kurs nicht existiert")
    void loescheKurs_ShouldThrowException_WhenCourseNotExists() {
        // Arrange
        when(kursRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kursService.loescheKurs(999L)
        );
        
        assertThat(exception.getMessage()).contains("existiert nicht");
        verify(kursRepository, times(1)).existsById(999L);
        verify(kursRepository, never()).deleteById(anyLong());
    }
}
