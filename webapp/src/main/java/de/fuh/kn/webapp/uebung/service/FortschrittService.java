package de.fuh.kn.webapp.uebung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.StudentAufgabenService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import de.fuh.kn.webapp.uebung.dto.AufgabeFortschrittDto;
import de.fuh.kn.webapp.uebung.dto.KursFortschrittDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service für die Verwaltung des Fortschritts von Studenten in Kursen.
 * Diese Klasse bietet Funktionen, um den Fortschritt von Studenten in Kursen zu ermitteln,
 * ohne dass die Kurs-Daten selbst verändert werden müssen.
 */
@Service
public class FortschrittService {

    private final StudentRepository studentRepository;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final LoesungsVersuchRepository loesungsVersuchRepository;
    private final StudentAufgabenService studentAufgabenService;
    private final AufgabeService aufgabeService;

    @Autowired
    public FortschrittService(
            StudentRepository studentRepository,
            TeilaufgabeRepository teilaufgabeRepository,
            LoesungsVersuchRepository loesungsVersuchRepository,
            StudentAufgabenService studentAufgabenService,
            AufgabeService aufgabeService) {
        this.studentRepository = studentRepository;
        this.teilaufgabeRepository = teilaufgabeRepository;
        this.loesungsVersuchRepository = loesungsVersuchRepository;
        this.studentAufgabenService = studentAufgabenService;
        this.aufgabeService = aufgabeService;
    }

    /**
     * Berechnet den Fortschritt eines Studenten in einem Kurs.
     *
     * @param studentDTO Der Student, dessen Fortschritt berechnet werden soll.
     * @param kursDTO Der Kurs, für den der Fortschritt berechnet werden soll.
     * @param istAktiv Gibt an, ob die Kursbelegung aktiv ist.
     * @return Ein KursFortschrittDTO mit den Fortschrittsinformationen.
     */
    @Transactional(readOnly = true)
    public KursFortschrittDTO berechneFortschritt(StudentDTO studentDTO, KursDTO kursDTO, boolean istAktiv) {
        // Student-Entity laden
        Student student = studentRepository.findById(studentDTO.getId())
                .orElseThrow(() -> new IllegalStateException("Student nicht gefunden: " + studentDTO.getId()));

        // Gesamtzahl aller Teilaufgaben im Kurs
        long gesamtAufgaben = teilaufgabeRepository.countTeilaufgabenByKursId(kursDTO.getId());

        // Anzahl der vom Studenten gelösten Teilaufgaben in diesem Kurs
        long geloesteTeilaufgaben = loesungsVersuchRepository.countByStudentIdAndKursIdAndKorrekt(
                student.getId(), kursDTO.getId(), true);

        // Fortschritt in Prozent berechnen
        int prozent = 0;
        if (gesamtAufgaben > 0) {
            prozent = (int) Math.round((double) geloesteTeilaufgaben / gesamtAufgaben * 100);
        }

        // KursFortschrittDTO erstellen
        return new KursFortschrittDTO(
                kursDTO,
                prozent,
                geloesteTeilaufgaben,
                gesamtAufgaben,
                istAktiv
        );
    }

    /**
     * Konvertiert eine Liste von Kursen in eine Liste von KursFortschrittDTOs mit Fortschrittsinformationen.
     *
     * @param studentDTO Der Student, dessen Fortschritt berechnet werden soll.
     * @param kursDTOs Die Liste der Kurse.
     * @param istAktiv Gibt an, ob die Kursbelegungen aktiv sind.
     * @return Eine Liste von KursFortschrittDTOs mit den Fortschrittsinformationen.
     */
    @Transactional(readOnly = true)
    public List<KursFortschrittDTO> berechneFortschrittFuerKurse(StudentDTO studentDTO, List<KursDTO> kursDTOs, boolean istAktiv) {
        if (kursDTOs == null || kursDTOs.isEmpty()) {
            return Collections.emptyList();
        }

        return kursDTOs.stream()
                .map(kursDTO -> berechneFortschritt(studentDTO, kursDTO, istAktiv))
                .collect(Collectors.toList());
    }

