package de.fuh.kn.webapp.llm.observability;

/**
 * Definiert die verschiedenen Operationstypen für LLM-Anfragen.
 * Wird verwendet, um Kosten nach Operationstyp zu klassifizieren und zuzuordnen.
 */
public enum OperationType {
    
    /**
     * Bewertung einer eingereichten Lösung
     */
    EVALUATION,
    
    /**
     * Generierung einer Erklärung zu einer Aufgabe oder Lösung
     */
    EXPLANATION,
    
    /**
     * Generierung von detailliertem Feedback zu einer Lösung
     */
    FEEDBACK,
    
    /**
     * Generierung einer Antwort in einem Chat
     */
    CHAT_MESSAGE,
    
    /**
     * Embedding für Vektordatenbank
     */
    EMBEDDING,
    
    /**
     * Extraktion von Aufgaben aus PDF-Dateien
     */
    PDF_IMPORT,
    
    /**
     * Sonstige unklassifizierte Operation
     */
    OTHER
}