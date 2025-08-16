package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für den KursMaterialImageController.
 * Diese Tests decken den Zugriff auf Bilder aus Kursmaterialien ab.
 */
@ExtendWith(MockitoExtension.class)
class KursMaterialImageControllerTest {

    @Mock
    private KursMaterialService kursMaterialService;
    
    @Mock
    private KursService kursService;
    
    @Mock
    private KurseinheitService kurseinheitService;
    
    @Mock
    private NutzerService nutzerService;
    
    @Mock
    private BelegungService belegungService;
    
    @InjectMocks
    private KursMaterialImageController controller;
    
    private KurseinheitDTO testKurseinheit;
    private KursDTO testKurs;
    private KursMaterialDTO testBild;
    private StudentDTO testStudent;
    private KursbetreuerDTO testKursbetreuer;
    
    @BeforeEach
    void setUp() {
        // Test-Kurs einrichten
        testKurs = new KursDTO();
        testKurs.setId(1L);
        testKurs.setName("Testkurs");
        
        // Test-Kurseinheit einrichten
        testKurseinheit = new KurseinheitDTO();
        testKurseinheit.setId(2L);
        testKurseinheit.setName("Testeinheit");
        testKurseinheit.setKursId(testKurs.getId());
        
        // Test-Bild einrichten
        testBild = new KursMaterialDTO();
        testBild.setId(3L);
        testBild.setName("testbild.jpg");
        testBild.setMimeType("image/jpeg");
        testBild.setInhalt("Testinhalt".getBytes());
        testBild.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);
        
        // Test-Student einrichten
        testStudent = new StudentDTO();
        testStudent.setId(4L);
        testStudent.setVorname("Test");
        testStudent.setNachname("Student");
        
