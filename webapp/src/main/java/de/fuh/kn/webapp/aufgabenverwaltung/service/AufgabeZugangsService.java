package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.service.StudentService;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import de.fuh.kn.webapp.persistence.repository.AufgabeRepository;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service zur Überprüfung von Zugangsbedingungen für Aufgaben.
 * Kontrolliert, ob ein Student die Voraussetzungen (vorherige Aufgaben abgeschlossen) erfüllt,
 * um eine bestimmte Aufgabe zu bearbeiten.
 */
@Service
public class AufgabeZugangsService {

    private final AufgabeRepository aufgabeRepository;
    private final LoesungsVersuchRepository loesungsVersuchRepository;
    private final StudentService studentService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Repositories und Services.
     *
     * @param aufgabeRepository Repository für Aufgabe-Entitäten
     * @param loesungsVersuchRepository Repository für LoesungsVersuch-Entitäten
     * @param studentService Service für den Zugriff auf Student-Entitäten
     */
    @Autowired
    public AufgabeZugangsService(
            AufgabeRepository aufgabeRepository, 
            LoesungsVersuchRepository loesungsVersuchRepository,
            StudentService studentService) {
        this.aufgabeRepository = aufgabeRepository;
        this.loesungsVersuchRepository = loesungsVersuchRepository;
        this.studentService = studentService;
    }

    /**
     * Überprüft, ob ein Student auf eine bestimmte Aufgabe zugreifen darf.
     * Der Zugriff ist erlaubt, wenn alle vorherigen Aufgaben im gesamten Kurs (basierend auf der Reihenfolge
     * der Kurseinheiten und der Aufgaben innerhalb der Kurseinheiten) mindestens einen Lösungsversuch haben,
     * der als abgeschlossen oder übersprungen markiert ist.
     *
     * @param aufgabeId Die ID der zu überprüfenden Aufgabe
     * @param studentId Die ID des Studenten, für den der Zugriff überprüft wird
     * @return true, wenn der Zugriff erlaubt ist, sonst false
     */
    @Transactional(readOnly = true)
    public boolean hatZugangZuAufgabe(Long aufgabeId, Long studentId) {
        Aufgabe aufgabe = aufgabeRepository.findById(aufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + aufgabeId + " existiert nicht."));

        // Überprüfen, ob der Student existiert
        if (!studentService.existsById(studentId)) {
            throw new IllegalArgumentException("Student mit ID " + studentId + " existiert nicht.");
        }

        // Die erste Aufgabe der ersten Kurseinheit ist immer zugänglich
        if (aufgabe.getReihenfolge() == 1 && aufgabe.getKurseinheit().getReihenfolge() == 1) {
            return true;
        }

        // Hole alle Kurseinheiten des Kurses, sortiert nach Reihenfolge
        List<Aufgabe> alleAufgabenDesKurses = new ArrayList<>();

        // Hole alle Kurseinheiten des gleichen Kurses
        List<Kurseinheit> alleKurseinheiten = aufgabeRepository.findAll().stream()
                .filter(a -> a.getKurseinheit().getKurs().equals(aufgabe.getKurseinheit().getKurs()))
                .map(Aufgabe::getKurseinheit)
                .distinct()
                .sorted(Comparator.comparing(Kurseinheit::getReihenfolge))
                .toList();

        // Füge alle Aufgaben in der richtigen Reihenfolge hinzu
        for (Kurseinheit kurseinheit : alleKurseinheiten) {
            // Nur Aufgaben bis zur aktuellen Kurseinheit oder Aufgaben in der aktuellen Kurseinheit
            // mit niedrigerer Reihenfolge als die aktuelle Aufgabe berücksichtigen
            if (kurseinheit.getReihenfolge() < aufgabe.getKurseinheit().getReihenfolge() ||
                (kurseinheit.getId().equals(aufgabe.getKurseinheit().getId()) &&
                 aufgabe.getReihenfolge() > 1)) {

                List<Aufgabe> aufgabenDerKurseinheit = aufgabeRepository
                        .findByKurseinheitOrderByReihenfolgeAsc(kurseinheit);

                // Wenn wir in der aktuellen Kurseinheit sind, nur Aufgaben mit niedrigerer Reihenfolge hinzufügen
                if (kurseinheit.getId().equals(aufgabe.getKurseinheit().getId())) {
                    aufgabenDerKurseinheit = aufgabenDerKurseinheit.stream()
                            .filter(a -> a.getReihenfolge() < aufgabe.getReihenfolge())
                            .toList();
                }

                alleAufgabenDesKurses.addAll(aufgabenDerKurseinheit);
            }
        }

