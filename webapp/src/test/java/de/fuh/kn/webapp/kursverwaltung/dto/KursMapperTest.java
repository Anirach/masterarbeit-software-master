package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testklasse für KursMapper.
 */
@SpringBootTest
class KursMapperTest {

    @Autowired
    private KursMapper kursMapper;

    /**
     * Testet die Konvertierung von Kurs-Entity zu KursDTO.
     */
    @Test
    void testKursToKursDTO() {
        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Entity zu DTO konvertieren
        KursDTO kursDTO = kursMapper.toDto(kurs);

        // Ergebnisse überprüfen
        assertNotNull(kursDTO);
        assertEquals(kurs.getId(), kursDTO.getId());
        assertEquals(kurs.getName(), kursDTO.getName());
        assertNotNull(kursDTO.getKurseinheiten());
        assertNotNull(kursDTO.getKursMaterialien());
    }

    /**
     * Testet die Konvertierung von KursDTO zu Kurs-Entity.
     */
    @Test
    void testKursDTOToKurs() {
        // KursDTO erstellen
        KursDTO kursDTO = new KursDTO();
        kursDTO.setId(1L);
        kursDTO.setName("Kommunikationsnetze");

        // DTO zu Entity konvertieren
        Kurs kurs = kursMapper.toEntity(kursDTO);

        // Ergebnisse überprüfen
        assertNotNull(kurs);
        assertEquals(kursDTO.getId(), kurs.getId());
        assertEquals(kursDTO.getName(), kurs.getName());
        assertNotNull(kurs.getKurseinheiten());
        assertNotNull(kurs.getKursMaterialien());
    }

    /**
     * Testet die Konvertierung von einer Liste von Kurs-Entities zu einer Liste von KursDTOs.
     */
    @Test
    void testKursListToKursDTOList() {
        // Kurs-Entities erstellen
        Kurs kurs1 = new Kurs();
        kurs1.setId(1L);
        kurs1.setName("Kommunikationsnetze");

        Kurs kurs2 = new Kurs();
        kurs2.setId(2L);
        kurs2.setName("Algorithmen und Datenstrukturen");

        List<Kurs> kurse = Arrays.asList(kurs1, kurs2);

        // Entities zu DTOs konvertieren
        List<KursDTO> kursDTOs = kursMapper.toDtoList(kurse);

        // Ergebnisse überprüfen
        assertNotNull(kursDTOs);
        assertEquals(2, kursDTOs.size());
        assertEquals(kurs1.getId(), kursDTOs.get(0).getId());
        assertEquals(kurs1.getName(), kursDTOs.get(0).getName());
        assertEquals(kurs2.getId(), kursDTOs.get(1).getId());
        assertEquals(kurs2.getName(), kursDTOs.get(1).getName());
    }

    /**
     * Testet die Konvertierung von Kurs-Entity mit Kurseinheiten zu KursDTO.
     */
    @Test
    void testKursWithKurseinheitenToKursDTO() {
        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Kurseinheiten erstellen
        Kurseinheit kurseinheit1 = new Kurseinheit();
        kurseinheit1.setId(1L);
        kurseinheit1.setName("Einheit 1");
        kurseinheit1.setReihenfolge(1);
        kurseinheit1.setKurs(kurs);

        Kurseinheit kurseinheit2 = new Kurseinheit();
        kurseinheit2.setId(2L);
        kurseinheit2.setName("Einheit 2");
        kurseinheit2.setReihenfolge(2);
        kurseinheit2.setKurs(kurs);

        kurs.setKurseinheiten(Arrays.asList(kurseinheit1, kurseinheit2));

        // Entity zu DTO konvertieren
        KursDTO kursDTO = kursMapper.toDto(kurs);

        // Ergebnisse überprüfen
        assertNotNull(kursDTO);
        assertEquals(kurs.getId(), kursDTO.getId());
        assertEquals(kurs.getName(), kursDTO.getName());
        assertEquals(2, kursDTO.getKurseinheiten().size());
        assertEquals(kurseinheit1.getId(), kursDTO.getKurseinheiten().get(0).getId());
        assertEquals(kurseinheit1.getName(), kursDTO.getKurseinheiten().get(0).getName());
        assertEquals(kurseinheit1.getReihenfolge(), kursDTO.getKurseinheiten().get(0).getReihenfolge());
        assertEquals(kurs.getId(), kursDTO.getKurseinheiten().get(0).getKursId());
    }
}
