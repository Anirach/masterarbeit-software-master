package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentMapper;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service für den Zugriff auf Student-Daten.
 * Bietet Methoden zum Laden von Student-DTOs für Controller und andere Services.
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    /**
     * Lädt ein StudentDTO anhand der ID.
     *
     * @param studentId Die ID des Studenten
     * @return Das StudentDTO
     * @throws IllegalArgumentException Wenn kein Student mit der angegebenen ID gefunden wurde
     */
    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long studentId) {
        return findStudentEntityById(studentId)
                .map(studentMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden: " + studentId));
    }

    /**
     * Lädt ein StudentDTO anhand der E-Mail-Adresse.
     *
     * @param email Die E-Mail-Adresse des Studenten
     * @return Optional mit dem StudentDTO oder ein leeres Optional, wenn kein Student
     *         mit der angegebenen E-Mail-Adresse gefunden wurde
     */
    @Transactional(readOnly = true)
    public Optional<StudentDTO> findStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .map(studentMapper::toDto);
    }

    /**
     * Methode zum Laden einer Student-Entität für interne Operationen anderer Services.
     * Diese Methode sollte nur von anderen Service-Klassen verwendet werden und ist nicht für Controller gedacht.
     *
     * ACHTUNG: Diese Methode verstößt absichtlich gegen die Clean Architecture-Regeln, da einige Services
     * aus technischen Gründen direkt mit Entitäten arbeiten müssen. Sie sollte möglichst vermieden werden,
     * wird aber für Kompatibilität mit bestehendem Code beibehalten.
     *
     * @param studentId Die ID des Studenten
     * @return Die Student-Entität
     * @throws IllegalArgumentException Wenn kein Student mit der angegebenen ID gefunden wurde
     */
    /**
     * Überprüft, ob ein Student mit der angegebenen ID existiert.
     *
     * @param studentId Die ID des Studenten
     * @return true, wenn der Student existiert, sonst false
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long studentId) {
        return studentRepository.existsById(studentId);
    }

    /**
     * Gibt die ID des Studenten zurück, der dem DTO entspricht.
     * (Triviale Helper-Methode, um Konsistenz zu wahren)
     *
     * @param studentDTO Das StudentDTO
     * @return Die ID des Studenten
     */
    public Long getStudentId(StudentDTO studentDTO) {
        return studentDTO.getId();
    }

    /**
     * Interne Hilfsmethode zum Laden einer Student-Entität für andere Services.
     * Diese Methode sollte nur innerhalb der Service-Schicht verwendet werden.
     *
     * @param studentId Die ID des Studenten
     * @return Optional mit der Student-Entität
     */
    private Optional<Student> findStudentEntityById(Long studentId) {
        return studentRepository.findById(studentId);
    }

}