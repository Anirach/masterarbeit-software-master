package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialMapper;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitMapper;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.repository.KursMaterialRepository;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.KurseinheitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für die KurseinheitService-Klasse.
 * Testet die Geschäftslogik für die Verwaltung von Kurseinheiten.
 */
@ExtendWith(MockitoExtension.class)
class KurseinheitServiceTest {

    @Mock
    private KurseinheitRepository kurseinheitRepository;

    @Mock
    private KursRepository kursRepository;

    @Mock
    private KursMaterialRepository kursMaterialRepository;

    @Mock
    private KurseinheitMapper kurseinheitMapper;

    @Mock
    private KursMaterialMapper kursMaterialMapper;

    @InjectMocks
    private KurseinheitService kurseinheitService;

    private Kurs testKurs;
    private Kurseinheit testKurseinheit1;
    private Kurseinheit testKurseinheit2;
    private KurseinheitDTO testKurseinheitDTO1;
    private KurseinheitDTO testKurseinheitDTO2;
    private KursMaterial testKursMaterial1;
    private KursMaterial testKursMaterial2;
    private KursMaterialDTO testKursMaterialDTO1;
    private KursMaterialDTO testKursMaterialDTO2;

    @BeforeEach
    void setUp() {
        // Testdaten anlegen
        testKurs = new Kurs();
        testKurs.setId(1L);
        testKurs.setName("Programmierung");

        testKurseinheit1 = new Kurseinheit();
        testKurseinheit1.setId(1L);
        testKurseinheit1.setName("Einführung");
        testKurseinheit1.setReihenfolge(1);
        testKurseinheit1.setKurs(testKurs);

        testKurseinheit2 = new Kurseinheit();
        testKurseinheit2.setId(2L);
        testKurseinheit2.setName("Fortgeschrittene Konzepte");
        testKurseinheit2.setReihenfolge(2);
        testKurseinheit2.setKurs(testKurs);

        testKurseinheitDTO1 = new KurseinheitDTO();
        testKurseinheitDTO1.setId(1L);
        testKurseinheitDTO1.setName("Einführung");
        testKurseinheitDTO1.setReihenfolge(1);
        testKurseinheitDTO1.setKursId(1L);

        testKurseinheitDTO2 = new KurseinheitDTO();
        testKurseinheitDTO2.setId(2L);
        testKurseinheitDTO2.setName("Fortgeschrittene Konzepte");
        testKurseinheitDTO2.setReihenfolge(2);
        testKurseinheitDTO2.setKursId(1L);

        testKursMaterial1 = new KursMaterial();
        testKursMaterial1.setId(1L);
        testKursMaterial1.setName("einfuehrung.pdf");
        testKursMaterial1.setMimeType("application/pdf");
        testKursMaterial1.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        testKursMaterial1.setKurseinheit(testKurseinheit1);

        testKursMaterial2 = new KursMaterial();
        testKursMaterial2.setId(2L);
        testKursMaterial2.setName("beispiel.jpg");
        testKursMaterial2.setMimeType("image/jpeg");
        testKursMaterial2.setTyp(KursMaterial.KursMaterialTyp.BILD);
        testKursMaterial2.setKurseinheit(testKurseinheit1);

        testKursMaterialDTO1 = new KursMaterialDTO();
        testKursMaterialDTO1.setId(1L);
        testKursMaterialDTO1.setName("einfuehrung.pdf");
        testKursMaterialDTO1.setMimeType("application/pdf");
        testKursMaterialDTO1.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);

