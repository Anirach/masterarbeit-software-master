package de.fuh.kn.webapp.nutzerverwaltung.belegung;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.*;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import de.fuh.kn.webapp.persistence.entity.Belegung;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.BelegungRepository;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import de.fuh.kn.webapp.uebung.dto.KursFortschrittDTO;
import de.fuh.kn.webapp.uebung.service.FortschrittService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Service für die Verwaltung von Kursbelegungen.
 * Ermöglicht das Hinzufügen, Entfernen und Abfragen von Belegungen.
 */
@Service
@Slf4j
public class BelegungService {

    private final BelegungRepository belegungRepository;
    private final StudentRepository studentRepository;
    private final KursRepository kursRepository;
    private final BelegungMapper belegungMapper;
    private final NutzerService nutzerService;
    private final FortschrittService fortschrittService;
    private final StudentMapper studentMapper;
    private final KursMapper kursMapper;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    /**
     * Konstruktor für die Dependency Injection.
     *
     * @param belegungRepository Repository für den Zugriff auf Belegungen
     * @param studentRepository Repository für den Zugriff auf Studenten
     * @param kursRepository Repository für den Zugriff auf Kurse
     * @param belegungMapper Mapper für die Konvertierung zwischen Entity und DTO
     */
    @Autowired
    public BelegungService(BelegungRepository belegungRepository,
                           StudentRepository studentRepository,
                           KursRepository kursRepository,
                           BelegungMapper belegungMapper,
                           NutzerService nutzerService,
                           FortschrittService fortschrittService,
                           StudentMapper studentMapper,
                           KursMapper kursMapper,
                           TeilaufgabeRepository teilaufgabeRepository, AktivitaetsProtokollierungService aktivitaetsProtokollierungService) {
        this.belegungRepository = belegungRepository;
        this.studentRepository = studentRepository;
        this.kursRepository = kursRepository;
        this.belegungMapper = belegungMapper;
        this.nutzerService = nutzerService;
        this.fortschrittService = fortschrittService;
        this.studentMapper = studentMapper;
        this.kursMapper = kursMapper;
        this.teilaufgabeRepository = teilaufgabeRepository;
        this.aktivitaetsProtokollierungService = aktivitaetsProtokollierungService;
    }
    
    /**
     * Fügt einen Studenten zu einem Kurs hinzu (erstellt eine neue Belegung).
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param studentDTO DTO des Studenten
     * @param kursDTO DTO des Kurses
     * @param startDatum Startdatum der Belegung
     * @param endDatum Enddatum der Belegung (kann null sein für unbegrenzte Belegung)
     * @return Die erstellte Belegung als DTO
     * @throws NoSuchElementException wenn Student oder Kurs nicht gefunden wird
     * @throws IllegalStateException wenn der Student den Kurs bereits belegt
     */
    @Transactional
    public BelegungDTO addStudentToKurs(StudentDTO studentDTO, KursDTO kursDTO, LocalDate startDatum, LocalDate endDatum) {

        Kurs kurs = kursRepository.findById(kursDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursDTO.getId() + " nicht gefunden"));
        
