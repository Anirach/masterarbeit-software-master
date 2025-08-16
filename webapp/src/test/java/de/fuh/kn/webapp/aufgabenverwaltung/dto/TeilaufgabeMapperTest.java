package de.fuh.kn.webapp.aufgabenverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für TeilaufgabeMapper.
 */
@SpringBootTest
class TeilaufgabeMapperTest {

    @Autowired
    private TeilaufgabeMapper teilaufgabeMapper;

    /**
     * Testet die Konvertierung von Teilaufgabe-Entity zu TeilaufgabeDto.
     */
    @Test
    void testTeilaufgabeToTeilaufgabeDto() {
        // Aufgabe-Entity erstellen
        Aufgabe aufgabe = new Aufgabe();
        aufgabe.setId(1L);
        aufgabe.setTitel("Aufgabe 1");

        // Teilaufgabe-Entity erstellen
        Teilaufgabe teilaufgabe = new Teilaufgabe();
        teilaufgabe.setId(1L);
        teilaufgabe.setReihenfolge(1);
        teilaufgabe.setAufgabenstellungMarkdown("# Teilaufgabe Markdown\n\nHier ist eine Aufgabe: {{field1}}");
        teilaufgabe.setMusterloesungBewertungshinweise("Bewertungshinweise für Teilaufgabe 1");
        teilaufgabe.setAufgabe(aufgabe);

        // Musterlösungen hinzufügen
        Map<String, String> musterloesungen = new HashMap<>();
        musterloesungen.put("field1", "Lösung für Feld 1");
        musterloesungen.put("field2", "Lösung für Feld 2");
        teilaufgabe.setMusterloesungFelder(musterloesungen);

        // Entity zu DTO konvertieren
        TeilaufgabeDto teilaufgabeDto = teilaufgabeMapper.toDto(teilaufgabe);

        // Ergebnisse überprüfen
        assertNotNull(teilaufgabeDto);
        assertEquals(teilaufgabe.getId(), teilaufgabeDto.getId());
        assertEquals(teilaufgabe.getReihenfolge(), teilaufgabeDto.getReihenfolge());
        assertEquals(teilaufgabe.getAufgabenstellungMarkdown(), teilaufgabeDto.getAufgabenstellungMarkdown());
        assertEquals(teilaufgabe.getMusterloesungBewertungshinweise(), teilaufgabeDto.getMusterloesungBewertungshinweise());
        assertEquals(aufgabe.getId(), teilaufgabeDto.getAufgabeId());
        
        // Musterlösungen überprüfen
        assertNotNull(teilaufgabeDto.getMusterloesungFelder());
        assertEquals(2, teilaufgabeDto.getMusterloesungFelder().size());
        assertEquals("Lösung für Feld 1", teilaufgabeDto.getMusterloesungFelder().get("field1"));
        assertEquals("Lösung für Feld 2", teilaufgabeDto.getMusterloesungFelder().get("field2"));
    }

    /**
     * Testet die Konvertierung von TeilaufgabeDto zu Teilaufgabe-Entity.
     */
    @Test
    void testTeilaufgabeDtoToTeilaufgabe() {
        // TeilaufgabeDto erstellen
        TeilaufgabeDto teilaufgabeDto = new TeilaufgabeDto();
        teilaufgabeDto.setId(1L);
        teilaufgabeDto.setReihenfolge(1);
        teilaufgabeDto.setAufgabenstellungMarkdown("# Teilaufgabe Markdown\n\nHier ist eine Aufgabe: {{field1}}");
        teilaufgabeDto.setMusterloesungBewertungshinweise("Bewertungshinweise für Teilaufgabe 1");
        teilaufgabeDto.setAufgabeId(1L);

        // Musterlösungen hinzufügen
        Map<String, String> musterloesungen = new HashMap<>();
        musterloesungen.put("field1", "Lösung für Feld 1");
        musterloesungen.put("field2", "Lösung für Feld 2");
        teilaufgabeDto.setMusterloesungFelder(musterloesungen);

        // DTO zu Entity konvertieren
        Teilaufgabe teilaufgabe = teilaufgabeMapper.toEntity(teilaufgabeDto);

        // Ergebnisse überprüfen
        assertNotNull(teilaufgabe);
        assertEquals(teilaufgabeDto.getId(), teilaufgabe.getId());
        assertEquals(teilaufgabeDto.getReihenfolge(), teilaufgabe.getReihenfolge());
        assertEquals(teilaufgabeDto.getAufgabenstellungMarkdown(), teilaufgabe.getAufgabenstellungMarkdown());
        assertEquals(teilaufgabeDto.getMusterloesungBewertungshinweise(), teilaufgabe.getMusterloesungBewertungshinweise());
        
        // Bei der Rückkonvertierung wird aufgabe nicht automatisch gesetzt
        assertNull(teilaufgabe.getAufgabe());
        
        // Musterlösungen überprüfen
        assertNotNull(teilaufgabe.getMusterloesungFelder());
        assertEquals(2, teilaufgabe.getMusterloesungFelder().size());
        assertEquals("Lösung für Feld 1", teilaufgabe.getMusterloesungFelder().get("field1"));
        assertEquals("Lösung für Feld 2", teilaufgabe.getMusterloesungFelder().get("field2"));
    }

