package de.fuh.kn.webapp.chat.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.LoesungsversuchService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.chat.dto.ChatDTO;
import de.fuh.kn.webapp.chat.dto.ChatNachrichtDTO;
import de.fuh.kn.webapp.chat.service.ChatService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.common.markdown.AufgabenMarkdownService;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller für den Chat-Bereich der Student-Ansicht.
 * Ermöglicht Studenten, Chats zu Aufgaben zu starten und Nachrichten zu senden.
 */
@Controller
@RequestMapping("/student/chat")
@RequiredArgsConstructor
public class StudentChatController {

    private final ChatService chatService;
    private final NutzerService nutzerService;
    private final AufgabeService aufgabeService;
    private final TeilaufgabeService teilaufgabeService;
    private final LoesungsversuchService loesungsversuchService;
    private final AufgabenMarkdownService markdownService;
    private final KurseinheitService kurseinheitService;

    /**
     * Zeigt einen Chat zu einer Aufgabe an oder erstellt einen neuen, wenn noch keiner existiert.
     * Unterstützt optional einen Erklärungs-Modus mit vorausgefüllter Anfrage.
     *
     * @param isExplanation Flag, ob es sich um eine Erklärungsanfrage handelt
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @param model Das Model für die View
     * @return Name der Template-Datei
     */
    @GetMapping("/{teilaufgabeId}")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.CHAT_OEFFNEN,
            beschreibung = "Chat zu Aufgabe geöffnet")
    public String zeigeChat(
            @PathVariable("teilaufgabeId") Long teilaufgabeId,
            @RequestParam(value = "isExplanation", required = false, defaultValue = "false") boolean isExplanation,
            Model model) {
        
        // Aktuellen Studenten laden
        Long nutzerID = nutzerService.getAuthenticatedNutzer().getId();
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerID)
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student: " + nutzerID));
        
        ChatDTO chatDTO = chatService.erstelleChatFuerTeilaufgabe(studentDTO.getId(), teilaufgabeId);

        // Optional: Wenn es ein Erklärungsmodus ist, die Anfrage vorbereiten
        if (isExplanation) {
            // Einfache Erklärungsnachricht anzeigen
            String explanationMessage = "Erkläre mir bitte diese Aufgabe.";

            model.addAttribute("initialMessage", explanationMessage);
                    
            // Dem Model ein Flag hinzufügen, um spezielle UI-Elemente anzuzeigen
            model.addAttribute("isExplanationMode", true);
        }
        
        // Markdown für System-Nachrichten in HTML umwandeln
        chatDTO.getNachrichten().stream()
                .filter(ChatNachrichtDTO::getIstSystemNachricht)
                .forEach(nachricht -> {
                    var renderResult = markdownService.renderMarkdownForPreview(nachricht.getInhalt(), 0L);
                    nachricht.setInhaltHtml(renderResult.getHtml());
                });
        
        // Daten an das Model übergeben
        TeilaufgabeDto teilaufgabe = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);
        AufgabeDto aufgabe = aufgabeService.getAufgabeById(teilaufgabe.getAufgabeId());
        
        // Kurseinheit und Kurs-Information für Breadcrumbs laden
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(aufgabe.getKurseinheitId());
        String kursName = kurseinheitService.getKursNameByKurseinheitId(aufgabe.getKurseinheitId());

        model.addAttribute("aufgabe", aufgabe);
        model.addAttribute("chat", chatDTO);
        model.addAttribute("student", studentDTO);
        model.addAttribute("teilaufgabe", teilaufgabe);
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("kursName", kursName);

        return "student/chat/chat-view";
    }

    /**
     * HTMX-Endpunkt zum Senden einer Nachricht und Erhalten einer Systemantwort.
     *
     * @param chatId Die ID des Chats
     * @param nachricht Die Nachricht des Studenten
     * @param isExplanationMode Flag, ob der Erklärungsmodus aktiv ist
     * @param teilaufgabeId Optional: ID der Teilaufgabe im Erklärungsmodus
     * @param model Das Model für die View
     * @return Name des Template-Fragments für HTMX
     */
    @PostMapping("/htmx/sende-nachricht")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.CHAT_NACHRICHT_SENDEN,
            beschreibung = "Chat-Nachricht gesendet")
    public String sendeNachricht(
            @RequestParam("chatId") Long chatId,
            @RequestParam("nachricht") String nachricht,
            @RequestParam(value = "isExplanationMode", required = false, defaultValue = "false") boolean isExplanationMode,
            @RequestParam(value = "teilaufgabeId", required = false) Long teilaufgabeId,
            Model model) {
        
        // Aktuellen Studenten laden
        Long nutzerID = nutzerService.getAuthenticatedNutzer().getId();
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerID)
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student: " + nutzerID));
        
        // Zugriffsprüfung
        if (!chatService.hatChatZugriff(chatId, studentDTO.getId())) {
            throw new IllegalArgumentException("Kein Zugriff auf diesen Chat");
        }
        
        // Nachricht senden
        ChatNachrichtDTO studentenNachricht = chatService.sendeNachricht(chatId, nachricht);
        
        // Systemantwort generieren - im Erklärungsmodus ein vertiefendes Template verwenden
        ChatNachrichtDTO systemAntwort;
        if (isExplanationMode) {
            // Im Erklärungsmodus spezielle Kontext-Anweisungen hinzufügen
            String enhancedPrompt = nachricht + "\n\nBitte erkläre die Konzepte dieser Aufgabe ausführlich. " +
                    "Gehe besonders auf typische Schwierigkeiten ein und erläutere die wesentlichen Punkte in einfachen Worten. " +
                    "Biete hilfreiche Lösungsansätze, ohne die direkte Lösung zu verraten.";
            
            systemAntwort = chatService.generiereAntwort(chatId, enhancedPrompt, true);
        } else {
            // Standard-Anfragemodus
            systemAntwort = chatService.generiereAntwort(chatId, nachricht, false);
        }
        
        // Aktuellen Chat mit allen Nachrichten laden
        ChatDTO aktuellerChat = chatService.getChat(chatId);
        
        // Markdown für System-Nachrichten in HTML umwandeln
        aktuellerChat.getNachrichten().stream()
                .filter(ChatNachrichtDTO::getIstSystemNachricht)
                .forEach(nachricht1 -> {
                    var renderResult = markdownService.renderMarkdownForPreview(nachricht1.getInhalt(), 0L);
                    nachricht1.setInhaltHtml(renderResult.getHtml());
                });
        
        // Daten an das Model übergeben
        model.addAttribute("chat", aktuellerChat);
        model.addAttribute("teilaufgabe", teilaufgabeService.getTeilaufgabeById(teilaufgabeId));

        return "student/chat/fragments/nachrichten-liste :: nachrichten";
    }

}