package de.fuh.kn.webapp.kursbetreuung.dashboard;

import de.fuh.kn.webapp.persistence.entity.ChatNachricht;
import de.fuh.kn.webapp.persistence.entity.LoesungsVersuch;
import de.fuh.kn.webapp.persistence.repository.ChatNachrichtRepository;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.NutzerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service für Dashboard-Statistiken
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardStatisticsService {

    private final LoesungsVersuchRepository loesungsVersuchRepository;
    private final ChatNachrichtRepository chatNachrichtRepository;
    private final NutzerRepository nutzerRepository;

    /**
     * Holt die Dashboard-Statistiken für die letzten 24 Stunden
     * 
     * @return DashboardStatisticsDTO mit den aggregierten Daten
     */
    public DashboardStatisticsDTO getDashboardStatistics() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        
        // Anzahl Lösungsversuche in den letzten 24h
        int anzahlLoesungsversuche = countLoesungsversucheSince(twentyFourHoursAgo);
        
        // Anzahl Chat-Nachrichten in den letzten 24h
        int anzahlChatNachrichten = countChatNachrichtenSince(twentyFourHoursAgo);
        
        // AI-Kosten der letzten 24h
        BigDecimal aiKostenGesamt = calculateAiKostenSince(twentyFourHoursAgo);
        
        // Anzahl aktive Nutzer (Studenten) in den letzten 24h
        int anzahlAktiveNutzer = countActiveUsersSince(twentyFourHoursAgo);
        
        return DashboardStatisticsDTO.builder()
                .anzahlLoesungsversuche(anzahlLoesungsversuche)
                .anzahlChatNachrichten(anzahlChatNachrichten)
                .aiKostenGesamt(aiKostenGesamt)
                .anzahlAktiveNutzer(anzahlAktiveNutzer)
                .build();
    }
    
    private int countLoesungsversucheSince(LocalDateTime since) {
        List<LoesungsVersuch> versuche = loesungsVersuchRepository.findAll();
        return (int) versuche.stream()
                .filter(v -> v.getZeitpunkt() != null && v.getZeitpunkt().isAfter(since))
                .count();
    }
    
    private int countChatNachrichtenSince(LocalDateTime since) {
        List<ChatNachricht> nachrichten = chatNachrichtRepository.findAll();
        return (int) nachrichten.stream()
                .filter(n -> n.getZeitpunkt() != null && n.getZeitpunkt().isAfter(since))
                .count();
    }
    
    private BigDecimal calculateAiKostenSince(LocalDateTime since) {
        BigDecimal totalCosts = BigDecimal.ZERO;
        
        // Kosten aus Lösungsversuchen
        List<LoesungsVersuch> versuche = loesungsVersuchRepository.findAll();
        for (LoesungsVersuch versuch : versuche) {
            if (versuch.getZeitpunkt() != null && versuch.getZeitpunkt().isAfter(since) && versuch.getKosten() != null) {
                totalCosts = totalCosts.add(versuch.getKosten());
            }
        }
        
        // Kosten aus Chat-Nachrichten
        List<ChatNachricht> nachrichten = chatNachrichtRepository.findAll();
        for (ChatNachricht nachricht : nachrichten) {
            if (nachricht.getZeitpunkt() != null && nachricht.getZeitpunkt().isAfter(since) && nachricht.getKosten() != null) {
                totalCosts = totalCosts.add(nachricht.getKosten());
            }
        }
        
        return totalCosts;
    }
    
    private int countActiveUsersSince(LocalDateTime since) {
        // Zähle eindeutige Studenten die in den letzten 24h aktiv waren
        // (entweder Lösungsversuche oder Chat-Nachrichten)
        List<LoesungsVersuch> versuche = loesungsVersuchRepository.findAll();
        List<ChatNachricht> nachrichten = chatNachrichtRepository.findAll();
        
        return (int) versuche.stream()
                .filter(v -> v.getZeitpunkt() != null && v.getZeitpunkt().isAfter(since))
                .map(v -> v.getStudent().getId())
                .distinct()
                .count();
    }
}