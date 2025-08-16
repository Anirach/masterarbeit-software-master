package de.fuh.kn.webapp.llm.rag.modules;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.llm.prompt.PromptTemplates;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementierung des QueryAugmenter-Interface für die RAG-basierte Antwortgenerierung im Kontext von Aufgaben.
 * <p>
 * Diese Klasse erweitert Benutzeranfragen um relevanten Kontext aus:
 * <ul>
 *   <li>Aufgabenstellung und Teilaufgaben</li>
 *   <li>Musterlösungen und Bewertungshinweise</li>
 *   <li>Relevanten Dokumenten aus dem Vektorspeicher</li>
 * </ul>
 * <p>
 * Die Klasse ist Teil der RAG-Pipeline (Retrieval Augmented Generation) und wird im
 * RetrievalAugmentationAdvisor verwendet, um den Kontext für die LLM-Anfrage zu erweitern.
 * <p>
 * Verwendung:
 * <pre>
 * {@code
 * RetrievalAugmentationAdvisor.builder()
 *     .documentRetriever(documentRetriever)
 *     .queryAugmenter(new AufgabeContextQueryAugmenter())
 *     .build();
 * }
 * </pre>
 *
 * @see org.springframework.ai.rag.generation.augmentation.QueryAugmenter
 * @see org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor
 */
public class AufgabeContextQueryAugmenter implements QueryAugmenter {

    /**
     * Funktion zur Formatierung der gefundenen Dokumente in einen lesbaren String.
     * Konvertiert jedes Dokument zu einem formatierten String und fügt sie mit Zeilenumbrüchen zusammen.
     */
    private final Function<List<Document>, String> DOCUMENT_FORMATTER = documents -> documents.stream()
            .map(this::documentToString)
            .collect(Collectors.joining(System.lineSeparator()));

    /**
     * Erweitert eine Benutzeranfrage mit Aufgabenkontext und relevanten Dokumenten.
     * <p>
     * Diese Methode:
     * <ol>
     *   <li>Prüft, ob Dokumente gefunden wurden</li>
     *   <li>Extrahiert die Aufgabe aus dem Abfragekontext</li>
     *   <li>Formatiert die Dokumente zu einem lesbaren String</li>
     *   <li>Kombiniert alle Informationen in einem strukturierten Prompt</li>
     *   <li>Gibt eine neue Query mit dem erweiterten Text zurück</li>
     * </ol>
     *
     * @param query Die ursprüngliche Benutzeranfrage, die erweitert werden soll
     * @param documents Die Liste der vom DocumentRetriever gefundenen relevanten Dokumente
     * @return Eine neue Query mit erweitertem Text, der den Aufgabenkontext und relevante Dokumente enthält
     * @throws IllegalArgumentException wenn query oder documents null sind
     */
    @Override
    public Query augment(Query query, List<Document> documents) {
        Assert.notNull(query, "query cannot be null");
        Assert.notNull(documents, "documents cannot be null");

        String documentContext;
        if(!documents.isEmpty()) {
            documentContext = this.DOCUMENT_FORMATTER.apply(documents);
        }else{
            documentContext = "Für diese Anfrage existiert kein relevantes Kursmaterial";
        }

        TeilaufgabeDto teilaufgabe = (TeilaufgabeDto) query.context().get("TEILAUFGABE");
        AufgabeDto aufgabe = (AufgabeDto) query.context().get("AUFGABE");

        Optional<LoesungsVersuchDTO> loesungsversuch = (Optional<LoesungsVersuchDTO>) query.context().get("LOESUNGSVERSUCH");

        boolean explanationMode = (boolean) query.context().get("EXPLANATION_MODE");

        String queryText = query.text();
        if(explanationMode){
            queryText += "Bitte erkläre die Konzepte dieser Aufgabe ausführlich. " +
            "Gehe besonders auf typische Schwierigkeiten ein und erläutere die wesentlichen Punkte in einfachen Worten. " +
                    "Biete hilfreiche Lösungsansätze, ohne die direkte Lösung zu verraten.";
        }

        Map<String, Object> promptParameters = Map.of(
                "query", queryText,
                "context", documentContext,
                "aufgabenText", getAufgabeText(teilaufgabe, aufgabe),
                "musterloesung", getMusterloesungText(teilaufgabe),
                "loesungsversuch", getLoesungsversuchText(loesungsversuch));

        PromptTemplate promptTemplate = PromptTemplate.builder().template(PromptTemplates.CHAT_TEMPLATE).build();
        String text = promptTemplate.render(promptParameters);

        return query.mutate()
                .text(text)
                .build();
    }

