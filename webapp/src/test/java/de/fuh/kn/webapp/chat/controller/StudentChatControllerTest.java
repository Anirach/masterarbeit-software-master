package de.fuh.kn.webapp.chat.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.LoesungsversuchService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.chat.dto.ChatDTO;
import de.fuh.kn.webapp.chat.dto.ChatNachrichtDTO;
import de.fuh.kn.webapp.chat.service.ChatService;
import de.fuh.kn.webapp.common.markdown.AufgabenMarkdownService;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.security.WithMockStudent;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-Tests für StudentChatController.
 * Testet die Chat-Funktionalität für Studenten.
 */
@ExtendWith(MockitoExtension.class)
class StudentChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;
    
    @Mock
    private NutzerService nutzerService;
    
    @Mock
    private AufgabeService aufgabeService;
    
    @Mock
    private TeilaufgabeService teilaufgabeService;
    
    @Mock
    private LoesungsversuchService loesungsversuchService;
    
    @Mock
    private AufgabenMarkdownService markdownService;
    
    @Mock
    private KurseinheitService kurseinheitService;

    @InjectMocks
    private StudentChatController controller;

    private NutzerDTO testNutzer;
    private StudentDTO testStudent;
    private AufgabeDto testAufgabe;
    private TeilaufgabeDto testTeilaufgabe;
    private ChatDTO testChat;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        // Test-Daten erstellen
        testNutzer = new NutzerDTO();
        testNutzer.setId(1L);
        testNutzer.setEmail("test@example.com");
        
        testStudent = new StudentDTO();
        testStudent.setId(1L);
        testStudent.setVorname("Test");
        testStudent.setNachname("Student");
        
        testAufgabe = new AufgabeDto();
        testAufgabe.setId(10L);
        testAufgabe.setTitel("Test Aufgabe");
        
        testTeilaufgabe = new TeilaufgabeDto();
        testTeilaufgabe.setId(20L);
        testTeilaufgabe.setAufgabeId(10L);
        testTeilaufgabe.setAufgabenstellungMarkdown("Test Aufgabenstellung");
        
        testChat = createTestChat();
    }

    @Test
    @DisplayName("zeigeChat - Normaler Chat ohne Erklärungsmodus")
    void testZeigeChat_NormalerModus() throws Exception {
        // Arrange
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudent));
        when(chatService.erstelleChatFuerTeilaufgabe(1L, 20L)).thenReturn(testChat);
        when(teilaufgabeService.getTeilaufgabeById(20L)).thenReturn(testTeilaufgabe);
        when(aufgabeService.getAufgabeById(10L)).thenReturn(testAufgabe);
        when(markdownService.renderMarkdownForPreview(anyString(), anyLong()))
            .thenReturn(new MarkdownRenderResult("<p>Rendered HTML</p>", null));
        
        // Act & Assert
        mockMvc.perform(get("/student/chat/20"))
            .andExpect(status().isOk())
            .andExpect(view().name("student/chat/chat-view"))
            .andExpect(model().attributeExists("aufgabe", "chat", "student", "teilaufgabe"))
            .andExpect(model().attribute("student", testStudent))
            .andExpect(model().attribute("aufgabe", testAufgabe))
            .andExpect(model().attribute("chat", testChat))
            .andExpect(model().attributeDoesNotExist("isExplanationMode"));
        
        // Verify
        verify(chatService).erstelleChatFuerTeilaufgabe(1L, 20L);
    }

    @Test
    @DisplayName("zeigeChat - Mit Erklärungsmodus")
    void testZeigeChat_ErklaerungsModus() throws Exception {
        // Arrange
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudent));
        when(chatService.erstelleChatFuerTeilaufgabe(1L, 20L)).thenReturn(testChat);
        when(teilaufgabeService.getTeilaufgabeById(20L)).thenReturn(testTeilaufgabe);
        when(aufgabeService.getAufgabeById(10L)).thenReturn(testAufgabe);
        when(markdownService.renderMarkdownForPreview(anyString(), anyLong()))
            .thenReturn(new MarkdownRenderResult("<p>Rendered HTML</p>", null));
        
        // Act & Assert
        mockMvc.perform(get("/student/chat/20")
                .param("isExplanation", "true"))
            .andExpect(status().isOk())
            .andExpect(view().name("student/chat/chat-view"))
            .andExpect(model().attribute("isExplanationMode", true))
            .andExpect(model().attribute("initialMessage", "Erkläre mir bitte diese Aufgabe."));
    }

    @Test
    @DisplayName("zeigeChat - Nutzer ist kein Student")
    void testZeigeChat_KeinStudent() throws Exception {
        // Arrange
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert - Exception wird geworfen
        assertThrows(ServletException.class, ()-> {

            mockMvc.perform(get("/student/chat/20"))
                    .andExpect(status().is5xxServerError());
        });
    }

    @Test
    @DisplayName("sendeNachricht - Normale Nachricht")
    void testSendeNachricht_Normal() throws Exception {
        // Arrange
        ChatNachrichtDTO studentenNachricht = createChatNachricht(1L, "Hallo", false);
        ChatNachrichtDTO systemAntwort = createChatNachricht(2L, "Antwort", true);
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudent));
        when(chatService.hatChatZugriff(100L, 1L)).thenReturn(true);
        when(chatService.sendeNachricht(100L, "Hallo")).thenReturn(studentenNachricht);
        when(chatService.generiereAntwort(100L, "Hallo", false)).thenReturn(systemAntwort);
        when(chatService.getChat(100L)).thenReturn(testChat);
        when(teilaufgabeService.getTeilaufgabeById(20L)).thenReturn(testTeilaufgabe);
        when(markdownService.renderMarkdownForPreview(anyString(), anyLong()))
            .thenReturn(new MarkdownRenderResult("<p>Rendered HTML</p>", null));
        
        // Act & Assert
        mockMvc.perform(post("/student/chat/htmx/sende-nachricht")
                .param("chatId", "100")
                .param("nachricht", "Hallo")
                .param("teilaufgabeId", "20"))
            .andExpect(status().isOk())
            .andExpect(view().name("student/chat/fragments/nachrichten-liste :: nachrichten"))
            .andExpect(model().attributeExists("chat", "teilaufgabe"));
        
        // Verify
        verify(chatService).sendeNachricht(100L, "Hallo");
        verify(chatService).generiereAntwort(100L, "Hallo", false);
    }

    @Test
    @DisplayName("sendeNachricht - Mit Erklärungsmodus")
    void testSendeNachricht_ErklaerungsModus() throws Exception {
        // Arrange
        ChatNachrichtDTO studentenNachricht = createChatNachricht(1L, "Erkläre mir die Aufgabe", false);
        ChatNachrichtDTO systemAntwort = createChatNachricht(2L, "Ausführliche Erklärung", true);
        
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudent));
        when(chatService.hatChatZugriff(100L, 1L)).thenReturn(true);
        when(chatService.sendeNachricht(100L, "Erkläre mir die Aufgabe")).thenReturn(studentenNachricht);
        when(chatService.generiereAntwort(eq(100L), contains("Bitte erkläre die Konzepte"), eq(true))).thenReturn(systemAntwort);
        when(chatService.getChat(100L)).thenReturn(testChat);
        when(teilaufgabeService.getTeilaufgabeById(20L)).thenReturn(testTeilaufgabe);
        when(markdownService.renderMarkdownForPreview(anyString(), anyLong()))
            .thenReturn(new MarkdownRenderResult("<p>Rendered HTML</p>", null));
        
        // Act & Assert
        mockMvc.perform(post("/student/chat/htmx/sende-nachricht")
                .param("chatId", "100")
                .param("nachricht", "Erkläre mir die Aufgabe")
                .param("isExplanationMode", "true")
                .param("teilaufgabeId", "20"))
            .andExpect(status().isOk())
            .andExpect(view().name("student/chat/fragments/nachrichten-liste :: nachrichten"));
        
        // Verify - Enhanced prompt sollte verwendet werden
        verify(chatService).generiereAntwort(eq(100L), contains("Bitte erkläre die Konzepte"), eq(true));
    }

    @Test
    @DisplayName("sendeNachricht - Kein Zugriff auf Chat")
    @WithMockStudent
    void testSendeNachricht_KeinZugriff() throws Exception {
        // Arrange
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(testStudent));
        when(chatService.hatChatZugriff(100L, 1L)).thenReturn(false);
        
        // Act & Assert
        assertThrows(ServletException.class, ()->{
            mockMvc.perform(post("/student/chat/htmx/sende-nachricht")
                            .param("chatId", "100")
                            .param("nachricht", "Hallo")
                            .param("teilaufgabeId", "20"))
                    .andExpect(status().is5xxServerError()); // IllegalArgumentException wird geworfen
        });

        // Verify
        verify(chatService, never()).sendeNachricht(anyLong(), anyString());
    }

    @Test
    @DisplayName("sendeNachricht - Nutzer ist kein Student")
    void testSendeNachricht_KeinStudent() throws Exception {
        // Arrange
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testNutzer);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ServletException.class, ()-> {

            mockMvc.perform(post("/student/chat/htmx/sende-nachricht")
                            .param("chatId", "100")
                            .param("nachricht", "Hallo")
                            .param("teilaufgabeId", "20"))
                    .andExpect(status().is5xxServerError()); // IllegalStateException wird geworfen
        });
    }

    // Helper-Methoden

    private ChatDTO createTestChat() {
        ChatDTO chat = new ChatDTO();
        chat.setId(100L);
        chat.setStudentId(1L);
        chat.setTeilaufgabeId(20L);
        chat.setZeitpunkt(LocalDateTime.now());
        
        ChatNachrichtDTO systemNachricht = createChatNachricht(1L, "Willkommen im Chat!", true);
        chat.setNachrichten(Arrays.asList(systemNachricht));
        
        return chat;
    }

    private ChatNachrichtDTO createChatNachricht(Long id, String inhalt, boolean istSystem) {
        ChatNachrichtDTO nachricht = new ChatNachrichtDTO();
        nachricht.setId(id);
        nachricht.setInhalt(inhalt);
        nachricht.setIstSystemNachricht(istSystem);
        nachricht.setZeitpunkt(LocalDateTime.now());
        return nachricht;
    }
}