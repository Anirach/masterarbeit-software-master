package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeZugangsService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.StudentService;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service für die Verwaltung von studentenbezogenen Aufgabenfunktionen.
 * Bietet Funktionen zur Bestimmung des Aufgaben-Abschlussstatus für Studenten.
 */
@Service
public class StudentAufgabenService {

    private final LoesungsVersuchRepository loesungsVersuchRepository;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final StudentService studentService;
    private final AufgabeZugangsService aufgabeZugangsService;
    private final AufgabeService aufgabeService;

    @Autowired
    public StudentAufgabenService(
            LoesungsVersuchRepository loesungsVersuchRepository,
            TeilaufgabeRepository teilaufgabeRepository,
            StudentService studentService,
            AufgabeZugangsService aufgabeZugangsService,
            AufgabeService aufgabeService) {
        this.loesungsVersuchRepository = loesungsVersuchRepository;
        this.teilaufgabeRepository = teilaufgabeRepository;
        this.studentService = studentService;
        this.aufgabeZugangsService = aufgabeZugangsService;
        this.aufgabeService = aufgabeService;
    }

    /**
     * Überprüft, ob ein Student Zugang zu bestimmten Aufgaben hat.
     *
     * @param studentDTO Der Student
     * @param aufgaben Die zu überprüfenden Aufgaben
     * @return Eine Map mit Aufgaben-IDs als Schlüssel und Zugangsstatus als Werte
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> bestimmeAufgabenZugangsstatus(StudentDTO studentDTO, List<AufgabeDto> aufgaben) {
        if (aufgaben == null || aufgaben.isEmpty()) {
            return Collections.emptyMap();
        }

        // Erstelle die Ergebnis-Map mit der erwarteten Größe für bessere Performance
        Map<Long, Boolean> aufgabenZugaenglich = new HashMap<>(aufgaben.size());

        // Hier könnte in Zukunft eine Batch-Verarbeitung implementiert werden,
        // falls die Methode hatZugangZuAufgabe eine solche unterstützt
        for (AufgabeDto aufgabe : aufgaben) {
            boolean hatZugang = aufgabeZugangsService.hatZugangZuAufgabe(aufgabe.getId(), studentDTO.getId());
            aufgabenZugaenglich.put(aufgabe.getId(), hatZugang);
        }

        return aufgabenZugaenglich;
    }

    /**
     * Berechnet für eine Aufgabe, wie viele Teilaufgaben ein Student bereits abgeschlossen hat.
     *
     * @param aufgabe Die Aufgabe, deren Teilaufgaben überprüft werden sollen.
     * @param studentId Die ID des Studenten.
     * @return Die Anzahl der abgeschlossenen Teilaufgaben.
     */
    @Transactional(readOnly = true)
    public int zaehleAbgeschlosseneTeilaufgaben(AufgabeDto aufgabe, Long studentId) {
        if (aufgabe == null || aufgabe.getTeilaufgaben() == null || aufgabe.getTeilaufgaben().isEmpty()) {
            return 0;
        }

        // Extrahiere die Teilaufgaben-IDs
        List<Long> teilaufgabenIds = aufgabe.getTeilaufgaben().stream()
                .map(TeilaufgabeDto::getId)
                .collect(Collectors.toList());

        // Batch-Anfrage, die alle abgeschlossenen Teilaufgaben für diesen Studenten zurückgibt
        List<Long> abgeschlosseneTeilaufgabenIds = loesungsVersuchRepository
                .findAbgeschlosseneTeilaufgabenIdsByStudentId(studentId, teilaufgabenIds);

        return abgeschlosseneTeilaufgabenIds.size();
    }

    /**
     * Berechnet für eine Aufgabe den durchschnittlichen Punktestand eines Studenten.
     * Diese Methode berechnet den Durchschnitt der Bewertungspunkte aller Teilaufgaben.
     *
     * @param aufgabe Die Aufgabe, deren Punktestand berechnet werden soll.
     * @param studentId Die ID des Studenten.
     * @return Der durchschnittliche Punktestand in Prozent (0-100).
     */
    @Transactional(readOnly = true)
    public int berechneAufgabenPunktestand(AufgabeDto aufgabe, Long studentId) {
        if (aufgabe == null || aufgabe.getTeilaufgaben() == null || aufgabe.getTeilaufgaben().isEmpty()) {
            return 0;
        }

        // Liste der Teilaufgaben-IDs erstellen
        List<Long> teilaufgabenIds = aufgabe.getTeilaufgaben().stream()
                .map(TeilaufgabeDto::getId)
                .collect(Collectors.toList());

        return berechneDurchschnittlichePunktzahl(studentId, teilaufgabenIds);
    }

