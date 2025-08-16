package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.auth.KursbetreuerUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.auth.StudentUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.dto.*;
import de.fuh.kn.webapp.persistence.entity.Kursbetreuer;
import de.fuh.kn.webapp.persistence.entity.Nutzer;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests für den NutzerService.
 * Fokussiert auf die Registrierung von Studierenden und die Konvertierung 
 * zwischen UserDetails und Entitäten.
 */
@ExtendWith(MockitoExtension.class)
public class NutzerServiceTest {

    @Mock
    private NutzerRepository nutzerRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private KursbetreuerRepository kursbetreuerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private RegistrierungMapper registrierungMapper;
    
    @Mock
    private ProfilMapper profilMapper;
    
    @Mock
    private StudentMapper studentMapper;
    
    @Mock
    private KursbetreuerMapper kursbetreuerMapper;
    
    @Mock
    private NutzerMapper nutzerMapper;

    @InjectMocks
    private NutzerService nutzerService;

    @Captor
    private ArgumentCaptor<Student> studentCaptor;
    
    @Captor
    private ArgumentCaptor<Kursbetreuer> kursbetreuerCaptor;

    private Student testStudent;
    private StudentDTO testStudentDTO;
    private Student dummyStudent;
    private Kursbetreuer testKursbetreuer;
    private KursbetreuerDTO testKursbetreuerDTO;
    private RegistrierungDTO testRegistrierungDTO;

    @BeforeEach
    void setUp() {
        // Student-Testdaten
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setEmail("student@test.de");
        testStudent.setPasswort("encodedPassword123");
        testStudent.setVorname("Max");
        testStudent.setNachname("Mustermann");
        testStudent.setMatrikelnummer("123456");
        testStudent.setIstRegistriert(true);

        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setEmail("student@test.de");
        testStudentDTO.setVorname("Max");
        testStudentDTO.setNachname("Mustermann");
        testStudentDTO.setMatrikelnummer("123456");
        testStudentDTO.setIstRegistriert(true);

        // Dummy-Student Testdaten
        dummyStudent = new Student();
        dummyStudent.setId(3L);
        dummyStudent.setMatrikelnummer("654321");
        dummyStudent.setPasswort("dummy-password");
        dummyStudent.setIstRegistriert(false);

        // Kursbetreuer-Testdaten
        testKursbetreuer = new Kursbetreuer();
        testKursbetreuer.setId(2L);
        testKursbetreuer.setEmail("betreuer@test.de");
        testKursbetreuer.setPasswort("encodedPassword456");
        testKursbetreuer.setVorname("Erika");
        testKursbetreuer.setNachname("Musterfrau");
        testKursbetreuer.setIstRegistriert(true);

        testKursbetreuerDTO = new KursbetreuerDTO();
        testKursbetreuerDTO.setId(2L);
        testKursbetreuerDTO.setEmail("betreuer@test.de");
        testKursbetreuerDTO.setVorname("Erika");
        testKursbetreuerDTO.setNachname("Musterfrau");
        testKursbetreuerDTO.setIstRegistriert(true);
        
        // RegistrierungDTO-Testdaten
        testRegistrierungDTO = new RegistrierungDTO();
        testRegistrierungDTO.setEmail("neu@student.de");
        testRegistrierungDTO.setVorname("Neuer");
        testRegistrierungDTO.setNachname("Student");
        testRegistrierungDTO.setMatrikelnummer("654321");
        testRegistrierungDTO.setPasswort("Passwort123");
        testRegistrierungDTO.setPasswortBestaetigung("Passwort123");
    }

    @Test
    void testRegistriereStudentErfolgreich() {
        // Aktualisierter Student nach der Registrierung
        Student registrierterStudent = new Student();
        registrierterStudent.setId(3L);
        registrierterStudent.setEmail("neu@student.de");
        registrierterStudent.setVorname("Neuer");
        registrierterStudent.setNachname("Student");
        registrierterStudent.setMatrikelnummer("654321");
        registrierterStudent.setPasswort("encodedPassword123");
        registrierterStudent.setIstRegistriert(true);
        
        // StudentDTO als Rückgabewert
        StudentDTO registrierterStudentDTO = new StudentDTO();
        registrierterStudentDTO.setId(3L);
        registrierterStudentDTO.setEmail("neu@student.de");
        registrierterStudentDTO.setVorname("Neuer");
        registrierterStudentDTO.setNachname("Student");
        registrierterStudentDTO.setMatrikelnummer("654321");
        registrierterStudentDTO.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(nutzerRepository.findByEmail("neu@student.de")).thenReturn(Optional.empty());
        when(studentRepository.findByMatrikelnummer("654321")).thenReturn(Optional.of(dummyStudent));
        when(passwordEncoder.encode("Passwort123")).thenReturn("encodedPassword123");
        when(studentRepository.save(any(Student.class))).thenReturn(registrierterStudent);
        when(studentMapper.toDto(registrierterStudent)).thenReturn(registrierterStudentDTO);
        
        // Methode aufrufen
        StudentDTO ergebnis = nutzerService.registriereStudent(testRegistrierungDTO);
        
        // Überprüfungen
        assertEquals(registrierterStudentDTO, ergebnis);
        verify(nutzerRepository).findByEmail("neu@student.de");
        verify(studentRepository).findByMatrikelnummer("654321");
        verify(passwordEncoder).encode("Passwort123");
        
        // Capture des gespeicherten Objekts, um Feldwerte zu überprüfen
        verify(studentRepository).save(studentCaptor.capture());
        Student gespeicherterStudent = studentCaptor.getValue();
        
        assertEquals("neu@student.de", gespeicherterStudent.getEmail());
        assertEquals("Neuer", gespeicherterStudent.getVorname());
        assertEquals("Student", gespeicherterStudent.getNachname());
        assertEquals("encodedPassword123", gespeicherterStudent.getPasswort());
        assertEquals("654321", gespeicherterStudent.getMatrikelnummer());
        assertTrue(gespeicherterStudent.getIstRegistriert());
        verify(studentMapper).toDto(registrierterStudent);
    }