    /**
     * Zählt die Anzahl der von einem Studenten gelösten Aufgaben.
     *
     * @param studentId Die ID des Studenten.
     * @return Die Anzahl der gelösten Aufgaben.
     */
    @Transactional(readOnly = true)
    public Long zaehleSolvedAssignments(Long studentId) {
        return loesungsVersuchRepository.countByStudentIdAndKorrekt(studentId, true);
    }

    /**
     * Erstellt ein AufgabeFortschrittDto für eine Aufgabe und einen Studenten.
     *
     * @param aufgabe Die Aufgabe, für die der Fortschritt berechnet werden soll
     * @param studentDTO Der Student, dessen Fortschritt berechnet werden soll
     * @param zugaenglich Map mit Informationen, ob die Aufgabe für den Studenten zugänglich ist
     * @return Ein AufgabeFortschrittDto mit allen relevanten Informationen
     */
    @Transactional(readOnly = true)
    public AufgabeFortschrittDto erstelleAufgabeFortschritt(
            AufgabeDto aufgabe,
            StudentDTO studentDTO,
            Map<Long, Boolean> zugaenglich) {

        // Anzahl der abgeschlossenen Teilaufgaben ermitteln
        int abgeschlosseneTeilaufgaben = studentAufgabenService.zaehleAbgeschlosseneTeilaufgaben(
                aufgabe, studentDTO.getId());

        // Durchschnittliche Punktzahl ermitteln
        int durchschnittlichePunktzahl = studentAufgabenService.berechneAufgabenPunktestand(
                aufgabe, studentDTO.getId());

        // Zugänglichkeitsstatus der Aufgabe
        boolean freigeschaltet = zugaenglich.getOrDefault(aufgabe.getId(), false);

        // AufgabeFortschrittDto erstellen
        return new AufgabeFortschrittDto(
                aufgabe,
                abgeschlosseneTeilaufgaben,
                durchschnittlichePunktzahl,
                freigeschaltet
        );
    }

    /**
     * Erstellt eine Map von AufgabeFortschrittDto-Objekten für einen Kurs.
     * Diese Methode ist effizienter als separate Aufrufe, da sie alle notwendigen Informationen
     * in einem Durchgang berechnet.
     *
     * @param kursDTO Der Kurs, für den die Aufgabenfortschritte berechnet werden sollen
     * @param studentDTO Der Student, dessen Fortschritt berechnet werden soll
     * @return Eine Map mit Aufgaben-IDs als Schlüssel und AufgabeFortschrittDto-Objekten als Werten
     */
    @Transactional(readOnly = true)
    public Map<Long, AufgabeFortschrittDto> erstelleAufgabenFortschritteMap(
            KursDTO kursDTO,
            StudentDTO studentDTO) {

        // Alle Aufgaben des Kurses sammeln
        List<AufgabeDto> alleAufgaben = kursDTO.getKurseinheiten().stream()
                .flatMap(kurseinheit -> aufgabeService.getAufgabenByKurseinheitId(kurseinheit.getId()).stream())
                .collect(Collectors.toList());

        // Berechne den Zugriffsstatus für alle Aufgaben in einem Aufruf
        Map<Long, Boolean> aufgabenZugaenglich = studentAufgabenService.bestimmeAufgabenZugangsstatus(
                studentDTO, alleAufgaben);
                
        // Optimiert: Berechne die abgeschlossenen Teilaufgaben für alle Aufgaben in einem Schritt
        Map<Long, Integer> abgeschlosseneTeilaufgaben = studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(
                alleAufgaben, studentDTO.getId());

        // Berechne optimiert den Fortschritt für alle Aufgaben
        Map<Long, AufgabeFortschrittDto> ergebnis = new HashMap<>();

        for (AufgabeDto aufgabe : alleAufgaben) {
            // Zugänglichkeit aus der berechneten Map holen
            boolean freigeschaltet = aufgabenZugaenglich.getOrDefault(aufgabe.getId(), false);
            
            // Abgeschlossene Teilaufgaben aus der optimierten Map holen
            Integer abgeschlossen = abgeschlosseneTeilaufgaben.getOrDefault(aufgabe.getId(), 0);
            
            // Punktzahl noch einzeln berechnen, da dies schwieriger zu batchen ist
            int durchschnittlichePunktzahl = studentAufgabenService.berechneAufgabenPunktestand(
                    aufgabe, studentDTO.getId());
            
            // AufgabeFortschrittDto direkt erstellen
            AufgabeFortschrittDto fortschrittDto = new AufgabeFortschrittDto(
                    aufgabe, 
                    abgeschlossen,
                    durchschnittlichePunktzahl,
                    freigeschaltet);
            
            ergebnis.put(aufgabe.getId(), fortschrittDto);
        }

        return ergebnis;
    }

