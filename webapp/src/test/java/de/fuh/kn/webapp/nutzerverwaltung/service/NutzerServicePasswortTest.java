package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentMapper;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.KursbetreuerRepository;
import de.fuh.kn.webapp.persistence.repository.NutzerRepository;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests für die Passwortfunktionalitäten des NutzerService.
 * Diese Tests konzentrieren sich speziell auf die Passwortverarbeitung und -validierung.
 */
@ExtendWith(MockitoExtension.class)
public class NutzerServicePasswortTest {

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
    private StudentMapper studentMapper;
    
    @Mock
    private KursbetreuerMapper kursbetreuerMapper;

    @Mock
    private NutzerMapper nutzerMapper;

    @Mock
    private RegistrierungMapper registrierungMapper;

    @InjectMocks
    private NutzerService nutzerService;

    @Captor
    private ArgumentCaptor<Student> studentCaptor;
    
    private RegistrierungDTO testRegistrierungDTO;

    @BeforeEach
    void setUp() {
        // RegistrierungDTO-Testdaten
        testRegistrierungDTO = new RegistrierungDTO();
        testRegistrierungDTO.setEmail("test@student.de");
        testRegistrierungDTO.setVorname("Test");
        testRegistrierungDTO.setNachname("Student");
        testRegistrierungDTO.setMatrikelnummer("123456");
        testRegistrierungDTO.setPasswort("MeinPasswort123!");
        testRegistrierungDTO.setPasswortBestaetigung("MeinPasswort123!");
    }

    @Test
    void testPasswortVerschluesselungBeiRegistrierungMitDTO() {
        // Test-Setup
        Student dummyStudent = new Student();
        dummyStudent.setEmail(null);
        dummyStudent.setVorname(null);
        dummyStudent.setNachname(null);
        dummyStudent.setMatrikelnummer("123456");
        dummyStudent.setPasswort("DummyPasswort");
        dummyStudent.setIstRegistriert(false);
        
        Student aktualisierterStudent = new Student();
        aktualisierterStudent.setEmail("test@student.de");
        aktualisierterStudent.setVorname("Test");
        aktualisierterStudent.setNachname("Student");
        aktualisierterStudent.setMatrikelnummer("123456");
        aktualisierterStudent.setPasswort("HashedPassword123");
        aktualisierterStudent.setIstRegistriert(true);

        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setEmail("test@student.de");
        studentDTO.setVorname("Test");
        studentDTO.setNachname("Student");
        studentDTO.setMatrikelnummer("123456");
        studentDTO.setIstRegistriert(true);

        // Mocks konfigurieren
        when(nutzerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByMatrikelnummer("123456")).thenReturn(Optional.of(dummyStudent));
        when(passwordEncoder.encode("MeinPasswort123!")).thenReturn("HashedPassword123");
        when(studentRepository.save(any(Student.class))).thenReturn(aktualisierterStudent);
        when(studentMapper.toDto(aktualisierterStudent)).thenReturn(studentDTO);

        // Methode aufrufen
        nutzerService.registriereStudent(testRegistrierungDTO);
        
        // Überprüfungen
        verify(nutzerRepository).findByEmail("test@student.de");
        verify(studentRepository).findByMatrikelnummer("123456");
        verify(passwordEncoder).encode("MeinPasswort123!");
        
        // Capture des gespeicherten Objekts, um Feldwerte zu überprüfen
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        Student gespeicherterStudent = studentCaptor.getValue();
        
        assertEquals("test@student.de", gespeicherterStudent.getEmail());
        assertEquals("Test", gespeicherterStudent.getVorname());
        assertEquals("Student", gespeicherterStudent.getNachname());
        assertEquals("HashedPassword123", gespeicherterStudent.getPasswort());
        assertEquals("123456", gespeicherterStudent.getMatrikelnummer());
        assertTrue(gespeicherterStudent.getIstRegistriert());
    }
    
