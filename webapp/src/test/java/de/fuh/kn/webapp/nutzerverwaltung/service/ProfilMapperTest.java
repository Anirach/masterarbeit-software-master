package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für ProfilMapper.
 */
@SpringBootTest
class ProfilMapperTest {

    @Autowired
    private ProfilMapper profilMapper;

    /**
     * Testet die Konvertierung von NutzerDTO zu ProfilAenderungDTO.
     * Da NutzerDTO eine abstrakte Klasse ist, verwenden wir für diesen Test
     * eine StudentDTO-Instanz als konkrete Implementation.
     */
    @Test
    void testNutzerToProfilAenderungDTO() {
        // StudentDTO erstellen
        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setEmail("student@fuh.de");
        student.setVorname("Max");
        student.setNachname("Mustermann");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        // DTO zu ProfilDTO konvertieren
        ProfilAenderungDTO profilDTO = profilMapper.toDto(student);

        // Ergebnisse überprüfen
        assertNotNull(profilDTO);
        assertEquals(student.getEmail(), profilDTO.getEmail());
        assertEquals(student.getVorname(), profilDTO.getVorname());
        assertEquals(student.getNachname(), profilDTO.getNachname());
    }

    /**
     * Testet die Aktualisierung einer Nutzer-Entity mit den Werten eines ProfilAenderungDTO.
     */
    @Test
    void testUpdateNutzerFromDto() {
        // Student-Entity erstellen
        Student student = new Student();
        student.setId(1L);
        student.setEmail("alt@fuh.de");
        student.setVorname("Alter");
        student.setNachname("Name");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        // ProfilAenderungDTO mit neuen Werten erstellen
        ProfilAenderungDTO profilAenderungDTO = new ProfilAenderungDTO();
        profilAenderungDTO.setEmail("neu@fuh.de");
        profilAenderungDTO.setVorname("Neuer");
        profilAenderungDTO.setNachname("Name");

        // Entity mit DTO aktualisieren
        profilMapper.updateNutzerFromDto(profilAenderungDTO, student);

        // Ergebnisse überprüfen
        assertEquals(profilAenderungDTO.getEmail(), student.getEmail());
        assertEquals(profilAenderungDTO.getVorname(), student.getVorname());
        assertEquals(profilAenderungDTO.getNachname(), student.getNachname());
        
        // Prüfen, dass andere Attribute unverändert bleiben
        assertEquals(1L, student.getId());
        assertEquals(true, student.getIstRegistriert());
        assertEquals("123456", student.getMatrikelnummer());
    }

    /**
     * Testet die Aktualisierung einer Nutzer-Entity mit teilweise leeren Werten eines ProfilAenderungDTO.
     */
    @Test
    void testUpdateNutzerFromDtoWithNullValues() {
        // Student-Entity erstellen
        Student student = new Student();
        student.setId(1L);
        student.setEmail("alt@fuh.de");
        student.setVorname("Alter");
        student.setNachname("Name");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        // ProfilAenderungDTO mit teilweise leeren Werten erstellen
        ProfilAenderungDTO profilDTO = new ProfilAenderungDTO();
        profilDTO.setEmail("neu@fuh.de");
        profilDTO.setVorname(null); // Wird auf null gesetzt
        profilDTO.setNachname(""); // Wird auf leeren String gesetzt

        // Entity mit DTO aktualisieren
        profilMapper.updateNutzerFromDto(profilDTO, student);

        // Ergebnisse überprüfen
        assertEquals(profilDTO.getEmail(), student.getEmail());
        assertNull(student.getVorname()); // Sollte auf null gesetzt sein
        assertEquals("", student.getNachname()); // Sollte auf leeren String gesetzt sein
    }
}
