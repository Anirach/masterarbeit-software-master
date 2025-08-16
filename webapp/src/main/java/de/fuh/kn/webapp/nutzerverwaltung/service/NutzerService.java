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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * Service für die Verwaltung von Nutzern.
 * Stellt Funktionen für die Registrierung und Verwaltung von Nutzern bereit.
 */
@Service
public class NutzerService {

    private final NutzerRepository nutzerRepository;
    private final StudentRepository studentRepository;
    private final KursbetreuerRepository kursbetreuerRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfilMapper profilMapper;
    private final StudentMapper studentMapper;
    private final KursbetreuerMapper kursbetreuerMapper;
    private final NutzerMapper nutzerMapper;
    
    private final SecureRandom random = new SecureRandom();
    private static final String PASSWORT_ZEICHEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_-+=<>?";
    private static final int PASSWORT_LAENGE = 12;

    /**
     * Konstruktor mit Dependency Injection der erforderlichen Repositories und des PasswordEncoders.
     *
     * @param nutzerRepository Das Repository für den Zugriff auf Nutzer-Daten.
     * @param studentRepository Das Repository für den Zugriff auf Student-Daten.
     * @param kursbetreuerRepository Das Repository für den Zugriff auf Kursbetreuer-Daten.
     * @param passwordEncoder Der PasswordEncoder für die sichere Speicherung von Passwörtern.
     * @param profilMapper Der Mapper für die Konvertierung von Profildaten.
     * @param studentMapper Der Mapper für die Konvertierung von Student-Entitäten zu DTOs.
     * @param kursbetreuerMapper Der Mapper für die Konvertierung von Kursbetreuer-Entitäten zu DTOs.
     */
    public NutzerService(
            NutzerRepository nutzerRepository,
            StudentRepository studentRepository,
            KursbetreuerRepository kursbetreuerRepository,
            PasswordEncoder passwordEncoder,
            ProfilMapper profilMapper,
            StudentMapper studentMapper,
            KursbetreuerMapper kursbetreuerMapper, NutzerMapper nutzerMapper) {
        this.nutzerRepository = nutzerRepository;
        this.studentRepository = studentRepository;
        this.kursbetreuerRepository = kursbetreuerRepository;
        this.passwordEncoder = passwordEncoder;
        this.profilMapper = profilMapper;
        this.studentMapper = studentMapper;
        this.kursbetreuerMapper = kursbetreuerMapper;
        this.nutzerMapper = nutzerMapper;
    }

    /**
     * Registriert einen Studenten im System.
     * Falls bereits ein Dummy-Student mit der angegebenen Matrikelnummer existiert,
     * wird dieser mit den neuen Daten aktualisiert, und die Belegungen bleiben erhalten.
     * Die Erstellung neuer Student-Objekte ist nicht mehr möglich.
     *
     * @param registrierungDTO Die Registrierungsdaten des Studenten.
     * @return Der registrierte Student.
     * @throws IllegalArgumentException wenn ein vollständig registrierter Nutzer mit der E-Mail-Adresse bereits existiert,
     *                                  kein Dummy-Student mit der angegebenen Matrikelnummer existiert,
     *                                  oder das Passwort leer oder kürzer als 8 Zeichen ist.
     */
    @Transactional
    public StudentDTO registriereStudent(RegistrierungDTO registrierungDTO) {
        // Prüfen, ob ein Nutzer mit dieser E-Mail bereits existiert und kein Dummy ist
        Optional<Nutzer> existingUser = nutzerRepository.findByEmail(registrierungDTO.getEmail());
        if (existingUser.isPresent() && existingUser.get().getIstRegistriert()) {
            throw new IllegalArgumentException("Ein Nutzer mit dieser E-Mail-Adresse existiert bereits");
        }

        // Prüfen, ob das Passwort die Mindestanforderungen erfüllt
        if (registrierungDTO.getPasswort() == null || registrierungDTO.getPasswort().isEmpty()) {
            throw new IllegalArgumentException("Das Passwort darf nicht leer sein");
        }
        
        if (registrierungDTO.getPasswort().length() < 8) {
            throw new IllegalArgumentException("Das Passwort muss mindestens 8 Zeichen lang sein");
        }

        // Prüfen, ob ein Dummy-Student mit dieser Matrikelnummer existiert
        Optional<Student> existingStudent = studentRepository.findByMatrikelnummer(registrierungDTO.getMatrikelnummer());
        
        if (existingStudent.isPresent() && !existingStudent.get().getIstRegistriert()) {
            // Wenn ein Dummy-Student gefunden wurde, aktualisieren wir diesen
            Student dummyStudent = existingStudent.get();
            dummyStudent.setEmail(registrierungDTO.getEmail());
            dummyStudent.setVorname(registrierungDTO.getVorname());
            dummyStudent.setNachname(registrierungDTO.getNachname());
            dummyStudent.setPasswort(passwordEncoder.encode(registrierungDTO.getPasswort()));
            dummyStudent.setIstRegistriert(true);

            Student savedStudent = studentRepository.save(dummyStudent);
            return studentMapper.toDto(savedStudent);
        } else {
            // Wenn kein Dummy-Student gefunden wurde oder der Student bereits registriert ist
            throw new IllegalArgumentException("Es existiert kein nicht-registrierter Student mit dieser Matrikelnummer im System");
        }
    }

