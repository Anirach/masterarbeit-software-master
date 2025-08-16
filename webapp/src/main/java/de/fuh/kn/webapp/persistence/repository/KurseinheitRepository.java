package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf Kurseinheit-Entitäten in der Datenbank.
 */
@Repository
public interface KurseinheitRepository extends JpaRepository<Kurseinheit, Long> {
    
    /**
     * Findet alle Kurseinheiten eines bestimmten Kurses.
     *
     * @param kurs Der Kurs, dessen Kurseinheiten gesucht werden.
     * @return Eine Liste aller Kurseinheiten des angegebenen Kurses.
     */
    List<Kurseinheit> findByKurs(Kurs kurs);
    
    /**
     * Findet alle Kurseinheiten eines bestimmten Kurses, sortiert nach Reihenfolge.
     *
     * @param kurs Der Kurs, dessen Kurseinheiten gesucht werden.
     * @return Eine Liste aller Kurseinheiten des angegebenen Kurses, sortiert nach Reihenfolge.
     */
    List<Kurseinheit> findByKursOrderByReihenfolgeAsc(Kurs kurs);
    
    /**
     * Findet eine Kurseinheit anhand ihres Namens und des zugehörigen Kurses.
     *
     * @param name Der Name der Kurseinheit.
     * @param kurs Der Kurs, zu dem die Kurseinheit gehört.
     * @return Ein Optional mit der gefundenen Kurseinheit oder ein leeres Optional, wenn keine Kurseinheit existiert.
     */
    Optional<Kurseinheit> findByNameAndKurs(String name, Kurs kurs);
    
    /**
     * Findet die nächste Kurseinheit nach einer gegebenen Reihenfolgenummer innerhalb eines Kurses.
     *
     * @param kurs Der Kurs, in dem gesucht wird.
     * @param reihenfolge Die Reihenfolgenummer, nach der die nächste Kurseinheit gesucht wird.
     * @return Ein Optional mit der nächsten Kurseinheit oder ein leeres Optional, wenn keine folgende Kurseinheit existiert.
     */
    Optional<Kurseinheit> findFirstByKursAndReihenfolgeGreaterThanOrderByReihenfolgeAsc(Kurs kurs, int reihenfolge);
}
