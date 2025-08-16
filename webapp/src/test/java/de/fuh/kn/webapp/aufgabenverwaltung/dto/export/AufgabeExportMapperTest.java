package de.fuh.kn.webapp.aufgabenverwaltung.dto.export;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
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
 * Integrationstests für AufgabeExportMapper.
 * Testet die Konvertierung zwischen AufgabeDto und AufgabeExportDTO.
 */
@SpringBootTest
class AufgabeExportMapperTest {

    @Autowired
    private AufgabeExportMapper aufgabeExportMapper;

    @Test
    @DisplayName("toExportDto - Einfache Aufgabe mit einer Teilaufgabe")
    void testToExportDto_EinfacheAufgabe() {
        // Arrange
        AufgabeDto aufgabeDto = createEinfacheAufgabeDto();
        
        // Act
        AufgabeExportDTO exportDto = aufgabeExportMapper.toExportDto(aufgabeDto);
        
        // Assert
        assertNotNull(exportDto);
        assertEquals(aufgabeDto.getId(), exportDto.getId());
        assertEquals(aufgabeDto.getTitel(), exportDto.getTitel());
        assertEquals(aufgabeDto.isEinfach(), exportDto.isEinfach());
        assertEquals(aufgabeDto.getAufgabenText(), exportDto.getAufgabenText());
        assertEquals(aufgabeDto.getReihenfolge(), exportDto.getReihenfolge());
        assertEquals(aufgabeDto.getKurseinheitId(), exportDto.getKurseinheitId());
        
        // Teilaufgaben prüfen
        assertNotNull(exportDto.getTeilaufgaben());
        assertEquals(1, exportDto.getTeilaufgaben().size());
        
        TeilaufgabeExportDTO teilaufgabeExport = exportDto.getTeilaufgaben().get(0);
        TeilaufgabeDto teilaufgabeDto = aufgabeDto.getTeilaufgaben().get(0);
        assertEquals(teilaufgabeDto.getId(), teilaufgabeExport.getId());
        assertEquals(teilaufgabeDto.getAufgabenstellungMarkdown(), teilaufgabeExport.getAufgabenstellungMarkdown());
    }

    @Test
    @DisplayName("toExportDto - Komplexe Aufgabe mit mehreren Teilaufgaben")
    void testToExportDto_KomplexeAufgabe() {
        // Arrange
        AufgabeDto aufgabeDto = createKomplexeAufgabeDto();
        
        // Act
        AufgabeExportDTO exportDto = aufgabeExportMapper.toExportDto(aufgabeDto);
        
        // Assert
        assertNotNull(exportDto);
        assertEquals(aufgabeDto.getId(), exportDto.getId());
        assertEquals(aufgabeDto.getTitel(), exportDto.getTitel());
        assertEquals(aufgabeDto.isEinfach(), exportDto.isEinfach());
        assertEquals(aufgabeDto.getAufgabenText(), exportDto.getAufgabenText());
        assertEquals(aufgabeDto.getReihenfolge(), exportDto.getReihenfolge());
        
        // Teilaufgaben prüfen
        assertNotNull(exportDto.getTeilaufgaben());
        assertEquals(2, exportDto.getTeilaufgaben().size());
    }

    @Test
    @DisplayName("fromExportDto - Konvertierung zurück zu AufgabeDto")
    void testFromExportDto() {
        // Arrange
        AufgabeExportDTO exportDto = createAufgabeExportDto();
        
        // Act
        AufgabeDto aufgabeDto = aufgabeExportMapper.fromExportDto(exportDto);
        
        // Assert
        assertNotNull(aufgabeDto);
        assertEquals(exportDto.getId(), aufgabeDto.getId());
        assertEquals(exportDto.getTitel(), aufgabeDto.getTitel());
        assertEquals(exportDto.isEinfach(), aufgabeDto.isEinfach());
        assertEquals(exportDto.getAufgabenText(), aufgabeDto.getAufgabenText());
        assertEquals(exportDto.getReihenfolge(), aufgabeDto.getReihenfolge());
        assertEquals(exportDto.getKurseinheitId(), aufgabeDto.getKurseinheitId());
    }

    @Test
    @DisplayName("toExportDtoList - Liste von AufgabeDto zu AufgabeExportDTO")
    void testToExportDtoList() {
        // Arrange
        List<AufgabeDto> aufgabeDtos = Arrays.asList(
            createEinfacheAufgabeDto(),
            createKomplexeAufgabeDto()
        );
        
        // Act
        List<AufgabeExportDTO> exportDtos = aufgabeExportMapper.toExportDtoList(aufgabeDtos);
        
        // Assert
        assertNotNull(exportDtos);
        assertEquals(2, exportDtos.size());
        assertEquals(aufgabeDtos.get(0).getTitel(), exportDtos.get(0).getTitel());
        assertEquals(aufgabeDtos.get(1).getTitel(), exportDtos.get(1).getTitel());
    }

