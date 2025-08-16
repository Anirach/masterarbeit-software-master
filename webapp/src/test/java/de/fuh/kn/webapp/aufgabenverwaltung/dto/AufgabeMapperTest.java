package de.fuh.kn.webapp.aufgabenverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für AufgabeMapper.
 */
@SpringBootTest
class AufgabeMapperTest {

    @Autowired
    private AufgabeMapper aufgabeMapper;

    /**
     * Testet die Konvertierung von Aufgabe-Entity zu AufgabeDto.
     */
    @Test
    void testAufgabeToAufgabeDto() {
        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = new Kurseinheit();
        kurseinheit.setId(1L);
        kurseinheit.setName("Einheit 1");
        kurseinheit.setReihenfolge(1);

        // Aufgabe-Entity erstellen
        Aufgabe aufgabe = new Aufgabe();
        aufgabe.setId(1L);
        aufgabe.setTitel("Aufgabe 1");
        aufgabe.setAufgabenText(null); // Einfache Aufgabe hat keinen allgemeinen Text
        aufgabe.setReihenfolge(1);
        aufgabe.setKurseinheit(kurseinheit);

        // Eine Teilaufgabe hinzufügen, um "Einfach" zu machen
        Teilaufgabe teilaufgabe = new Teilaufgabe();
        teilaufgabe.setId(1L);
        teilaufgabe.setReihenfolge(1);
        teilaufgabe.setAufgabe(aufgabe);
        List<Teilaufgabe> teilaufgaben = new ArrayList<>();
        teilaufgaben.add(teilaufgabe);
        aufgabe.setTeilaufgaben(teilaufgaben);

        // Entity zu DTO konvertieren
        AufgabeDto aufgabeDto = aufgabeMapper.toDto(aufgabe);

        // Ergebnisse überprüfen
        assertNotNull(aufgabeDto);
        assertEquals(aufgabe.getId(), aufgabeDto.getId());
        assertEquals(aufgabe.getTitel(), aufgabeDto.getTitel());
        assertEquals(aufgabe.getAufgabenText(), aufgabeDto.getAufgabenText());
        assertEquals(aufgabe.getReihenfolge(), aufgabeDto.getReihenfolge());
        assertEquals(kurseinheit.getId(), aufgabeDto.getKurseinheitId());
        assertNotNull(aufgabeDto.getTeilaufgaben());
        assertEquals(1, aufgabeDto.getTeilaufgaben().size());
        
        // Überprüfen, dass isEinfach true ist, wenn genau eine Teilaufgabe
        assertTrue(aufgabeDto.isEinfach());
    }

