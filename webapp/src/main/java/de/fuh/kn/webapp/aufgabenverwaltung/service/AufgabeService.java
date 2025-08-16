package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.repository.AufgabeRepository;
import de.fuh.kn.webapp.persistence.repository.KurseinheitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service für die Verwaltung von Aufgaben.
 * Diese Klasse bietet Funktionen zum Abrufen, Erstellen, Aktualisieren und Löschen von Aufgaben.
 */
@Service
public class AufgabeService {

    private final AufgabeRepository aufgabeRepository;
    private final KurseinheitRepository kurseinheitRepository;
    private final AufgabeMapper aufgabeMapper;
    private final TeilaufgabeService teilaufgabeService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Repositories, Mapper und Services.
     *
     * @param aufgabeRepository Repository für Aufgabe-Entitäten
     * @param kurseinheitRepository Repository für Kurseinheit-Entitäten
     * @param aufgabeMapper Mapper für die Konvertierung zwischen Aufgabe-Entitäten und DTOs
     * @param teilaufgabeService Service für die Verwaltung von Teilaufgaben
     */
    @Autowired
    public AufgabeService(AufgabeRepository aufgabeRepository, 
                         KurseinheitRepository kurseinheitRepository,
                         AufgabeMapper aufgabeMapper,
                         TeilaufgabeService teilaufgabeService) {
        this.aufgabeRepository = aufgabeRepository;
        this.kurseinheitRepository = kurseinheitRepository;
        this.aufgabeMapper = aufgabeMapper;
        this.teilaufgabeService = teilaufgabeService;
    }

