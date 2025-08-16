package de.fuh.kn.webapp.llm.rag.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.prompt.PromptTemplates;
import de.fuh.kn.webapp.llm.rag.config.RagConfig;
import de.fuh.kn.webapp.llm.rag.document.DokumentParserService;
import de.fuh.kn.webapp.llm.rag.document.TextSegmentierungsService;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.repository.KursMaterialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.ai.vectorstore.mariadb.MariaDBFilterExpressionConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service für die Verwaltung des Vektorspeichers.
 * Extrahiert Text aus Dokumenten, segmentiert diesen und speichert die Vektoren für semantische Suche.
 * Nutzt Spring AI's VectorStore für die Speicherung und Suche von Dokumenten.
 */
@Service
@Slf4j
public class VektorSpeicherService {
    
    private final DokumentParserService dokumentParserService;
    private final TextSegmentierungsService textSegmentierungsService;
    private final KursMaterialRepository kursMaterialRepository;
    private final VectorStore vectorStore;
    private final TokenUsageObserver tokenUsageObserver;
    private final KursMaterialService kursMaterialService;
    
    private final ChatModel evaluationChatModel;
    private final KursService kursService;
    private final KurseinheitService kurseinheitService;
    private final RagConfig.RagProperties ragProperties;

    public VektorSpeicherService(DokumentParserService dokumentParserService, TextSegmentierungsService textSegmentierungsService, KursMaterialRepository kursMaterialRepository, VectorStore vectorStore, TokenUsageObserver tokenUsageObserver, KursMaterialService kursMaterialService, @Qualifier("evaluationChatClient") ChatModel evaluationChatModel, KursService kursService, KurseinheitService kurseinheitService, RagConfig.RagProperties ragProperties) {
        this.dokumentParserService = dokumentParserService;
        this.textSegmentierungsService = textSegmentierungsService;
        this.kursMaterialRepository = kursMaterialRepository;
        this.vectorStore = vectorStore;
        this.tokenUsageObserver = tokenUsageObserver;
        this.kursMaterialService = kursMaterialService;
        this.evaluationChatModel = evaluationChatModel;
        this.kursService = kursService;
        this.kurseinheitService = kurseinheitService;
        this.ragProperties = ragProperties;
    }

