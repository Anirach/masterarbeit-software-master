package de.fuh.kn.webapp.llm.prompt;

/**
 * Diese Klasse enthält die Prompt-Templates für die verschiedenen LLM-Anfragen.
 */
public final class PromptTemplates {

    private PromptTemplates() {
        // Private Konstruktor, um Instanziierung zu verhindern
    }

    /**
     * Template für die Bewertung einer Lösung.
     */
    public static final String EVALUATION_TEMPLATE = """
        Bewerte die Antwort zur Aufgabe anhand der Musterlösung.
        Bewerte nach inhaltlicher Korrektheit, nicht nach exakter Übereinstimmung mit der Musterleistung.
        Schreibfehler sollen keinen Punktabzug bedeuten. Worte mit gleicher Bedeutung sollen kein Punktabzug bedeuten.
        Die Aufgabe enthält Platzhalter in der Form \\{feldname:feldtyp:feldlänge\\}, die Antwort und Musterlösung enthält die Werte für diese Platzhalter.
        Beziehe dich beim Feedback auf den Kontext der Aufgabe, nicht auf auf die Feldnamen.
        Leere Antworten sind falsch.
        
        Gib im Key 'punkte' einen Punktwert (0-100) zurück. Gib im Key 'feedback' einen kurzen Feedback-Text zurück. Feedback kann mit $ <formel> $ LaTeX enthalten. Das Feedback soll nicht direkt die Musterlösung verraten.
        Gib für jedes Input-field den Wert 'red' (Antwort falsch), 'yellow' (Antwort fast richtig), 'green' (Antwort richtig) zurück.
        
        {bewertungshinweise}
        
        ------
        Aufgabe:
        {aufgabe}
        ------
        Musterlösung:
        {musterloesung}
        ------
        Antwort:
        {antwort}
        ------
        {format}
        """;

    /**
     * Template für die Generierung einer Erklärung zu einer Aufgabe.
     */
    public static final String EXPLANATION_TEMPLATE = """
        Erkläre das Vorgehen zum Lösen dieser Aufgabe sowie notwendige Hintergrundinformationen, ohne die
        Musterlösung direkt zu verraten. Ausgabe als simples HTML-Fragment (ohne html,head,body Tags) mit Bootstrap-Klassen, ohne weiteren Rahmen oder ```
        
        ------
        Aufgabe:
        {aufgabe}
        ------
        Musterlösung:
        {musterloesung}
        ------
        Antwort des Benutzers:
        {antwort}
        """;

    /**
     * Template für die Generierung von detailliertem Feedback.
     */
    public static final String FEEDBACK_TEMPLATE = """
        Generiere ein konstruktives Feedback für die eingereichte Lösung basierend auf der gegebenen Punktzahl.
        Sei ermutigend und gib konkrete Verbesserungsvorschläge. Vermeide es, die komplette Musterlösung zu verraten.
        Das Feedback sollte maximal 3-4 Sätze lang sein und sich auf die wichtigsten Aspekte konzentrieren.
        
        ------
        Aufgabe:
        {aufgabe}
        ------
        Musterlösung:
        {musterloesung}
        ------
        Antwort:
        {antwort}
        ------
        Erreichte Punktzahl: {punkte}
        """;
        