        // Prüfe für jede vorherige Aufgabe im gesamten Kurs, ob der Student sie abgeschlossen hat
        for (Aufgabe vorherigeAufgabe : alleAufgabenDesKurses) {
            if (!hatAufgabeAbgeschlossenInternal(vorherigeAufgabe, studentId)) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Hilfsmethode, die intern verwendet wird, um zu prüfen, ob ein Student eine Aufgabe abgeschlossen hat.
     *
     * @param aufgabe Die Aufgabe, die überprüft werden soll
     * @param studentId Die ID des Studenten
     * @return true, wenn die Aufgabe abgeschlossen wurde, sonst false
     */
    private boolean hatAufgabeAbgeschlossenInternal(Aufgabe aufgabe, Long studentId) {
        List<Teilaufgabe> teilaufgaben = aufgabe.getTeilaufgaben();

        // Prüfe jede Teilaufgabe
        for (Teilaufgabe teilaufgabe : teilaufgaben) {
            boolean teilaufgabeAbgeschlossen = false;

            // Prüfe direkt mit Repository-Methoden, die keine Entities benötigen
            boolean istAbgeschlossen = loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(
                    studentId, teilaufgabe.getId());
            boolean istUebersprungen = loesungsVersuchRepository.existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(
                    studentId, teilaufgabe.getId());

            teilaufgabeAbgeschlossen = istAbgeschlossen || istUebersprungen;

            // Wenn eine Teilaufgabe nicht abgeschlossen ist, ist die gesamte Aufgabe nicht abgeschlossen
            if (!teilaufgabeAbgeschlossen) {
                return false;
            }
        }

        // Alle Teilaufgaben sind abgeschlossen oder übersprungen
        return true;
    }

    /**
     * Überprüft, ob ein Student eine Aufgabe abgeschlossen hat.
     * Eine Aufgabe gilt als abgeschlossen, wenn für jede ihrer Teilaufgaben mindestens
     * ein Lösungsversuch existiert, der als abgeschlossen oder übersprungen markiert ist.
     *
     * @param aufgabeId Die ID der zu überprüfenden Aufgabe
     * @param studentId Die ID des Studenten, für den die Überprüfung durchgeführt wird
     * @return true, wenn die Aufgabe als abgeschlossen gilt, sonst false
     */
    @Transactional(readOnly = true)
    public boolean hatAufgabeAbgeschlossen(Long aufgabeId, Long studentId) {
        Aufgabe aufgabe = aufgabeRepository.findById(aufgabeId)
                .orElseThrow(() -> new IllegalArgumentException("Aufgabe mit ID " + aufgabeId + " existiert nicht."));

        // Überprüfen, ob der Student existiert
        if (!studentService.existsById(studentId)) {
            throw new IllegalArgumentException("Student mit ID " + studentId + " existiert nicht.");
        }

        return hatAufgabeAbgeschlossenInternal(aufgabe, studentId);
    }

    /**
     * Überprüft, ob ein Student zur nächsten Aufgabe weitergehen darf.
     * 
     * @param kurseinheitId Die ID der Kurseinheit
     * @param aktuelleAufgabeId Die ID der aktuellen Aufgabe
     * @param studentId Die ID des Studenten, für den die Überprüfung durchgeführt wird
     * @return true, wenn der Student zur nächsten Aufgabe weitergehen darf, sonst false
     */
    @Transactional(readOnly = true)
    public boolean darfZurNaechstenAufgabe(Long kurseinheitId, Long aktuelleAufgabeId, Long studentId) {
        // Diese Methode benötigt keine Kurseinheit, daher wird der Parameter kurseinheitId nicht verwendet
        return hatAufgabeAbgeschlossen(aktuelleAufgabeId, studentId);
    }
}