    /**
     * Findet einen Nutzer anhand seiner E-Mail-Adresse.
     *
     * @param email Die E-Mail-Adresse des Nutzers.
     * @return Ein Optional mit dem gefundenen Nutzer oder ein leeres Optional, wenn kein Nutzer gefunden wurde.
     */
    @Transactional(readOnly = true)
    public Optional<NutzerDTO> findeNutzerNachEmail(String email) {
        return nutzerRepository.findByEmail(email)
                .map(nutzer -> {
                    if(nutzer instanceof Student student) {
                        return studentMapper.toDto(student);
                    }else if(nutzer instanceof Kursbetreuer kursbetreuer) {
                        return kursbetreuerMapper.toDto(kursbetreuer);
                    }else{
                        return nutzerMapper.toDto(nutzer);
                    }
                });
    }
    
    /**
     * Findet das verschlüsselte Passwort eines Nutzers anhand seiner E-Mail-Adresse.
     *
     * @param email Die E-Mail-Adresse des Nutzers.
     * @return Ein Optional mit dem verschlüsselten Passwort oder ein leeres Optional, wenn kein Nutzer gefunden wurde.
     */
    public Optional<String> findePasswortNachEmail(String email) {
        return nutzerRepository.findByEmail(email)
                .map(Nutzer::getPasswort);
    }

    /**
     * Findet einen Studenten anhand seiner ID.
     *
     * @param id Die ID des Studenten.
     * @return Ein Optional mit dem gefundenen Studenten oder ein leeres Optional, wenn kein Student gefunden wurde.
     */
    private Optional<Student> findeStudentNachId(Long id) {
        return studentRepository.findById(id);
    }
    
    /**
     * Findet einen Kursbetreuer anhand seiner ID.
     *
     * @param id Die ID des Kursbetreuers.
     * @return Ein Optional mit dem gefundenen Kursbetreuer oder ein leeres Optional, wenn kein Kursbetreuer gefunden wurde.
     */
    private Optional<Kursbetreuer> findeKursbetreuerNachId(Long id) {
        return kursbetreuerRepository.findById(id);
    }
    
    /**
     * Konvertiert ein UserDetails-Objekt in die entsprechende Entität.
     *
     * @param userDetails Das UserDetails-Objekt
     * @return Die entsprechende Nutzer-Entität oder null, wenn kein passender Nutzer gefunden wurde
     */
    private Nutzer konvertiereUserDetailsZuEntity(UserDetails userDetails) {
        if (userDetails instanceof StudentUserDetails studentDetails) {
            return findeStudentNachId(studentDetails.getId()).orElse(null);
        } else if (userDetails instanceof KursbetreuerUserDetails kursbetreuerDetails) {
            return findeKursbetreuerNachId(kursbetreuerDetails.getId()).orElse(null);
        }
        return null;
    }

    /**
     * Prüft, ob ein Dummy-Student mit der angegebenen Matrikelnummer existiert.
     * Ein Dummy-Student ist ein Student, der noch nicht registriert ist.
     *
     * @param matrikelnummer Die zu prüfende Matrikelnummer.
     * @return true, wenn ein nicht-registrierter Student mit dieser Matrikelnummer existiert, sonst false.
     */
    public boolean existiertDummyStudentMitMatrikelnummer(String matrikelnummer) {
        Optional<Student> student = studentRepository.findByMatrikelnummer(matrikelnummer);
        return student.isPresent() && !student.get().getIstRegistriert();
    }
    
