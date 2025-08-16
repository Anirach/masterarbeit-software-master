package de.fuh.kn.webapp.aufgabenverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.auth.StudentUserDetails;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkippedAssignmentResetListenerTest {

    @Mock
    private LoesungsversuchService loesungsversuchService;

    /**
     * Testspezifische Version des Listeners, die einfach prüft,
     * ob das UserDetails-Objekt vom StudentUserDetails-Mock stammt.
     */
    @InjectMocks
    private SkippedAssignmentResetListener listener;
    
    private Student mockStudent;
    
    @BeforeEach
    void setUp() {
        mockStudent = new Student();
        mockStudent.setId(123L);
        mockStudent.setVorname("Max");
        mockStudent.setNachname("Mustermann");
        mockStudent.setEmail("student@example.com");
    }
    
    @Test
    void onAuthenticationSuccess_shouldResetSkippedAssignmentsForStudent() {
        // Arrange
        String username = "student@example.com";

        // Mock StudentUserDetails - minimally setup just the class and username
        StudentUserDetails studentUserDetails = mock(StudentUserDetails.class);
        when(studentUserDetails.getUsername()).thenReturn(username);

        // Create authentication with minimal details
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(studentUserDetails, "password");

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        when(loesungsversuchService.findeStudentIdByEmail(username)).thenReturn(Optional.of(mockStudent.getId()));
        when(loesungsversuchService.setzeUebersprungeneLoesungsversucheZurueck(mockStudent.getId()))
                .thenReturn(3); // 3 assignments were reset

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(loesungsversuchService).findeStudentIdByEmail(username);
        verify(loesungsversuchService).setzeUebersprungeneLoesungsversucheZurueck(mockStudent.getId());
    }
    
    @Test
    void onAuthenticationSuccess_shouldNotResetForNonStudentRole() {
        // Arrange
        String username = "admin@example.com";

        // Use regular UserDetails object (not StudentUserDetails)
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, "password");

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verifyNoInteractions(loesungsversuchService);
    }
    
    @Test
    void onAuthenticationSuccess_shouldHandleStudentNotFound() {
        // Arrange
        String username = "nonexistent@example.com";

        // Mock StudentUserDetails
        StudentUserDetails studentUserDetails = mock(StudentUserDetails.class);
        when(studentUserDetails.getUsername()).thenReturn(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(studentUserDetails, "password");

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        when(loesungsversuchService.findeStudentIdByEmail(username)).thenReturn(Optional.empty());

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(loesungsversuchService).findeStudentIdByEmail(username);
        verify(loesungsversuchService, never()).setzeUebersprungeneLoesungsversucheZurueck(any());
    }
    
    @Test
    void onAuthenticationSuccess_shouldHandleExceptions() {
        // Arrange
        String username = "student@example.com";

        // Mock StudentUserDetails
        StudentUserDetails studentUserDetails = mock(StudentUserDetails.class);
        when(studentUserDetails.getUsername()).thenReturn(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(studentUserDetails, "password");

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        when(loesungsversuchService.findeStudentIdByEmail(username))
            .thenThrow(new RuntimeException("Test exception"));

        // Act & Assert - should not propagate the exception
        listener.onAuthenticationSuccess(event);

        // Verify it attempted to find student but exception prevented reset
        verify(loesungsversuchService).findeStudentIdByEmail(username);
        verify(loesungsversuchService, never()).setzeUebersprungeneLoesungsversucheZurueck(any());
    }
    
    @Test
    void onAuthenticationSuccess_shouldHandleNonUserDetailsPrincipal() {
        // Arrange
        // Use a String principal instead of UserDetails
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken("Not a UserDetails", "password");
        
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        
        // Act
        listener.onAuthenticationSuccess(event);
        
        // Assert
        verifyNoInteractions(loesungsversuchService);
    }
}