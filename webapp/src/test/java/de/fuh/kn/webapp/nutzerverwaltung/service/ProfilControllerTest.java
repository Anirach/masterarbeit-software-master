package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.nutzerverwaltung.auth.StudentUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.dto.*;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test für den ProfilController.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProfilControllerTest {

    @Mock
    private NutzerService nutzerService;

    @Mock
    private ProfilMapper profilMapper;
    
    @Mock
    private NutzerMapper nutzerMapper;

    @Mock
    private UserDetailsService userDetailsService;
    
    @Mock
    private AktivitaetsService aktivitaetsService;
    
    @Mock
    private AktivitaetMapper aktivitaetMapper;
    
    @Mock
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private ProfilController profilController;

    private MockMvc mockMvc;
    private StudentDTO testStudent;
    private StudentUserDetails testStudentUserDetails;
    private ProfilAenderungDTO profilAenderungDTO;
    private PasswortAenderungDTO passwortAenderungDTO;

    @BeforeEach
    void setUp() {
        // Controller mit allen Abhängigkeiten manuell erstellen
        profilController = new ProfilController(
            nutzerService, 
            profilMapper, 
            userDetailsService, 
            aktivitaetsService, 
            aktivitaetMapper, 
            nutzerMapper,
            aktivitaetsProtokollierungService
        );
        
        // MockMvc setup
        mockMvc = MockMvcBuilders.standaloneSetup(profilController).build();

        // SecurityContext Mock-Setup
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // Nutzer-Daten für Tests
        testStudent = new StudentDTO();
        testStudent.setId(1L);
        testStudent.setEmail("student@example.com");
        testStudent.setVorname("Test");
        testStudent.setNachname("Student");
        testStudent.setMatrikelnummer("123456");
        testStudent.setIstRegistriert(true);

        // UserDetails für Tests
        testStudentUserDetails = mock(StudentUserDetails.class);
        when(testStudentUserDetails.getId()).thenReturn(1L);
        when(testStudentUserDetails.getUsername()).thenReturn("student@example.com");

        // ProfilAenderungDTO für Tests
        profilAenderungDTO = new ProfilAenderungDTO();
        profilAenderungDTO.setEmail("neuemail@example.com");
        profilAenderungDTO.setVorname("NeuerVorname");
        profilAenderungDTO.setNachname("NeuerNachname");

        // PasswortAenderungDTO für Tests
        passwortAenderungDTO = new PasswortAenderungDTO();
        passwortAenderungDTO.setAktuellesPasswort("aktuellesPasswort");
        passwortAenderungDTO.setNeuesPasswort("neuesPasswort123");
        passwortAenderungDTO.setPasswortBestaetigung("neuesPasswort123");

        // Mock-Verhalten für AktivitaetsService
        when(aktivitaetsService.findeAktivitaetenFuerNutzerPaged(any(NutzerDTO.class), eq(0), eq(10)))
                .thenReturn(Page.empty());
    }

    @Test
    void showProfilForm_mitAuthenticatedUser_zeigtProfilSeite() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testStudentUserDetails);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(profilMapper.toDto(any(NutzerDTO.class))).thenReturn(profilAenderungDTO);

        // When
        String viewName = profilController.showProfilForm(model);

        // Then
        assertEquals("profil/profil", viewName);
        verify(model).addAttribute(eq("profilAenderung"), eq(profilAenderungDTO));
        verify(model).addAttribute(eq("passwortAenderung"), any(PasswortAenderungDTO.class));
        verify(model).addAttribute(eq("aktivitaeten"), any(Page.class));
        verify(aktivitaetsService).findeAktivitaetenFuerNutzerPaged(testStudent, 0, 10);
    }

    @Test
    void showProfilForm_ohneAuthenticatedUser_redirectZuLogin() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        String viewName = profilController.showProfilForm(model);

        // Then
        assertEquals("redirect:/login", viewName);
        verify(model, never()).addAttribute(anyString(), any());
    }

    @Test
    void updateProfil_mitValidenDaten_aktualisiertProfil() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testStudentUserDetails);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testStudentUserDetails);

        // When
        String viewName = profilController.updateProfil(profilAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/profil", viewName);
        verify(nutzerService).aktualisiereNutzerProfil(eq(testStudent), eq(profilAenderungDTO));
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(securityContext).setAuthentication(any());
    }

    @Test
    void updateProfil_mitBindingErrors_zeigtProfilSeiteMitErrors() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String viewName = profilController.updateProfil(profilAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("profil/profil", viewName);
        verify(nutzerService, never()).aktualisiereNutzerProfil(any(), any());
        verify(redirectAttributes, never()).addFlashAttribute(anyString(), anyString());
    }

    @Test
    void updateProfil_mitIllegalArgumentException_zeigtFehlermeldung() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testStudentUserDetails);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // Die Methode wirft eine Exception
        doThrow(new IllegalArgumentException("E-Mail bereits vergeben")).when(nutzerService)
            .aktualisiereNutzerProfil(any(), any());

        // When
        String viewName = profilController.updateProfil(profilAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/profil", viewName);
        verify(nutzerService).aktualisiereNutzerProfil(eq(testStudent), eq(profilAenderungDTO));
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    void changePassword_mitValidenDaten_aendertPasswort() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testStudentUserDetails);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            any(NutzerDTO.class),
            eq(AktivitaetsTyp.PASSWORT_AENDERN),
            anyString(), 
            isNull(), 
            eq(true), 
            isNull(), 
            isNull())).thenReturn(new AktivitaetDTO());

        // When
        String viewName = profilController.changePassword(passwortAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/profil", viewName);
        verify(nutzerService).aenderePasswort(eq(testStudent), eq(passwortAenderungDTO));
        verify(redirectAttributes).addFlashAttribute(eq("passwordSuccessMessage"), anyString());
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            eq(testStudent),
            eq(AktivitaetsTyp.PASSWORT_AENDERN), 
            anyString(), 
            isNull(), 
            eq(true), 
            isNull(), 
            isNull());
    }

    @Test
    void changePassword_mitNichtUebereinstimmendenPasswoertern_zeigtFehlermeldung() {
        // Given
        passwortAenderungDTO.setPasswortBestaetigung("anderesPasswort");

        // When
        String viewName = profilController.changePassword(passwortAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/profil", viewName);
        verify(nutzerService, never()).aenderePasswort(any(), any());
        verify(redirectAttributes).addFlashAttribute(eq("passwordErrorMessage"), anyString());
        verify(bindingResult).rejectValue(eq("passwortBestaetigung"), anyString(), anyString());
    }

    @Test
    void changePassword_mitLeeremPasswort_zeigtFehlermeldung() {
        // Given
        passwortAenderungDTO.setNeuesPasswort("");
        passwortAenderungDTO.setPasswortBestaetigung("");

        // When
        String viewName = profilController.changePassword(passwortAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/profil", viewName);
        verify(nutzerService, never()).aenderePasswort(any(), any());
        verify(redirectAttributes).addFlashAttribute(eq("passwordErrorMessage"), eq("Das Passwort darf nicht leer sein"));
        verify(bindingResult).rejectValue(eq("neuesPasswort"), anyString(), anyString());
    }

    @Test
    void changePassword_mitZuKurzemPasswort_zeigtFehlermeldung() {
        // Given
        passwortAenderungDTO.setNeuesPasswort("kurz");
        passwortAenderungDTO.setPasswortBestaetigung("kurz");

        // When
        String viewName = profilController.changePassword(passwortAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/profil", viewName);
        verify(nutzerService, never()).aenderePasswort(any(), any());
        verify(redirectAttributes).addFlashAttribute(eq("passwordErrorMessage"), eq("Das Passwort muss mindestens 8 Zeichen lang sein"));
        verify(bindingResult).rejectValue(eq("neuesPasswort"), anyString(), anyString());
    }

    @Test
    void changePassword_mitBindingErrors_zeigtProfilSeiteMitErrors() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String viewName = profilController.changePassword(passwortAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("profil/profil", viewName);
        verify(nutzerService, never()).aenderePasswort(any(), any());
    }

    @Test
    void changePassword_mitIllegalArgumentException_zeigtFehlermeldung() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testStudentUserDetails);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(bindingResult.hasErrors()).thenReturn(false);

        // Die Methode wirft eine Exception
        doThrow(new IllegalArgumentException("Aktuelles Passwort falsch")).when(nutzerService)
            .aenderePasswort(any(), any());

        // When
        String viewName = profilController.changePassword(passwortAenderungDTO, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/profil", viewName);
        verify(nutzerService).aenderePasswort(eq(testStudent), eq(passwortAenderungDTO));
        verify(redirectAttributes).addFlashAttribute(eq("passwordErrorMessage"), anyString());
    }
}