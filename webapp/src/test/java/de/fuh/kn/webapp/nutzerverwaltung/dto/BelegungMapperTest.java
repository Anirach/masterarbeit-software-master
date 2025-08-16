package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Belegung;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für BelegungMapper.
 */
@SpringBootTest
class BelegungMapperTest {

    @Autowired
    private BelegungMapper belegungMapper;

    /**
     * Testet die Konvertierung von Belegung-Entity zu BelegungDTO.
     */
    @Test
    void testBelegungToBelegungDTO() {
        // Student-Entity erstellen
        Student student = new Student();
        student.setId(1L);
        student.setVorname("Max");
        student.setNachname("Mustermann");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Belegung-Entity erstellen
        Belegung belegung = new Belegung();
        belegung.setId(1L);
        belegung.setStudent(student);
        belegung.setKurs(kurs);
        belegung.setStartDatum(LocalDate.now().minusDays(10));
        belegung.setEndDatum(LocalDate.now().plusMonths(6));

        // Entity zu DTO konvertieren
        BelegungDTO belegungDTO = belegungMapper.toDto(belegung);

        // Ergebnisse überprüfen
        assertNotNull(belegungDTO);
        assertEquals(belegung.getId(), belegungDTO.getId());
        assertEquals(student.getId(), belegungDTO.getStudentId());
        assertEquals(student.getMatrikelnummer(), belegungDTO.getMatrikelnummer());
        assertEquals("Max Mustermann (Matrikelnr. 123456)", belegungDTO.getStudentName());
        assertEquals(kurs.getId(), belegungDTO.getKursId());
        assertEquals(kurs.getName(), belegungDTO.getKursName());
        assertEquals(belegung.getStartDatum(), belegungDTO.getStartDatum());
        assertEquals(belegung.getEndDatum(), belegungDTO.getEndDatum());
        assertTrue(belegungDTO.isAktiv()); // Belegung sollte aktiv sein
        assertEquals(student.getIstRegistriert(), belegungDTO.getIstRegistriert());
    }

    /**
     * Testet die Konvertierung von BelegungDTO zu Belegung-Entity.
     */
    @Test
    void testBelegungDTOToBelegung() {
        // BelegungDTO erstellen
        BelegungDTO belegungDTO = new BelegungDTO();
        belegungDTO.setId(1L);
        belegungDTO.setStudentId(1L);
        belegungDTO.setKursId(1L);
        belegungDTO.setStartDatum(LocalDate.now().minusDays(10));
        belegungDTO.setEndDatum(LocalDate.now().plusMonths(6));

        // DTO zu Entity konvertieren
        Belegung belegung = belegungMapper.toEntity(belegungDTO);

        // Ergebnisse überprüfen
        assertNotNull(belegung);
        assertEquals(belegungDTO.getId(), belegung.getId());
        assertEquals(belegungDTO.getStartDatum(), belegung.getStartDatum());
        assertEquals(belegungDTO.getEndDatum(), belegung.getEndDatum());
        // Kurs und Student müssen separat gesetzt werden
        assertNull(belegung.getStudent());
        assertNull(belegung.getKurs());
    }

    /**
     * Testet die Konvertierung von einer Liste von Belegung-Entities zu einer Liste von BelegungDTOs.
     */
    @Test
    void testBelegungListToBelegungDTOList() {
        // Student-Entity erstellen
        Student student = new Student();
        student.setId(1L);
        student.setVorname("Max");
        student.setNachname("Mustermann");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        // Kurs-Entities erstellen
        Kurs kurs1 = new Kurs();
        kurs1.setId(1L);
        kurs1.setName("Kommunikationsnetze");

        Kurs kurs2 = new Kurs();
        kurs2.setId(2L);
        kurs2.setName("Algorithmen und Datenstrukturen");

        // Belegung-Entities erstellen
        Belegung belegung1 = new Belegung();
        belegung1.setId(1L);
        belegung1.setStudent(student);
        belegung1.setKurs(kurs1);
        belegung1.setStartDatum(LocalDate.now().minusDays(10));
        belegung1.setEndDatum(null); // Unbegrenzte Belegung

        Belegung belegung2 = new Belegung();
        belegung2.setId(2L);
        belegung2.setStudent(student);
        belegung2.setKurs(kurs2);
        belegung2.setStartDatum(LocalDate.now().minusDays(5));
        belegung2.setEndDatum(LocalDate.now().plusMonths(6));

        List<Belegung> belegungen = Arrays.asList(belegung1, belegung2);

        // Entities zu DTOs konvertieren
        List<BelegungDTO> belegungDTOs = belegungMapper.toDtoList(belegungen);

        // Ergebnisse überprüfen
        assertNotNull(belegungDTOs);
        assertEquals(2, belegungDTOs.size());
        
        // Belegung 1 überprüfen
        assertEquals(belegung1.getId(), belegungDTOs.get(0).getId());
        assertEquals(student.getId(), belegungDTOs.get(0).getStudentId());
        assertEquals(student.getMatrikelnummer(), belegungDTOs.get(0).getMatrikelnummer());
        assertEquals(kurs1.getId(), belegungDTOs.get(0).getKursId());
        assertEquals(kurs1.getName(), belegungDTOs.get(0).getKursName());
        assertEquals(belegung1.getStartDatum(), belegungDTOs.get(0).getStartDatum());
        assertEquals(belegung1.getEndDatum(), belegungDTOs.get(0).getEndDatum());
        assertTrue(belegungDTOs.get(0).isAktiv()); // Sollte aktiv sein, da kein Enddatum
        
        // Belegung 2 überprüfen
        assertEquals(belegung2.getId(), belegungDTOs.get(1).getId());
        assertEquals(student.getId(), belegungDTOs.get(1).getStudentId());
        assertEquals(student.getMatrikelnummer(), belegungDTOs.get(1).getMatrikelnummer());
        assertEquals(kurs2.getId(), belegungDTOs.get(1).getKursId());
        assertEquals(kurs2.getName(), belegungDTOs.get(1).getKursName());
        assertEquals(belegung2.getStartDatum(), belegungDTOs.get(1).getStartDatum());
        assertEquals(belegung2.getEndDatum(), belegungDTOs.get(1).getEndDatum());
        assertTrue(belegungDTOs.get(1).isAktiv()); // Sollte aktiv sein, da Enddatum in der Zukunft
    }