    /**
     * Template für die Extraktion von Aufgaben aus einer einzelnen PDF-Datei.
     */
    public static final String PDF_IMPORT_TEMPLATE = """
        Du bist ein Experte für die Analyse von Übungsaufgaben aus Vorlesungsunterlagen und Lehrveranstaltungen.
        Deine Aufgabe ist es, aus dem bereitgestellten PDF-Dokument alle Übungsaufgaben zu extrahieren und in einem strukturierten Format zurückzugeben.
        
        # Anweisungen
        
        1. Identifiziere alle Übungsaufgaben im Dokument. Diese sind typischerweise nummeriert (z.B. "Aufgabe 1", "Übung 2") oder durch andere Merkmale als Aufgaben erkennbar.
        2. Für jede Aufgabe:
           - Extrahiere den Titel der Aufgabe (falls vorhanden)
           - Extrahiere die Aufgabenstellung vollständig
           - Identifiziere, ob die Aufgabe in Teilaufgaben unterteilt ist
           - Falls Musterlösungen vorhanden sind, extrahiere diese ebenfalls
        3. Strukturiere die extrahierten Aufgaben im angegebenen JSON-Format
        
        # Berücksichtige folgende Regeln:
        
        - Extrahiere den vollständigen Text der Aufgaben, inklusive aller relevanten Anweisungen, Hinweise und Anforderungen
        - Berücksichtige Formeln und mathematische Ausdrücke und stelle sie in LaTeX-Syntax dar (z.B. $ \\frac{1}{2} $ für einen Bruch)
        - Bei Teilaufgaben (a, b, c oder i, ii, iii) unterteile die Aufgabe entsprechend
        - Achte auf Eingabefelder: Diese sollten als Markdown-Formular-Elemente im Format `{feldname:typ:feldlaengeInZeichen}` oder `{{{feldname}}}` dargestellt werden
        - Typen für Eingabefelder: `:num` für numerische Eingaben (nur Nummern und , erlaubt!), `:text` für kurze Texteingaben, `:tex` für Formel-Eingaben, ohne Suffix für Freitextfelder
        - Eine Aufgabe muss mindestens ein Eingabefeld enthalten, falls in der Aufgabenstellung keines vorhanden ist soll ein {{{loesung}}} hinzugefügt werden
        
        # Beispiele für Eingabefelder im Markdown-Format:
        
        - Zahleneingabe: `Ergebnis der Berechnung: {ergebnis:num:5}`
        - Kurze Texteingabe: `Begriff für diesen Prozess: {begriff:text:20}`
        - Freitextfeld für längere Antworten: `Begründe deine Antwort: {{{begruendung}}}`
        
        # Ausgabeformat (JSON):
        
        ```json
        {
          "aufgaben": [
            {
              "titel": "Titel der Aufgabe",
              "aufgabenText": "Text für komplexe Aufgaben mit gemeinsamer Beschreibung", // nur bei mehr als einer Teilaufgabe, als Text der für mehrere Aufgabenteile gilt z.B. Einleitung zur Aufgabe
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
            },
            // weitere Aufgaben...
          ]
        }
        ```
        
        Gib nur das JSON-Objekt zurück, ohne zusätzliche Erläuterungen. Wenn keine Aufgaben gefunden wurden, gib ein leeres Array zurück.
        {format}
        """;
        
    /**
     * Template für die Extraktion von Aufgaben aus einer Aufgaben-PDF und einer Lösungs-PDF.
     */
    public static final String PDF_PAIR_IMPORT_TEMPLATE = """
        Du bist ein Experte für die Analyse von Übungsaufgaben aus Vorlesungsunterlagen und Lehrveranstaltungen.
        Deine Aufgabe ist es, Aufgaben aus einem Aufgaben-PDF zu extrahieren und mit den Lösungen aus einem Lösungs-PDF zu kombinieren.
        
        # Kontext
        
        Du hast zwei PDF-Dateien erhalten:
        1. Ein Aufgaben-PDF mit den eigentlichen Übungsaufgaben
        2. Ein Lösungs-PDF mit den Musterlösungen zu diesen Aufgaben
        
        # Anweisungen
        
        1. Identifiziere alle Übungsaufgaben im Dokument. Diese sind typischerweise nummeriert (z.B. "Aufgabe 1", "Übung 2") oder durch andere Merkmale als Aufgaben erkennbar.
        2. Für jede Aufgabe:
           - Extrahiere den Titel der Aufgabe (falls vorhanden)
           - Extrahiere die Aufgabenstellung vollständig
           - Identifiziere, ob die Aufgabe in Teilaufgaben unterteilt ist
           - Falls Musterlösungen vorhanden sind, extrahiere diese ebenfalls
        3. Strukturiere die extrahierten Aufgaben im angegebenen JSON-Format
        
        # Berücksichtige folgende Regeln:
        
        - Extrahiere den vollständigen Text der Aufgaben, inklusive aller relevanten Anweisungen, Hinweise und Anforderungen
        - Berücksichtige Formeln und mathematische Ausdrücke und stelle sie in LaTeX-Syntax dar (z.B. $ \\frac{1}{2} $ für einen Bruch)
        - Bei Teilaufgaben (a, b, c oder i, ii, iii) unterteile die Aufgabe entsprechend
        - Achte auf Eingabefelder: Diese sollten als Markdown-Formular-Elemente im Format `{feldname:typ:feldlaengeInZeichen}` oder `{{{feldname}}}` dargestellt werden
        - Typen für Eingabefelder: `:num` für numerische Eingaben (NUR Zahlen und ,.-), `:text` für kurze Texteingaben, `:tex` für Formel-Eingaben, ohne Suffix für Freitextfelder
        
        # Beispiele für Eingabefelder im Markdown-Format:
        
        - Zahleneingabe: `Ergebnis der Berechnung: {ergebnis:num}`
        - Kurze Texteingabe: `Begriff für diesen Prozess: {begriff:text}`
        - Freitextfeld für längere Antworten: `Begründe deine Antwort: {{{begruendung}}}`
        
        # Ausgabeformat (JSON):
        
        ```json
        {
          "aufgaben": [
            {
              "titel": "Titel der Aufgabe",
              "aufgabenText": "Text für komplexe Aufgaben mit gemeinsamer Beschreibung", // nur bei mehr als einer Teilaufgabe, als Text der für mehrere Aufgabenteile gilt z.B. Einleitung zur Aufgabe
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
            },
            // weitere Aufgaben...
          ]
        }
        ```
        
        Gib nur das JSON-Objekt zurück, ohne zusätzliche Erläuterungen. Wenn keine Aufgaben gefunden wurden, gib ein leeres Array zurück.
        {format}
        """;
        
