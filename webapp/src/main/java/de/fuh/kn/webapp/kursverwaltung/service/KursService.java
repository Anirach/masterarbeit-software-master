package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMapper;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service für die Verwaltung von Kursen.
 * Diese Klasse bietet Funktionen zum Abrufen, Erstellen, Aktualisieren und Löschen von Kursen
 * sowie für den Zugriff auf zugehörige Kurseinheiten und Materialien.
 */
@Service
public class KursService {

    private final KursRepository kursRepository;
    private final KursMapper kursMapper;

    /**
     * Konstruktor mit Dependency Injection der benötigten Repositories und Mapper.
     *
     * @param kursRepository Repository für Kurs-Entitäten
     * @param kursMapper Mapper für die Konvertierung zwischen Kurs-Entitäten und DTOs
     */
    @Autowired
    public KursService(KursRepository kursRepository, KursMapper kursMapper) {
        this.kursRepository = kursRepository;
        this.kursMapper = kursMapper;
    }

    /**
     * Gibt alle verfügbaren Kurse zurück.
     *
     * @return Liste aller Kurse als DTOs
     */
    @Transactional(readOnly = true)
    public List<KursDTO> getAlleKurse() {
        List<Kurs> kurse = kursRepository.findAll();
        return kurse.stream()
                .map(kursMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gibt einen Kurs anhand seiner ID zurück.
     *
     * @param id ID des gesuchten Kurses
     * @return DTO des gefundenen Kurses oder null, wenn kein Kurs mit der ID existiert
     */
    @Transactional(readOnly = true)
    public KursDTO getKursById(Long id) {
        return kursRepository.findById(id)
                .map(kursMapper::toDto)
                .orElse(null);
    }

    /**
     * Gibt einen Kurs anhand seiner ID zurück, inklusive aller zugehörigen Kurseinheiten 
     * und der Anzahl der Belegungen.
     *
     * @param id ID des gesuchten Kurses
     * @return DTO des gefundenen Kurses inklusive Kurseinheiten und Belegungszahlen oder null, 
     *         wenn kein Kurs mit der ID existiert
     */
    @Transactional(readOnly = true)
    public KursDTO getKursByIdMitKurseinheiten(Long id) {
        return kursRepository.findById(id)
                .map(kurs -> {
                    KursDTO kursDTO = kursMapper.toDto(kurs);
                    
                    // Gesamtanzahl der Belegungen setzen
                    int anzahlBelegungen = kurs.getBelegungen() != null ? kurs.getBelegungen().size() : 0;
                    kursDTO.setAnzahlBelegungen(anzahlBelegungen);
                    
                    // Anzahl der aktiven Belegungen berechnen
                    java.time.LocalDate heute = java.time.LocalDate.now();
                    long anzahlAktive = kurs.getBelegungen() != null ? 
                        kurs.getBelegungen().stream()
                            .filter(b -> !heute.isBefore(b.getStartDatum()) && 
                                         (b.getEndDatum() == null || !heute.isAfter(b.getEndDatum())))
                            .count() : 0;
                    kursDTO.setAnzahlAktiveBelegungen((int) anzahlAktive);
                    
                    return kursDTO;
                })
                .orElse(null);
    }

    /**
     * Erstellt einen neuen Kurs.
     *
     * @param kursDTO DTO mit den Daten des neuen Kurses
     * @return DTO des erstellten Kurses
     */
    @Transactional
    public KursDTO erstelleKurs(KursDTO kursDTO) {
        Kurs kurs = kursMapper.toEntity(kursDTO);
        Kurs gespeicherterKurs = kursRepository.save(kurs);
        return kursMapper.toDto(gespeicherterKurs);
    }

    /**
     * Aktualisiert einen bestehenden Kurs.
     *
     * @param kursDTO DTO mit den aktualisierten Daten des Kurses
     * @return DTO des aktualisierten Kurses
     */
    @Transactional
    public KursDTO aktualisiereKurs(KursDTO kursDTO) {
        // Prüfen, ob der Kurs existiert
        if (!kursRepository.existsById(kursDTO.getId())) {
            throw new IllegalArgumentException("Kurs mit ID " + kursDTO.getId() + " existiert nicht.");
        }
        
        // Aktuellen Kurs aus der Datenbank laden, um sicherzustellen, dass keine Daten verloren gehen
        Kurs existierenderKurs = kursRepository.findById(kursDTO.getId()).orElseThrow();
        
        // Aktualisieren der Kurs-Daten
        existierenderKurs.setName(kursDTO.getName());
        
        // Kurs speichern
        Kurs aktualisierterKurs = kursRepository.save(existierenderKurs);
        
        return kursMapper.toDto(aktualisierterKurs);
    }

    /**
     * Löscht einen Kurs anhand seiner ID.
     *
     * @param id ID des zu löschenden Kurses
     */
    @Transactional
    public void loescheKurs(Long id) {
        if (!kursRepository.existsById(id)) {
            throw new IllegalArgumentException("Kurs mit ID " + id + " existiert nicht.");
        }
        kursRepository.deleteById(id);
    }
}
