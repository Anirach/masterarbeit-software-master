package de.fuh.kn.webapp.aufgabenverwaltung.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfImportRequestDto;
import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfImportResponseDto;
import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfPairImportRequestDto;
import de.fuh.kn.webapp.llm.service.LlmAufgabenImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service für den Import von Aufgaben aus PDF-Dateien und JSON-Dateien.
 * Diese Klasse bietet Funktionalität zum Importieren von Aufgaben aus verschiedenen Quellen:
 * - PDF-Dateien (mittels LLM-gestützter Extraktion)
 * - JSON-Dateien (direkter Import aus strukturierten Daten)
 */
@Service
@Slf4j
public class AufgabenImportService {

    private final AufgabeService aufgabeService;
    private final TeilaufgabeService teilaufgabeService;
    private final AufgabeExportMapper aufgabeExportMapper;
    private final ObjectMapper objectMapper;
    private final LlmAufgabenImportService llmAufgabenImportService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services und Mapper.
     *
     * @param aufgabeService Service für die Verwaltung von Aufgaben
     * @param teilaufgabeService Service für die Verwaltung von Teilaufgaben
     * @param aufgabeExportMapper Mapper für die Konvertierung zwischen AufgabeExportDTO und AufgabeDto
     * @param objectMapper Jackson ObjectMapper für die JSON-Deserialisierung
     * @param llmAufgabenImportService Service für die LLM-basierte PDF-Verarbeitung
     */
    @Autowired
    public AufgabenImportService(AufgabeService aufgabeService,
                               TeilaufgabeService teilaufgabeService,
                               AufgabeExportMapper aufgabeExportMapper,
                               ObjectMapper objectMapper,
                               LlmAufgabenImportService llmAufgabenImportService) {
        this.aufgabeService = aufgabeService;
        this.teilaufgabeService = teilaufgabeService;
        this.aufgabeExportMapper = aufgabeExportMapper;
        this.objectMapper = objectMapper;
        this.llmAufgabenImportService = llmAufgabenImportService;
    }

    /**
     * Importiert Aufgaben aus einer einzelnen PDF-Datei.
     * Verwendet das LLM, um Aufgaben aus der PDF zu extrahieren und zu strukturieren.
     *
     * @param pdfFile Die zu importierende PDF-Datei
     * @param kurseinheitId Die ID der Kurseinheit, der die Aufgaben zugeordnet werden sollen
     * @return Eine Liste der importierten Aufgaben als DTOs
     * @throws IOException Wenn ein Fehler beim Lesen der Datei auftritt
     */
    @Transactional
    public List<AufgabeDto> importAufgabenAusPdf(MultipartFile pdfFile, Long kurseinheitId) throws IOException {
        log.info("Beginne Import von Aufgaben aus PDF für Kurseinheit mit ID: {}", kurseinheitId);
        
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("Die PDF-Datei darf nicht leer sein.");
        }
        
        if (!isPdfFile(pdfFile)) {
            throw new IllegalArgumentException("Die hochgeladene Datei ist keine PDF-Datei.");
        }
        
