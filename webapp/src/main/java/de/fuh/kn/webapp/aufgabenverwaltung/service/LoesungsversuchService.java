package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchMapper;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsMapper;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsRequestDto;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsResponseDto;
import de.fuh.kn.webapp.llm.service.LlmBewertungService;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.LoesungsVersuch;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service für die Verwaltung von Lösungsversuchen für Aufgaben.
 * Bietet Funktionen zum Verwalten und Zurücksetzen von Lösungsversuchen,
 * sowie zum Finden von Studenten anhand ihrer E-Mail und zum Zurücksetzen
 * von übersprungenen Lösungsversuchen beim Login. Enthält auch Funktionen zur
 * Überprüfung, ob ein Student die Schwelle für den Zugriff auf Musterlösungen erreicht hat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoesungsversuchService {

    private final LoesungsVersuchRepository loesungsVersuchRepository;
    private final StudentRepository studentRepository;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final KursRepository kursRepository;
    private final LoesungsVersuchMapper loesungsVersuchMapper;
    private final BewertungsMapper bewertungsMapper;
    private final LlmBewertungService llmBewertungService;

    /**
     * Findet einen Studenten anhand seiner E-Mail-Adresse.
     *
     * @param email Die E-Mail-Adresse des Studenten
     * @return Optional mit der Student-ID oder ein leeres Optional, wenn kein Student mit der
     * E-Mail-Adresse gefunden wurde
     */
    @Transactional(readOnly = true)
    public Optional<Long> findeStudentIdByEmail(String email) {
        return studentRepository.findByEmail(email)
                .map(Student::getId);
    }

    /**
     * Setzt alle übersprungenen (aber nicht abgeschlossenen) Lösungsversuche eines Studenten zurück.
     * Übersprungene Lösungsversuche werden beim Login zurückgesetzt, damit Studenten
     * die Aufgaben später noch lösen können oder bewusst erneut überspringen müssen.
     *
     * @param studentId Die ID des Studenten, dessen übersprungene Lösungsversuche zurückgesetzt werden sollen
     * @return Die Anzahl der zurückgesetzten Lösungsversuche
     */
    @Transactional
    public int setzeUebersprungeneLoesungsversucheZurueck(Long studentId) {
        if (studentId == null) {
            log.warn("Versuch, übersprungene Lösungsversuche für null-Student-ID zurückzusetzen");
            return 0;
        }

        try {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

            // Finde alle Lösungsversuche, die übersprungen aber nicht abgeschlossen sind
            List<LoesungsVersuch> uebersprungeneVersuche = loesungsVersuchRepository.findByStudentAndIstUebersprungenTrueAndIstAbgeschlossenFalse(student);

            int anzahl = uebersprungeneVersuche.size();
            if (anzahl > 0) {
                log.info("Setze {} übersprungene Lösungsversuche zurück für Student ID: {}", anzahl, studentId);

                // Setze übersprungen-Flag zurück für alle gefundenen Versuche
                for (LoesungsVersuch versuch : uebersprungeneVersuche) {
                    versuch.setIstUebersprungen(false);
                    loesungsVersuchRepository.save(versuch);
                }
            }

            return anzahl;
        } catch (Exception e) {
            log.error("Fehler beim Zurücksetzen übersprungener Lösungsversuche für Student ID {}: {}",
                    studentId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Sucht den letzten Lösungsversuch eines Studenten für eine Teilaufgabe.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Optional mit dem letzten Lösungsversuch oder ein leeres Optional, wenn kein Versuch gefunden wurde
     */
    @Transactional(readOnly = true)
    public Optional<LoesungsVersuchDTO> findeNeuesterLoesungsversuch(Long studentId, Long teilaufgabeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        Optional<LoesungsVersuch> neusterVersuch = loesungsVersuchRepository
                .findFirstByStudentAndTeilaufgabeOrderByZeitpunktDesc(student, teilaufgabe);

        // Nur zurückgeben, wenn nicht zurückgesetzt
        if (neusterVersuch.isPresent() && !neusterVersuch.get().getIstZurueckGesetzt()) {
            return neusterVersuch.map(loesungsVersuchMapper::toDto);
        }

        return Optional.empty();
    }

    /**
     * Überprüft, ob eine Teilaufgabe von einem Studenten abgeschlossen wurde.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return true, wenn die Teilaufgabe abgeschlossen wurde, sonst false
     */
    @Transactional(readOnly = true)
    public boolean istTeilaufgabeAbgeschlossen(Long studentId, Long teilaufgabeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        return loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(student, teilaufgabe);
    }

    /**
     * Überprüft, ob eine Teilaufgabe von einem Studenten übersprungen wurde.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return true, wenn die Teilaufgabe übersprungen wurde, sonst false
     */
    @Transactional(readOnly = true)
    public boolean istTeilaufgabeUebersprungen(Long studentId, Long teilaufgabeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        return loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstUebersprungenTrue(student, teilaufgabe);
    }

    /**
     * Überprüft, ob eine Teilaufgabe von einem Studenten erledigt wurde (abgeschlossen oder übersprungen).
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return true, wenn die Teilaufgabe erledigt wurde, sonst false
     */
    @Transactional(readOnly = true)
    public boolean istTeilaufgabeErledigt(Long studentId, Long teilaufgabeId) {
        return istTeilaufgabeAbgeschlossen(studentId, teilaufgabeId) ||
               istTeilaufgabeUebersprungen(studentId, teilaufgabeId);
    }

    /**
     * Markiert einen Lösungsversuch als abgeschlossen.
     *
     * @param loesungsversuchId Die ID des Lösungsversuchs
     * @param studentId Die ID des Studenten (zur Berechtigung)
     * @return Der aktualisierte Lösungsversuch als DTO
     * @throws IllegalStateException Wenn der Lösungsversuch nicht dem angegebenen Studenten gehört
     *                              oder nicht genügend Punkte hat
     */
    @Transactional
    public LoesungsVersuchDTO markiereLoesungsversuchAbgeschlossen(Long loesungsversuchId, Long studentId) {
        LoesungsVersuch loesungsVersuch = loesungsVersuchRepository.findById(loesungsversuchId)
                .orElseThrow(() -> new IllegalStateException("Lösungsversuch nicht gefunden: " + loesungsversuchId));

        // Prüfe, ob der Student der Eigentümer ist
        if (!loesungsVersuch.getStudent().getId().equals(studentId)) {
            throw new IllegalStateException("Nicht berechtigt, diesen Lösungsversuch zu ändern");
        }

        // Prüfe, ob genug Punkte erreicht wurden (mindestens 50)
        if (loesungsVersuch.getBewertungPunkte() == null || loesungsVersuch.getBewertungPunkte() < 50) {
            throw new IllegalStateException("Lösungsversuch hat nicht genügend Punkte erreicht (mind. 50 Punkte erforderlich)");
        }

        // Als abgeschlossen markieren und speichern
        loesungsVersuch.setIstAbgeschlossen(true);
        LoesungsVersuch gespeicherterVersuch = loesungsVersuchRepository.save(loesungsVersuch);

        return loesungsVersuchMapper.toDto(gespeicherterVersuch);
    }

    /**
     * Erstellt einen neuen Lösungsversuch, der als übersprungen markiert ist.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Der erstellte Lösungsversuch als DTO
     */
    @Transactional
    public LoesungsVersuchDTO erstelleUebersprungenenLoesungsversuch(Long studentId, Long teilaufgabeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        // Neuen Lösungsversuch erstellen, der als übersprungen markiert ist
        LoesungsVersuch loesungsVersuch = new LoesungsVersuch();
        loesungsVersuch.setStudent(student);
        loesungsVersuch.setTeilaufgabe(teilaufgabe);
        loesungsVersuch.setZeitpunkt(LocalDateTime.now());
        loesungsVersuch.setIstUebersprungen(true);
        loesungsVersuch.setLoesungFelder(new HashMap<>());
        loesungsVersuch.setBewertungFelderFarbe(new HashMap<>());

        // Speichern des Lösungsversuchs
        LoesungsVersuch gespeicherterVersuch = loesungsVersuchRepository.save(loesungsVersuch);

        return loesungsVersuchMapper.toDto(gespeicherterVersuch);
    }

    /**
     * Erstellt einen neuen Lösungsversuch für eine Teilaufgabe mit den angegebenen Lösungsfeldern und
     * bewertet diesen Versuch (Mock-Implementierung).
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @param loesungFelder Die eingereichten Lösungen für die Felder
     * @return Der erstellte und bewertete Lösungsversuch als DTO
     */
    @Transactional
    public LoesungsVersuchDTO erstelleUndBewerteLoesungsversuch(Long studentId, Long teilaufgabeId, Map<String, String> loesungFelder) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        // Neuen Lösungsversuch erstellen
        LoesungsVersuch loesungsVersuch = new LoesungsVersuch();
        loesungsVersuch.setStudent(student);
        loesungsVersuch.setTeilaufgabe(teilaufgabe);
        loesungsVersuch.setZeitpunkt(LocalDateTime.now());
        loesungsVersuch.setLoesungFelder(loesungFelder);
        loesungsVersuch.setIstAbgeschlossen(false);

        // Bewertungsanfrage erstellen und an LLM-Service übergeben
        BewertungsRequestDto requestDto = bewertungsMapper.createRequestDto(teilaufgabe, loesungsVersuch);

        // Tatsächliche Bewertung durch GPT-4.1-nano durchführen
        log.info("Bewertung der Lösung für Teilaufgabe {}", teilaufgabeId);
        BewertungsResponseDto responseDto = llmBewertungService.evaluateSolution(requestDto);

        // Loggen der Tokens und Kosten für Monitoring
        log.info("Bewertung abgeschlossen. Input-Tokens: {}, Output-Tokens: {}, Kosten: ${}",
                responseDto.getInputToken(),
                responseDto.getOutputToken(),
                responseDto.getCost() != null ? responseDto.getCost().toPlainString() : "unbekannt");

        // Lösungsversuch mit der Bewertung aktualisieren
        loesungsVersuch = bewertungsMapper.updateLoesungsVersuch(loesungsVersuch, responseDto);

        // Lösungsversuch speichern
        LoesungsVersuch gespeicherterVersuch = loesungsVersuchRepository.save(loesungsVersuch);

        return loesungsVersuchMapper.toDto(gespeicherterVersuch);
    }

    /**
     * Überprüft, ob ein Student mindestens 50% der Punkte für eine Teilaufgabe erreicht hat.
     * Diese Methode prüft alle bisherigen Lösungsversuche des Studenten für die angegebene Teilaufgabe
     * und gibt true zurück, wenn mindestens ein Versuch 50 oder mehr Punkte erreicht hat.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return true, wenn der Student mindestens 50% der Punkte erreicht hat, sonst false
     */
    @Transactional(readOnly = true)
    public boolean hatMindestens50ProzentErreicht(Long studentId, Long teilaufgabeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        // Alle Lösungsversuche des Studenten für diese Teilaufgabe laden
        List<LoesungsVersuch> versuche = loesungsVersuchRepository.findByStudentAndTeilaufgabe(student, teilaufgabe);

        // Prüfen, ob mindestens ein Versuch 50 oder mehr Punkte hat
        return versuche.stream()
                .anyMatch(versuch -> versuch.getBewertungPunkte() != null && versuch.getBewertungPunkte() >= 50);
    }

    /**
     * Findet den neuesten Lösungsversuch für jede Teilaufgabe einer Aufgabe.
     * Rückgabe ist eine Map mit TeilaufgabeId -> LoesungsVersuchDTO.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeIds Liste der IDs aller Teilaufgaben der Aufgabe
     * @return Map mit TeilaufgabeId -> LoesungsVersuchDTO für alle Teilaufgaben, für die Versuche existieren
     */
    @Transactional(readOnly = true)
    public Map<Long, LoesungsVersuchDTO> findeNeuesteLoesungsversucheFuerAlleTeilaufgaben(Long studentId, List<Long> teilaufgabeIds) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Map<Long, LoesungsVersuchDTO> ergebnisse = new HashMap<>();

        // Für jede Teilaufgabe-ID den neuesten Lösungsversuch finden
        for (Long teilaufgabeId : teilaufgabeIds) {
            Optional<LoesungsVersuchDTO> neusterVersuch = findeNeuesterLoesungsversuch(studentId, teilaufgabeId);
            neusterVersuch.ifPresent(versuch -> ergebnisse.put(teilaufgabeId, versuch));
        }

        return ergebnisse;
    }

    /**
     * Findet alle Lösungsversuche eines Studenten für eine bestimmte Teilaufgabe.
     * Sortiert nach Zeitpunkt absteigend (neueste zuerst).
     * Schließt auch zurückgesetzte Versuche ein, um eine vollständige Historie anzuzeigen.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Liste aller Lösungsversuche für die angegebene Teilaufgabe, inkl. zurückgesetzte Versuche
     */
    @Transactional(readOnly = true)
    public List<LoesungsVersuchDTO> findeAlleLösungsversuche(Long studentId, Long teilaufgabeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        // Alle Lösungsversuche des Studenten für diese Teilaufgabe laden, sortiert nach Zeitpunkt
        List<LoesungsVersuch> versuche = loesungsVersuchRepository.findByStudentAndTeilaufgabeOrderByZeitpunktDesc(student, teilaufgabe);

        // Alle Versuche anzeigen, auch zurückgesetzte, damit die Historie vollständig ist
        return versuche.stream()
                .map(loesungsVersuchMapper::toDto)
                .toList();
    }

    /**
     * Setzt alle Lösungsversuche eines Studenten für eine Teilaufgabe zurück.
     * Nach dem Zurücksetzen werden die Lösungsfelder nicht mehr vorausgefüllt.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Die Anzahl der zurückgesetzten Lösungsversuche
     */
    @Transactional
    public int setzeLoesungsversucheZurueck(Long studentId, Long teilaufgabeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Teilaufgabe teilaufgabe = teilaufgabeRepository.findById(teilaufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId));

        // Alle Lösungsversuche des Studenten für diese Teilaufgabe laden
        List<LoesungsVersuch> versuche = loesungsVersuchRepository.findByStudentAndTeilaufgabe(student, teilaufgabe);

        // Alle Versuche als zurückgesetzt markieren
        for (LoesungsVersuch versuch : versuche) {
            versuch.setIstZurueckGesetzt(true);
            loesungsVersuchRepository.save(versuch);
        }

        return versuche.size();
    }


    /**
     * Setzt alle Lösungsversuche eines Studenten in einem Kurs zurück.
     *
     * @param studentId Die ID des Studenten, dessen Fortschritt zurückgesetzt werden soll
     * @param kursId Die ID des Kurses, für den der Fortschritt zurückgesetzt werden soll
     * @throws IllegalArgumentException wenn Student oder Kurs nicht gefunden wird
     */
    @Transactional
    public void setzeKursLoesungsversucheZurueck(Long studentId, Long kursId) {
        // Student und Kurs laden
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));

        Kurs kurs = kursRepository.findById(kursId)
                .orElseThrow(() -> new IllegalArgumentException("Kurs nicht gefunden: " + kursId));

        // Alle Lösungsversuche des Studenten in diesem Kurs löschen
        List<LoesungsVersuch> loesungsversuche = loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(student, kurs);

        for (LoesungsVersuch loesungsVersuch : loesungsversuche) {
            loesungsVersuch.setIstZurueckGesetzt(true);
            loesungsVersuchRepository.save(loesungsVersuch);
        }
    }
}