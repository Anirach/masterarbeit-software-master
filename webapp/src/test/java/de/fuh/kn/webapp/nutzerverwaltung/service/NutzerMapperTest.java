package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerMapper;
import de.fuh.kn.webapp.persistence.entity.Nutzer;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testklasse für NutzerMapper.
 */
@SpringBootTest
class NutzerMapperTest {

    @Autowired
    private NutzerMapper nutzerMapper;

    /**
     * Testet die Konvertierung von Nutzer-Entity zu NutzerDTO.
     * Da Nutzer eine abstrakte Klasse ist, verwenden wir für diesen Test
     * eine Student-Instanz als konkrete Implementation.
     */
    @Test
    void testNutzerToNutzerDTO() {
        // Student-Entity erstellen, da Nutzer abstrakt ist
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@fuh.de");
        student.setVorname("Max");
        student.setNachname("Mustermann");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        // Entity zu DTO konvertieren
        NutzerDTO nutzerDTO = nutzerMapper.toDto(student);

        // Ergebnisse überprüfen
        assertNotNull(nutzerDTO);
        assertEquals(student.getId(), nutzerDTO.getId());
        assertEquals(student.getEmail(), nutzerDTO.getEmail());
        assertEquals(student.getVorname(), nutzerDTO.getVorname());
        assertEquals(student.getNachname(), nutzerDTO.getNachname());
        assertEquals(student.getIstRegistriert(), nutzerDTO.getIstRegistriert());
        assertEquals("Max Mustermann", nutzerDTO.getDisplayName());
    }

    /**
     * Testet die getDisplayName Methode für einen Nutzer, der nicht vollständig registriert ist.
     */
    @Test
    void testGetDisplayNameNichtRegistriert() {
        // Student-Entity erstellen, der nicht registriert ist
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@fuh.de");
        student.setVorname(null);
        student.setNachname(null);
        student.setIstRegistriert(false);
        student.setMatrikelnummer("123456");

        // Entity zu DTO konvertieren
        NutzerDTO nutzerDTO = nutzerMapper.toDto(student);

        // Ergebnisse überprüfen
        assertNotNull(nutzerDTO);
        assertEquals("Nicht registriert", nutzerDTO.getDisplayName());
    }

    /**
     * Testet die Konvertierung von einer Liste von Nutzer-Entities zu einer Liste von NutzerDTOs.
     */
    @Test
    void testNutzerListToNutzerDTOList() {
        // Student-Entities erstellen
        Student student1 = new Student();
        student1.setId(1L);
        student1.setEmail("student1@fuh.de");
        student1.setVorname("Max");
        student1.setNachname("Mustermann");
        student1.setIstRegistriert(true);
        student1.setMatrikelnummer("123456");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setEmail("student2@fuh.de");
        student2.setVorname("Erika");
        student2.setNachname("Musterfrau");
        student2.setIstRegistriert(true);
        student2.setMatrikelnummer("654321");

        List<Nutzer> nutzer = Arrays.asList(student1, student2);

        // Entities zu DTOs konvertieren
        List<NutzerDTO> nutzerDTOs = nutzerMapper.toDtoList(nutzer);

        // Ergebnisse überprüfen
        assertNotNull(nutzerDTOs);
        assertEquals(2, nutzerDTOs.size());
        
        // Student 1 überprüfen
        assertEquals(student1.getId(), nutzerDTOs.get(0).getId());
        assertEquals(student1.getEmail(), nutzerDTOs.get(0).getEmail());
        assertEquals(student1.getVorname(), nutzerDTOs.get(0).getVorname());
        assertEquals(student1.getNachname(), nutzerDTOs.get(0).getNachname());
        assertEquals(student1.getIstRegistriert(), nutzerDTOs.get(0).getIstRegistriert());
        assertEquals("Max Mustermann", nutzerDTOs.get(0).getDisplayName());
        
        // Student 2 überprüfen
        assertEquals(student2.getId(), nutzerDTOs.get(1).getId());
        assertEquals(student2.getEmail(), nutzerDTOs.get(1).getEmail());
        assertEquals(student2.getVorname(), nutzerDTOs.get(1).getVorname());
        assertEquals(student2.getNachname(), nutzerDTOs.get(1).getNachname());
        assertEquals(student2.getIstRegistriert(), nutzerDTOs.get(1).getIstRegistriert());
        assertEquals("Erika Musterfrau", nutzerDTOs.get(1).getDisplayName());
    }
}
