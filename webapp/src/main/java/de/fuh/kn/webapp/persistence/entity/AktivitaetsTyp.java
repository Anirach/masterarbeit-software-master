package de.fuh.kn.webapp.persistence.entity;

import lombok.Getter;

/**
 * Enum zur Kategorisierung verschiedener Arten von Benutzeraktivitäten im System.
 * Wird zur Klassifizierung von Aktivitäten in der Aktivitätsprotokollierung verwendet.
 * Für jede Aktivität gibt es einen Anzeigenamen sowie eine Bootstrap-Icon-Klasse
 */
@Getter
public enum AktivitaetsTyp {
    // Nutzeraktivitäten
    LOGIN("Login", "bi-person-lock"),
    LOGOUT("Logout", "bi-box-arrow-right"),
    REGISTRIEREN("Nutzer registriert", "bi-person-plus"),
    NUTZER_ERSTELLT("Nutzer erstellt", "bi-person-check"),
    PROFIL_BEARBEITEN("Profil bearbeitet", "bi-pencil"),
    PASSWORT_AENDERN("Passwort geändert", "bi-key"),

    // Studierendenaktivitäten
    AUFGABE_BEWERTEN("Aufgabe bewertet", "bi-star"),
    AUFGABE_ABSCHLIESSEN("Aufgabe abgeschlossen", "bi-check-circle"),
    AUFGABE_ERKLAERUNG("Aufgabenerklärung angefordert", "bi-question-circle"),
    AUFGABE_FEEDBACK("Verbesserungsvorschläge angefordert", "bi-signpost-2"),
    AUFGABE_VERSUCHE_ANZEIGEN("Lösungsversuche angezeigt", "bi-list-check"),
    AUFGABE_ZURUECKSETZEN("Lösungsversuche zurückgesetzt", "bi-arrow-repeat"),
    
    // Chat-Aktivitäten
    CHAT_OEFFNEN("Chat geöffnet", "bi-chat-left-dots"),
    CHAT_NACHRICHT_SENDEN("Chat-Nachricht gesendet", "bi-chat-text"),
    CHAT_STARTEN("Chat gestartet", "bi-chat-left-dots"),
    FRAGE_STELLEN("Frage gestellt", "bi-question-circle"),

    // Kursbetreuungsaktivitäten
    KURS_BEARBEITEN("Kurs bearbeitet", "bi-book"),
    KURS_LOESCHEN("Kurs gelöscht", "bi-trash"),
    KURSEINHEIT_BEARBEITEN("Kurseinheit bearbeitet", "bi-card-list"),
    KURSEINHEIT_LOESCHEN("Kurseinheit gelöscht", "bi-trash"),
    KURSMATERIAL_HOCHLADEN("Kursmaterial hochgeladen", "bi-upload"),
    KURSMATERIAL_LOESCHEN("Kursmaterial gelöscht", "bi-trash"),
    AUFGABE_ERSTELLEN("Aufgabe erstellt", "bi-pencil-square"),
    AUFGABE_BEARBEITEN("Aufgabe bearbeitet", "bi-pencil-square"),
    AUFGABE_LOESCHEN("Aufgabe gelöscht", "bi-trash"),
    NUTZER_ANLEGEN("Nutzer angelegt", "bi-person-add"),
    NUTZER_BEARBEITEN("Nutzer bearbeitet", "bi-pencil"),
    NUTZER_LOESCHEN("Nutzer gelöscht", "bi-person-x"),
    BELEGUNG_ERSTELLEN("Belegung erstellt", "bi-file-earmark-plus"),
    BELEGUNG_LOESCHEN("Belegung gelöscht", "bi-file-earmark-x"),

    // Export/Import Aktivitäten
    KURS_EXPORTIEREN("Kurs exportiert", "bi-download"),
    KURS_IMPORTIEREN("Kurs importiert", "bi-upload"),
    AUFGABE_EXPORTIEREN("Aufgabe exportiert", "bi-download"),
    AUFGABE_IMPORTIEREN("Aufgaben importiert", "bi-upload"),

    ;

    private final String displayName;
    private final String iconClass;

    /**
     * Enum einer Aktivität
     * @param displayName Anzeigename für die Aktivität
     * @param iconClass   Bootstrap-Icon-Klasse (bi-xxx)
     */
    AktivitaetsTyp(String displayName, String iconClass) {
        this.displayName = displayName;
        this.iconClass = iconClass;
    }
}