    /**
     * Prüft, ob bereits ein registrierter Student mit der angegebenen Matrikelnummer existiert.
     *
     * @param matrikelnummer Die zu prüfende Matrikelnummer.
     * @return true, wenn ein registrierter Student mit dieser Matrikelnummer existiert, sonst false.
     */
    public boolean existiertRegistrierterStudentMitMatrikelnummer(String matrikelnummer) {
        Optional<Student> student = studentRepository.findByMatrikelnummer(matrikelnummer);
        return student.isPresent() && student.get().getIstRegistriert();
    }

    /**
     * Erstellt einen Dummy-Studenten mit der angegebenen Matrikelnummer
     * Der Dummy-Student hat nur die Matrikelnummer und ein verschlüsseltes Dummy-Passwort.
     * Email, Vorname und Nachname bleiben null, da diese jetzt optional sind.
     * Das Flag istRegistriert wird auf false gesetzt.
     *
     * @param matrikelnummer Die Matrikelnummer des zu erstellenden Dummy-Studenten
     * @return Der erstellte Student
     */
    @Transactional
    public StudentDTO erstelleDummyStudent(String matrikelnummer) {
        // Prüfen ob Student mit dieser Matrikelnummer bereits existiert
        Optional<Student> existingStudent = studentRepository.findByMatrikelnummer(matrikelnummer);
        if (existingStudent.isPresent()) {
            throw new IllegalArgumentException("Student mit Matrikelnummer "+matrikelnummer+" existiert bereits");
        }
        
        // Dummy-Student erstellen
        Student dummyStudent = new Student();
        dummyStudent.setMatrikelnummer(matrikelnummer);
        
        // Nur die minimal notwendigen Felder setzen
        // E-Mail, Vorname und Nachname bleiben null
        dummyStudent.setPasswort(passwordEncoder.encode("dummy-password"));
        dummyStudent.setIstRegistriert(false);

        Student savedStudent = studentRepository.save(dummyStudent);
        return studentMapper.toDto(savedStudent);
    }

    /**
     * Aktualisiert die Profildaten eines Nutzers.
     * Prüft, ob die neue E-Mail-Adresse bereits von einem anderen Nutzer verwendet wird.
     *
     * @param nutzerDTO Der Nutzer, dessen Profil aktualisiert werden soll
     * @param profilAenderungDTO Die neuen Profildaten
     * @return Der aktualisierte Nutzer
     * @throws IllegalArgumentException wenn ein anderer Nutzer mit der neuen E-Mail-Adresse bereits existiert
     */
    @Transactional
    public NutzerDTO aktualisiereNutzerProfil(NutzerDTO nutzerDTO, ProfilAenderungDTO profilAenderungDTO) {
        // Prüfen, ob ein anderer Nutzer mit der neuen E-Mail-Adresse bereits existiert
        if (!profilAenderungDTO.getEmail().equals(nutzerDTO.getEmail())) {
            Optional<Nutzer> nutzerMitEmail = nutzerRepository.findByEmail(profilAenderungDTO.getEmail());
            if (nutzerMitEmail.isPresent() && !nutzerMitEmail.get().getId().equals(nutzerDTO.getId())) {
                throw new IllegalArgumentException("Ein anderer Nutzer mit dieser E-Mail-Adresse existiert bereits");
            }
        }

        Optional<Nutzer> optionalNutzer = nutzerRepository.findById(nutzerDTO.getId());
        if(optionalNutzer.isEmpty()) {
            throw new IllegalArgumentException("Nutzer "+nutzerDTO.getId()+" nicht gefunden");
        }

        Nutzer nutzer = optionalNutzer.get();

        // Nutzer-Profil aktualisieren
        profilMapper.updateNutzerFromDto(profilAenderungDTO, nutzer);
        profilMapper.updateNutzerFromDto(profilAenderungDTO, nutzerDTO);


        Nutzer savedNutzer = nutzerRepository.save(nutzer);
        return nutzerMapper.toDto(savedNutzer);
    }

