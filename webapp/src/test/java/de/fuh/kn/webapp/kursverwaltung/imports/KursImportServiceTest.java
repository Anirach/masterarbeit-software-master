package de.fuh.kn.webapp.kursverwaltung.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.TeilaufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.*;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests für KursImportService.
 * Testet die Import-Funktionalität für Kurse aus JSON und ZIP-Dateien.
 */
@ExtendWith(MockitoExtension.class)
class KursImportServiceTest {

    @Mock
    private KursService kursService;

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private KursMaterialService kursMaterialService;

    @Mock
    private AufgabeService aufgabeService;

    @Mock
    private KursExportMapper kursExportMapper;

    @Mock
    private AufgabeExportMapper aufgabeExportMapper;

    @Mock
    private KursMaterialExportMapper kursMaterialExportMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private KursImportService kursImportService;

    private KursExportDTO testKursExport;
    private KursDTO testKursDTO;
    private KurseinheitExportDTO testKurseinheitExport;
    private AufgabeExportDTO testAufgabeExport;
    private KursMaterialExportDTO testMaterialExport;

    @BeforeEach
    void setUp() {
        // Test-Kursmaterial erstellen
        testMaterialExport = new KursMaterialExportDTO();
        testMaterialExport.setName("test.pdf");
        testMaterialExport.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
        testMaterialExport.setMimeType("application/pdf");
        testMaterialExport.setInhaltBase64(Base64.getEncoder().encodeToString("Test PDF Content".getBytes()));
        
        // Test-Kurseinheit erstellen
        testKurseinheitExport = new KurseinheitExportDTO();
        testKurseinheitExport.setId(1L);
        testKurseinheitExport.setName("Test Kurseinheit");
        testKurseinheitExport.setReihenfolge(1);
        testKurseinheitExport.setKursMaterialien(new ArrayList<>());
        
        // Test-Teilaufgabe erstellen
        TeilaufgabeExportDTO teilaufgabeExport = new TeilaufgabeExportDTO();
        teilaufgabeExport.setReihenfolge(1);
        teilaufgabeExport.setAufgabenstellungMarkdown("Test Aufgabenstellung");
        teilaufgabeExport.setMusterloesungFelder(new HashMap<>());
        
        // Test-Aufgabe erstellen
        testAufgabeExport = new AufgabeExportDTO();
        testAufgabeExport.setTitel("Test Aufgabe");
        testAufgabeExport.setAufgabenText("Test Aufgabentext");
        testAufgabeExport.setKurseinheitId(1L);
        testAufgabeExport.setTeilaufgaben(Arrays.asList(teilaufgabeExport));
        
        // Test-Kurs Export DTO erstellen
        testKursExport = new KursExportDTO();
        testKursExport.setName("Test Kurs");
        testKursExport.setKurseinheiten(Arrays.asList(testKurseinheitExport));
        testKursExport.setAufgaben(Arrays.asList(testAufgabeExport));
        testKursExport.setKursMaterialien(Arrays.asList(testMaterialExport));
        
        // Test-Kurs DTO erstellen
        testKursDTO = new KursDTO();
        testKursDTO.setName("Test Kurs");
    }

    @Test
    void importiereAusJson_MitMultipartFile_ErfolgreichImportiert() throws IOException {
        // Arrange
        String jsonContent = objectMapper.writeValueAsString(testKursExport);
        MultipartFile jsonFile = mock(MultipartFile.class);
        when(jsonFile.isEmpty()).thenReturn(false);
        when(jsonFile.getBytes()).thenReturn(jsonContent.getBytes());
        
        // Mock-Setup für den Import
        setupMocksForImport();
        
        // Act
        KursDTO result = kursImportService.importiereAusJson(jsonFile);
        
        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(jsonFile).getBytes();
        verify(kursService).erstelleKurs(any(KursDTO.class));
    }

