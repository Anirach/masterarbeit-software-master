package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Kursbetreuer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository für den Zugriff auf Kursbetreuer-Entitäten in der Datenbank.
 */
@Repository
public interface KursbetreuerRepository extends JpaRepository<Kursbetreuer, Long> {
    
    /**
     * Findet einen Kursbetreuer anhand seiner E-Mail-Adresse.
     *
     * @param email Die E-Mail-Adresse des Kursbetreuers.
     * @return Ein Optional mit dem gefundenen Kursbetreuer oder ein leeres Optional, wenn kein Kursbetreuer mit der E-Mail-Adresse existiert.
     */
    Optional<Kursbetreuer> findByEmail(String email);
    
    /**
     * Überprüft, ob ein Kursbetreuer mit der angegebenen E-Mail-Adresse existiert.
     *
     * @param email Die E-Mail-Adresse des Kursbetreuers.
     * @return True, wenn ein Kursbetreuer mit der E-Mail-Adresse existiert, sonst False.
     */
    boolean existsByEmail(String email);
    
    /**
     * Findet alle Kursbetreuer, sortiert nach Nachname und Vorname.
     *
     * @return Eine Liste aller Kursbetreuer, sortiert nach Namen.
     */
    java.util.List<Kursbetreuer> findAllByOrderByNachnameAscVornameAsc();
}
