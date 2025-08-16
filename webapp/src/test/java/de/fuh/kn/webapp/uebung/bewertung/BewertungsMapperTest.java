package de.fuh.kn.webapp.uebung.bewertung;

import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsMapper;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsRequestDto;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsResponseDto;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.LoesungsVersuch;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BewertungsMapperTest {

    private BewertungsMapper mapper;
    private Aufgabe aufgabe;
    private Teilaufgabe teilaufgabe;
    private LoesungsVersuch loesungsVersuch;
    private BewertungsResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Mapper instanziieren
        mapper = Mappers.getMapper(BewertungsMapper.class);
        
        // Test-Entitäten vorbereiten
        aufgabe = new Aufgabe();
        aufgabe.setId(1L);
        aufgabe.setTitel("Test-Aufgabe");
        aufgabe.setAufgabenText("Dies ist eine Test-Aufgabe zur Mathematik");
        
        teilaufgabe = new Teilaufgabe();
        teilaufgabe.setId(2L);
        teilaufgabe.setAufgabe(aufgabe);
        teilaufgabe.setAufgabenstellungMarkdown("Berechnen Sie 2 + 2 = ?");
        teilaufgabe.setMusterloesungBewertungshinweise("Auf Rechenregeln achten");
        
        // Musterlösung
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("Antwort", "4");
        teilaufgabe.setMusterloesungFelder(musterloesungFelder);
        
        // Student
        Student student = new Student();
        student.setId(3L);
        
        // LoesungsVersuch
        loesungsVersuch = new LoesungsVersuch();
        loesungsVersuch.setId(4L);
        loesungsVersuch.setStudent(student);
        loesungsVersuch.setTeilaufgabe(teilaufgabe);
        loesungsVersuch.setZeitpunkt(LocalDateTime.now());
        
        // Studentenantwort
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("Antwort", "5");
        loesungsVersuch.setLoesungFelder(loesungFelder);
        
        // BewertungsResponseDto
        responseDto = new BewertungsResponseDto();
        responseDto.setPunkte(0);
        responseDto.setFeedback("Die Antwort ist falsch.");
        Map<String, String> felderBewertung = new HashMap<>();
        felderBewertung.put("Antwort", "red");
        responseDto.setFelderBewertung(felderBewertung);
        responseDto.setInputToken(450);
        responseDto.setOutputToken(120);
        responseDto.setCost(new BigDecimal("0.00125"));
    }

    @Test
    @DisplayName("Test der createRequestDto Methode")
    void testCreateRequestDto() {
        // Act
        BewertungsRequestDto requestDto = mapper.createRequestDto(teilaufgabe, loesungsVersuch);
        
        // Assert
        assertNotNull(requestDto);
        assertEquals(2L, requestDto.getTeilaufgabeId());
        assertEquals("Dies ist eine Test-Aufgabe zur Mathematik", requestDto.getAufgabenstellungAufgabe());
        assertEquals("Berechnen Sie 2 + 2 = ?", requestDto.getAufgabenstellungTeilaufgabe());
        assertEquals("Auf Rechenregeln achten", requestDto.getBewertungshinweise());
        
        // Maps überprüfen
        assertEquals(1, requestDto.getMusterloesungFelder().size());
        assertEquals("4", requestDto.getMusterloesungFelder().get("Antwort"));
        
        assertEquals(1, requestDto.getLoesungFelder().size());
        assertEquals("5", requestDto.getLoesungFelder().get("Antwort"));
    }

    @Test
    @DisplayName("Test der updateLoesungsVersuch Methode")
    void testUpdateLoesungsVersuch() {
        // Act
        LoesungsVersuch updatedVersuch = mapper.updateLoesungsVersuch(loesungsVersuch, responseDto);
        
        // Assert
        assertNotNull(updatedVersuch);
        assertEquals(0, updatedVersuch.getBewertungPunkte());
        assertEquals("Die Antwort ist falsch.", updatedVersuch.getBewertungFeedback());
        
        // Map überprüfen
        assertEquals(1, updatedVersuch.getBewertungFelderFarbe().size());
        assertEquals("red", updatedVersuch.getBewertungFelderFarbe().get("Antwort"));
        
        // Token überprüfen
        assertEquals(570, updatedVersuch.getInputToken()+updatedVersuch.getOutputToken()); // 450 + 120
    }

    @Test
    @DisplayName("Test der sumTokens Hilfsmethode mit normalen Werten")
    void testSumTokens() {
        // Act
        Integer sum = mapper.sumTokens(100, 50);
        
        // Assert
        assertEquals(150, sum);
    }

    @Test
    @DisplayName("Test der sumTokens Hilfsmethode mit null-Werten")
    void testSumTokensWithNulls() {
        // Act & Assert
        assertEquals(50, mapper.sumTokens(null, 50));
        assertEquals(100, mapper.sumTokens(100, null));
        assertNull(mapper.sumTokens(null, null));
    }
    
    // Note: The BewertungsMapper interface doesn't actually implement toDto in a way
    // that maps the entity automatically - this would need a custom implementation.
    // Instead, we'll test the update method which is the primary functionality.
    @Test
    @DisplayName("Test der updateLoesungsVersuch Methode (statt Entity-zu-DTO)")
    void testUpdateEntityWithDto() {
        // Vorbereitung
        BewertungsResponseDto dto = new BewertungsResponseDto();
        dto.setPunkte(75);
        dto.setFeedback("Gut gemacht!");
        Map<String, String> felderBewertung = new HashMap<>();
        felderBewertung.put("Antwort", "yellow");
        dto.setFelderBewertung(felderBewertung);
        dto.setInputToken(300);
        dto.setOutputToken(270);
        
        // Act
        LoesungsVersuch updated = mapper.updateLoesungsVersuch(loesungsVersuch, dto);
        
        // Assert
        assertNotNull(updated);
        assertEquals(75, updated.getBewertungPunkte());
        assertEquals("Gut gemacht!", updated.getBewertungFeedback());
        assertEquals("yellow", updated.getBewertungFelderFarbe().get("Antwort"));
        assertEquals(570, updated.getInputToken()+updated.getOutputToken()); // 300 + 270
    }
    
    // This test is not applicable as there's no fromDto method in the interface
    // The BewertungsMapper only handles entity-to-dto and doesn't convert back directly
    // Instead, it updates existing entities with updateLoesungsVersuch method
}