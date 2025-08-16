package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.*;
import de.fuh.kn.webapp.common.markdown.AufgabenMarkdownService;
import de.fuh.kn.webapp.common.markdown.flexmark.InputFieldDto;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import de.fuh.kn.webapp.persistence.repository.AufgabeRepository;
import de.fuh.kn.webapp.persistence.repository.LoesungsVersuchRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import de.fuh.kn.webapp.persistence.repository.TeilaufgabeRepository;
import de.fuh.kn.webapp.security.WithMockStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentAufgabeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AufgabeService aufgabeService;

    @Mock
    private TeilaufgabeService teilaufgabeService;

    @Mock
    private NutzerService nutzerService;

    @Mock
    private AufgabenMarkdownService markdownService;

    @Mock
    private LoesungsVersuchRepository loesungsVersuchRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeilaufgabeRepository teilaufgabeRepository;

    @Mock
    private AufgabeRepository aufgabeRepository;

    @Mock
    private AufgabeZugangsService aufgabeZugangsService;
    
    @Spy
    private SpringTemplateEngine templateEngine = new SpringTemplateEngine();

    @Mock
    private LoesungsversuchService loesungsversuchService;

    @Mock
    private StudentAufgabeService studentAufgabeService;

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private KursService kursService;

    @InjectMocks
    private StudentAufgabeController controller;

    private Student mockStudent;
    private StudentDTO mockStudentDTO;
    private Teilaufgabe mockTeilaufgabe;
    private Aufgabe mockAufgabe;
    private Kurseinheit mockKurseinheit;
    private TeilaufgabeDto mockTeilaufgabeDto;
    private AufgabeDto mockAufgabeDto;
    private RedirectAttributes redirectAttributes;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        // Mock-Objekte initialisieren
        mockStudent = new Student();
        mockStudent.setId(1L);
        
        mockStudentDTO = new StudentDTO();
        mockStudentDTO.setId(1L);
        
        mockKurseinheit = new Kurseinheit();
        mockKurseinheit.setId(10L);
        
        mockAufgabe = new Aufgabe();
        mockAufgabe.setId(100L);
        mockAufgabe.setKurseinheit(mockKurseinheit);
        mockAufgabe.setReihenfolge(2);
        
        mockTeilaufgabe = new Teilaufgabe();
        mockTeilaufgabe.setId(1000L);
        mockTeilaufgabe.setAufgabe(mockAufgabe);
        mockTeilaufgabe.setReihenfolge(1);
        
        mockTeilaufgabeDto = new TeilaufgabeDto();
        mockTeilaufgabeDto.setId(1000L);
        mockTeilaufgabeDto.setReihenfolge(1);
        mockTeilaufgabeDto.setMusterloesungFelder(new HashMap<>());
        mockTeilaufgabeDto.setAufgabenstellungMarkdown("aufgabenstellung");
        
        mockAufgabeDto = new AufgabeDto();
        mockAufgabeDto.setId(100L);
        mockAufgabeDto.setKurseinheitId(10L);
        List<TeilaufgabeDto> teilaufgaben = new ArrayList<>();
        teilaufgaben.add(mockTeilaufgabeDto);
        mockAufgabeDto.setTeilaufgaben(teilaufgaben);
        
        redirectAttributes = new RedirectAttributesModelMap();
    }

    @Test
    @WithMockStudent
    void ueberspringe_erstelltUebersprungenLoesungsversuch() {
        // Arrange - Setup mocks
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(mockStudentDTO);
        when(nutzerService.getStudentById(anyLong())).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeById(anyLong())).thenReturn(mockAufgabeDto);
        when(aufgabeService.getAufgabeByTeilaufgabeId(anyLong())).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.findeNaechsteTeilaufgabe(any(), anyLong(), anyLong())).thenReturn(Optional.empty());
        when(teilaufgabeService.getTeilaufgabeById(anyLong())).thenReturn(mockTeilaufgabeDto);

        // Mock LoesungsversuchDTO
        LoesungsVersuchDTO mockLoesungsVersuchDTO = new LoesungsVersuchDTO();
        mockLoesungsVersuchDTO.setId(2000L);
        mockLoesungsVersuchDTO.setStudentId(1L);
        mockLoesungsVersuchDTO.setTeilaufgabeId(1000L);
        mockLoesungsVersuchDTO.setIstUebersprungen(true);

        when(loesungsversuchService.erstelleUebersprungenenLoesungsversuch(1L, 1000L))
                .thenReturn(mockLoesungsVersuchDTO);

        // Mock KurseinheitDTO for redirect
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L); // The correct course ID
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);

        // Act
        String result = controller.ueberspringe(1000L, redirectAttributes, new org.springframework.ui.ConcurrentModel());

        // Assert
        // Verify service method was called
        verify(loesungsversuchService).erstelleUebersprungenenLoesungsversuch(1L, 1000L);

        // Verify redirect message
        String warningMessage = (String) redirectAttributes.getFlashAttributes().get("warningMessage");
        assertNotNull(warningMessage);
        assertTrue(warningMessage.contains("übersprungen"));

        // Verify redirect to kurs
        assertEquals("redirect:/student/kurs/8", result);
    }

    @Test
    @WithMockStudent
    void ueberspringe_redirectsToNextTeilaufgabe() {
        // Arrange - Setup mocks
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(mockStudentDTO);
        when(nutzerService.getStudentById(anyLong())).thenReturn(Optional.of(mockStudentDTO));

        // Create a second teilaufgabe to be the "next" one
        TeilaufgabeDto nextTeilaufgabeDto = new TeilaufgabeDto();
        nextTeilaufgabeDto.setId(1001L);
        nextTeilaufgabeDto.setReihenfolge(2);

        // Mock LoesungsversuchDTO
        LoesungsVersuchDTO mockLoesungsVersuchDTO = new LoesungsVersuchDTO();
        mockLoesungsVersuchDTO.setId(2000L);
        mockLoesungsVersuchDTO.setStudentId(1L);
        mockLoesungsVersuchDTO.setTeilaufgabeId(1000L);
        mockLoesungsVersuchDTO.setIstUebersprungen(true);

        when(loesungsversuchService.erstelleUebersprungenenLoesungsversuch(1L, 1000L))
                .thenReturn(mockLoesungsVersuchDTO);

        when(aufgabeService.getAufgabeByTeilaufgabeId(anyLong())).thenReturn(mockAufgabeDto);

        // Set up the mock to return the next teilaufgabe
        when(studentAufgabeService.findeNaechsteTeilaufgabe(any(), anyLong(), anyLong()))
                .thenReturn(Optional.of(nextTeilaufgabeDto));
        when(teilaufgabeService.getTeilaufgabeById(anyLong())).thenReturn(mockTeilaufgabeDto);

        // Act
        String result = controller.ueberspringe(1000L, redirectAttributes, new org.springframework.ui.ConcurrentModel());

        // Assert
        // Verify service methods were called
        verify(loesungsversuchService).erstelleUebersprungenenLoesungsversuch(1L, 1000L);
        verify(studentAufgabeService).findeNaechsteTeilaufgabe(any(), anyLong(), anyLong());

        // Verify redirect to next teilaufgabe
        assertEquals("redirect:/student/aufgaben/100?teilaufgabe=1001", result);
    }

    @Test
    void ueberspringenEndpoint_redirectsWithCorrectParams() throws Exception {
        // Mock controller behavior
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(mockStudentDTO);
        when(nutzerService.getStudentById(anyLong())).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeByTeilaufgabeId(anyLong())).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.findeNaechsteTeilaufgabe(any(), anyLong(), anyLong())).thenReturn(Optional.empty());
        when(teilaufgabeService.getTeilaufgabeById(anyLong())).thenReturn(mockTeilaufgabeDto);

        // Mock LoesungsversuchDTO
        LoesungsVersuchDTO mockLoesungsVersuchDTO = new LoesungsVersuchDTO();
        mockLoesungsVersuchDTO.setId(2000L);
        mockLoesungsVersuchDTO.setStudentId(1L);
        mockLoesungsVersuchDTO.setTeilaufgabeId(1000L);
        mockLoesungsVersuchDTO.setIstUebersprungen(true);

        when(loesungsversuchService.erstelleUebersprungenenLoesungsversuch(anyLong(), anyLong()))
                .thenReturn(mockLoesungsVersuchDTO);

        // Mock KurseinheitDTO for redirect
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L); // The correct course ID
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);

        // MVC Test
        mockMvc.perform(
                post("/student/aufgaben/ueberspringen")
                        .param("teilaufgabeId", "1000")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(request -> {
                            request.setAttribute("org.springframework.security.web.csrf.CsrfToken", null);
                            return request;
                        })
        )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/kurs/8"));
    }

    @Test
    @DisplayName("showAufgabe sollte Aufgabe mit allen Details anzeigen")
    void showAufgabe_ShouldDisplayAufgabeWithAllDetails() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeById(100L)).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.hatZugangZuAufgabe(100L, 1L)).thenReturn(true);
        when(studentAufgabeService.ermittleAktiveTeilaufgabe(mockAufgabeDto, null, 1L)).thenReturn(mockTeilaufgabeDto);
        
        // Mock Markdown rendering
        MarkdownRenderResult markdownResult = new MarkdownRenderResult("<p>Test</p>", new ArrayList<>());
        when(markdownService.renderMarkdownForStudentInput(anyString(), anyLong())).thenReturn(markdownResult);
        
        // Mock KurseinheitDTO
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L);
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);
        
        // Mock KursDTO
        KursDTO mockKurs = new KursDTO();
        mockKurs.setId(8L);
        mockKurs.setName("Test Kurs");
        when(kursService.getKursById(8L)).thenReturn(mockKurs);
        
        // Act
        String viewName = controller.showAufgabe(100L, null, model, redirectAttributes);
        
        // Assert
        assertEquals("student/aufgabe/aufgabe-bearbeiten", viewName);
        assertNotNull(model.getAttribute("aufgabe"));
        assertNotNull(model.getAttribute("aktiveTeilaufgabe"));
        assertNotNull(model.getAttribute("student"));
        assertEquals(8L, model.getAttribute("kursId"));
        verify(aufgabeService).getAufgabeById(100L);
        verify(studentAufgabeService).hatZugangZuAufgabe(100L, 1L);
    }

    @Test
    @DisplayName("showAufgabe sollte redirect wenn kein Zugang zur Aufgabe")
    void showAufgabe_ShouldRedirectWhenNoAccess() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        // Mock KurseinheitDTO
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L); // The correct course ID

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeById(100L)).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.hatZugangZuAufgabe(100L, 1L)).thenReturn(false);
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);
        
        // Act
        String viewName = controller.showAufgabe(100L, null, model, redirectAttributes);
        
        // Assert
        assertEquals("redirect:/student/kurs/8", viewName);
        assertNotNull(redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    @DisplayName("showAufgabe sollte Musterlösung anzeigen wenn berechtigt")
    void showAufgabe_ShouldShowSolutionWhenAuthorized() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        // Setup Teilaufgabe mit Musterlösung
        mockTeilaufgabeDto.setAufgabenstellungMarkdown("Test Aufgabe");
        mockTeilaufgabeDto.getMusterloesungFelder().put("field1", "Lösung 1");

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeById(100L)).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.hatZugangZuAufgabe(100L, 1L)).thenReturn(true);
        when(studentAufgabeService.ermittleAktiveTeilaufgabe(mockAufgabeDto, 1000L, 1L)).thenReturn(mockTeilaufgabeDto);
        when(loesungsversuchService.hatMindestens50ProzentErreicht(1L, 1000L)).thenReturn(true);
        
        // Mock Markdown rendering
        MarkdownRenderResult markdownResult = new MarkdownRenderResult("<p>Test</p>", new ArrayList<>());
        when(markdownService.renderMarkdownForStudentInput(anyString(), anyLong())).thenReturn(markdownResult);
        when(markdownService.renderMarkdownForSolution(anyString(), anyLong())).thenReturn(markdownResult);
        
        // Mock KurseinheitDTO
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L);
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);
        
        // Mock KursDTO
        KursDTO mockKurs = new KursDTO();
        mockKurs.setId(8L);
        mockKurs.setName("Test Kurs");
        when(kursService.getKursById(8L)).thenReturn(mockKurs);
        
        // Act
        String viewName = controller.showAufgabe(100L, 1000L, model, redirectAttributes);
        
        // Assert
        assertEquals("student/aufgabe/aufgabe-bearbeiten", viewName);
        assertTrue((Boolean) model.getAttribute("kannMusterloesungSehen"));
        assertNotNull(model.getAttribute("teilaufgabeMusterloesungMarkdown"));
        assertEquals(8L, model.getAttribute("kursId"));
    }

    @Test
    @DisplayName("showAufgabe sollte mit letztem Lösungsversuch umgehen")
    void showAufgabe_ShouldHandleLastAttempt() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        // Setup Lösungsversuch
        LoesungsVersuchDTO letzterVersuch = new LoesungsVersuchDTO();
        letzterVersuch.getLoesungFelder().put("field1", "Meine Antwort");
        letzterVersuch.getBewertungFelderFarbe().put("field1", "green");

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeById(100L)).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.hatZugangZuAufgabe(100L, 1L)).thenReturn(true);
        when(studentAufgabeService.ermittleAktiveTeilaufgabe(mockAufgabeDto, null, 1L)).thenReturn(mockTeilaufgabeDto);
        when(loesungsversuchService.findeNeuesterLoesungsversuch(1L, 1000L)).thenReturn(Optional.of(letzterVersuch));
        
        // Mock Markdown rendering mit Feldern
        InputFieldDto field = InputFieldDto.builder()
                .fieldName("field1")
                .fieldType("text")
                .fieldSize(10)
                .isMultiline(false)
                .fieldIndex(0)
                .html("<input>")
                .build();
        List<InputFieldDto> fields = new ArrayList<>();
        fields.add(field);
        MarkdownRenderResult markdownResult = new MarkdownRenderResult("<p>Test {{field-field1}}</p>", fields);
        when(markdownService.renderMarkdownForStudentInput(anyString(), anyLong())).thenReturn(markdownResult);
        
        // Mock KurseinheitDTO
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L);
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);
        
        // Mock KursDTO
        KursDTO mockKurs = new KursDTO();
        mockKurs.setId(8L);
        mockKurs.setName("Test Kurs");
        when(kursService.getKursById(8L)).thenReturn(mockKurs);
        
        // Act
        String viewName = controller.showAufgabe(100L, null, model, redirectAttributes);
        
        // Assert
        assertEquals("student/aufgabe/aufgabe-bearbeiten", viewName);
        assertNotNull(model.getAttribute("letzterVersuch"));
        assertEquals(letzterVersuch, model.getAttribute("letzterVersuch"));
        assertEquals(8L, model.getAttribute("kursId"));
    }

    @Test
    @DisplayName("markiereLoesungsversuchAbgeschlossen sollte Versuch abschließen und weiterleiten")
    void markiereLoesungsversuchAbgeschlossen_ShouldCompleteAndRedirect() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        LoesungsVersuchDTO abgeschlossenerVersuch = new LoesungsVersuchDTO();
        abgeschlossenerVersuch.setId(2000L);
        abgeschlossenerVersuch.setTeilaufgabeId(1000L);
        abgeschlossenerVersuch.setBewertungPunkte(85);

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(loesungsversuchService.markiereLoesungsversuchAbgeschlossen(2000L, 1L)).thenReturn(abgeschlossenerVersuch);
        when(teilaufgabeService.getTeilaufgabeById(1000L)).thenReturn(mockTeilaufgabeDto);
        when(aufgabeService.getAufgabeByTeilaufgabeId(1000L)).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.findeNaechsteTeilaufgabe(any(), eq(1000L), eq(1L))).thenReturn(Optional.empty());

        // Mock KurseinheitDTO for redirect
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L); // The correct course ID
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);

        // Act
        String result = controller.markiereLoesungsversuchAbgeschlossen(2000L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/student/kurs/8", result);
        String successMessage = (String) redirectAttributes.getFlashAttributes().get("successMessage");
        assertNotNull(successMessage);
        verify(loesungsversuchService).markiereLoesungsversuchAbgeschlossen(2000L, 1L);
    }

    @Test
    @DisplayName("markiereLoesungsversuchAbgeschlossen sollte zur nächsten Teilaufgabe weiterleiten")
    void markiereLoesungsversuchAbgeschlossen_ShouldRedirectToNextTeilaufgabe() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        LoesungsVersuchDTO abgeschlossenerVersuch = new LoesungsVersuchDTO();
        abgeschlossenerVersuch.setId(2000L);
        abgeschlossenerVersuch.setTeilaufgabeId(1000L);
        abgeschlossenerVersuch.setBewertungPunkte(75);

        TeilaufgabeDto naechsteTeilaufgabe = new TeilaufgabeDto();
        naechsteTeilaufgabe.setId(1001L);

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(loesungsversuchService.markiereLoesungsversuchAbgeschlossen(2000L, 1L)).thenReturn(abgeschlossenerVersuch);
        when(teilaufgabeService.getTeilaufgabeById(1000L)).thenReturn(mockTeilaufgabeDto);
        when(aufgabeService.getAufgabeByTeilaufgabeId(1000L)).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.findeNaechsteTeilaufgabe(any(), eq(1000L), eq(1L))).thenReturn(Optional.of(naechsteTeilaufgabe));

        // Act
        String result = controller.markiereLoesungsversuchAbgeschlossen(2000L, redirectAttributes, model);

        // Assert
        assertEquals("redirect:/student/aufgaben/100?teilaufgabe=1001", result);
        String successMessage = (String) redirectAttributes.getFlashAttributes().get("successMessage");
        assertNotNull(successMessage);
        assertTrue(successMessage.contains("75"));
    }

    @Test
    @DisplayName("showAufgabe sollte Exception werfen wenn Aufgabe nicht gefunden")
    void showAufgabe_ShouldThrowExceptionWhenAufgabeNotFound() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeById(999L)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            controller.showAufgabe(999L, null, model, redirectAttributes)
        );
    }

    @Test
    @DisplayName("showAufgabe sollte mit allgemeinem Aufgabentext umgehen")
    void showAufgabe_ShouldHandleGeneralAufgabentext() {
        // Arrange
        Model model = new ConcurrentModel();
        NutzerDTO nutzerDTO = new StudentDTO();
        nutzerDTO.setId(1L);

        // Setup Aufgabe mit allgemeinem Text
        mockAufgabeDto.setAufgabenText("Allgemeiner Aufgabentext");

        when(nutzerService.getAuthenticatedNutzer()).thenReturn(nutzerDTO);
        when(nutzerService.getStudentById(1L)).thenReturn(Optional.of(mockStudentDTO));
        when(aufgabeService.getAufgabeById(100L)).thenReturn(mockAufgabeDto);
        when(studentAufgabeService.hatZugangZuAufgabe(100L, 1L)).thenReturn(true);
        when(studentAufgabeService.ermittleAktiveTeilaufgabe(mockAufgabeDto, null, 1L)).thenReturn(mockTeilaufgabeDto);
        
        // Mock Markdown rendering
        MarkdownRenderResult markdownResult = new MarkdownRenderResult("<p>Allgemeiner Text</p>", new ArrayList<>());
        when(markdownService.renderMarkdownForStudentInput(anyString(), anyLong())).thenReturn(markdownResult);
        
        // Mock KurseinheitDTO
        KurseinheitDTO mockKurseinheit = new KurseinheitDTO();
        mockKurseinheit.setId(10L);
        mockKurseinheit.setKursId(8L);
        when(kurseinheitService.getKurseinheitById(10L)).thenReturn(mockKurseinheit);
        
        // Mock KursDTO
        KursDTO mockKurs = new KursDTO();
        mockKurs.setId(8L);
        mockKurs.setName("Test Kurs");
        when(kursService.getKursById(8L)).thenReturn(mockKurs);
        
        // Act
        String viewName = controller.showAufgabe(100L, null, model, redirectAttributes);
        
        // Assert
        assertEquals("student/aufgabe/aufgabe-bearbeiten", viewName);
        assertNotNull(model.getAttribute("allgemeinerAufgabentext"));
        assertEquals(8L, model.getAttribute("kursId"));
        verify(markdownService, times(2)).renderMarkdownForStudentInput(anyString(), anyLong());
    }
}