        // Test-Kursbetreuer einrichten
        testKursbetreuer = new KursbetreuerDTO();
        testKursbetreuer.setId(5L);
        testKursbetreuer.setVorname("Test");
        testKursbetreuer.setNachname("Betreuer");
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte ein Bild von der Kurseinheit zurückgeben")
    void getKurseinheitBild_ShouldReturnImageFromKurseinheit() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(testKurseinheit);
        when(kursService.getKursById(testKurs.getId())).thenReturn(testKurs);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testKursbetreuer);
        when(kursMaterialService.findBildByNameAndKurseinheitId(testKurseinheit.getId(), testBild.getName())).thenReturn(testBild);
        
        // Act
        ResponseEntity<byte[]> response = controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName());
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("image/jpeg"), response.getHeaders().getContentType());
        assertArrayEquals(testBild.getInhalt(), response.getBody());
        
        verify(kurseinheitService).getKurseinheitById(testKurseinheit.getId());
        verify(kursService).getKursById(testKurs.getId());
        verify(nutzerService).getAuthenticatedNutzer();
        verify(kursMaterialService).findBildByNameAndKurseinheitId(testKurseinheit.getId(), testBild.getName());
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte ein Bild vom Kurs zurückgeben, wenn es nicht in der Kurseinheit ist")
    void getKurseinheitBild_ShouldReturnImageFromKurs_WhenNotInKurseinheit() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(testKurseinheit);
        when(kursService.getKursById(testKurs.getId())).thenReturn(testKurs);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testKursbetreuer);
        when(kursMaterialService.findBildByNameAndKurseinheitId(testKurseinheit.getId(), testBild.getName())).thenReturn(null);
        when(kursMaterialService.findBildByNameAndKursId(testKurs.getId(), testBild.getName())).thenReturn(testBild);
        
        // Act
        ResponseEntity<byte[]> response = controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName());
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("image/jpeg"), response.getHeaders().getContentType());
        assertArrayEquals(testBild.getInhalt(), response.getBody());
        
        verify(kursMaterialService).findBildByNameAndKurseinheitId(testKurseinheit.getId(), testBild.getName());
        verify(kursMaterialService).findBildByNameAndKursId(testKurs.getId(), testBild.getName());
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte für einen eingeschriebenen Studenten ein Bild zurückgeben")
    void getKurseinheitBild_ShouldReturnImage_ForEnrolledStudent() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(testKurseinheit);
        when(kursService.getKursById(testKurs.getId())).thenReturn(testKurs);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(belegungService.isStudentEnrolledInKurs(testStudent, testKurs)).thenReturn(true);
        when(kursMaterialService.findBildByNameAndKurseinheitId(testKurseinheit.getId(), testBild.getName())).thenReturn(testBild);
        
        // Act
        ResponseEntity<byte[]> response = controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName());
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("image/jpeg"), response.getHeaders().getContentType());
        assertArrayEquals(testBild.getInhalt(), response.getBody());
        
        verify(belegungService).isStudentEnrolledInKurs(testStudent, testKurs);
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte einen 404 Fehler zurückgeben, wenn die Kurseinheit nicht gefunden wird")
    void getKurseinheitBild_ShouldThrow404_WhenKurseinheitNotFound() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(null);
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName())
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertThat(exception.getReason()).contains("Kurseinheit nicht gefunden");
        
        verify(kurseinheitService).getKurseinheitById(testKurseinheit.getId());
        verify(kursMaterialService, never()).findBildByNameAndKurseinheitId(anyLong(), anyString());
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte einen 404 Fehler zurückgeben, wenn der Kurs nicht gefunden wird")
    void getKurseinheitBild_ShouldThrow404_WhenKursNotFound() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(testKurseinheit);
        when(kursService.getKursById(testKurs.getId())).thenReturn(null);
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName())
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertThat(exception.getReason()).contains("Kurs nicht gefunden");
        
        verify(kurseinheitService).getKurseinheitById(testKurseinheit.getId());
        verify(kursService).getKursById(testKurs.getId());
        verify(kursMaterialService, never()).findBildByNameAndKurseinheitId(anyLong(), anyString());
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte einen 403 Fehler zurückgeben, wenn der Student nicht für den Kurs eingeschrieben ist")
    void getKurseinheitBild_ShouldThrow403_WhenStudentNotEnrolled() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(testKurseinheit);
        when(kursService.getKursById(testKurs.getId())).thenReturn(testKurs);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testStudent);
        when(belegungService.isStudentEnrolledInKurs(testStudent, testKurs)).thenReturn(false);
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName())
        );
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertThat(exception.getReason()).contains("Kein Zugriff auf dieses Bild - Kurs nicht belegt");
        
        verify(belegungService).isStudentEnrolledInKurs(testStudent, testKurs);
        verify(kursMaterialService, never()).findBildByNameAndKurseinheitId(anyLong(), anyString());
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte einen 404 Fehler zurückgeben, wenn das Bild nicht gefunden wird")
    void getKurseinheitBild_ShouldThrow404_WhenImageNotFound() {
        // Arrange
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(testKurseinheit);
        when(kursService.getKursById(testKurs.getId())).thenReturn(testKurs);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(testKursbetreuer);
        when(kursMaterialService.findBildByNameAndKurseinheitId(testKurseinheit.getId(), testBild.getName())).thenReturn(null);
        when(kursMaterialService.findBildByNameAndKursId(testKurs.getId(), testBild.getName())).thenReturn(null);
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName())
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertThat(exception.getReason()).contains("Bild nicht gefunden");
        
        verify(kursMaterialService).findBildByNameAndKurseinheitId(testKurseinheit.getId(), testBild.getName());
        verify(kursMaterialService).findBildByNameAndKursId(testKurs.getId(), testBild.getName());
    }
    
    @Test
    @DisplayName("getKurseinheitBild sollte einen 403 Fehler zurückgeben, wenn der Nutzer weder Student noch Kursbetreuer ist")
    void getKurseinheitBild_ShouldThrow403_WhenUserNotStudentOrTeacher() {
        // Arrange - ein Nutzer, der weder Student noch Kursbetreuer ist
        NutzerDTO otherUser = new NutzerDTO();
        otherUser.setId(6L);
        
        when(kurseinheitService.getKurseinheitById(testKurseinheit.getId())).thenReturn(testKurseinheit);
        when(kursService.getKursById(testKurs.getId())).thenReturn(testKurs);
        when(nutzerService.getAuthenticatedNutzer()).thenReturn(otherUser);
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getKurseinheitBild(testKurseinheit.getId(), testBild.getName())
        );
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertThat(exception.getReason()).contains("Kein Zugriff auf dieses Bild");
        
        verify(kursMaterialService, never()).findBildByNameAndKurseinheitId(anyLong(), anyString());
    }
}