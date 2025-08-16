package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialMapper;
import de.fuh.kn.webapp.llm.rag.event.KursMaterialEvent;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.repository.KursMaterialRepository;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.KurseinheitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service für die Verwaltung von Kursmaterialien.
 * Diese Klasse bietet Funktionen zum Hochladen, Abrufen und Löschen von Kursmaterialien.
 */
@Service
public class KursMaterialService {
    
    private final KursMaterialRepository kursMaterialRepository;
    private final KursRepository kursRepository;
    private final KurseinheitRepository kurseinheitRepository;
    private final KursMaterialMapper kursMaterialMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Liste der erlaubten MIME-Typen für Dokumente
     */
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
        "application/pdf", 
        "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain"
    );
    
    /**
     * Liste der erlaubten MIME-Typen für Bilder
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", 
        "image/png", 
        "image/gif", 
        "image/svg+xml"
    );

    /**
     * Konstruktor mit Dependency Injection der benötigten Repositories, Mapper und Event Publisher.
     *
     * @param kursMaterialRepository Repository für KursMaterial-Entitäten
     * @param kursRepository Repository für Kurs-Entitäten
     * @param kurseinheitRepository Repository für Kurseinheit-Entitäten
     * @param kursMaterialMapper Mapper für die Konvertierung zwischen KursMaterial-Entitäten und DTOs
     * @param eventPublisher Publisher für das Veröffentlichen von Events
     */
    @Autowired
    public KursMaterialService(
            KursMaterialRepository kursMaterialRepository,
            KursRepository kursRepository,
            KurseinheitRepository kurseinheitRepository,
            KursMaterialMapper kursMaterialMapper,
            ApplicationEventPublisher eventPublisher) {
        this.kursMaterialRepository = kursMaterialRepository;
        this.kursRepository = kursRepository;
        this.kurseinheitRepository = kurseinheitRepository;
        this.kursMaterialMapper = kursMaterialMapper;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Lädt ein Dokument als Kursmaterial für einen Kurs hoch.
     *
     * @param kursId ID des Kurses
     * @param file Die hochgeladene Datei
     * @return DTO des erstellten Kursmaterials
     * @throws IOException wenn ein Fehler beim Lesen der Datei auftritt
     * @throws IllegalArgumentException wenn der Dateityp nicht erlaubt ist oder der Kurs nicht existiert
     */
    @Transactional
    public KursMaterialDTO kursDateiHochladen(Long kursId, MultipartFile file) throws IOException {
        // Kurs abrufen
        Optional<Kurs> kursOptional = kursRepository.findById(kursId);
        if (kursOptional.isEmpty()) {
            throw new IllegalArgumentException("Kurs mit ID " + kursId + " wurde nicht gefunden.");
        }
        
        // Neues KursMaterial erstellen
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setName(file.getOriginalFilename());
        kursMaterial.setMimeType(file.getContentType());
        kursMaterial.setInhalt(file.getBytes());
        kursMaterial.setKurs(kursOptional.get());
        
        // Typ basierend auf MIME-Type bestimmen
        if (ALLOWED_DOCUMENT_TYPES.contains(file.getContentType())) {
            kursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        } else if (ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            kursMaterial.setTyp(KursMaterial.KursMaterialTyp.BILD);
        } else {
            throw new IllegalArgumentException("Dateityp nicht erlaubt: " + file.getContentType());
        }
        
        // KursMaterial speichern
        KursMaterial gespeichertesKursMaterial = kursMaterialRepository.save(kursMaterial);

        // Event für Dokumentindexierung veröffentlichen, wenn es sich um ein Dokument handelt
        if (gespeichertesKursMaterial.getTyp() == KursMaterial.KursMaterialTyp.DOKUMENT) {
            eventPublisher.publishEvent(new KursMaterialEvent(
                    this,
                    KursMaterialEvent.KursMaterialOperation.CREATE,
                    gespeichertesKursMaterial.getId()));
        }

        return kursMaterialMapper.toDto(gespeichertesKursMaterial);
    }
    
    /**
     * Lädt ein Dokument als Kursmaterial für eine Kurseinheit hoch.
     *
     * @param kurseinheitId ID der Kurseinheit
     * @param file Die hochgeladene Datei
     * @return DTO des erstellten Kursmaterials
     * @throws IOException wenn ein Fehler beim Lesen der Datei auftritt
     * @throws IllegalArgumentException wenn der Dateityp nicht erlaubt ist oder die Kurseinheit nicht existiert
     */
    @Transactional
    public KursMaterialDTO kurseinheitDateiHochladen(Long kurseinheitId, MultipartFile file) throws IOException {
        // Kurseinheit abrufen
        Optional<Kurseinheit> kurseinheitOptional = kurseinheitRepository.findById(kurseinheitId);
        if (kurseinheitOptional.isEmpty()) {
            throw new IllegalArgumentException("Kurseinheit mit ID " + kurseinheitId + " wurde nicht gefunden.");
        }
        
        // Neues KursMaterial erstellen
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setName(file.getOriginalFilename());
        kursMaterial.setMimeType(file.getContentType());
        kursMaterial.setInhalt(file.getBytes());
        kursMaterial.setKurseinheit(kurseinheitOptional.get());
        
        // Typ basierend auf MIME-Type bestimmen
        if (ALLOWED_DOCUMENT_TYPES.contains(file.getContentType())) {
            kursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        } else if (ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            kursMaterial.setTyp(KursMaterial.KursMaterialTyp.BILD);
        } else {
            throw new IllegalArgumentException("Dateityp nicht erlaubt: " + file.getContentType());
        }
        
        // KursMaterial speichern
        KursMaterial gespeichertesKursMaterial = kursMaterialRepository.save(kursMaterial);

        // Event für Dokumentindexierung veröffentlichen, wenn es sich um ein Dokument handelt
        if (gespeichertesKursMaterial.getTyp() == KursMaterial.KursMaterialTyp.DOKUMENT) {
            eventPublisher.publishEvent(new KursMaterialEvent(
                    this,
                    KursMaterialEvent.KursMaterialOperation.CREATE,
                    gespeichertesKursMaterial.getId()));
        }

        return kursMaterialMapper.toDto(gespeichertesKursMaterial);
    }
    
    /**
     * Löscht ein Kursmaterial anhand seiner ID.
     *
     * @param id ID des zu löschenden Kursmaterials
     * @throws IllegalArgumentException wenn das Kursmaterial nicht existiert
     */
    @Transactional
    public void loescheKursMaterial(Long id) {
        Optional<KursMaterial> kursMaterialOptional = kursMaterialRepository.findById(id);
        if (kursMaterialOptional.isEmpty()) {
            throw new IllegalArgumentException("Kursmaterial mit ID " + id + " existiert nicht.");
        }

        // Kursmaterial löschen
        kursMaterialRepository.deleteById(id);
    }
    
    /**
     * Gibt ein Kursmaterial anhand seiner ID zurück.
     *
     * @param id ID des gesuchten Kursmaterials
     * @return DTO des gefundenen Kursmaterials oder null, wenn kein Kursmaterial mit der ID existiert
     */
    @Transactional(readOnly = true)
    public KursMaterialDTO getKursMaterialById(Long id) {
        return kursMaterialRepository.findById(id)
                .map(kursMaterialMapper::toDto)
                .orElse(null);
    }

    /**
     * Findet ein Bild anhand des Namens für einen Kurs.
     *
     * @param kursId ID des Kurses
     * @param bildName Name des gesuchten Bildes
     * @return Das gefundene Bild als DTO oder null, wenn kein passendes Bild gefunden wurde
     */
    @Transactional(readOnly = true)
    public KursMaterialDTO findBildByNameAndKursId(Long kursId, String bildName) {
        Optional<Kurs> kursOptional = kursRepository.findById(kursId);
        if (kursOptional.isEmpty()) {
            return null;
        }
        
        Kurs kurs = kursOptional.get();
        return kursMaterialRepository.findImageByNameAndKurs(bildName, kurs)
                .map(kursMaterialMapper::toDto)
                .orElse(null);
    }
    
    /**
     * Findet ein Bild anhand des Namens für eine Kurseinheit.
     *
     * @param kurseinheitId ID der Kurseinheit
     * @param bildName Name des gesuchten Bildes
     * @return Das gefundene Bild als DTO oder null, wenn kein passendes Bild gefunden wurde
     */
    @Transactional(readOnly = true)
    public KursMaterialDTO findBildByNameAndKurseinheitId(Long kurseinheitId, String bildName) {
        Optional<Kurseinheit> kurseinheitOptional = kurseinheitRepository.findById(kurseinheitId);
        if (kurseinheitOptional.isEmpty()) {
            return null;
        }
        
        Kurseinheit kurseinheit = kurseinheitOptional.get();
        return kursMaterialRepository.findImageByNameAndKurseinheit(bildName, kurseinheit)
                .map(kursMaterialMapper::toDto)
                .orElse(null);
    }
    
    /**
     * Findet alle Bilder (Typ = BILD) einer Kurseinheit.
     *
     * @param kurseinheitId ID der Kurseinheit
     * @return Liste aller Bilder der Kurseinheit
     */
    @Transactional(readOnly = true)
    public List<KursMaterialDTO> findAllBilderByKurseinheitId(Long kurseinheitId) {
        Optional<Kurseinheit> kurseinheitOptional = kurseinheitRepository.findById(kurseinheitId);
        if (kurseinheitOptional.isEmpty()) {
            return List.of(); // Leere Liste zurückgeben, wenn Kurseinheit nicht gefunden
        }
        
        return kursMaterialRepository
                .findByKurseinheitAndTyp(kurseinheitOptional.get(), KursMaterial.KursMaterialTyp.BILD)
                .stream()
                .map(kursMaterialMapper::toDto)
                .toList();
    }
    
    /**
     * Findet alle Bilder (Typ = BILD) eines Kurses.
     *
     * @param kursId ID des Kurses
     * @return Liste aller Bilder des Kurses
     */
    @Transactional(readOnly = true)
    public List<KursMaterialDTO> findAllBilderByKursId(Long kursId) {
        Optional<Kurs> kursOptional = kursRepository.findById(kursId);
        if (kursOptional.isEmpty()) {
            return List.of(); // Leere Liste zurückgeben, wenn Kurs nicht gefunden
        }
        
        return kursMaterialRepository
                .findByKursAndTyp(kursOptional.get(), KursMaterial.KursMaterialTyp.BILD)
                .stream()
                .map(kursMaterialMapper::toDto)
                .toList();
    }
    
    /**
     * Findet alle Bilder einer Kurseinheit und ihres zugehörigen Kurses.
     * Die DTOs haben entweder kurseinheitId oder kursId gesetzt, je nachdem,
     * woher das Bild stammt.
     *
     * @param kurseinheitId ID der Kurseinheit
     * @return Liste aller Bilder, die für die Kurseinheit verfügbar sind (aus der Kurseinheit und dem zugehörigen Kurs)
     */
    @Transactional(readOnly = true)
    public List<KursMaterialDTO> findAllBilderForMarkdownEditor(Long kurseinheitId) {
        // Kurseinheit abrufen
        Optional<Kurseinheit> kurseinheitOptional = kurseinheitRepository.findById(kurseinheitId);
        if (kurseinheitOptional.isEmpty()) {
            return List.of(); // Leere Liste zurückgeben, wenn Kurseinheit nicht gefunden
        }

        Kurseinheit kurseinheit = kurseinheitOptional.get();
        Kurs kurs = kurseinheit.getKurs();

        // Bilder aus der Kurseinheit abrufen
        List<KursMaterialDTO> kurseinheitBilder = findAllBilderByKurseinheitId(kurseinheit.getId());

        // Bilder aus dem Kurs abrufen
        List<KursMaterialDTO> kursBilder = findAllBilderByKursId(kurs.getId());

        // Bilder zusammenführen
        List<KursMaterialDTO> alleBilder = new ArrayList<>(kurseinheitBilder);
        alleBilder.addAll(kursBilder);

        return alleBilder;
    }

    /**
     * Gibt alle Materialien eines Kurses zurück.
     *
     * @param kursId ID des Kurses
     * @return Liste aller Materialien des Kurses als DTOs
     */
    @Transactional(readOnly = true)
    public List<KursMaterialDTO> getMaterialienByKursId(Long kursId) {
        Optional<Kurs> kursOptional = kursRepository.findById(kursId);
        if (kursOptional.isEmpty()) {
            return List.of(); // Leere Liste zurückgeben, wenn Kurs nicht gefunden
        }

        Kurs kurs = kursOptional.get();
        return kursMaterialRepository.findByKurs(kurs)
                .stream()
                .map(kursMaterialMapper::toDto)
                .toList();
    }

    /**
     * Erstellt ein neues Kursmaterial aus einem DTO.
     *
     * @param kursMaterialDTO DTO mit den Daten des neuen Kursmaterials
     * @return DTO des erstellten Kursmaterials
     */
    @Transactional
    public KursMaterialDTO erstelleKursMaterial(KursMaterialDTO kursMaterialDTO) {
        KursMaterial kursMaterial = kursMaterialMapper.toEntity(kursMaterialDTO);

        // Kurs oder Kurseinheit setzen, je nach dem welche ID gesetzt ist
        if (kursMaterialDTO.getKursId() != null) {
            Kurs kurs = kursRepository.findById(kursMaterialDTO.getKursId())
                    .orElseThrow(() -> new IllegalArgumentException("Kurs mit ID " + kursMaterialDTO.getKursId() + " nicht gefunden."));
            kursMaterial.setKurs(kurs);
        } else if (kursMaterialDTO.getKurseinheitId() != null) {
            Kurseinheit kurseinheit = kurseinheitRepository.findById(kursMaterialDTO.getKurseinheitId())
                    .orElseThrow(() -> new IllegalArgumentException("Kurseinheit mit ID " + kursMaterialDTO.getKurseinheitId() + " nicht gefunden."));
            kursMaterial.setKurseinheit(kurseinheit);
        } else {
            throw new IllegalArgumentException("Weder Kurs-ID noch Kurseinheit-ID gesetzt.");
        }

        KursMaterial gespeichertesMaterial = kursMaterialRepository.save(kursMaterial);

        // Event für Dokumentindexierung veröffentlichen, wenn es sich um ein Dokument handelt
        if (gespeichertesMaterial.getTyp() == KursMaterial.KursMaterialTyp.DOKUMENT) {
            eventPublisher.publishEvent(new KursMaterialEvent(
                    this,
                    KursMaterialEvent.KursMaterialOperation.CREATE,
                    gespeichertesMaterial.getId()));
        }

        return kursMaterialMapper.toDto(gespeichertesMaterial);
    }
}
