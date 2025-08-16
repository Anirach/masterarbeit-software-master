package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf KursMaterial-Entitäten in der Datenbank.
 */
@Repository
public interface KursMaterialRepository extends JpaRepository<KursMaterial, Long> {
    
    /**
     * Findet alle Kursmaterialien eines bestimmten Kurses.
     *
     * @param kurs Der Kurs, dessen Materialien gesucht werden.
     * @return Eine Liste aller Kursmaterialien des angegebenen Kurses.
     */
    List<KursMaterial> findByKurs(Kurs kurs);
    
    /**
     * Findet alle Kursmaterialien einer bestimmten Kurseinheit.
     *
     * @param kurseinheit Die Kurseinheit, deren Materialien gesucht werden.
     * @return Eine Liste aller Kursmaterialien der angegebenen Kurseinheit.
     */
    List<KursMaterial> findByKurseinheit(Kurseinheit kurseinheit);
    
    /**
     * Findet alle Dokument-Kursmaterialien (d.h. Materialien, die vom Typ "Dokument" sind)
     * und lädt die zugehörigen Kurs- und Kurseinheit-Entitäten eager.
     *
     * @return Eine Liste aller Dokument-Kursmaterialien mit eager geladenen Beziehungen.
     */
    @Query("SELECT km FROM KursMaterial km LEFT JOIN FETCH km.kurs LEFT JOIN FETCH km.kurseinheit WHERE km.typ = 'DOKUMENT'")
    List<KursMaterial> findAllDocuments();
    
    /**
     * Findet alle nicht-indexierten Dokument-Kursmaterialien und lädt die zugehörigen 
     * Kurs- und Kurseinheit-Entitäten eager.
     *
     * @return Eine Liste aller nicht-indexierten Dokument-Kursmaterialien mit eager geladenen Beziehungen.
     */
    @Query("SELECT km FROM KursMaterial km LEFT JOIN FETCH km.kurs LEFT JOIN FETCH km.kurseinheit WHERE km.typ = 'DOKUMENT' AND (km.indexiert = false OR km.indexiert IS NULL)")
    List<KursMaterial> findAllNonIndexedDocuments();
    
    /**
     * Findet alle Bild-Kursmaterialien (d.h. Materialien, die vom Typ "Bild" sind).
     *
     * @return Eine Liste aller Bild-Kursmaterialien.
     */
    @Query("SELECT km FROM KursMaterial km WHERE km.typ = 'BILD'")
    List<KursMaterial> findAllImages();

    /**
     * Findet ein Bild-Kursmaterial anhand des Namens in einem bestimmten Kurs.
     *
     * @param name Der Name des Bildes.
     * @param kurs Der Kurs, in dem das Bild gesucht wird.
     * @return Ein Optional mit dem gefundenen Bild oder ein leeres Optional.
     */
    @Query("SELECT km FROM KursMaterial km WHERE km.typ = 'BILD' AND km.name = :name AND km.kurs = :kurs")
    Optional<KursMaterial> findImageByNameAndKurs(@Param("name") String name, @Param("kurs") Kurs kurs);
    
    /**
     * Findet ein Bild-Kursmaterial anhand des Namens in einer bestimmten Kurseinheit.
     *
     * @param name Der Name des Bildes.
     * @param kurseinheit Die Kurseinheit, in der das Bild gesucht wird.
     * @return Ein Optional mit dem gefundenen Bild oder ein leeres Optional.
     */
    @Query("SELECT km FROM KursMaterial km WHERE km.typ = 'BILD' AND km.name = :name AND km.kurseinheit = :kurseinheit")
    Optional<KursMaterial> findImageByNameAndKurseinheit(@Param("name") String name, @Param("kurseinheit") Kurseinheit kurseinheit);
    
    /**
     * Findet alle Kursmaterialien eines bestimmten Typs in einer Kurseinheit.
     *
     * @param kurseinheit Die Kurseinheit, deren Materialien gesucht werden.
     * @param typ Der Typ der Kursmaterialien.
     * @return Eine Liste aller Kursmaterialien des angegebenen Typs in der Kurseinheit.
     */
    @Query("SELECT km FROM KursMaterial km WHERE km.kurseinheit = :kurseinheit AND km.typ = :typ")
    List<KursMaterial> findByKurseinheitAndTyp(@Param("kurseinheit") Kurseinheit kurseinheit, @Param("typ") KursMaterial.KursMaterialTyp typ);
    
    /**
     * Findet alle Kursmaterialien eines bestimmten Typs in einem Kurs.
     *
     * @param kurs Der Kurs, dessen Materialien gesucht werden.
     * @param typ Der Typ der Kursmaterialien.
     * @return Eine Liste aller Kursmaterialien des angegebenen Typs im Kurs.
     */
    @Query("SELECT km FROM KursMaterial km WHERE km.kurs = :kurs AND km.typ = :typ")
    List<KursMaterial> findByKursAndTyp(@Param("kurs") Kurs kurs, @Param("typ") KursMaterial.KursMaterialTyp typ);

    /**
     * Findet ein Kursmaterial anhand seiner ID und lädt die zugehörigen Kurs- und Kurseinheit-Entitäten eager.
     * Dies verhindert LazyInitializationExceptions in asynchronen Kontexten.
     *
     * @param id Die ID des Kursmaterials.
     * @return Ein Optional mit dem gefundenen Kursmaterial oder ein leeres Optional.
     */
    @Query("SELECT km FROM KursMaterial km LEFT JOIN FETCH km.kurs LEFT JOIN FETCH km.kurseinheit WHERE km.id = :id")
    Optional<KursMaterial> findByIdWithKursAndKurseinheit(@Param("id") Long id);
}
