package de.fuh.kn.webapp.aufgabenverwaltung.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service für den Export von Aufgaben.
 * Diese Klasse bietet Funktionalität zum Exportieren von Aufgaben in verschiedene Formate.
 */
@Service
public class AufgabenExportService {

    private final AufgabeService aufgabeService;
    private final AufgabeExportMapper aufgabeExportMapper;
    private final ObjectMapper objectMapper;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services und Mapper.
     *
     * @param aufgabeService Service für die Verwaltung von Aufgaben
     * @param aufgabeExportMapper Mapper für die Konvertierung zwischen AufgabeDto und AufgabeExportDTO
     * @param objectMapper Jackson ObjectMapper für die JSON-Serialisierung
     */
    @Autowired
    public AufgabenExportService(AufgabeService aufgabeService,
                                AufgabeExportMapper aufgabeExportMapper,
                                ObjectMapper objectMapper) {
        this.aufgabeService = aufgabeService;
        this.aufgabeExportMapper = aufgabeExportMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Exportiert eine Aufgabe anhand ihrer ID in ein Export-DTO.
     *
     * @param aufgabeId ID der zu exportierenden Aufgabe
     * @return Das Export-DTO der Aufgabe
     * @throws IllegalArgumentException Wenn keine Aufgabe mit der angegebenen ID existiert
     */
    @Transactional(readOnly = true)
    public AufgabeExportDTO exportiereAufgabe(Long aufgabeId) {
        AufgabeDto aufgabeDto = aufgabeService.getAufgabeById(aufgabeId);
        if (aufgabeDto == null) {
            throw new IllegalArgumentException("Aufgabe mit ID " + aufgabeId + " nicht gefunden.");
        }
        return aufgabeExportMapper.toExportDto(aufgabeDto);
    }

    /**
     * Exportiert alle Aufgaben einer Kurseinheit in Export-DTOs.
     *
     * @param kurseinheitId ID der Kurseinheit, deren Aufgaben exportiert werden sollen
     * @return Liste der Export-DTOs der Aufgaben
     * @throws IllegalArgumentException Wenn keine Kurseinheit mit der angegebenen ID existiert
     */
    @Transactional(readOnly = true)
    public List<AufgabeExportDTO> exportiereAufgabenVonKurseinheit(Long kurseinheitId) {
        List<AufgabeDto> aufgabenDtos = aufgabeService.getAufgabenByKurseinheitId(kurseinheitId);
        return aufgabenDtos.stream()
                .map(aufgabeExportMapper::toExportDto)
                .collect(Collectors.toList());
    }

    /**
     * Exportiert eine Aufgabe als JSON-String.
     *
     * @param aufgabeExport Das Export-DTO der Aufgabe
     * @return Die Aufgabe als JSON-String
     * @throws RuntimeException Wenn die JSON-Serialisierung fehlschlägt
     */
    public String exportiereAlsJson(AufgabeExportDTO aufgabeExport) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aufgabeExport);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Exportieren der Aufgabe als JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Exportiert eine Liste von Aufgaben als JSON-String.
     *
     * @param aufgabenExport Die Liste der Export-DTOs der Aufgaben
     * @return Die Aufgaben als JSON-String
     * @throws RuntimeException Wenn die JSON-Serialisierung fehlschlägt
     */
    public String exportiereAlsJson(List<AufgabeExportDTO> aufgabenExport) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aufgabenExport);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Exportieren der Aufgaben als JSON: " + e.getMessage(), e);
        }
    }
}