        return belegungErstellen(studentDTO.getMatrikelnummer(), kurs, startDatum, endDatum);
    }
    
    /**
     * Entfernt einen Studenten aus einem Kurs (löscht die Belegung).
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param studentDTO DTO des Studenten
     * @param kursDTO DTO des Kurses
     * @throws NoSuchElementException wenn Student, Kurs oder die Belegung nicht gefunden wird
     */
    @Transactional
    public void removeStudentFromKurs(StudentDTO studentDTO, KursDTO kursDTO) {
        Student student = studentRepository.findById(studentDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Student mit ID " + studentDTO.getId() + " nicht gefunden"));
        
        Kurs kurs = kursRepository.findById(kursDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursDTO.getId() + " nicht gefunden"));
        
        Belegung belegung = belegungRepository.findByStudentAndKurs(student, kurs)
                .orElseThrow(() -> new NoSuchElementException("Belegung nicht gefunden"));
        
        belegungRepository.delete(belegung);
        protokolliereBelegungGeloescht(belegung);
    }
    
    /**
     * Entfernt einen Studenten aus einem Kurs basierend auf einer Belegung DTO.
     * Diese Methode bietet eine effizientere Möglichkeit, eine Belegung zu löschen,
     * wenn bereits ein BelegungDTO vorhanden ist.
     *
     * @param belegungDTO DTO der zu löschenden Belegung
     * @throws NoSuchElementException wenn die Belegung nicht gefunden wird
     */
    @Transactional
    public void removeBelegung(BelegungDTO belegungDTO) {
        Belegung belegung = belegungRepository.findById(belegungDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Belegung mit ID " + belegungDTO.getId() + " nicht gefunden"));

        belegungRepository.delete(belegung);
        protokolliereBelegungGeloescht(belegung);
    }
    
    /**
     * Entfernt mehrere Belegungen anhand von BelegungDTOs.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     * 
     * @param belegungDTOs Liste der DTOs der zu löschenden Belegungen
     * @return Eine Liste mit den IDs der erfolgreich gelöschten Belegungen
     */
    @Transactional
    public List<Long> removeBelegungen(List<BelegungDTO> belegungDTOs) {
        List<Long> erfolgreichGeloescht = new ArrayList<>();
        
        for (BelegungDTO belegungDTO : belegungDTOs) {
            try {
                Optional<Belegung> belegungOpt = belegungRepository.findById(belegungDTO.getId());
                
                if (belegungOpt.isPresent()) {
                    Belegung belegung = belegungOpt.get();

                    // Belegung löschen
                    belegungRepository.delete(belegung);
                    erfolgreichGeloescht.add(belegungDTO.getId());
                    protokolliereBelegungGeloescht(belegung);
                }
            } catch (Exception e) {
                // Bei Fehlern für eine bestimmte Belegung weitermachen mit den nächsten
                log.warn("Bei Massenlöschung konnte eine Belegung nicht gelöscht werden: {}", belegungDTO, e);
                continue;
            }
        }
        
        return erfolgreichGeloescht;
    }

    private void protokolliereBelegungGeloescht(Belegung belegung) {
        // Aktivitätsprotokollierung für das Löschen einer Belegung
        NutzerDTO adminDTO = nutzerService.getAuthenticatedNutzer();

        Student student = belegung.getStudent();
        Kurs kurs = belegung.getKurs();

        // Erstelle eine Beschreibung mit Kursname und Zeitraum
        String beschreibung = "Student aus Kurs entfernt: " + kurs.getName();

        // Optionale zusätzliche Details zur Aktivität
        Map<String, Object> details = new HashMap<>();
        details.put("kursId", kurs.getId());
        details.put("kursName", kurs.getName());
        details.put("studentId", student.getId());
        details.put("matrikelnummer", student.getMatrikelnummer());

        // Protokolliere die Aktivität mit dem Admin als Akteur und dem Studenten als Referenz
        aktivitaetsProtokollierungService.protokolliereAktivitaet(
                adminDTO,
                AktivitaetsTyp.BELEGUNG_LOESCHEN,
                beschreibung,
                details,
                true,
                "Student", // Referenztyp ist der Student
                student.getId()  // Referenz-ID ist die ID des Studenten
        );
    }
    
    /**
     * Aktualisiert das Start- und Enddatum einer Belegung.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param belegungDTO DTO der Belegung
     * @param startDatum Neues Startdatum
     * @param endDatum Neues Enddatum (kann null sein für unbegrenzte Belegung)
     * @return Die aktualisierte Belegung als DTO
     * @throws NoSuchElementException wenn die Belegung nicht gefunden wird
     */
    @Transactional
    public BelegungDTO updateBelegungDates(BelegungDTO belegungDTO, LocalDate startDatum, LocalDate endDatum) {
        Belegung belegung = belegungRepository.findById(belegungDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Belegung mit ID " + belegungDTO.getId() + " nicht gefunden"));
        
        belegung.setStartDatum(startDatum);
        belegung.setEndDatum(endDatum);
        
        Belegung updatedBelegung = belegungRepository.save(belegung);
        return belegungMapper.toDto(updatedBelegung);
    }
    
    /**
     * Gibt alle aktiven Belegungen eines Studenten zurück.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param studentDTO DTO des Studenten
     * @return Liste der aktiven Belegungen
     * @throws NoSuchElementException wenn der Student nicht gefunden wird
     */
    @Transactional(readOnly = true)
    public List<BelegungDTO> getActiveEnrollmentsByStudent(StudentDTO studentDTO) {
        Student student = studentRepository.findById(studentDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Student mit ID " + studentDTO.getId() + " nicht gefunden"));
        
        List<Belegung> aktiveBelegungen = belegungRepository.findActiveByStudent(student, LocalDate.now());
        return belegungMapper.toDtoList(aktiveBelegungen);
    }
    
    /**
     * Gibt alle Belegungen eines Studenten zurück, unabhängig davon, ob sie aktiv sind oder nicht.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param studentDTO DTO des Studenten
     * @return Liste aller Belegungen des Studenten
     * @throws NoSuchElementException wenn der Student nicht gefunden wird
     */
    @Transactional(readOnly = true)
    public List<BelegungDTO> getAllEnrollmentsByStudent(StudentDTO studentDTO) {
        Student student = studentRepository.findById(studentDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Student mit ID " + studentDTO.getId() + " nicht gefunden"));
        
        List<Belegung> alleBelegungen = belegungRepository.findByStudent(student);
        return belegungMapper.toDtoList(alleBelegungen);
    }
    
    /**
     * Gibt alle Studenten zurück, die einen bestimmten Kurs belegen.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param kursDTO DTO des Kurses
     * @return Liste der Belegungen für den Kurs
     * @throws NoSuchElementException wenn der Kurs nicht gefunden wird
     */
    @Transactional(readOnly = true)
    public List<BelegungDTO> getEnrollmentsByKurs(KursDTO kursDTO) {
        Kurs kurs = kursRepository.findById(kursDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursDTO.getId() + " nicht gefunden"));
        
        List<Belegung> kursBelegungen = belegungRepository.findByKurs(kurs);
        return belegungMapper.toDtoList(kursBelegungen);
    }
    
    /**
     * Gibt alle aktiven Belegungen für einen Kurs zurück.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param kursDTO DTO des Kurses
     * @return Liste der aktiven Belegungen für den Kurs
     * @throws NoSuchElementException wenn der Kurs nicht gefunden wird
     */
    @Transactional(readOnly = true)
    public List<BelegungDTO> getActiveEnrollmentsByKurs(KursDTO kursDTO) {
        Kurs kurs = kursRepository.findById(kursDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursDTO.getId() + " nicht gefunden"));
        
        LocalDate heute = LocalDate.now();
        List<Belegung> kursBelegungen = belegungRepository.findByKurs(kurs);
        
        // Filtern der aktiven Belegungen
        List<Belegung> aktiveBelegungen = kursBelegungen.stream()
                .filter(b -> !heute.isBefore(b.getStartDatum()) && 
                             (b.getEndDatum() == null || !heute.isAfter(b.getEndDatum())))
                .toList();
        
        return belegungMapper.toDtoList(aktiveBelegungen);
    }
    
    /**
     * Prüft, ob ein Student einen bestimmten Kurs aktiv belegt.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param studentDTO DTO des Studenten
     * @param kursDTO DTO des Kurses
     * @return true, wenn der Student den Kurs aktiv belegt, sonst false
     */
    @Transactional(readOnly = true)
    public boolean isStudentEnrolledInKurs(StudentDTO studentDTO, KursDTO kursDTO) {
        Student student = studentRepository.findById(studentDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Student mit ID " + studentDTO.getId() + " nicht gefunden"));
        
        Kurs kurs = kursRepository.findById(kursDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursDTO.getId() + " nicht gefunden"));
        
        Optional<Belegung> belegungOpt = belegungRepository.findByStudentAndKurs(student, kurs);
        
        if (belegungOpt.isEmpty()) {
            return false;
        }
        
        Belegung belegung = belegungOpt.get();
        LocalDate heute = LocalDate.now();
        
        return !heute.isBefore(belegung.getStartDatum()) && 
               (belegung.getEndDatum() == null || !heute.isAfter(belegung.getEndDatum()));
    }
    
    /**
     * Gibt eine spezifische Belegung anhand von StudentDTO und KursDTO zurück.
     * Diese Methode verwendet DTOs statt IDs zur besseren Konsistenz mit anderen Services.
     *
     * @param studentDTO DTO des Studenten
     * @param kursDTO DTO des Kurses
     * @return Die gefundene Belegung als DTO
     * @throws NoSuchElementException wenn Student, Kurs oder die Belegung nicht gefunden wird
     */
    @Transactional(readOnly = true)
    public BelegungDTO getBelegungByStudentAndKurs(StudentDTO studentDTO, KursDTO kursDTO) {
        Student student = studentRepository.findById(studentDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Student mit ID " + studentDTO.getId() + " nicht gefunden"));
        
        Kurs kurs = kursRepository.findById(kursDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursDTO.getId() + " nicht gefunden"));
        
        Belegung belegung = belegungRepository.findByStudentAndKurs(student, kurs)
                .orElseThrow(() -> new NoSuchElementException("Belegung nicht gefunden"));
        
        return belegungMapper.toDto(belegung);
    }
    
    /**
     * Gibt eine Belegung anhand ihrer ID zurück.
     *
     * @param belegungId ID der gesuchten Belegung
     * @return Die gefundene Belegung als DTO
     * @throws NoSuchElementException wenn keine Belegung mit der angegebenen ID gefunden wird
     */
    @Transactional(readOnly = true)
    public BelegungDTO getBelegungById(Long belegungId) {
        Belegung belegung = belegungRepository.findById(belegungId)
                .orElseThrow(() -> new NoSuchElementException("Belegung mit ID " + belegungId + " nicht gefunden"));
        return belegungMapper.toDto(belegung);
    }
    
    /**
     * Fügt einen Studenten zu einem Kurs hinzu (erstellt eine neue Belegung).
     * Falls der Student mit der angegebenen Matrikelnummer nicht existiert, wird ein Dummy-Student erstellt.
     *
     * @param matrikelnummer Matrikelnummer des Studenten
     * @param kursId ID des Kurses
     * @param startDatum Startdatum der Belegung
     * @param endDatum Enddatum der Belegung (kann null sein für unbegrenzte Belegung)
     * @return Die erstellte Belegung als DTO
     * @throws NoSuchElementException wenn der Kurs nicht gefunden wird
     * @throws IllegalStateException wenn der Student den Kurs bereits belegt
     */
    @Transactional
    public BelegungDTO addStudentByMatrikelnummerToKurs(String matrikelnummer, Long kursId, LocalDate startDatum, LocalDate endDatum) {

        Kurs kurs = kursRepository.findById(kursId)
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursId + " nicht gefunden"));

        return belegungErstellen(matrikelnummer, kurs, startDatum, endDatum);
    }
    
    /**
     * Fügt mehrere Studenten anhand ihrer Matrikelnummern zu einem Kurs hinzu.
     * Falls Studenten mit den angegebenen Matrikelnummern nicht existieren, werden Dummy-Studenten erstellt.
     * 
     * @param matrikelnummern Liste der Matrikelnummern der Studenten
     * @param kursId ID des Kurses
     * @param startDatum Startdatum der Belegungen
     * @param endDatum Enddatum der Belegungen (kann null sein für unbegrenzte Belegung)
     * @return Eine Liste der erstellten Belegungen als DTOs
     * @throws NoSuchElementException wenn der Kurs nicht gefunden wird
     */
    @Transactional
    public List<BelegungDTO> addMultipleStudentsByMatrikelnummerToKurs(
            List<String> matrikelnummern, Long kursId, LocalDate startDatum, LocalDate endDatum) {
        
        Kurs kurs = kursRepository.findById(kursId)
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursId + " nicht gefunden"));
        
        List<BelegungDTO> erstellteBelegungen = new ArrayList<>();
        
        for (String matrikelnummer : matrikelnummern) {
            // Leerzeichen und andere Whitespaces entfernen
            String sauberMatrikelnummer = matrikelnummer.trim();
            
            // Leere Einträge überspringen
            if (sauberMatrikelnummer.isEmpty()) {
                continue;
            }
            
            try {

                BelegungDTO belegungDTO = belegungErstellen(sauberMatrikelnummer, kurs, startDatum, endDatum);
                erstellteBelegungen.add(belegungDTO);

            } catch (Exception e) {
                // Fehler bei diesem Studenten, zum nächsten weitergehen
                log.warn("Bei einer Massenerstellung konnte eine Belegung nicht erstellt werden: {}", matrikelnummer, e);
                continue;
            }
        }
        
        return erstellteBelegungen;
    }

    private BelegungDTO belegungErstellen(String matrikelnummer, Kurs kurs, LocalDate startDatum, LocalDate endDatum){

        // Prüfen, ob ein Student mit dieser Matrikelnummer bereits existiert
        Optional<StudentDTO> studentByMatrikelnummer = nutzerService.getStudentByMatrikelnummer(matrikelnummer);
        StudentDTO studentDTO;

        //Falls der Student noch nicht existiert, soll ein neuer angelegt werden.
        if(studentByMatrikelnummer.isEmpty()) {
            studentDTO = nutzerService.erstelleDummyStudent(matrikelnummer);

            //Aktivitätsprotokollierung
            NutzerDTO kursverwalterDTO = nutzerService.getAuthenticatedNutzer();
            aktivitaetsProtokollierungService.protokolliereAktivitaet(
                    kursverwalterDTO,
                    AktivitaetsTyp.NUTZER_ERSTELLT,
                    "Neuer Nutzer erstellt",
                    null,
                    true,
                    "Student",
                    studentDTO.getId()
            );

        }else{
            studentDTO = studentByMatrikelnummer.get();
        }

        Student student = studentRepository.findById(studentDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Student mit ID " + studentDTO.getId() + " nicht gefunden"));


        // Prüfen, ob der Student den Kurs bereits belegt
        Optional<Belegung> existingBelegung = belegungRepository.findByStudentAndKurs(student, kurs);
        if (existingBelegung.isPresent()) {
           throw new IllegalStateException("Student belegt diesen Kurs bereits");
        }

        Belegung belegung = new Belegung();
        belegung.setStudent(student);
        belegung.setKurs(kurs);
        belegung.setStartDatum(startDatum);
        belegung.setEndDatum(endDatum);

        Belegung savedBelegung = belegungRepository.save(belegung);
        
        // Aktivitätsprotokollierung für das Hinzufügen zu einem Kurs
        NutzerDTO adminDTO = nutzerService.getAuthenticatedNutzer();
        
        // Erstelle eine Beschreibung mit Kursname und Zeitraum
        StringBuilder beschreibung = new StringBuilder();
        beschreibung.append("Student zu Kurs hinzugefügt: ").append(kurs.getName());
        beschreibung.append(" (Laufzeit: ").append(startDatum);
        if (endDatum != null) {
            beschreibung.append(" bis ").append(endDatum);
        } else {
            beschreibung.append(" bis unbegrenzt");
        }
        beschreibung.append(")");
        
        // Optionale zusätzliche Details zur Aktivität
        Map<String, Object> details = new HashMap<>();
        details.put("kursId", kurs.getId());
        details.put("kursName", kurs.getName());
        details.put("studentId", student.getId());
        details.put("matrikelnummer", matrikelnummer);
        details.put("startDatum", startDatum.toString());
        if (endDatum != null) {
            details.put("endDatum", endDatum.toString());
        }
        
        // Protokolliere die Aktivität mit dem Admin als Akteur und dem Studenten als Referenz
        aktivitaetsProtokollierungService.protokolliereAktivitaet(
                adminDTO,
                AktivitaetsTyp.BELEGUNG_ERSTELLEN,
                beschreibung.toString(),
                details,
                true,
                "Student", // Referenztyp ist der Student
                student.getId()  // Referenz-ID ist die ID des Studenten
        );
        
        return belegungMapper.toDto(savedBelegung);
    }
    
    /**
     * Gibt alle Belegungen eines Kurses mit Fortschrittsinformationen zurück.
     * Diese Methode ergänzt die Belegungsdaten um Fortschrittsinformationen wie
     * Prozentsatz der abgeschlossenen Aufgaben.
     *
     * @param kursDTO DTO des Kurses
     * @return Liste der Belegungen mit Fortschrittsinformationen
     * @throws NoSuchElementException wenn der Kurs nicht gefunden wird
     */
    @Transactional(readOnly = true)
    public List<BelegungDTO> getEnrollmentsByKursWithProgress(KursDTO kursDTO) {
        Kurs kurs = kursRepository.findById(kursDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Kurs mit ID " + kursDTO.getId() + " nicht gefunden"));
        
        List<Belegung> kursBelegungen = belegungRepository.findByKurs(kurs);
        List<BelegungDTO> belegungDTOs = belegungMapper.toDtoList(kursBelegungen);
        
        // Fortschrittsinformationen für jede Belegung hinzufügen
        for (BelegungDTO belegungDTO : belegungDTOs) {
            try {
                // Student-Entity laden für Fortschrittsberechnung
                Student student = studentRepository.findById(belegungDTO.getStudentId())
                        .orElse(null);
                
                if (student != null) {
                    StudentDTO studentDTO = studentMapper.toDto(student);
                    
                    // Fortschritt berechnen
                    KursFortschrittDTO fortschritt = fortschrittService.berechneFortschritt(
                            studentDTO, kursDTO, belegungDTO.isAktiv());
                    
                    // Fortschrittsdaten in BelegungDTO übertragen
                    if (fortschritt != null) {
                        belegungDTO.setFortschrittProzent(
                                fortschritt.getFortschrittProzent() != null ? 
                                fortschritt.getFortschrittProzent().doubleValue() : 0.0);
                        belegungDTO.setAbgeschlosseneTeilaufgaben(
                                fortschritt.getAbgeschlosseneTeilaufgaben() != null ? 
                                fortschritt.getAbgeschlosseneTeilaufgaben().intValue() : 0);
                        belegungDTO.setGesamtTeilaufgaben(
                                fortschritt.getGesamtTeilaufgaben() != null ? 
                                fortschritt.getGesamtTeilaufgaben().intValue() : 0);
                    }
                }
            } catch (Exception e) {
                // Bei Fehler einfach ohne Fortschrittsdaten weitermachen
                log.warn("Fehler beim Berechnen des Fortschritts für Belegung {}: {}", 
                        belegungDTO.getId(), e.getMessage());
            }
        }
        
        return belegungDTOs;
    }
    
    /**
     * Findet eine Belegung für einen Studenten basierend auf einer Teilaufgabe.
     * Diese Methode wird verwendet, um die passende Belegung für Aktivitäten zu finden,
     * die mit Teilaufgaben verknüpft sind.
     * 
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Optional mit der BelegungDTO oder empty wenn keine gefunden wurde
     */
    @Transactional(readOnly = true)
    public Optional<BelegungDTO> findBelegungByStudentAndTeilaufgabe(Long studentId, Long teilaufgabeId) {
        try {
            // Lade die Teilaufgabe, um den zugehörigen Kurs zu finden
            de.fuh.kn.webapp.persistence.entity.Teilaufgabe teilaufgabe = 
                    teilaufgabeRepository.findById(teilaufgabeId).orElse(null);
            
            if (teilaufgabe == null || teilaufgabe.getAufgabe() == null || 
                    teilaufgabe.getAufgabe().getKurseinheit() == null || 
                    teilaufgabe.getAufgabe().getKurseinheit().getKurs() == null) {
                return Optional.empty();
            }
            
            Kurs kurs = teilaufgabe.getAufgabe().getKurseinheit().getKurs();
            Student student = studentRepository.findById(studentId).orElse(null);
            
            if (student == null) {
                return Optional.empty();
            }
            
            // Finde die Belegung für diesen Studenten und Kurs
            Optional<Belegung> belegung = belegungRepository.findByStudentAndKurs(student, kurs);
            return belegung.map(belegungMapper::toDto);
            
        } catch (Exception e) {
            log.error("Fehler beim Finden der Belegung für Student {} und Teilaufgabe {}", 
                    studentId, teilaufgabeId, e);
            return Optional.empty();
        }
    }
}