package de.fuh.kn.webapp.llm.rag.event;

import de.fuh.kn.webapp.llm.rag.storage.VektorSpeicherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Komponente, die auf KursMaterialEvents reagiert und entsprechende Aktionen auslöst.
 * Insbesondere die automatische Indexierung von neu hochgeladenen Dokumenten.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KursMaterialEventListener {
    
    private final VektorSpeicherService vektorSpeicherService;
    
    /**
     * Verarbeitet asynchron KursMaterialEvents.
     * Je nach Operation (CREATE, UPDATE, DELETE) werden verschiedene Aktionen ausgeführt.
     *
     * @param event Das zu verarbeitende Event.
     */
    @Async
    @EventListener
    public void handleKursMaterialEvent(KursMaterialEvent event) {
        Long kursMaterialId = event.getKursMaterialId();
        String source = event.getSource().getClass().getSimpleName();
        
        switch (event.getOperation()) {
            case CREATE:
                log.info("Neues Kursmaterial {} für Indexierung erkannt (Quelle: {})", kursMaterialId, source);
                vektorSpeicherService.indexiereKursMaterialAsync(kursMaterialId);
                break;
                
            case UPDATE:
                log.info("Aktualisiertes Kursmaterial {} für Neuindexierung erkannt (Quelle: {})", kursMaterialId, source);
                // Zuerst alte Einträge entfernen, dann neu indexieren
                vektorSpeicherService.entferneKursMaterialAusVektorspeicher(kursMaterialId);
                vektorSpeicherService.indexiereKursMaterialAsync(kursMaterialId);
                break;
                
            case DELETE:
                log.info("Gelöschtes Kursmaterial {} für Entfernung aus Vektorspeicher erkannt (Quelle: {})", kursMaterialId, source);
                vektorSpeicherService.entferneKursMaterialAusVektorspeicher(kursMaterialId);
                break;
                
            default:
                log.warn("Unbekannte Operation für Kursmaterial {}: {} (Quelle: {})", 
                        kursMaterialId, event.getOperation(), source);
                break;
        }
    }
}