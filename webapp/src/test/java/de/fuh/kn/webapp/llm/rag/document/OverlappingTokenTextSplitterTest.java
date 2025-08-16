package de.fuh.kn.webapp.llm.rag.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test für die OverlappingTokenTextSplitter-Klasse.
 * Überprüft die korrekte Funktionalität der Textsegmentierung mit definierter Überlappung.
 */
class OverlappingTokenTextSplitterTest {

    @Test
    @DisplayName("Sollte Text in Chunks mit definierter Überlappung aufteilen")
    void sollteTextInChunksMitUeberlappungAufteilen() {
        // Arrange
        // Ein sehr langer Text, der garantiert in mehrere Chunks aufgeteilt wird
        StringBuilder longTextBuilder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longTextBuilder.append("Dies ist Absatz ").append(i+1).append(" in unserem Testtext. ");
            longTextBuilder.append("Jeder Absatz enthält mehrere Sätze mit verschiedenen Informationen. ");
            longTextBuilder.append("Wir verwenden diesen Text, um das Chunking mit Überlappung zu testen. ");
            longTextBuilder.append("Die Überlappung sollte sicherstellen, dass wichtiger Kontext nicht verloren geht. ");
            longTextBuilder.append("\n\n");
        }
        String text = longTextBuilder.toString();
        
        Document document = new Document(text, Map.of("source", "test"));
        
        // 150 Token entspricht ungefähr dem Text eines Absatzes
        int chunkSize = 150;
        int chunkOverlap = 50; // 50 Token Überlappung
        
        OverlappingTokenTextSplitter splitter = OverlappingTokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withChunkOverlap(chunkOverlap)
                .withMinChunkSizeChars(50)
                .withKeepSeparator(true)
                .build();
        
        // Act
        List<Document> chunks = splitter.apply(List.of(document));
        
        // Assert
        // Wir erwarten mehrere Chunks für diesen langen Text
        assertThat(chunks).isNotEmpty();
        // Der Text sollte in mehrere Chunks zerlegt werden
        assertThat(chunks.size()).isGreaterThan(10);
        
        // Jeder Chunk sollte die source-Metadaten behalten
        for (Document chunk : chunks) {
            assertThat(chunk.getMetadata()).containsEntry("source", "test");

            assertThat(chunk.getMetadata()).containsKey("chunk_index");
        }

        //Testen ob Index von Chunks da ist
        assertEquals(0, chunks.get(0).getMetadata().get("chunk_index"));
        assertEquals(10, chunks.get(10).getMetadata().get("chunk_index"));

        
        // Chunks sollten nicht zu kurz sein
        for (Document chunk : chunks) {
            //Letzter Chunk darf kürzer sein
            if(chunks.indexOf(chunk) == chunks.size() - 1) {
                continue;
            }
            assertThat(chunk.getText().length()).isGreaterThan(50);
        }
        

        // Prüfen wir, ob es zumindest einige Überlappungen zwischen Chunks gibt
        int overlapsFound = 0;
        for (int i = 0; i < chunks.size() - 1; i++) {
            String currentChunk = chunks.get(i).getText();
            String nextChunk = chunks.get(i + 1).getText();
            
            // Ein Teil des aktuellen Chunks sollte im nächsten Chunk enthalten sein
            // Wir prüfen dies, indem wir die letzten Wörter des aktuellen Chunks nehmen
            String[] currentWords = currentChunk.split("\\s+");
            
            if (currentWords.length > 5) {
                // Nehmen wir die letzten 5 Wörter (sollten in der Überlappung enthalten sein)
                StringBuilder lastWords = new StringBuilder();
                for (int j = currentWords.length - 5; j < currentWords.length; j++) {
                    lastWords.append(currentWords[j]).append(" ");
                }
                
                String overlapText = lastWords.toString().trim();
                
                // Wenn die letzten Wörter nicht leer sind und länger als 3 Zeichen
                if (overlapText.length() > 3) {
                    // Der nächste Chunk sollte diese Wörter am Anfang enthalten
                    // Beachten Sie, dass dies keine perfekte Überprüfung ist, da die genaue
                    // Token-Überlappung nicht direkt auf Textebene sichtbar sein könnte
                    boolean containsOverlap = nextChunk.contains(overlapText);
                    
                    if (containsOverlap) {
                        overlapsFound++;
                    }
                    
                    // Ausgabe für Debug-Zwecke
                    if (!containsOverlap) {
                        System.out.println("Keine direkte Überlappung gefunden zwischen Chunks " + i + " und " + (i+1));
                        System.out.println("Gesuchte Überlappung: " + overlapText);
                        System.out.println("Ende Chunk " + i + ": ..." + currentChunk.substring(Math.max(0, currentChunk.length() - 50)));
                        System.out.println("Anfang Chunk " + (i+1) + ": " + nextChunk.substring(0, Math.min(50, nextChunk.length())) + "...");
                    }
                }
            }
        }
        
