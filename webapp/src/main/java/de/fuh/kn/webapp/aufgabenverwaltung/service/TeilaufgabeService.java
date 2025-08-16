package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeMapper;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import de.fuh.kn.webapp.persistence.repository.AufgabeRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service für die Verwaltung von Teilaufgaben.
 * Diese Klasse bietet Funktionen zum Abrufen, Erstellen, Aktualisieren und Löschen von Teilaufgaben
 * sowie für die Verwaltung von Musterlösungen.
 */
@Service
public class TeilaufgabeService {

    private final TeilaufgabeRepository teilaufgabeRepository;
    private final AufgabeRepository aufgabeRepository;
    private final TeilaufgabeMapper teilaufgabeMapper;

    /**
     * Konstruktor mit Dependency Injection der benötigten Repositories und Mapper.
     *
     * @param teilaufgabeRepository Repository für Teilaufgabe-Entitäten
     * @param aufgabeRepository Repository für Aufgabe-Entitäten
     * @param teilaufgabeMapper Mapper für die Konvertierung zwischen Teilaufgabe-Entitäten und DTOs
     */
    @Autowired
    public TeilaufgabeService(TeilaufgabeRepository teilaufgabeRepository,
                             AufgabeRepository aufgabeRepository,
                             TeilaufgabeMapper teilaufgabeMapper) {
        this.teilaufgabeRepository = teilaufgabeRepository;
        this.aufgabeRepository = aufgabeRepository;
        this.teilaufgabeMapper = teilaufgabeMapper;
    }

    /**
     * Gibt alle Teilaufgaben einer Aufgabe zurück, sortiert nach Reihenfolge.
     *
     * @param aufgabeId ID der Aufgabe
     * @return Liste aller Teilaufgaben der Aufgabe als DTOs
     */
    @Transactional(readOnly = true)
    public List<TeilaufgabeDto> getTeilaufgabenByAufgabeId(Long aufgabeId) {
        Aufgabe aufgabe = aufgabeRepository.findById(aufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + aufgabeId + " existiert nicht."));
        
        List<Teilaufgabe> teilaufgaben = teilaufgabeRepository.findByAufgabeOrderByReihenfolgeAsc(aufgabe);
        return teilaufgaben.stream()
                .map(teilaufgabeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gibt eine Teilaufgabe anhand ihrer ID zurück.
     *
     * @param id ID der gesuchten Teilaufgabe
     * @return DTO der gefundenen Teilaufgabe oder null, wenn keine Teilaufgabe mit der ID existiert
     */
    @Transactional(readOnly = true)
    public TeilaufgabeDto getTeilaufgabeById(Long id) {
        return teilaufgabeRepository.findById(id)
                .map(teilaufgabeMapper::toDto)
                .orElse(null);
    }

    /**
     * Erstellt eine neue Teilaufgabe.
     *
     * @param teilaufgabeDto DTO mit den Daten der neuen Teilaufgabe
     * @return DTO der erstellten Teilaufgabe
     */
    @Transactional
    public TeilaufgabeDto erstelleTeilaufgabe(TeilaufgabeDto teilaufgabeDto) {
        // Prüfen, ob die Aufgabe existiert
        Aufgabe aufgabe = aufgabeRepository.findById(teilaufgabeDto.getAufgabeId())
                .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + teilaufgabeDto.getAufgabeId() + " existiert nicht."));
        
        // Setze die Reihenfolge auf die nächste verfügbare Nummer, falls nicht angegeben
        if (teilaufgabeDto.getReihenfolge() == null) {
            teilaufgabeDto.setReihenfolge(getNextReihenfolge(aufgabe));
        }
        
        // Teilaufgabe-Entity erstellen
        Teilaufgabe teilaufgabe = teilaufgabeMapper.toEntity(teilaufgabeDto);
        
        // WICHTIG: Aufgabe-Referenz explizit setzen, da diese vom Mapper ignoriert wird
        teilaufgabe.setAufgabe(aufgabe);
        
        // Teilaufgabe speichern
        Teilaufgabe gespeicherteTeilaufgabe = teilaufgabeRepository.save(teilaufgabe);
        