    /**
     * Testet die isAktiv Methode für verschiedene Belegungszustände.
     */
    @Test
    void testIsAktiv() {
        // Student und Kurs für alle Belegungen
        Student student = new Student();
        student.setId(1L);
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // 1. Belegung mit Startdatum in der Vergangenheit und ohne Enddatum (sollte aktiv sein)
        Belegung belegungOhneEnde = new Belegung();
        belegungOhneEnde.setId(1L);
        belegungOhneEnde.setStudent(student);
        belegungOhneEnde.setKurs(kurs);
        belegungOhneEnde.setStartDatum(LocalDate.now().minusDays(10));
        belegungOhneEnde.setEndDatum(null);

        // 2. Belegung mit Startdatum in der Vergangenheit und Enddatum in der Zukunft (sollte aktiv sein)
        Belegung belegungAktiv = new Belegung();
        belegungAktiv.setId(2L);
        belegungAktiv.setStudent(student);
        belegungAktiv.setKurs(kurs);
        belegungAktiv.setStartDatum(LocalDate.now().minusDays(10));
        belegungAktiv.setEndDatum(LocalDate.now().plusMonths(6));

        // 3. Belegung mit Startdatum in der Vergangenheit und Enddatum in der Vergangenheit (sollte inaktiv sein)
        Belegung belegungAbgelaufen = new Belegung();
        belegungAbgelaufen.setId(3L);
        belegungAbgelaufen.setStudent(student);
        belegungAbgelaufen.setKurs(kurs);
        belegungAbgelaufen.setStartDatum(LocalDate.now().minusMonths(12));
        belegungAbgelaufen.setEndDatum(LocalDate.now().minusMonths(6));

        // 4. Belegung mit Startdatum in der Zukunft (sollte inaktiv sein)
        Belegung belegungZukuenftig = new Belegung();
        belegungZukuenftig.setId(4L);
        belegungZukuenftig.setStudent(student);
        belegungZukuenftig.setKurs(kurs);
        belegungZukuenftig.setStartDatum(LocalDate.now().plusDays(10));
        belegungZukuenftig.setEndDatum(LocalDate.now().plusMonths(6));

        // DTOs konvertieren
        BelegungDTO belegungOhneEndeDTO = belegungMapper.toDto(belegungOhneEnde);
        BelegungDTO belegungAktivDTO = belegungMapper.toDto(belegungAktiv);
        BelegungDTO belegungAbgelaufenDTO = belegungMapper.toDto(belegungAbgelaufen);
        BelegungDTO belegungZukuenftigDTO = belegungMapper.toDto(belegungZukuenftig);

        // Aktiv-Status überprüfen
        assertTrue(belegungOhneEndeDTO.isAktiv());
        assertTrue(belegungAktivDTO.isAktiv());
        assertFalse(belegungAbgelaufenDTO.isAktiv());
        assertFalse(belegungZukuenftigDTO.isAktiv());
    }

    /**
     * Testet die Anzeige des Studentennamens für nicht registrierte Studenten.
     */
    @Test
    void testStudentNameNichtRegistriert() {
        // Student-Entity erstellen (nicht registriert)
        Student student = new Student();
        student.setId(1L);
        student.setVorname(null);
        student.setNachname(null);
        student.setIstRegistriert(false);
        student.setMatrikelnummer("123456");

        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Belegung-Entity erstellen
        Belegung belegung = new Belegung();
        belegung.setId(1L);
        belegung.setStudent(student);
        belegung.setKurs(kurs);
        belegung.setStartDatum(LocalDate.now().minusDays(10));
        belegung.setEndDatum(LocalDate.now().plusMonths(6));

        // Entity zu DTO konvertieren
        BelegungDTO belegungDTO = belegungMapper.toDto(belegung);

        // Ergebnisse überprüfen
        assertNotNull(belegungDTO);
        assertEquals("Nicht registriert (Matrikelnr. 123456)", belegungDTO.getStudentName());
        assertFalse(belegungDTO.getIstRegistriert());
    }
}
