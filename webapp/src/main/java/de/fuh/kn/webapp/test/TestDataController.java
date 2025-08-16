package de.fuh.kn.webapp.test;

import de.fuh.kn.webapp.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller für Test-spezifische Funktionen.
 * Nur verfügbar wenn app.initial-data.enabled=true ist.
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "app.test-data.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TestDataController {

    private final LoesungsVersuchRepository loesungsVersuchRepository;
    private final ChatNachrichtRepository chatNachrichtRepository;
    private final ChatRepository chatRepository;
    private final BelegungRepository belegungRepository;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final AufgabeRepository aufgabeRepository;
    private final KurseinheitRepository kurseinheitRepository;
    private final KursMaterialRepository kursMaterialRepository;
    private final KursRepository kursRepository;
    private final StudentRepository studentRepository;
    private final KursbetreuerRepository kursbetreuerRepository;
    private final AktivitaetRepository aktivitaetRepository;
    private final ChatNachrichtReferenzRepository chatNachrichtReferenzRepository;
    
    private final TestDataCreator testDataCreator;

    /**
     * Setzt die Datenbank auf den initialen Zustand zurück.
     * Diese Methode löscht alle Daten und erstellt die initialen Testdaten neu.
     */
    @PostMapping("/db/reset")
    @Transactional
    public ResponseEntity<String> resetDatabase() {
        try {
            log.info("Starting database reset...");
            
            // Lösche alle Daten in der richtigen Reihenfolge (wegen Foreign Keys)
            chatNachrichtReferenzRepository.deleteAll();
            chatNachrichtRepository.deleteAll();
            chatRepository.deleteAll();
            loesungsVersuchRepository.deleteAll();
            aktivitaetRepository.deleteAll();
            belegungRepository.deleteAll();
            teilaufgabeRepository.deleteAll();
            aufgabeRepository.deleteAll();
            kursMaterialRepository.deleteAll();
            kurseinheitRepository.deleteAll();
            kursRepository.deleteAll();
            studentRepository.deleteAll();
            kursbetreuerRepository.deleteAll();
            
            log.info("All data deleted successfully");
            
            // Führe die initiale Datenbank-Setup Logik aus
            testDataCreator.createInitialData();
            
            log.info("Database reset completed successfully");
            return ResponseEntity.ok("Database reset completed");
            
        } catch (Exception e) {
            log.error("Error during database reset", e);
            return ResponseEntity.internalServerError()
                .body("Error during database reset: " + e.getMessage());
        }
    }
}