        return teilaufgabeMapper.toDto(gespeicherteTeilaufgabe);
    }

    /**
     * Aktualisiert eine bestehende Teilaufgabe.
     *
     * @param teilaufgabeDto DTO mit den aktualisierten Daten der Teilaufgabe
     * @return DTO der aktualisierten Teilaufgabe
     */
    @Transactional
    public TeilaufgabeDto aktualisiereTeilaufgabe(TeilaufgabeDto teilaufgabeDto) {
        // Prüfen, ob die Teilaufgabe existiert
        Teilaufgabe existierendeTeilaufgabe = teilaufgabeRepository.findById(teilaufgabeDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeDto.getId() + " existiert nicht."));
        
        // Prüfen, ob die Aufgabe existiert
        Aufgabe aufgabe = aufgabeRepository.findById(teilaufgabeDto.getAufgabeId())
                .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + teilaufgabeDto.getAufgabeId() + " existiert nicht."));
        
        // Prüfe, ob die Aufgabe geändert wurde
        if (!existierendeTeilaufgabe.getAufgabe().getId().equals(teilaufgabeDto.getAufgabeId())) {
            existierendeTeilaufgabe.setAufgabe(aufgabe);
            // Bei Wechsel der Aufgabe: setze Reihenfolge auf nächste verfügbare Nummer
            existierendeTeilaufgabe.setReihenfolge(getNextReihenfolge(aufgabe));
        }
        
        // Aktualisieren der Teilaufgaben-Daten
        existierendeTeilaufgabe.setAufgabenstellungMarkdown(teilaufgabeDto.getAufgabenstellungMarkdown());
        existierendeTeilaufgabe.setMusterloesungBewertungshinweise(teilaufgabeDto.getMusterloesungBewertungshinweise());
        
        // Übernehme Musterlösungen, falls vorhanden
        if (teilaufgabeDto.getMusterloesungFelder() != null) {
            existierendeTeilaufgabe.setMusterloesungFelder(new HashMap<>(teilaufgabeDto.getMusterloesungFelder()));
        }
        
        // Reihenfolge nur aktualisieren, wenn sie explizit übergeben wurde
        if (teilaufgabeDto.getReihenfolge() != null) {
            existierendeTeilaufgabe.setReihenfolge(teilaufgabeDto.getReihenfolge());
        }
        
        // Teilaufgabe speichern
        Teilaufgabe aktualisierteTeilaufgabe = teilaufgabeRepository.save(existierendeTeilaufgabe);
        
        return teilaufgabeMapper.toDto(aktualisierteTeilaufgabe);
    }

    /**
     * Löscht eine Teilaufgabe anhand ihrer ID.
     *
     * @param id ID der zu löschenden Teilaufgabe
     */
    @Transactional
    public void loescheTeilaufgabe(Long id) {
        if (!teilaufgabeRepository.existsById(id)) {
            throw new IllegalArgumentException("Teilaufgabe mit ID " + id + " existiert nicht.");
        }
        teilaufgabeRepository.deleteById(id);
    }

    /**
     * Aktualisiert die Reihenfolge mehrerer Teilaufgaben.
     * Diese Methode wird für das Drag & Drop Reordering verwendet.
     *
     * @param aufgabeId ID der Aufgabe, deren Teilaufgaben neu sortiert werden
     * @param teilaufgabenIds Liste der Teilaufgaben-IDs in der neuen Reihenfolge
     * @return Liste der aktualisierten Teilaufgaben als DTOs
     */
    @Transactional
    public List<TeilaufgabeDto> aktualisiereTeilaufgabenReihenfolge(Long aufgabeId, List<Long> teilaufgabenIds) {
        // Prüfen, ob die Aufgabe existiert
        Aufgabe aufgabe = aufgabeRepository.findById(aufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + aufgabeId + " existiert nicht."));
        
        // Teilaufgaben neu sortieren
        for (int i = 0; i < teilaufgabenIds.size(); i++) {
            Long teilaufgabeId = teilaufgabenIds.get(i);
            Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                    .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeId + " existiert nicht."));
            
            // Prüfen, ob die Teilaufgabe zu dieser Aufgabe gehört
            if (!teilaufgabe.getAufgabe().getId().equals(aufgabeId)) {
                throw new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeId + " gehört nicht zur Aufgabe mit ID " + aufgabeId);
            }
            
            // Reihenfolge aktualisieren (beginnend bei 1)
            teilaufgabe.setReihenfolge(i + 1);
            teilaufgabeRepository.save(teilaufgabe);
        }
        
        // Aktualisierte Teilaufgaben zurückgeben
        List<Teilaufgabe> aktualisierteTeilaufgaben = teilaufgabeRepository.findByAufgabeOrderByReihenfolgeAsc(aufgabe);
        return aktualisierteTeilaufgaben.stream()
                .map(teilaufgabeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Aktualisiert die Musterlösung einer Teilaufgabe.
     *
     * @param teilaufgabeId ID der Teilaufgabe
     * @param musterloesungFelder Map mit den Musterlösungen für die Eingabefelder
     * @param bewertungshinweise Bewertungshinweise für die Teilaufgabe
     * @return DTO der aktualisierten Teilaufgabe
     */
    @Transactional
    public TeilaufgabeDto aktualisiereMusterloesung(Long teilaufgabeId, Map<String, String> musterloesungFelder, String bewertungshinweise) {
        // Prüfen, ob die Teilaufgabe existiert
        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeId + " existiert nicht."));
        
        // Musterlösung aktualisieren
        if (musterloesungFelder != null) {
            teilaufgabe.setMusterloesungFelder(new HashMap<>(musterloesungFelder));
        }
        
        teilaufgabe.setMusterloesungBewertungshinweise(bewertungshinweise);
        
        // Teilaufgabe speichern
        Teilaufgabe aktualisierteTeilaufgabe = teilaufgabeRepository.save(teilaufgabe);
        
        return teilaufgabeMapper.toDto(aktualisierteTeilaufgabe);
    }

    /**
     * Gibt die Musterlösung einer Teilaufgabe zurück.
     *
     * @param teilaufgabeId ID der Teilaufgabe
     * @return Map mit den Musterlösungen für die Eingabefelder oder leere Map, wenn keine Musterlösung existiert
     */
    @Transactional(readOnly = true)
    public Map<String, String> getMusterloesungFelder(Long teilaufgabeId) {
        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeId + " existiert nicht."));
        
        return new HashMap<>(teilaufgabe.getMusterloesungFelder());
    }

    /**
     * Gibt die Bewertungshinweise einer Teilaufgabe zurück.
     *
     * @param teilaufgabeId ID der Teilaufgabe
     * @return Bewertungshinweise für die Teilaufgabe oder null, wenn keine Bewertungshinweise existieren
     */
    @Transactional(readOnly = true)
    public String getMusterloesungBewertungshinweise(Long teilaufgabeId) {
        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeId + " existiert nicht."));
        
        return teilaufgabe.getMusterloesungBewertungshinweise();
    }

    /**
     * Fügt ein neues Feld zur Musterlösung einer Teilaufgabe hinzu.
     *
     * @param teilaufgabeId ID der Teilaufgabe
     * @param feldName Name des Feldes
     * @param feldWert Musterlösung für das Feld
     * @return DTO der aktualisierten Teilaufgabe
     */
    @Transactional
    public TeilaufgabeDto fuegeMusterloesungFeldHinzu(Long teilaufgabeId, String feldName, String feldWert) {
        // Prüfen, ob die Teilaufgabe existiert
        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeId + " existiert nicht."));
        
        // Feld hinzufügen oder aktualisieren
        Map<String, String> musterloesungFelder = new HashMap<>(teilaufgabe.getMusterloesungFelder());
        musterloesungFelder.put(feldName, feldWert);
        teilaufgabe.setMusterloesungFelder(musterloesungFelder);
        
        // Teilaufgabe speichern
        Teilaufgabe aktualisierteTeilaufgabe = teilaufgabeRepository.save(teilaufgabe);
        
        return teilaufgabeMapper.toDto(aktualisierteTeilaufgabe);
    }

    /**
     * Entfernt ein Feld aus der Musterlösung einer Teilaufgabe.
     *
     * @param teilaufgabeId ID der Teilaufgabe
     * @param feldName Name des Feldes
     * @return DTO der aktualisierten Teilaufgabe
     */
    @Transactional
    public TeilaufgabeDto entferneMusterloesungFeld(Long teilaufgabeId, String feldName) {
        // Prüfen, ob die Teilaufgabe existiert
        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe mit ID " + teilaufgabeId + " existiert nicht."));
        
        // Feld entfernen, falls vorhanden
        Map<String, String> musterloesungFelder = new HashMap<>(teilaufgabe.getMusterloesungFelder());
        musterloesungFelder.remove(feldName);
        teilaufgabe.setMusterloesungFelder(musterloesungFelder);
        
        // Teilaufgabe speichern
        Teilaufgabe aktualisierteTeilaufgabe = teilaufgabeRepository.save(teilaufgabe);
        
        return teilaufgabeMapper.toDto(aktualisierteTeilaufgabe);
    }

    /**
     * Hilfsmethode zur Bestimmung der nächsten verfügbaren Reihenfolgennummer in einer Aufgabe.
     *
     * @param aufgabe Die Aufgabe, für die die nächste Reihenfolge berechnet werden soll
     * @return Die nächste verfügbare Reihenfolgennummer
     */
    private int getNextReihenfolge(Aufgabe aufgabe) {
        List<Teilaufgabe> teilaufgaben = teilaufgabeRepository.findByAufgabeOrderByReihenfolgeAsc(aufgabe);
        return teilaufgaben.isEmpty() ? 1 : teilaufgaben.get(teilaufgaben.size() - 1).getReihenfolge() + 1;
    }
}
