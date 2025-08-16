package de.fuh.kn.webapp.llm.rag.config;

import de.fuh.kn.webapp.llm.rag.storage.VektorSpeicherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Komponente zum automatischen Indexieren von nicht-indexierten Dokumenten beim Anwendungsstart.
 * Diese Komponente indexiert beim Start der Anwendung alle Dokumente, die noch nicht im Vektorspeicher
 * indexiert wurden.
 * <p>
 * Die automatische Indexierung erfolgt nur, wenn das Profil nicht "test" ist, um die Tests nicht
 * zu verlangsamen oder zu beeinflussen.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!unittest")
public class RagStartupIndexer implements ApplicationListener<ApplicationReadyEvent> {

    private final VektorSpeicherService vektorSpeicherService;

    /**
     * Wird aufgerufen, wenn die Anwendung gestartet und bereit ist.
     * Indexiert alle nicht-indexierten Dokumente im Vektorspeicher.
     *
     * @param event Das ApplicationReadyEvent, das signalisiert, dass die Anwendung bereit ist.
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Anwendung gestartet. Beginne automatische Indexierung nicht-indexierter Dokumente...");
        
        try {
            int indexierteAnzahl = vektorSpeicherService.indexiereNichtIndexierteDokumente();
            if (indexierteAnzahl > 0) {
                log.info("Automatische Indexierung abgeschlossen. {} Dokumente erfolgreich indexiert.", indexierteAnzahl);
            } else {
                log.info("Automatische Indexierung abgeschlossen. Keine Dokumente zum Indexieren gefunden oder alle Indexierungsversuche fehlgeschlagen.");
            }
        } catch (Exception e) {
            log.error("Fehler bei der automatischen Indexierung: {}", e.getMessage(), e);
        }
    }
}