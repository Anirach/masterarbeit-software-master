package de.fuh.kn.webapp.aufgabenverwaltung.service.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AufgabenExportServiceTest {

    @Mock
    private AufgabeService aufgabeService;

    @Mock
    private AufgabeExportMapper aufgabeExportMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectWriter objectWriter;

    @InjectMocks
    private AufgabenExportService aufgabenExportService;

    private AufgabeDto testAufgabeDto;
    private AufgabeExportDTO testAufgabeExportDTO;

    @BeforeEach
    void setUp() {
        // Setup test data
        testAufgabeDto = new AufgabeDto();
        testAufgabeDto.setId(1L);
        testAufgabeDto.setTitel("Test Aufgabe");
        testAufgabeDto.setAufgabenText("Test Beschreibung");
        testAufgabeDto.setKurseinheitId(10L);

        testAufgabeExportDTO = new AufgabeExportDTO();
        testAufgabeExportDTO.setTitel("Test Aufgabe");
        testAufgabeExportDTO.setAufgabenText("Test Beschreibung");
        testAufgabeExportDTO.setTeilaufgaben(Collections.emptyList());
    }

    @Test
    @DisplayName("Exportiere Aufgabe - Erfolg")
    void exportiereAufgabe_WennAufgabeExistiert_SollteExportDTOZurueckgeben() {
        // Given
        Long aufgabeId = 1L;
        when(aufgabeService.getAufgabeById(aufgabeId)).thenReturn(testAufgabeDto);
        when(aufgabeExportMapper.toExportDto(testAufgabeDto)).thenReturn(testAufgabeExportDTO);

        // When
        AufgabeExportDTO result = aufgabenExportService.exportiereAufgabe(aufgabeId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testAufgabeExportDTO);
        verify(aufgabeService).getAufgabeById(aufgabeId);
        verify(aufgabeExportMapper).toExportDto(testAufgabeDto);
    }

    @Test
    @DisplayName("Exportiere Aufgabe - Aufgabe nicht gefunden")
    void exportiereAufgabe_WennAufgabeNichtExistiert_SollteExceptionWerfen() {
        // Given
        Long aufgabeId = 999L;
        when(aufgabeService.getAufgabeById(aufgabeId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> aufgabenExportService.exportiereAufgabe(aufgabeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aufgabe mit ID 999 nicht gefunden.");

        verify(aufgabeService).getAufgabeById(aufgabeId);
        verify(aufgabeExportMapper, never()).toExportDto(any());
    }

    @Test
    @DisplayName("Exportiere Aufgaben von Kurseinheit - Erfolg")
    void exportiereAufgabenVonKurseinheit_WennAufgabenExistieren_SollteListeZurueckgeben() {
        // Given
        Long kurseinheitId = 10L;
        AufgabeDto aufgabe1 = createAufgabeDto(1L, "Aufgabe 1");
        AufgabeDto aufgabe2 = createAufgabeDto(2L, "Aufgabe 2");
        List<AufgabeDto> aufgabenDtos = Arrays.asList(aufgabe1, aufgabe2);

        AufgabeExportDTO export1 = createAufgabeExportDTO("Aufgabe 1");
        AufgabeExportDTO export2 = createAufgabeExportDTO("Aufgabe 2");

        when(aufgabeService.getAufgabenByKurseinheitId(kurseinheitId)).thenReturn(aufgabenDtos);
        when(aufgabeExportMapper.toExportDto(aufgabe1)).thenReturn(export1);
        when(aufgabeExportMapper.toExportDto(aufgabe2)).thenReturn(export2);

        // When
        List<AufgabeExportDTO> result = aufgabenExportService.exportiereAufgabenVonKurseinheit(kurseinheitId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(export1, export2);
        verify(aufgabeService).getAufgabenByKurseinheitId(kurseinheitId);
        verify(aufgabeExportMapper, times(2)).toExportDto(any(AufgabeDto.class));
    }

    @Test
    @DisplayName("Exportiere Aufgaben von Kurseinheit - Keine Aufgaben")
    void exportiereAufgabenVonKurseinheit_WennKeineAufgaben_SollteLeereListeZurueckgeben() {
        // Given
        Long kurseinheitId = 10L;
        when(aufgabeService.getAufgabenByKurseinheitId(kurseinheitId)).thenReturn(Collections.emptyList());

        // When
        List<AufgabeExportDTO> result = aufgabenExportService.exportiereAufgabenVonKurseinheit(kurseinheitId);

        // Then
        assertThat(result).isEmpty();
        verify(aufgabeService).getAufgabenByKurseinheitId(kurseinheitId);
        verify(aufgabeExportMapper, never()).toExportDto(any());
    }

    @Test
    @DisplayName("Exportiere als JSON - Einzelne Aufgabe Erfolg")
    void exportiereAlsJson_MitEinzelnerAufgabe_SollteJsonStringZurueckgeben() throws JsonProcessingException {
        // Given
        String expectedJson = "{\n  \"titel\": \"Test Aufgabe\"\n}";
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(testAufgabeExportDTO)).thenReturn(expectedJson);

        // When
        String result = aufgabenExportService.exportiereAlsJson(testAufgabeExportDTO);

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(objectWriter).writeValueAsString(testAufgabeExportDTO);
    }

    @Test
    @DisplayName("Exportiere als JSON - Einzelne Aufgabe Fehler")
    void exportiereAlsJson_MitEinzelnerAufgabe_BeiFehler_SollteRuntimeExceptionWerfen() throws JsonProcessingException {
        // Given
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any(AufgabeExportDTO.class)))
                .thenThrow(new JsonProcessingException("JSON Fehler") {});

        // When & Then
        assertThatThrownBy(() -> aufgabenExportService.exportiereAlsJson(testAufgabeExportDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fehler beim Exportieren der Aufgabe als JSON");
    }

    @Test
    @DisplayName("Exportiere als JSON - Liste von Aufgaben Erfolg")
    void exportiereAlsJson_MitAufgabenListe_SollteJsonStringZurueckgeben() throws JsonProcessingException {
        // Given
        List<AufgabeExportDTO> aufgabenListe = Arrays.asList(
                createAufgabeExportDTO("Aufgabe 1"),
                createAufgabeExportDTO("Aufgabe 2")
        );
        String expectedJson = "[{\n  \"titel\": \"Aufgabe 1\"\n}, {\n  \"titel\": \"Aufgabe 2\"\n}]";
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(aufgabenListe)).thenReturn(expectedJson);

        // When
        String result = aufgabenExportService.exportiereAlsJson(aufgabenListe);

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(objectWriter).writeValueAsString(aufgabenListe);
    }

    @Test
    @DisplayName("Exportiere als JSON - Liste von Aufgaben Fehler")
    void exportiereAlsJson_MitAufgabenListe_BeiFehler_SollteRuntimeExceptionWerfen() throws JsonProcessingException {
        // Given
        List<AufgabeExportDTO> aufgabenListe = Collections.singletonList(testAufgabeExportDTO);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any(List.class)))
                .thenThrow(new JsonProcessingException("JSON Fehler") {});

        // When & Then
        assertThatThrownBy(() -> aufgabenExportService.exportiereAlsJson(aufgabenListe))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fehler beim Exportieren der Aufgaben als JSON");
    }

    // Helper methods
    private AufgabeDto createAufgabeDto(Long id, String titel) {
        AufgabeDto dto = new AufgabeDto();
        dto.setId(id);
        dto.setTitel(titel);
        dto.setAufgabenText("Beschreibung für " + titel);
        return dto;
    }

    private AufgabeExportDTO createAufgabeExportDTO(String titel) {
        AufgabeExportDTO dto = new AufgabeExportDTO();
        dto.setTitel(titel);
        dto.setAufgabenText("Beschreibung für " + titel);
        dto.setTeilaufgaben(Collections.emptyList());
        return dto;
    }
}