    /**
     * Ändert das Passwort eines Nutzers.
     * Prüft, ob das aktuelle Passwort korrekt ist und ob das neue Passwort die Anforderungen erfüllt.
     *
     * @param nutzerDTO Der Nutzer, dessen Passwort geändert werden soll
     * @param passwortAenderungDTO Die Daten für die Passwortänderung
     * @return Der Nutzer mit aktualisiertem Passwort
     * @throws IllegalArgumentException wenn das aktuelle Passwort falsch ist,
     *                                  das neue Passwort leer oder zu kurz ist,
     *                                  oder das neue Passwort und die Bestätigung nicht übereinstimmen
     */
    @Transactional
    public NutzerDTO aenderePasswort(NutzerDTO nutzerDTO, PasswortAenderungDTO passwortAenderungDTO) {

        String password = this.findePasswortNachEmail(nutzerDTO.getEmail()).orElseThrow(() -> new IllegalArgumentException("Nutzer nicht gefunden"));

        // Prüfen, ob das aktuelle Passwort korrekt ist
        if (!passwordEncoder.matches(passwortAenderungDTO.getAktuellesPasswort(), password)) {
            throw new IllegalArgumentException("Das aktuelle Passwort ist nicht korrekt");
        }
        
        // Prüfen, ob das neue Passwort die Anforderungen erfüllt
        if (passwortAenderungDTO.getNeuesPasswort() == null || passwortAenderungDTO.getNeuesPasswort().isEmpty()) {
            throw new IllegalArgumentException("Das neue Passwort darf nicht leer sein");
        }
        
        if (passwortAenderungDTO.getNeuesPasswort().length() < 8) {
            throw new IllegalArgumentException("Das neue Passwort muss mindestens 8 Zeichen lang sein");
        }
        
        // Prüfen, ob das neue Passwort und die Bestätigung übereinstimmen
        if (!passwortAenderungDTO.getNeuesPasswort().equals(passwortAenderungDTO.getPasswortBestaetigung())) {
            throw new IllegalArgumentException("Das neue Passwort und die Bestätigung stimmen nicht überein");
        }
        
        // Passwort aktualisieren
        Nutzer nutzer = nutzerRepository.findById(nutzerDTO.getId()).orElseThrow(() -> new IllegalArgumentException("Nutzer existiert nicht"));
        nutzer.setPasswort(passwordEncoder.encode(passwortAenderungDTO.getNeuesPasswort()));
        Nutzer savedNutzer = nutzerRepository.save(nutzer);
        return nutzerMapper.toDto(savedNutzer);
    }

    /**
     * Gibt eine Liste aller Studenten zurück.
     *
     * @return Eine Liste aller Studenten als DTOs
     */
    public List<StudentDTO> getAlleStudenten() {
        List<Student> studenten = studentRepository.findAllByOrderByNachnameAscVornameAsc();
        return studentMapper.toDtoList(studenten);
    }
    
    /**
     * Gibt eine Liste aller Kursbetreuer zurück.
     *
     * @return Eine Liste aller Kursbetreuer als DTOs
     */
    public List<KursbetreuerDTO> getAlleKursbetreuer() {
        List<Kursbetreuer> kursbetreuer = kursbetreuerRepository.findAllByOrderByNachnameAscVornameAsc();
        return kursbetreuerMapper.toDtoList(kursbetreuer);
    }
    
    /**
     * Gibt einen Studenten anhand seiner ID zurück.
     *
     * @param id Die ID des Studenten
     * @return Ein Optional mit dem StudentDTO oder ein leeres Optional, wenn kein Student gefunden wurde
     */
    public Optional<StudentDTO> getStudentById(Long id) {
        return studentRepository.findById(id)
                .map(studentMapper::toDto);
    }

    /**
     * Gibt einen Studenten anhand seiner Matrikelnummer zurück
     *
     * @param matrikelnummer Matrikelnummer des Studenten
     * @return Ein Optional mit dem StudentDTO oder ein leeres Optional, wenn kein Student gefunden wurde
     */
    public Optional<StudentDTO> getStudentByMatrikelnummer(String matrikelnummer) {
        return studentRepository.findByMatrikelnummer(matrikelnummer)
                .map(studentMapper::toDto);
    }

    /**
     * Gibt einen Kursbetreuer anhand seiner ID zurück.
     *
     * @param id Die ID des Kursbetreuers
     * @return Ein Optional mit dem KursbetreuerDTO oder ein leeres Optional, wenn kein Kursbetreuer gefunden wurde
     */
    public Optional<KursbetreuerDTO> getKursbetreuerById(Long id) {
        return kursbetreuerRepository.findById(id)
                .map(kursbetreuerMapper::toDto);
    }

    /**
     * Gibt einen Nutzer anhand seiner ID zurück.
     *
     * @param id Die ID des Kursbetreuers
     * @return Ein Optional mit dem KursbetreuerDTO oder ein leeres Optional, wenn kein Kursbetreuer gefunden wurde
     */
    public Optional<NutzerDTO> getNutzerById(Long id) {
        return nutzerRepository.findById(id)
                .map(nutzerMapper::toDto);
    }

