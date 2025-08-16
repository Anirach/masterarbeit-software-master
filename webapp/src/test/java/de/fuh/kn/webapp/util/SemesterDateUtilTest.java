package de.fuh.kn.webapp.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testklasse für den SemesterDateUtil.
 * Testet die Logik zur Bestimmung von Semester-Startdatum und Enddatum.
 */
class SemesterDateUtilTest {

    /**
     * Parametrisierter Test für die Logik zur Bestimmung des Semesterstartdatums.
     * Testet verschiedene Datumskonstellationen und prüft, ob die erwarteten Daten berechnet werden.
     * 
     * @param currentDate Aktuelles Datum
     * @param expectedStartDate Erwartetes Startdatum
     * @param expectedEndDate Erwartetes Enddatum
     * @param scenario Beschreibung des Testszenarios
     */
    @ParameterizedTest(name = "{3}: Bei aktuellem Datum {0} sollte Start={1}, Ende={2}")
    @MethodSource("provideDateTestCases")
    void semesterDateLogic_ShouldSelectCorrectDates(
            LocalDate currentDate, 
            LocalDate expectedStartDate, 
            LocalDate expectedEndDate, 
            String scenario) {
        
        // Act
        LocalDate[] dates = SemesterDateUtil.calculateSemesterDates(currentDate);
        LocalDate calculatedStartDate = dates[0];
        LocalDate calculatedEndDate = dates[1];

        // Assert
        assertEquals(expectedStartDate, calculatedStartDate, 
                "Startdatum sollte korrekt berechnet werden: " + scenario);
        assertEquals(expectedEndDate, calculatedEndDate,
                "Enddatum sollte korrekt berechnet werden: " + scenario);
    }

    /**
     * Liefert Testfälle für die Semesterdaten-Logik.
     * Jeder Testfall enthält:
     * - aktuelles Datum
     * - erwartetes Startdatum
     * - erwartetes Enddatum
     * - Beschreibung des Szenarios
     */
    private static Stream<Arguments> provideDateTestCases() {
        int year = 2025;
        return Stream.of(
            // Standard-Szenarien für verschiedene Monate
            Arguments.of(
                LocalDate.of(year, 1, 15),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Januar: Sollte April dieses Jahres wählen"),
                
            Arguments.of(
                LocalDate.of(year, 5, 15),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Mai: Sollte April dieses Jahres wählen (auch wenn in Vergangenheit)"),
                
            Arguments.of(
                LocalDate.of(year, 8, 15),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "August: Sollte Oktober dieses Jahres wählen"),
                
            Arguments.of(
                LocalDate.of(year, 11, 15),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "November: Sollte Oktober dieses Jahres wählen (auch wenn in Vergangenheit)"),
                
            Arguments.of(
                LocalDate.of(year, 12, 31),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "Dezember: Sollte Oktober dieses Jahres wählen (auch wenn in Vergangenheit)"),
                
            // Grenzfälle
            Arguments.of(
                LocalDate.of(year, 3, 31),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Grenzfall 31. März: Tag vor Semesterbeginn"),
                
            Arguments.of(
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 4, 1),
                LocalDate.of(year, 9, 30),
                "Grenzfall 1. April: Genau am Semesterbeginn"),
                
            Arguments.of(
                LocalDate.of(year, 9, 30),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "Grenzfall 30. September: Genau am Semesterende"),
                
            Arguments.of(
                LocalDate.of(year, 10, 1),
                LocalDate.of(year, 10, 1),
                LocalDate.of(year + 1, 3, 31),
                "Grenzfall 1. Oktober: Genau am Semesterbeginn"),
                
            Arguments.of(
                LocalDate.of(year + 1, 3, 31),
                LocalDate.of(year + 1, 4, 1),
                LocalDate.of(year + 1, 9, 30),
                "Grenzfall 31. März Folgejahr: Genau am Semesterende")
        );
    }
}