    @Test
    void importiereAusJson_MitLeeremFile_ThrowsIllegalArgumentException() {
        // Arrange
        MultipartFile jsonFile = mock(MultipartFile.class);
        when(jsonFile.isEmpty()).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            kursImportService.importiereAusJson(jsonFile));
    }

    @Test
    void importiereAusJson_MitJsonString_ErfolgreichImportiert() throws IOException {
        // Arrange
        setupMocksForImport();
        String jsonString = objectMapper.writeValueAsString(testKursExport);
        
        // Act
        KursDTO result = kursImportService.importiereAusJson(jsonString);
        
        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(kursService).erstelleKurs(any(KursDTO.class));
        verify(kurseinheitService).erstelleKurseinheit(any(KurseinheitDTO.class));
        verify(kursMaterialService).erstelleKursMaterial(any(KursMaterialDTO.class));
        verify(aufgabeService).erstelleAufgabe(any(AufgabeDto.class));
    }

    @Test
    void importiereAusZip_MitMultipartFile_ErfolgreichImportiert() throws IOException {
        // Arrange
        byte[] zipData = createTestZipData();
        MultipartFile zipFile = mock(MultipartFile.class);
        when(zipFile.isEmpty()).thenReturn(false);
        when(zipFile.getBytes()).thenReturn(zipData);
        
        // Mock-Setup für den Import
        setupMocksForImport();
        
        // Act
        KursDTO result = kursImportService.importiereAusZip(zipFile);
        
        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(zipFile).getBytes();
        verify(kursService).erstelleKurs(any(KursDTO.class));
    }

    @Test
    void importiereAusZip_MitLeeremFile_ThrowsIllegalArgumentException() {
        // Arrange
        MultipartFile zipFile = mock(MultipartFile.class);
        when(zipFile.isEmpty()).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            kursImportService.importiereAusZip(zipFile));
    }

    @Test
    void speichereImportiertenKurs_ErfolgreichGespeichert() {
        // Arrange
        setupMocksForImport();
        
        // Act
        KursDTO result = kursImportService.speichereImportiertenKurs(testKursExport);
        
        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Test Kurs", result.getName());
        
        // Verify Kurs wurde erstellt
        ArgumentCaptor<KursDTO> kursCaptor = ArgumentCaptor.forClass(KursDTO.class);
        verify(kursService).erstelleKurs(kursCaptor.capture());
        assertNull(kursCaptor.getValue().getId()); // ID sollte zurückgesetzt sein
        
        // Verify Kurseinheit wurde erstellt
        ArgumentCaptor<KurseinheitDTO> kurseinheitCaptor = ArgumentCaptor.forClass(KurseinheitDTO.class);
        verify(kurseinheitService).erstelleKurseinheit(kurseinheitCaptor.capture());
        assertEquals(100L, kurseinheitCaptor.getValue().getKursId());
        
        // Verify Aufgabe wurde erstellt mit korrekter Kurseinheit-ID
        ArgumentCaptor<AufgabeDto> aufgabeCaptor = ArgumentCaptor.forClass(AufgabeDto.class);
        verify(aufgabeService).erstelleAufgabe(aufgabeCaptor.capture());
        assertEquals(200L, aufgabeCaptor.getValue().getKurseinheitId());
    }

    @Test
    void speichereImportiertenKurs_OhneKurseinheiten_ÜberspringtAufgaben() {
        // Arrange
        testKursExport.setKurseinheiten(new ArrayList<>());
        
        when(kursExportMapper.fromExportDto(testKursExport)).thenReturn(testKursDTO);
        
        KursDTO gespeicherterKurs = new KursDTO();
        gespeicherterKurs.setId(100L);
        when(kursService.erstelleKurs(any(KursDTO.class))).thenReturn(gespeicherterKurs);
        
        AufgabeDto aufgabeDto = new AufgabeDto();
        when(aufgabeExportMapper.fromExportDto(any(AufgabeExportDTO.class))).thenReturn(aufgabeDto);
        
        KursMaterialDTO materialDTO = new KursMaterialDTO();
        when(kursMaterialExportMapper.fromExportDto(any(KursMaterialExportDTO.class))).thenReturn(materialDTO);
        
        KursMaterialDTO gespeichertesMaterial = new KursMaterialDTO();
        when(kursMaterialService.erstelleKursMaterial(any(KursMaterialDTO.class))).thenReturn(gespeichertesMaterial);
        
        // Act
        KursDTO result = kursImportService.speichereImportiertenKurs(testKursExport);
        
        // Assert
        assertNotNull(result);
        verify(aufgabeService, never()).erstelleAufgabe(any());
    }

    @Test
    void isJsonFile_MitJsonFile_ReturnsTrue() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/json");
        when(file.getOriginalFilename()).thenReturn("test.json");
        
        // Act
        boolean result = kursImportService.isJsonFile(file);
        
        // Assert
        assertTrue(result);
    }

    @Test
    void isJsonFile_MitJsonEndung_ReturnsTrue() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getOriginalFilename()).thenReturn("test.JSON");
        
        // Act
        boolean result = kursImportService.isJsonFile(file);
        
        // Assert
        assertTrue(result);
    }

    @Test
    void isJsonFile_MitAnderemFile_ReturnsFalse() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        
        // Act
        boolean result = kursImportService.isJsonFile(file);
        
        // Assert
        assertFalse(result);
    }

    @Test
    void isZipFile_MitZipFile_ReturnsTrue() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/zip");
        when(file.getOriginalFilename()).thenReturn("test.zip");
        
        // Act
        boolean result = kursImportService.isZipFile(file);
        
        // Assert
        assertTrue(result);
    }

    @Test
    void isZipFile_MitZipEndung_ReturnsTrue() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/octet-stream");
        when(file.getOriginalFilename()).thenReturn("test.ZIP");
        
        // Act
        boolean result = kursImportService.isZipFile(file);
        
        // Assert
        assertTrue(result);
    }

    @Test
    void isZipFile_MitAnderemFile_ReturnsFalse() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/json");
        when(file.getOriginalFilename()).thenReturn("test.json");
        
        // Act
        boolean result = kursImportService.isZipFile(file);
        
        // Assert
        assertFalse(result);
    }

    // Helper-Methode zum Erstellen von Test-ZIP-Daten
    private byte[] createTestZipData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // kurs.json hinzufügen
            ZipEntry kursEntry = new ZipEntry("kurs.json");
            zos.putNextEntry(kursEntry);
            String kursJson = objectMapper.writeValueAsString(testKursExport);
            zos.write(kursJson.getBytes());
            zos.closeEntry();
            
            // Material-Datei hinzufügen
            ZipEntry materialEntry = new ZipEntry("test.pdf");
            zos.putNextEntry(materialEntry);
            zos.write("Test PDF Content".getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
    
    // Helper-Methode zum Setup der Mocks für den Import
    private void setupMocksForImport() {
        // Kurs-Mocks
        when(kursExportMapper.fromExportDto(any(KursExportDTO.class))).thenReturn(testKursDTO);
        KursDTO gespeicherterKurs = new KursDTO();
        gespeicherterKurs.setId(100L);
        gespeicherterKurs.setName("Test Kurs");
        when(kursService.erstelleKurs(any(KursDTO.class))).thenReturn(gespeicherterKurs);
        
        // Kurseinheit-Mocks
        KurseinheitDTO gespeicherteKurseinheit = new KurseinheitDTO();
        gespeicherteKurseinheit.setId(200L);
        gespeicherteKurseinheit.setName("Test Kurseinheit");
        when(kurseinheitService.erstelleKurseinheit(any(KurseinheitDTO.class))).thenReturn(gespeicherteKurseinheit);
        
        // Kursmaterial-Mocks
        KursMaterialDTO gespeichertesMaterial = new KursMaterialDTO();
        gespeichertesMaterial.setId(300L);
        gespeichertesMaterial.setName("test.pdf");
        gespeichertesMaterial.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
        when(kursMaterialService.erstelleKursMaterial(any(KursMaterialDTO.class))).thenReturn(gespeichertesMaterial);
        
        KursMaterialDTO materialDTO = new KursMaterialDTO();
        materialDTO.setName("test.pdf");
        when(kursMaterialExportMapper.fromExportDto(any(KursMaterialExportDTO.class))).thenReturn(materialDTO);
        
        // Aufgabe-Mocks
        AufgabeDto aufgabeDto = new AufgabeDto();
        aufgabeDto.setTitel("Test Aufgabe");
        aufgabeDto.setKurseinheitId(1L);
        aufgabeDto.setTeilaufgaben(new ArrayList<>());
        when(aufgabeExportMapper.fromExportDto(any(AufgabeExportDTO.class))).thenReturn(aufgabeDto);
        
        AufgabeDto gespeicherteAufgabe = new AufgabeDto();
        gespeicherteAufgabe.setId(400L);
        gespeicherteAufgabe.setTitel("Test Aufgabe");
        gespeicherteAufgabe.setTeilaufgaben(Arrays.asList(new TeilaufgabeDto()));
        when(aufgabeService.erstelleAufgabe(any(AufgabeDto.class))).thenReturn(gespeicherteAufgabe);
    }
}