    /**
     * Erstellt einen neuen Kursbetreuer.
     *
     * @param kursbetreuerDTO Die Daten des zu erstellenden Kursbetreuers
     * @return Der erstellte Kursbetreuer als DTO
     * @throws IllegalArgumentException wenn ein Nutzer mit der angegebenen E-Mail-Adresse bereits existiert
     */
    @Transactional
    public KursbetreuerDTO erstelleKursbetreuer(KursbetreuerDTO kursbetreuerDTO) {
        // Prüfen, ob ein Nutzer mit dieser E-Mail bereits existiert
        if (nutzerRepository.existsByEmail(kursbetreuerDTO.getEmail())) {
            throw new IllegalArgumentException("Ein Nutzer mit dieser E-Mail-Adresse existiert bereits");
        }
        
        // Kursbetreuer erstellen
        Kursbetreuer kursbetreuer = kursbetreuerMapper.toEntity(kursbetreuerDTO);
        
        // Zufälliges Passwort generieren
        String passwort = generiereZufallsPasswort();
        kursbetreuer.setPasswort(passwordEncoder.encode(passwort));
        kursbetreuer.setIstRegistriert(true);
        
        Kursbetreuer gespeicherterKursbetreuer = kursbetreuerRepository.save(kursbetreuer);
        KursbetreuerDTO result = kursbetreuerMapper.toDto(gespeicherterKursbetreuer);
        
        // Temporäres Feld für generiertes Passwort (nur für die Anzeige)
        result.setKlartext_passwort(passwort);
        
        return result;
    }
    
    /**
     * Generiert ein zufälliges Passwort.
     * 
     * @return Ein zufällig generiertes Passwort
     */
    public String generiereZufallsPasswort() {
        StringBuilder passwort = new StringBuilder(PASSWORT_LAENGE);
        for (int i = 0; i < PASSWORT_LAENGE; i++) {
            int index = random.nextInt(PASSWORT_ZEICHEN.length());
            passwort.append(PASSWORT_ZEICHEN.charAt(index));
        }
        return passwort.toString();
    }
    
    /**
     * Setzt das Passwort eines Nutzers zurück und gibt das neue Passwort zurück.
     * 
     * @param nutzerId Die ID des Nutzers
     * @return Das neue generierte Passwort
     * @throws IllegalArgumentException wenn kein Nutzer mit dieser ID gefunden wurde
     */
    @Transactional
    public String setzePasswortZurueck(Long nutzerId) {
        Nutzer nutzer = nutzerRepository.findById(nutzerId)
                .orElseThrow(() -> new IllegalArgumentException("Kein Nutzer mit dieser ID gefunden"));
        
        String neuesPasswort = generiereZufallsPasswort();
        nutzer.setPasswort(passwordEncoder.encode(neuesPasswort));
        
        nutzerRepository.save(nutzer);
        
        return neuesPasswort;
    }
    
    /**
     * Aktualisiert die Daten eines Studenten.
     * Die Matrikelnummer darf nicht geändert werden.
     *
     * @param id Die ID des zu aktualisierenden Studenten
     * @param studentDTO Die neuen Daten des Studenten
     * @return Der aktualisierte Student als DTO
     * @throws IllegalArgumentException wenn kein Student mit der angegebenen ID existiert
     *                                 oder ein anderer Nutzer mit der neuen E-Mail-Adresse bereits existiert
     *                                 oder versucht wird, die Matrikelnummer zu ändern
     */
    @Transactional
    public StudentDTO aktualisiereStudent(Long id, StudentDTO studentDTO) {
        // Prüfen, ob ein Student mit dieser ID existiert
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kein Student mit dieser ID gefunden"));
        
        // Prüfen, ob ein anderer Nutzer mit der neuen E-Mail-Adresse bereits existiert
        if (studentDTO.getEmail() != null && !studentDTO.getEmail().equals(student.getEmail())) {
            Optional<Nutzer> nutzerMitEmail = nutzerRepository.findByEmail(studentDTO.getEmail());
            if (nutzerMitEmail.isPresent() && !nutzerMitEmail.get().getId().equals(id)) {
                throw new IllegalArgumentException("Ein anderer Nutzer mit dieser E-Mail-Adresse existiert bereits");
            }
        }
        
        // Die Matrikelnummer darf nicht geändert werden
        if (!student.getMatrikelnummer().equals(studentDTO.getMatrikelnummer())) {
            throw new IllegalArgumentException("Die Matrikelnummer darf nicht geändert werden");
        }

        if(!student.getIstRegistriert().equals(studentDTO.getIstRegistriert())) {
            throw new IllegalArgumentException("Registrier-Status darf nicht geändert werden");
        }
        
        // Student aktualisieren
        student.setEmail(studentDTO.getEmail());
        student.setVorname(studentDTO.getVorname());
        student.setNachname(studentDTO.getNachname());
        student.setIstRegistriert(studentDTO.getIstRegistriert());
        
        Student gespeicherterStudent = studentRepository.save(student);
        return studentMapper.toDto(gespeicherterStudent);
    }
    