    /**
     * Systemtext für die Optimierung von Suchanfragen.
     */
    public static final String QUERY_TRANSFORMATION_TEMPLATE = """
        Du bist ein Experte für die Reformulierung von Suchanfragen.
        Deine Aufgabe ist es, Suchanfragen so umzuformulieren, dass sie für 
        eine Vektorsuche in Lehrmaterialien optimiert sind. Behalte dabei den 
        ursprünglichen Sinn bei, aber formuliere die Anfrage präziser und 
        vollständiger. Füge relevante Fachbegriffe hinzu, die in akademischen 
        Dokumenten vorkommen könnten. Verwende die Sprache, in der die Eingabe formuliert ist.
        
        Gib nur die Reformulierte Suchanfrage zurück, ohne weitere Erläuterung.
        """;
        
    /**
     * Template für die Extraktion von Suchbegriffen aus einer Suchanfrage.
     * Gibt relevante Fachbegriffe und wichtige Suchbegriffe für eine direkte Textsuche zurück.
     */
    public static final String KEYWORD_EXTRACTION_TEMPLATE = """
        Du bist ein Experte für das Extrahieren von Suchbegriffen.
        Deine Aufgabe ist es, aus einer Suchanfrage relevante Begriffe zu extrahieren, 
        die für eine direkte Textsuche in akademischen Dokumenten geeignet sind.
        Verwende die Sprache, in der die Eingabe ist.
        
        Extrahiere die wichtigsten Begriffe aus der Suchanfrage und gib sie als 
        JSON-Array zurück. Berücksichtige dabei folgende Punkte:
        
        1. Identifiziere Fachbegriffe, die in akademischen Dokumenten vorkommen könnten
        2. Konzentriere dich auf substantielle Schlüsselwörter
        3. Ignoriere Stopwörter und allgemeine Begriffe ("Eigenschaften", "Funktionen")
        4. Füge bei Bedarf Synonyme und verwandte Konzepte hinzu
        5. Behalte sowohl einzelne wichtige Wörter als auch wichtige Phrasen
        6. Die Wörter sollen knapp sein und nicht zusammengesetzt
        7. Gib ein JSON-Array mit Strings zurück
        
        Beispiel-Ausgabe: ["Algorithmus", "Komplexität", "O-Notation", "Laufzeit"]
        """;
        
    /**
     * Template für die Chat-Interaktion im Kontext einer Aufgabe.
     * Verwendet relevante Kursmaterialien zur Beantwortung der Fragen.
     */
    public static final String CHAT_TEMPLATE = """
        Beantworte eine Frage eines Studenten. Beziehe dich bei der Beantwortung auf die vorliegende Übungsaufgabe und
        die Auszüge aus dem Kursmaterial.
        
        Frage des Studenten: {query}
        ---
        Aufgabenstellung:
        {aufgabenText}
        ----
        Musterlösung:
        {musterloesung}
        ----
        Letzter Lösungsversuch des Studenten:
        {loesungsversuch}
        ----
        Relevante Informationen aus dem Kursmaterial:
        {context}
        ----
        
        Beantworte die Frage des Studenten basierend auf diesen Informationen. 
        Wenn du die Antwort nicht kennst, gib ehrlich an, dass du nicht genügend Informationen hast, 
        anstatt falsche Informationen zu erfinden.
        Es kann Markdown verwendet werden, für LaTeX-Syntax kann $ <formel> $ verwendet werden, ohne Multi-Line-Latex.
        
        Frage: {query}
        """;

    public static final String GENERATOR_TEMPLATE = """
        Generiere eine Aufgabe zum Thema "{thema}". Orientiere dich dabei an den Auszügen aus dem Kursmaterial,
        eine Lösung sollte für Studierende nach Bearbeitung des Kursmaterials möglich sein.
        
        Orientiere dich für die Struktur der Aufgabe an den Beispiel-Aufgaben. Eingabefelder innerhalb der Aufgabenstellung sollen im Markdown als `\\{feldname:feldtyp:feldlaenge\\}` für Inline-Felder oder `\\{\\{\\{feldname\\}\\}\\}` mehrzeilige Felder ausgegeben werden. Gültige Feldtypen sind "text", "num" und "tex".
        Für jedes Feld soll eine Musterlösung angegeben werden.
        Für LaTeX-Syntax kann $ <formel> $ verwendet werden.
        
        # Kursmaterial
        {kursmaterial}
        
        # Beispielaufgaben
        {beispielaufgaben}
        
        # Ausgabeformat (JSON):
        
        ```json
            {format}
        ```
        
        Gib nur das JSON-Objekt zurück, ohne zusätzliche Erläuterungen.
        """;
}