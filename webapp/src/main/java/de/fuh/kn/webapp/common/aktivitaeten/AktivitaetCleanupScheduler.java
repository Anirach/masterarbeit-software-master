package de.fuh.kn.webapp.common.aktivitaeten;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduler-Komponente, die regelmäßig alte Aktivitätseinträge aus der Datenbank entfernt.
 * Die maximale Aufbewahrungsdauer (in Monaten) wird über die Anwendungskonfiguration gesteuert.
 */
@Component
@Slf4j
public class AktivitaetCleanupScheduler {


    private final AktivitaetsService aktivitaetsService;
    /**
     * Anzahl der Monate, nach denen Aktivitätseinträge als alt gelten und gelöscht werden.
     * Konfigurierbar über application.yaml (cleanup.aktivitaet.months).
     */
    @Value("${app.aktivitaet-cleanup-months:6}")
    private int months;

    public AktivitaetCleanupScheduler(AktivitaetsService aktivitaetsService) {
        this.aktivitaetsService = aktivitaetsService;
    }

    /**
     * Geplanter Task, der täglich um 2 Uhr morgens alte Aktivitätseinträge löscht.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteOldAktivitaeten() {
        long deletedCount = aktivitaetsService.loescheAktivitaetenAelterAls(months);
        log.info("{} alte Aktivitätseinträge wurden gelöscht.", deletedCount);
    }
}
