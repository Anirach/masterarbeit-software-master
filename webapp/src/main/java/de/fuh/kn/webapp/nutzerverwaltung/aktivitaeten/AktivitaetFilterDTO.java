package de.fuh.kn.webapp.nutzerverwaltung.aktivitaeten;

import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Data Transfer Objekt für Filter-Parameter bei Aktivitätsanfragen.
 * Kapselt alle Filter- und Paginierungsparameter, die bei der Suche nach Aktivitäten verwendet werden.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AktivitaetFilterDTO {
    /**
     * Der zu filternde Aktivitätstyp (optional)
     */
    private String aktivitaetsTyp;

    /**
     * Startdatum für die Filterung nach Zeitraum (optional)
     */
    private String startDatum;
    
    /**
     * Enddatum für die Filterung nach Zeitraum (optional)
     */
    private String endDatum;
    
    /**
     * Allgemeiner Suchbegriff für Beschreibung (optional)
     */
    private String suchbegriff;
    
    /**
     * Die aktuelle Seitennummer (standardmäßig 0)
     */
    @Builder.Default
    private int page = 0;
    
    /**
     * Die Seitengröße (standardmäßig 10)
     */
    @Builder.Default
    private int size = 10;
    
    /**
     * Die ID des Nutzers, dessen Aktivitäten angezeigt werden sollen (optional)
     */
    private Long nutzerId;
    
    /**
     * Flag, ob alle Nutzer angezeigt werden sollen
     */
    private boolean showAllUsers;
    
    /**
     * Konvertiert das String-Attribut aktivitaetsTyp in den Enum-Wert AktivitaetsTyp,
     * falls ein gültiger Wert vorhanden ist.
     *
     * @return Der entsprechende AktivitaetsTyp oder null, wenn nicht vorhanden oder ungültig
     */
    public AktivitaetsTyp getAktivitaetsTypEnum() {
        if (aktivitaetsTyp == null || aktivitaetsTyp.isEmpty()) {
            return null;
        }
        
        try {
            return AktivitaetsTyp.valueOf(aktivitaetsTyp);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Konvertiert das startDatum-String in ein LocalDateTime-Objekt für den Anfang des Tages.
     * Falls kein Datum angegeben ist, wird null zurückgegeben.
     *
     * @return LocalDateTime für den Beginn des angegebenen Tages oder null
     */
    public LocalDateTime getStartDateTime() {
        if (startDatum == null || startDatum.isEmpty()) {
            return null;
        }
        
        try {
            LocalDate date = LocalDate.parse(startDatum);
            return date.atStartOfDay(); // Setzt die Zeit auf 00:00:00
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Konvertiert das endDatum-String in ein LocalDateTime-Objekt für das Ende des Tages.
     * Falls kein Datum angegeben ist, wird null zurückgegeben.
     *
     * @return LocalDateTime für das Ende des angegebenen Tages oder null
     */
    public LocalDateTime getEndDateTime() {
        if (endDatum == null || endDatum.isEmpty()) {
            return null;
        }
        
        try {
            LocalDate date = LocalDate.parse(endDatum);
            return date.atTime(LocalTime.MAX); // Setzt die Zeit auf 23:59:59.999999999
        } catch (Exception e) {
            return null;
        }
    }
}
