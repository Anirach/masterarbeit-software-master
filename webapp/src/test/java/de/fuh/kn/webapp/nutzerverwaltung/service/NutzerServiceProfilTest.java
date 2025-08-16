package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.persistence.entity.Kursbetreuer;
import de.fuh.kn.webapp.persistence.entity.Nutzer;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.KursbetreuerRepository;
import de.fuh.kn.webapp.persistence.repository.NutzerRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test für die Profil-bezogenen Methoden des NutzerService.
 */
@ExtendWith(MockitoExtension.class)
public class NutzerServiceProfilTest {

    @Mock
    private NutzerRepository nutzerRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private KursbetreuerRepository kursbetreuerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfilMapper profilMapper;

    @Mock
    private NutzerMapper nutzerMapper;

    @InjectMocks
    private NutzerService nutzerService;

    private Student testStudent;
    private StudentDTO testStudentDTO;
    private Kursbetreuer testKursbetreuer;
    private ProfilAenderungDTO profilAenderungDTO;
    private PasswortAenderungDTO passwortAenderungDTO;

    @BeforeEach
    void setUp() {
        // Testdaten für Student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setEmail("student@example.com");
        testStudent.setVorname("Test");
        testStudent.setNachname("Student");
        testStudent.setPasswort("encodedPassword");
        testStudent.setIstRegistriert(true);
        testStudent.setMatrikelnummer("123456");

        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setEmail("student@example.com");
        testStudentDTO.setVorname("Test");
        testStudentDTO.setNachname("Student");
        testStudentDTO.setIstRegistriert(true);
        testStudentDTO.setMatrikelnummer("123456");

        // Testdaten für Kursbetreuer
        testKursbetreuer = new Kursbetreuer();
        testKursbetreuer.setId(2L);
        testKursbetreuer.setEmail("betreuer@example.com");
        testKursbetreuer.setVorname("Test");
        testKursbetreuer.setNachname("Betreuer");
        testKursbetreuer.setPasswort("encodedPassword");
        testKursbetreuer.setIstRegistriert(true);

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
    }

    @Test
    void aktualisiereNutzerProfil_mitValidenDaten_returnsAktualisiertenNutzer() {
        // Given
        when(nutzerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(nutzerRepository.save(any(Nutzer.class))).thenReturn(testStudent);
        
        // Verhalten für den Mapper konfigurieren
        doNothing().when(profilMapper).updateNutzerFromDto(any(ProfilAenderungDTO.class), any(Nutzer.class));
        when(nutzerRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(nutzerMapper.toDto(testStudent)).thenReturn(new NutzerDTO());

        // When
        NutzerDTO aktualisierterNutzer = nutzerService.aktualisiereNutzerProfil(testStudentDTO, profilAenderungDTO);

        // Then
        assertNotNull(aktualisierterNutzer);
        verify(profilMapper).updateNutzerFromDto(eq(profilAenderungDTO), eq(testStudent));
        verify(nutzerRepository).save(testStudent);
    }

    @Test
    void aktualisiereNutzerProfil_mitExistierenderEmail_throwsException() {
        // Given
        Student andererStudent = new Student();
        andererStudent.setId(3L);
        andererStudent.setEmail("neuemail@example.com");

        when(nutzerRepository.findByEmail("neuemail@example.com")).thenReturn(Optional.of(andererStudent));

        // When, Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            nutzerService.aktualisiereNutzerProfil(testStudentDTO, profilAenderungDTO);
        });

        assertEquals("Ein anderer Nutzer mit dieser E-Mail-Adresse existiert bereits", exception.getMessage());
        verify(profilMapper, never()).updateNutzerFromDto(any(), any(Nutzer.class));
        verify(profilMapper, never()).updateNutzerFromDto(any(), any(NutzerDTO.class));
        verify(nutzerRepository, never()).save(any());
    }

    @Test
    void aktualisiereNutzerProfil_mitGleicherEmail_aktualisiert() {
        // Given
        profilAenderungDTO.setEmail(testStudent.getEmail()); // Gleiche E-Mail wie vorher

        when(nutzerRepository.save(any(Nutzer.class))).thenReturn(testStudent);
        doNothing().when(profilMapper).updateNutzerFromDto(any(ProfilAenderungDTO.class), any(Nutzer.class));
        when(nutzerRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(nutzerMapper.toDto(testStudent)).thenReturn(new NutzerDTO());

        // When
        NutzerDTO aktualisierterNutzer = nutzerService.aktualisiereNutzerProfil(testStudentDTO, profilAenderungDTO);

        // Then
        assertNotNull(aktualisierterNutzer);
        verify(profilMapper).updateNutzerFromDto(eq(profilAenderungDTO), eq(testStudent));
        verify(nutzerRepository).save(testStudent);
        // Keine Abfrage nach der E-Mail, da sie identisch ist
        verify(nutzerRepository, never()).findByEmail(anyString());
    }

}