    /**
     * Extrahiert den Aufgabentext aus einem AufgabeDto-Objekt.
     * <p>
     * Kombiniert den Hauptaufgabentext und alle Teilaufgabentexte zu einem
     * zusammenhängenden String für die Verwendung im Prompt.
     *
     * @param teilaufgabeDto Die Aufgabe, deren Text extrahiert werden soll
     * @param aufgabe
     * @return Ein String mit dem vollständigen Aufgabentext inklusive aller Teilaufgaben
     */
    private String getAufgabeText(TeilaufgabeDto teilaufgabeDto, AufgabeDto aufgabe) {
        StringBuilder sb = new StringBuilder();

        if(aufgabe.getAufgabenText() != null) {
            sb.append(aufgabe.getAufgabenText()).append("\n");
        }

        sb.append(teilaufgabeDto.getAufgabenstellungMarkdown()).append("\n");

        return sb.toString();
    }

    /**
     * Extrahiert die Musterlösung und Bewertungshinweise aus einem AufgabeDto-Objekt.
     * <p>
     * Sammelt alle Musterlösungsfelder und Bewertungshinweise aus allen Teilaufgaben
     * und formatiert sie für die Verwendung im Prompt.
     *
     * @return Ein String mit allen Musterlösungsfeldern und Bewertungshinweisen
     */
    private String getMusterloesungText(TeilaufgabeDto teilaufgabeDto) {
        StringBuilder sb = new StringBuilder();

        teilaufgabeDto.getMusterloesungFelder().forEach((fieldName, fieldValue) -> {
            if(teilaufgabeDto.getAufgabenstellungMarkdown().contains(fieldName) && fieldValue != null && !fieldValue.isEmpty()) {
                sb.append(fieldName).append(": ").append(fieldValue).append("\n");
            }
        });
        sb.append(teilaufgabeDto.getMusterloesungBewertungshinweise()).append("\n\n");

        return sb.toString();
    }

    private String getLoesungsversuchText(Optional<LoesungsVersuchDTO> loesungsversuchOptional) {

        if(!loesungsversuchOptional.isPresent()) {
            return "Bisher kein Lösungsversuch";
        }

        LoesungsVersuchDTO loesungsVersuchDTO = loesungsversuchOptional.get();

        StringBuilder sb = new StringBuilder();
        loesungsVersuchDTO.getLoesungFelder().forEach((fieldName, fieldValue) -> {
            sb.append(fieldName).append(": ").append(fieldValue).append("\n");
        });

        sb.append("\n");
        sb.append("Erhaltene Punkte: ").append(loesungsVersuchDTO.getBewertungPunkte()).append("/100\n");
        sb.append("Erhaltenes Feedback: ").append(loesungsVersuchDTO.getBewertungFeedback());
        return sb.toString();
    }

    /**
     * Konvertiert ein Document-Objekt in einen formatierten String.
     * <p>
     * Formatiert ein Dokument mit Metadaten (Dateiname, Seitennummer) und
     * bereinigtem Text für die bessere Lesbarkeit im Prompt.
     *
     * @param document Das zu formatierende Dokument
     * @return Ein formatierter String mit Metadaten und Dokumentinhalt
     */
    private String documentToString(Document document) {
        StringBuilder sb = new StringBuilder();

        sb.append("[fileName=").append(document.getMetadata().get("file_name")).append(",page=").append(document.getMetadata().get("page_number")).append("]\n");
        sb.append(document.getText().trim().replaceAll("\\s+", " ")).append("\n\n");

        return sb.toString();
    }
}
