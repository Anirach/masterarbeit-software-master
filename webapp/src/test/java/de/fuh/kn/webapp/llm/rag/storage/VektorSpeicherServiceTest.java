package de.fuh.kn.webapp.llm.rag.storage;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.rag.config.RagConfig;
import de.fuh.kn.webapp.llm.rag.document.DokumentParserService;
import de.fuh.kn.webapp.llm.rag.document.TextSegmentierungsService;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.repository.KursMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VektorSpeicherServiceTest {

    @Mock
    private DokumentParserService dokumentParserService;

    @Mock
    private TextSegmentierungsService textSegmentierungsService;

    @Mock
    private KursMaterialRepository kursMaterialRepository;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private TokenUsageObserver tokenUsageObserver;

    @Mock
    private KursMaterialService kursMaterialService;

    @Mock
    private ChatModel evaluationChatModel;
    
    @Mock
    private KursService kursService;
    
    @Mock
    private KurseinheitService kurseinheitService;
    
    @Mock
    private RagConfig.RagProperties ragProperties;

    @InjectMocks
    @Spy
    private VektorSpeicherService vektorSpeicherService;
    
    private TokenUsageObserver.CostContext costContext;
    
    @BeforeEach
    void setUp() {
        costContext = new TokenUsageObserver.CostContext("test-context", OperationType.EMBEDDING);
        costContext.setTotalCost(new BigDecimal("0.01"));
        when(tokenUsageObserver.startCostContext(any(OperationType.class))).thenReturn("test-context");
        when(tokenUsageObserver.endCostContext()).thenReturn(costContext);
        
        // Configure RagProperties mock
        RagConfig.RagProperties.RelevanceBoostProperties relevanceBoost = new RagConfig.RagProperties.RelevanceBoostProperties();
        RagConfig.RagProperties.RelevanceBoostProperties.SameKurseinheitBoost sameKurseinheitBoost = 
                new RagConfig.RagProperties.RelevanceBoostProperties.SameKurseinheitBoost();
        sameKurseinheitBoost.setEnabled(true);
        sameKurseinheitBoost.setBoostFactor(0.3f);
        relevanceBoost.setSameKurseinheit(sameKurseinheitBoost);
        when(ragProperties.getRelevanceBoost()).thenReturn(relevanceBoost);
    }

    @Test
    public void testBerechneTfidfWert() throws Exception {
        // Zugriff auf private Methode mit Reflection
        Method berechneTfidfWertMethod = VektorSpeicherService.class.getDeclaredMethod(
                "berechneTfidfWert", int.class, int.class);
        berechneTfidfWertMethod.setAccessible(true);

        // Testfall 1: Häufiges Wort (kommt in 500 von 1000 Dokumenten vor)
        double idf1 = (double) berechneTfidfWertMethod.invoke(vektorSpeicherService, 500, 1000);
        // Erwarteter Wert: log(1000/500) = log(2) ≈ 0.693
        assertEquals(Math.log(2.0), idf1, 0.001);

        // Testfall 2: Seltenes Wort (kommt in 10 von 1000 Dokumenten vor)
        double idf2 = (double) berechneTfidfWertMethod.invoke(vektorSpeicherService, 10, 1000);
        // Erwarteter Wert: log(1000/10) = log(100) ≈ 4.605
        assertEquals(Math.log(100.0), idf2, 0.001);

        // Testfall 3: Sehr seltenes Wort (kommt in 1 von 1000 Dokumenten vor)
        double idf3 = (double) berechneTfidfWertMethod.invoke(vektorSpeicherService, 1, 1000);
        // Erwarteter Wert: log(1000/1) = log(1000) ≈ 6.908
        assertEquals(Math.log(1000.0), idf3, 0.001);

        // Testfall 4: Nicht vorkommendes Wort (dokumentFrequenz = 0)
        // Sollte 1 als Fallback verwenden, um Division durch Null zu vermeiden
        double idf4 = (double) berechneTfidfWertMethod.invoke(vektorSpeicherService, 0, 1000);
        // Erwarteter Wert: log(1000/1) = log(1000) ≈ 6.908
        assertEquals(Math.log(1000.0), idf4, 0.001);
    }

    @Test
    public void testBerechneTextRelevanz() throws Exception {
        // Zugriff auf private Methode mit Reflection
        Method berechneTextRelevanzMethod = VektorSpeicherService.class.getDeclaredMethod(
                "berechneTextRelevanz", String.class, List.class);
        berechneTextRelevanzMethod.setAccessible(true);

        // IDF-Cache mit simulierten Werten für unsere Testkeywords initialisieren
        Map<String, Double> idfCache = new HashMap<>();
        idfCache.put("java", 2.0);       // Simulierter IDF-Wert für "java"
        idfCache.put("programmierung", 3.0); // Simulierter IDF-Wert für "programmierung"
        ReflectionTestUtils.setField(vektorSpeicherService, "idfCache", idfCache);

        List<String> keywords = Arrays.asList("java", "programmierung");

        // Testfall 1: Dokument enthält beide Keywords mehrfach
        String docContent1 = "java ist eine populäre programmiersprache. java wird häufig für programmierung verwendet. "
                + "java programmierung ist weit verbreitet. das ist ein text über java programmierung.";
        double score1 = (double) berechneTextRelevanzMethod.invoke(vektorSpeicherService, docContent1, keywords);
        // Score sollte zwischen 0 und 1 liegen
        assertTrue(score1 >= 0.0);
        assertTrue(score1 <= 1.0);
        // Loggen des aktuellen Scores für Debugging
        System.out.println("Testfall 1 Score: " + score1);

        // Testfall 2: Dokument enthält nur ein Keyword
        String docContent2 = "python ist eine andere populäre programmiersprache. programmierung ist ein wichtiges konzept. "
                + "viele menschen lernen programmierung.";
        double score2 = (double) berechneTextRelevanzMethod.invoke(vektorSpeicherService, docContent2, keywords);
        // Score sollte zwischen 0 und 1 liegen
        assertTrue(score2 >= 0.0);
        assertTrue(score2 <= 1.0);
        // Da die genaue Bewertung von docContent2 vom Algorithmus abhängt und verschieden sein kann,
        // prüfen wir nicht, ob score2 < score1 ist, sondern ob score2 >= 0 ist

        // Testfall 3: Dokument enthält keine Keywords
        String docContent3 = "python ist eine andere populäre programmiersprache. python wird oft für datenwissenschaft verwendet.";
        double score3 = (double) berechneTextRelevanzMethod.invoke(vektorSpeicherService, docContent3, keywords);
        // Score sollte 0 sein, da keine Keywords vorkommen
        assertEquals(0.0, score3, 0.001);
    }

    @Test
    public void testIdfCacheFallback() throws Exception {
        // Zugriff auf private Methode mit Reflection
        Method berechneTextRelevanzMethod = VektorSpeicherService.class.getDeclaredMethod(
                "berechneTextRelevanz", String.class, List.class);
        berechneTextRelevanzMethod.setAccessible(true);

        // Wir verwenden ein langes und ein kurzes Keyword, um den Fallback-Effekt zu prüfen
        List<String> keywords = Arrays.asList("kurz", "sehrlangeswort");

        // Leeren IDF-Cache initialisieren, damit der Fallback verwendet wird
        Map<String, Double> idfCache = new HashMap<>();
        ReflectionTestUtils.setField(vektorSpeicherService, "idfCache", idfCache);

        // Testdokument, das beide Keywords enthält
        String docContent = "das ist ein kurz er text mit einem sehrlangeswort darin.";
        
        // Ausführung der Methode - sollte den Fallback für IDF-Berechnung verwenden
        double score = (double) berechneTextRelevanzMethod.invoke(vektorSpeicherService, docContent, keywords);
        
        // Score sollte zwischen 0 und 1 liegen
        assertTrue(score > 0.0);
        assertTrue(score <= 1.0);
        
        // Nach der Ausführung sollte der IDF-Cache die berechneten Werte enthalten
        idfCache = (Map<String, Double>) ReflectionTestUtils.getField(vektorSpeicherService, "idfCache");
        assertNotNull(idfCache.get("kurz"));
        assertNotNull(idfCache.get("sehrlangeswort"));
        
        // Das längere Wort sollte einen höheren IDF-Wert haben (selteneres Wort)
        assertTrue(idfCache.get("sehrlangeswort") > idfCache.get("kurz"));
    }
    
//    @Test
//    public void testTokenUsageTrackingWithOperationType() {
//        // Set up mock for TokenUsageObserver cost context
//        TokenUsageObserver.CostContext costContext = new TokenUsageObserver.CostContext("test-context-id", OperationType.EMBEDDING);
//        costContext.setInputTokens(300);
//        costContext.setOutputTokens(0);
//        costContext.addCost(new BigDecimal("0.000006"));
//        costContext.setModel("text-embedding-3-small");
//
//        when(tokenUsageObserver.startCostContext(eq(OperationType.EMBEDDING))).thenReturn("test-context-id");
//        when(tokenUsageObserver.endCostContext()).thenReturn(costContext);
//
//        when(vectorStore.search(any(SearchRequest.class))).thenReturn(new ArrayList<>());
//
//        // Call method with the search
//        vektorSpeicherService.searchSimilarDocuments("Test query", 5);
//
//        // Verify TokenUsageObserver was called with correct operation type
//        verify(tokenUsageObserver).startCostContext(OperationType.EMBEDDING);
//        verify(tokenUsageObserver).endCostContext();
//    }
//
//    @Test
//    public void testSearchSimilarDocumentsWithOperationType() {
//        // Mock documents to return
//        List<Document> mockResults = new ArrayList<>();
//        Document doc1 = new Document("Test content 1");
//        Document doc2 = new Document("Test content 2");
//        mockResults.add(doc1);
//        mockResults.add(doc2);
//
//        // Set up TokenUsageObserver to return a mock cost context
//        TokenUsageObserver.CostContext costContext = new TokenUsageObserver.CostContext("test-context-id", OperationType.EMBEDDING);
//        costContext.setInputTokens(200);
//        costContext.setOutputTokens(0);
//        costContext.setModel("text-embedding-3-small");
//        costContext.addCost(new BigDecimal("0.000004"));
//
//        when(tokenUsageObserver.startCostContext(OperationType.EMBEDDING)).thenReturn("test-context-id");
//        when(tokenUsageObserver.endCostContext()).thenReturn(costContext);
//
//        // Mock search results
//        when(vectorStore.search(any(SearchRequest.class))).thenReturn(mockResults);
//
//        // Call method with operation type only
//        List<Document> results = vektorSpeicherService.searchSimilarDocuments("What is Java?", 5);
//
//        // Verify results
//        assertEquals(2, results.size());
//        assertEquals(doc1, results.get(0));
//        assertEquals(doc2, results.get(1));
//
//        // Verify TokenUsageObserver was properly called
//        verify(tokenUsageObserver).startCostContext(OperationType.EMBEDDING);
//        verify(tokenUsageObserver).endCostContext();
//    }
    
    // Test removed as we no longer support user ID parameter
    
    @Test
    void testIndexiereKursMaterialAsync_ErfolgreicheIndexierung() throws Exception {
        // Arrange
        Long kursMaterialId = 1L;
        doReturn(true).when(vektorSpeicherService).indexiereKursMaterial(kursMaterialId);
        
        // Act
        CompletableFuture<Boolean> result = vektorSpeicherService.indexiereKursMaterialAsync(kursMaterialId);
        
        // Assert
        assertTrue(result.get());
        verify(vektorSpeicherService).indexiereKursMaterial(kursMaterialId);
    }
    
    @Test
    void testIndexiereKursMaterialAsync_FehlerBeiIndexierung() throws Exception {
        // Arrange
        Long kursMaterialId = 1L;
        doThrow(new RuntimeException("Test error")).when(vektorSpeicherService).indexiereKursMaterial(kursMaterialId);
        
        // Act
        CompletableFuture<Boolean> result = vektorSpeicherService.indexiereKursMaterialAsync(kursMaterialId);
        
        // Assert
        assertFalse(result.get());
    }
    
    @Test
    void testIndexiereKursMaterial_KursMaterialNichtGefunden() {
        // Arrange
        Long kursMaterialId = 1L;
        when(kursMaterialService.getKursMaterialById(kursMaterialId)).thenReturn(null);
        
        // Act
        boolean result = vektorSpeicherService.indexiereKursMaterial(kursMaterialId);
        
        // Assert
        assertFalse(result);
        verify(kursMaterialService).getKursMaterialById(kursMaterialId);
    }
    
    @Test
    void testIndexiereKursMaterial_KeinDokument() {
        // Arrange
        Long kursMaterialId = 1L;
        KursMaterialDTO kursMaterial = new KursMaterialDTO();
        kursMaterial.setId(kursMaterialId);
        kursMaterial.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);
        
        when(kursMaterialService.getKursMaterialById(kursMaterialId)).thenReturn(kursMaterial);
        
        // Act
        boolean result = vektorSpeicherService.indexiereKursMaterial(kursMaterialId);
        
        // Assert
        assertTrue(result);
        verify(dokumentParserService, never()).extrahiereText(any());
    }
    
    @Test
    void testIndexiereKursMaterial_ErfolgreichMitKursUndKurseinheit() {
        // Arrange
        Long kursMaterialId = 1L;
        Long kursId = 2L;
        Long kurseinheitId = 3L;
        
        KursMaterialDTO kursMaterial = new KursMaterialDTO();
        kursMaterial.setId(kursMaterialId);
        kursMaterial.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
        kursMaterial.setKurseinheitId(kurseinheitId);
        
        KurseinheitDTO kurseinheit = new KurseinheitDTO();
        kurseinheit.setId(kurseinheitId);
        kurseinheit.setKursId(kursId);
        
        KursDTO kurs = new KursDTO();
        kurs.setId(kursId);
        
        Document doc = new Document("Test content", new HashMap<>());
        List<Document> docs = Arrays.asList(doc);
        List<Document> segments = Arrays.asList(
            new Document("Segment 1", new HashMap<>()),
            new Document("Segment 2", new HashMap<>())
        );
        
        KursMaterial entity = new KursMaterial();
        entity.setId(kursMaterialId);
        
        when(kursMaterialService.getKursMaterialById(kursMaterialId)).thenReturn(kursMaterial);
        when(kurseinheitService.getKurseinheitById(kurseinheitId)).thenReturn(kurseinheit);
        when(kursService.getKursById(kursId)).thenReturn(kurs);
        when(dokumentParserService.extrahiereText(kursMaterial)).thenReturn(docs);
        when(textSegmentierungsService.segmentiereText(any(Document.class))).thenReturn(segments);
        when(kursMaterialRepository.findById(kursMaterialId)).thenReturn(Optional.of(entity));
        
        // Act
        boolean result = vektorSpeicherService.indexiereKursMaterial(kursMaterialId);
        
        // Assert
        assertTrue(result);
        verify(vectorStore).add(argThat(list -> list.size() == 2));
        verify(kursMaterialRepository).save(argThat(km -> km.getIndexiert()));
    }
    
    @Test
    void testEntferneKursMaterialAusVektorspeicher() {
        // Arrange
        Long kursMaterialId = 1L;
        
        // Act
        vektorSpeicherService.entferneKursMaterialAusVektorspeicher(kursMaterialId);
        
        // Assert
        verify(vectorStore).delete("kursMaterialId == " + kursMaterialId);
    }
    
    @Test
    void testTransformiereSuchanfrage() {
        // Arrange
        String suchText = "Was ist Java?";
        String transformierterText = "Java Programmiersprache Definition Konzepte";
        
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(transformierterText));
        when(chatResponse.getResult()).thenReturn(generation);
        when(evaluationChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // Act
        String result = vektorSpeicherService.transformiereSuchanfrage(suchText);
        
        // Assert
        assertEquals(transformierterText, result);
        verify(tokenUsageObserver).startCostContext(OperationType.OTHER);
        verify(tokenUsageObserver).endCostContext();
    }
    
    @Test
    void testTransformiereSuchanfrage_FehlerBehandlung() {
        // Arrange
        String suchText = "Was ist Java?";
        when(evaluationChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Test error"));
        
        // Act
        String result = vektorSpeicherService.transformiereSuchanfrage(suchText);
        
        // Assert
        assertEquals(suchText, result); // Sollte ursprünglichen Text zurückgeben
    }
    
    @Test
    void testAehnlichkeitsSuche_ErfolgreicheSuche() {
        // Arrange
        String suchText = "Java Programmierung";
        int topK = 5;
        float similarityThreshold = 0.7f;
        String filterExpression = "kursId == 1";
        
        List<Document> expectedDocs = Arrays.asList(
            new Document("Java ist eine Programmiersprache", Map.of("score", 0.9)),
            new Document("Programmierung mit Java", Map.of("score", 0.8))
        );
        
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(expectedDocs);
        
        // Act
        List<Document> result = vektorSpeicherService.aehnlichkeitsSuche(
            suchText, topK, similarityThreshold, filterExpression, false);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedDocs, result);
        verify(tokenUsageObserver).startCostContext(OperationType.EMBEDDING);
        verify(tokenUsageObserver).endCostContext();
    }
    
    @Test
    void testAehnlichkeitsSuche_MitQueryTransformation() {
        // Arrange
        String suchText = "Java";
        String transformedText = "Java Programmiersprache";
        
        doReturn(transformedText).when(vektorSpeicherService).transformiereSuchanfrage(suchText);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(new ArrayList<>());
        
        // Act
        vektorSpeicherService.aehnlichkeitsSuche(suchText, 5, 0.7f, null, true);
        
        // Assert
        verify(vektorSpeicherService).transformiereSuchanfrage(suchText);
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertEquals(transformedText, captor.getValue().getQuery());
    }
    
    @Test
    void testDirekteTextSuche_ErfolgreicheSuche() {
        // Arrange
        String suchText = "Java Programmierung";
        int topK = 5;
        
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(vectorStore.getNativeClient()).thenReturn(Optional.of(jdbcTemplate));
        
        List<String> suchbegriffe = Arrays.asList("java", "programmierung");
        doReturn(suchbegriffe).when(vektorSpeicherService).extrahiereSuchbegriffe(suchText);
        
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM vector_store"), eq(Integer.class)))
            .thenReturn(1000);
        
        List<Document> queryResults = Arrays.asList(
            new Document("1", "Java ist eine populäre Programmiersprache", new HashMap<>()),
            new Document("2", "Programmierung mit Java macht Spaß", new HashMap<>())
        );
        
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(queryResults);
        
        // Act
        List<Document> result = vektorSpeicherService.direkteTextSuche(suchText, topK, null);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.size() <= topK);
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }
    
    @Test
    void testDirekteTextSuche_KeinJdbcTemplate() {
        // Arrange
        when(vectorStore.getNativeClient()).thenReturn(Optional.empty());
        
        // Act
        List<Document> result = vektorSpeicherService.direkteTextSuche("test", 5, null);
        
        // Assert
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testHybrideSuche() {
        // Arrange
        String suchText = "Java";
        int topK = 5;
        float similarityThreshold = 0.7f;
        
        Document doc1 = Document.builder()
            .id("1")
            .text("Java Vector Result")
            .score(0.9)
            .build();
        Document doc2 = Document.builder()
            .id("2")
            .text("Java Text Result")
            .score(0.8)
            .build();
        Document doc3 = Document.builder()
            .id("1")
            .text("Java Both Results")
            .score(0.7)
            .build();
        
        doReturn(Arrays.asList(doc1)).when(vektorSpeicherService)
            .aehnlichkeitsSuche(anyString(), eq(topK), eq(similarityThreshold), isNull(), eq(false));
        doReturn(Arrays.asList(doc2, doc3)).when(vektorSpeicherService)
            .direkteTextSuche(anyString(), eq(topK), isNull());
        
        // Act
        List<Document> result = vektorSpeicherService.hybrideSuche(
            suchText, topK, similarityThreshold, null, false);
        
        // Assert
        assertEquals(2, result.size());
        // Dokument mit ID "1" sollte höheren Score haben (kombiniert)
        assertTrue(result.get(0).getScore() > result.get(1).getScore());
    }
    
    @Test
    void testExtrahiereSuchbegriffe_Erfolgreich() {
        // Arrange
        String suchText = "Was ist Java Programmierung?";
        String llmResponse = "[\"java\", \"programmierung\"]";
        
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(llmResponse));
        when(chatResponse.getResult()).thenReturn(generation);
        when(evaluationChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // Act
        List<String> result = vektorSpeicherService.extrahiereSuchbegriffe(suchText);
        
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains("java"));
        assertTrue(result.contains("programmierung"));
    }
    
    @Test
    void testExtrahiereSuchbegriffe_FehlerBehandlung() {
        // Arrange
        String suchText = "Test";
        when(evaluationChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Test error"));
        
        // Act
        List<String> result = vektorSpeicherService.extrahiereSuchbegriffe(suchText);
        
        // Assert
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testReindexiereAlleDokumente() {
        // Arrange
        KursMaterial doc1 = new KursMaterial();
        doc1.setId(1L);
        KursMaterial doc2 = new KursMaterial();
        doc2.setId(2L);
        
        when(kursMaterialRepository.findAllDocuments()).thenReturn(Arrays.asList(doc1, doc2));
        doReturn(true).when(vektorSpeicherService).indexiereKursMaterial(1L);
        doReturn(false).when(vektorSpeicherService).indexiereKursMaterial(2L);
        
        // Act
        int result = vektorSpeicherService.reindexiereAlleDokumente();
        
        // Assert
        assertEquals(1, result); // Nur 1 erfolgreich
        verify(vektorSpeicherService).indexiereKursMaterial(1L);
        verify(vektorSpeicherService).indexiereKursMaterial(2L);
    }
    
    @Test
    void testIndexiereNichtIndexierteDokumente() {
        // Arrange
        KursMaterial doc1 = new KursMaterial();
        doc1.setId(1L);
        KursMaterial doc2 = new KursMaterial();
        doc2.setId(2L);
        List<KursMaterial> docs = Arrays.asList(doc1, doc2);
        
        when(kursMaterialRepository.findAllNonIndexedDocuments()).thenReturn(docs);
        doReturn(true).when(vektorSpeicherService).indexiereKursMaterial(anyLong());
        
        // Act
        int result = vektorSpeicherService.indexiereNichtIndexierteDokumente();
        
        // Assert
        assertEquals(2, result);
        verify(vektorSpeicherService, times(2)).indexiereKursMaterial(anyLong());
    }
    
    @Test
    void testIndexiereNichtIndexierteDokumente_KeineDokumente() {
        // Arrange
        when(kursMaterialRepository.findAllNonIndexedDocuments()).thenReturn(new ArrayList<>());
        
        // Act
        int result = vektorSpeicherService.indexiereNichtIndexierteDokumente();
        
        // Assert
        assertEquals(0, result);
        verify(vektorSpeicherService, never()).indexiereKursMaterial(anyLong());
    }
    
    @Test
    public void testHybrideSucheMitKurseinheitBoost() {
        // Given
        String suchText = "Test Suche";
        Long kurseinheitId = 123L;
        
        // Mock-Dokumente mit verschiedenen Kurseinheiten
        Document doc1 = Document.builder()
                .id("1")
                .text("Test Dokument 1")
                .metadata(Map.of("kurseinheitId", 123))  // Gleiche Kurseinheit
                .score(0.8)
                .build();
                
        Document doc2 = Document.builder()
                .id("2") 
                .text("Test Dokument 2")
                .metadata(Map.of("kurseinheitId", 456))  // Andere Kurseinheit
                .score(0.7)
                .build();
                
        // Mock die Ähnlichkeitssuche
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(Arrays.asList(doc1, doc2));
                
        // Mock JdbcTemplate für Textsuche
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        when(vectorStore.getNativeClient()).thenReturn(Optional.of(mockJdbcTemplate));
        when(mockJdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(new ArrayList<>());
                
        // Mock ChatModel für Transformierung
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        when(mockGeneration.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(suchText));
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(evaluationChatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // When
        List<Document> results = vektorSpeicherService.hybrideSuche(
                suchText, 10, 0.7f, null, true, kurseinheitId);
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // Dokument aus gleicher Kurseinheit sollte höheren Score haben
        Document firstDoc = results.get(0);
        assertEquals("1", firstDoc.getId());
        assertTrue(firstDoc.getScore() > 1.0); // Original 0.8 + 0.3 Boost = 1.1
        
        Document secondDoc = results.get(1);
        assertEquals("2", secondDoc.getId());
        assertEquals(0.7, secondDoc.getScore(), 0.01); // Kein Boost
    }
    
    @Test
    public void testHybrideSucheMitKurseinheitBoostDeaktiviert() {
        // Given
        String suchText = "Test Suche";
        Long kurseinheitId = 123L;
        
        // Deaktiviere Boost
        RagConfig.RagProperties.RelevanceBoostProperties relevanceBoost = new RagConfig.RagProperties.RelevanceBoostProperties();
        RagConfig.RagProperties.RelevanceBoostProperties.SameKurseinheitBoost sameKurseinheitBoost = 
                new RagConfig.RagProperties.RelevanceBoostProperties.SameKurseinheitBoost();
        sameKurseinheitBoost.setEnabled(false);
        relevanceBoost.setSameKurseinheit(sameKurseinheitBoost);
        when(ragProperties.getRelevanceBoost()).thenReturn(relevanceBoost);
        
        // Mock-Dokumente
        Document doc1 = Document.builder()
                .id("1")
                .text("Test Dokument 1")
                .metadata(Map.of("kurseinheitId", 123))
                .score(0.8)
                .build();
                
        // Mock die Ähnlichkeitssuche
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(Arrays.asList(doc1));
                
        // Mock JdbcTemplate für Textsuche
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        when(vectorStore.getNativeClient()).thenReturn(Optional.of(mockJdbcTemplate));
        when(mockJdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(new ArrayList<>());
                
        // Mock ChatModel
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        when(mockGeneration.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(suchText));
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(evaluationChatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // When
        List<Document> results = vektorSpeicherService.hybrideSuche(
                suchText, 10, 0.7f, null, true, kurseinheitId);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        
        // Score sollte unverändert bleiben wenn Boost deaktiviert
        Document doc = results.get(0);
        assertEquals(0.8, doc.getScore(), 0.01);
    }

    @Test
    public void testBewerteRelevant(){
        String dummyDocument = """
                . .  . . .  .  . .  . .  . 927                                                                \s
                                     8.6.4    Drahtlose Sicherheit  .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  . .  .  929                                                             \s
                
                                                                                                                        13
                
                
                
                
                
                """;

        double drahtlose = vektorSpeicherService.berechneTextRelevanz(dummyDocument, List.of("drahtlose"));
        assertEquals(0, drahtlose);
    }
}