    @Test
    void testRegistriereStudentEmailExistiertBereits() {
        // Bereits vorhandener registrierter Nutzer mit gleicher Email
        Nutzer existierenderNutzer = new Student();
        existierenderNutzer.setEmail("neu@student.de");
        existierenderNutzer.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(nutzerRepository.findByEmail("neu@student.de")).thenReturn(Optional.of(existierenderNutzer));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.registriereStudent(testRegistrierungDTO));
        
        assertEquals("Ein Nutzer mit dieser E-Mail-Adresse existiert bereits", exception.getMessage());
        verify(nutzerRepository).findByEmail("neu@student.de");
        verify(studentRepository, never()).findByMatrikelnummer(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void testRegistriereStudentKeinDummyStudentVorhanden() {
        // Mocks konfigurieren
        when(nutzerRepository.findByEmail("neu@student.de")).thenReturn(Optional.empty());
        when(studentRepository.findByMatrikelnummer("654321")).thenReturn(Optional.empty());
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.registriereStudent(testRegistrierungDTO));
        
        assertEquals("Es existiert kein nicht-registrierter Student mit dieser Matrikelnummer im System", exception.getMessage());
        verify(nutzerRepository).findByEmail("neu@student.de");
        verify(studentRepository).findByMatrikelnummer("654321");
        verify(passwordEncoder, never()).encode(anyString());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void testRegistriereStudentDummyStudentBereitsRegistriert() {
        // Student mit gleicher Matrikelnummer, aber bereits registriert
        Student existierenderStudent = new Student();
        existierenderStudent.setMatrikelnummer("654321");
        existierenderStudent.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(nutzerRepository.findByEmail("neu@student.de")).thenReturn(Optional.empty());
        when(studentRepository.findByMatrikelnummer("654321")).thenReturn(Optional.of(existierenderStudent));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.registriereStudent(testRegistrierungDTO));
        
        assertEquals("Es existiert kein nicht-registrierter Student mit dieser Matrikelnummer im System", exception.getMessage());
        verify(nutzerRepository).findByEmail("neu@student.de");
        verify(studentRepository).findByMatrikelnummer("654321");
        verify(passwordEncoder, never()).encode(anyString());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void testFindeNutzerNachEmail() {
        // NutzerDTO erstellen
        StudentDTO testNutzerDTO = new StudentDTO();
        testNutzerDTO.setId(1L);
        testNutzerDTO.setEmail("student@test.de");
        testNutzerDTO.setVorname("Max");
        testNutzerDTO.setNachname("Mustermann");
        testNutzerDTO.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(nutzerRepository.findByEmail("student@test.de")).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDto(testStudent)).thenReturn(testNutzerDTO);
        
        // Methode aufrufen
        Optional<NutzerDTO> gefundenerNutzer = nutzerService.findeNutzerNachEmail("student@test.de");
        
        // Überprüfungen
        assertTrue(gefundenerNutzer.isPresent());
        assertEquals(testNutzerDTO, gefundenerNutzer.get());
        verify(nutzerRepository).findByEmail("student@test.de");
        verify(studentMapper).toDto(testStudent);
    }