        testKursMaterialDTO2 = new KursMaterialDTO();
        testKursMaterialDTO2.setId(2L);
        testKursMaterialDTO2.setName("beispiel.jpg");
        testKursMaterialDTO2.setMimeType("image/jpeg");
        testKursMaterialDTO2.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);
    }

    @Test
    @DisplayName("getKurseinheitenByKursId sollte alle Kurseinheiten eines Kurses zurückgeben")
    void getKurseinheitenByKursId_ShouldReturnAllUnits() {
        // Arrange
        List<Kurseinheit> kurseinheiten = Arrays.asList(testKurseinheit1, testKurseinheit2);
        List<KurseinheitDTO> kurseinheitDTOs = Arrays.asList(testKurseinheitDTO1, testKurseinheitDTO2);

        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kurseinheitRepository.findByKursOrderByReihenfolgeAsc(testKurs)).thenReturn(kurseinheiten);
        when(kurseinheitMapper.toDto(testKurseinheit1)).thenReturn(testKurseinheitDTO1);
        when(kurseinheitMapper.toDto(testKurseinheit2)).thenReturn(testKurseinheitDTO2);

        // Act
        List<KurseinheitDTO> result = kurseinheitService.getKurseinheitenByKursId(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertEquals(kurseinheitDTOs, result);
        verify(kursRepository).findById(1L);
        verify(kurseinheitRepository).findByKursOrderByReihenfolgeAsc(testKurs);
        verify(kurseinheitMapper, times(2)).toDto(any(Kurseinheit.class));
    }

    @Test
    @DisplayName("getKurseinheitenByKursId sollte eine Exception werfen, wenn der Kurs nicht existiert")
    void getKurseinheitenByKursId_ShouldThrowException_WhenCourseNotExists() {
        // Arrange
        when(kursRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kurseinheitService.getKurseinheitenByKursId(999L)
        );

        assertThat(exception.getMessage()).contains("Kurs mit ID 999 existiert nicht");
        verify(kursRepository).findById(999L);
        verify(kurseinheitRepository, never()).findByKursOrderByReihenfolgeAsc(any());
    }

    @Test
    @DisplayName("getKurseinheitById sollte eine Kurseinheit zurückgeben, wenn sie existiert")
    void getKurseinheitById_ShouldReturnUnit_WhenExists() {
        // Arrange
        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(testKurseinheit1));
        when(kurseinheitMapper.toDto(testKurseinheit1)).thenReturn(testKurseinheitDTO1);

        // Act
        KurseinheitDTO result = kurseinheitService.getKurseinheitById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testKurseinheitDTO1, result);
        verify(kurseinheitRepository).findById(1L);
        verify(kurseinheitMapper).toDto(testKurseinheit1);
    }

    @Test
    @DisplayName("getKurseinheitById sollte null zurückgeben, wenn Kurseinheit nicht existiert")
    void getKurseinheitById_ShouldReturnNull_WhenNotExists() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        KurseinheitDTO result = kurseinheitService.getKurseinheitById(999L);

        // Assert
        assertNull(result);
        verify(kurseinheitRepository).findById(999L);
        verify(kurseinheitMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("erstelleKurseinheit sollte eine neue Kurseinheit erstellen")
    void erstelleKurseinheit_ShouldCreateNewUnit() {
        // Arrange
        KurseinheitDTO neueKurseinheitDTO = new KurseinheitDTO();
        neueKurseinheitDTO.setName("Neue Kurseinheit");
        neueKurseinheitDTO.setReihenfolge(3);
        neueKurseinheitDTO.setKursId(1L);

        Kurseinheit neueKurseinheit = new Kurseinheit();
        neueKurseinheit.setId(3L);
        neueKurseinheit.setName("Neue Kurseinheit");
        neueKurseinheit.setReihenfolge(3);

        KurseinheitDTO erstellteKurseinheitDTO = new KurseinheitDTO();
        erstellteKurseinheitDTO.setId(3L);
        erstellteKurseinheitDTO.setName("Neue Kurseinheit");
        erstellteKurseinheitDTO.setReihenfolge(3);
        erstellteKurseinheitDTO.setKursId(1L);

        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kurseinheitMapper.toEntity(neueKurseinheitDTO)).thenReturn(neueKurseinheit);
        when(kurseinheitRepository.save(any(Kurseinheit.class))).thenReturn(neueKurseinheit);
        when(kurseinheitMapper.toDto(neueKurseinheit)).thenReturn(erstellteKurseinheitDTO);

        // Act
        KurseinheitDTO result = kurseinheitService.erstelleKurseinheit(neueKurseinheitDTO);

        // Assert
        assertNotNull(result);
        assertEquals(erstellteKurseinheitDTO, result);
        verify(kursRepository).findById(1L);
        verify(kurseinheitMapper).toEntity(neueKurseinheitDTO);
        verify(kurseinheitRepository).save(any(Kurseinheit.class));
        verify(kurseinheitMapper).toDto(neueKurseinheit);
    }

    @Test
    @DisplayName("erstelleKurseinheit sollte eine Exception werfen, wenn der Kurs nicht existiert")
    void erstelleKurseinheit_ShouldThrowException_WhenCourseNotExists() {
        // Arrange
        KurseinheitDTO neueKurseinheitDTO = new KurseinheitDTO();
        neueKurseinheitDTO.setName("Neue Kurseinheit");
        neueKurseinheitDTO.setReihenfolge(3);
        neueKurseinheitDTO.setKursId(999L);

        when(kursRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kurseinheitService.erstelleKurseinheit(neueKurseinheitDTO)
        );

        assertThat(exception.getMessage()).contains("Kurs mit ID 999 existiert nicht");
        verify(kursRepository).findById(999L);
        verify(kurseinheitRepository, never()).save(any());
    }

    @Test
    @DisplayName("aktualisiereKurseinheit sollte eine Kurseinheit aktualisieren")
    void aktualisiereKurseinheit_ShouldUpdateUnit() {
        // Arrange
        KurseinheitDTO updateDTO = new KurseinheitDTO();
        updateDTO.setId(1L);
        updateDTO.setName("Aktualisierte Einführung");
        updateDTO.setReihenfolge(3);
        updateDTO.setKursId(1L);

        Kurseinheit existingKurseinheit = new Kurseinheit();
        existingKurseinheit.setId(1L);
        existingKurseinheit.setName("Einführung");
        existingKurseinheit.setReihenfolge(1);
        existingKurseinheit.setKurs(testKurs);

        Kurseinheit updatedKurseinheit = new Kurseinheit();
        updatedKurseinheit.setId(1L);
        updatedKurseinheit.setName("Aktualisierte Einführung");
        updatedKurseinheit.setReihenfolge(3);
        updatedKurseinheit.setKurs(testKurs);

        KurseinheitDTO updatedDTO = new KurseinheitDTO();
        updatedDTO.setId(1L);
        updatedDTO.setName("Aktualisierte Einführung");
        updatedDTO.setReihenfolge(3);
        updatedDTO.setKursId(1L);

        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(existingKurseinheit));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kurseinheitRepository.save(any(Kurseinheit.class))).thenReturn(updatedKurseinheit);
        when(kurseinheitMapper.toDto(updatedKurseinheit)).thenReturn(updatedDTO);

        // Act
        KurseinheitDTO result = kurseinheitService.aktualisiereKurseinheit(updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(updatedDTO, result);
        assertEquals("Aktualisierte Einführung", result.getName());
        assertEquals(3, result.getReihenfolge());
        verify(kurseinheitRepository).findById(1L);
        verify(kursRepository).findById(1L);
        verify(kurseinheitRepository).save(any(Kurseinheit.class));
        verify(kurseinheitMapper).toDto(updatedKurseinheit);
    }

    @Test
    @DisplayName("aktualisiereKurseinheit sollte Exception werfen, wenn Kurseinheit nicht existiert")
    void aktualisiereKurseinheit_ShouldThrowException_WhenUnitNotExists() {
        // Arrange
        KurseinheitDTO updateDTO = new KurseinheitDTO();
        updateDTO.setId(999L);
        updateDTO.setName("Nicht existierende Kurseinheit");
        updateDTO.setKursId(1L);

        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kurseinheitService.aktualisiereKurseinheit(updateDTO)
        );

        assertThat(exception.getMessage()).contains("Kurseinheit mit ID 999 existiert nicht");
        verify(kurseinheitRepository).findById(999L);
        verify(kursRepository, never()).findById(anyLong());
        verify(kurseinheitRepository, never()).save(any());
    }

    @Test
    @DisplayName("aktualisiereKurseinheit sollte Exception werfen, wenn Kurs nicht existiert")
    void aktualisiereKurseinheit_ShouldThrowException_WhenCourseNotExists() {
        // Arrange
        KurseinheitDTO updateDTO = new KurseinheitDTO();
        updateDTO.setId(1L);
        updateDTO.setName("Aktualisierte Einführung");
        updateDTO.setKursId(999L);

        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(testKurseinheit1));
        when(kursRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kurseinheitService.aktualisiereKurseinheit(updateDTO)
        );

        assertThat(exception.getMessage()).contains("Kurs mit ID 999 existiert nicht");
        verify(kurseinheitRepository).findById(1L);
        verify(kursRepository).findById(999L);
        verify(kurseinheitRepository, never()).save(any());
    }

    @Test
    @DisplayName("loescheKurseinheit sollte eine Kurseinheit löschen")
    void loescheKurseinheit_ShouldDeleteUnit() {
        // Arrange
        when(kurseinheitRepository.existsById(1L)).thenReturn(true);
        doNothing().when(kurseinheitRepository).deleteById(1L);

        // Act
        kurseinheitService.loescheKurseinheit(1L);

        // Assert
        verify(kurseinheitRepository).existsById(1L);
        verify(kurseinheitRepository).deleteById(1L);
    }

    @Test
    @DisplayName("loescheKurseinheit sollte Exception werfen, wenn Kurseinheit nicht existiert")
    void loescheKurseinheit_ShouldThrowException_WhenUnitNotExists() {
        // Arrange
        when(kurseinheitRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kurseinheitService.loescheKurseinheit(999L)
        );

        assertThat(exception.getMessage()).contains("Kurseinheit mit ID 999 existiert nicht");
        verify(kurseinheitRepository).existsById(999L);
        verify(kurseinheitRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("getKursNameByKurseinheitId sollte den Kursnamen zurückgeben")
    void getKursNameByKurseinheitId_ShouldReturnCourseName() {
        // Arrange
        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(testKurseinheit1));

        // Act
        String result = kurseinheitService.getKursNameByKurseinheitId(1L);

        // Assert
        assertEquals("Programmierung", result);
        verify(kurseinheitRepository).findById(1L);
    }

    @Test
    @DisplayName("getKursNameByKurseinheitId sollte Exception werfen, wenn Kurseinheit nicht existiert")
    void getKursNameByKurseinheitId_ShouldThrowException_WhenUnitNotExists() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kurseinheitService.getKursNameByKurseinheitId(999L)
        );

        assertThat(exception.getMessage()).contains("Kurseinheit mit ID 999 existiert nicht");
        verify(kurseinheitRepository).findById(999L);
    }

    @Test
    @DisplayName("getKursMaterialienByKurseinheitId sollte alle Materialien einer Kurseinheit zurückgeben")
    void getKursMaterialienByKurseinheitId_ShouldReturnAllMaterials() {
        // Arrange
        List<KursMaterial> materialien = Arrays.asList(testKursMaterial1, testKursMaterial2);
        List<KursMaterialDTO> materialienDTOs = Arrays.asList(testKursMaterialDTO1, testKursMaterialDTO2);

        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(testKurseinheit1));
        when(kursMaterialRepository.findByKurseinheit(testKurseinheit1)).thenReturn(materialien);
        when(kursMaterialMapper.toDto(testKursMaterial1)).thenReturn(testKursMaterialDTO1);
        when(kursMaterialMapper.toDto(testKursMaterial2)).thenReturn(testKursMaterialDTO2);

        // Act
        List<KursMaterialDTO> result = kurseinheitService.getKursMaterialienByKurseinheitId(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertEquals(materialienDTOs, result);
        verify(kurseinheitRepository).findById(1L);
        verify(kursMaterialRepository).findByKurseinheit(testKurseinheit1);
        verify(kursMaterialMapper, times(2)).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("getKursMaterialienByKurseinheitId sollte leere Liste zurückgeben, wenn keine Materialien existieren")
    void getKursMaterialienByKurseinheitId_ShouldReturnEmptyList_WhenNoMaterialsExist() {
        // Arrange
        when(kurseinheitRepository.findById(1L)).thenReturn(Optional.of(testKurseinheit1));
        when(kursMaterialRepository.findByKurseinheit(testKurseinheit1)).thenReturn(Collections.emptyList());

        // Act
        List<KursMaterialDTO> result = kurseinheitService.getKursMaterialienByKurseinheitId(1L);

        // Assert
        assertTrue(result.isEmpty());
        verify(kurseinheitRepository).findById(1L);
        verify(kursMaterialRepository).findByKurseinheit(testKurseinheit1);
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("getKursMaterialienByKurseinheitId sollte Exception werfen, wenn Kurseinheit nicht existiert")
    void getKursMaterialienByKurseinheitId_ShouldThrowException_WhenUnitNotExists() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kurseinheitService.getKursMaterialienByKurseinheitId(999L)
        );

        assertThat(exception.getMessage()).contains("Kurseinheit mit ID 999 existiert nicht");
        verify(kurseinheitRepository).findById(999L);
        verify(kursMaterialRepository, never()).findByKurseinheit(any());
    }
}
