package de.fuh.kn.webapp.aufgabenverwaltung.dto.export;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrationstests für TeilaufgabeExportMapper.
 * Testet die Konvertierung zwischen TeilaufgabeDto und TeilaufgabeExportDTO.
 */
@SpringBootTest
class TeilaufgabeExportMapperTest {

    @Autowired
    private TeilaufgabeExportMapper teilaufgabeExportMapper;

    @Test
    @DisplayName("toExportDto - Vollständige Teilaufgabe")
    void testToExportDto_VollstaendigeTeilaufgabe() {
        // Arrange
        TeilaufgabeDto teilaufgabeDto = createVollstaendigeTeilaufgabeDto();
        
        // Act
        TeilaufgabeExportDTO exportDto = teilaufgabeExportMapper.toExportDto(teilaufgabeDto);
        
        // Assert
        assertNotNull(exportDto);
        assertEquals(teilaufgabeDto.getId(), exportDto.getId());
        assertEquals(teilaufgabeDto.getReihenfolge(), exportDto.getReihenfolge());
        assertEquals(teilaufgabeDto.getAufgabenstellungMarkdown(), exportDto.getAufgabenstellungMarkdown());
        assertEquals(teilaufgabeDto.getMusterloesungBewertungshinweise(), exportDto.getMusterloesungBewertungshinweise());
        // aufgabeId wird bei Export nicht übertragen
        
        // Musterlösungsfelder prüfen
        assertNotNull(exportDto.getMusterloesungFelder());
        assertEquals(teilaufgabeDto.getMusterloesungFelder().size(), exportDto.getMusterloesungFelder().size());
        assertEquals(teilaufgabeDto.getMusterloesungFelder().get("antwort"), exportDto.getMusterloesungFelder().get("antwort"));
        assertEquals(teilaufgabeDto.getMusterloesungFelder().get("erklaerung"), exportDto.getMusterloesungFelder().get("erklaerung"));
    }

    @Test
    @DisplayName("toExportDto - Minimale Teilaufgabe")
    void testToExportDto_MinimaleTeilaufgabe() {
        // Arrange
        TeilaufgabeDto teilaufgabeDto = new TeilaufgabeDto();
        teilaufgabeDto.setId(1L);
        teilaufgabeDto.setReihenfolge(1);
        teilaufgabeDto.setAufgabenstellungMarkdown("Minimale Aufgabe");
        
        // Act
        TeilaufgabeExportDTO exportDto = teilaufgabeExportMapper.toExportDto(teilaufgabeDto);
        
        // Assert
        assertNotNull(exportDto);
        assertEquals(teilaufgabeDto.getId(), exportDto.getId());
        assertEquals(teilaufgabeDto.getReihenfolge(), exportDto.getReihenfolge());
        assertEquals(teilaufgabeDto.getAufgabenstellungMarkdown(), exportDto.getAufgabenstellungMarkdown());
        assertNull(exportDto.getMusterloesungBewertungshinweise());
        // aufgabeId wird bei Export nicht übertragen
        assertNotNull(exportDto.getMusterloesungFelder());
        assertTrue(exportDto.getMusterloesungFelder().isEmpty());
    }

    @Test
    @DisplayName("fromExportDto - Konvertierung zurück zu TeilaufgabeDto")
    void testFromExportDto() {
        // Arrange
        TeilaufgabeExportDTO exportDto = createTeilaufgabeExportDto();
        
        // Act
        TeilaufgabeDto teilaufgabeDto = teilaufgabeExportMapper.fromExportDto(exportDto);
        
        // Assert
        assertNotNull(teilaufgabeDto);
        assertEquals(exportDto.getId(), teilaufgabeDto.getId());
        assertEquals(exportDto.getReihenfolge(), teilaufgabeDto.getReihenfolge());
        assertEquals(exportDto.getAufgabenstellungMarkdown(), teilaufgabeDto.getAufgabenstellungMarkdown());
        assertEquals(exportDto.getMusterloesungBewertungshinweise(), teilaufgabeDto.getMusterloesungBewertungshinweise());
        // aufgabeId wird bei Import wiederhergestellt
        
        // Musterlösungsfelder prüfen
        assertNotNull(teilaufgabeDto.getMusterloesungFelder());
        assertEquals(exportDto.getMusterloesungFelder().size(), teilaufgabeDto.getMusterloesungFelder().size());
    }

