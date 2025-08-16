package de.fuh.kn.webapp.util;

import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Utility-Klasse für die Berechnung von Semesterdaten.
 * Diese Klasse wird von verschiedenen Controllern verwendet, um Startdatum und Enddatum eines Semesters zu bestimmen.
 */
public class SemesterDateUtil {

    /**
     * Berechnet das nächstliegende Startdatum eines Semesters und das entsprechende Enddatum.
     * Semester beginnen entweder am 1.4. oder am 1.10. eines Jahres.
     * 
     * @param currentDate Das aktuelle Datum
     * @return Ein Array mit zwei Daten: [0] = Startdatum, [1] = Enddatum
     */
    public static LocalDate[] calculateSemesterDates(LocalDate currentDate) {
        int currentYear = currentDate.getYear();

        // Erstelle die drei möglichen Startdaten
        LocalDate aprilFirst = LocalDate.of(currentYear, 4, 1);
        LocalDate octoberFirst = LocalDate.of(currentYear, 10, 1);
        LocalDate nextAprilFirst = LocalDate.of(currentYear + 1, 4, 1);
        
        // Bestimme, welches Datum am nächsten zum aktuellen Datum liegt (auch in der Vergangenheit)
        long daysToApril = Math.abs(DAYS.between(currentDate, aprilFirst));
        long daysToOctober = Math.abs(DAYS.between(currentDate, octoberFirst));
        long daysToNextApril = Math.abs(DAYS.between(currentDate, nextAprilFirst));
        
        LocalDate startDatum;
        LocalDate endDatum;
        
        // Wähle das Datum, das am nächsten liegt
        if (daysToApril <= daysToOctober && daysToApril <= daysToNextApril) {
            // April dieses Jahres ist am nächsten
            startDatum = aprilFirst;
            endDatum = LocalDate.of(aprilFirst.getYear(), 9, 30);
        } else if (daysToOctober <= daysToApril && daysToOctober <= daysToNextApril) {
            // Oktober dieses Jahres ist am nächsten
            startDatum = octoberFirst;
            endDatum = LocalDate.of(octoberFirst.getYear() + 1, 3, 31);
        } else {
            // April nächsten Jahres ist am nächsten
            startDatum = nextAprilFirst;
            endDatum = LocalDate.of(nextAprilFirst.getYear(), 9, 30);
        }
        
        return new LocalDate[] { startDatum, endDatum };
    }
}
