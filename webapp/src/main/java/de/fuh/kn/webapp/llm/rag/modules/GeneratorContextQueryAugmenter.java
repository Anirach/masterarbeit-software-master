package de.fuh.kn.webapp.llm.rag.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.llm.prompt.PromptTemplates;
import lombok.SneakyThrows;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GeneratorContextQueryAugmenter implements QueryAugmenter {

    /**
     * Funktion zur Formatierung der gefundenen Dokumente in einen lesbaren String.
     * Konvertiert jedes Dokument zu einem formatierten String und fügt sie mit Zeilenumbrüchen zusammen.
     */
    private final Function<List<Document>, String> DOCUMENT_FORMATTER = documents -> documents.stream()
            .map(this::documentToString)
            .collect(Collectors.joining(System.lineSeparator()));

    private final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
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

        List<AufgabeDto> aufgaben = (List<AufgabeDto>) query.context().get("BEISPIELAUFGABEN");
        String aufgabenContext;
        if (aufgaben != null && !aufgaben.isEmpty()) {
            aufgabenContext = mapper.writeValueAsString(aufgaben);
        } else {
            aufgabenContext = "[]";
        }

        String jsonFormat = """
            {
              "titel": "Titel der Aufgabe",
              "aufgabenText": "Text für komplexe Aufgaben mit gemeinsamer Beschreibung", // nur bei mehr als einer Teilaufgabe, als Text der für mehrere Aufgabenteile gilt
              "teilaufgaben": [
                {
                  "aufgabenstellungMarkdown": "Vollständige Aufgabenstellung als Markdown mit Eingabefeldern",
                  "reihenfolge": 1,
                  "musterloesungFelder": {
                    "feldname1": "Musterlösung für Feld 1",
                    "feldname2": "Musterlösung für Feld 2"
                  },
                  "musterloesungBewertungshinweise": "Hinweise zur Bewertung der Lösung"
                },
                // weitere Teilaufgaben...
              ]
            }
            """;

        Map<String, Object> promptParameters = Map.of(
                "thema", query.text(),
                "kursmaterial", documentContext,
                "format", jsonFormat,
                "beispielaufgaben", aufgabenContext);

        PromptTemplate promptTemplate = PromptTemplate.builder().template(PromptTemplates.GENERATOR_TEMPLATE).build();
        String text = promptTemplate.render(promptParameters);

        return query.mutate()
                .text(text)
                .build();
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