        // PDF-Datei verarbeiten und Aufgaben extrahieren
        LlmPdfImportRequestDto request = new LlmPdfImportRequestDto(pdfFile.getResource(), pdfFile.getContentType());
        LlmPdfImportResponseDto response = llmAufgabenImportService.extractAufgabenFromPdf(request);
        return speichereExtrahierteAufgaben(response, kurseinheitId);
    }
    
    /**
     * Importiert Aufgaben aus zwei PDF-Dateien (Aufgaben und Lösungen).
     * Verwendet das LLM, um Aufgaben und Lösungen zu extrahieren und zu kombinieren.
     *
     * @param assignmentFile Die PDF-Datei mit den Aufgaben
     * @param solutionFile Die PDF-Datei mit den Lösungen (kann null sein)
     * @param kurseinheitId Die ID der Kurseinheit, der die Aufgaben zugeordnet werden sollen
     * @return Eine Liste der importierten Aufgaben als DTOs
     * @throws IOException Wenn ein Fehler beim Lesen der Dateien auftritt
     */
    @Transactional
    public List<AufgabeDto> importAufgabenAusPdfPair(MultipartFile assignmentFile, 
                                                   MultipartFile solutionFile, 
                                                   Long kurseinheitId) throws IOException {
        log.info("Beginne Import von Aufgaben aus Aufgaben-PDF und Lösungs-PDF für Kurseinheit mit ID: {}", kurseinheitId);
        
        if (assignmentFile == null || assignmentFile.isEmpty()) {
            throw new IllegalArgumentException("Die Aufgaben-PDF-Datei darf nicht leer sein.");
        }
        
        if (!isPdfFile(assignmentFile)) {
            throw new IllegalArgumentException("Die hochgeladene Aufgabendatei ist keine PDF-Datei.");
        }
        
        if (solutionFile != null && !solutionFile.isEmpty() && !isPdfFile(solutionFile)) {
            throw new IllegalArgumentException("Die hochgeladene Lösungsdatei ist keine PDF-Datei.");
        }
        
        // PDF-Dateien verarbeiten und Aufgaben extrahieren
        LlmPdfPairImportRequestDto request = new LlmPdfPairImportRequestDto(
                assignmentFile.getResource(), assignmentFile.getContentType(),
                solutionFile.getResource(), solutionFile.getContentType());
        LlmPdfImportResponseDto response = llmAufgabenImportService.extractAufgabenFromPdfPair(request);
        return speichereExtrahierteAufgaben(response, kurseinheitId);
    }
    
    
    /**
     * Speichert die aus PDF-Dateien extrahierten Aufgaben in der Datenbank.
     *
     * @param response Die Antwort mit den extrahierten Aufgaben
     * @param kurseinheitId Die ID der Kurseinheit, der die Aufgaben zugeordnet werden sollen
     * @return Eine Liste der gespeicherten Aufgaben als DTOs
     * @throws IOException Wenn ein Fehler beim Speichern der Aufgaben auftritt
     */
    private List<AufgabeDto> speichereExtrahierteAufgaben(LlmPdfImportResponseDto response, Long kurseinheitId) throws IOException {
        List<AufgabeDto> extrahierteAufgaben = response.getAufgaben();
        
        if (extrahierteAufgaben == null || extrahierteAufgaben.isEmpty()) {
            log.warn("Keine Aufgaben aus der PDF-Datei extrahiert.");
            throw new IllegalArgumentException("Es konnten keine Aufgaben aus der PDF-Datei extrahiert werden. " +
                    "Bitte überprüfen Sie, ob das Dokument Aufgaben enthält oder versuchen Sie es mit einer anderen Datei.");
        }
        
        log.info("Aufgaben aus PDF extrahiert: {} Aufgaben gefunden", extrahierteAufgaben.size());
        
        // Den extrahierten Aufgaben die Kurseinheit-ID zuweisen
        extrahierteAufgaben.forEach(aufgabeDto -> {
            aufgabeDto.setKurseinheitId(kurseinheitId);
            // IDs zurücksetzen, damit neue Entitäten erstellt werden
            aufgabeDto.setId(null);
            if (aufgabeDto.getTeilaufgaben() != null) {
                aufgabeDto.getTeilaufgaben().forEach(teilaufgabe -> teilaufgabe.setId(null));
            }
        });
        
        List<AufgabeDto> importierteAufgaben = new ArrayList<>();
        
        // Jede extrahierte Aufgabe speichern
        for (AufgabeDto aufgabeDto : extrahierteAufgaben) {
            // Speichere die Aufgabe in der Datenbank
            log.info("Speichere extrahierte Aufgabe '{}'{}", aufgabeDto.getTitel(),
                    aufgabeDto.getTeilaufgaben() != null ? " mit " + aufgabeDto.getTeilaufgaben().size() + " Teilaufgaben" : "");
            
            try {
                AufgabeDto gespeicherteAufgabe = aufgabeService.erstelleAufgabe(aufgabeDto);
                log.info("Aufgabe erfolgreich gespeichert mit ID: {}", gespeicherteAufgabe.getId());
                
                // Teilaufgaben separat speichern, da sie jetzt mit der korrekten Aufgaben-ID versehen werden müssen
                if (aufgabeDto.getTeilaufgaben() != null && !aufgabeDto.getTeilaufgaben().isEmpty()) {
                    List<TeilaufgabeDto> teilaufgaben = new ArrayList<>();
                    
                    for (TeilaufgabeDto teilaufgabeDto : aufgabeDto.getTeilaufgaben()) {
                        teilaufgabeDto.setAufgabeId(gespeicherteAufgabe.getId());
                        
                        // Optional: Falls musterloesungFelder oder bewertungshinweise NULL sind, initialisiere sie
                        if (teilaufgabeDto.getMusterloesungFelder() == null) {
                            teilaufgabeDto.setMusterloesungFelder(Map.of());
                        }
                        
                        if (teilaufgabeDto.getMusterloesungBewertungshinweise() == null) {
                            teilaufgabeDto.setMusterloesungBewertungshinweise("");
                        }
                        
                        TeilaufgabeDto gespeicherteTeilaufgabe = teilaufgabeService.erstelleTeilaufgabe(teilaufgabeDto);
                        log.info("Teilaufgabe erfolgreich gespeichert mit ID: {}", gespeicherteTeilaufgabe.getId());
                        teilaufgaben.add(gespeicherteTeilaufgabe);
                    }
                    
                    gespeicherteAufgabe.setTeilaufgaben(teilaufgaben);
                }
                
                importierteAufgaben.add(gespeicherteAufgabe);
            } catch (Exception e) {
                log.error("Fehler beim Speichern der extrahierten Aufgabe", e);
            }
        }

        log.info("PDF-Import abgeschlossen. {} Aufgaben importiert", importierteAufgaben.size());
        
        // Wenn keine Aufgaben importiert werden konnten, wirf eine Exception
        if (importierteAufgaben.isEmpty()) {
            throw new IOException("Fehler beim Importieren der Aufgaben aus der PDF-Datei. " +
                    "Es konnten keine Aufgaben gespeichert werden.");
        }
        
        return importierteAufgaben;
    }

    /**
     * Prüft, ob eine Datei ein PDF ist.
     *
     * @param file Die zu prüfende Datei
     * @return true, wenn die Datei ein PDF ist, sonst false
     */
    public boolean isPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        // Prüfe auf Content-Type
        boolean hasPdfContentType = contentType != null && contentType.equals("application/pdf");

        // Prüfe auf Dateiendung
        boolean hasPdfExtension = fileName != null && fileName.toLowerCase().endsWith(".pdf");

        return hasPdfContentType || hasPdfExtension;
    }

    /**
     * Prüft, ob eine Datei ein JSON ist.
     *
     * @param file Die zu prüfende Datei
     * @return true, wenn die Datei ein JSON ist, sonst false
     */
    public boolean isJsonFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        // Prüfe auf Content-Type
        boolean hasJsonContentType = contentType != null &&
                (contentType.equals("application/json") || contentType.equals("text/json"));

        // Prüfe auf Dateiendung
        boolean hasJsonExtension = fileName != null && fileName.toLowerCase().endsWith(".json");

        return hasJsonContentType || hasJsonExtension;
    }

    /**
     * Importiert Aufgaben aus einer JSON-Datei.
     *
     * @param jsonFile Die zu importierende JSON-Datei
     * @param kurseinheitId Die ID der Kurseinheit, der die Aufgaben zugeordnet werden sollen
     * @return Eine Liste der importierten Aufgaben als DTOs
     * @throws IOException Wenn ein Fehler beim Lesen der Datei auftritt
     */
    @Transactional
    public List<AufgabeDto> importiereAufgabenAusJson(MultipartFile jsonFile, Long kurseinheitId) throws IOException {
        if (jsonFile == null || jsonFile.isEmpty()) {
            throw new IllegalArgumentException("Die JSON-Datei darf nicht leer sein.");
        }

        String jsonContent = new String(jsonFile.getBytes());
        return importiereAufgabenAusJson(jsonContent, kurseinheitId);
    }

    /**
     * Importiert Aufgaben aus einem JSON-String.
     *
     * @param jsonString Der JSON-String mit den zu importierenden Aufgaben
     * @param kurseinheitId Die ID der Kurseinheit, der die Aufgaben zugeordnet werden sollen
     * @return Eine Liste der importierten Aufgaben als DTOs
     * @throws IOException Wenn ein Fehler beim Parsen des JSON-Strings auftritt
     */
    @Transactional
    public List<AufgabeDto> importiereAufgabenAusJson(String jsonString, Long kurseinheitId) throws IOException {
        // Prüfe, ob der JSON-String eine Liste oder ein einzelnes Objekt enthält
        log.info("Beginne Import von Aufgaben aus JSON für Kurseinheit mit ID: {}", kurseinheitId);
        if (jsonString.trim().startsWith("[")) {
            // Import einer Liste von Aufgaben
            List<AufgabeExportDTO> aufgabenExport = objectMapper.readValue(jsonString,
                    new TypeReference<List<AufgabeExportDTO>>() {});
            log.info("JSON enthält eine Liste von {} Aufgaben", aufgabenExport.size());
            return speichereImportierteAufgaben(aufgabenExport, kurseinheitId);
        } else {
            // Import einer einzelnen Aufgabe
            log.info("JSON enthält eine einzelne Aufgabe");
            AufgabeExportDTO aufgabeExport = objectMapper.readValue(jsonString, AufgabeExportDTO.class);
            log.info("Einzelne Aufgabe gefunden: '{}'", aufgabeExport.getTitel());
            List<AufgabeExportDTO> aufgabenExport = new ArrayList<>();
            aufgabenExport.add(aufgabeExport);
            return speichereImportierteAufgaben(aufgabenExport, kurseinheitId);
        }
    }

    /**
     * Speichert importierte Aufgaben in der Datenbank.
     *
     * @param aufgabenExport Die Liste der importierten Aufgaben als Export-DTOs
     * @param kurseinheitId Die ID der Kurseinheit, der die Aufgaben zugeordnet werden sollen
     * @return Eine Liste der gespeicherten Aufgaben als DTOs
     */
    @Transactional
    public List<AufgabeDto> speichereImportierteAufgaben(List<AufgabeExportDTO> aufgabenExport, Long kurseinheitId) {
        log.info("Beginne Speichern von {} importierten Aufgaben für Kurseinheit mit ID: {}",
                aufgabenExport.size(), kurseinheitId);
        List<AufgabeDto> importierteAufgaben = new ArrayList<>();

        for (AufgabeExportDTO aufgabeExport : aufgabenExport) {
            // Konvertiere das Export-DTO in ein reguläres DTO
            AufgabeDto aufgabeDto = aufgabeExportMapper.fromExportDto(aufgabeExport);

            // Setze die Kurseinheit-ID
            aufgabeDto.setKurseinheitId(kurseinheitId);

            // Entferne ID, damit eine neue Entität erstellt wird
            aufgabeDto.setId(null);

            // Entferne IDs von Teilaufgaben, damit neue Entitäten erstellt werden
            if (aufgabeDto.getTeilaufgaben() != null) {
                aufgabeDto.getTeilaufgaben().forEach(ta -> ta.setId(null));
            }

            // Speichere die Aufgabe in der Datenbank
            log.info("Speichere Aufgabe '{}'{}", aufgabeDto.getTitel(),
                    aufgabeDto.getTeilaufgaben() != null ? " mit " + aufgabeDto.getTeilaufgaben().size() + " Teilaufgaben" : "");
            AufgabeDto gespeicherteAufgabe = aufgabeService.erstelleAufgabe(aufgabeDto);
            log.info("Aufgabe erfolgreich gespeichert mit ID: {}", gespeicherteAufgabe.getId());
            importierteAufgaben.add(gespeicherteAufgabe);
        }

        log.info("Import abgeschlossen. Insgesamt wurden {} Aufgaben erfolgreich importiert", importierteAufgaben.size());
        return importierteAufgaben;
    }
}