    /**
     * Testet die Konvertierung von Aufgabe-Entity mit mehreren Teilaufgaben zu AufgabeDto.
     */
    @Test
    void testAufgabeMitTeilaufgabenToAufgabeDto() {
        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = new Kurseinheit();
        kurseinheit.setId(1L);
        kurseinheit.setName("Einheit 1");
        kurseinheit.setReihenfolge(1);

        // Aufgabe-Entity erstellen
        Aufgabe aufgabe = new Aufgabe();
        aufgabe.setId(1L);
        aufgabe.setTitel("Aufgabe mit Teilaufgaben");
        aufgabe.setAufgabenText("Dies ist eine Aufgabe mit mehreren Teilaufgaben.");
        aufgabe.setReihenfolge(1);
        aufgabe.setKurseinheit(kurseinheit);

        // Teilaufgaben erstellen
        Teilaufgabe teilaufgabe1 = new Teilaufgabe();
        teilaufgabe1.setId(1L);
        teilaufgabe1.setReihenfolge(1);
        teilaufgabe1.setAufgabenstellungMarkdown("Teilaufgabe 1 Markdown");
        teilaufgabe1.setMusterloesungBewertungshinweise("Bewertungshinweise 1");
        teilaufgabe1.setAufgabe(aufgabe);

        Teilaufgabe teilaufgabe2 = new Teilaufgabe();
        teilaufgabe2.setId(2L);
        teilaufgabe2.setReihenfolge(2);
        teilaufgabe2.setAufgabenstellungMarkdown("Teilaufgabe 2 Markdown");
        teilaufgabe2.setMusterloesungBewertungshinweise("Bewertungshinweise 2");
        teilaufgabe2.setAufgabe(aufgabe);

        List<Teilaufgabe> teilaufgaben = new ArrayList<>();
        teilaufgaben.add(teilaufgabe1);
        teilaufgaben.add(teilaufgabe2);
        aufgabe.setTeilaufgaben(teilaufgaben);

        // Entity zu DTO konvertieren
        AufgabeDto aufgabeDto = aufgabeMapper.toDto(aufgabe);

        // Ergebnisse überprüfen
        assertNotNull(aufgabeDto);
        assertEquals(aufgabe.getId(), aufgabeDto.getId());
        assertEquals(aufgabe.getTitel(), aufgabeDto.getTitel());
        assertEquals(aufgabe.getAufgabenText(), aufgabeDto.getAufgabenText());
        assertEquals(aufgabe.getReihenfolge(), aufgabeDto.getReihenfolge());
        assertEquals(kurseinheit.getId(), aufgabeDto.getKurseinheitId());
        
        // Überprüfen, dass isEinfach false ist, wenn mehrere Teilaufgaben
        assertFalse(aufgabeDto.isEinfach());
        
        // Teilaufgaben überprüfen
        assertNotNull(aufgabeDto.getTeilaufgaben());
        assertEquals(2, aufgabeDto.getTeilaufgaben().size());
        
        // Teilaufgabe 1 überprüfen
        TeilaufgabeDto teilaufgabeDto1 = aufgabeDto.getTeilaufgaben().get(0);
        assertEquals(teilaufgabe1.getId(), teilaufgabeDto1.getId());
        assertEquals(teilaufgabe1.getReihenfolge(), teilaufgabeDto1.getReihenfolge());
        assertEquals(teilaufgabe1.getAufgabenstellungMarkdown(), teilaufgabeDto1.getAufgabenstellungMarkdown());
        assertEquals(teilaufgabe1.getMusterloesungBewertungshinweise(), teilaufgabeDto1.getMusterloesungBewertungshinweise());
        assertEquals(aufgabe.getId(), teilaufgabeDto1.getAufgabeId());
        
        // Teilaufgabe 2 überprüfen
        TeilaufgabeDto teilaufgabeDto2 = aufgabeDto.getTeilaufgaben().get(1);
        assertEquals(teilaufgabe2.getId(), teilaufgabeDto2.getId());
        assertEquals(teilaufgabe2.getReihenfolge(), teilaufgabeDto2.getReihenfolge());
        assertEquals(teilaufgabe2.getAufgabenstellungMarkdown(), teilaufgabeDto2.getAufgabenstellungMarkdown());
        assertEquals(teilaufgabe2.getMusterloesungBewertungshinweise(), teilaufgabeDto2.getMusterloesungBewertungshinweise());
        assertEquals(aufgabe.getId(), teilaufgabeDto2.getAufgabeId());
    }

    /**
     * Testet die Konvertierung von AufgabeDto zu Aufgabe-Entity.
     */
    @Test
    void testAufgabeDtoToAufgabe() {
        // AufgabeDto erstellen
        AufgabeDto aufgabeDto = new AufgabeDto();
        aufgabeDto.setId(1L);
        aufgabeDto.setTitel("Aufgabe 1");
        aufgabeDto.setAufgabenText(null);
        aufgabeDto.setReihenfolge(1);
        aufgabeDto.setKurseinheitId(1L);
        aufgabeDto.setEinfach(true); // Setzen des einfach Felds

        // DTO zu Entity konvertieren
        Aufgabe aufgabe = aufgabeMapper.toEntity(aufgabeDto);

        // Ergebnisse überprüfen
        assertNotNull(aufgabe);
        assertEquals(aufgabeDto.getId(), aufgabe.getId());
        assertEquals(aufgabeDto.getTitel(), aufgabe.getTitel());
        assertEquals(aufgabeDto.getAufgabenText(), aufgabe.getAufgabenText());
        assertEquals(aufgabeDto.getReihenfolge(), aufgabe.getReihenfolge());
        
        // Bei der Rückkonvertierung wird kurseinheit nicht automatisch gesetzt
        assertNull(aufgabe.getKurseinheit());
        assertNotNull(aufgabe.getTeilaufgaben());
        assertTrue(aufgabe.getTeilaufgaben().isEmpty());
    }

