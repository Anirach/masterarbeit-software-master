package de.fuh.kn.webapp.kursverwaltung.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.*;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KursExportServiceTest {

    @Mock
    private KursService kursService;
    
    @Mock
    private KurseinheitService kurseinheitService;
    
    @Mock
    private KursMaterialService kursMaterialService;
    
    @Mock
    private AufgabeService aufgabeService;
    
    @Mock
    private KursExportMapper kursExportMapper;
    
    @Mock
    private AufgabeExportMapper aufgabeExportMapper;
    
    @Mock
    private KursMaterialExportMapper kursMaterialExportMapper;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private ObjectWriter objectWriter;

    @InjectMocks
    private KursExportService kursExportService;

    private KursDTO testKursDTO;
    private KursExportDTO testKursExportDTO;
    private KurseinheitDTO testKurseinheitDTO;

    @BeforeEach
    void setUp() {
        // Setup test data
        testKurseinheitDTO = new KurseinheitDTO();
        testKurseinheitDTO.setId(100L);
        testKurseinheitDTO.setReihenfolge(1);
        testKurseinheitDTO.setName("Kurseinheit 1");

        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Test Kurs");
        testKursDTO.setKurseinheiten(Collections.singletonList(testKurseinheitDTO));

        testKursExportDTO = new KursExportDTO();
        testKursExportDTO.setName("Test Kurs");
        testKursExportDTO.setKurseinheiten(Collections.emptyList());
        testKursExportDTO.setKursMaterialien(Collections.emptyList());
    }

    @Test
    @DisplayName("Exportiere Kurs - Erfolg")
    void exportiereKurs_WennKursExistiert_SollteExportDTOZurueckgeben() {
        // Given
        Long kursId = 1L;
        AufgabeDto aufgabe1 = createAufgabeDto(1L, "Aufgabe 1");
        AufgabeExportDTO aufgabeExport1 = createAufgabeExportDTO("Aufgabe 1");
        
        when(kursService.getKursByIdMitKurseinheiten(kursId)).thenReturn(testKursDTO);
        when(kursExportMapper.toExportDto(testKursDTO)).thenReturn(testKursExportDTO);
        when(aufgabeService.getAufgabenByKurseinheitId(100L)).thenReturn(Collections.singletonList(aufgabe1));
        when(aufgabeExportMapper.toExportDto(aufgabe1)).thenReturn(aufgabeExport1);

        // When
        KursExportDTO result = kursExportService.exportiereKurs(kursId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAufgaben()).hasSize(1);
        assertThat(result.getAufgaben().get(0)).isEqualTo(aufgabeExport1);
        verify(kursService).getKursByIdMitKurseinheiten(kursId);
        verify(kursExportMapper).toExportDto(testKursDTO);
        verify(aufgabeService).getAufgabenByKurseinheitId(100L);
    }

    @Test
    @DisplayName("Exportiere Kurs - Kurs nicht gefunden")
    void exportiereKurs_WennKursNichtExistiert_SollteExceptionWerfen() {
        // Given
        Long kursId = 999L;
        when(kursService.getKursByIdMitKurseinheiten(kursId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> kursExportService.exportiereKurs(kursId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kurs mit ID 999 nicht gefunden.");

        verify(kursService).getKursByIdMitKurseinheiten(kursId);
        verify(kursExportMapper, never()).toExportDto(any());
    }

    @Test
    @DisplayName("Exportiere Kurs mit mehreren Kurseinheiten und Aufgaben")
    void exportiereKurs_MitMehrerenKurseinheiten_SollteAlleAufgabenSammeln() {
        // Given
        Long kursId = 1L;
        KurseinheitDTO kurseinheit2 = new KurseinheitDTO();
        kurseinheit2.setId(200L);
        kurseinheit2.setReihenfolge(2);
        kurseinheit2.setName("Kurseinheit 2");
        
        testKursDTO.setKurseinheiten(Arrays.asList(testKurseinheitDTO, kurseinheit2));
        
        AufgabeDto aufgabe1 = createAufgabeDto(1L, "Aufgabe 1");
        AufgabeDto aufgabe2 = createAufgabeDto(2L, "Aufgabe 2");
        AufgabeDto aufgabe3 = createAufgabeDto(3L, "Aufgabe 3");
        
        AufgabeExportDTO aufgabeExport1 = createAufgabeExportDTO("Aufgabe 1");
        AufgabeExportDTO aufgabeExport2 = createAufgabeExportDTO("Aufgabe 2");
        AufgabeExportDTO aufgabeExport3 = createAufgabeExportDTO("Aufgabe 3");

        when(kursService.getKursByIdMitKurseinheiten(kursId)).thenReturn(testKursDTO);
        when(kursExportMapper.toExportDto(testKursDTO)).thenReturn(testKursExportDTO);
        when(aufgabeService.getAufgabenByKurseinheitId(100L)).thenReturn(Arrays.asList(aufgabe1, aufgabe2));
        when(aufgabeService.getAufgabenByKurseinheitId(200L)).thenReturn(Collections.singletonList(aufgabe3));
        when(aufgabeExportMapper.toExportDto(aufgabe1)).thenReturn(aufgabeExport1);
        when(aufgabeExportMapper.toExportDto(aufgabe2)).thenReturn(aufgabeExport2);
        when(aufgabeExportMapper.toExportDto(aufgabe3)).thenReturn(aufgabeExport3);

        // When
        KursExportDTO result = kursExportService.exportiereKurs(kursId);

        // Then
        assertThat(result.getAufgaben()).hasSize(3);
        verify(aufgabeService).getAufgabenByKurseinheitId(100L);
        verify(aufgabeService).getAufgabenByKurseinheitId(200L);
        verify(aufgabeExportMapper, times(3)).toExportDto(any(AufgabeDto.class));
    }

    @Test
    @DisplayName("Exportiere als JSON - Erfolg")
    void exportiereAlsJson_SollteJsonStringZurueckgeben() throws JsonProcessingException {
        // Given
        String expectedJson = "{\n  \"kursname\": \"Test Kurs\"\n}";
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(testKursExportDTO)).thenReturn(expectedJson);

        // When
        String result = kursExportService.exportiereAlsJson(testKursExportDTO);

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(objectWriter).writeValueAsString(testKursExportDTO);
    }

    @Test
    @DisplayName("Exportiere als JSON - Fehler")
    void exportiereAlsJson_BeiFehler_SollteRuntimeExceptionWerfen() throws JsonProcessingException {
        // Given
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any(KursExportDTO.class)))
                .thenThrow(new JsonProcessingException("JSON Fehler") {});

        // When & Then
        assertThatThrownBy(() -> kursExportService.exportiereAlsJson(testKursExportDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fehler beim Exportieren des Kurses als JSON");
    }

    @Test
    @DisplayName("Exportiere als ZIP - Erfolg ohne Materialien")
    void exportiereAlsZip_OhneMaterialien_SollteZipErstellenMitNurJson() throws JsonProcessingException {
        // Given
        String kursJson = "{\n  \"kursname\": \"Test Kurs\"\n}";
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(testKursExportDTO)).thenReturn(kursJson);

        // When
        byte[] result = kursExportService.exportiereAlsZip(testKursExportDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(objectWriter).writeValueAsString(testKursExportDTO);
    }

    @Test
    @DisplayName("Exportiere als ZIP - Mit Kursmaterialien")
    void exportiereAlsZip_MitKursmaterialien_SollteZipMitMaterialienErstellen() throws JsonProcessingException {
        // Given
        KursMaterialExportDTO material1 = new KursMaterialExportDTO();
        material1.setName("material1.pdf");
        material1.setInhaltBase64(Base64.getEncoder().encodeToString("Test Content".getBytes()));
        
        KurseinheitExportDTO kurseinheitExport = new KurseinheitExportDTO();
        kurseinheitExport.setReihenfolge(1);
        kurseinheitExport.setName("Kurseinheit 1");
        KursMaterialExportDTO material2 = new KursMaterialExportDTO();
        material2.setName("material2.pdf");
        material2.setInhaltBase64(Base64.getEncoder().encodeToString("Test Content 2".getBytes()));
        kurseinheitExport.setKursMaterialien(Collections.singletonList(material2));
        
        testKursExportDTO.setKursMaterialien(Collections.singletonList(material1));
        testKursExportDTO.setKurseinheiten(Collections.singletonList(kurseinheitExport));
        
        String kursJson = "{\n  \"kursname\": \"Test Kurs\"\n}";
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(testKursExportDTO)).thenReturn(kursJson);
        when(kursMaterialExportMapper.base64ToBytes(material1.getInhaltBase64())).thenReturn("Test Content".getBytes());
        when(kursMaterialExportMapper.base64ToBytes(material2.getInhaltBase64())).thenReturn("Test Content 2".getBytes());

        // When
        byte[] result = kursExportService.exportiereAlsZip(testKursExportDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(kursMaterialExportMapper, times(2)).base64ToBytes(anyString());
    }

    @Test
    @DisplayName("Exportiere als ZIP - Fehler beim Erstellen")
    void exportiereAlsZip_BeiFehler_SollteRuntimeExceptionWerfen() throws JsonProcessingException {
        // Given
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("JSON Fehler") {});

        // When & Then
        assertThatThrownBy(() -> kursExportService.exportiereAlsZip(testKursExportDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fehler beim Exportieren des Kurses als ZIP");
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
        return dto;
    }
}