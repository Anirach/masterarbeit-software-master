package de.fuh.kn.webapp.llm.rag.document;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ein erweiterter TextSplitter, der Text in überlappende Chunks basierend auf Tokens aufteilt.
 * Diese Implementierung bietet eine präzisere Kontrolle über die Überlappung zwischen Chunks,
 * was für eine verbesserte RAG-Performance wichtig ist.
 */
public class OverlappingTokenTextSplitter extends TextSplitter {
    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 150;
    private static final int DEFAULT_MIN_CHUNK_SIZE_CHARS = 350;
    private static final int DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int DEFAULT_MAX_NUM_CHUNKS = 10000;
    private static final boolean DEFAULT_KEEP_SEPARATOR = true;

    private final EncodingRegistry registry;
    private final Encoding encoding;
    private final int chunkSize;
    private final int chunkOverlap;
    private final int minChunkSizeChars;
    private final int minChunkLengthToEmbed;
    private final int maxNumChunks;
    private final boolean keepSeparator;

    /**
     * Erstellt einen OverlappingTokenTextSplitter mit Standardwerten.
     */
    public OverlappingTokenTextSplitter() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, DEFAULT_MIN_CHUNK_SIZE_CHARS, 
             DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED, DEFAULT_MAX_NUM_CHUNKS, DEFAULT_KEEP_SEPARATOR);
    }

    /**
     * Erstellt einen OverlappingTokenTextSplitter mit benutzerdefinierten Werten.
     *
     * @param chunkSize Die Zielgröße der Chunks in Tokens.
     * @param chunkOverlap Die Anzahl der überlappenden Tokens zwischen benachbarten Chunks.
     * @param minChunkSizeChars Die Mindestgröße eines Chunks in Zeichen.
     * @param minChunkLengthToEmbed Die Mindestlänge eines Chunks, damit er in den Vector Store aufgenommen wird.
     * @param maxNumChunks Die maximale Anzahl der zu erzeugenden Chunks.
     * @param keepSeparator Flag, ob Separatoren zwischen Chunks beibehalten werden sollen.
     */
    public OverlappingTokenTextSplitter(
            int chunkSize,
            int chunkOverlap,
            int minChunkSizeChars,
            int minChunkLengthToEmbed,
            int maxNumChunks,
            boolean keepSeparator) {
        
        this.registry = Encodings.newLazyEncodingRegistry();
        this.encoding = this.registry.getEncoding(EncodingType.CL100K_BASE);
        this.chunkSize = chunkSize;
        this.chunkOverlap = Math.min(chunkOverlap, chunkSize - 1); // Überlappung darf nicht größer als Chunk-Größe sein
        this.minChunkSizeChars = minChunkSizeChars;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks;
        this.keepSeparator = keepSeparator;
    }

    /**
     * Builder-Methode für eine flüssigere API.
     *
     * @return Ein neuer Builder für OverlappingTokenTextSplitter.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Überschreibt die apply-Methode aus TextSplitter, um Metadaten für den Chunk-Index hinzuzufügen.
     * Dies ermöglicht die Navigation zwischen benachbarten Chunks in der Anwendung.
     *
     * @param documents Die zu verarbeitenden Dokumente
     * @return Eine Liste von Dokumenten mit Chunk-Index-Metadaten
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        // Rufe die Standardimplementierung auf, um die Dokumente zu splitten
        List<Document> splitDocuments = super.apply(documents);

        for(int i = 0; i<splitDocuments.size(); i++) {
            Document document = splitDocuments.get(i);
            document.getMetadata().put("chunk_index", i);
        }
        
        return splitDocuments;
    }

    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Wenn der Text sehr kurz ist, gib ihn direkt als einen einzigen Chunk zurück
        if (isShortText(text)) {
            return List.of(text.trim());
        }

        List<Integer> tokens = getEncodedTokens(text);
        List<String> chunks = new ArrayList<>();
        int numChunks = 0;
        int startIndex = 0;

        while (startIndex < tokens.size() && numChunks < this.maxNumChunks) {
            // Berechne das Ende dieses Chunks (begrenzt durch die verbleibenden Tokens)
            int endIndex = Math.min(startIndex + chunkSize, tokens.size());
            
            if (endIndex <= startIndex) {
                break; // Keine weiteren Tokens mehr
            }

            // Extrahiere die Token-Sublist für diesen Chunk
            List<Integer> chunkTokens = tokens.subList(startIndex, endIndex);
            String chunkText = decodeTokens(chunkTokens);
            
            // Optimiere den Chunk-Endpunkt an natürlichen Grenzen mit Präferenz für Absatzenden
            if (chunkText.length() > minChunkSizeChars) {
                // Zuerst direkt nach einem Absatzende suchen (Zeilenumbruch)
                int paragraphEnd = chunkText.lastIndexOf('\n');
                
                // Wenn ein Absatzende gefunden wurde und es nach der Minimallänge liegt
                if (paragraphEnd != -1 && paragraphEnd > minChunkSizeChars) {
                    // Absatzende gefunden - bevorzuge dies auch wenn es zu einem kürzeren Chunk führt
                    chunkText = chunkText.substring(0, paragraphEnd + 1);
                    int chunkLength = getEncodedTokens(chunkText).size();
                    endIndex = startIndex + chunkLength;
                } else {
                    // Kein passendes Absatzende gefunden, versuche ein Satzende zu finden
                    int sentenceEnd = Math.max(
                        chunkText.lastIndexOf('.'),
                        Math.max(
                            chunkText.lastIndexOf('?'),
                            chunkText.lastIndexOf('!')
                        )
                    );
                    
                    // Wenn ein Satzende gefunden wurde und es nach der Minimallänge liegt
                    if (sentenceEnd != -1 && sentenceEnd > minChunkSizeChars) {
                        chunkText = chunkText.substring(0, sentenceEnd + 1);
                        int chunkLength = getEncodedTokens(chunkText).size();
                        endIndex = startIndex + chunkLength;
                    }
                }
            }

            // Verarbeitung des Chunks
            String chunkTextToAppend = this.keepSeparator ? 
                chunkText.trim() : 
                chunkText.replace(System.lineSeparator(), " ").trim();
                
            if (chunkTextToAppend.length() > this.minChunkLengthToEmbed) {
                chunks.add(chunkTextToAppend);
                numChunks++;
            }

            // Berechne den nächsten Startindex mit Überlappung
            startIndex = endIndex - chunkOverlap;
            
            // Wenn wir nahe am Ende sind und der verbleibende Text zu kurz ist,
            // verarbeiten wir den Rest als letzten Chunk und brechen dann ab
            if (tokens.size() - startIndex < chunkSize / 2) {
                // Wenn noch genug Tokens für einen sinnvollen Chunk übrig sind, verarbeiten wir diesen
                if (tokens.size() - startIndex > minChunkLengthToEmbed) {
                    List<Integer> lastChunkTokens = tokens.subList(startIndex, tokens.size());
                    String lastChunkText = decodeTokens(lastChunkTokens);
                    String lastChunkToAppend = this.keepSeparator ? 
                        lastChunkText.trim() : 
                        lastChunkText.replace(System.lineSeparator(), " ").trim();
                    
                    if (lastChunkToAppend.length() > this.minChunkLengthToEmbed) {
                        chunks.add(lastChunkToAppend);
                    }
                }
                break; // Beende die Schleife, da wir den Rest bereits verarbeitet haben
            }
        }

        // Spezialfall: Wenn keine Chunks erzeugt wurden, aber Text vorhanden ist
        if (chunks.isEmpty() && text.trim().length() > 0) {
            chunks.add(text.trim());
        }

        return chunks;
    }

    /**
     * Findet die letzte Satzgrenze (Punkt, Fragezeichen, Ausrufezeichen) oder Absatzgrenze (Zeilenumbruch) in einem Text.
     * Priorisiert Absatzende (Zeilenumbruch) über Satzende (Punkt, Fragezeichen, Ausrufezeichen).
     *
     * @param text Der zu untersuchende Text.
     * @return Der Index der letzten Satzgrenze oder -1, wenn keine gefunden wurde.
     */
    private int findLastSentenceBoundary(String text) {
        // Zuerst nach Absatzgrenzen suchen, da diese priorisiert werden sollen
        int lastParagraph = text.lastIndexOf('\n');
        if (lastParagraph != -1) {
            return lastParagraph;
        }
        
        // Wenn kein Absatzende gefunden wurde, nach Satzenden suchen
        return Math.max(
            text.lastIndexOf('.'), 
            Math.max(
                text.lastIndexOf('?'), 
                text.lastIndexOf('!')
            )
        );
    }

    /**
     * Konvertiert einen Text in eine Liste von Token-IDs.
     *
     * @param text Der zu tokensisierende Text.
     * @return Eine Liste von Token-IDs.
     */
    private List<Integer> getEncodedTokens(String text) {
        Assert.notNull(text, "Text must not be null");
        return this.encoding.encode(text).boxed();
    }

    /**
     * Dekodiert eine Liste von Token-IDs zurück in Text.
     *
     * @param tokens Die zu dekodierenden Token-IDs.
     * @return Der dekodierte Text.
     */
    private String decodeTokens(List<Integer> tokens) {
        Assert.notNull(tokens, "Tokens must not be null");
        IntArrayList tokensIntArray = new IntArrayList(tokens.size());
        Objects.requireNonNull(tokensIntArray);
        tokens.forEach(tokensIntArray::add);
        return this.encoding.decode(tokensIntArray);
    }
    
    /**
     * Bestimmt, ob ein Text als "kurz" betrachtet werden sollte.
     * Kurze Texte sollten nicht in Chunks aufgeteilt werden.
     *
     * @param text Der zu prüfende Text.
     * @return true, wenn der Text als "kurz" betrachtet werden sollte.
     */
    private boolean isShortText(String text) {
        if (text == null || text.length() < this.minChunkSizeChars * 2) {
            return true;
        }
        
        List<Integer> tokens = getEncodedTokens(text);
        // Wenn die Anzahl der Tokens kleiner als der Chunk-Size ist, 
        // betrachten wir den Text als kurz und es ist nicht sinnvoll, 
        // ihn weiter zu teilen
        return tokens.size() <= this.chunkSize;
    }

    /**
     * Builder-Klasse für OverlappingTokenTextSplitter.
     */
    public static final class Builder {
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private int chunkOverlap = DEFAULT_CHUNK_OVERLAP;
        private int minChunkSizeChars = DEFAULT_MIN_CHUNK_SIZE_CHARS;
        private int minChunkLengthToEmbed = DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED;
        private int maxNumChunks = DEFAULT_MAX_NUM_CHUNKS;
        private boolean keepSeparator = DEFAULT_KEEP_SEPARATOR;

        private Builder() {
        }

        public Builder withChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder withChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
            return this;
        }

        public Builder withMinChunkSizeChars(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;
            return this;
        }

        public Builder withMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;
            return this;
        }

        public Builder withMaxNumChunks(int maxNumChunks) {
            this.maxNumChunks = maxNumChunks;
            return this;
        }

        public Builder withKeepSeparator(boolean keepSeparator) {
            this.keepSeparator = keepSeparator;
            return this;
        }

        public OverlappingTokenTextSplitter build() {
            return new OverlappingTokenTextSplitter(
                this.chunkSize, 
                this.chunkOverlap, 
                this.minChunkSizeChars,
                this.minChunkLengthToEmbed, 
                this.maxNumChunks, 
                this.keepSeparator
            );
        }
    }
}