    @Test
    void testGetAuthenticatedNutzer_Student() {
        // Testdaten für StudentUserDetails erstellen
        StudentUserDetails studentUserDetails = new StudentUserDetails(testStudentDTO, "1234");

        // Authentication-Objekt erstellen
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(studentUserDetails);
        when(authentication.isAuthenticated()).thenReturn(true);
        
        // SecurityContext erstellen und in den SecurityContextHolder setzen
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        // Mocks konfigurieren
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        
        // Methode aufrufen
        NutzerDTO ergebnis = nutzerService.getAuthenticatedNutzer();
        
        // Überprüfungen
        assertNotNull(ergebnis);
        assertEquals(testStudentDTO, ergebnis);
        verify(studentRepository).findById(1L);
        verify(studentMapper).toDto(testStudent);
        
        // SecurityContext zurücksetzen
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testGetAuthenticatedNutzer_Kursbetreuer() {
        // Testdaten für KursbetreuerUserDetails erstellen
        KursbetreuerUserDetails kursbetreuerUserDetails = new KursbetreuerUserDetails(testKursbetreuerDTO, "1234");

        // Authentication-Objekt erstellen
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(kursbetreuerUserDetails);
        when(authentication.isAuthenticated()).thenReturn(true);
        
        // SecurityContext erstellen und in den SecurityContextHolder setzen
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        // KursbetreuerDTO erstellen
        
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(2L)).thenReturn(Optional.of(testKursbetreuer));
        when(kursbetreuerMapper.toDto(testKursbetreuer)).thenReturn(testKursbetreuerDTO);
        
        // Methode aufrufen
        NutzerDTO ergebnis = nutzerService.getAuthenticatedNutzer();
        
        // Überprüfungen
        assertNotNull(ergebnis);
        assertEquals(testKursbetreuerDTO, ergebnis);
        verify(kursbetreuerRepository).findById(2L);
        verify(kursbetreuerMapper).toDto(testKursbetreuer);
        
        // SecurityContext zurücksetzen
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testGetAuthenticatedNutzer_NichtAuthentifiziert() {
        // Authentication-Objekt erstellen (nicht authentifiziert)
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        
        // SecurityContext erstellen und in den SecurityContextHolder setzen
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        // Methode aufrufen
        NutzerDTO ergebnis = nutzerService.getAuthenticatedNutzer();
        
        // Überprüfungen
        assertNull(ergebnis);
        
        // SecurityContext zurücksetzen
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testGetAuthenticatedNutzer_KeinUserDetails() {
        // Authentication-Objekt erstellen (mit anderem Principal als UserDetails)
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        when(authentication.isAuthenticated()).thenReturn(true);
        
        // SecurityContext erstellen und in den SecurityContextHolder setzen
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        // Methode aufrufen
        NutzerDTO ergebnis = nutzerService.getAuthenticatedNutzer();
        
        // Überprüfungen
        assertNull(ergebnis);
        
        // SecurityContext zurücksetzen
        SecurityContextHolder.clearContext();
    }

    @Test
    void testExistiertDummyStudentMitMatrikelnummer() {
        // Mocks konfigurieren
        when(studentRepository.findByMatrikelnummer("654321")).thenReturn(Optional.of(dummyStudent));
        
        // Methode aufrufen
        boolean existiert = nutzerService.existiertDummyStudentMitMatrikelnummer("654321");
        
        // Überprüfungen
        assertTrue(existiert);
        verify(studentRepository).findByMatrikelnummer("654321");
    }
    
    @Test
    void testExistiertDummyStudentMitMatrikelnummerNichtVorhanden() {
        // Mocks konfigurieren
        when(studentRepository.findByMatrikelnummer("999999")).thenReturn(Optional.empty());
        
        // Methode aufrufen
        boolean existiert = nutzerService.existiertDummyStudentMitMatrikelnummer("999999");
        
        // Überprüfungen
        assertFalse(existiert);
        verify(studentRepository).findByMatrikelnummer("999999");
    }
    
    @Test
    void testExistiertDummyStudentMitMatrikelnummerBereitsRegistriert() {
        // Mocks konfigurieren
        when(studentRepository.findByMatrikelnummer("123456")).thenReturn(Optional.of(testStudent));
        
        // Methode aufrufen
        boolean existiert = nutzerService.existiertDummyStudentMitMatrikelnummer("123456");
        
        // Überprüfungen
        assertFalse(existiert);
        verify(studentRepository).findByMatrikelnummer("123456");
    }
    
    @Test
    void testErstelleDummyStudent_NeuerDummy() {
        // Neuer Dummy-Student
        Student neuerDummyStudent = new Student();
        neuerDummyStudent.setMatrikelnummer("888888");
        neuerDummyStudent.setPasswort("encoded-dummy-password");
        neuerDummyStudent.setIstRegistriert(false);

        StudentDTO neuerDummyStudentDto = new StudentDTO();

        // Mocks konfigurieren
        when(studentRepository.findByMatrikelnummer("888888")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("dummy-password")).thenReturn("encoded-dummy-password");
        when(studentRepository.save(any(Student.class))).thenReturn(neuerDummyStudent);
        when(studentMapper.toDto(neuerDummyStudent)).thenReturn(neuerDummyStudentDto);

        // Methode aufrufen
        StudentDTO ergebnis = nutzerService.erstelleDummyStudent("888888");
        
        // Überprüfungen
        assertEquals(neuerDummyStudentDto, ergebnis);
        verify(studentRepository).findByMatrikelnummer("888888");
        verify(passwordEncoder).encode("dummy-password");
        
        // Capture des gespeicherten Objekts, um Feldwerte zu überprüfen
        verify(studentRepository).save(studentCaptor.capture());
        Student gespeicherterStudent = studentCaptor.getValue();
        
        assertEquals("888888", gespeicherterStudent.getMatrikelnummer());
        assertEquals("encoded-dummy-password", gespeicherterStudent.getPasswort());
        assertFalse(gespeicherterStudent.getIstRegistriert());
        assertNull(gespeicherterStudent.getEmail());
        assertNull(gespeicherterStudent.getVorname());
        assertNull(gespeicherterStudent.getNachname());
    }
    
    @Test
    void testErstelleDummyStudent_ExistierenderDummy() {

        StudentDTO neuerDummyStudentDto = new StudentDTO();

        // Mocks konfigurieren
        when(studentRepository.findByMatrikelnummer("654321")).thenReturn(Optional.of(dummyStudent));

        // Methode aufrufen
        assertThrows(IllegalArgumentException.class, ()->nutzerService.erstelleDummyStudent("654321"));

        // Überprüfungen
        verify(studentRepository).findByMatrikelnummer("654321");
        verify(passwordEncoder, never()).encode(anyString());
        verify(studentRepository, never()).save(any(Student.class));
    }
    
    @Test
    void testGetAlleStudenten() {
        // Testdaten vorbereiten
        List<Student> studentenListe = new ArrayList<>();
        studentenListe.add(testStudent);
        studentenListe.add(dummyStudent);
        
        List<StudentDTO> studentDTOListe = new ArrayList<>();
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(1L);
        studentDTO.setVorname("Max");
        studentDTO.setNachname("Mustermann");
        studentDTOListe.add(studentDTO);
        
        // Mocks konfigurieren
        when(studentRepository.findAllByOrderByNachnameAscVornameAsc()).thenReturn(studentenListe);
        when(studentMapper.toDtoList(studentenListe)).thenReturn(studentDTOListe);
        
        // Methode aufrufen
        List<StudentDTO> ergebnis = nutzerService.getAlleStudenten();
        
        // Überprüfungen
        assertEquals(studentDTOListe, ergebnis);
        verify(studentRepository).findAllByOrderByNachnameAscVornameAsc();
        verify(studentMapper).toDtoList(studentenListe);
    }
    
    @Test
    void testGetAlleKursbetreuer() {
        // Testdaten vorbereiten
        List<Kursbetreuer> kursbetreuerListe = new ArrayList<>();
        kursbetreuerListe.add(testKursbetreuer);
        
        List<KursbetreuerDTO> kursbetreuerDTOListe = new ArrayList<>();
        KursbetreuerDTO kursbetreuerDTO = new KursbetreuerDTO();
        kursbetreuerDTO.setId(2L);
        kursbetreuerDTO.setVorname("Erika");
        kursbetreuerDTO.setNachname("Musterfrau");
        kursbetreuerDTOListe.add(kursbetreuerDTO);
        
        // Mocks konfigurieren
        when(kursbetreuerRepository.findAllByOrderByNachnameAscVornameAsc()).thenReturn(kursbetreuerListe);
        when(kursbetreuerMapper.toDtoList(kursbetreuerListe)).thenReturn(kursbetreuerDTOListe);
        
        // Methode aufrufen
        List<KursbetreuerDTO> ergebnis = nutzerService.getAlleKursbetreuer();
        
        // Überprüfungen
        assertEquals(kursbetreuerDTOListe, ergebnis);
        verify(kursbetreuerRepository).findAllByOrderByNachnameAscVornameAsc();
        verify(kursbetreuerMapper).toDtoList(kursbetreuerListe);
    }
    
    @Test
    void testGetStudentById() {
        // Testdaten vorbereiten
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(1L);
        studentDTO.setEmail("student@test.de");
        studentDTO.setVorname("Max");
        studentDTO.setNachname("Mustermann");
        studentDTO.setMatrikelnummer("123456");
        studentDTO.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDto(testStudent)).thenReturn(studentDTO);
        
        // Methode aufrufen
        Optional<StudentDTO> ergebnis = nutzerService.getStudentById(1L);
        
        // Überprüfungen
        assertTrue(ergebnis.isPresent());
        assertEquals(studentDTO, ergebnis.get());
        verify(studentRepository).findById(1L);
        verify(studentMapper).toDto(testStudent);
    }
    
    @Test
    void testGetStudentById_NichtGefunden() {
        // Mocks konfigurieren
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Methode aufrufen
        Optional<StudentDTO> ergebnis = nutzerService.getStudentById(99L);
        
        // Überprüfungen
        assertFalse(ergebnis.isPresent());
        verify(studentRepository).findById(99L);
        verify(studentMapper, never()).toDto(any(Student.class));
    }
    
    @Test
    void testGetKursbetreuerById() {
        // Testdaten vorbereiten
        KursbetreuerDTO kursbetreuerDTO = new KursbetreuerDTO();
        kursbetreuerDTO.setId(2L);
        kursbetreuerDTO.setEmail("betreuer@test.de");
        kursbetreuerDTO.setVorname("Erika");
        kursbetreuerDTO.setNachname("Musterfrau");
        kursbetreuerDTO.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(2L)).thenReturn(Optional.of(testKursbetreuer));
        when(kursbetreuerMapper.toDto(testKursbetreuer)).thenReturn(kursbetreuerDTO);
        
        // Methode aufrufen
        Optional<KursbetreuerDTO> ergebnis = nutzerService.getKursbetreuerById(2L);
        
        // Überprüfungen
        assertTrue(ergebnis.isPresent());
        assertEquals(kursbetreuerDTO, ergebnis.get());
        verify(kursbetreuerRepository).findById(2L);
        verify(kursbetreuerMapper).toDto(testKursbetreuer);
    }
    