    /**
     * Berechnet den durchschnittlichen Punktestand für eine Liste von Teilaufgaben.
     * Diese Hilfsmethode kann auch von anderen Diensten verwendet werden, um Batch-Berechnungen zu ermöglichen.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabenIds Liste der Teilaufgaben-IDs
     * @return Der durchschnittliche Punktestand in Prozent (0-100)
     */
    @Transactional(readOnly = true)
    public int berechneDurchschnittlichePunktzahl(Long studentId, List<Long> teilaufgabenIds) {
        if (teilaufgabenIds == null || teilaufgabenIds.isEmpty()) {
            return 0;
        }

        // Summe und Anzahl der Bewertungspunkte initialisieren
        int summe = 0;
        int anzahlTeilaufgabenMitBewertung = 0;

        // Für jede Teilaufgabe den letzten Lösungsversuch mit Bewertung holen
        for (Long teilaufgabeId : teilaufgabenIds) {
            // JPA Query direkt verwenden, um Entitäten nicht zu exponieren
            List<Integer> bewertungspunkte = loesungsVersuchRepository.findBewertungspunkteByStudentIdAndTeilaufgabeId(
                    studentId, teilaufgabeId);

            // Wenn es Bewertungspunkte gibt, zum Durchschnitt hinzufügen
            if (!bewertungspunkte.isEmpty() && bewertungspunkte.get(0) != null) {
                summe += bewertungspunkte.get(0);
                anzahlTeilaufgabenMitBewertung++;
            }
        }

        // Durchschnitt berechnen
        return anzahlTeilaufgabenMitBewertung > 0 ? (int) Math.round((double) summe / anzahlTeilaufgabenMitBewertung) : 0;
    }
    
    /**
     * Erstellt eine Map von abgeschlossenen Teilaufgaben pro Aufgabe für eine Liste von Aufgaben.
     * Diese Methode optimiert die Berechnung der Fortschritte für mehrere Aufgaben gleichzeitig.
     *
     * @param aufgaben Die Liste der Aufgaben, für die Fortschritte berechnet werden sollen
     * @param studentId Die ID des Studenten, dessen Fortschritte berechnet werden sollen
     * @return Eine Map mit Aufgaben-IDs als Schlüssel und der Anzahl abgeschlossener Teilaufgaben als Werte
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> berechneAbgeschlosseneTeilaufgabenMap(List<AufgabeDto> aufgaben, Long studentId) {
        if (aufgaben == null || aufgaben.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Extrahiere alle Teilaufgaben-IDs und ordne sie den Aufgaben zu
        Map<Long, List<Long>> teilaufgabenIdsProAufgabe = new HashMap<>();
        List<Long> alleTeilaufgabenIds = new ArrayList<>();
        
        for (AufgabeDto aufgabe : aufgaben) {
            if (aufgabe.getTeilaufgaben() != null && !aufgabe.getTeilaufgaben().isEmpty()) {
                List<Long> teilaufgabenIds = aufgabe.getTeilaufgaben().stream()
                        .map(TeilaufgabeDto::getId)
                        .collect(Collectors.toList());
                
                teilaufgabenIdsProAufgabe.put(aufgabe.getId(), teilaufgabenIds);
                alleTeilaufgabenIds.addAll(teilaufgabenIds);
            } else {
                teilaufgabenIdsProAufgabe.put(aufgabe.getId(), Collections.emptyList());
            }
        }
        
        // Hole alle abgeschlossenen Teilaufgaben in einem Schritt
        List<Long> abgeschlosseneTeilaufgabenIds = loesungsVersuchRepository
                .findAbgeschlosseneTeilaufgabenIdsByStudentId(studentId, alleTeilaufgabenIds);
        
        // Zähle die abgeschlossenen Teilaufgaben pro Aufgabe
        Map<Long, Integer> ergebnis = new HashMap<>();
        for (AufgabeDto aufgabe : aufgaben) {
            List<Long> teilaufgabenIds = teilaufgabenIdsProAufgabe.get(aufgabe.getId());
            int anzahlAbgeschlossen = 0;
            
            for (Long teilaufgabeId : teilaufgabenIds) {
                if (abgeschlosseneTeilaufgabenIds.contains(teilaufgabeId)) {
                    anzahlAbgeschlossen++;
                }
            }
            
            ergebnis.put(aufgabe.getId(), anzahlAbgeschlossen);
        }
        
        return ergebnis;
    }
}