    @Test
    @DisplayName("toExportDtoList - Liste von TeilaufgabeDto zu TeilaufgabeExportDTO")
    void testToExportDtoList() {
        // Arrange
        List<TeilaufgabeDto> teilaufgabeDtos = Arrays.asList(
            createVollstaendigeTeilaufgabeDto(),
            createVollstaendigeTeilaufgabeDto()
        );
        teilaufgabeDtos.get(1).setId(2L);
        teilaufgabeDtos.get(1).setReihenfolge(2);
        
        // Act
        List<TeilaufgabeExportDTO> exportDtos = teilaufgabeExportMapper.toExportDtoList(teilaufgabeDtos);
        
        // Assert
        assertNotNull(exportDtos);
        assertEquals(2, exportDtos.size());
        assertEquals(1L, exportDtos.get(0).getId());
        assertEquals(2L, exportDtos.get(1).getId());
        assertEquals(1, exportDtos.get(0).getReihenfolge());
        assertEquals(2, exportDtos.get(1).getReihenfolge());
    }

    @Test
    @DisplayName("fromExportDtoList - Liste von TeilaufgabeExportDTO zu TeilaufgabeDto")
    void testFromExportDtoList() {
        // Arrange
        List<TeilaufgabeExportDTO> exportDtos = Arrays.asList(
            createTeilaufgabeExportDto(),
            createTeilaufgabeExportDto()
        );
        exportDtos.get(1).setId(2L);
        exportDtos.get(1).setReihenfolge(2);
        
        // Act
        List<TeilaufgabeDto> teilaufgabeDtos = teilaufgabeExportMapper.fromExportDtoList(exportDtos);
        
        // Assert
        assertNotNull(teilaufgabeDtos);
        assertEquals(2, teilaufgabeDtos.size());
        assertEquals(1L, teilaufgabeDtos.get(0).getId());
        assertEquals(2L, teilaufgabeDtos.get(1).getId());
    }

    @Test
    @DisplayName("toExportDto - Null-Eingabe")
    void testToExportDto_NullInput() {
        // Act
        TeilaufgabeExportDTO result = teilaufgabeExportMapper.toExportDto(null);
        
        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("fromExportDto - Null-Eingabe")
    void testFromExportDto_NullInput() {
        // Act
        TeilaufgabeDto result = teilaufgabeExportMapper.fromExportDto(null);
        
        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("toExportDto - Leere Musterlösungsfelder")
    void testToExportDto_LeereMusterloesungFelder() {
        // Arrange
        TeilaufgabeDto teilaufgabeDto = new TeilaufgabeDto();
        teilaufgabeDto.setId(1L);
        teilaufgabeDto.setReihenfolge(1);
        teilaufgabeDto.setAufgabenstellungMarkdown("Aufgabe ohne Felder");
        teilaufgabeDto.setMusterloesungFelder(new HashMap<>()); // Explizit leere Map
        
        // Act
        TeilaufgabeExportDTO exportDto = teilaufgabeExportMapper.toExportDto(teilaufgabeDto);
        
        // Assert
        assertNotNull(exportDto);
        assertNotNull(exportDto.getMusterloesungFelder());
        assertTrue(exportDto.getMusterloesungFelder().isEmpty());
    }

    // Helper-Methoden

    private TeilaufgabeDto createVollstaendigeTeilaufgabeDto() {
        TeilaufgabeDto teilaufgabe = new TeilaufgabeDto();
        teilaufgabe.setId(1L);
        teilaufgabe.setReihenfolge(1);
        teilaufgabe.setAufgabenstellungMarkdown("## Aufgabe 1\nBeschreiben Sie den Algorithmus");
        teilaufgabe.setMusterloesungBewertungshinweise("Achten Sie auf vollständige Erklärung");
        teilaufgabe.setAufgabeId(10L);
        
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("antwort", "Der Algorithmus funktioniert wie folgt...");
        musterloesungFelder.put("erklaerung", "Die Zeitkomplexität beträgt O(n)");
        teilaufgabe.setMusterloesungFelder(musterloesungFelder);
        
        return teilaufgabe;
    }

    private TeilaufgabeExportDTO createTeilaufgabeExportDto() {
        TeilaufgabeExportDTO exportDto = new TeilaufgabeExportDTO();
        exportDto.setId(1L);
        exportDto.setReihenfolge(1);
        exportDto.setAufgabenstellungMarkdown("# Export Aufgabe\nLösen Sie folgendes Problem");
        exportDto.setMusterloesungBewertungshinweise("Bewertungshinweise für Export");
        // aufgabeId existiert in Export DTO nicht
        
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("loesung", "42");
        musterloesungFelder.put("begruendung", "Weil es die Antwort auf alles ist");
        exportDto.setMusterloesungFelder(musterloesungFelder);
        
        return exportDto;
    }
}