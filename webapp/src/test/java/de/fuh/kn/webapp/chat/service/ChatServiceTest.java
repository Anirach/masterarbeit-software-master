package de.fuh.kn.webapp.chat.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;
    
    @Mock
    private ChatNachrichtRepository chatNachrichtRepository;
    
    @Mock
    private StudentRepository studentRepository;
    
    @Mock
    private TeilaufgabeRepository teilaufgabeRepository;
    
    @Mock
    private ChatMapper chatMapper;
    
    @Mock
    private ChatNachrichtMapper chatNachrichtMapper;
    
    @Mock
    private LoesungsversuchService loesungsversuchService;
    
    @Mock
    private KursMaterialRepository kursMaterialRepository;
    
    @Mock
    private TeilaufgabeMapper teilaufgabeMapper;
    
    @Mock
    private AufgabeMapper aufgabeMapper;
    
    @Mock
    private LlmChatService llmChatService;
    
    @InjectMocks
    private ChatService chatService;
    
    private Student testStudent;
    private Teilaufgabe testTeilaufgabe;
    private Aufgabe testAufgabe;
    private Kurseinheit testKurseinheit;
    private Kurs testKurs;
    private Chat testChat;
    private ChatDTO testChatDTO;
    private ChatNachricht testChatNachricht;
    private ChatNachrichtDTO testChatNachrichtDTO;
    
    @BeforeEach
    void setUp() {
        // Test-Entitäten erstellen
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setVorname("Max");
        testStudent.setNachname("Mustermann");
        
        testKurs = new Kurs();
        testKurs.setId(1L);
        testKurs.setName("Testkurs");
        
        testKurseinheit = new Kurseinheit();
        testKurseinheit.setId(1L);
        testKurseinheit.setName("Testeinheit");
        testKurseinheit.setKurs(testKurs);
        
        testAufgabe = new Aufgabe();
        testAufgabe.setId(1L);
        testAufgabe.setTitel("Testaufgabe");
        testAufgabe.setKurseinheit(testKurseinheit);
        
        testTeilaufgabe = new Teilaufgabe();
        testTeilaufgabe.setId(1L);
        testTeilaufgabe.setAufgabenstellungMarkdown("Testteilaufgabe");
        testTeilaufgabe.setAufgabe(testAufgabe);
        
        testChat = new Chat();
        testChat.setId(1L);
        testChat.setStudent(testStudent);
        testChat.setTeilaufgabe(testTeilaufgabe);
        testChat.setZeitpunkt(LocalDateTime.now());
        testChat.setNachrichten(new ArrayList<>());
        
        testChatDTO = new ChatDTO();
        testChatDTO.setId(1L);
        
        testChatNachricht = new ChatNachricht();
        testChatNachricht.setId(1L);
        testChatNachricht.setInhalt("Test Nachricht");
        testChatNachricht.setZeitpunkt(LocalDateTime.now());
        testChatNachricht.setIstSystemNachricht(false);
        testChatNachricht.setChat(testChat);
        
        testChatNachrichtDTO = new ChatNachrichtDTO();
        testChatNachrichtDTO.setId(1L);
        testChatNachrichtDTO.setInhalt("Test Nachricht");
    }
    
    @Test
    void erstelleChatFuerTeilaufgabe_MitGueltigemStudentUndTeilaufgabe_ErstelltNeuenChat() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(testTeilaufgabe));
        when(chatRepository.findByStudentAndTeilaufgabe(testStudent, testTeilaufgabe)).thenReturn(new ArrayList<>());
        when(chatRepository.save(any(Chat.class))).thenReturn(testChat);
        when(chatMapper.toDto(testChat)).thenReturn(testChatDTO);
        
        // Act
        ChatDTO result = chatService.erstelleChatFuerTeilaufgabe(1L, 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(chatRepository).save(any(Chat.class));
        verify(chatMapper).toDto(testChat);
    }
    
    @Test
    void erstelleChatFuerTeilaufgabe_MitBereitsExistierendemChat_GibtExistierendenChatZurueck() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(testTeilaufgabe));
        when(chatRepository.findByStudentAndTeilaufgabe(testStudent, testTeilaufgabe)).thenReturn(Arrays.asList(testChat));
        when(chatMapper.toDto(testChat)).thenReturn(testChatDTO);
        
        // Act
        ChatDTO result = chatService.erstelleChatFuerTeilaufgabe(1L, 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(chatRepository, never()).save(any(Chat.class));
        verify(chatMapper).toDto(testChat);
    }
    
    @Test
    void erstelleChatFuerTeilaufgabe_MitUngueltigemStudent_GibtNullZurueck() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.of(testTeilaufgabe));
        
        // Act
        ChatDTO result = chatService.erstelleChatFuerTeilaufgabe(1L, 1L);
        
        // Assert
        assertNull(result);
        verify(chatRepository, never()).save(any(Chat.class));
        verify(chatMapper, never()).toDto(any(Chat.class));
    }
    
    @Test
    void erstelleChatFuerTeilaufgabe_MitUngueltigerTeilaufgabe_GibtNullZurueck() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(teilaufgabeRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act
        ChatDTO result = chatService.erstelleChatFuerTeilaufgabe(1L, 1L);
        
        // Assert
        assertNull(result);
        verify(chatRepository, never()).save(any(Chat.class));
        verify(chatMapper, never()).toDto(any(Chat.class));
    }
    
    @Test
    void sendeNachricht_MitGueltigemChat_ErstelltNeueNachricht() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(chatNachrichtRepository.save(any(ChatNachricht.class))).thenReturn(testChatNachricht);
        when(chatNachrichtMapper.toDto(testChatNachricht)).thenReturn(testChatNachrichtDTO);
        
        // Act
        ChatNachrichtDTO result = chatService.sendeNachricht(1L, "Test Nachricht");
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Nachricht", result.getInhalt());
        
        ArgumentCaptor<ChatNachricht> captor = ArgumentCaptor.forClass(ChatNachricht.class);
        verify(chatNachrichtRepository).save(captor.capture());
        ChatNachricht gespeicherteNachricht = captor.getValue();
        assertFalse(gespeicherteNachricht.getIstSystemNachricht());
        assertEquals("Test Nachricht", gespeicherteNachricht.getInhalt());
        assertEquals(testChat, gespeicherteNachricht.getChat());
    }
    
    @Test
    void sendeNachricht_MitUngueltigemChat_GibtNullZurueck() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act
        ChatNachrichtDTO result = chatService.sendeNachricht(1L, "Test Nachricht");
        
        // Assert
        assertNull(result);
        verify(chatNachrichtRepository, never()).save(any(ChatNachricht.class));
    }
    
    @Test
    void generiereAntwort_MitGueltigemChat_ErstelltSystemAntwort() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(chatNachrichtRepository.findByChatOrderByZeitpunktAsc(testChat)).thenReturn(new ArrayList<>());
        when(loesungsversuchService.findeNeuesterLoesungsversuch(1L, 1L)).thenReturn(Optional.empty());
        
        AufgabeDto aufgabeDTO = new AufgabeDto();
        TeilaufgabeDto teilaufgabeDTO = new TeilaufgabeDto();
        when(aufgabeMapper.toDto(testAufgabe)).thenReturn(aufgabeDTO);
        when(teilaufgabeMapper.toDto(testTeilaufgabe)).thenReturn(teilaufgabeDTO);
        
        LlmChatResponseDto llmResponse = new LlmChatResponseDto();
        llmResponse.setContent("Generierte Antwort");
        llmResponse.setModel("gpt-4");
        llmResponse.setInputTokens(100);
        llmResponse.setOutputTokens(50);
        llmResponse.setCost(BigDecimal.valueOf(0.01));
        llmResponse.setDocumentReferences(new ArrayList<>());
        
        when(llmChatService.generiereAntwort(any(LlmChatRequestDto.class))).thenReturn(llmResponse);
        
        ChatNachricht systemNachricht = new ChatNachricht();
        systemNachricht.setId(2L);
        systemNachricht.setInhalt("Generierte Antwort");
        systemNachricht.setIstSystemNachricht(true);
        when(chatNachrichtRepository.save(any(ChatNachricht.class))).thenReturn(systemNachricht);
        
        ChatNachrichtDTO systemNachrichtDTO = new ChatNachrichtDTO();
        systemNachrichtDTO.setInhalt("Generierte Antwort");
        when(chatNachrichtMapper.toDto(systemNachricht)).thenReturn(systemNachrichtDTO);
        
        // Act
        ChatNachrichtDTO result = chatService.generiereAntwort(1L, "Studentenfrage", false);
        
        // Assert
        assertNotNull(result);
        assertEquals("Generierte Antwort", result.getInhalt());
        
        ArgumentCaptor<ChatNachricht> captor = ArgumentCaptor.forClass(ChatNachricht.class);
        verify(chatNachrichtRepository).save(captor.capture());
        ChatNachricht gespeicherteNachricht = captor.getValue();
        assertTrue(gespeicherteNachricht.getIstSystemNachricht());
        assertEquals("Generierte Antwort", gespeicherteNachricht.getInhalt());
        assertEquals(100, gespeicherteNachricht.getInputToken());
        assertEquals(50, gespeicherteNachricht.getOutputToken());
        assertEquals("gpt-4", gespeicherteNachricht.getModell());
        assertEquals(BigDecimal.valueOf(0.01), gespeicherteNachricht.getKosten());
    }
    
    @Test
    void generiereAntwort_MitDokumentReferenzen_FuegtReferenzenHinzu() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(chatNachrichtRepository.findByChatOrderByZeitpunktAsc(testChat)).thenReturn(new ArrayList<>());
        when(loesungsversuchService.findeNeuesterLoesungsversuch(1L, 1L)).thenReturn(Optional.empty());
        
        AufgabeDto aufgabeDTO = new AufgabeDto();
        TeilaufgabeDto teilaufgabeDTO = new TeilaufgabeDto();
        when(aufgabeMapper.toDto(testAufgabe)).thenReturn(aufgabeDTO);
        when(teilaufgabeMapper.toDto(testTeilaufgabe)).thenReturn(teilaufgabeDTO);
        
        // LLM Response mit Dokumentreferenzen
        LlmChatResponseDto llmResponse = new LlmChatResponseDto();
        llmResponse.setContent("Antwort mit Referenzen");
        llmResponse.setModel("gpt-4");
        llmResponse.setInputTokens(100);
        llmResponse.setOutputTokens(50);
        llmResponse.setCost(BigDecimal.valueOf(0.01));
        
        LlmChatResponseDto.DocumentReference docRef = new LlmChatResponseDto.DocumentReference();
        docRef.setKursMaterialId(10L);
        docRef.setPageNumber(5);
        llmResponse.setDocumentReferences(Arrays.asList(docRef));
        
        when(llmChatService.generiereAntwort(any(LlmChatRequestDto.class))).thenReturn(llmResponse);
        
        // Kursmaterial Setup
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(10L);
        kursMaterial.setName("test.pdf");
        when(kursMaterialRepository.findById(10L)).thenReturn(Optional.of(kursMaterial));
        
        ChatNachricht systemNachricht = new ChatNachricht();
        systemNachricht.setId(2L);
        systemNachricht.setInhalt("Antwort mit Referenzen");
        systemNachricht.setIstSystemNachricht(true);
        systemNachricht.setReferenzen(new ArrayList<>());
        when(chatNachrichtRepository.save(any(ChatNachricht.class))).thenReturn(systemNachricht);
        
        ChatNachrichtDTO systemNachrichtDTO = new ChatNachrichtDTO();
        systemNachrichtDTO.setInhalt("Antwort mit Referenzen");
        when(chatNachrichtMapper.toDto(systemNachricht)).thenReturn(systemNachrichtDTO);
        
        // Act
        ChatNachrichtDTO result = chatService.generiereAntwort(1L, "Studentenfrage", false);
        
        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<ChatNachricht> captor = ArgumentCaptor.forClass(ChatNachricht.class);
        verify(chatNachrichtRepository).save(captor.capture());
        ChatNachricht gespeicherteNachricht = captor.getValue();
        assertEquals(1, gespeicherteNachricht.getReferenzen().size());
        
        ChatNachrichtReferenz referenz = gespeicherteNachricht.getReferenzen().iterator().next();
        assertEquals(kursMaterial, referenz.getKursMaterial());
        assertEquals(5, referenz.getSeitennummer());
    }
    
    @Test
    void generiereAntwort_MitLlmFehler_ErstelltFallbackAntwort() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(chatNachrichtRepository.findByChatOrderByZeitpunktAsc(testChat)).thenReturn(new ArrayList<>());
        when(loesungsversuchService.findeNeuesterLoesungsversuch(1L, 1L)).thenReturn(Optional.empty());
        
        AufgabeDto aufgabeDTO = new AufgabeDto();
        TeilaufgabeDto teilaufgabeDTO = new TeilaufgabeDto();
        when(aufgabeMapper.toDto(testAufgabe)).thenReturn(aufgabeDTO);
        when(teilaufgabeMapper.toDto(testTeilaufgabe)).thenReturn(teilaufgabeDTO);
        
        when(llmChatService.generiereAntwort(any(LlmChatRequestDto.class))).thenThrow(new RuntimeException("LLM Fehler"));
        
        ChatNachricht fallbackNachricht = new ChatNachricht();
        fallbackNachricht.setId(2L);
        fallbackNachricht.setInhalt("Entschuldigung, bei der Generierung einer Antwort ist ein Fehler aufgetreten. " +
                    "Bitte versuche es später noch einmal oder formuliere deine Frage anders.");
        fallbackNachricht.setIstSystemNachricht(true);
        when(chatNachrichtRepository.save(any(ChatNachricht.class))).thenReturn(fallbackNachricht);
        
        ChatNachrichtDTO fallbackDTO = new ChatNachrichtDTO();
        fallbackDTO.setInhalt(fallbackNachricht.getInhalt());
        when(chatNachrichtMapper.toDto(fallbackNachricht)).thenReturn(fallbackDTO);
        
        // Act
        ChatNachrichtDTO result = chatService.generiereAntwort(1L, "Studentenfrage", false);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getInhalt().contains("Entschuldigung"));
        
        ArgumentCaptor<ChatNachricht> captor = ArgumentCaptor.forClass(ChatNachricht.class);
        verify(chatNachrichtRepository).save(captor.capture());
        ChatNachricht gespeicherteNachricht = captor.getValue();
        assertTrue(gespeicherteNachricht.getIstSystemNachricht());
        assertTrue(gespeicherteNachricht.getInhalt().contains("Entschuldigung"));
    }
    
    @Test
    void generiereAntwort_MitChatHistory_VerwendetKontext() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        
        // Vorherige Nachrichten
        List<ChatNachricht> vorherigeNachrichten = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ChatNachricht nachricht = new ChatNachricht();
            nachricht.setInhalt("Nachricht " + i);
            nachricht.setIstSystemNachricht(i % 2 == 0);
            vorherigeNachrichten.add(nachricht);
        }
        when(chatNachrichtRepository.findByChatOrderByZeitpunktAsc(testChat)).thenReturn(vorherigeNachrichten);
        
        LoesungsVersuchDTO loesungsversuch = new LoesungsVersuchDTO();
        // Lösungsversuch mit Feldern
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("field1", "Meine Lösung");
        loesungsversuch.setLoesungFelder(loesungFelder);
        when(loesungsversuchService.findeNeuesterLoesungsversuch(1L, 1L)).thenReturn(Optional.of(loesungsversuch));
        
        AufgabeDto aufgabeDTO = new AufgabeDto();
        TeilaufgabeDto teilaufgabeDTO = new TeilaufgabeDto();
        when(aufgabeMapper.toDto(testAufgabe)).thenReturn(aufgabeDTO);
        when(teilaufgabeMapper.toDto(testTeilaufgabe)).thenReturn(teilaufgabeDTO);
        
        LlmChatResponseDto llmResponse = new LlmChatResponseDto();
        llmResponse.setContent("Antwort mit Kontext");
        llmResponse.setModel("gpt-4");
        llmResponse.setInputTokens(200);
        llmResponse.setOutputTokens(100);
        llmResponse.setCost(BigDecimal.valueOf(0.02));
        when(llmChatService.generiereAntwort(any(LlmChatRequestDto.class))).thenReturn(llmResponse);
        
        ChatNachricht systemNachricht = new ChatNachricht();
        systemNachricht.setInhalt("Antwort mit Kontext");
        when(chatNachrichtRepository.save(any(ChatNachricht.class))).thenReturn(systemNachricht);
        
        ChatNachrichtDTO systemNachrichtDTO = new ChatNachrichtDTO();
        systemNachrichtDTO.setInhalt("Antwort mit Kontext");
        when(chatNachrichtMapper.toDto(systemNachricht)).thenReturn(systemNachrichtDTO);
        
        // Act
        ChatNachrichtDTO result = chatService.generiereAntwort(1L, "Neue Frage", true);
        
        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<LlmChatRequestDto> requestCaptor = ArgumentCaptor.forClass(LlmChatRequestDto.class);
        verify(llmChatService).generiereAntwort(requestCaptor.capture());
        
        LlmChatRequestDto capturedRequest = requestCaptor.getValue();
        assertEquals("Neue Frage", capturedRequest.getUserMessage());
        assertEquals(5, capturedRequest.getChatHistory().size()); // Nur die letzten 5 Nachrichten
        assertTrue(capturedRequest.isExplanationMode());
        assertTrue(capturedRequest.getLoesungsversuch().isPresent());
        assertEquals("Meine Lösung", capturedRequest.getLoesungsversuch().get().getLoesungFelder().get("field1"));
    }
    
    @Test
    void getChat_MitGueltigemChat_GibtChatZurueck() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(chatMapper.toDto(testChat)).thenReturn(testChatDTO);
        
        // Act
        ChatDTO result = chatService.getChat(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(chatMapper).toDto(testChat);
    }
    
    @Test
    void getChat_MitUngueltigemChat_GibtNullZurueck() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act
        ChatDTO result = chatService.getChat(1L);
        
        // Assert
        assertNull(result);
        verify(chatMapper, never()).toDto(any(Chat.class));
    }
    
    @Test
    void hatChatZugriff_MitBerechtigtemStudent_GibtTrueZurueck() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        
        // Act
        boolean result = chatService.hatChatZugriff(1L, 1L);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void hatChatZugriff_MitUnberechtigtemStudent_GibtFalseZurueck() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        
        // Act
        boolean result = chatService.hatChatZugriff(1L, 2L); // Andere Student ID
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void hatChatZugriff_MitUngueltigemChat_GibtFalseZurueck() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act
        boolean result = chatService.hatChatZugriff(1L, 1L);
        
        // Assert
        assertFalse(result);
    }
}