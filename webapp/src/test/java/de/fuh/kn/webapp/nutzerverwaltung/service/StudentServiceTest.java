package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentMapper;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    
    @Mock
    private StudentMapper studentMapper;
    
    @InjectMocks
    private StudentService studentService;
    
    private Student testStudent;
    private StudentDTO testStudentDTO;
    
    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setEmail("test@example.com");
        testStudent.setVorname("Max");
        testStudent.setNachname("Mustermann");
        
        testStudentDTO = new StudentDTO();
        testStudentDTO.setId(1L);
        testStudentDTO.setEmail("test@example.com");
        testStudentDTO.setVorname("Max");
        testStudentDTO.setNachname("Mustermann");
    }
    
    @Test
    @DisplayName("getStudentById sollte StudentDTO zurückgeben wenn Student existiert")
    void testGetStudentById_ShouldReturnStudentDTO_WhenStudentExists() {
        // Arrange
        Long studentId = 1L;
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        
        // Act
        StudentDTO result = studentService.getStudentById(studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testStudentDTO.getId(), result.getId());
        assertEquals(testStudentDTO.getEmail(), result.getEmail());
        assertEquals(testStudentDTO.getVorname(), result.getVorname());
        assertEquals(testStudentDTO.getNachname(), result.getNachname());
        
        verify(studentRepository).findById(studentId);
        verify(studentMapper).toDto(testStudent);
    }
    
    @Test
    @DisplayName("getStudentById sollte IllegalArgumentException werfen wenn Student nicht existiert")
    void testGetStudentById_ShouldThrowException_WhenStudentNotFound() {
        // Arrange
        Long studentId = 999L;
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> studentService.getStudentById(studentId)
        );
        
        assertEquals("Student nicht gefunden: " + studentId, exception.getMessage());
        verify(studentRepository).findById(studentId);
        verify(studentMapper, never()).toDto(any());
    }
    
    @Test
    @DisplayName("findStudentByEmail sollte Optional mit StudentDTO zurückgeben wenn Student existiert")
    void testFindStudentByEmail_ShouldReturnOptionalWithStudentDTO_WhenStudentExists() {
        // Arrange
        String email = "test@example.com";
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDTO);
        
        // Act
        Optional<StudentDTO> result = studentService.findStudentByEmail(email);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testStudentDTO.getId(), result.get().getId());
        assertEquals(testStudentDTO.getEmail(), result.get().getEmail());
        
        verify(studentRepository).findByEmail(email);
        verify(studentMapper).toDto(testStudent);
    }
    
    @Test
    @DisplayName("findStudentByEmail sollte leeres Optional zurückgeben wenn Student nicht existiert")
    void testFindStudentByEmail_ShouldReturnEmptyOptional_WhenStudentNotFound() {
        // Arrange
        String email = "nonexistent@example.com";
        when(studentRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // Act
        Optional<StudentDTO> result = studentService.findStudentByEmail(email);
        
        // Assert
        assertFalse(result.isPresent());
        
        verify(studentRepository).findByEmail(email);
        verify(studentMapper, never()).toDto(any());
    }
    
    @Test
    @DisplayName("existsById sollte true zurückgeben wenn Student existiert")
    void testExistsById_ShouldReturnTrue_WhenStudentExists() {
        // Arrange
        Long studentId = 1L;
        when(studentRepository.existsById(studentId)).thenReturn(true);
        
        // Act
        boolean result = studentService.existsById(studentId);
        
        // Assert
        assertTrue(result);
        verify(studentRepository).existsById(studentId);
    }
    
    @Test
    @DisplayName("existsById sollte false zurückgeben wenn Student nicht existiert")
    void testExistsById_ShouldReturnFalse_WhenStudentNotExists() {
        // Arrange
        Long studentId = 999L;
        when(studentRepository.existsById(studentId)).thenReturn(false);
        
        // Act
        boolean result = studentService.existsById(studentId);
        
        // Assert
        assertFalse(result);
        verify(studentRepository).existsById(studentId);
    }
    
    @Test
    @DisplayName("getStudentId sollte ID aus StudentDTO zurückgeben")
    void testGetStudentId_ShouldReturnIdFromStudentDTO() {
        // Arrange
        Long expectedId = 1L;
        testStudentDTO.setId(expectedId);
        
        // Act
        Long result = studentService.getStudentId(testStudentDTO);
        
        // Assert
        assertEquals(expectedId, result);
    }
    
    @Test
    @DisplayName("getStudentId sollte mit null ID funktionieren")
    void testGetStudentId_ShouldHandleNullId() {
        // Arrange
        testStudentDTO.setId(null);
        
        // Act
        Long result = studentService.getStudentId(testStudentDTO);
        
        // Assert
        assertNull(result);
    }
}