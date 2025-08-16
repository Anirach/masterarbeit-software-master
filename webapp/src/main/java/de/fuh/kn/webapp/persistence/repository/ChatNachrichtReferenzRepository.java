package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.ChatNachricht;
import de.fuh.kn.webapp.persistence.entity.ChatNachrichtReferenz;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository für den Zugriff auf ChatNachrichtReferenz-Entitäten.
 */
@Repository
public interface ChatNachrichtReferenzRepository extends JpaRepository<ChatNachrichtReferenz, Long> {
    
    /**
     * Findet alle Referenzen für eine bestimmte ChatNachricht.
     * 
     * @param chatNachricht Die ChatNachricht, für die die Referenzen gefunden werden sollen
     * @return Liste der Referenzen
     */
    List<ChatNachrichtReferenz> findByChatNachricht(ChatNachricht chatNachricht);
    
    /**
     * Findet alle Referenzen für ein bestimmtes Kursmaterial.
     * 
     * @param kursMaterial Das Kursmaterial, für das die Referenzen gefunden werden sollen
     * @return Liste der Referenzen
     */
    List<ChatNachrichtReferenz> findByKursMaterial(KursMaterial kursMaterial);
}