    @Test
    void testGetKursbetreuerById_NichtGefunden() {
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Methode aufrufen
        Optional<KursbetreuerDTO> ergebnis = nutzerService.getKursbetreuerById(99L);
        
        // Überprüfungen
        assertFalse(ergebnis.isPresent());
        verify(kursbetreuerRepository).findById(99L);
        verify(kursbetreuerMapper, never()).toDto(any(Kursbetreuer.class));
    }
    
    @Test
    void testErstelleKursbetreuer() {
        // Testdaten vorbereiten
        KursbetreuerDTO neuerKursbetreuerDTO = new KursbetreuerDTO();
        neuerKursbetreuerDTO.setEmail("neu@betreuer.de");
        neuerKursbetreuerDTO.setVorname("Neuer");
        neuerKursbetreuerDTO.setNachname("Betreuer");
        neuerKursbetreuerDTO.setIstRegistriert(true);
        
        Kursbetreuer neuerKursbetreuer = new Kursbetreuer();
        neuerKursbetreuer.setEmail("neu@betreuer.de");
        neuerKursbetreuer.setVorname("Neuer");
        neuerKursbetreuer.setNachname("Betreuer");
        
        KursbetreuerDTO ergebnisDTO = new KursbetreuerDTO();
        ergebnisDTO.setEmail("neu@betreuer.de");
        ergebnisDTO.setVorname("Neuer");
        ergebnisDTO.setNachname("Betreuer");
        ergebnisDTO.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(nutzerRepository.existsByEmail("neu@betreuer.de")).thenReturn(false);
        when(kursbetreuerMapper.toEntity(neuerKursbetreuerDTO)).thenReturn(neuerKursbetreuer);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(kursbetreuerRepository.save(any(Kursbetreuer.class))).thenReturn(neuerKursbetreuer);
        when(kursbetreuerMapper.toDto(neuerKursbetreuer)).thenReturn(ergebnisDTO);
        
        // Spy verwenden, um die Methode zu mocken
        NutzerService spyService = spy(nutzerService);
        doReturn("zufallspasswort123").when(spyService).generiereZufallsPasswort();
        
        // Methode aufrufen
        KursbetreuerDTO ergebnis = spyService.erstelleKursbetreuer(neuerKursbetreuerDTO);
        
        // Überprüfungen
        assertEquals(ergebnisDTO, ergebnis);
        assertEquals("zufallspasswort123", ergebnis.getKlartext_passwort());
        verify(nutzerRepository).existsByEmail("neu@betreuer.de");
        verify(kursbetreuerMapper).toEntity(neuerKursbetreuerDTO);
        verify(passwordEncoder).encode("zufallspasswort123");
        verify(kursbetreuerRepository).save(any(Kursbetreuer.class));
        verify(kursbetreuerMapper).toDto(neuerKursbetreuer);
    }
    