    /**
     * Testet die Konvertierung von einer Liste von Aufgabe-Entities zu einer Liste von AufgabeDtos.
     */
    @Test
    void testAufgabeListToAufgabeDtoList() {
        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = new Kurseinheit();
        kurseinheit.setId(1L);
        kurseinheit.setName("Einheit 1");
        kurseinheit.setReihenfolge(1);

        // Aufgabe-Entities erstellen
        Aufgabe aufgabe1 = new Aufgabe();
        aufgabe1.setId(1L);
        aufgabe1.setTitel("Aufgabe 1");
        aufgabe1.setAufgabenText(null);
        aufgabe1.setReihenfolge(1);
        aufgabe1.setKurseinheit(kurseinheit);
        
        // Eine Teilaufgabe hinzufügen, um "Einfach" zu machen
        Teilaufgabe teilaufgabe1 = new Teilaufgabe();
        teilaufgabe1.setAufgabe(aufgabe1);
        List<Teilaufgabe> teilaufgaben1 = new ArrayList<>();
        teilaufgaben1.add(teilaufgabe1);
        aufgabe1.setTeilaufgaben(teilaufgaben1);

        Aufgabe aufgabe2 = new Aufgabe();
        aufgabe2.setId(2L);
        aufgabe2.setTitel("Aufgabe 2");
        aufgabe2.setAufgabenText("Aufgabentext für Aufgabe 2");
        aufgabe2.setReihenfolge(2);
        aufgabe2.setKurseinheit(kurseinheit);
        
        // Mehrere Teilaufgaben hinzufügen, um "Nicht Einfach" zu machen
        Teilaufgabe teilaufgabe2a = new Teilaufgabe();
        teilaufgabe2a.setAufgabe(aufgabe2);
        Teilaufgabe teilaufgabe2b = new Teilaufgabe();
        teilaufgabe2b.setAufgabe(aufgabe2);
        List<Teilaufgabe> teilaufgaben2 = new ArrayList<>();
        teilaufgaben2.add(teilaufgabe2a);
        teilaufgaben2.add(teilaufgabe2b);
        aufgabe2.setTeilaufgaben(teilaufgaben2);

        List<Aufgabe> aufgaben = Arrays.asList(aufgabe1, aufgabe2);

        // Entities zu DTOs konvertieren
        List<AufgabeDto> aufgabeDtos = aufgabeMapper.toDtoList(aufgaben);

        // Ergebnisse überprüfen
        assertNotNull(aufgabeDtos);
        assertEquals(2, aufgabeDtos.size());
        
        // Erste Aufgabe überprüfen
        assertEquals(aufgabe1.getId(), aufgabeDtos.get(0).getId());
        assertEquals(aufgabe1.getTitel(), aufgabeDtos.get(0).getTitel());
        assertEquals(aufgabe1.getAufgabenText(), aufgabeDtos.get(0).getAufgabenText());
        assertEquals(aufgabe1.getReihenfolge(), aufgabeDtos.get(0).getReihenfolge());
        assertEquals(kurseinheit.getId(), aufgabeDtos.get(0).getKurseinheitId());
        assertTrue(aufgabeDtos.get(0).isEinfach()); // Überprüfen, dass isEinfach=true für genau eine Teilaufgabe
        
        // Zweite Aufgabe überprüfen
        assertEquals(aufgabe2.getId(), aufgabeDtos.get(1).getId());
        assertEquals(aufgabe2.getTitel(), aufgabeDtos.get(1).getTitel());
        assertEquals(aufgabe2.getAufgabenText(), aufgabeDtos.get(1).getAufgabenText());
        assertEquals(aufgabe2.getReihenfolge(), aufgabeDtos.get(1).getReihenfolge());
        assertEquals(kurseinheit.getId(), aufgabeDtos.get(1).getKurseinheitId());
        assertFalse(aufgabeDtos.get(1).isEinfach()); // Überprüfen, dass isEinfach=false für mehrere Teilaufgaben
    }
}