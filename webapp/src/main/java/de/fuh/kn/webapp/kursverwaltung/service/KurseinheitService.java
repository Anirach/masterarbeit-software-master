package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialMapper;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitMapper;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.repository.KursMaterialRepository;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.KurseinheitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service für die Verwaltung von Kurseinheiten.
 * Diese Klasse bietet Funktionen zum Abrufen, Erstellen, Aktualisieren und Löschen von Kurseinheiten.
 */
@Service
public class KurseinheitService {

    private final KurseinheitRepository kurseinheitRepository;
    private final KursRepository kursRepository;
    private final KursMaterialRepository kursMaterialRepository;
    private final KurseinheitMapper kurseinheitMapper;
    private final KursMaterialMapper kursMaterialMapper;

    /**
     * Konstruktor mit Dependency Injection der benötigten Repositories und Mapper.
     *
     * @param kurseinheitRepository Repository für Kurseinheit-Entitäten
     * @param kursRepository Repository für Kurs-Entitäten
     * @param kursMaterialRepository Repository für KursMaterial-Entitäten
     * @param kurseinheitMapper Mapper für die Konvertierung zwischen Kurseinheit-Entitäten und DTOs
     * @param kursMaterialMapper Mapper für die Konvertierung zwischen KursMaterial-Entitäten und DTOs
     */
    @Autowired
    public KurseinheitService(KurseinheitRepository kurseinheitRepository, 
                             KursRepository kursRepository,
                             KursMaterialRepository kursMaterialRepository,
                             KurseinheitMapper kurseinheitMapper,
                             KursMaterialMapper kursMaterialMapper) {
        this.kurseinheitRepository = kurseinheitRepository;
        this.kursRepository = kursRepository;
        this.kursMaterialRepository = kursMaterialRepository;
        this.kurseinheitMapper = kurseinheitMapper;
        this.kursMaterialMapper = kursMaterialMapper;
    }

    /**
     * Gibt alle Kurseinheiten eines Kurses zurück, sortiert nach Reihenfolge.
     *
     * @param kursId ID des Kurses
     * @return Liste aller Kurseinheiten des Kurses als DTOs
     */
    @Transactional(readOnly = true)
    public List<KurseinheitDTO> getKurseinheitenByKursId(Long kursId) {
        Kurs kurs = kursRepository.findById(kursId)
                .orElseThrow(() -> new IllegalArgumentException("Kurs mit ID " + kursId + " existiert nicht."));
        
        List<Kurseinheit> kurseinheiten = kurseinheitRepository.findByKursOrderByReihenfolgeAsc(kurs);
        return kurseinheiten.stream()
                .map(kurseinheitMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gibt eine Kurseinheit anhand ihrer ID zurück.
     *
     * @param id ID der gesuchten Kurseinheit
     * @return DTO der gefundenen Kurseinheit oder null, wenn keine Kurseinheit mit der ID existiert
     */
    @Transactional(readOnly = true)
    public KurseinheitDTO getKurseinheitById(Long id) {
        return kurseinheitRepository.findById(id)
                .map(kurseinheitMapper::toDto)
                .orElse(null);
    }

    /**
     * Erstellt eine neue Kurseinheit.
     *
     * @param kurseinheitDTO DTO mit den Daten der neuen Kurseinheit
     * @return DTO der erstellten Kurseinheit
     */
    @Transactional
    public KurseinheitDTO erstelleKurseinheit(KurseinheitDTO kurseinheitDTO) {
        // Prüfen, ob der Kurs existiert
        Kurs kurs = kursRepository.findById(kurseinheitDTO.getKursId())
                .orElseThrow(() -> new IllegalArgumentException("Kurs mit ID " + kurseinheitDTO.getKursId() + " existiert nicht."));
        
        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = kurseinheitMapper.toEntity(kurseinheitDTO);
        kurseinheit.setKurs(kurs);
        
        // Kurseinheit speichern
        Kurseinheit gespeicherteKurseinheit = kurseinheitRepository.save(kurseinheit);
        
        return kurseinheitMapper.toDto(gespeicherteKurseinheit);
    }

    /**
     * Aktualisiert eine bestehende Kurseinheit.
     *
     * @param kurseinheitDTO DTO mit den aktualisierten Daten der Kurseinheit
     * @return DTO der aktualisierten Kurseinheit
     */
    @Transactional
    public KurseinheitDTO aktualisiereKurseinheit(KurseinheitDTO kurseinheitDTO) {
        // Prüfen, ob die Kurseinheit existiert
        Kurseinheit existierendeKurseinheit = kurseinheitRepository.findById(kurseinheitDTO.getId())
                .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitDTO.getId() + " existiert nicht."));
        
        // Prüfen, ob der Kurs existiert
        Kurs kurs = kursRepository.findById(kurseinheitDTO.getKursId())
                .orElseThrow(() -> new IllegalArgumentException("Kurs mit ID " + kurseinheitDTO.getKursId() + " existiert nicht."));
        
        // Aktualisieren der Kurseinheit-Daten
        existierendeKurseinheit.setName(kurseinheitDTO.getName());
        existierendeKurseinheit.setReihenfolge(kurseinheitDTO.getReihenfolge());
        existierendeKurseinheit.setKurs(kurs);
        
        // Kurseinheit speichern
        Kurseinheit aktualisierteKurseinheit = kurseinheitRepository.save(existierendeKurseinheit);
        
        return kurseinheitMapper.toDto(aktualisierteKurseinheit);
    }