    @Test
    void testErstelleKursbetreuer_EmailExistiertBereits() {
        // Testdaten vorbereiten
        KursbetreuerDTO neuerKursbetreuerDTO = new KursbetreuerDTO();
        neuerKursbetreuerDTO.setEmail("betreuer@test.de");
        neuerKursbetreuerDTO.setVorname("Neuer");
        neuerKursbetreuerDTO.setNachname("Betreuer");
        
        // Mocks konfigurieren
        when(nutzerRepository.existsByEmail("betreuer@test.de")).thenReturn(true);
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.erstelleKursbetreuer(neuerKursbetreuerDTO));
        
        assertEquals("Ein Nutzer mit dieser E-Mail-Adresse existiert bereits", exception.getMessage());
        verify(nutzerRepository).existsByEmail("betreuer@test.de");
        verify(kursbetreuerMapper, never()).toEntity(any(KursbetreuerDTO.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(kursbetreuerRepository, never()).save(any(Kursbetreuer.class));
    }
    
    @Test
    void testAktualisiereStudent() {
        // Testdaten vorbereiten
        StudentDTO aktualisiertStudentDTO = new StudentDTO();
        aktualisiertStudentDTO.setId(1L);
        aktualisiertStudentDTO.setEmail("aktualisiert@student.de");
        aktualisiertStudentDTO.setVorname("Aktualisierter");
        aktualisiertStudentDTO.setNachname("Name");
        aktualisiertStudentDTO.setMatrikelnummer("123456");
        aktualisiertStudentDTO.setIstRegistriert(true);
        
        Student aktualisierterStudent = new Student();
        aktualisierterStudent.setId(1L);
        aktualisierterStudent.setEmail("aktualisiert@student.de");
        aktualisierterStudent.setVorname("Aktualisierter");
        aktualisierterStudent.setNachname("Name");
        aktualisierterStudent.setMatrikelnummer("123456");
        aktualisierterStudent.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(nutzerRepository.findByEmail("aktualisiert@student.de")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenReturn(aktualisierterStudent);
        when(studentMapper.toDto(aktualisierterStudent)).thenReturn(aktualisiertStudentDTO);
        
        // Methode aufrufen
        StudentDTO ergebnis = nutzerService.aktualisiereStudent(1L, aktualisiertStudentDTO);
        
        // Überprüfungen
        assertEquals(aktualisiertStudentDTO, ergebnis);
        verify(studentRepository).findById(1L);
        verify(nutzerRepository).findByEmail("aktualisiert@student.de");
        
        // Capture des gespeicherten Objekts, um Feldwerte zu überprüfen
        verify(studentRepository).save(studentCaptor.capture());
        Student gespeicherterStudent = studentCaptor.getValue();
        
        assertEquals("aktualisiert@student.de", gespeicherterStudent.getEmail());
        assertEquals("Aktualisierter", gespeicherterStudent.getVorname());
        assertEquals("Name", gespeicherterStudent.getNachname());
        assertEquals("123456", gespeicherterStudent.getMatrikelnummer());
        assertTrue(gespeicherterStudent.getIstRegistriert());
        
        verify(studentMapper).toDto(aktualisierterStudent);
    }
    
    @Test
    void testAktualisiereStudent_NichtGefunden() {
        // Testdaten vorbereiten
        StudentDTO aktualisiertStudentDTO = new StudentDTO();
        aktualisiertStudentDTO.setId(99L);
        aktualisiertStudentDTO.setEmail("aktualisiert@student.de");
        
        // Mocks konfigurieren
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.aktualisiereStudent(99L, aktualisiertStudentDTO));
        
        assertEquals("Kein Student mit dieser ID gefunden", exception.getMessage());
        verify(studentRepository).findById(99L);
        verify(studentRepository, never()).save(any(Student.class));
    }
    
    @Test
    void testAktualisiereStudent_EmailExistiertBereits() {
        // Testdaten vorbereiten
        StudentDTO aktualisiertStudentDTO = new StudentDTO();
        aktualisiertStudentDTO.setId(1L);
        aktualisiertStudentDTO.setEmail("betreuer@test.de");
        aktualisiertStudentDTO.setMatrikelnummer("123456");
        
        Nutzer existierenderNutzer = new Kursbetreuer();
        existierenderNutzer.setId(2L);
        existierenderNutzer.setEmail("betreuer@test.de");
        
        // Mocks konfigurieren
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(nutzerRepository.findByEmail("betreuer@test.de")).thenReturn(Optional.of(existierenderNutzer));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.aktualisiereStudent(1L, aktualisiertStudentDTO));
        
        assertEquals("Ein anderer Nutzer mit dieser E-Mail-Adresse existiert bereits", exception.getMessage());
        verify(studentRepository).findById(1L);
        verify(nutzerRepository).findByEmail("betreuer@test.de");
        verify(studentRepository, never()).save(any(Student.class));
    }
    
