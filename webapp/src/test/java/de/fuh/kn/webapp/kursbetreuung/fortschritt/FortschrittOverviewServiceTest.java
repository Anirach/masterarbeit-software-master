package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import de.fuh.kn.webapp.chat.dto.ChatNachrichtReferenzMapper;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderer;
import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldNodeRenderer;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentMapper;
import de.fuh.kn.webapp.persistence.entity.*;
import de.fuh.kn.webapp.persistence.repository.*;
import de.fuh.kn.webapp.uebung.dto.KursFortschrittDTO;
import de.fuh.kn.webapp.uebung.service.FortschrittService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FortschrittOverviewServiceTest {

    @Mock
    private BelegungRepository belegungRepository;
    
    @Mock
    private KursRepository kursRepository;
    
    @Mock
    private StudentRepository studentRepository;
    
    @Mock
    private TeilaufgabeRepository teilaufgabeRepository;
    
    @Mock
    private FortschrittService fortschrittService;
    
    @Mock
    private ChatNachrichtRepository chatNachrichtRepository;
    
    @Mock
    private ChatRepository chatRepository;
    
    @Mock
    private LoesungsVersuchRepository loesungsVersuchRepository;
    
    @Mock
    private StudentMapper studentMapper;
    
    @Mock
    private KursMapper kursMapper;
    
    @Mock
    private MarkdownRenderer markdownRenderer;
    
    @Mock
    private ChatNachrichtReferenzMapper chatNachrichtReferenzMapper;
    
    @Mock
    private ChatNachrichtReferenzRepository chatNachrichtReferenzRepository;
    
    @InjectMocks
    private FortschrittOverviewService fortschrittOverviewService;
    
    private Kurs testKurs;
    private KursDTO testKursDTO;
    private Student testStudent;
    private StudentDTO testStudentDTO;
    private Belegung testBelegung;
    private Kurseinheit testKurseinheit;
    private Aufgabe testAufgabe;
    private Teilaufgabe testTeilaufgabe;
    private Chat testChat;
    private ChatNachricht testChatNachricht;
    private LoesungsVersuch testLoesungsVersuch;
    private KursFortschrittDTO testKursFortschritt;
    
    @BeforeEach
    void setUp() {
        // Test-Entitäten erstellen
        testKurs = new Kurs();
        testKurs.setId(1L);
        testKurs.setName("Testkurs");
        
        testKursDTO = new KursDTO();
        testKursDTO.setId(1L);
        testKursDTO.setName("Testkurs");
        
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setVorname("Max");
        testStudent.setNachname("Mustermann");
        testStudent.setEmail("max.mustermann@test.de");
        
        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setVorname("Max");
        testStudentDTO.setNachname("Mustermann");
        testStudentDTO.setEmail("max.mustermann@test.de");
        
        testBelegung = new Belegung();
        testBelegung.setId(1L);
        testBelegung.setStudent(testStudent);
        testBelegung.setKurs(testKurs);
        testBelegung.setStartDatum(LocalDate.now().minusDays(30));
        testBelegung.setEndDatum(null); // Aktive Belegung
        
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
        testTeilaufgabe.setReihenfolge(1);
        
        testChat = new Chat();
        testChat.setId(1L);
        testChat.setStudent(testStudent);
        testChat.setTeilaufgabe(testTeilaufgabe);
        testChat.setZeitpunkt(LocalDateTime.now());
        
        testChatNachricht = new ChatNachricht();
        testChatNachricht.setId(1L);
        testChatNachricht.setChat(testChat);
        testChatNachricht.setInhalt("Test Nachricht");
        testChatNachricht.setZeitpunkt(LocalDateTime.now());
        testChatNachricht.setIstSystemNachricht(true); // Change to system message so markdown renderer is called
        testChatNachricht.setKosten(BigDecimal.valueOf(0.01));
        
        testLoesungsVersuch = new LoesungsVersuch();
        testLoesungsVersuch.setId(1L);
        testLoesungsVersuch.setStudent(testStudent);
        testLoesungsVersuch.setTeilaufgabe(testTeilaufgabe);
        testLoesungsVersuch.setZeitpunkt(LocalDateTime.now().minusHours(1));
        testLoesungsVersuch.setIstAbgeschlossen(true);
        testLoesungsVersuch.setBewertungPunkte(Integer.valueOf(80));
        testLoesungsVersuch.setKosten(BigDecimal.valueOf(0.02));
        
        testKursFortschritt = new KursFortschrittDTO();
        testKursFortschritt.setGesamtTeilaufgaben(10L);
        testKursFortschritt.setAbgeschlosseneTeilaufgaben(8L);
        testKursFortschritt.setFortschrittProzent(80);
    }
    
    @Test
    void getFortschrittOverviewForKurs_OhneFilter_GibtAlleBelegungenZurueck() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Belegung> belegungen = Arrays.asList(testBelegung);
        Page<Belegung> belegungPage = new PageImpl<>(belegungen, pageable, 1);
        
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByKurs(testKurs, pageable)).thenReturn(belegungPage);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        when(fortschrittService.berechneFortschritt(testStudentDTO, testKursDTO, true)).thenReturn(testKursFortschritt);
        when(chatNachrichtRepository.countByStudentAndKurs(testStudent, testKurs)).thenReturn(5L);
        when(loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(testStudent, testKurs))
                .thenReturn(Arrays.asList(testLoesungsVersuch));
        when(chatNachrichtRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.05));
        when(loesungsVersuchRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.10));
        
        // Act
        Page<FortschrittOverviewDTO> result = fortschrittOverviewService.getFortschrittOverviewForKurs(
                testKursDTO, "", false, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        
        FortschrittOverviewDTO overview = result.getContent().get(0);
        assertEquals(1L, overview.getBelegungId());
        assertEquals(1L, overview.getStudentId());
        assertEquals("Max Mustermann", overview.getStudentName());
        assertEquals("max.mustermann@test.de", overview.getStudentEmail());
        assertTrue(overview.isAktiv());
        assertEquals(10, overview.getGesamtAufgaben());
        assertEquals(8, overview.getAbgeschlosseneAufgaben());
        assertEquals(80.0, overview.getFortschrittProzent());
        assertEquals(5L, overview.getAnzahlNachrichten());
        assertEquals(BigDecimal.valueOf(0.15), overview.getAiKostenGesamt());
        assertEquals(testLoesungsVersuch.getZeitpunkt(), overview.getLetzteAktivitaet());
    }
    
    @Test
    void getFortschrittOverviewForKurs_MitAktivFilter_GibtNurAktiveBelegungenZurueck() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Belegung> belegungen = Arrays.asList(testBelegung);
        Page<Belegung> belegungPage = new PageImpl<>(belegungen, pageable, 1);
        
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(belegungPage);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        when(fortschrittService.berechneFortschritt(testStudentDTO, testKursDTO, true)).thenReturn(testKursFortschritt);
        when(chatNachrichtRepository.countByStudentAndKurs(testStudent, testKurs)).thenReturn(5L);
        when(loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(testStudent, testKurs))
                .thenReturn(Arrays.asList(testLoesungsVersuch));
        when(chatNachrichtRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.05));
        when(loesungsVersuchRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.10));
        
        // Act
        Page<FortschrittOverviewDTO> result = fortschrittOverviewService.getFortschrittOverviewForKurs(
                testKursDTO, "", true, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(belegungRepository).findAll(any(Specification.class), eq(pageable));
    }
    
    @Test
    void getFortschrittOverviewForKurs_MitSuche_FiltriertNachStudentenname() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Belegung> belegungen = Arrays.asList(testBelegung);
        Page<Belegung> belegungPage = new PageImpl<>(belegungen, pageable, 1);
        
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByKursAndStudentNameContaining(testKurs, "Max", pageable))
                .thenReturn(belegungPage);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        when(fortschrittService.berechneFortschritt(testStudentDTO, testKursDTO, true)).thenReturn(testKursFortschritt);
        when(chatNachrichtRepository.countByStudentAndKurs(testStudent, testKurs)).thenReturn(5L);
        when(loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(testStudent, testKurs))
                .thenReturn(Arrays.asList(testLoesungsVersuch));
        when(chatNachrichtRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.05));
        when(loesungsVersuchRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.10));
        
        // Act
        Page<FortschrittOverviewDTO> result = fortschrittOverviewService.getFortschrittOverviewForKurs(
                testKursDTO, "Max", false, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(belegungRepository).findByKursAndStudentNameContaining(testKurs, "Max", pageable);
    }
    
    @Test
    void getFortschrittOverviewForKurs_OhneLoesungsversuche_VerwendetBelegungsdatum() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Belegung> belegungen = Arrays.asList(testBelegung);
        Page<Belegung> belegungPage = new PageImpl<>(belegungen, pageable, 1);
        
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByKurs(testKurs, pageable)).thenReturn(belegungPage);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        when(fortschrittService.berechneFortschritt(testStudentDTO, testKursDTO, true)).thenReturn(testKursFortschritt);
        when(chatNachrichtRepository.countByStudentAndKurs(testStudent, testKurs)).thenReturn(0L);
        when(loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(testStudent, testKurs))
                .thenReturn(new ArrayList<>()); // Keine Lösungsversuche
        when(chatNachrichtRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(null);
        when(loesungsVersuchRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(null);
        
        // Act
        Page<FortschrittOverviewDTO> result = fortschrittOverviewService.getFortschrittOverviewForKurs(
                testKursDTO, "", false, pageable);
        
        // Assert
        assertNotNull(result);
        FortschrittOverviewDTO overview = result.getContent().get(0);
        assertEquals(testBelegung.getStartDatum().atStartOfDay(), overview.getLetzteAktivitaet());
        assertEquals(BigDecimal.ZERO, overview.getAiKostenGesamt());
    }
    
    @Test
    void getStudentDetail_MitGueltigemStudentUndKurs_GibtDetailzurueck() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.of(testBelegung));
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        when(kursMapper.toDto(testKurs)).thenReturn(testKursDTO);
        when(fortschrittService.berechneFortschritt(testStudentDTO, testKursDTO, true)).thenReturn(testKursFortschritt);
        when(chatNachrichtRepository.countByStudentAndKurs(testStudent, testKurs)).thenReturn(5L);
        when(loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(testStudent, testKurs))
                .thenReturn(Arrays.asList(testLoesungsVersuch));
        when(chatNachrichtRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.05));
        when(loesungsVersuchRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.10));
        when(teilaufgabeRepository.findByKursOrderByHierarchy(testKurs)).thenReturn(Arrays.asList(testTeilaufgabe));
        when(loesungsVersuchRepository.findByStudentAndTeilaufgabeOrderByZeitpunktDesc(testStudent, testTeilaufgabe))
                .thenReturn(Arrays.asList(testLoesungsVersuch));
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(testStudent, testTeilaufgabe))
                .thenReturn(true);
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstUebersprungenTrue(testStudent, testTeilaufgabe))
                .thenReturn(false);
        when(chatRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Arrays.asList(testChat));
        when(chatNachrichtRepository.findByChatOrderByZeitpunktAsc(testChat))
                .thenReturn(Arrays.asList(testChatNachricht));
        when(chatNachrichtReferenzRepository.findByChatNachricht(testChatNachricht)).thenReturn(new ArrayList<>());
        
        MarkdownRenderResult renderResult = mock(MarkdownRenderResult.class);
        when(renderResult.getHtml()).thenReturn("<p>Test Nachricht</p>");
        when(markdownRenderer.renderMarkdown(anyString(), any(InputFieldNodeRenderer.RenderMode.class), any()))
                .thenReturn(renderResult);
        
        // Act
        StudentDetailDTO result = fortschrittOverviewService.getStudentDetail(1L, 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getStudentId());
        assertEquals("Max Mustermann", result.getStudentName());
        assertEquals("max.mustermann@test.de", result.getStudentEmail());
        assertEquals(1L, result.getKursId());
        assertEquals("Testkurs", result.getKursName());
        assertEquals(10, result.getGesamtTeilaufgaben());
        assertEquals(8, result.getAbgeschlosseneTeilaufgaben());
        assertEquals(80.0, result.getFortschrittProzent());
        assertEquals(5L, result.getAnzahlNachrichten());
        assertEquals(BigDecimal.valueOf(0.15), result.getAiKostenGesamt());
        
        // Teilaufgaben prüfen
        assertNotNull(result.getTeilaufgaben());
        assertEquals(1, result.getTeilaufgaben().size());
        TeilaufgabeDetailDTO teilaufgabeDetail = result.getTeilaufgaben().get(0);
        assertEquals(1L, teilaufgabeDetail.getTeilaufgabeId());
        assertEquals("Testaufgabe", teilaufgabeDetail.getAufgabeTitel());
        assertEquals("Testeinheit", teilaufgabeDetail.getKurseinheitName());
        assertTrue(teilaufgabeDetail.isIstAbgeschlossen());
        assertFalse(teilaufgabeDetail.isIstUebersprungen());
        assertEquals(1, teilaufgabeDetail.getAnzahlVersuche());
        
        // Chats prüfen
        assertNotNull(result.getChats());
        assertEquals(1, result.getChats().size());
        ChatUebersichtDTO chatDetail = result.getChats().get(0);
        assertEquals(1L, chatDetail.getTeilaufgabeId());
        assertEquals(1L, chatDetail.getChatId());
        assertEquals(1, chatDetail.getAnzahlNachrichten());
        assertEquals(BigDecimal.valueOf(0.01), chatDetail.getGesamtKosten());
    }
    
    @Test
    void getStudentDetail_MitUngueltigemStudent_WirftException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            fortschrittOverviewService.getStudentDetail(1L, 1L)
        );
    }
    
    @Test
    void getStudentDetail_MitUngueltigemKurs_WirftException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            fortschrittOverviewService.getStudentDetail(1L, 1L)
        );
    }
    
    @Test
    void getStudentDetail_OhneBelegung_WirftException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            fortschrittOverviewService.getStudentDetail(1L, 1L)
        );
    }
    
    @Test
    void getFortschrittOverviewForKurs_MitInaktiverBelegung_ZeigtInaktivAn() {
        // Arrange
        testBelegung.setEndDatum(LocalDate.now().minusDays(1)); // Beendet gestern
        
        Pageable pageable = PageRequest.of(0, 10);
        List<Belegung> belegungen = Arrays.asList(testBelegung);
        Page<Belegung> belegungPage = new PageImpl<>(belegungen, pageable, 1);
        
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByKurs(testKurs, pageable)).thenReturn(belegungPage);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        when(fortschrittService.berechneFortschritt(testStudentDTO, testKursDTO, false)).thenReturn(testKursFortschritt);
        when(chatNachrichtRepository.countByStudentAndKurs(testStudent, testKurs)).thenReturn(5L);
        when(loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(testStudent, testKurs))
                .thenReturn(Arrays.asList(testLoesungsVersuch));
        when(chatNachrichtRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.05));
        when(loesungsVersuchRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(BigDecimal.valueOf(0.10));
        
        // Act
        Page<FortschrittOverviewDTO> result = fortschrittOverviewService.getFortschrittOverviewForKurs(
                testKursDTO, "", false, pageable);
        
        // Assert
        assertNotNull(result);
        FortschrittOverviewDTO overview = result.getContent().get(0);
        assertFalse(overview.isAktiv());
        verify(fortschrittService).berechneFortschritt(testStudentDTO, testKursDTO, false);
    }
    
    @Test
    void getStudentDetail_MitChatOhneNachrichten_HandhabtLeerenChat() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(belegungRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Optional.of(testBelegung));
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        when(kursMapper.toDto(testKurs)).thenReturn(testKursDTO);
        when(fortschrittService.berechneFortschritt(testStudentDTO, testKursDTO, true)).thenReturn(testKursFortschritt);
        when(chatNachrichtRepository.countByStudentAndKurs(testStudent, testKurs)).thenReturn(0L);
        when(loesungsVersuchRepository.findByStudentAndKursOrderByZeitpunktDesc(testStudent, testKurs))
                .thenReturn(new ArrayList<>());
        when(chatNachrichtRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(null);
        when(loesungsVersuchRepository.sumKostenByStudentAndKurs(testStudent, testKurs)).thenReturn(null);
        when(teilaufgabeRepository.findByKursOrderByHierarchy(testKurs)).thenReturn(Arrays.asList(testTeilaufgabe));
        when(loesungsVersuchRepository.findByStudentAndTeilaufgabeOrderByZeitpunktDesc(testStudent, testTeilaufgabe))
                .thenReturn(new ArrayList<>());
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(testStudent, testTeilaufgabe))
                .thenReturn(false);
        when(loesungsVersuchRepository.existsByStudentAndTeilaufgabeAndIstUebersprungenTrue(testStudent, testTeilaufgabe))
                .thenReturn(false);
        when(chatRepository.findByStudentAndKurs(testStudent, testKurs)).thenReturn(Arrays.asList(testChat));
        when(chatNachrichtRepository.findByChatOrderByZeitpunktAsc(testChat))
                .thenReturn(new ArrayList<>()); // Keine Nachrichten
        
        // Act
        StudentDetailDTO result = fortschrittOverviewService.getStudentDetail(1L, 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getChats().size());
        ChatUebersichtDTO chatDetail = result.getChats().get(0);
        assertEquals(0, chatDetail.getAnzahlNachrichten());
        assertNull(chatDetail.getLetzteNachrichtZeitpunkt());
        assertEquals(BigDecimal.ZERO, chatDetail.getGesamtKosten());
    }
}