    /**
     * Löscht eine Kurseinheit anhand ihrer ID.
     *
     * @param id ID der zu löschenden Kurseinheit
     */
    @Transactional
    public void loescheKurseinheit(Long id) {
        if (!kurseinheitRepository.existsById(id)) {
            throw new IllegalArgumentException("Kurseinheit mit ID " + id + " existiert nicht.");
        }
        kurseinheitRepository.deleteById(id);
    }
    
    /**
     * Gibt den Namen des Kurses zurück, zu dem die Kurseinheit gehört.
     *
     * @param kurseinheitId ID der Kurseinheit
     * @return Name des Kurses
     */
    @Transactional(readOnly = true)
    public String getKursNameByKurseinheitId(Long kurseinheitId) {
        Kurseinheit kurseinheit = kurseinheitRepository.findById(kurseinheitId)
                .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " existiert nicht."));
        
        return kurseinheit.getKurs().getName();
    }
    
    /**
     * Gibt alle Kursmaterialien einer Kurseinheit zurück.
     *
     * @param kurseinheitId ID der Kurseinheit
     * @return Liste aller Kursmaterialien der Kurseinheit als DTOs
     */
    @Transactional(readOnly = true)
    public List<KursMaterialDTO> getKursMaterialienByKurseinheitId(Long kurseinheitId) {
        Kurseinheit kurseinheit = kurseinheitRepository.findById(kurseinheitId)
                .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " existiert nicht."));
        
        List<KursMaterial> materialien = kursMaterialRepository.findByKurseinheit(kurseinheit);
        
        if (materialien == null || materialien.isEmpty()) {
            return Collections.emptyList();
        }
        
        return materialien.stream()
                .map(kursMaterialMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Aktualisiert die Reihenfolge mehrerer Kurseinheiten.
     * Diese Methode wird für das Drag & Drop Reordering verwendet.
     *
     * @param kursId ID des Kurses, dessen Kurseinheiten neu sortiert werden
     * @param kurseinheitIds Liste der Kurseinheit-IDs in der neuen Reihenfolge
     * @return Liste der aktualisierten Kurseinheiten als DTOs
     */
    @Transactional
    public List<KurseinheitDTO> aktualisiereKurseinheitenReihenfolge(Long kursId, List<Long> kurseinheitIds) {
        // Prüfen, ob der Kurs existiert
        Kurs kurs = kursRepository.findById(kursId)
                .orElseThrow(() -> new IllegalArgumentException("Kurs mit ID " + kursId + " existiert nicht."));
        
        // Kurseinheiten neu sortieren
        for (int i = 0; i < kurseinheitIds.size(); i++) {
            Long kurseinheitId = kurseinheitIds.get(i);
            Kurseinheit kurseinheit = kurseinheitRepository.findById(kurseinheitId)
                    .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " existiert nicht."));
            
            // Prüfen, ob die Kurseinheit zu diesem Kurs gehört
            if (!kurseinheit.getKurs().getId().equals(kursId)) {
                throw new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " gehört nicht zum Kurs mit ID " + kursId);
            }
            
            // Reihenfolge aktualisieren (beginnend bei 1)
            kurseinheit.setReihenfolge(i + 1);
            kurseinheitRepository.save(kurseinheit);
        }
        
        // Aktualisierte Kurseinheiten zurückgeben
        List<Kurseinheit> aktualisierteKurseinheiten = kurseinheitRepository.findByKursOrderByReihenfolgeAsc(kurs);
        return aktualisierteKurseinheiten.stream()
                .map(kurseinheitMapper::toDto)
                .collect(Collectors.toList());
    }
}
