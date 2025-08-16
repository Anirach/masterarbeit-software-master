package de.fuh.kn.webapp.llm.rag.modules;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AufgabeContextQueryAugmenterTest {
    
    private AufgabeContextQueryAugmenter augmenter;
    private Query testQuery;
    private AufgabeDto testAufgabe;
    private TeilaufgabeDto testTeilaufgabe;
    private LoesungsVersuchDTO testLoesungsversuch;
    private List<Document> testDocuments;
    
    @BeforeEach
    void setUp() {
        augmenter = new AufgabeContextQueryAugmenter();
        
        // Test-Aufgabe erstellen
        testAufgabe = new AufgabeDto();
        testAufgabe.setId(1L);
        testAufgabe.setTitel("Testaufgabe");
        testAufgabe.setAufgabenText("Dies ist eine Testaufgabe über Java-Programmierung.");
        
        // Test-Teilaufgabe erstellen
        testTeilaufgabe = new TeilaufgabeDto();
        testTeilaufgabe.setId(1L);
        testTeilaufgabe.setAufgabenstellungMarkdown("Implementieren Sie eine Methode, die [[add:text]] berechnet.");
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("add", "zwei Zahlen addiert");
        testTeilaufgabe.setMusterloesungFelder(musterloesungFelder);
        testTeilaufgabe.setMusterloesungBewertungshinweise("Die Methode sollte korrekt zwei Zahlen addieren und das Ergebnis zurückgeben.");
        
        // Test-Lösungsversuch erstellen
        testLoesungsversuch = new LoesungsVersuchDTO();
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("add", "die Summe zweier Zahlen");
        testLoesungsversuch.setLoesungFelder(loesungFelder);
        testLoesungsversuch.setBewertungPunkte(80);
        testLoesungsversuch.setBewertungFeedback("Gute Lösung, aber die Formulierung könnte präziser sein.");
        
        // Test-Dokumente erstellen
        testDocuments = new ArrayList<>();
        Document doc1 = new Document("Dies ist ein relevantes Dokument über Addition in Java.");
        doc1.getMetadata().put("file_name", "java-basics.pdf");
        doc1.getMetadata().put("page_number", "5");
        testDocuments.add(doc1);
        
        Document doc2 = new Document("Hier werden mathematische Operationen erklärt.");
        doc2.getMetadata().put("file_name", "math-operations.pdf");
        doc2.getMetadata().put("page_number", "12");
        testDocuments.add(doc2);
    }
    
    @Test
    void augment_MitAllenDaten_ErstelltErweiterteQuery() {
        // Arrange
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.of(testLoesungsversuch));
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Wie implementiere ich die Addition?")
                .context(context)
                .build();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, testDocuments);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        
        // Prüfen, ob die wichtigen Elemente im erweiterten Text enthalten sind
        assertTrue(augmentedText.contains("Wie implementiere ich die Addition?"));
        assertTrue(augmentedText.contains("Dies ist eine Testaufgabe über Java-Programmierung."));
        assertTrue(augmentedText.contains("Implementieren Sie eine Methode"));
        assertTrue(augmentedText.contains("add: zwei Zahlen addiert"));
        assertTrue(augmentedText.contains("Die Methode sollte korrekt zwei Zahlen addieren"));
        assertTrue(augmentedText.contains("add: die Summe zweier Zahlen"));
        assertTrue(augmentedText.contains("Erhaltene Punkte: 80/100"));
        assertTrue(augmentedText.contains("Gute Lösung, aber die Formulierung könnte präziser sein."));
        assertTrue(augmentedText.contains("[fileName=java-basics.pdf,page=5]"));
        assertTrue(augmentedText.contains("Dies ist ein relevantes Dokument über Addition in Java."));
    }
    
    @Test
    void augment_MitExplanationMode_FuegtErklaerungenHinzu() {
        // Arrange
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.empty());
        context.put("EXPLANATION_MODE", true);
        
        testQuery = Query.builder()
                .text("Was bedeutet Addition?")
                .context(context)
                .build();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, testDocuments);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        
        // Prüfen, ob Erklärungsmodus-Text hinzugefügt wurde
        assertTrue(augmentedText.contains("Bitte erkläre die Konzepte dieser Aufgabe ausführlich"));
        assertTrue(augmentedText.contains("typische Schwierigkeiten"));
        assertTrue(augmentedText.contains("hilfreiche Lösungsansätze"));
    }
    
    @Test
    void augment_OhneDokumente_ZeigtKeineRelevantenDokumente() {
        // Arrange
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.empty());
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Test Frage")
                .context(context)
                .build();
        
        List<Document> emptyDocuments = new ArrayList<>();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, emptyDocuments);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        assertTrue(augmentedText.contains("Für diese Anfrage existiert kein relevantes Kursmaterial"));
    }
    
    @Test
    void augment_OhneLoesungsversuch_ZeigtKeinenLoesungsversuch() {
        // Arrange
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.empty());
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Test Frage")
                .context(context)
                .build();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, testDocuments);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        assertTrue(augmentedText.contains("Bisher kein Lösungsversuch"));
    }
    
    @Test
    void augment_MitNullQuery_WirftException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            augmenter.augment(null, testDocuments)
        );
    }
    
    @Test
    void augment_MitNullDocuments_WirftException() {
        // Arrange
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.empty());
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Test Frage")
                .context(context)
                .build();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            augmenter.augment(testQuery, null)
        );
    }
    
    @Test
    void augment_MitAufgabeOhneAufgabentext_FunktioniertTrotzdem() {
        // Arrange
        testAufgabe.setAufgabenText(null); // Kein Aufgabentext
        
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.empty());
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Test Frage")
                .context(context)
                .build();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, testDocuments);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        // Sollte trotzdem die Teilaufgabe enthalten
        assertTrue(augmentedText.contains("Implementieren Sie eine Methode"));
    }
    
    @Test
    void augment_MitMehrerenMusterloesungsFeldern_AlleWerdenHinzugefuegt() {
        // Arrange
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("add", "zwei Zahlen addiert");
        musterloesungFelder.put("multiply", "zwei Zahlen multipliziert");
        musterloesungFelder.put("divide", "zwei Zahlen dividiert");
        testTeilaufgabe.setMusterloesungFelder(musterloesungFelder);
        testTeilaufgabe.setAufgabenstellungMarkdown("Implementieren Sie Methoden, die [[add:text]], [[multiply:text]] und [[divide:text]].");
        
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.empty());
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Test Frage")
                .context(context)
                .build();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, testDocuments);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        assertTrue(augmentedText.contains("add: zwei Zahlen addiert"));
        assertTrue(augmentedText.contains("multiply: zwei Zahlen multipliziert"));
        assertTrue(augmentedText.contains("divide: zwei Zahlen dividiert"));
    }
    
    @Test
    void augment_MitLoesungsversuchOhnePunkte_HandhabtNullWerte() {
        // Arrange
        testLoesungsversuch.setBewertungPunkte(null);
        testLoesungsversuch.setBewertungFeedback(null);
        
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.of(testLoesungsversuch));
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Test Frage")
                .context(context)
                .build();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, testDocuments);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        // Sollte trotzdem funktionieren und null-Werte anzeigen
        assertTrue(augmentedText.contains("Erhaltene Punkte: null/100"));
        assertTrue(augmentedText.contains("Erhaltenes Feedback: null"));
    }
    
    @Test
    void augment_DokumentFormatierung_EntferntUeberfluessigeLeerzeichen() {
        // Arrange
        Document docWithSpaces = new Document("Dies    ist    ein    Text    mit    vielen    Leerzeichen.");
        docWithSpaces.getMetadata().put("file_name", "spaces.pdf");
        docWithSpaces.getMetadata().put("page_number", "1");
        List<Document> docsWithSpaces = Arrays.asList(docWithSpaces);
        
        Map<String, Object> context = new HashMap<>();
        context.put("AUFGABE", testAufgabe);
        context.put("TEILAUFGABE", testTeilaufgabe);
        context.put("LOESUNGSVERSUCH", Optional.empty());
        context.put("EXPLANATION_MODE", false);
        
        testQuery = Query.builder()
                .text("Test Frage")
                .context(context)
                .build();
        
        // Act
        Query augmentedQuery = augmenter.augment(testQuery, docsWithSpaces);
        
        // Assert
        assertNotNull(augmentedQuery);
        String augmentedText = augmentedQuery.text();
        // Leerzeichen sollten normalisiert sein
        assertTrue(augmentedText.contains("Dies ist ein Text mit vielen Leerzeichen."));
        assertFalse(augmentedText.contains("Dies    ist    ein"));
    }
}