    /**
     * Testet die Konvertierung von einer Liste von Teilaufgabe-Entities zu einer Liste von TeilaufgabeDtos.
     */
    @Test
    void testTeilaufgabeListToTeilaufgabeDtoList() {
        // Aufgabe-Entity erstellen
        Aufgabe aufgabe = new Aufgabe();
        aufgabe.setId(1L);
        aufgabe.setTitel("Aufgabe 1");

        // Teilaufgabe-Entities erstellen
        Teilaufgabe teilaufgabe1 = new Teilaufgabe();
        teilaufgabe1.setId(1L);
        teilaufgabe1.setReihenfolge(1);
        teilaufgabe1.setAufgabenstellungMarkdown("# Teilaufgabe 1 Markdown");
        teilaufgabe1.setMusterloesungBewertungshinweise("Bewertungshinweise für Teilaufgabe 1");
        teilaufgabe1.setAufgabe(aufgabe);

        Map<String, String> musterloesungen1 = new HashMap<>();
        musterloesungen1.put("field1", "Lösung für Feld 1");
        teilaufgabe1.setMusterloesungFelder(musterloesungen1);

        Teilaufgabe teilaufgabe2 = new Teilaufgabe();
        teilaufgabe2.setId(2L);
        teilaufgabe2.setReihenfolge(2);
        teilaufgabe2.setAufgabenstellungMarkdown("# Teilaufgabe 2 Markdown");
        teilaufgabe2.setMusterloesungBewertungshinweise("Bewertungshinweise für Teilaufgabe 2");
        teilaufgabe2.setAufgabe(aufgabe);

        Map<String, String> musterloesungen2 = new HashMap<>();
        musterloesungen2.put("field2", "Lösung für Feld 2");
        teilaufgabe2.setMusterloesungFelder(musterloesungen2);

        List<Teilaufgabe> teilaufgaben = Arrays.asList(teilaufgabe1, teilaufgabe2);

        // Entities zu DTOs konvertieren
        List<TeilaufgabeDto> teilaufgabeDtos = teilaufgabeMapper.toDtoList(teilaufgaben);

        // Ergebnisse überprüfen
        assertNotNull(teilaufgabeDtos);
        assertEquals(2, teilaufgabeDtos.size());
        
        // Erste Teilaufgabe überprüfen
        TeilaufgabeDto teilaufgabeDto1 = teilaufgabeDtos.get(0);
        assertEquals(teilaufgabe1.getId(), teilaufgabeDto1.getId());
        assertEquals(teilaufgabe1.getReihenfolge(), teilaufgabeDto1.getReihenfolge());
        assertEquals(teilaufgabe1.getAufgabenstellungMarkdown(), teilaufgabeDto1.getAufgabenstellungMarkdown());
        assertEquals(teilaufgabe1.getMusterloesungBewertungshinweise(), teilaufgabeDto1.getMusterloesungBewertungshinweise());
        assertEquals(aufgabe.getId(), teilaufgabeDto1.getAufgabeId());
        assertNotNull(teilaufgabeDto1.getMusterloesungFelder());
        assertEquals(1, teilaufgabeDto1.getMusterloesungFelder().size());
        assertEquals("Lösung für Feld 1", teilaufgabeDto1.getMusterloesungFelder().get("field1"));
        
        // Zweite Teilaufgabe überprüfen
        TeilaufgabeDto teilaufgabeDto2 = teilaufgabeDtos.get(1);
        assertEquals(teilaufgabe2.getId(), teilaufgabeDto2.getId());
        assertEquals(teilaufgabe2.getReihenfolge(), teilaufgabeDto2.getReihenfolge());
        assertEquals(teilaufgabe2.getAufgabenstellungMarkdown(), teilaufgabeDto2.getAufgabenstellungMarkdown());
        assertEquals(teilaufgabe2.getMusterloesungBewertungshinweise(), teilaufgabeDto2.getMusterloesungBewertungshinweise());
        assertEquals(aufgabe.getId(), teilaufgabeDto2.getAufgabeId());
        assertNotNull(teilaufgabeDto2.getMusterloesungFelder());
        assertEquals(1, teilaufgabeDto2.getMusterloesungFelder().size());
        assertEquals("Lösung für Feld 2", teilaufgabeDto2.getMusterloesungFelder().get("field2"));
    }

    /**
     * Testet die Konvertierung von Teilaufgabe-Entity zu TeilaufgabeDto mit leeren Musterlösungsfeldern.
     */
    @Test
    void testTeilaufgabeToTeilaufgabeDtoWithEmptyMusterloesungen() {
        // Aufgabe-Entity erstellen
        Aufgabe aufgabe = new Aufgabe();
        aufgabe.setId(1L);
        aufgabe.setTitel("Aufgabe 1");
        
        // Teilaufgabe-Entity erstellen
        Teilaufgabe teilaufgabe = new Teilaufgabe();
        teilaufgabe.setId(1L);
        teilaufgabe.setReihenfolge(1);
        teilaufgabe.setAufgabenstellungMarkdown("# Teilaufgabe Markdown");
        teilaufgabe.setAufgabe(aufgabe);
        // Keine Musterlösungen oder Bewertungshinweise

        // Entity zu DTO konvertieren
        TeilaufgabeDto teilaufgabeDto = teilaufgabeMapper.toDto(teilaufgabe);

        // Ergebnisse überprüfen
        assertNotNull(teilaufgabeDto);
        assertEquals(teilaufgabe.getId(), teilaufgabeDto.getId());
        assertEquals(teilaufgabe.getReihenfolge(), teilaufgabeDto.getReihenfolge());
        assertEquals(teilaufgabe.getAufgabenstellungMarkdown(), teilaufgabeDto.getAufgabenstellungMarkdown());
        assertNull(teilaufgabeDto.getMusterloesungBewertungshinweise());
        assertEquals(aufgabe.getId(), teilaufgabeDto.getAufgabeId());
        
        // Musterlösungen überprüfen
        assertNotNull(teilaufgabeDto.getMusterloesungFelder());
        assertTrue(teilaufgabeDto.getMusterloesungFelder().isEmpty());
    }
}