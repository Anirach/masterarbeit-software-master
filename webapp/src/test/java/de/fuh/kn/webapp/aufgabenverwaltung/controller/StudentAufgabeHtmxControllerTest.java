package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.LoesungsversuchService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.security.WithMockStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentAufgabeHtmxControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NutzerService nutzerService;

    @Mock
    private TeilaufgabeService teilaufgabeService;

    @Mock
    private LoesungsversuchService loesungsversuchService;

    @InjectMocks
    private StudentAufgabeHtmxController controller;

    private StudentDTO mockStudentDTO;
    private TeilaufgabeDto mockTeilaufgabeDto;
    private List<LoesungsVersuchDTO> mockLoesungsversuche;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Mock Student
        mockStudentDTO = new StudentDTO();
        mockStudentDTO.setId(1L);

        // Mock Teilaufgabe
        mockTeilaufgabeDto = new TeilaufgabeDto();
        mockTeilaufgabeDto.setId(1000L);
        mockTeilaufgabeDto.setReihenfolge(1);
        mockTeilaufgabeDto.setAufgabeId(100L);

        // Mock Lösungsversuche
        mockLoesungsversuche = new ArrayList<>();
        LoesungsVersuchDTO versuch1 = new LoesungsVersuchDTO();
        versuch1.setId(10001L);
        versuch1.setStudentId(1L);
        versuch1.setTeilaufgabeId(1000L);
        versuch1.setZeitpunkt(LocalDateTime.now().minusMinutes(30));
        versuch1.setBewertungPunkte(75);
        versuch1.setBewertungFeedback("Gute Arbeit!");
        versuch1.setLoesungFelder(new HashMap<>());
        mockLoesungsversuche.add(versuch1);

        LoesungsVersuchDTO versuch2 = new LoesungsVersuchDTO();
        versuch2.setId(10002L);
        versuch2.setStudentId(1L);
        versuch2.setTeilaufgabeId(1000L);
        versuch2.setZeitpunkt(LocalDateTime.now().minusHours(1));
        versuch2.setBewertungPunkte(40);
        versuch2.setBewertungFeedback("Noch Verbesserungsbedarf.");
        versuch2.setLoesungFelder(new HashMap<>());
        versuch2.setIstZurueckGesetzt(true);
        mockLoesungsversuche.add(versuch2);
    }

    @Test
    @WithMockStudent
    void getLösungsversuche_returnsLoesungsversucheView() throws Exception {
        // Arrange
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(mockStudentDTO);
        when(nutzerService.getStudentById(anyLong())).thenReturn(Optional.of(mockStudentDTO));
        when(teilaufgabeService.getTeilaufgabeById(anyLong())).thenReturn(mockTeilaufgabeDto);
        when(loesungsversuchService.findeAlleLösungsversuche(anyLong(), anyLong())).thenReturn(mockLoesungsversuche);

        // Act & Assert
        mockMvc.perform(get("/student/aufgaben/htmx/loesungsversuche")
                .param("teilaufgabeId", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/aufgabe/fragments/loesungsversuche-modal-fragment :: loesungsversuche-content"))
                .andExpect(model().attribute("teilaufgabe", mockTeilaufgabeDto))
                .andExpect(model().attribute("loesungsversuche", mockLoesungsversuche));
    }
}