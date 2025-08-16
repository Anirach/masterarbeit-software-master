package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service für die Verwaltung von Aufgaben aus Studentensicht.
 * Bietet Funktionen zum Anzeigen, Navigieren und Verwalten von Aufgaben für Studenten.
 */
@Service
@RequiredArgsConstructor
public class StudentAufgabeService {

    private final AufgabeService aufgabeService;
    private final AufgabeZugangsService aufgabeZugangsService;
    private final LoesungsversuchService loesungsversuchService;

    /**
     * Überprüft, ob ein Student Zugang zu einer Aufgabe hat.
     * 
     * @param aufgabeId Die ID der Aufgabe
     * @param studentId Die ID des Studenten
     * @return true, wenn der Student Zugang zur Aufgabe hat, sonst false
     */
    @Transactional(readOnly = true)
    public boolean hatZugangZuAufgabe(Long aufgabeId, Long studentId) {
        return aufgabeZugangsService.hatZugangZuAufgabe(aufgabeId, studentId);
    }
    
    /**
     * Ermittelt die aktuelle Teilaufgabe für einen Studenten bei einer Aufgabe.
     * Gibt entweder die angeforderte Teilaufgabe zurück, die erste nicht abgeschlossene
     * Teilaufgabe oder die erste Teilaufgabe, wenn alle abgeschlossen sind.
     * 
     * @param aufgabeDto Die Aufgabe
     * @param teilaufgabeId Optional die ID einer konkreten Teilaufgabe
     * @param studentId Die ID des Studenten
     * @return Die Teilaufgabe, die der Student bearbeiten soll
     */
    @Transactional(readOnly = true)
    public TeilaufgabeDto ermittleAktiveTeilaufgabe(AufgabeDto aufgabeDto, Long teilaufgabeId, Long studentId) {
        // 1. Wenn teilaufgabeId als Parameter übergeben wurde, diese verwenden
        if (teilaufgabeId != null) {
            for (TeilaufgabeDto teilaufgabeDto : aufgabeDto.getTeilaufgaben()) {
                if (teilaufgabeDto.getId().equals(teilaufgabeId)) {
                    return teilaufgabeDto;
                }
            }
        }
        
        // 2. Falls keine Teilaufgabe durch ID gefunden wurde, erste nicht abgeschlossene verwenden
        for (TeilaufgabeDto teilaufgabeDto : aufgabeDto.getTeilaufgaben()) {
            if (!loesungsversuchService.istTeilaufgabeAbgeschlossen(studentId, teilaufgabeDto.getId())) {
                return teilaufgabeDto;
            }
        }
        
        // 3. Wenn alle abgeschlossen sind, erste anzeigen
        if (!aufgabeDto.getTeilaufgaben().isEmpty()) {
            return aufgabeDto.getTeilaufgaben().get(0);
        }
        
        return null;
    }
    
    /**
     * Sucht die nächste nicht erledigte Teilaufgabe nach der aktuellen Teilaufgabe.
     * 
     * @param aufgabeDto Die Aufgabe
     * @param aktuelleTeillaufgabeId Die ID der aktuellen Teilaufgabe
     * @param studentId Die ID des Studenten
     * @return Optional mit der nächsten Teilaufgabe oder leeres Optional, wenn keine
     *         mehr vorhanden ist
     */
    @Transactional(readOnly = true)
    public Optional<TeilaufgabeDto> findeNaechsteTeilaufgabe(
            AufgabeDto aufgabeDto, Long aktuelleTeillaufgabeId, Long studentId) {
        boolean aktuelleGefunden = false;
        
        for (TeilaufgabeDto teilaufgabeDto : aufgabeDto.getTeilaufgaben()) {
            // Wenn die aktuelle Teilaufgabe gefunden wurde, suche nach der nächsten
            if (aktuelleGefunden) {
                if (!loesungsversuchService.istTeilaufgabeErledigt(studentId, teilaufgabeDto.getId())) {
                    return Optional.of(teilaufgabeDto);
                }
            }
            
            // Aktuelle Teilaufgabe gefunden
            if (teilaufgabeDto.getId().equals(aktuelleTeillaufgabeId)) {
                aktuelleGefunden = true;
            }
        }
        
        return Optional.empty();
    }

}