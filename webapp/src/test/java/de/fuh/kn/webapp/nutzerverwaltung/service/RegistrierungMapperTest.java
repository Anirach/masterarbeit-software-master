package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.persistence.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für RegistrierungMapper.
 */
@SpringBootTest
class RegistrierungMapperTest {

    @Autowired
    private RegistrierungMapper registrierungMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Testet die Konvertierung von RegistrierungDTO zu Student-Entity.
     */
    @Test
    void testRegistrierungDTOToStudent() {
        // RegistrierungDTO erstellen
        RegistrierungDTO registrierungDTO = new RegistrierungDTO();
        registrierungDTO.setEmail("student@fuh.de");
        registrierungDTO.setVorname("Max");
        registrierungDTO.setNachname("Mustermann");
        registrierungDTO.setPasswort("passwort123");
        registrierungDTO.setPasswortBestaetigung("passwort123");
        registrierungDTO.setMatrikelnummer("1234567");

        // DTO zu Entity konvertieren
        Student student = registrierungMapper.toStudent(registrierungDTO);

        // Ergebnisse überprüfen
        assertNotNull(student);
        assertEquals(registrierungDTO.getEmail(), student.getEmail());
        assertEquals(registrierungDTO.getVorname(), student.getVorname());
        assertEquals(registrierungDTO.getNachname(), student.getNachname());
        assertEquals(registrierungDTO.getMatrikelnummer(), student.getMatrikelnummer());
        
        // Diese Attribute sollten gesetzt oder initialisiert sein
        assertNotNull(student.getBelegungen());
        assertTrue(student.getBelegungen().isEmpty());
        assertNotNull(student.getLoesungsVersuche());
        assertTrue(student.getLoesungsVersuche().isEmpty());
        assertNotNull(student.getChats());
        assertTrue(student.getChats().isEmpty());
        
        // Diese Attribute sollten ignoriert werden
        assertNull(student.getId());
        assertNull(student.getPasswort()); // Passwort wird in dieser Methode nicht gesetzt
        assertTrue(student.getIstRegistriert()); // Default sollte true sein
    }

    /**
     * Testet die Konvertierung von RegistrierungDTO zu Student-Entity mit verschlüsseltem Passwort.
     */
    @Test
    void testToStudentWithEncodedPassword() {
        // RegistrierungDTO erstellen
        RegistrierungDTO registrierungDTO = new RegistrierungDTO();
        registrierungDTO.setEmail("student@fuh.de");
        registrierungDTO.setVorname("Max");
        registrierungDTO.setNachname("Mustermann");
        registrierungDTO.setPasswort("passwort123");
        registrierungDTO.setPasswortBestaetigung("passwort123");
        registrierungDTO.setMatrikelnummer("1234567");

        // DTO zu Entity mit verschlüsseltem Passwort konvertieren
        Student student = registrierungMapper.toStudentWithEncodedPassword(registrierungDTO, passwordEncoder);

        // Ergebnisse überprüfen
        assertNotNull(student);
        assertEquals(registrierungDTO.getEmail(), student.getEmail());
        assertEquals(registrierungDTO.getVorname(), student.getVorname());
        assertEquals(registrierungDTO.getNachname(), student.getNachname());
        assertEquals(registrierungDTO.getMatrikelnummer(), student.getMatrikelnummer());
        
        // Passwort sollte gesetzt und verschlüsselt sein
        assertNotNull(student.getPasswort());
        assertNotEquals(registrierungDTO.getPasswort(), student.getPasswort());
        assertTrue(passwordEncoder.matches(registrierungDTO.getPasswort(), student.getPasswort()));
        
        // Andere Attribute überprüfen
        assertNull(student.getId());
        assertTrue(student.getIstRegistriert());
        assertNotNull(student.getBelegungen());
        assertTrue(student.getBelegungen().isEmpty());
    }
}