    @Test
    void testLeeresPasswortRegistrierungMitDTO() {
        // Test-Setup für leeres Passwort
        RegistrierungDTO registrierungMitLeeremPasswort = new RegistrierungDTO();
        registrierungMitLeeremPasswort.setEmail("test@student.de");
        registrierungMitLeeremPasswort.setVorname("Test");
        registrierungMitLeeremPasswort.setNachname("Student");
        registrierungMitLeeremPasswort.setMatrikelnummer("123456");
        registrierungMitLeeremPasswort.setPasswort("");
        registrierungMitLeeremPasswort.setPasswortBestaetigung("");
        
        // Mocks konfigurieren - nur die, die tatsächlich aufgerufen werden
        when(nutzerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.registriereStudent(registrierungMitLeeremPasswort));
        
        assertEquals("Das Passwort darf nicht leer sein", exception.getMessage());
        verify(nutzerRepository).findByEmail("test@student.de");
        
        // Diese Methoden werden nie aufgerufen, da vorher schon eine Exception geworfen wird
        verify(studentRepository, never()).findByMatrikelnummer(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(studentRepository, never()).save(any(Student.class));
    }
    
    @Test
    void testSonderzeichenImPasswortMitDTO() {
        // Test-Setup für Passwort mit Sonderzeichen
        RegistrierungDTO registrierungMitSonderzeichen = new RegistrierungDTO();
        registrierungMitSonderzeichen.setEmail("test@student.de");
        registrierungMitSonderzeichen.setVorname("Test");
        registrierungMitSonderzeichen.setNachname("Student");
        registrierungMitSonderzeichen.setMatrikelnummer("123456");
        String klartext = "Pass!@#$%^&*()_+{}[]|:;<>,.?/~`word123";
        registrierungMitSonderzeichen.setPasswort(klartext);
        registrierungMitSonderzeichen.setPasswortBestaetigung(klartext);
        
        Student dummyStudent = new Student();
        dummyStudent.setMatrikelnummer("123456");
        dummyStudent.setPasswort("DummyPasswort");
        dummyStudent.setIstRegistriert(false);
        
        Student aktualisierterStudent = new Student();
        aktualisierterStudent.setEmail("test@student.de");
        aktualisierterStudent.setVorname("Test");
        aktualisierterStudent.setNachname("Student");
        aktualisierterStudent.setMatrikelnummer("123456");
        aktualisierterStudent.setPasswort("ComplexHashedPassword");
        aktualisierterStudent.setIstRegistriert(true);

        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setEmail("test@student.de");
        studentDTO.setVorname("Test");
        studentDTO.setNachname("Student");
        studentDTO.setMatrikelnummer("123456");
        studentDTO.setIstRegistriert(true);

        // Mocks konfigurieren
        when(nutzerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByMatrikelnummer("123456")).thenReturn(Optional.of(dummyStudent));
        when(passwordEncoder.encode(klartext)).thenReturn("ComplexHashedPassword");
        when(studentRepository.save(any(Student.class))).thenReturn(aktualisierterStudent);
        when(studentMapper.toDto(aktualisierterStudent)).thenReturn(studentDTO);

        // Methode aufrufen
        nutzerService.registriereStudent(registrierungMitSonderzeichen);
        
        // Überprüfungen
        verify(nutzerRepository).findByEmail("test@student.de");
        verify(studentRepository).findByMatrikelnummer("123456");
        verify(passwordEncoder).encode(klartext);
        
        // Capture des gespeicherten Objekts, um Feldwerte zu überprüfen
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        Student gespeicherterStudent = studentCaptor.getValue();
        
        assertEquals("test@student.de", gespeicherterStudent.getEmail());
        assertEquals("Test", gespeicherterStudent.getVorname());
        assertEquals("Student", gespeicherterStudent.getNachname());
        assertEquals("ComplexHashedPassword", gespeicherterStudent.getPasswort());
        assertEquals("123456", gespeicherterStudent.getMatrikelnummer());
        assertTrue(gespeicherterStudent.getIstRegistriert());
    }
    
    @Test
    void testPasswortMitMinimalllaenge() {
        // Test-Setup für Passwort mit zu wenig Zeichen
        RegistrierungDTO registrierungMitKurzemPasswort = new RegistrierungDTO();
        registrierungMitKurzemPasswort.setEmail("test@student.de");
        registrierungMitKurzemPasswort.setVorname("Test");
        registrierungMitKurzemPasswort.setNachname("Student");
        registrierungMitKurzemPasswort.setMatrikelnummer("123456");
        registrierungMitKurzemPasswort.setPasswort("1234567"); // Nur 7 Zeichen
        registrierungMitKurzemPasswort.setPasswortBestaetigung("1234567");
        
        // Mocks konfigurieren - nur die, die tatsächlich aufgerufen werden
        when(nutzerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            nutzerService.registriereStudent(registrierungMitKurzemPasswort));
        
        assertEquals("Das Passwort muss mindestens 8 Zeichen lang sein", exception.getMessage());
        verify(nutzerRepository).findByEmail("test@student.de");
        
        // Diese Methoden werden nie aufgerufen, da vorher schon eine Exception geworfen wird
        verify(studentRepository, never()).findByMatrikelnummer(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(studentRepository, never()).save(any(Student.class));
    }
    
    @Test
    void testPasswortMitExakterMinimalllaenge() {
        // Test-Setup für Passwort mit genau 8 Zeichen
        RegistrierungDTO registrierungMitAchtZeichenPasswort = new RegistrierungDTO();
        registrierungMitAchtZeichenPasswort.setEmail("test@student.de");
        registrierungMitAchtZeichenPasswort.setVorname("Test");
        registrierungMitAchtZeichenPasswort.setNachname("Student");
        registrierungMitAchtZeichenPasswort.setMatrikelnummer("123456");
        registrierungMitAchtZeichenPasswort.setPasswort("12345678"); // Genau 8 Zeichen
        registrierungMitAchtZeichenPasswort.setPasswortBestaetigung("12345678");
        
        Student dummyStudent = new Student();
        dummyStudent.setMatrikelnummer("123456");
        dummyStudent.setPasswort("DummyPasswort");
        dummyStudent.setIstRegistriert(false);
        
        Student aktualisierterStudent = new Student();
        aktualisierterStudent.setEmail("test@student.de");
        aktualisierterStudent.setVorname("Test");
        aktualisierterStudent.setNachname("Student");
        aktualisierterStudent.setMatrikelnummer("123456");
        aktualisierterStudent.setPasswort("HashedPassword");
        aktualisierterStudent.setIstRegistriert(true);

        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setEmail("test@student.de");
        studentDTO.setVorname("Test");
        studentDTO.setNachname("Student");
        studentDTO.setMatrikelnummer("123456");
        studentDTO.setIstRegistriert(true);

        // Mocks konfigurieren
        when(nutzerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByMatrikelnummer("123456")).thenReturn(Optional.of(dummyStudent));
        when(passwordEncoder.encode("12345678")).thenReturn("HashedPassword");
        when(studentRepository.save(any(Student.class))).thenReturn(aktualisierterStudent);
        when(studentMapper.toDto(aktualisierterStudent)).thenReturn(studentDTO);

        // Methode aufrufen
        StudentDTO ergebnis = nutzerService.registriereStudent(registrierungMitAchtZeichenPasswort);

        // Überprüfungen
        assertEquals(studentDTO, ergebnis);
        verify(nutzerRepository).findByEmail("test@student.de");
        verify(studentRepository).findByMatrikelnummer("123456");
        verify(passwordEncoder).encode("12345678");
        
        // Capture des gespeicherten Objekts, um Feldwerte zu überprüfen
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        Student gespeicherterStudent = studentCaptor.getValue();
        
        assertEquals("test@student.de", gespeicherterStudent.getEmail());
        assertEquals("Test", gespeicherterStudent.getVorname());
        assertEquals("Student", gespeicherterStudent.getNachname());
        assertEquals("HashedPassword", gespeicherterStudent.getPasswort());
        assertEquals("123456", gespeicherterStudent.getMatrikelnummer());
        assertTrue(gespeicherterStudent.getIstRegistriert());
    }
}