    /**
     * Gibt alle Aufgaben einer Kurseinheit zurück, sortiert nach Reihenfolge.
     *
     * @param kurseinheitId ID der Kurseinheit
     * @return Liste aller Aufgaben der Kurseinheit als DTOs
     */
    @Transactional(readOnly = true)
    public List<AufgabeDto> getAufgabenByKurseinheitId(Long kurseinheitId) {
        Kurseinheit kurseinheit = kurseinheitRepository.findById(kurseinheitId)
                .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " existiert nicht."));
        
        List<Aufgabe> aufgaben = aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit);
        return aufgaben.stream()
                .map(aufgabeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gibt alle Aufgaben eines Kurses zurück, sortiert nach Kurseinheit und Reihenfolge.
     *
     * @param kursId ID des Kurses
     * @return Liste aller Aufgaben des Kurses als DTOs
     */
    @Transactional(readOnly = true)
    public List<AufgabeDto> getAufgabenByKursId(Long kursId) {
        List<Aufgabe> aufgaben = aufgabeRepository.findByKursIdOrderByKurseinheitAndReihenfolge(kursId);
        return aufgaben.stream()
                .map(aufgabeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gibt eine Aufgabe anhand ihrer ID zurück.
     *
     * @param id ID der gesuchten Aufgabe
     * @return DTO der gefundenen Aufgabe oder null, wenn keine Aufgabe mit der ID existiert
     */
    @Transactional(readOnly = true)
    public AufgabeDto getAufgabeById(Long id) {
        return aufgabeRepository.findById(id)
                .map(aufgabeMapper::toDto)
                .orElse(null);
    }

    /**
     * Gibt die Aufgabe zurück, zu der eine Teilaufgabe gehört.
     *
     * @param teilaufgabeId ID der Teilaufgabe
     * @return DTO der Aufgabe, zu der die Teilaufgabe gehört
     * @throws IllegalArgumentException Wenn keine Teilaufgabe mit der angegebenen ID existiert
     *                                  oder diese keiner Aufgabe zugeordnet ist
     */
    @Transactional(readOnly = true)
    public AufgabeDto getAufgabeByTeilaufgabeId(Long teilaufgabeId) {
        Aufgabe aufgabe = aufgabeRepository.findByTeilaufgabenId(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Keine Aufgabe für Teilaufgabe mit ID " + teilaufgabeId + " gefunden."));

        return aufgabeMapper.toDto(aufgabe);
    }

    /**
     * Erstellt eine neue Aufgabe.
     *
     * @param aufgabeDto DTO mit den Daten der neuen Aufgabe
     * @return DTO der erstellten Aufgabe
     */
    /**
     * Erstellt eine neue Aufgabe mit ihren Teilaufgaben.
     * Verwendet eine zweistufige Speicherung, um Referenzprobleme zu vermeiden:
     * 1. Speichert zuerst die Aufgabe ohne Teilaufgaben
     * 2. Speichert dann jede Teilaufgabe mit Referenz auf die gespeicherte Aufgabe
     *
     * @param aufgabeDto DTO mit den Daten der neuen Aufgabe und ihren Teilaufgaben
     * @return DTO der erstellten Aufgabe mit allen Teilaufgaben
     */
    @Transactional
    public AufgabeDto erstelleAufgabe(AufgabeDto aufgabeDto) {
        // Prüfen, ob die Kurseinheit existiert
        Kurseinheit kurseinheit = kurseinheitRepository.findById(aufgabeDto.getKurseinheitId())
                .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + aufgabeDto.getKurseinheitId() + " existiert nicht."));
        
        // Setze die Reihenfolge auf die nächste verfügbare Nummer, falls nicht angegeben
        if (aufgabeDto.getReihenfolge() == null) {
            aufgabeDto.setReihenfolge(getNextReihenfolge(kurseinheit));
        }
        
        // Teilaufgaben-Liste temporär speichern
        List<TeilaufgabeDto> teilaufgaben = new ArrayList<>();
        if (aufgabeDto.getTeilaufgaben() != null) {
            teilaufgaben.addAll(aufgabeDto.getTeilaufgaben());
        }
        
        // DTO ohne Teilaufgaben vorbereiten, um sie separat zu speichern
        aufgabeDto.setTeilaufgaben(new ArrayList<>());
        
        // Aufgabe-Entity erstellen und speichern
        Aufgabe aufgabe = aufgabeMapper.toEntity(aufgabeDto);
        aufgabe.setKurseinheit(kurseinheit);
        Aufgabe gespeicherteAufgabe = aufgabeRepository.save(aufgabe);
        
        // Resultat-DTO erstellen
        AufgabeDto resultDto = aufgabeMapper.toDto(gespeicherteAufgabe);
        
        // Teilaufgaben mit Referenz zur gespeicherten Aufgabe speichern
        if (!teilaufgaben.isEmpty()) {
            List<TeilaufgabeDto> gespeicherteTeilaufgaben = new ArrayList<>();
            
            for (TeilaufgabeDto teilaufgabeDto : teilaufgaben) {
                teilaufgabeDto.setAufgabeId(gespeicherteAufgabe.getId());
                
                // Optional: Reihenfolge setzen, falls nicht vorhanden
                if (teilaufgabeDto.getReihenfolge() == null) {
                    teilaufgabeDto.setReihenfolge(teilaufgaben.indexOf(teilaufgabeDto) + 1);
                }
                
                // Teilaufgabe über den TeilaufgabeService speichern
                TeilaufgabeDto gespeicherteTeilaufgabe = teilaufgabeService.erstelleTeilaufgabe(teilaufgabeDto);
                gespeicherteTeilaufgaben.add(gespeicherteTeilaufgabe);
            }
            
            resultDto.setTeilaufgaben(gespeicherteTeilaufgaben);
        }
        
        return resultDto;
    }


    /**
     * Aktualisiert eine bestehende Aufgabe und ihre Teilaufgaben.
     * Verwendet eine zweistufige Aktualisierung, um Referenzprobleme zu vermeiden:
     * 1. Aktualisiert zuerst die Aufgabe selbst
     * 2. Aktualisiert oder erstellt dann die Teilaufgaben
     *
     * @param aufgabeDto DTO mit den aktualisierten Daten der Aufgabe und ihrer Teilaufgaben
     * @return DTO der aktualisierten Aufgabe mit allen Teilaufgaben
     */
    @Transactional
    public AufgabeDto aktualisiereAufgabe(AufgabeDto aufgabeDto) {
        // Prüfen, ob die Aufgabe existiert
        Aufgabe existierendeAufgabe = aufgabeRepository.findById(aufgabeDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + aufgabeDto.getId() + " existiert nicht."));
        
        // Prüfen, ob die Kurseinheit existiert
        Kurseinheit kurseinheit = kurseinheitRepository.findById(aufgabeDto.getKurseinheitId())
                .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + aufgabeDto.getKurseinheitId() + " existiert nicht."));
        
        // Teilaufgaben-Liste temporär speichern
        List<TeilaufgabeDto> teilaufgaben = new ArrayList<>();
        if (aufgabeDto.getTeilaufgaben() != null) {
            teilaufgaben.addAll(aufgabeDto.getTeilaufgaben());
        }
        
        // Prüfe, ob die Kurseinheit geändert wurde
        if (!existierendeAufgabe.getKurseinheit().getId().equals(aufgabeDto.getKurseinheitId())) {
            existierendeAufgabe.setKurseinheit(kurseinheit);
            // Bei Wechsel der Kurseinheit: setze Reihenfolge auf nächste verfügbare Nummer
            existierendeAufgabe.setReihenfolge(getNextReihenfolge(kurseinheit));
        }
        
        // Aktualisieren der Aufgaben-Daten
        existierendeAufgabe.setTitel(aufgabeDto.getTitel());
        existierendeAufgabe.setAufgabenText(aufgabeDto.getAufgabenText());
        
        // Reihenfolge nur aktualisieren, wenn sie explizit übergeben wurde
        if (aufgabeDto.getReihenfolge() != null) {
            existierendeAufgabe.setReihenfolge(aufgabeDto.getReihenfolge());
        }
        
        // Aufgabe speichern
        Aufgabe aktualisierteAufgabe = aufgabeRepository.save(existierendeAufgabe);
        AufgabeDto resultDto = aufgabeMapper.toDto(aktualisierteAufgabe);
        
        // Teilaufgaben mit Referenz zur aktualisierten Aufgabe speichern/aktualisieren
        if (!teilaufgaben.isEmpty()) {
            List<TeilaufgabeDto> verarbeiteteTeilaufgaben = new ArrayList<>();
            
            for (TeilaufgabeDto teilaufgabeDto : teilaufgaben) {
                teilaufgabeDto.setAufgabeId(aktualisierteAufgabe.getId());
                
                // Optional: Reihenfolge setzen, falls nicht vorhanden
                if (teilaufgabeDto.getReihenfolge() == null) {
                    teilaufgabeDto.setReihenfolge(teilaufgaben.indexOf(teilaufgabeDto) + 1);
                }
                
                // Neue Teilaufgabe erstellen oder bestehende aktualisieren
                TeilaufgabeDto verarbeiteteTeilaufgabe;
                if (teilaufgabeDto.getId() == null) {
                    verarbeiteteTeilaufgabe = teilaufgabeService.erstelleTeilaufgabe(teilaufgabeDto);
                } else {
                    verarbeiteteTeilaufgabe = teilaufgabeService.aktualisiereTeilaufgabe(teilaufgabeDto);
                }
                
                verarbeiteteTeilaufgaben.add(verarbeiteteTeilaufgabe);
            }
            
            resultDto.setTeilaufgaben(verarbeiteteTeilaufgaben);
        }
        
        return resultDto;
    }

    /**
     * Löscht eine Aufgabe anhand ihrer ID.
     *
     * @param id ID der zu löschenden Aufgabe
     */
    @Transactional
    public void loescheAufgabe(Long id) {
        if (!aufgabeRepository.existsById(id)) {
            throw new IllegalArgumentException("Aufgabe mit ID " + id + " existiert nicht.");
        }
        aufgabeRepository.deleteById(id);
    }

    /**
     * Aktualisiert die Reihenfolge mehrerer Aufgaben.
     * Diese Methode wird für das Drag & Drop Reordering verwendet.
     *
     * @param kurseinheitId ID der Kurseinheit, deren Aufgaben neu sortiert werden
     * @param aufgabenIds Liste der Aufgaben-IDs in der neuen Reihenfolge
     * @return Liste der aktualisierten Aufgaben als DTOs
     */
    @Transactional
    public List<AufgabeDto> aktualisiereAufgabenReihenfolge(Long kurseinheitId, List<Long> aufgabenIds) {
        // Prüfen, ob die Kurseinheit existiert
        Kurseinheit kurseinheit = kurseinheitRepository.findById(kurseinheitId)
                .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " existiert nicht."));
        
        // Aufgaben neu sortieren
        for (int i = 0; i < aufgabenIds.size(); i++) {
            Long aufgabeId = aufgabenIds.get(i);
            Aufgabe aufgabe = aufgabeRepository.findById(aufgabeId)
                    .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + aufgabeId + " existiert nicht."));
            
            // Prüfen, ob die Aufgabe zu dieser Kurseinheit gehört
            if (!aufgabe.getKurseinheit().getId().equals(kurseinheitId)) {
                throw new IllegalArgumentException("Aufgabe mit ID " + aufgabeId + " gehört nicht zur Kurseinheit mit ID " + kurseinheitId);
            }
            
            // Reihenfolge aktualisieren (beginnend bei 1)
            aufgabe.setReihenfolge(i + 1);
            aufgabeRepository.save(aufgabe);
        }
        
        // Aktualisierte Aufgaben zurückgeben
        List<Aufgabe> aktualisierteAufgaben = aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit);
        return aktualisierteAufgaben.stream()
                .map(aufgabeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Hilfsmethode zur Bestimmung der nächsten verfügbaren Reihenfolgennummer in einer Kurseinheit.
     *
     * @param kurseinheit Die Kurseinheit, für die die nächste Reihenfolge berechnet werden soll
     * @return Die nächste verfügbare Reihenfolgennummer
     */
    private int getNextReihenfolge(Kurseinheit kurseinheit) {
        List<Aufgabe> aufgaben = aufgabeRepository.findByKurseinheitOrderByReihenfolgeAsc(kurseinheit);
        return aufgaben.isEmpty() ? 1 : aufgaben.get(aufgaben.size() - 1).getReihenfolge() + 1;
    }
}
