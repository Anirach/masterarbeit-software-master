package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Belegung;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für StudentMapper.
 */
@SpringBootTest
class StudentMapperTest {

    @Autowired
    private StudentMapper studentMapper;

    /**
     * Testet die Konvertierung von Student-Entity zu StudentDTO.
     */
    @Test
    void testStudentToStudentDTO() {
        // Student-Entity erstellen
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@fuh.de");
        student.setVorname("Max");
        student.setNachname("Mustermann");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");
        student.setBelegungen(new ArrayList<>());

        // Entity zu DTO konvertieren
        StudentDTO studentDTO = studentMapper.toDto(student);

        // Ergebnisse überprüfen
        assertNotNull(studentDTO);
        assertEquals(student.getId(), studentDTO.getId());
        assertEquals(student.getEmail(), studentDTO.getEmail());
        assertEquals(student.getVorname(), studentDTO.getVorname());
        assertEquals(student.getNachname(), studentDTO.getNachname());
        assertEquals(student.getIstRegistriert(), studentDTO.getIstRegistriert());
        assertEquals(student.getMatrikelnummer(), studentDTO.getMatrikelnummer());
        assertNotNull(studentDTO.getBelegungIds());
        assertTrue(studentDTO.getBelegungIds().isEmpty());
        assertEquals("Max Mustermann (Matrikelnr. 123456)", studentDTO.getDisplayName());
    }

    /**
     * Testet die getDisplayName Methode für einen Studenten, der nicht vollständig registriert ist.
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
        student.setBelegungen(new ArrayList<>());

        // Entity zu DTO konvertieren
        StudentDTO studentDTO = studentMapper.toDto(student);

        // Ergebnisse überprüfen
        assertNotNull(studentDTO);
        assertEquals("Nicht registriert (Matrikelnr. 123456)", studentDTO.getDisplayName());
    }

    /**
     * Testet die Konvertierung von StudentDTO zu Student-Entity.
     */
    @Test
    void testStudentDTOToStudent() {
        // StudentDTO erstellen
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(1L);
        studentDTO.setEmail("student@fuh.de");
        studentDTO.setVorname("Max");
        studentDTO.setNachname("Mustermann");
        studentDTO.setIstRegistriert(true);
        studentDTO.setMatrikelnummer("123456");
        studentDTO.setBelegungIds(Arrays.asList(1L, 2L));

        // DTO zu Entity konvertieren
        Student student = studentMapper.toEntity(studentDTO);

        // Ergebnisse überprüfen
        assertNotNull(student);
        assertEquals(studentDTO.getId(), student.getId());
        assertEquals(studentDTO.getEmail(), student.getEmail());
        assertEquals(studentDTO.getVorname(), student.getVorname());
        assertEquals(studentDTO.getNachname(), student.getNachname());
        assertEquals(studentDTO.getIstRegistriert(), student.getIstRegistriert());
        assertEquals(studentDTO.getMatrikelnummer(), student.getMatrikelnummer());
        // Belegungen werden nicht zurück gemappt, da sie separat verwaltet werden
        assertNotNull(student.getBelegungen());
        assertTrue(student.getBelegungen().isEmpty());
    }

