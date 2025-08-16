package de.fuh.kn.webapp.llm.rag.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event, das bei Operationen auf Kursmaterialien ausgelöst wird.
 * Kann für asynchrone Verarbeitung von Dokumenten verwendet werden.
 */
@Getter
public class KursMaterialEvent extends ApplicationEvent {

    /**
     *  Gibt die Operation zurück, die auf dem Kursmaterial durchgeführt wurde.
     *
     * @return Die Operation.
     */
    private final KursMaterialOperation operation;
    /**
     *  Gibt die ID des betroffenen Kursmaterials zurück.
     *
     * @return Die ID des Kursmaterials.
     */
    private final Long kursMaterialId;
    
    /**
     * Erstellt ein neues KursMaterialEvent.
     *
     * @param source Die Quelle des Events.
     * @param operation Die Operation, die auf dem Kursmaterial durchgeführt wurde.
     * @param kursMaterialId Die ID des betroffenen Kursmaterials.
     */
    public KursMaterialEvent(Object source, KursMaterialOperation operation, Long kursMaterialId) {
        super(source);
        this.operation = operation;
        this.kursMaterialId = kursMaterialId;
    }

    /**
     * Aufzählung der möglichen Operationen auf Kursmaterialien.
     */
    public enum KursMaterialOperation {
        /**
         * Ein neues Kursmaterial wurde erstellt.
         */
        CREATE,
        
        /**
         * Ein bestehendes Kursmaterial wurde aktualisiert.
         */
        UPDATE,
        
        /**
         * Ein Kursmaterial wurde gelöscht.
         */
        DELETE
    }
}