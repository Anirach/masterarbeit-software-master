package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Aktivitaet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository für den Zugriff auf Aktivitätsdaten in der Datenbank.
 * Stellt Methoden zum Speichern, Abfragen und Verwalten von Aktivitätsprotokollen bereit.
 */
@Repository
public interface AktivitaetRepository extends JpaRepository<Aktivitaet, Long>, JpaSpecificationExecutor<Aktivitaet> {


    /**
     * Löscht alle Aktivitätseinträge, deren Zeitpunkt vor dem angegebenen Stichtag liegt.
     *
     * @param cutoff Zeitpunkt, vor dem Einträge als alt gelten
     * @return Anzahl der gelöschten Einträge
     */
    long deleteByZeitpunktBefore(LocalDateTime cutoff);

}
