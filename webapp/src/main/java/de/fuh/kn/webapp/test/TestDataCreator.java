package de.fuh.kn.webapp.test;

import de.fuh.kn.webapp.persistence.entity.*;
import de.fuh.kn.webapp.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Konfiguration zum Erstellen von initialen Testdaten beim Start der Anwendung.
 */
@Configuration
@ConditionalOnProperty(name = "app.test-data.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TestDataCreator {

    private final KursbetreuerRepository kursbetreuerRepository;
    private final StudentRepository studentRepository;
    private final KursRepository kursRepository;
    private final BelegungRepository belegungRepository;
    private final KurseinheitRepository kurseinheitRepository;
    private final AufgabeRepository aufgabeRepository;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Erstellt Testdaten beim Start der Anwendung.
     *
     * @return Ein CommandLineRunner, der beim Start der Anwendung ausgeführt wird.
     */
    @Bean
    public CommandLineRunner initialUserSetup() {
        return args -> createInitialData();
    }
    
    /**
     * Erstellt die initialen Testdaten. Diese Methode kann auch von anderen
     * Komponenten aufgerufen werden, z.B. vom TestDataController.
     */
    public void createInitialData() {
        log.info("Starte Initialdaten-Erstellung...");

        // Erstellen eines Test-Kursbetreuers
        Kursbetreuer admin = null;
        if (kursbetreuerRepository.count() == 0) {
            admin = new Kursbetreuer();
                admin.setVorname("Admin");
                admin.setNachname("Kursbetreuer");
                admin.setEmail("admin@fernuni-hagen.de");
                admin.setPasswort(passwordEncoder.encode("password"));
                
                admin = kursbetreuerRepository.save(admin);
                
                log.info("Test-Kursbetreuer wurde erstellt.");
                log.info("E-Mail: admin@fernuni-hagen.de");
                log.info("Passwort: password");
            } else {
                admin = kursbetreuerRepository.findByEmail("admin@fernuni-hagen.de").orElse(null);
            }

            // Erstellen eines Test-Studenten
            Student student = null;
            if (studentRepository.count() == 0) {
                student = new Student();
                student.setVorname("Student");
                student.setNachname("Student");
                student.setMatrikelnummer("1234");
                student.setEmail("student@fernuni-hagen.de");
                student.setIstRegistriert(true);
                student.setPasswort(passwordEncoder.encode("password"));

                student = studentRepository.save(student);

                log.info("Test-Student wurde erstellt.");
                log.info("E-Mail: student@fernuni-hagen.de");
                log.info("Passwort: password");
            } else {
                student = studentRepository.findByMatrikelnummer("1234").orElse(null);
            }

            // Erstellen eines Test-Kurses
            Kurs kurs = null;
            if (kursRepository.count() == 0) {
                kurs = new Kurs();
                kurs.setName("Testkurs für automatische Tests");
                
                kurs = kursRepository.save(kurs);
                
                log.info("Test-Kurs wurde erstellt: {}", kurs.getName());
            } else {
                kurs = kursRepository.findByName("Testkurs für automatische Tests").orElse(null);
                
                // Falls kein Kurs mit diesem Namen existiert, nehmen wir den ersten verfügbaren Kurs
                if (kurs == null && !kursRepository.findAll().isEmpty()) {
                    kurs = kursRepository.findAll().get(0);
                }
                
                // Falls immer noch kein Kurs verfügbar ist, erstellen wir einen neuen
                if (kurs == null) {
                    kurs = new Kurs();
                    kurs.setName("Testkurs für automatische Tests");
                    kurs = kursRepository.save(kurs);
                    log.info("Test-Kurs wurde erstellt: {}", kurs.getName());
                }
            }

            // Erstellen einer Test-Belegung für den Studenten, falls noch keine existiert
            if (student != null && kurs != null) {
                boolean belegungExists = belegungRepository.findByStudentAndKurs(student, kurs).isPresent();
                
                if (!belegungExists) {
                    Belegung belegung = new Belegung();
                    belegung.setStudent(student);
                    belegung.setKurs(kurs);
                    belegung.setStartDatum(LocalDate.now().minusDays(10)); // 10 Tage in der Vergangenheit
                    belegung.setEndDatum(LocalDate.now().plusMonths(6));   // 6 Monate in der Zukunft
                    
                    belegungRepository.save(belegung);
                    
                    log.info("Test-Belegung wurde erstellt für Student {} und Kurs {}", 
                             student.getMatrikelnummer(), kurs.getName());
                }
            }

            // Erstellen von Test-Kurseinheiten und Aufgaben
            if (kurs != null) {
                // Prüfen ob bereits Kurseinheiten existieren
                boolean hasKurseinheiten = !kurseinheitRepository.findByKurs(kurs).isEmpty();
                
                if (!hasKurseinheiten) {
                    // Erstelle Test-Kurseinheit
                    Kurseinheit kurseinheit = new Kurseinheit();
                    kurseinheit.setName("Einführung in die Programmierung");
                    kurseinheit.setReihenfolge(1);
                    kurseinheit.setKurs(kurs);
                    kurseinheit = kurseinheitRepository.save(kurseinheit);
                    log.info("Test-Kurseinheit wurde erstellt: {}", kurseinheit.getName());

                    // Erstelle Test-Aufgabe
                    Aufgabe aufgabe = new Aufgabe();
                    aufgabe.setTitel("Erste Programmieraufgabe");
                    aufgabe.setKurseinheit(kurseinheit);
                    aufgabe.setReihenfolge(1);
                    aufgabe = aufgabeRepository.save(aufgabe);
                    log.info("Test-Aufgabe wurde erstellt: {}", aufgabe.getTitel());

                    // Erstelle Test-Teilaufgabe mit einfacher Mathe-Aufgabe
                    Teilaufgabe teilaufgabe1 = new Teilaufgabe();
                    teilaufgabe1.setAufgabe(aufgabe);
                    teilaufgabe1.setReihenfolge(1);
                    teilaufgabe1.setAufgabenstellungMarkdown(
                        "## Einfache Berechnung\n\n" +
                        "Berechnen Sie das Ergebnis der folgenden Addition:\n\n" +
                        "Was ist 15 + 27?\n\n" +
                        "Antwort: {ergebnis}"
                    );
                    
                    Map<String, String> musterloesung1 = new HashMap<>();
                    musterloesung1.put("ergebnis", "42");
                    teilaufgabe1.setMusterloesungFelder(musterloesung1);
                    teilaufgabe1.setMusterloesungBewertungshinweise(
                        "Die korrekte Antwort ist 42. Achten Sie auf die richtige Berechnung."
                    );
                    teilaufgabeRepository.save(teilaufgabe1);

                    // Erstelle zweite Test-Teilaufgabe mit Programmieraufgabe
                    Teilaufgabe teilaufgabe2 = new Teilaufgabe();
                    teilaufgabe2.setAufgabe(aufgabe);
                    teilaufgabe2.setReihenfolge(2);
                    teilaufgabe2.setAufgabenstellungMarkdown(
                        "## Java-Methode\n\n" +
                        "Implementieren Sie eine Java-Methode, die zwei Zahlen addiert.\n\n" +
                        "Die Methode sollte:\n" +
                        "- Zwei Integer-Parameter akzeptieren\n" +
                        "- Die Summe der beiden Zahlen zurückgeben\n\n" +
                        "\n" +
                        "public int {{{methodenImplementierung}}}\n"
                    );
                    
                    Map<String, String> musterloesung2 = new HashMap<>();
                    musterloesung2.put("methodenImplementierung", 
                        "addiere(int a, int b) {\n    return a + b;\n}");
                    teilaufgabe2.setMusterloesungFelder(musterloesung2);
                    teilaufgabe2.setMusterloesungBewertungshinweise(
                        "Die Methode sollte korrekt zwei Integer addieren und das Ergebnis zurückgeben. " +
                        "Achten Sie auf die richtige Syntax und den korrekten Rückgabetyp."
                    );
                    teilaufgabeRepository.save(teilaufgabe2);
                    
                    log.info("Test-Teilaufgaben wurden erstellt");
                }
            }
    }
}
