package de.fuh.kn.webapp.aufgabenverwaltung.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.llm.service.LlmAufgabenImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AufgabenImportServiceTest {

    @Mock
    private AufgabeService aufgabeService;
    
    @Mock
    private TeilaufgabeService teilaufgabeService;
    
    @Mock
    private AufgabeExportMapper aufgabeExportMapper;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private LlmAufgabenImportService llmAufgabenImportService;
    
    @InjectMocks
    private AufgabenImportService aufgabenImportService;
    
    private AufgabeDto testAufgabe;
    private TeilaufgabeDto testTeilaufgabe;
    private AufgabeExportDTO testAufgabeExport;
    
    @BeforeEach
    void setUp() {
        testTeilaufgabe = new TeilaufgabeDto();
        testTeilaufgabe.setId(1L);
        testTeilaufgabe.setReihenfolge(1);
        testTeilaufgabe.setAufgabenstellungMarkdown("Test Teilaufgabe");
        testTeilaufgabe.setMusterloesungFelder(Map.of("field1", "solution1"));
        testTeilaufgabe.setMusterloesungBewertungshinweise("Test Bewertungshinweise");
        
        testAufgabe = new AufgabeDto();
        testAufgabe.setId(1L);
        testAufgabe.setTitel("Test Aufgabe");
        testAufgabe.setKurseinheitId(100L);
        testAufgabe.setTeilaufgaben(Arrays.asList(testTeilaufgabe));
        
        testAufgabeExport = new AufgabeExportDTO();
        testAufgabeExport.setTitel("Test Export Aufgabe");
    }
    
    @Test
    @DisplayName("isPdfFile sollte true zurückgeben für PDF mit korrektem Content-Type")
    void testIsPdfFile_ShouldReturnTrue_WhenValidPdfContentType() {
        // Arrange
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "PDF content".getBytes()
        );
        
        // Act
        boolean result = aufgabenImportService.isPdfFile(pdfFile);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    @DisplayName("isPdfFile sollte true zurückgeben für Datei mit PDF-Endung")
    void testIsPdfFile_ShouldReturnTrue_WhenValidPdfExtension() {
        // Arrange
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", "test.pdf", "application/octet-stream", "PDF content".getBytes()
        );
        
        // Act
        boolean result = aufgabenImportService.isPdfFile(pdfFile);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    @DisplayName("isPdfFile sollte false zurückgeben für null oder leere Datei")
    void testIsPdfFile_ShouldReturnFalse_WhenFileIsNullOrEmpty() {
        // Test null file
        assertFalse(aufgabenImportService.isPdfFile(null));
        
        // Test empty file
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "", new byte[0]);
        assertFalse(aufgabenImportService.isPdfFile(emptyFile));
    }
    
    @Test
    @DisplayName("isPdfFile sollte false zurückgeben für Nicht-PDF-Datei")
    void testIsPdfFile_ShouldReturnFalse_WhenNotPdfFile() {
        // Arrange
        MockMultipartFile textFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "Text content".getBytes()
        );
        
        // Act
        boolean result = aufgabenImportService.isPdfFile(textFile);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    @DisplayName("isJsonFile sollte true zurückgeben für JSON mit korrektem Content-Type")
    void testIsJsonFile_ShouldReturnTrue_WhenValidJsonContentType() {
        // Arrange
        MockMultipartFile jsonFile = new MockMultipartFile(
            "file", "test.json", "application/json", "{\"test\": \"value\"}".getBytes()
        );
        
        // Act
        boolean result = aufgabenImportService.isJsonFile(jsonFile);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    @DisplayName("isJsonFile sollte true zurückgeben für Datei mit JSON-Endung")
    void testIsJsonFile_ShouldReturnTrue_WhenValidJsonExtension() {
        // Arrange
        MockMultipartFile jsonFile = new MockMultipartFile(
            "file", "test.json", "application/octet-stream", "{\"test\": \"value\"}".getBytes()
        );
        
        // Act
        boolean result = aufgabenImportService.isJsonFile(jsonFile);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    @DisplayName("isJsonFile sollte false zurückgeben für null oder leere Datei")
    void testIsJsonFile_ShouldReturnFalse_WhenFileIsNullOrEmpty() {
        // Test null file
        assertFalse(aufgabenImportService.isJsonFile(null));
        
        // Test empty file
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "", new byte[0]);
        assertFalse(aufgabenImportService.isJsonFile(emptyFile));
    }
    
    @Test
    @DisplayName("importAufgabenAusPdf sollte Exception werfen für null Datei")
    void testImportAufgabenAusPdf_ShouldThrowException_WhenFileIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importAufgabenAusPdf(null, 1L)
        );
        
        assertEquals("Die PDF-Datei darf nicht leer sein.", exception.getMessage());
        verifyNoInteractions(llmAufgabenImportService);
    }
    
    @Test
    @DisplayName("importAufgabenAusPdf sollte Exception werfen für leere Datei")
    void testImportAufgabenAusPdf_ShouldThrowException_WhenFileIsEmpty() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "", new byte[0]);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importAufgabenAusPdf(emptyFile, 1L)
        );
        
        assertEquals("Die PDF-Datei darf nicht leer sein.", exception.getMessage());
        verifyNoInteractions(llmAufgabenImportService);
    }
    
    @Test
    @DisplayName("importAufgabenAusPdf sollte Exception werfen für Nicht-PDF-Datei")
    void testImportAufgabenAusPdf_ShouldThrowException_WhenFileIsNotPdf() {
        // Arrange
        MockMultipartFile textFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "Text content".getBytes()
        );
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importAufgabenAusPdf(textFile, 1L)
        );
        
        assertEquals("Die hochgeladene Datei ist keine PDF-Datei.", exception.getMessage());
        verifyNoInteractions(llmAufgabenImportService);
    }
    
    @Test
    @DisplayName("importAufgabenAusPdfPair sollte Exception werfen für null Assignment-Datei")
    void testImportAufgabenAusPdfPair_ShouldThrowException_WhenAssignmentFileIsNull() {
        // Arrange
        MockMultipartFile solutionFile = new MockMultipartFile(
            "solution", "solution.pdf", "application/pdf", "Solution content".getBytes()
        );
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importAufgabenAusPdfPair(null, solutionFile, 1L)
        );
        
        assertEquals("Die Aufgaben-PDF-Datei darf nicht leer sein.", exception.getMessage());
        verifyNoInteractions(llmAufgabenImportService);
    }
    
    @Test
    @DisplayName("importAufgabenAusPdfPair sollte Exception werfen für Nicht-PDF Assignment-Datei")
    void testImportAufgabenAusPdfPair_ShouldThrowException_WhenAssignmentFileIsNotPdf() {
        // Arrange
        MockMultipartFile assignmentFile = new MockMultipartFile(
            "assignment", "assignment.txt", "text/plain", "Assignment content".getBytes()
        );
        MockMultipartFile solutionFile = new MockMultipartFile(
            "solution", "solution.pdf", "application/pdf", "Solution content".getBytes()
        );
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importAufgabenAusPdfPair(assignmentFile, solutionFile, 1L)
        );
        
        assertEquals("Die hochgeladene Aufgabendatei ist keine PDF-Datei.", exception.getMessage());
        verifyNoInteractions(llmAufgabenImportService);
    }
    
    @Test
    @DisplayName("importAufgabenAusPdfPair sollte Exception werfen für Nicht-PDF Solution-Datei")
    void testImportAufgabenAusPdfPair_ShouldThrowException_WhenSolutionFileIsNotPdf() {
        // Arrange
        MockMultipartFile assignmentFile = new MockMultipartFile(
            "assignment", "assignment.pdf", "application/pdf", "Assignment content".getBytes()
        );
        MockMultipartFile solutionFile = new MockMultipartFile(
            "solution", "solution.txt", "text/plain", "Solution content".getBytes()
        );
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importAufgabenAusPdfPair(assignmentFile, solutionFile, 1L)
        );
        
        assertEquals("Die hochgeladene Lösungsdatei ist keine PDF-Datei.", exception.getMessage());
        verifyNoInteractions(llmAufgabenImportService);
    }
    
    @Test
    @DisplayName("importiereAufgabenAusJson sollte Exception werfen für null Datei")
    void testImportiereAufgabenAusJson_ShouldThrowException_WhenFileIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importiereAufgabenAusJson((MultipartFile) null, 1L)
        );
        
        assertEquals("Die JSON-Datei darf nicht leer sein.", exception.getMessage());
    }
    
    @Test
    @DisplayName("importiereAufgabenAusJson sollte Exception werfen für leere Datei")
    void testImportiereAufgabenAusJson_ShouldThrowException_WhenFileIsEmpty() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "", new byte[0]);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aufgabenImportService.importiereAufgabenAusJson(emptyFile, 1L)
        );
        
        assertEquals("Die JSON-Datei darf nicht leer sein.", exception.getMessage());
    }
    
    @Test
    @DisplayName("speichereImportierteAufgaben sollte Aufgaben korrekt speichern")
    void testSpeichereImportierteAufgaben_ShouldSaveAufgabenCorrectly() {
        // Arrange
        Long kurseinheitId = 100L;
        List<AufgabeExportDTO> aufgabenExport = Arrays.asList(testAufgabeExport);
        
        when(aufgabeExportMapper.fromExportDto(testAufgabeExport)).thenReturn(testAufgabe);
        when(aufgabeService.erstelleAufgabe(any(AufgabeDto.class))).thenReturn(testAufgabe);
        
        // Act
        List<AufgabeDto> result = aufgabenImportService.speichereImportierteAufgaben(aufgabenExport, kurseinheitId);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAufgabe.getTitel(), result.get(0).getTitel());
        
        verify(aufgabeExportMapper).fromExportDto(testAufgabeExport);
        verify(aufgabeService).erstelleAufgabe(any(AufgabeDto.class));
    }
    
    @Test
    @DisplayName("speichereImportierteAufgaben sollte IDs zurücksetzen und Kurseinheit-ID setzen")
    void testSpeichereImportierteAufgaben_ShouldResetIdsAndSetKurseinheitId() {
        // Arrange
        Long kurseinheitId = 100L;
        List<AufgabeExportDTO> aufgabenExport = Arrays.asList(testAufgabeExport);
        
        // Mock die Rückgabe, sodass wir prüfen können was übergeben wird
        when(aufgabeExportMapper.fromExportDto(testAufgabeExport)).thenReturn(testAufgabe);
        when(aufgabeService.erstelleAufgabe(any(AufgabeDto.class))).thenAnswer(invocation -> {
            AufgabeDto aufgabe = invocation.getArgument(0);
            // Prüfe, dass die ID zurückgesetzt wurde
            assertNull(aufgabe.getId());
            // Prüfe, dass die Kurseinheit-ID gesetzt wurde
            assertEquals(kurseinheitId, aufgabe.getKurseinheitId());
            // Prüfe, dass Teilaufgaben-IDs zurückgesetzt wurden
            if (aufgabe.getTeilaufgaben() != null) {
                aufgabe.getTeilaufgaben().forEach(ta -> assertNull(ta.getId()));
            }
            return testAufgabe;
        });
        
        // Act
        aufgabenImportService.speichereImportierteAufgaben(aufgabenExport, kurseinheitId);
        
        // Assert
        verify(aufgabeService).erstelleAufgabe(any(AufgabeDto.class));
    }
    
    @Test
    @DisplayName("speichereImportierteAufgaben sollte leere Liste zurückgeben für leere Eingabe")
    void testSpeichereImportierteAufgaben_ShouldReturnEmptyList_WhenInputIsEmpty() {
        // Arrange
        List<AufgabeExportDTO> aufgabenExport = Collections.emptyList();
        
        // Act
        List<AufgabeDto> result = aufgabenImportService.speichereImportierteAufgaben(aufgabenExport, 100L);
        
        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(aufgabeExportMapper, aufgabeService);
    }
}