    /**
     * Testet die Konvertierung von einer Liste von Student-Entities zu einer Liste von StudentDTOs.
     */
    @Test
    void testStudentListToStudentDTOList() {
        // Student-Entities erstellen
        Student student1 = new Student();
        student1.setId(1L);
        student1.setEmail("student1@fuh.de");
        student1.setVorname("Max");
        student1.setNachname("Mustermann");
        student1.setIstRegistriert(true);
        student1.setMatrikelnummer("123456");
        student1.setBelegungen(new ArrayList<>());

        Student student2 = new Student();
        student2.setId(2L);
        student2.setEmail("student2@fuh.de");
        student2.setVorname("Erika");
        student2.setNachname("Musterfrau");
        student2.setIstRegistriert(true);
        student2.setMatrikelnummer("654321");
        student2.setBelegungen(new ArrayList<>());

        List<Student> studenten = Arrays.asList(student1, student2);

        // Entities zu DTOs konvertieren
        List<StudentDTO> studentDTOs = studentMapper.toDtoList(studenten);

        // Ergebnisse überprüfen
        assertNotNull(studentDTOs);
        assertEquals(2, studentDTOs.size());
        
        // Student 1 überprüfen
        assertEquals(student1.getId(), studentDTOs.get(0).getId());
        assertEquals(student1.getEmail(), studentDTOs.get(0).getEmail());
        assertEquals(student1.getVorname(), studentDTOs.get(0).getVorname());
        assertEquals(student1.getNachname(), studentDTOs.get(0).getNachname());
        assertEquals(student1.getIstRegistriert(), studentDTOs.get(0).getIstRegistriert());
        assertEquals(student1.getMatrikelnummer(), studentDTOs.get(0).getMatrikelnummer());
        assertEquals("Max Mustermann (Matrikelnr. 123456)", studentDTOs.get(0).getDisplayName());
        
        // Student 2 überprüfen
        assertEquals(student2.getId(), studentDTOs.get(1).getId());
        assertEquals(student2.getEmail(), studentDTOs.get(1).getEmail());
        assertEquals(student2.getVorname(), studentDTOs.get(1).getVorname());
        assertEquals(student2.getNachname(), studentDTOs.get(1).getNachname());
        assertEquals(student2.getIstRegistriert(), studentDTOs.get(1).getIstRegistriert());
        assertEquals(student2.getMatrikelnummer(), studentDTOs.get(1).getMatrikelnummer());
        assertEquals("Erika Musterfrau (Matrikelnr. 654321)", studentDTOs.get(1).getDisplayName());
    }

    /**
     * Testet die Konvertierung von Student-Entity mit Belegungen zu StudentDTO.
     */
    @Test
    void testStudentWithBelegungenToStudentDTO() {
        // Student-Entity erstellen
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@fuh.de");
        student.setVorname("Max");
        student.setNachname("Mustermann");
        student.setIstRegistriert(true);
        student.setMatrikelnummer("123456");

        // Kurs-Entities erstellen
        Kurs kurs1 = new Kurs();
        kurs1.setId(1L);
        kurs1.setName("Kommunikationsnetze");

        Kurs kurs2 = new Kurs();
        kurs2.setId(2L);
        kurs2.setName("Algorithmen und Datenstrukturen");

        // Belegung-Entities erstellen
        Belegung belegung1 = new Belegung();
        belegung1.setId(1L);
        belegung1.setStudent(student);
        belegung1.setKurs(kurs1);
        belegung1.setStartDatum(LocalDate.now().minusDays(10));
        belegung1.setEndDatum(null);

        Belegung belegung2 = new Belegung();
        belegung2.setId(2L);
        belegung2.setStudent(student);
        belegung2.setKurs(kurs2);
        belegung2.setStartDatum(LocalDate.now().minusDays(5));
        belegung2.setEndDatum(LocalDate.now().plusMonths(6));

        student.setBelegungen(Arrays.asList(belegung1, belegung2));

        // Entity zu DTO konvertieren
        StudentDTO studentDTO = studentMapper.toDto(student);

        // Ergebnisse überprüfen
        assertNotNull(studentDTO);
        assertEquals(student.getId(), studentDTO.getId());
        assertEquals(student.getEmail(), studentDTO.getEmail());
        assertEquals(student.getVorname(), studentDTO.getVorname());
        assertEquals(student.getNachname(), studentDTO.getNachname());
        assertEquals(student.getIstRegistriert(), studentDTO.getIstRegistriert());
        assertEquals(student.getMatrikelnummer(), studentDTO.getMatrikelnummer());
        assertEquals("Max Mustermann (Matrikelnr. 123456)", studentDTO.getDisplayName());
        
        // Belegungen überprüfen
        assertNotNull(studentDTO.getBelegungIds());
        assertEquals(2, studentDTO.getBelegungIds().size());
        assertTrue(studentDTO.getBelegungIds().contains(belegung1.getId()));
        assertTrue(studentDTO.getBelegungIds().contains(belegung2.getId()));
    }
}