    @Test
    void testAktualisiereStudent_MatrikelnummerGeaendert() {
        // Testdaten vorbereiten
        StudentDTO aktualisiertStudentDTO = new StudentDTO();
        aktualisiertStudentDTO.setId(1L);
        aktualisiertStudentDTO.setEmail("aktualisiert@student.de");
        aktualisiertStudentDTO.setMatrikelnummer("654321"); // Versuche, die Matrikelnummer zu ändern
        
        // Mocks konfigurieren
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.aktualisiereStudent(1L, aktualisiertStudentDTO));
        
        assertEquals("Die Matrikelnummer darf nicht geändert werden", exception.getMessage());
        verify(studentRepository).findById(1L);
        verify(studentRepository, never()).save(any(Student.class));
    }
    
    @Test
    void testAktualisiereKursbetreuer() {
        // Testdaten vorbereiten
        KursbetreuerDTO aktualisiertKursbetreuerDTO = new KursbetreuerDTO();
        aktualisiertKursbetreuerDTO.setId(2L);
        aktualisiertKursbetreuerDTO.setEmail("aktualisiert@betreuer.de");
        aktualisiertKursbetreuerDTO.setVorname("Aktualisierter");
        aktualisiertKursbetreuerDTO.setNachname("Betreuer");
        aktualisiertKursbetreuerDTO.setIstRegistriert(true);
        
        Kursbetreuer aktualisierterKursbetreuer = new Kursbetreuer();
        aktualisierterKursbetreuer.setId(2L);
        aktualisierterKursbetreuer.setEmail("aktualisiert@betreuer.de");
        aktualisierterKursbetreuer.setVorname("Aktualisierter");
        aktualisierterKursbetreuer.setNachname("Betreuer");
        aktualisierterKursbetreuer.setIstRegistriert(true);
        
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(2L)).thenReturn(Optional.of(testKursbetreuer));
        when(nutzerRepository.findByEmail("aktualisiert@betreuer.de")).thenReturn(Optional.empty());
        when(kursbetreuerRepository.save(any(Kursbetreuer.class))).thenReturn(aktualisierterKursbetreuer);
        when(kursbetreuerMapper.toDto(aktualisierterKursbetreuer)).thenReturn(aktualisiertKursbetreuerDTO);
        
        // Methode aufrufen
        KursbetreuerDTO ergebnis = nutzerService.aktualisiereKursbetreuer(2L, aktualisiertKursbetreuerDTO);
        
        // Überprüfungen
        assertEquals(aktualisiertKursbetreuerDTO, ergebnis);
        verify(kursbetreuerRepository).findById(2L);
        verify(nutzerRepository).findByEmail("aktualisiert@betreuer.de");
        
        // Capture des gespeicherten Objekts, um Feldwerte zu überprüfen
        verify(kursbetreuerRepository).save(kursbetreuerCaptor.capture());
        Kursbetreuer gespeicherterKursbetreuer = kursbetreuerCaptor.getValue();
        
        assertEquals("aktualisiert@betreuer.de", gespeicherterKursbetreuer.getEmail());
        assertEquals("Aktualisierter", gespeicherterKursbetreuer.getVorname());
        assertEquals("Betreuer", gespeicherterKursbetreuer.getNachname());
        assertTrue(gespeicherterKursbetreuer.getIstRegistriert());
        
        verify(kursbetreuerMapper).toDto(aktualisierterKursbetreuer);
    }
    
    @Test
    void testAktualisiereKursbetreuer_NichtGefunden() {
        // Testdaten vorbereiten
        KursbetreuerDTO aktualisiertKursbetreuerDTO = new KursbetreuerDTO();
        aktualisiertKursbetreuerDTO.setId(99L);
        aktualisiertKursbetreuerDTO.setEmail("aktualisiert@betreuer.de");
        
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.aktualisiereKursbetreuer(99L, aktualisiertKursbetreuerDTO));
        
        assertEquals("Kein Kursbetreuer mit dieser ID gefunden", exception.getMessage());
        verify(kursbetreuerRepository).findById(99L);
        verify(kursbetreuerRepository, never()).save(any(Kursbetreuer.class));
    }
    
    @Test
    void testAktualisiereKursbetreuer_EmailExistiertBereits() {
        // Testdaten vorbereiten
        KursbetreuerDTO aktualisiertKursbetreuerDTO = new KursbetreuerDTO();
        aktualisiertKursbetreuerDTO.setId(2L);
        aktualisiertKursbetreuerDTO.setEmail("student@test.de");
        
        Nutzer existierenderNutzer = new Student();
        existierenderNutzer.setId(1L);
        existierenderNutzer.setEmail("student@test.de");
        
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(2L)).thenReturn(Optional.of(testKursbetreuer));
        when(nutzerRepository.findByEmail("student@test.de")).thenReturn(Optional.of(existierenderNutzer));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.aktualisiereKursbetreuer(2L, aktualisiertKursbetreuerDTO));
        
        assertEquals("Ein anderer Nutzer mit dieser E-Mail-Adresse existiert bereits", exception.getMessage());
        verify(kursbetreuerRepository).findById(2L);
        verify(nutzerRepository).findByEmail("student@test.de");
        verify(kursbetreuerRepository, never()).save(any(Kursbetreuer.class));
    }
    
    @Test
    void testLoescheStudent() {
        // Mocks konfigurieren
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        
        // Methode aufrufen
        nutzerService.loescheStudent(1L);
        
        // Überprüfungen
        verify(studentRepository).findById(1L);
        verify(studentRepository).delete(testStudent);
    }
    
    @Test
    void testLoescheStudent_NichtGefunden() {
        // Mocks konfigurieren
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.loescheStudent(99L));
        
        assertEquals("Kein Student mit dieser ID gefunden", exception.getMessage());
        verify(studentRepository).findById(99L);
        verify(studentRepository, never()).delete(any(Student.class));
    }
    
    @Test
    void testLoescheKursbetreuer() {
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(2L)).thenReturn(Optional.of(testKursbetreuer));
        
        // Methode aufrufen
        nutzerService.loescheKursbetreuer(2L);
        
        // Überprüfungen
        verify(kursbetreuerRepository).findById(2L);
        verify(kursbetreuerRepository).delete(testKursbetreuer);
    }
    
    @Test
    void testLoescheKursbetreuer_NichtGefunden() {
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.loescheKursbetreuer(99L));
        
        assertEquals("Kein Kursbetreuer mit dieser ID gefunden", exception.getMessage());
        verify(kursbetreuerRepository).findById(99L);
        verify(kursbetreuerRepository, never()).delete(any(Kursbetreuer.class));
    }
    
    @Test
    void testGeneriereZufallsPasswort() {
        // Methode aufrufen
        String passwort = nutzerService.generiereZufallsPasswort();
        
        // Überprüfungen
        assertNotNull(passwort);
        assertEquals(12, passwort.length()); // Standardlänge ist 12 Zeichen
    }
    
    @Test
    void testSetzePasswortZurueck() {
        // Mocks konfigurieren
        when(nutzerRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(passwordEncoder.encode(anyString())).thenReturn("neues-encoded-password");
        
        // Spy verwenden, um die Methode zu mocken
        NutzerService spyService = spy(nutzerService);
        doReturn("neuesPasswort123").when(spyService).generiereZufallsPasswort();
        
        // Methode aufrufen
        String neuesPasswort = spyService.setzePasswortZurueck(1L);
        
        // Überprüfungen
        assertEquals("neuesPasswort123", neuesPasswort);
        verify(nutzerRepository).findById(1L);
        verify(passwordEncoder).encode("neuesPasswort123");
        
        // Prüfen, dass das Passwort des Nutzers aktualisiert wurde
        verify(nutzerRepository).save(testStudent);
        assertEquals("neues-encoded-password", testStudent.getPasswort());
    }
    
    @Test
    void testAktualisiereStudent_IstRegistriertGeaendert() {
        // Testdaten vorbereiten
        StudentDTO aktualisiertStudentDTO = new StudentDTO();
        aktualisiertStudentDTO.setId(1L);
        aktualisiertStudentDTO.setEmail("aktualisiert@student.de");
        aktualisiertStudentDTO.setVorname("Aktualisierter");
        aktualisiertStudentDTO.setNachname("Name");
        aktualisiertStudentDTO.setMatrikelnummer("123456");
        aktualisiertStudentDTO.setIstRegistriert(false); // Versuche, istRegistriert zu ändern (von true zu false)
        
        // Mocks konfigurieren
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.aktualisiereStudent(1L, aktualisiertStudentDTO));
        
        assertEquals("Registrier-Status darf nicht geändert werden", exception.getMessage());
        verify(studentRepository).findById(1L);
        verify(studentRepository, never()).save(any(Student.class));
    }
    
    @Test
    void testAktualisiereKursbetreuer_IstRegistriertGeaendert() {
        // Testdaten vorbereiten
        KursbetreuerDTO aktualisiertKursbetreuerDTO = new KursbetreuerDTO();
        aktualisiertKursbetreuerDTO.setId(2L);
        aktualisiertKursbetreuerDTO.setEmail("aktualisiert@betreuer.de");
        aktualisiertKursbetreuerDTO.setVorname("Aktualisierter");
        aktualisiertKursbetreuerDTO.setNachname("Betreuer");
        aktualisiertKursbetreuerDTO.setIstRegistriert(false); // Versuche, istRegistriert zu ändern (von true zu false)
        
        // Mocks konfigurieren
        when(kursbetreuerRepository.findById(2L)).thenReturn(Optional.of(testKursbetreuer));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            nutzerService.aktualisiereKursbetreuer(2L, aktualisiertKursbetreuerDTO));
        
        assertEquals("Registrier-Status darf nicht geändert werden", exception.getMessage());
        verify(kursbetreuerRepository).findById(2L);
        verify(kursbetreuerRepository, never()).save(any(Kursbetreuer.class));
    }
    
    @Test
    void testAenderePasswort_Erfolgreich() {
        // Testdaten vorbereiten
        PasswortAenderungDTO passwortAenderungDTO = new PasswortAenderungDTO();
        passwortAenderungDTO.setAktuellesPasswort("encodedPassword123");
        passwortAenderungDTO.setNeuesPasswort("neuesPasswort456");
        passwortAenderungDTO.setPasswortBestaetigung("neuesPasswort456");

        // Mocks konfigurieren
        when(passwordEncoder.matches("encodedPassword123", "encodedPassword123")).thenReturn(true);
        when(passwordEncoder.encode("neuesPasswort456")).thenReturn("encodedNeuesPasswort456");
        when(nutzerRepository.save(any(Nutzer.class))).thenReturn(testStudent);
        when(nutzerRepository.findByEmail("student@test.de")).thenReturn(Optional.of(testStudent));
        when(nutzerRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(nutzerMapper.toDto(testStudent)).thenReturn(new NutzerDTO());

        // Methode aufrufen
        NutzerDTO ergebnis = nutzerService.aenderePasswort(testStudentDTO, passwortAenderungDTO);

        // Überprüfungen
        assertNotNull(ergebnis);
        verify(passwordEncoder).matches("encodedPassword123", "encodedPassword123");
        verify(passwordEncoder).encode("neuesPasswort456");
        verify(nutzerRepository).save(testStudent);
        verify(nutzerMapper).toDto(testStudent);
    }

    @Test
    void testAenderePasswort_FalschesAktuellesPasswort() {
        // Testdaten vorbereiten
        PasswortAenderungDTO passwortAenderungDTO = new PasswortAenderungDTO();
        passwortAenderungDTO.setAktuellesPasswort("falschesPasswort");
        passwortAenderungDTO.setNeuesPasswort("neuesPasswort456");
        passwortAenderungDTO.setPasswortBestaetigung("neuesPasswort456");
        
        // Mocks konfigurieren
        when(passwordEncoder.matches("falschesPasswort", testStudent.getPasswort())).thenReturn(false);
        when(nutzerRepository.findByEmail("student@test.de")).thenReturn(Optional.of(testStudent));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            nutzerService.aenderePasswort(testStudentDTO, passwortAenderungDTO));

        assertEquals("Das aktuelle Passwort ist nicht korrekt", exception.getMessage());
        verify(passwordEncoder).matches("falschesPasswort", testStudent.getPasswort());
        verify(passwordEncoder, never()).encode(anyString());
        verify(nutzerRepository, never()).save(any(Nutzer.class));
    }

    @Test
    void testAenderePasswort_LeeresNeuesPasswort() {
        // Testdaten vorbereiten
        PasswortAenderungDTO passwortAenderungDTO = new PasswortAenderungDTO();
        passwortAenderungDTO.setAktuellesPasswort("encodedPassword123");
        passwortAenderungDTO.setNeuesPasswort("");
        passwortAenderungDTO.setPasswortBestaetigung("");
        
        // Mocks konfigurieren
        when(passwordEncoder.matches("encodedPassword123", testStudent.getPasswort())).thenReturn(true);
        when(nutzerRepository.findByEmail("student@test.de")).thenReturn(Optional.of(testStudent));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            nutzerService.aenderePasswort(testStudentDTO, passwortAenderungDTO));
        
        assertEquals("Das neue Passwort darf nicht leer sein", exception.getMessage());
        verify(passwordEncoder).matches("encodedPassword123", testStudent.getPasswort());
        verify(passwordEncoder, never()).encode(anyString());
        verify(nutzerRepository, never()).save(any(Nutzer.class));
    }
    
    @Test
    void testAenderePasswort_ZuKurzesNeuesPasswort() {
        // Testdaten vorbereiten
        PasswortAenderungDTO passwortAenderungDTO = new PasswortAenderungDTO();
        passwortAenderungDTO.setAktuellesPasswort("encodedPassword123");
        passwortAenderungDTO.setNeuesPasswort("kurz");  // Weniger als 8 Zeichen
        passwortAenderungDTO.setPasswortBestaetigung("kurz");
        
        // Mocks konfigurieren
        when(passwordEncoder.matches("encodedPassword123", testStudent.getPasswort())).thenReturn(true);
        when(nutzerRepository.findByEmail("student@test.de")).thenReturn(Optional.of(testStudent));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            nutzerService.aenderePasswort(testStudentDTO, passwortAenderungDTO));
        
        assertEquals("Das neue Passwort muss mindestens 8 Zeichen lang sein", exception.getMessage());
        verify(passwordEncoder).matches("encodedPassword123", testStudent.getPasswort());
        verify(passwordEncoder, never()).encode(anyString());
        verify(nutzerRepository, never()).save(any(Nutzer.class));
    }
    
    @Test
    void testAenderePasswort_PasswoerterStimmenNichtUeberein() {
        // Testdaten vorbereiten
        PasswortAenderungDTO passwortAenderungDTO = new PasswortAenderungDTO();
        passwortAenderungDTO.setAktuellesPasswort("encodedPassword123");
        passwortAenderungDTO.setNeuesPasswort("neuesPasswort456");
        passwortAenderungDTO.setPasswortBestaetigung("anderePasswortBestaetigung");
        
        // Mocks konfigurieren
        when(passwordEncoder.matches("encodedPassword123", testStudent.getPasswort())).thenReturn(true);
        when(nutzerRepository.findByEmail("student@test.de")).thenReturn(Optional.of(testStudent));
        
        // Überprüfen, dass Exception geworfen wird
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            nutzerService.aenderePasswort(testStudentDTO, passwortAenderungDTO));
        
        assertEquals("Das neue Passwort und die Bestätigung stimmen nicht überein", exception.getMessage());
        verify(passwordEncoder).matches("encodedPassword123", testStudent.getPasswort());
        verify(passwordEncoder, never()).encode(anyString());
        verify(nutzerRepository, never()).save(any(Nutzer.class));
    }
}