    /**
     * Indexiert ein Kursmaterial asynchron.
     * Extrahiert Text, segmentiert diesen und speichert ihn im Vektorspeicher.
     *
     * @param kursMaterialId Die ID des zu indexierenden Kursmaterials.
     * @return Ein CompletableFuture, das true zurückgibt, wenn die Indexierung erfolgreich war.
     */
    @Async
    public CompletableFuture<Boolean> indexiereKursMaterialAsync(Long kursMaterialId) {
        try {
            boolean erfolg = indexiereKursMaterial(kursMaterialId);
            return CompletableFuture.completedFuture(erfolg);
        } catch (Exception e) {
            log.error("Fehler bei der asynchronen Indexierung von Kursmaterial {}: {}", kursMaterialId, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Indexiert ein Kursmaterial.
     * Extrahiert Text, segmentiert diesen und speichert ihn im Vektorspeicher.
     * Aktualisiert den Indexierungsstatus in der Datenbank.
     *
     * @param kursMaterialId Die ID des zu indexierenden Kursmaterials.
     * @return true, wenn die Indexierung erfolgreich war.
     */
    @Transactional
    public boolean indexiereKursMaterial(Long kursMaterialId) {
        log.info("Starte Indexierung von Kursmaterial {}", kursMaterialId);
        
        // Kursmaterial abrufen mit eager Loading der Kurs- und Kurseinheit-Entitäten
        KursMaterialDTO kursMaterialDTO = kursMaterialService.getKursMaterialById(kursMaterialId);
        if (kursMaterialDTO == null) {
            log.warn("Kursmaterial {} nicht gefunden", kursMaterialId);
            return false;
        }
        
        // Nur Dokumente indexieren (keine Bilder)
        if (!kursMaterialDTO.getTyp().equals(KursMaterialDTO.KursMaterialTyp.DOKUMENT)) {
            log.info("Kursmaterial {} ist kein Dokument, sondern {}, wird übersprungen", 
                    kursMaterialId, kursMaterialDTO.getTyp());
            return true;
        }

        KursDTO kurs = null;
        if (kursMaterialDTO.getKursId() != null) {
            kurs = kursService.getKursById(kursMaterialDTO.getKursId());
        }

        KurseinheitDTO kurseinheit = null;
        if (kursMaterialDTO.getKurseinheitId() != null) {
            kurseinheit = kurseinheitService.getKurseinheitById(kursMaterialDTO.getKurseinheitId());
            kurs = kursService.getKursById(kurseinheit.getKursId());
        }
        
        try {
            // Vorhandene Einträge entfernen
            entferneKursMaterialAusVektorspeicher(kursMaterialId);
            
            // Text aus Dokument extrahieren als Spring AI Documents
            List<Document> dokumente = dokumentParserService.extrahiereText(kursMaterialDTO);
            if (dokumente.isEmpty()) {
                log.warn("Keine Texte aus Kursmaterial {} extrahiert", kursMaterialId);
                markiereAlsNichtIndexiert(kursMaterialDTO);
                return false;
            }
            
            List<Document> allSegments = new ArrayList<>();
            
            // Für jedes extrahierte Dokument
            for (Document dokument : dokumente) {
                // Metadaten ergänzen
                Map<String, Object> metadata = new HashMap<>(dokument.getMetadata());
                metadata.put("kursMaterialId", kursMaterialId);

                if(kurs != null) {
                    metadata.put("kursId", kurs.getId());
                }

                if (kurseinheit != null) {
                    metadata.put("kurseinheitId", kurseinheit.getId());
                }

                Document dokumentMitMetadaten = new Document(dokument.getText(), metadata);
                
                // Text segmentieren mit Spring AI Textsplitter
                List<Document> segments = textSegmentierungsService.segmentiereText(dokumentMitMetadaten);
                
                if (!segments.isEmpty()) {
                    allSegments.addAll(segments);
                }
            }
            
            if (allSegments.isEmpty()) {
                log.warn("Keine Textsegmente für Kursmaterial {} generiert", kursMaterialId);
                markiereAlsNichtIndexiert(kursMaterialDTO);
                return false;
            }
            
            // Segmente im VectorStore speichern - automatische Embedding-Generierung durch Spring AI
            // Embedding-Operation
            tokenUsageObserver.startCostContext(OperationType.EMBEDDING);
            vectorStore.add(allSegments);
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            
            // Als indexiert markieren
            markiereAlsIndexiert(kursMaterialDTO);
            
            log.info("Kursmaterial {} erfolgreich indexiert. {} Segmente gespeichert. Kosten: ${}", 
                    kursMaterialId, allSegments.size(), costContext.getTotalCost().toPlainString());
            
            return true;
        } catch (Exception e) {
            log.error("Fehler bei der Indexierung von Kursmaterial {}: {}", kursMaterialId, e.getMessage(), e);
            markiereAlsNichtIndexiert(kursMaterialDTO);
            return false;
        }
    }
    
    /**
     * Markiert ein Kursmaterial als erfolgreich indexiert.
     *
     * @param kursMaterial Das Kursmaterial, das indexiert wurde.
     */
    private void markiereAlsIndexiert(KursMaterialDTO kursMaterial) {
        KursMaterialDTO kursMaterialDTO = kursMaterialService.getKursMaterialById(kursMaterial.getId());
        Optional<KursMaterial> optionalKursMaterial = kursMaterialRepository.findById(kursMaterialDTO.getId());
        if (optionalKursMaterial.isPresent()) {
            optionalKursMaterial.get().setIndexiert(true);
            kursMaterialRepository.save(optionalKursMaterial.get());
        }
    }
    
    /**
     * Markiert ein Kursmaterial als nicht indexiert.
     *
     * @param kursMaterial Das Kursmaterial, das nicht indexiert werden konnte.
     */
    private void markiereAlsNichtIndexiert(KursMaterialDTO kursMaterial) {
        KursMaterialDTO kursMaterialDTO = kursMaterialService.getKursMaterialById(kursMaterial.getId());
        Optional<KursMaterial> optionalKursMaterial = kursMaterialRepository.findById(kursMaterialDTO.getId());
        if (optionalKursMaterial.isPresent()) {
            optionalKursMaterial.get().setIndexiert(false);
            kursMaterialRepository.save(optionalKursMaterial.get());
        }
    }
    
    /**
     * Führt eine Neuindexierung aller Dokumente in der Datenbank durch.
     * Löscht dabei alle Caches, da sich die Dokumentfrequenzen ändern können.
     *
     * @return Die Anzahl der erfolgreich indexierten Dokumente.
     */
    @Transactional
    public int reindexiereAlleDokumente() {
        log.info("Starte Neuindexierung aller Dokumente");
        
        // Caches leeren, da sich Dokumentfrequenzen ändern können
        suchCache.clear();
        dokumentFrequenzCache.clear();
        idfCache.clear();
        gesamtDokumentAnzahlCache = -1;
        
        // Alle Dokument-Kursmaterialien abrufen
        List<KursMaterial> dokumente = kursMaterialRepository.findAllDocuments();
        
        int erfolgreiche = 0;
        for (KursMaterial dokument : dokumente) {
            try {
                boolean erfolg = indexiereKursMaterial(dokument.getId());
                if (erfolg) {
                    erfolgreiche++;
                }
            } catch (Exception e) {
                log.error("Fehler bei der Neuindexierung von Dokument {}: {}", dokument.getId(), e.getMessage());
            }
        }
        
        log.info("Neuindexierung abgeschlossen. {}/{} Dokumente erfolgreich indexiert. Caches zurückgesetzt.", 
                erfolgreiche, dokumente.size());
        
        return erfolgreiche;
    }
    
    /**
     * Indexiert alle noch nicht indexierten Dokumente.
     * Aktualisiert die Caches für Dokumentfrequenzen, da sich diese ändern könnten.
     *
     * @return Die Anzahl der erfolgreich indexierten Dokumente.
     */
    @Transactional
    public int indexiereNichtIndexierteDokumente() {
        log.info("Starte Indexierung aller nicht-indexierten Dokumente");
        
        // Alle nicht-indexierten Dokument-Kursmaterialien abrufen
        List<KursMaterial> nichtIndexierteDokumente = kursMaterialRepository.findAllNonIndexedDocuments();
        
        if (nichtIndexierteDokumente.isEmpty()) {
            log.info("Keine nicht-indexierten Dokumente gefunden");
            return 0;
        }
        
        log.info("{} nicht-indexierte Dokumente gefunden", nichtIndexierteDokumente.size());
        
        // Wenn eine signifikante Anzahl neuer Dokumente hinzukommt, Caches leeren
        if (nichtIndexierteDokumente.size() >= 5) { // Schwellenwert für "signifikant"
            suchCache.clear();
            dokumentFrequenzCache.clear();
            idfCache.clear();
            gesamtDokumentAnzahlCache = -1;
            log.info("Caches zurückgesetzt, da eine signifikante Anzahl neuer Dokumente indexiert wird");
        }
        
        int erfolgreiche = 0;
        for (KursMaterial dokument : nichtIndexierteDokumente) {
            try {
                boolean erfolg = indexiereKursMaterial(dokument.getId());
                if (erfolg) {
                    erfolgreiche++;
                }
            } catch (Exception e) {
                log.error("Fehler bei der Indexierung von Dokument {}: {}", dokument.getId(), e.getMessage());
            }
        }
        
        log.info("Indexierung nicht-indexierter Dokumente abgeschlossen. {}/{} Dokumente erfolgreich indexiert", 
                erfolgreiche, nichtIndexierteDokumente.size());
        
        return erfolgreiche;
    }
    
    /**
     * Entfernt ein Kursmaterial aus dem Vektorspeicher.
     *
     * @param kursMaterialId Die ID des zu entfernenden Kursmaterials.
     */
    @Transactional
    public void entferneKursMaterialAusVektorspeicher(Long kursMaterialId) {
        try {
            // Alle Dokumente mit der kursMaterialId in den Metadaten suchen
            vectorStore.delete("kursMaterialId == " + kursMaterialId);
            log.info("Dokumente für Kursmaterial {} aus dem Vektorspeicher entfernt", kursMaterialId);
            
        } catch (Exception e) {
            log.error("Fehler beim Entfernen des Kursmaterials {} aus dem Vektorspeicher: {}", 
                    kursMaterialId, e.getMessage());
        }
    }
    
    /**
     * Transformiert eine Suchanfrage mittels LLM für bessere Retrieval-Ergebnisse.
     * 
     * @param suchText Die ursprüngliche Suchanfrage.
     * @return Die transformierte Suchanfrage.
     */
    protected String transformiereSuchanfrage(String suchText) {
        if (suchText == null || suchText.isEmpty()) {
            return suchText;
        }
        
        try {
            // LLM-basierte Query-Transformation
            tokenUsageObserver.startCostContext(OperationType.OTHER);
            
            // Prompt für Suchanfragen-Transformation aus PromptTemplates
            Message systemMessage = new SystemPromptTemplate(PromptTemplates.QUERY_TRANSFORMATION_TEMPLATE).createMessage(Map.of());
            Message userMessage = new UserMessage(suchText);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            
            // Anfrage an das LLM senden
            String transformierterText = evaluationChatModel.call(prompt)
                    .getResult().getOutput().getText();
            
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            
            log.info("Suchanfrage transformiert: '{}' -> '{}'. Kosten: ${}", 
                     suchText, transformierterText, costContext.getTotalCost().toPlainString());
            
            return transformierterText;
        } catch (Exception e) {
            log.error("Fehler bei der Transformation der Suchanfrage '{}': {}", suchText, e.getMessage());
            return suchText; // Im Fehlerfall die ursprüngliche Anfrage zurückgeben
        }
    }
    
    /**
     * Durchsucht den Vektorspeicher nach ähnlichen Dokumenten zu einer Suchanfrage.
     * Verwendet optional Query-Transformation für bessere Ergebnisse und unterstützt Filter.
     *
     * @param suchText Die Suchanfrage.
     * @param topK Die maximale Anzahl zurückzugebender Dokumente.
     * @param similarityThreshold Der Mindestähnlichkeitswert (0.0 - 1.0).
     * @param filterExpression Ein optionaler Filterausdruck für die Suche (kann null sein).
     * @param useQueryTransformation Ob die Suchanfrage transformiert werden soll.
     * @return Liste der ähnlichsten Dokumente.
     */
    @Transactional(readOnly = true)
    public List<Document> aehnlichkeitsSuche(String suchText, int topK, float similarityThreshold, 
                                          String filterExpression, boolean useQueryTransformation) {
        if (suchText == null || suchText.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Kostentracking starten
            tokenUsageObserver.startCostContext(OperationType.EMBEDDING);
            
            // Anfrage transformieren, wenn gewünscht
            String queryText = useQueryTransformation ? transformiereSuchanfrage(suchText) : suchText;
            
            // Request Builder mit Basis-Parametern
            SearchRequest.Builder requestBuilder = SearchRequest.builder()
                    .query(queryText)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold);
            
            // Filter hinzufügen, wenn vorhanden
            if (filterExpression != null && !filterExpression.isEmpty()) {
                requestBuilder.filterExpression(filterExpression);
            }
            
            // Suche durchführen
            List<Document> ergebnisse = vectorStore.similaritySearch(requestBuilder.build());
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            log.info("Vektorsuche für '{}' durchgeführt. {} Ergebnisse gefunden. Kosten: ${}", 
                    queryText, ergebnisse.size(), costContext.getTotalCost().toPlainString());
            
            return ergebnisse;
        } catch (Exception e) {
            log.error("Fehler bei der Vektorsuche für '{}': {}", suchText, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Cache für IDF-Werte (Term -> IDF), wird aus Perfomancegründen gecacht
    private final Map<String, Double> idfCache = new HashMap<>();
    
    // Cache für Dokumentfrequenzen (Term -> Anzahl Dokumente mit dem Term)
    private final Map<String, Integer> dokumentFrequenzCache = new HashMap<>();
    
    // Cache für Gesamtdokumentenanzahl (wird gelegentlich aktualisiert)
    private int gesamtDokumentAnzahlCache = -1;
    private long letzteGesamtDokumentAnzahlAktualisierung = 0;
    private static final long DOKUMENT_ANZAHL_CACHE_TTL = 60 * 60 * 1000; // 1 Stunde
    
    // Cache für die gefundenen Dokumente pro Suchanfrage (TTL-basiert)
    private final Map<String, Map.Entry<Long, List<Document>>> suchCache = new HashMap<>();
    
    // TTL für Cache in Millisekunden (5 Minuten)
    private static final long SUCH_CACHE_TTL = 5 * 60 * 1000;
    
    // Maximale Größe für den Such-Cache
    private static final int SUCH_CACHE_MAX_SIZE = 100;
    
    /**
     * Räumt den Such-Cache auf, um Speicherlecks zu vermeiden.
     * Bereinigt abgelaufene Einträge und entfernt älteste Einträge, wenn Cache-Größe überschritten wird.
     */
    private void reinigeCache() {
        // Aktuelle Zeit
        long aktuelleZeit = System.currentTimeMillis();
        
        // 1. Abgelaufene Einträge entfernen
        suchCache.entrySet().removeIf(entry -> 
            aktuelleZeit - entry.getValue().getKey() > SUCH_CACHE_TTL);
        
        // 2. Älteste Einträge entfernen, wenn Cache zu groß wird
        if (suchCache.size() > SUCH_CACHE_MAX_SIZE) {
            // Nach Zeitstempel sortieren und nur die neuesten SUCH_CACHE_MAX_SIZE behalten
            List<Map.Entry<String, Map.Entry<Long, List<Document>>>> sortedEntries = new ArrayList<>(suchCache.entrySet());
            sortedEntries.sort(Comparator.comparing(e -> e.getValue().getKey()));
            
            // Anzahl zu löschender Einträge
            int zuLoeschen = sortedEntries.size() - SUCH_CACHE_MAX_SIZE;
            
            // Älteste Einträge entfernen
            for (int i = 0; i < zuLoeschen; i++) {
                suchCache.remove(sortedEntries.get(i).getKey());
            }
            
            log.debug("{} alte Cache-Einträge entfernt.", zuLoeschen);
        }
    }
    
    /**
     * Führt eine direkte Textsuche ohne Embedding-Generierung durch.
     * Implementiert TF-IDF für bessere Ranking-Ergebnisse.
     * 
     * @param suchText Der Suchtext, der im Inhalt der Dokumente vorkommen soll.
     * @param topK Die maximale Anzahl zurückzugebender Dokumente.
     * @param filterExpressionText Ein optionaler zusätzlicher Filterausdruck.
     * @return Eine Liste von Dokumenten, die den Suchtext enthalten, nach TF-IDF-Relevanz sortiert.
     */
    @Transactional(readOnly = true)
    public List<Document> direkteTextSuche(String suchText, int topK, String filterExpressionText) {
        if (suchText == null || suchText.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Cache bereinigen, um Speicherlecks zu vermeiden
        reinigeCache();
        
        // Cache-Key erstellen
        String cacheKey = suchText + "|" + topK + "|" + (filterExpressionText != null ? filterExpressionText : "");
        
        // Cache prüfen
        if (suchCache.containsKey(cacheKey)) {
            Map.Entry<Long, List<Document>> cachedEntry = suchCache.get(cacheKey);
            long cachedTime = cachedEntry.getKey();
            
            // Wenn Cache noch gültig ist (TTL nicht abgelaufen)
            if (System.currentTimeMillis() - cachedTime < SUCH_CACHE_TTL) {
                log.debug("Cache-Treffer für Textsuche '{}'", suchText);
                return cachedEntry.getValue();
            } else {
                // Cache-Eintrag ist abgelaufen, aus Cache entfernen
                suchCache.remove(cacheKey);
            }
        }
        
        try {
            // Kostentracking starten - keine Token-Kosten, aber für konsistentes Logging
            tokenUsageObserver.startCostContext(OperationType.OTHER);
            
            // JdbcTemplate über native Client holen
            Optional<JdbcTemplate> jdbcTemplateOpt = vectorStore.getNativeClient();
            
            if (jdbcTemplateOpt.isEmpty()) {
                log.warn("Konnte keinen JdbcTemplate vom VectorStore bekommen - Textsuche nicht möglich");
                return Collections.emptyList();
            }
            
            JdbcTemplate jdbcTemplate = jdbcTemplateOpt.get();

            // Suchbegriffe mit LLM extrahieren
            List<String> relevanteBegriffe = extrahiereSuchbegriffe(suchText);
            
            if (relevanteBegriffe.isEmpty()) {
                // Fallback: Einfache Extraktion ohne LLM
                String[] suchbegriffe = suchText.toLowerCase().split("\\s+");
                relevanteBegriffe = Arrays.stream(suchbegriffe)
                        .filter(begriff -> begriff.length() > 3) // Nur längere Wörter berücksichtigen
                        .toList();
            }
            
            if (relevanteBegriffe.isEmpty()) {
                return Collections.emptyList();
            }

            //Falls Begriffe Leerzeichen enthalten, dann nochmal trennen
            relevanteBegriffe = relevanteBegriffe.stream()
                    .flatMap(str -> str.contains("-") ?
                            Arrays.stream(str.split("-")).filter(s -> !s.isEmpty()) :
                            Stream.of(str))
                    .flatMap(str -> str.contains(" ") ?
                            Arrays.stream(str.split(" ")).filter(s -> !s.isEmpty()) :
                            Stream.of(str))
                    .collect(Collectors.toList());
            
            // Vorbereitende TF-IDF-Berechnung: Dokumentfrequenzen für jeden Begriff ermitteln
            Map<String, Integer> dokumentFrequenzen = new HashMap<>();
            int gesamtDokumentAnzahl = getGesamtDokumentAnzahl(jdbcTemplate);
            
            // SQL für direkte LIKE-Suche erstellen
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT id, content, metadata FROM vector_store WHERE ");
            
            // LIKE-Klauseln für jeden Begriff
            List<String> likeClauseln = new ArrayList<>();
            for (String begriff : relevanteBegriffe) {
                likeClauseln.add("LOWER(content) LIKE ?");
                
                // Wenn nicht im Cache, Dokumentfrequenz für diesen Begriff berechnen
                if (!idfCache.containsKey(begriff)) {
                    int dokumentFrequenz = getAnzahlDokumenteMitTerm(jdbcTemplate, begriff);
                    dokumentFrequenzen.put(begriff, dokumentFrequenz);
                    
                    // IDF berechnen und cachen
                    double idf = berechneTfidfWert(dokumentFrequenz, gesamtDokumentAnzahl);
                    idfCache.put(begriff, idf);
                }
            }
            
            sqlBuilder.append("(").append(String.join(" OR ", likeClauseln)).append(")");
            
            // Zusätzlichen Filter hinzufügen, wenn vorhanden
            if (filterExpressionText != null && !filterExpressionText.isEmpty()) {

                Filter.Expression filterExpression = new FilterExpressionTextParser().parse(filterExpressionText);
                String filterSql = new MariaDBFilterExpressionConverter("metadata").convertExpression(filterExpression);

                sqlBuilder.append(" AND (").append(filterSql).append(")");
            }
            
            // Parameter vorbereiten
            List<Object> params = new ArrayList<>();
            for (String begriff : relevanteBegriffe) {
                params.add("%" + begriff.toLowerCase() + "%");
            }
            
            // Query ausführen (kein LIMIT hier, da wir alle Dokumente bewerten und dann erst sortieren)
            List<Document> ergebnisse = jdbcTemplate.query(
                sqlBuilder.toString(),
                (rs, rowNum) -> {
                    String id = rs.getString("id");
                    String content = rs.getString("content");
                    String metadataJson = rs.getString("metadata");
                    
                    // Metadaten deserialisieren
                    Map<String, Object> metadata;
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        metadata = mapper.readValue(metadataJson, Map.class);
                    } catch (Exception e) {
                        log.warn("Fehler beim Deserialisieren der Metadaten für Dokument {}: {}", id, e.getMessage());
                        metadata = new HashMap<>();
                    }
                    
                    // Erstelle Document ohne Score, wird später hinzugefügt
                    return new Document(id, content, metadata);
                },
                params.toArray()
            );
            
            // Relevanz für jedes Dokument mit TF-IDF berechnen und im Dokument speichern
            List<Document> bewerteteErgebnisse = new ArrayList<>();
            for (Document doc : ergebnisse) {
                String content = doc.getText().toLowerCase();
                
                // Berechne TF-IDF-basierte Relevanz
                double tfidfRelevance = berechneTextRelevanz(content, relevanteBegriffe);
                
                // Erstelle eine neue Dokument-Instanz mit Score
                Document docWithScore = Document.builder()
                        .id(doc.getId())
                        .text(doc.getText())
                        .metadata(doc.getMetadata())
                        .score(tfidfRelevance)
                        .build();
                
                bewerteteErgebnisse.add(docWithScore);
            }
            
            // Nach Score sortieren (absteigend) und auf topK begrenzen
            List<Document> sortierteBewerteteErgebnisse = bewerteteErgebnisse.stream()
                    .sorted(Comparator.comparing(doc -> -(doc.getScore() != null ? doc.getScore() : 0.0)))
                    .limit(topK)
                    .collect(Collectors.toList());
            
            // Ergebnisse in Cache speichern
            suchCache.put(cacheKey, Map.entry(
                    System.currentTimeMillis(),
                    sortierteBewerteteErgebnisse
            ));
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            log.info("TF-IDF Textsuche für '{}' durchgeführt. {} Ergebnisse gefunden. Kosten: ${}", 
                    suchText, sortierteBewerteteErgebnisse.size(), costContext.getTotalCost().toPlainString());
                    
            return sortierteBewerteteErgebnisse;
        } catch (Exception e) {
            log.error("Fehler bei der TF-IDF Textsuche für '{}': {}", suchText, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Ermittelt die Gesamtanzahl der Dokumente im Vektorspeicher.
     * Verwendet einen Cache mit TTL, um die Datenbankabfragen zu reduzieren.
     * 
     * @param jdbcTemplate Der JdbcTemplate für Datenbankzugriffe.
     * @return Die Anzahl der Dokumente.
     */
    private int getGesamtDokumentAnzahl(JdbcTemplate jdbcTemplate) {
        long aktuelleZeit = System.currentTimeMillis();
        
        // Wenn Cache leer oder abgelaufen ist, aktualisieren
        if (gesamtDokumentAnzahlCache < 0 || 
            (aktuelleZeit - letzteGesamtDokumentAnzahlAktualisierung > DOKUMENT_ANZAHL_CACHE_TTL)) {
            try {
                gesamtDokumentAnzahlCache = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vector_store", Integer.class);
                letzteGesamtDokumentAnzahlAktualisierung = aktuelleZeit;
                log.debug("Gesamtdokumentanzahl-Cache aktualisiert: {}", gesamtDokumentAnzahlCache);
            } catch (Exception e) {
                log.warn("Fehler beim Ermitteln der Gesamtdokumentanzahl: {}", e.getMessage());
                // Wenn wir schon einen Cache-Wert haben, verwenden wir diesen, sonst Fallback
                if (gesamtDokumentAnzahlCache < 0) {
                    gesamtDokumentAnzahlCache = 1000; // Fallback-Wert
                }
            }
        }
        
        return gesamtDokumentAnzahlCache;
    }
    
    /**
     * Ermittelt die Anzahl der Dokumente, die einen bestimmten Term enthalten.
     * Verwendet einen Cache, um wiederholte Datenbankabfragen zu vermeiden.
     * 
     * @param jdbcTemplate Der JdbcTemplate für Datenbankzugriffe.
     * @param term Der zu suchende Term.
     * @return Die Anzahl der Dokumente, die den Term enthalten.
     */
    private int getAnzahlDokumenteMitTerm(JdbcTemplate jdbcTemplate, String term) {
        // Wenn bereits im Cache, direkt zurückgeben
        if (dokumentFrequenzCache.containsKey(term)) {
            return dokumentFrequenzCache.get(term);
        }
        
        try {
            int dokumentFrequenz = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store WHERE LOWER(content) LIKE ?", 
                    Integer.class, 
                    "%" + term.toLowerCase() + "%");
            
            // Im Cache speichern
            dokumentFrequenzCache.put(term, dokumentFrequenz);
            
            return dokumentFrequenz;
        } catch (Exception e) {
            log.warn("Fehler beim Ermitteln der Dokumentfrequenz für Term '{}': {}", term, e.getMessage());
            return 1; // Fallback-Wert
        }
    }
    
    /**
     * Berechnet den IDF-Wert (Inverse Document Frequency) für einen Term.
     * 
     * @param dokumentFrequenz Die Anzahl der Dokumente, die den Term enthalten.
     * @param gesamtDokumentAnzahl Die Gesamtanzahl der Dokumente.
     * @return Der IDF-Wert.
     */
    private double berechneTfidfWert(int dokumentFrequenz, int gesamtDokumentAnzahl) {
        // Vermeidung von Division durch Null
        if (dokumentFrequenz == 0) {
            dokumentFrequenz = 1;
        }
        
        // IDF = log(N/df), wobei N die Gesamtanzahl der Dokumente ist und df die Dokumentfrequenz
        return Math.log((double) gesamtDokumentAnzahl / dokumentFrequenz);
    }

    /**
     * Führt eine hybride Suche durch, die Vektorsuche und Textsuche kombiniert.
     * 
     * @param suchText Die Suchanfrage.
     * @param topK Die maximale Anzahl zurückzugebender Dokumente.
     * @param similarityThreshold Der Mindestähnlichkeitswert für die Vektorsuche (0.0 - 1.0).
     * @param filterExpression Ein optionaler Filterausdruck für die Suche (kann null sein).
     * @param useQueryTransformation Ob die Suchanfrage transformiert werden soll.
     * @return Liste der relevantesten Dokumente, kombiniert aus Vektor- und Textsuche.
     */
    @Transactional(readOnly = true)
    public List<Document> hybrideSuche(String suchText, int topK, float similarityThreshold, 
                                     String filterExpression, boolean useQueryTransformation) {
        return hybrideSuche(suchText, topK, similarityThreshold, filterExpression, useQueryTransformation, null);
    }
    
    /**
     * Führt eine hybride Suche durch, die Vektorsuche und Textsuche kombiniert.
     * Unterstützt optionales Relevanz-Boosting für Dokumente aus derselben Kurseinheit.
     * 
     * @param suchText Die Suchanfrage.
     * @param topK Die maximale Anzahl zurückzugebender Dokumente.
     * @param similarityThreshold Der Mindestähnlichkeitswert für die Vektorsuche (0.0 - 1.0).
     * @param filterExpression Ein optionaler Filterausdruck für die Suche (kann null sein).
     * @param useQueryTransformation Ob die Suchanfrage transformiert werden soll.
     * @param kurseinheitId Die ID der Kurseinheit für Relevanz-Boosting (kann null sein).
     * @return Liste der relevantesten Dokumente, kombiniert aus Vektor- und Textsuche.
     */
    @Transactional(readOnly = true)
    public List<Document> hybrideSuche(String suchText, int topK, float similarityThreshold, 
                                     String filterExpression, boolean useQueryTransformation, Long kurseinheitId) {
        if (suchText == null || suchText.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Kostentracking starten
            tokenUsageObserver.startCostContext(OperationType.EMBEDDING);
            
            // Anfrage transformieren, wenn gewünscht
            String queryText = useQueryTransformation ? transformiereSuchanfrage(suchText) : suchText;
            
            // 1. Vektorsuche durchführen
            List<Document> vectorErgebnisse = aehnlichkeitsSuche(
                    queryText, topK, similarityThreshold, filterExpression, false);
            
            // 2. Textsuche durchführen
            List<Document> textErgebnisse = direkteTextSuche(queryText+" "+suchText, topK, filterExpression);
            
            // 3. Ergebnisse kombinieren
            // Alle eindeutigen Dokumente zusammenfassen und Scores kombinieren
            Map<String, Document> allDocs = new HashMap<>();
            
            // Füge Vektorergebnisse hinzu
            for (Document doc : vectorErgebnisse) {
                allDocs.put(doc.getId(), doc);
            }
            
            // Füge Textergebnisse hinzu oder kombiniere Scores, wenn bereits vorhanden
            for (Document doc : textErgebnisse) {
                if (allDocs.containsKey(doc.getId())) {
                    // Dokument existiert bereits, kombiniere Scores
                    Document existingDoc = allDocs.get(doc.getId());
                    double vectorScore = existingDoc.getScore() != null ? existingDoc.getScore() : 0.0;
                    double textScore = doc.getScore() != null ? doc.getScore() : 0.0;
                    double combinedScore = vectorScore + textScore + 0.2; // Bonus für Dokumente in beiden Ergebnismengen
                    
                    // Erstelle neues Dokument mit kombiniertem Score
                    Document docWithCombinedScore = doc.mutate()
                            .score(combinedScore)
                            .build();
                    
                    allDocs.put(doc.getId(), docWithCombinedScore);
                } else {
                    // Neues Dokument, direkt hinzufügen
                    allDocs.put(doc.getId(), doc);
                }
            }
            
            // 4. Relevanz-Boosting für Dokumente aus derselben Kurseinheit anwenden
            if (kurseinheitId != null && ragProperties.getRelevanceBoost().getSameKurseinheit().isEnabled()) {
                float boostFactor = ragProperties.getRelevanceBoost().getSameKurseinheit().getBoostFactor();
                
                for (Map.Entry<String, Document> entry : allDocs.entrySet()) {
                    Document doc = entry.getValue();
                    
                    // Prüfe ob das Dokument aus derselben Kurseinheit stammt
                    if (doc.getMetadata().containsKey("kurseinheitId")) {
                        Object docKurseinheitId = doc.getMetadata().get("kurseinheitId");
                        Long docKurseinheitIdLong = null;
                        
                        if (docKurseinheitId instanceof Integer) {
                            docKurseinheitIdLong = ((Integer) docKurseinheitId).longValue();
                        } else if (docKurseinheitId instanceof Long) {
                            docKurseinheitIdLong = (Long) docKurseinheitId;
                        }
                        
                        if (kurseinheitId.equals(docKurseinheitIdLong)) {
                            // Dokument stammt aus derselben Kurseinheit - Boost anwenden
                            double currentScore = doc.getScore() != null ? doc.getScore() : 0.0;
                            double boostedScore = currentScore + boostFactor;
                            
                            Document boostedDoc = doc.mutate()
                                    .score(boostedScore)
                                    .build();
                                    
                            allDocs.put(doc.getId(), boostedDoc);
                            
                            log.debug("Dokument {} aus Kurseinheit {} geboostet: Score {} -> {}", 
                                    doc.getId(), kurseinheitId, currentScore, boostedScore);
                        }
                    }
                }
            }
            
            // Nach Score sortieren (absteigend)
            List<Document> kombinierteErgebnisse = allDocs.values().stream()
                    .sorted(Comparator.comparing(doc -> -(doc.getScore() != null ? doc.getScore() : 0.0))) // Absteigend sortieren
                    .limit(topK)
                    .collect(Collectors.toList());
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            
            String boostInfo = "";
            if (kurseinheitId != null && ragProperties.getRelevanceBoost().getSameKurseinheit().isEnabled()) {
                boostInfo = String.format(", Kurseinheit-Boost aktiv für ID %d", kurseinheitId);
            }
            
            log.info("Hybride Suche für '{}' durchgeführt. {} Ergebnisse gefunden (Vektor: {}, Text: {}{}). Kosten: ${}", 
                    queryText, kombinierteErgebnisse.size(), vectorErgebnisse.size(), textErgebnisse.size(), 
                    boostInfo, costContext.getTotalCost().toPlainString());
            
            return kombinierteErgebnisse;
        } catch (Exception e) {
            log.error("Fehler bei der hybriden Suche für '{}': {}", suchText, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Extrahiert relevante Suchbegriffe aus einer Suchanfrage mit Hilfe eines LLMs.
     * Verwendet das KEYWORD_EXTRACTION_TEMPLATE für die LLM-Anfrage.
     * 
     * @param suchText Die ursprüngliche Suchanfrage.
     * @return Eine Liste von relevanten Suchbegriffen.
     */
    protected List<String> extrahiereSuchbegriffe(String suchText) {
        if (suchText == null || suchText.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Kostentracking starten
            tokenUsageObserver.startCostContext(OperationType.OTHER);
            
            // Prompt für Suchbegriff-Extraktion aus PromptTemplates
            Message systemMessage = new SystemMessage(PromptTemplates.KEYWORD_EXTRACTION_TEMPLATE);
            Message userMessage = new UserMessage(suchText);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            
            // Anfrage an das LLM senden
            String response = evaluationChatModel.call(prompt)
                    .getResult().getOutput().getText();
            
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            
            // JSON-Array als Liste von Strings parsen
            List<String> suchbegriffe = parseKeywords(response);
            
            if (suchbegriffe.isEmpty()) {
                log.warn("Keine relevanten Suchbegriffe für '{}' gefunden. LLM-Antwort: {}", suchText, response);
                return Collections.emptyList();
            }
            
            log.info("Suchbegriffe für '{}' extrahiert: {}. Kosten: ${}", 
                     suchText, suchbegriffe, costContext.getTotalCost().toPlainString());
            
            return suchbegriffe;
        } catch (Exception e) {
            log.error("Fehler bei der Extraktion der Suchbegriffe für '{}': {}", suchText, e.getMessage());
            return Collections.emptyList(); // Im Fehlerfall eine leere Liste zurückgeben
        }
    }
    
    /**
     * Parst ein JSON-Array aus der LLM-Antwort für die Keyword-Extraktion.
     * 
     * @param response Die Antwort des LLMs (sollte ein JSON-Array sein).
     * @return Eine Liste von Suchbegriffen.
     */
    private List<String> parseKeywords(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Bereinigen des Strings - Alles vor dem ersten [ und nach dem letzten ] entfernen
            String jsonContent = response.trim();
            int startIdx = jsonContent.indexOf('[');
            int endIdx = jsonContent.lastIndexOf(']');
            
            if (startIdx >= 0 && endIdx >= 0 && startIdx < endIdx) {
                jsonContent = jsonContent.substring(startIdx, endIdx + 1);
            } else {
                log.warn("JSON-Array in der Antwort nicht gefunden: {}", response);
                return Collections.emptyList();
            }
            
            // JSON-Array parsen
            return mapper.readValue(jsonContent, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Fehler beim Parsen der Suchbegriffe aus LLM-Antwort: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Berechnet die Textrelevanz für ein Dokument anhand der Suchanfrage mit TF-IDF.
     * Implementiert TF-IDF (Term Frequency - Inverse Document Frequency) für bessere Relevanzberechnung.
     * Verwendet gecachte IDF-Werte für bessere Performance.
     * 
     * @param docContent Der Inhalt des Dokuments (bereits in Kleinbuchstaben).
     * @param keywords Die Suchanfrage.
     * @return Ein Relevanz-Score zwischen 0.0 und 1.0.
     */
    public double berechneTextRelevanz(String docContent, List<String> keywords) {

        if (keywords.isEmpty()) {
            return 0.0;
        }

        //Sinnlose Dokumente rausfiltern
        String cleanText = docContent.replaceAll("[\\s\\.]+", "");

        if(cleanText.length() < 30){
            log.debug("Text ist zu kurs");
            return 0;
        }

        long letterCount = docContent.chars().filter(Character::isLetter).count();
        double letterRatio = (double) letterCount / docContent.length();
        if (letterRatio < 0.3) {
            log.debug("Zu wenige Buchstaben im Dokument: '{}'", docContent.substring(0, Math.min(50, docContent.length())));
            return 0;
        }

        if(docContent.contains("Inhaltsverzeichnis")){
            return 0;
        }

        // Dokument in Wörter zerlegen für TF Berechnung
        List<String> docWords = Arrays.stream(docContent.toLowerCase().split("\\s+"))
                .filter(word -> word.length() > 3)
                .collect(Collectors.toList());
        
        int docLength = docWords.size();
        if (docLength == 0) {
            return 0.0;
        }
        
        // Zähle Häufigkeit jedes Wortes im Dokument (Term Frequency)
        Map<String, Integer> wordFrequencies = new HashMap<>();
        for (String word : docWords) {
            wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0) + 1);
        }
        
        // TF-IDF Score berechnen
        double tfidfScore = 0.0;
        int keywordCount = 0;
        
        // Für jedes Keyword TF-IDF berechnen
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            
            // Prüfen, ob das Keyword überhaupt im Text vorkommt
            if (docContent.contains(lowerKeyword)) {
                keywordCount++;
                
                // Term Frequency (TF): Anzahl der Vorkommen des Terms im Dokument / Dokumentlänge
                int termCount = countOccurrences(docContent, lowerKeyword);
                double tf = (double) termCount / docLength;
                
                // Inverse Document Frequency (IDF): Entweder aus Cache oder berechnen
                double idf;
                if (idfCache.containsKey(lowerKeyword)) {
                    // Aus Cache holen
                    idf = idfCache.get(lowerKeyword);
                } else {
                    // Fallback: Bessere Schätzung als die Wortlänge
                    // Berechne IDF mit einer durchschnittlichen Dokumentfrequenz, die mit der Wortlänge skaliert
                    // Längere Wörter sind tendenziell seltener, aber nicht extrem selten
                    int geschaetzteDokumentFrequenz = Math.max(5, 50 - lowerKeyword.length());
                    int gesamtDokumentAnzahl = 1000; // Annahme einer durchschnittlichen Korpusgröße
                    
                    // IDF-Wert berechnen mit vorhandener Methode
                    idf = berechneTfidfWert(geschaetzteDokumentFrequenz, gesamtDokumentAnzahl);
                    
                    // Im Cache speichern für spätere Nutzung
                    idfCache.put(lowerKeyword, idf);
                }
                
                // Position des ersten Vorkommens als zusätzlicher Faktor
                int position = docContent.indexOf(lowerKeyword);
                double positionFactor = 1.0 - (Math.min(position, 1000) / 1000.0);
                
                // TF-IDF mit Positionsfaktor kombinieren
                double termScore = tf * idf * (0.7 + (positionFactor * 0.3));
                tfidfScore += termScore;
                
                // Debug-Logging für TF-IDF-Werte
                if (log.isDebugEnabled()) {
                    log.debug("Term: '{}', TF: {}, IDF: {}, Position: {}, Score: {}", 
                            lowerKeyword, tf, idf, position, termScore);
                }
            }
        }
        
        // Normalisieren des Scores
        if (keywordCount > 0) {
            // Durchschnitt pro gefundenem Keyword
            double avgTfidfScore = tfidfScore / keywordCount;
            
            // Vollständigkeitsfaktor: Wie viele der Keywords wurden gefunden
            double matchRatio = (double) keywordCount / keywords.size();
            
            // Kombinierter Score aus TF-IDF und Vollständigkeit
            double combinedScore = (avgTfidfScore * 0.7) + (matchRatio * 0.3);
            
            // Skalierung auf einen Wert zwischen 0 und 1
            double normalizedScore = 1.0 - (1.0 / (1.0 + combinedScore));
            
            // Clipping auf 0.0 - 1.0
            return Math.min(1.0, Math.max(0.0, normalizedScore));
        }
        
        return 0.0;
    }
    
    /**
     * Zählt das Vorkommen eines Substrings in einem String.
     *
     * @param text Der Text, in dem gesucht wird.
     * @param subString Der zu suchende Substring.
     * @return Die Anzahl der Vorkommen.
     */
    private int countOccurrences(String text, String subString) {
        int count = 0;
        int idx = 0;
        
        while ((idx = text.indexOf(subString, idx)) != -1) {
            count++;
            idx += subString.length();
        }
        
        return count;
    }

}