        // Überprüfen, dass wir zumindest einige Überlappungen gefunden haben
        // (Die genaue Zahl hängt von der Implementierung ab)
        assertThat(overlapsFound).isGreaterThan(0);
        
        // Überprüfen wir, ob wir den gesamten Originaltext abdecken
        String combinedText = chunks.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + " " + b)
                .trim();
        
        // Der kombinierte Text sollte länger sein als der Original-Text (wegen Überlappungen)
        assertThat(combinedText.length()).isGreaterThan(text.length());
        
        // Alle wichtigen Textfragmente sollten im kombinierten Text enthalten sein
        assertThat(combinedText).contains("Absatz 1 in unserem Testtext.");
        assertThat(combinedText).contains("Absatz 2 in unserem Testtext.");
        assertThat(combinedText).contains("Absatz 3 in unserem Testtext.");
        assertThat(combinedText).contains("Absatz 4 in unserem Testtext.");
    }

    @Test
    @DisplayName("Sollte leeren Text korrekt behandeln")
    void sollteLeereTexteKorrektBehandeln() {
        // Arrange
        OverlappingTokenTextSplitter splitter = new OverlappingTokenTextSplitter();
        Document emptyDocument = new Document("", Map.of());
        
        // Act
        List<Document> result = splitter.apply(List.of(emptyDocument));
        
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Sollte sehr kurzen Text als einzelnen Chunk zurückgeben")
    void sollteKurzenTextAlsEinzelnenChunkZurueckgeben() {
        // Arrange
        String shortText = "Dies ist ein kurzer Text.";
        Document shortDocument = new Document(shortText, Map.of("source", "test"));
        
        OverlappingTokenTextSplitter splitter = new OverlappingTokenTextSplitter();
        
        // Act
        List<Document> result = splitter.apply(List.of(shortDocument));
        
        // Assert
        assertThat(result).hasSize(1);
        // Der Text wird getrimmt, also vergleichen wir mit dem getrimmten Original
        assertThat(result.get(0).getText()).isEqualTo(shortText.trim());
    }
    
    @Test
    @DisplayName("Sollte alle Metadaten korrekt auf Chunks übertragen")
    void sollteMetadatenKorrektUebertragen() {
        // Arrange
        String text = """
                Dies ist ein Text mit Metadaten.
                Er enthält mehrere Zeilen
                und sollte in Chunks aufgeteilt werden.
                """;
        
        Map<String, Object> metadata = Map.of(
                "source", "test-file.txt",
                "author", "Test Autor",
                "date", "2023-05-15",
                "category", "test"
        );
        
        Document document = new Document(text, metadata);
        OverlappingTokenTextSplitter splitter = new OverlappingTokenTextSplitter();
        
        // Act
        List<Document> chunks = splitter.apply(List.of(document));
        
        // Assert
        assertThat(chunks).isNotEmpty();
        
        // Überprüfe, ob alle Metadaten in jedem Chunk vorhanden sind
        for (Document chunk : chunks) {
            assertThat(chunk.getMetadata())
                    .containsEntry("source", "test-file.txt")
                    .containsEntry("author", "Test Autor")
                    .containsEntry("date", "2023-05-15")
                    .containsEntry("category", "test");
        }
    }
}