    /**
     * Erstellt eine Map von AufgabenFortschrittDto-Objekten pro Kurseinheit für einen Kurs.
     * Diese Methode ist optimiert für Effizienz und berechnet alle notwendigen Informationen
     * in einem Durchgang und gruppiert sie nach Kurseinheiten.
     *
     * @param kursDTO Der Kurs, für den die Aufgabenfortschritte berechnet werden sollen
     * @param studentDTO Der Student, dessen Fortschritt berechnet werden soll
     * @return Eine Map mit Kurseinheit-IDs als Schlüssel und Listen von AufgabeFortschrittDto-Objekten als Werten
     */
    @Transactional(readOnly = true)
    public Map<Long, List<AufgabeFortschrittDto>> erstelleAufgabenFortschritteProKurseinheitMap(
            KursDTO kursDTO,
            StudentDTO studentDTO) {

        // Aufgaben pro Kurseinheit und alle Aufgaben sammeln
        Map<Long, List<AufgabeDto>> aufgabenProKurseinheit = new HashMap<>();
        List<AufgabeDto> alleAufgaben = new ArrayList<>();

        for (KurseinheitDTO kurseinheit : kursDTO.getKurseinheiten()) {
            List<AufgabeDto> aufgaben = aufgabeService.getAufgabenByKurseinheitId(kurseinheit.getId());
            aufgabenProKurseinheit.put(kurseinheit.getId(), aufgaben);
            alleAufgaben.addAll(aufgaben);
        }

        // Berechne den Zugriffsstatus für alle Aufgaben in einem Aufruf
        Map<Long, Boolean> aufgabenZugaenglich = studentAufgabenService.bestimmeAufgabenZugangsstatus(
                studentDTO, alleAufgaben);
                
        // Optimiert: Berechne die abgeschlossenen Teilaufgaben für alle Aufgaben in einem Schritt
        Map<Long, Integer> abgeschlosseneTeilaufgaben = studentAufgabenService.berechneAbgeschlosseneTeilaufgabenMap(
                alleAufgaben, studentDTO.getId());

        // Fortschritte pro Kurseinheit erstellen
        Map<Long, List<AufgabeFortschrittDto>> ergebnis = new HashMap<>();

        for (Map.Entry<Long, List<AufgabeDto>> entry : aufgabenProKurseinheit.entrySet()) {
            Long kurseinheitId = entry.getKey();
            List<AufgabeDto> aufgaben = entry.getValue();
            List<AufgabeFortschrittDto> fortschrittDtos = new ArrayList<>(aufgaben.size());

            for (AufgabeDto aufgabe : aufgaben) {
                // Zugänglichkeit aus der berechneten Map holen
                boolean freigeschaltet = aufgabenZugaenglich.getOrDefault(aufgabe.getId(), false);
                
                // Abgeschlossene Teilaufgaben aus der optimierten Map holen
                Integer abgeschlossen = abgeschlosseneTeilaufgaben.getOrDefault(aufgabe.getId(), 0);
                
                // Punktzahl noch einzeln berechnen, da dies schwieriger zu batchen ist
                int durchschnittlichePunktzahl = studentAufgabenService.berechneAufgabenPunktestand(
                        aufgabe, studentDTO.getId());
                
                // AufgabeFortschrittDto direkt erstellen
                AufgabeFortschrittDto fortschrittDto = new AufgabeFortschrittDto(
                        aufgabe, 
                        abgeschlossen,
                        durchschnittlichePunktzahl,
                        freigeschaltet);
                
                fortschrittDtos.add(fortschrittDto);
            }
            
            ergebnis.put(kurseinheitId, fortschrittDtos);
        }

        return ergebnis;
    }
}