    /**
     * Aktualisiert die Daten eines Kursbetreuers.
     *
     * @param id Die ID des zu aktualisierenden Kursbetreuers
     * @param kursbetreuerDTO Die neuen Daten des Kursbetreuers
     * @return Der aktualisierte Kursbetreuer als DTO
     * @throws IllegalArgumentException wenn kein Kursbetreuer mit der angegebenen ID existiert
     *                                 oder ein anderer Nutzer mit der neuen E-Mail-Adresse bereits existiert
     */
    @Transactional
    public KursbetreuerDTO aktualisiereKursbetreuer(Long id, KursbetreuerDTO kursbetreuerDTO) {
        // Prüfen, ob ein Kursbetreuer mit dieser ID existiert
        Kursbetreuer kursbetreuer = kursbetreuerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kein Kursbetreuer mit dieser ID gefunden"));
        
        // Prüfen, ob ein anderer Nutzer mit der neuen E-Mail-Adresse bereits existiert
        if (kursbetreuerDTO.getEmail() != null && !kursbetreuerDTO.getEmail().equals(kursbetreuer.getEmail())) {
            Optional<Nutzer> nutzerMitEmail = nutzerRepository.findByEmail(kursbetreuerDTO.getEmail());
            if (nutzerMitEmail.isPresent() && !nutzerMitEmail.get().getId().equals(id)) {
                throw new IllegalArgumentException("Ein anderer Nutzer mit dieser E-Mail-Adresse existiert bereits");
            }
        }

        if(!kursbetreuer.getIstRegistriert().equals(kursbetreuerDTO.getIstRegistriert())) {
            throw new IllegalArgumentException("Registrier-Status darf nicht geändert werden");
        }
        
        // Kursbetreuer aktualisieren
        kursbetreuer.setEmail(kursbetreuerDTO.getEmail());
        kursbetreuer.setVorname(kursbetreuerDTO.getVorname());
        kursbetreuer.setNachname(kursbetreuerDTO.getNachname());
        kursbetreuer.setIstRegistriert(kursbetreuerDTO.getIstRegistriert());
        
        Kursbetreuer gespeicherterKursbetreuer = kursbetreuerRepository.save(kursbetreuer);
        return kursbetreuerMapper.toDto(gespeicherterKursbetreuer);
    }
    
    /**
     * Löscht einen Studenten.
     *
     * @param id Die ID des zu löschenden Studenten
     * @throws IllegalArgumentException wenn kein Student mit der angegebenen ID existiert
     */
    @Transactional
    public void loescheStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kein Student mit dieser ID gefunden"));
        
        studentRepository.delete(student);
    }
    
    /**
     * Löscht einen Kursbetreuer.
     *
     * @param id Die ID des zu löschenden Kursbetreuers
     * @throws IllegalArgumentException wenn kein Kursbetreuer mit der angegebenen ID existiert
     */
    @Transactional
    public void loescheKursbetreuer(Long id) {
        Kursbetreuer kursbetreuer = kursbetreuerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kein Kursbetreuer mit dieser ID gefunden"));
        
        kursbetreuerRepository.delete(kursbetreuer);
    }


    /**
     * Hilfsmethode zum Abrufen des aktuell authentifizierten Nutzers.
     *
     * @return Der authentifizierte Nutzer oder null, wenn kein Nutzer authentifiziert ist.
     */
    public NutzerDTO getAuthenticatedNutzer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }

        Nutzer nutzer = this.konvertiereUserDetailsZuEntity(userDetails);
        if(nutzer instanceof Student student) {
            return studentMapper.toDto(student);
        }else if(nutzer instanceof Kursbetreuer kursbetreuer) {
            return kursbetreuerMapper.toDto(kursbetreuer);
        }else{
            return nutzerMapper.toDto(nutzer);
        }
    }
}
