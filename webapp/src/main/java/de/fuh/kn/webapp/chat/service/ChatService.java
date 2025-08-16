package de.fuh.kn.webapp.chat.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.service.LoesungsversuchService;
import de.fuh.kn.webapp.chat.dto.ChatDTO;
import de.fuh.kn.webapp.chat.dto.ChatMapper;
import de.fuh.kn.webapp.chat.dto.ChatNachrichtDTO;
import de.fuh.kn.webapp.chat.dto.ChatNachrichtMapper;
import de.fuh.kn.webapp.llm.dto.chat.LlmChatRequestDto;
import de.fuh.kn.webapp.llm.dto.chat.LlmChatResponseDto;
import de.fuh.kn.webapp.llm.service.LlmChatService;
import de.fuh.kn.webapp.persistence.entity.*;
import de.fuh.kn.webapp.persistence.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service für die Verwaltung von Chat-Funktionen.
 * Implementiert die grundlegenden Operationen für die Interaktion mit Chats,
 * einschließlich RAG-basierter Antwortgenerierung.
 */
@Service
@Slf4j
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatNachrichtRepository chatNachrichtRepository;
    private final StudentRepository studentRepository;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final ChatMapper chatMapper;
    private final ChatNachrichtMapper chatNachrichtMapper;
    private final LoesungsversuchService loesungsversuchService;
    private final KursMaterialRepository kursMaterialRepository;
    private final TeilaufgabeMapper teilaufgabeMapper;
    private final AufgabeMapper aufgabeMapper;
    private final LlmChatService llmChatService;

    public ChatService(
            ChatRepository chatRepository,
            ChatNachrichtRepository chatNachrichtRepository,
            StudentRepository studentRepository,
            TeilaufgabeRepository teilaufgabeRepository,
            ChatMapper chatMapper,
            ChatNachrichtMapper chatNachrichtMapper,
            LoesungsversuchService loesungsversuchService,
            KursMaterialRepository kursMaterialRepository,
            TeilaufgabeMapper teilaufgabeMapper,
            AufgabeMapper aufgabeMapper,
            LlmChatService llmChatService) {
        
        this.chatRepository = chatRepository;
        this.chatNachrichtRepository = chatNachrichtRepository;
        this.studentRepository = studentRepository;
        this.teilaufgabeRepository = teilaufgabeRepository;
        this.chatMapper = chatMapper;
        this.chatNachrichtMapper = chatNachrichtMapper;
        this.loesungsversuchService = loesungsversuchService;
        this.kursMaterialRepository = kursMaterialRepository;
        this.teilaufgabeMapper = teilaufgabeMapper;
        this.aufgabeMapper = aufgabeMapper;
        this.llmChatService = llmChatService;
    }
    

    /**
     * Erstellt einen neuen Chat für einen Studenten zu einer Teilaufgabe.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Der erstellte Chat als DTO oder null, wenn Student oder Teilaufgabe nicht gefunden wurden
     */
    public ChatDTO erstelleChatFuerTeilaufgabe(Long studentId, Long teilaufgabeId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Teilaufgabe> teilaufgabeOpt = teilaufgabeRepository.findById(teilaufgabeId);
        
        if (studentOpt.isEmpty() || teilaufgabeOpt.isEmpty()) {
            log.warn("Chat konnte nicht erstellt werden. Student ID {} oder Teilaufgabe ID {} nicht gefunden", 
                    studentId, teilaufgabeId);
            return null;
        }
        
        Student student = studentOpt.get();
        Teilaufgabe teilaufgabe = teilaufgabeOpt.get();
        
        // Überprüfen, ob bereits ein Chat für diese Teilaufgabe existiert
        List<Chat> existierendeChats = chatRepository.findByStudentAndTeilaufgabe(student, teilaufgabe);
        if (!existierendeChats.isEmpty()) {
            log.info("Bestehender Chat gefunden für Student {} und Teilaufgabe {}", studentId, teilaufgabeId);
            return chatMapper.toDto(existierendeChats.get(0));
        }
        
        // Neuen Chat erstellen
        Chat chat = new Chat();
        chat.setZeitpunkt(LocalDateTime.now());
        chat.setStudent(student);
        chat.setTeilaufgabe(teilaufgabe);

        Chat gespeicherterChat = chatRepository.save(chat);
        log.info("Neuer Chat erstellt für Student {} und Teilaufgabe {}", studentId, teilaufgabeId);
        
        return chatMapper.toDto(gespeicherterChat);
    }

    /**
     * Sendet eine Nachricht von einem Studenten an das System.
     *
     * @param chatId Die ID des Chats
     * @param nachricht Der Inhalt der Nachricht
     * @return Die erstellte Nachricht als DTO oder null, wenn der Chat nicht gefunden wurde
     */
    public ChatNachrichtDTO sendeNachricht(Long chatId, String nachricht) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            log.warn("Nachricht konnte nicht gesendet werden. Chat ID {} nicht gefunden", chatId);
            return null;
        }
        
        ChatNachricht chatNachricht = new ChatNachricht();
        chatNachricht.setZeitpunkt(LocalDateTime.now());
        chatNachricht.setInhalt(nachricht);
        chatNachricht.setIstSystemNachricht(false); // Studentennachricht
        chatNachricht.setChat(chatOpt.get());
        
        ChatNachricht gespeicherteNachricht = chatNachrichtRepository.save(chatNachricht);
        log.info("Neue Studentennachricht gespeichert für Chat {}", chatId);
        
        return chatNachrichtMapper.toDto(gespeicherteNachricht);
    }

    /**
     * Generiert eine Antwort vom System auf die Nachricht eines Studenten.
     * Delegiert die LLM-Verarbeitung an den LlmChatService und verwaltet die
     * Persistierung der Antwort sowie der gefundenen Dokumentreferenzen.
     *
     * @param chatId Die ID des Chats
     * @param studentenNachricht Die Nachricht des Studenten, auf die geantwortet werden soll
     * @param explanationMode Erklärung für die Aufgabe
     * @return Die generierte Antwort als DTO oder null, wenn der Chat nicht gefunden wurde
     */
    public ChatNachrichtDTO generiereAntwort(Long chatId, String studentenNachricht, boolean explanationMode) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            log.warn("Antwort konnte nicht generiert werden. Chat ID {} nicht gefunden", chatId);
            return null;
        }
        
        Chat chat = chatOpt.get();
        Teilaufgabe teilaufgabe = chat.getTeilaufgabe();
        Aufgabe aufgabe = teilaufgabe.getAufgabe();
        
        try {
            // Vorherige Nachrichten für Kontext laden (max. letzte 5)
            List<ChatNachricht> vorherigeNachrichten = chatNachrichtRepository.findByChatOrderByZeitpunktAsc(chat);
            List<ChatNachricht> kontextNachrichten = vorherigeNachrichten.size() > 5
                ? vorherigeNachrichten.subList(vorherigeNachrichten.size() - 5, vorherigeNachrichten.size())
                : vorherigeNachrichten;
            
            // Lösungsversuch laden, falls vorhanden
            Optional<LoesungsVersuchDTO> lastLoesungsVersuch = loesungsversuchService.findeNeuesterLoesungsversuch(
                    chat.getStudent().getId(), chat.getTeilaufgabe().getId());

            // Request DTO für LlmChatService erstellen
            LlmChatRequestDto request = new LlmChatRequestDto();
            request.setUserMessage(studentenNachricht);
            request.setKursId(aufgabe.getKurseinheit().getKurs().getId());
            request.setAufgabe(aufgabeMapper.toDto(aufgabe));
            request.setTeilaufgabe(teilaufgabeMapper.toDto(teilaufgabe));
            request.setLoesungsversuch(lastLoesungsVersuch);
            request.setExplanationMode(explanationMode);
            
            // Chat-Historie umwandeln
            List<LlmChatRequestDto.ChatMessage> chatHistory = new ArrayList<>();
            for (ChatNachricht nachricht : kontextNachrichten) {
                chatHistory.add(new LlmChatRequestDto.ChatMessage(
                        nachricht.getInhalt(), 
                        nachricht.getIstSystemNachricht()
                ));
            }
            request.setChatHistory(chatHistory);

            // LLM-Service aufrufen
            LlmChatResponseDto llmResponse = llmChatService.generiereAntwort(request);

            // Antwort in der Datenbank speichern
            ChatNachricht antwortNachricht = new ChatNachricht();
            antwortNachricht.setZeitpunkt(LocalDateTime.now());
            antwortNachricht.setInhalt(llmResponse.getContent());
            antwortNachricht.setIstSystemNachricht(true);
            antwortNachricht.setChat(chat);
            antwortNachricht.setInputToken(llmResponse.getInputTokens());
            antwortNachricht.setOutputToken(llmResponse.getOutputTokens());
            antwortNachricht.setModell(llmResponse.getModel());
            antwortNachricht.setKosten(llmResponse.getCost());

            // Dokumentreferenzen hinzufügen
            if (llmResponse.getDocumentReferences() != null) {
                for (LlmChatResponseDto.DocumentReference docRef : llmResponse.getDocumentReferences()) {
                    Optional<KursMaterial> optionalKursMaterial = kursMaterialRepository.findById(docRef.getKursMaterialId());
                    
                    if (optionalKursMaterial.isPresent()) {
                        KursMaterial kursMaterial = optionalKursMaterial.get();
                        
                        ChatNachrichtReferenz referenz = new ChatNachrichtReferenz();
                        referenz.setChatNachricht(antwortNachricht);
                        referenz.setKursMaterial(kursMaterial);
                        if (docRef.getPageNumber() != null) {
                            referenz.setSeitennummer(docRef.getPageNumber());
                        }
                        
                        antwortNachricht.getReferenzen().add(referenz);
                    }
                }
            }
            
            // Chat-Nachricht speichern
            ChatNachricht gespeicherteAntwort = chatNachrichtRepository.save(antwortNachricht);
            
            log.info("Chat-Antwort für Chat {} generiert. Modell: {}, Input-Token: {}, Output-Token: {}, Kosten: ${}", 
                    chatId, 
                    llmResponse.getModel(),
                    llmResponse.getInputTokens(),
                    llmResponse.getOutputTokens(),
                    llmResponse.getCost().toPlainString());
            
            return chatNachrichtMapper.toDto(gespeicherteAntwort);
            
        } catch (Exception e) {
            log.error("Fehler bei der Generierung der Antwort für Chat {}: {}", chatId, e.getMessage(), e);
            
            // Fallback-Antwort erstellen
            ChatNachricht fallbackAntwort = new ChatNachricht();
            fallbackAntwort.setZeitpunkt(LocalDateTime.now());
            fallbackAntwort.setInhalt("Entschuldigung, bei der Generierung einer Antwort ist ein Fehler aufgetreten. " +
                    "Bitte versuche es später noch einmal oder formuliere deine Frage anders.");
            fallbackAntwort.setIstSystemNachricht(true);
            fallbackAntwort.setChat(chat);
            
            ChatNachricht gespeicherteFallbackAntwort = chatNachrichtRepository.save(fallbackAntwort);
            log.info("Fallback-Antwort für Chat {} erstellt", chatId);
            
            return chatNachrichtMapper.toDto(gespeicherteFallbackAntwort);
        }
    }

    /**
     * Ruft einen spezifischen Chat mit allen Nachrichten ab.
     *
     * @param chatId Die ID des Chats
     * @return Der Chat als DTO mit allen zugehörigen Nachrichten
     */
    @Transactional(readOnly = true)
    public ChatDTO getChat(Long chatId) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            log.warn("Chat konnte nicht abgerufen werden. Chat ID {} nicht gefunden", chatId);
            return null;
        }
        
        Chat chat = chatOpt.get();
        // Nachrichten explizit laden, falls sie nicht bereits geladen sind
        chat.getNachrichten().size(); // Trigger lazy loading
        
        return chatMapper.toDto(chat);
    }

    /**
     * Prüft, ob ein Student auf einen bestimmten Chat zugreifen darf.
     *
     * @param chatId Die ID des Chats
     * @param studentId Die ID des Studenten
     * @return true, wenn der Zugriff erlaubt ist, sonst false
     */
    @Transactional(readOnly = true)
    public boolean hatChatZugriff(Long chatId, Long studentId) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            return false;
        }
        
        Chat chat = chatOpt.get();
        return chat.getStudent().getId().equals(studentId);
    }
}