    @Test
    @DisplayName("fromExportDtoList - Liste von AufgabeExportDTO zu AufgabeDto")
    void testFromExportDtoList() {
        // Arrange
        List<AufgabeExportDTO> exportDtos = Arrays.asList(
            createAufgabeExportDto(),
            createAufgabeExportDto()
        );
        exportDtos.get(1).setId(2L);
        exportDtos.get(1).setTitel("Aufgabe 2");
        
        // Act
        List<AufgabeDto> aufgabeDtos = aufgabeExportMapper.fromExportDtoList(exportDtos);
        
        // Assert
        assertNotNull(aufgabeDtos);
        assertEquals(2, aufgabeDtos.size());
        assertEquals(exportDtos.get(0).getTitel(), aufgabeDtos.get(0).getTitel());
        assertEquals(exportDtos.get(1).getTitel(), aufgabeDtos.get(1).getTitel());
    }

    @Test
    @DisplayName("toExportDto - Null-Eingabe")
    void testToExportDto_NullInput() {
        // Act
        AufgabeExportDTO result = aufgabeExportMapper.toExportDto(null);
        
        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("fromExportDto - Null-Eingabe")
    void testFromExportDto_NullInput() {
        // Act
        AufgabeDto result = aufgabeExportMapper.fromExportDto(null);
        
        // Assert
        assertNull(result);
    }

    // Helper-Methoden

    private AufgabeDto createEinfacheAufgabeDto() {
        AufgabeDto aufgabe = new AufgabeDto();
        aufgabe.setId(1L);
        aufgabe.setTitel("Einfache Aufgabe");
        aufgabe.setEinfach(true);
        aufgabe.setReihenfolge(1);
        aufgabe.setKurseinheitId(10L);
        
        TeilaufgabeDto teilaufgabe = new TeilaufgabeDto();
        teilaufgabe.setId(1L);
        teilaufgabe.setReihenfolge(1);
        teilaufgabe.setAufgabenstellungMarkdown("Berechnen Sie 2 + 2");
        teilaufgabe.setMusterloesungBewertungshinweise("Die Antwort sollte 4 sein");
        
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("antwort", "4");
        teilaufgabe.setMusterloesungFelder(musterloesungFelder);
        teilaufgabe.setAufgabeId(1L);
        
        aufgabe.setTeilaufgaben(Arrays.asList(teilaufgabe));
        
        return aufgabe;
    }

    private AufgabeDto createKomplexeAufgabeDto() {
        AufgabeDto aufgabe = new AufgabeDto();
        aufgabe.setId(2L);
        aufgabe.setTitel("Komplexe Aufgabe");
        aufgabe.setEinfach(false);
        aufgabe.setAufgabenText("Diese Aufgabe besteht aus mehreren Teilen");
        aufgabe.setReihenfolge(2);
        aufgabe.setKurseinheitId(10L);
        
        TeilaufgabeDto teilaufgabe1 = new TeilaufgabeDto();
        teilaufgabe1.setId(2L);
        teilaufgabe1.setReihenfolge(1);
        teilaufgabe1.setAufgabenstellungMarkdown("Teil 1: Definieren Sie...");
        teilaufgabe1.setMusterloesungBewertungshinweise("Definition sollte enthalten...");
        teilaufgabe1.setAufgabeId(2L);
        
        TeilaufgabeDto teilaufgabe2 = new TeilaufgabeDto();
        teilaufgabe2.setId(3L);
        teilaufgabe2.setReihenfolge(2);
        teilaufgabe2.setAufgabenstellungMarkdown("Teil 2: Berechnen Sie...");
        teilaufgabe2.setMusterloesungBewertungshinweise("Rechenweg beachten");
        teilaufgabe2.setAufgabeId(2L);
        
        aufgabe.setTeilaufgaben(Arrays.asList(teilaufgabe1, teilaufgabe2));
        
        return aufgabe;
    }

    private AufgabeExportDTO createAufgabeExportDto() {
        AufgabeExportDTO exportDto = new AufgabeExportDTO();
        exportDto.setId(1L);
        exportDto.setTitel("Export Aufgabe");
        exportDto.setEinfach(true);
        exportDto.setReihenfolge(1);
        exportDto.setKurseinheitId(10L);
        
        TeilaufgabeExportDTO teilaufgabeExport = new TeilaufgabeExportDTO();
        teilaufgabeExport.setId(1L);
        teilaufgabeExport.setReihenfolge(1);
        teilaufgabeExport.setAufgabenstellungMarkdown("Export Aufgabenstellung");
        teilaufgabeExport.setMusterloesungBewertungshinweise("Export Hinweise");
        
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("feld1", "wert1");
        teilaufgabeExport.setMusterloesungFelder(musterloesungFelder);
        // aufgabeId wird bei Export nicht benötigt
        
        exportDto.setTeilaufgaben(Arrays.asList(teilaufgabeExport));
        
        return exportDto;
    }
}