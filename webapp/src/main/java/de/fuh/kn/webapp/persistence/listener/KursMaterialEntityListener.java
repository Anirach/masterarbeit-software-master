package de.fuh.kn.webapp.persistence.listener;

import de.fuh.kn.webapp.llm.rag.event.KursMaterialEvent;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import jakarta.persistence.PostRemove;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Entity Listener für KursMaterial-Entitäten.
 * Registriert Lifecycle-Events für Kursmaterialien, insbesondere für Löschoperationen.
 * Dient dazu, Events auch bei Cascade-Operationen (z.B. Löschen eines Kurses) auszulösen.
 */
@Component
@Slf4j
public class KursMaterialEntityListener {

    private static ApplicationEventPublisher eventPublisher;

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        KursMaterialEntityListener.eventPublisher = eventPublisher;
    }

    /**
     * Wird nach dem Löschen einer KursMaterial-Entität aufgerufen.
     * Löst ein DELETE-Event aus, damit der Vektorspeicher aktualisiert werden kann.
     *
     * @param kursMaterial Das gelöschte Kursmaterial
     */
    @PostRemove
    public void postRemove(KursMaterial kursMaterial) {
        if (eventPublisher != null && kursMaterial.getId() != null) {
            log.info("KursMaterial {} wurde gelöscht, KursMaterialEvent wird gefeuert", kursMaterial.getId());
            if (kursMaterial.getTyp() == KursMaterial.KursMaterialTyp.DOKUMENT) {
                eventPublisher.publishEvent(new KursMaterialEvent(
                        this,
                        KursMaterialEvent.KursMaterialOperation.DELETE,
                        kursMaterial.getId()));
            }
        } else {
            if (eventPublisher == null) {
                log.warn("EventPublisher ist null, kann kein Event für gelöschtes KursMaterial {} auslösen", 
                        kursMaterial.getId());
            }
        }
    }
}