package de.fuh.kn.webapp.common.aktivitaeten;

import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.persistence.entity.Aktivitaet;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import de.fuh.kn.webapp.persistence.entity.Nutzer;
import de.fuh.kn.webapp.persistence.repository.AktivitaetRepository;
import de.fuh.kn.webapp.persistence.repository.NutzerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service für die Verwaltung und Protokollierung von Benutzeraktivitäten.
 * Stellt Methoden zum Erfassen, Abfragen und Auswerten von Aktivitäten bereit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitaetsService {

    private final AktivitaetRepository aktivitaetRepository;
    private final AktivitaetMapper aktivitaetMapper;
    private final NutzerRepository nutzerRepository;
    private final BelegungService belegungService;
    private final TeilaufgabeService teilaufgabeService;


    /**
     * Findet alle Aktivitäten eines bestimmten Nutzers paginiert.
     * Sucht sowohl nach Aktivitäten, bei denen der Nutzer in der Nutzer-Spalte steht,
     * als auch nach Aktivitäten, bei denen der Nutzer als Referenz (refType und refId) angegeben ist.
     *
     * @param nutzerDTO Der Nutzer, dessen Aktivitäten abgefragt werden sollen
     * @param page Seitenzahl (0-basiert)
     * @param size Anzahl der Einträge pro Seite
     * @return Eine Page mit Aktivitäten des Nutzers
     */
    @Transactional(readOnly = true)
    public Page<AktivitaetDTO> findeAktivitaetenFuerNutzerPaged(NutzerDTO nutzerDTO, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("zeitpunkt").descending());

        // Spezifikation erstellen, um sowohl nach Nutzer als auch nach Referenzen zu suchen
        Specification<Aktivitaet> spec = Specification.where(null);
        
        // Aktivitäten finden, bei denen der Nutzer direkt zugeordnet ist
        Specification<Aktivitaet> byNutzerId = (root, query, cb) -> cb.equal(root.get("nutzer").get("id"), nutzerDTO.getId());
        
        // Aktivitäten finden, bei denen der Nutzer als Referenz angegeben ist
        Specification<Aktivitaet> byReferenz = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("referenzTyp"), nutzerDTO.getEntityTypeName()),
                cb.equal(root.get("referenzId"), nutzerDTO.getId())
            );
        
        // Beide Spezifikationen mit ODER verknüpfen
        spec = byNutzerId.or(byReferenz);
        
        // Abfrage ausführen
        Page<Aktivitaet> pageResponse = aktivitaetRepository.findAll(spec, pageable);
        
        return pageResponse.map(aktivitaetMapper::toDTO).map(this::enrichtActivityWithBelegungInfo);
    }

    /**
     * Findet alle Aktivitäten paginiert.
     * 
     * @param page Seitenzahl (0-basiert)
     * @param size Anzahl der Einträge pro Seite
     * @return Eine Page mit allen Aktivitäten
     */
    @Transactional(readOnly = true)
    public Page<AktivitaetDTO> getAlleAktivitaeten(int page, int size) {
        return getFilteredAktivitaeten(null, null, page, size, null);
    }
    
    /**
     * Filtert Aktivitäten nach verschiedenen Kriterien.
     * 
     * @param aktivitaetsTyp Der Typ der Aktivitäten (optional)
     * @param suchbegriff Suchbegriff für Beschreibung (optional)
     * @param page Seitenzahl (0-basiert)
     * @param size Anzahl der Einträge pro Seite
     * @param dateRangeSpec Spezifikation für den Datumsbereich (optional)
     * @return Eine gefilterte Page mit Aktivitäten
     */
    @Transactional(readOnly = true)
    public Page<AktivitaetDTO> getFilteredAktivitaeten(
            AktivitaetsTyp aktivitaetsTyp,
            String suchbegriff,
            int page,
            int size,
            Specification<Aktivitaet> dateRangeSpec) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("zeitpunkt").descending());
        
        // Spezifikation für dynamische Abfragen erstellen
        Specification<Aktivitaet> spec = Specification.where(null);

        // Gemeinsame Filter anwenden (Suchbegriff)
        spec = applyCommonFilters(spec, suchbegriff, aktivitaetsTyp, dateRangeSpec);
        
        // Abfrage ausführen
        Page<Aktivitaet> aktivitaeten = aktivitaetRepository.findAll(spec, pageable);
        return aktivitaeten.map(aktivitaetMapper::toDTO).map(this::enrichtActivityWithBelegungInfo);
    }
    
    /**
     * Filtert Aktivitäten eines bestimmten Nutzers nach verschiedenen Kriterien.
     * Berücksichtigt sowohl Aktivitäten, bei denen der Nutzer in der Nutzer-Spalte steht,
     * als auch Aktivitäten, bei denen der Nutzer als Referenz (refType und refId) angegeben ist.
     * 
     * @param nutzerId Die ID des Nutzers
     * @param aktivitaetsTyp Der Typ der Aktivitäten (optional)
     * @param suchbegriff Suchbegriff für Beschreibung (optional)
     * @param page Seitenzahl (0-basiert)
     * @param size Anzahl der Einträge pro Seite
     * @param dateRangeSpec Spezifikation für den Datumsbereich (optional)
     * @return Eine gefilterte Page mit Aktivitäten des Nutzers
     */
    @Transactional(readOnly = true)
    public Page<AktivitaetDTO> getFilteredAktivitaetenByNutzerId(
            Long nutzerId,
            AktivitaetsTyp aktivitaetsTyp,
            String suchbegriff,
            int page,
            int size,
            Specification<Aktivitaet> dateRangeSpec) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("zeitpunkt").descending());
        
        // Basisabfrage für Aktivitäten, bei denen der Nutzer direkt beteiligt ist
        Specification<Aktivitaet> byNutzerId = (root, query, cb) -> cb.equal(root.get("nutzer").get("id"), nutzerId);
        
        // Hole den EntityTypeName für den Nutzer
        Optional<Nutzer> nutzerOptional = nutzerRepository.findById(nutzerId);
        if (nutzerOptional.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        Nutzer nutzer = nutzerOptional.get();
        String entityTypeName = nutzer.getClass().getSimpleName();
        
        // Basisabfrage für Aktivitäten, bei denen der Nutzer als Referenz angegeben ist
        Specification<Aktivitaet> byReferenz = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("referenzTyp"), entityTypeName),
                cb.equal(root.get("referenzId"), nutzerId)
            );
        
        // Kombiniere beide Abfragen mit OR
        Specification<Aktivitaet> spec = byNutzerId.or(byReferenz);
        

        
        // Gemeinsame Filter anwenden (Suchbegriff)
        spec = applyCommonFilters(spec, suchbegriff, aktivitaetsTyp, dateRangeSpec);
        
        // Abfrage ausführen
        Page<Aktivitaet> aktivitaeten = aktivitaetRepository.findAll(spec, pageable);
        return aktivitaeten.map(aktivitaetMapper::toDTO).map(this::enrichtActivityWithBelegungInfo);
    }

    /**
     * Wendet gemeinsame Filter (Suchbegriff) auf eine Spezifikation an.
     *
     * @param spec           Die bestehende Spezifikation
     * @param suchbegriff    Suchbegriff für Beschreibung oder Nutzernamen (optional)
     * @param aktivitaetsTyp Filter nach Aktivitätstyp (optional)
     * @param dateRangeSpec  Filter nach Datum (optional)
     * @return Die erweiterte Spezifikation
     */
    private Specification<Aktivitaet> applyCommonFilters(
            Specification<Aktivitaet> spec,
            String suchbegriff,
            AktivitaetsTyp aktivitaetsTyp,
            Specification<Aktivitaet> dateRangeSpec) {

        // Nach Suchbegriff in der Beschreibung oder im Nutzernamen filtern
        if (StringUtils.hasText(suchbegriff)) {
            // Nutzer finden, deren Namen den Suchbegriff enthalten (auch vollständige Namen)
            List<Nutzer> matchendeNutzer = nutzerRepository.findByNameContaining(suchbegriff);
            
            // Spezifikation, um in Beschreibung zu suchen
            Specification<Aktivitaet> beschreibungSpec = (root, query, cb) ->
                cb.like(cb.lower(root.get("beschreibung")), "%" + suchbegriff.toLowerCase() + "%");
                
            // Spezifikation, um nach Nutzern zu suchen
            Specification<Aktivitaet> nutzerSpec = null;
            if (!matchendeNutzer.isEmpty()) {
                // Suche nach Aktivitäten, bei denen die gefundenen Nutzer direkt beteiligt sind
                Specification<Aktivitaet> nutzerDirektSpec = (root, query, cb) -> root.get("nutzer").in(matchendeNutzer);
                
                // Suche nach Aktivitäten, bei denen die gefundenen Nutzer als Referenz angegeben sind
                Specification<Aktivitaet> nutzerReferenzSpec = null;
                for (Nutzer nutzer : matchendeNutzer) {
                    String entityTypeName = nutzer.getClass().getSimpleName();
                    Specification<Aktivitaet> einzelnerNutzerReferenzSpec = (root, query, cb) -> 
                        cb.and(
                            cb.equal(root.get("referenzTyp"), entityTypeName),
                            cb.equal(root.get("referenzId"), nutzer.getId())
                        );
                    
                    if (nutzerReferenzSpec == null) {
                        nutzerReferenzSpec = einzelnerNutzerReferenzSpec;
                    } else {
                        nutzerReferenzSpec = nutzerReferenzSpec.or(einzelnerNutzerReferenzSpec);
                    }
                }
                
                // Kombiniere direkte Nutzer-Suche und Referenz-Suche mit ODER
                nutzerSpec = nutzerDirektSpec;
                nutzerSpec = nutzerSpec.or(nutzerReferenzSpec);
            }
            
            // Spezifikationen mit ODER verknüpfen, wenn Nutzer gefunden wurden
            if (nutzerSpec != null) {
                spec = spec.and(beschreibungSpec.or(nutzerSpec));
            } else {
                spec = spec.and(beschreibungSpec);
            }

        }

        // Nach Aktivitätstyp filtern
        if (aktivitaetsTyp != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("aktivitaetsTyp"), aktivitaetsTyp));
        }

        // Nach Datumsbereich filtern, wenn angegeben
        if (dateRangeSpec != null) {
            spec = spec.and(dateRangeSpec);
        }
        
        return spec;
    }

    private AktivitaetDTO enrichtActivityWithBelegungInfo(AktivitaetDTO aktivitaet) {
        Optional<BelegungDTO> belegungDTOOptional = Optional.empty();

        if(aktivitaet.getReferenzTyp() != null && aktivitaet.getReferenzTyp().equals("Teilaufgabe")){

            Long studentId = aktivitaet.getNutzerId();
            Long teilaufgabeId = aktivitaet.getReferenzId();
            belegungDTOOptional = belegungService.findBelegungByStudentAndTeilaufgabe(studentId, teilaufgabeId);
            
            // Get parent Aufgabe ID for URL generation
            var teilaufgabeDto = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);
            if (teilaufgabeDto != null) {
                aktivitaet.setParentAufgabeId(teilaufgabeDto.getAufgabeId());
            }
        }

        //Belegung in Aktivität übernehmen
        belegungDTOOptional.ifPresent(belegungDTO -> aktivitaet.setBelegungId(belegungDTO.getId()));

        return aktivitaet;
    }


    /**
     * Löscht alle Aktivitätseinträge, die älter als X Monate sind
     * @param monate Anzahl der Monate
     * @return Anzahl der gelöschten Einträge
     */
    public long loescheAktivitaetenAelterAls(int monate){
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(monate);
        return aktivitaetRepository.deleteByZeitpunktBefore(cutoffDate);
    }

}
