package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.llm.dto.generator.LlmGeneratorRequestDto;
import de.fuh.kn.webapp.llm.dto.generator.LlmGeneratorResponseDto;
import de.fuh.kn.webapp.llm.service.LlmAufgabenGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service für die Generierung neuer Aufgaben basierend auf Kursmaterial und Beispielaufgaben.
 * <p>
 * Diese Klasse bietet Funktionalität zur KI-gestützten Generierung von Aufgaben:
 * <ul>
 *   <li>Nutzung von RAG, um relevante Kursmaterialien zu finden</li>
 *   <li>Generierung von Aufgaben basierend auf bestehendem Kursmaterial</li>
 *   <li>Verwendung bestehender Aufgaben als Beispiele</li>
 * </ul>
 */
@Service
@Slf4j
public class AufgabenGeneratorService {

    private final AufgabeService aufgabeService;
    private final TeilaufgabeService teilaufgabeService;
    private final KurseinheitService kurseinheitService;
    private final LlmAufgabenGeneratorService llmAufgabenGeneratorService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param aufgabeService Der Service für Aufgaben
     * @param teilaufgabeService Der Service für Teilaufgaben
     * @param kurseinheitService Der Service für Kurseinheiten
     * @param llmAufgabenGeneratorService Service für die LLM-basierte Aufgabengenerierung
     */
    @Autowired
    public AufgabenGeneratorService(
            AufgabeService aufgabeService,
            TeilaufgabeService teilaufgabeService,
            KurseinheitService kurseinheitService,
            LlmAufgabenGeneratorService llmAufgabenGeneratorService) {
        this.aufgabeService = aufgabeService;
        this.teilaufgabeService = teilaufgabeService;
        this.kurseinheitService = kurseinheitService;
        this.llmAufgabenGeneratorService = llmAufgabenGeneratorService;
    }




    /**
     * Generiert eine neue Aufgabe basierend auf einem Thema und Beispielaufgaben.
     *
     * @param thema Das Thema für die neue Aufgabe
     * @param kurseinheitId Die ID der Kurseinheit für die Aufgabe
     * @param beispielAufgabenIds IDs von Beispielaufgaben (optional)
     * @return Die generierte Aufgabe als DTO
     * @throws Exception bei Fehlern während der Generierung
     */
    @Transactional
    public AufgabeDto generiereAufgabe(String thema, Long kurseinheitId, List<Long> beispielAufgabenIds) throws Exception {
        log.info("Starte Generierung einer Aufgabe zum Thema '{}' für Kurseinheit {}", thema, kurseinheitId);
        
        // Prüfen, ob die Kurseinheit existiert
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            throw new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " nicht gefunden");
        }
        
        // Beispielaufgaben laden, falls IDs vorhanden
        List<AufgabeDto> beispielaufgaben = new ArrayList<>();
        if (beispielAufgabenIds != null && !beispielAufgabenIds.isEmpty()) {
            for (Long aufgabeId : beispielAufgabenIds) {
                AufgabeDto aufgabe = aufgabeService.getAufgabeById(aufgabeId);
                if (aufgabe != null) {
                    beispielaufgaben.add(aufgabe);
                }
            }
        } else {
            // Falls keine Beispielaufgaben angegeben, automatisch einige aus dem gesamten Kurs verwenden
            beispielaufgaben = aufgabeService.getAufgabenByKursId(kurseinheit.getKursId());
            // Beschränke auf maximal 3 Beispielaufgaben
            if (beispielaufgaben.size() > 3) {
                beispielaufgaben = beispielaufgaben.subList(0, 3);
            }
        }

        // Aufgabe generieren mit LLM
        LlmGeneratorRequestDto request = new LlmGeneratorRequestDto(thema, kurseinheit.getKursId(), kurseinheit, beispielaufgaben);
        LlmGeneratorResponseDto response = llmAufgabenGeneratorService.generiereAufgabe(request);
        
        if (!response.isSuccessful()) {
            throw new Exception("Fehler bei der Aufgabengenerierung: " + response.getErrorMessage());
        }
        
        AufgabeDto generierteAufgabe = response.getAufgabe();
        generierteAufgabe.setKurseinheitId(kurseinheit.getId());
        
        log.info("Aufgabengenerierung durchgeführt mit {} Input-Token und {} Output-Token. Kosten: ${}",
                response.getInputTokens(), response.getOutputTokens(), 
                response.getCost() != null ? response.getCost().toPlainString() : "0.00");
        
        return generierteAufgabe;
    }


    /**
     * Erstellt eine Aufgabe auf Basis einer generierten Aufgabe.
     *
     * @param aufgabeDto Die generierte Aufgabe als DTO
     * @return Die erstellte Aufgabe als DTO
     * @throws Exception bei Fehlern während der Erstellung
     */
    @Transactional
    public AufgabeDto erstelleGenerierteAufgabe(AufgabeDto aufgabeDto) throws Exception {
        try {
            // Aufgabe speichern
            AufgabeDto erstellteAufgabe = aufgabeService.erstelleAufgabe(aufgabeDto);
            
            // Teilaufgaben speichern
            if (aufgabeDto.getTeilaufgaben() != null && !aufgabeDto.getTeilaufgaben().isEmpty()) {
                for (TeilaufgabeDto teilaufgabeDto : aufgabeDto.getTeilaufgaben()) {
                    teilaufgabeDto.setAufgabeId(erstellteAufgabe.getId());
                    teilaufgabeService.erstelleTeilaufgabe(teilaufgabeDto);
                }
            }
            
            return erstellteAufgabe;
        } catch (Exception e) {
            log.error("Fehler beim Erstellen der generierten Aufgabe: {}", e.getMessage(), e);
            throw new Exception("Fehler beim Erstellen der Aufgabe: " + e.getMessage(), e);
        }
    }

}