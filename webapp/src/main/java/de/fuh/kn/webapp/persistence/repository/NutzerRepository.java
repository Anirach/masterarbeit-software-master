package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Nutzer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf Nutzer-Entitäten in der Datenbank.
 */
@Repository
public interface NutzerRepository extends JpaRepository<Nutzer, Long> {
    
    /**
     * Findet einen Nutzer anhand seiner E-Mail-Adresse.
     *
     * @param email Die E-Mail-Adresse des Nutzers.
     * @return Ein Optional mit dem gefundenen Nutzer oder ein leeres Optional, wenn kein Nutzer mit der E-Mail-Adresse existiert.
     */
    Optional<Nutzer> findByEmail(String email);
    
    /**
     * Überprüft, ob ein Nutzer mit der angegebenen E-Mail-Adresse existiert.
     *
     * @param email Die E-Mail-Adresse des Nutzers.
     * @return True, wenn ein Nutzer mit der E-Mail-Adresse existiert, sonst False.
     */
    boolean existsByEmail(String email);

    /**
     * Findet alle Nutzer, deren Vor- oder Nachname den angegebenen Suchbegriff (case-insensitive) enthält
     * oder deren vollständiger Name (Vorname + Nachname) den Suchbegriff enthält.
     *
     * @param suchbegriff Der Suchbegriff
     * @return Eine Liste der gefundenen Nutzer.
     */
    @Query("SELECT n FROM Nutzer n WHERE " +
           "LOWER(n.vorname) LIKE LOWER(CONCAT('%', :suchbegriff, '%')) OR " +
           "LOWER(n.nachname) LIKE LOWER(CONCAT('%', :suchbegriff, '%')) OR " +
           "LOWER(CONCAT(n.vorname, ' ', n.nachname)) LIKE LOWER(CONCAT('%', :suchbegriff, '%'))")
    List<Nutzer> findByNameContaining(@Param("suchbegriff") String suchbegriff);
}
