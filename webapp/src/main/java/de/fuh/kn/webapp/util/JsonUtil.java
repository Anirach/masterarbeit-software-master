package de.fuh.kn.webapp.util;

public class JsonUtil {

    /**
     * Bereinigt JSON-Antworten von LLMs durch Escaping von Backslashes.
     *
     * @param jsonResponse Die rohe JSON-Antwort vom LLM
     * @return Die bereinigte JSON-Antwort mit korrekt escapeten Backslashes
     */
    public static String sanitizeJsonResponse(String jsonResponse) {
        return jsonResponse;
        //wird dann irgendwie doch nicht benötigt..
//        if (jsonResponse == null || jsonResponse.isEmpty()) {
//            return jsonResponse;
//        }
//
//        // Escape backslashes nur innerhalb von JSON-String-Werten
//        // Verwende eine einfache Regex-basierte Lösung für häufige LaTeX-Ausdrücke
//        return jsonResponse.replace("\\\\", "\\");
    }

}
