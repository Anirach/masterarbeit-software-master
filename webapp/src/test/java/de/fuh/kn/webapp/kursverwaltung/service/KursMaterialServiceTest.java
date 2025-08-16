package de.fuh.kn.webapp.kursverwaltung.service;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialMapper;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import de.fuh.kn.webapp.persistence.repository.KursMaterialRepository;
import de.fuh.kn.webapp.persistence.repository.KursRepository;
import de.fuh.kn.webapp.persistence.repository.KurseinheitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für die KursMaterialService-Klasse.
 * Testet die Geschäftslogik für das Hochladen, Abrufen und Löschen von Kursmaterialien.
 */
@ExtendWith(MockitoExtension.class)
class KursMaterialServiceTest {

    @Mock
    private KursMaterialRepository kursMaterialRepository;

    @Mock
    private KursRepository kursRepository;

    @Mock
    private KurseinheitRepository kurseinheitRepository;

    @Mock
    private KursMaterialMapper kursMaterialMapper;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private KursMaterialService kursMaterialService;

    @Captor
    private ArgumentCaptor<KursMaterial> kursMaterialCaptor;

    private Kurs testKurs;
    private Kurseinheit testKurseinheit;
    private KursMaterial testKursMaterial;
    private KursMaterialDTO testKursMaterialDTO;
    private KursMaterial testBild;
    private KursMaterialDTO testBildDTO;
    private MockMultipartFile pdfFile;
    private MockMultipartFile imageFile;
    private MockMultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        // Testdaten anlegen
        testKurs = new Kurs();
        testKurs.setId(1L);
        testKurs.setName("Testprogrammierung");

        testKurseinheit = new Kurseinheit();
        testKurseinheit.setId(2L);
        testKurseinheit.setName("Testfall-Erstellung");
        testKurseinheit.setKurs(testKurs);

        testKursMaterial = new KursMaterial();
        testKursMaterial.setId(3L);
        testKursMaterial.setName("testdokument.pdf");
        testKursMaterial.setMimeType("application/pdf");
        testKursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        testKursMaterial.setKurs(testKurs);

        testKursMaterialDTO = new KursMaterialDTO();
        testKursMaterialDTO.setId(3L);
        testKursMaterialDTO.setName("testdokument.pdf");
        testKursMaterialDTO.setMimeType("application/pdf");
        testKursMaterialDTO.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);

        // Testbild
        testBild = new KursMaterial();
        testBild.setId(4L);
        testBild.setName("testbild.jpg");
        testBild.setMimeType("image/jpeg");
        testBild.setTyp(KursMaterial.KursMaterialTyp.BILD);
        testBild.setInhalt("bildinhalt".getBytes());

        testBildDTO = new KursMaterialDTO();
        testBildDTO.setId(4L);
        testBildDTO.setName("testbild.jpg");
        testBildDTO.setMimeType("image/jpeg");
        testBildDTO.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);
        testBildDTO.setInhalt("bildinhalt".getBytes());

        // Mock-Dateien erstellen
        byte[] pdfBytes = "PDF Test Content".getBytes();
        pdfFile = new MockMultipartFile(
                "file",
                "testdokument.pdf",
                "application/pdf",
                pdfBytes
        );

        byte[] imageBytes = "Image Test Content".getBytes();
        imageFile = new MockMultipartFile(
                "file",
                "testbild.jpg",
                "image/jpeg",
                imageBytes
        );

        byte[] invalidBytes = "Invalid Content".getBytes();
        invalidFile = new MockMultipartFile(
                "file",
                "ungueltig.xyz",
                "application/xyz",
                invalidBytes
        );
    }

    @Test
    @DisplayName("kursDateiHochladen sollte ein PDF-Dokument erfolgreich hochladen")
    void kursDateiHochladen_ShouldUploadPdfDocument_Successfully() throws IOException {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kursMaterialRepository.save(any(KursMaterial.class))).thenReturn(testKursMaterial);
        when(kursMaterialMapper.toDto(any(KursMaterial.class))).thenReturn(testKursMaterialDTO);

        // Act
        KursMaterialDTO result = kursMaterialService.kursDateiHochladen(1L, pdfFile);

        // Assert
        assertNotNull(result);
        assertEquals(testKursMaterialDTO, result);
        verify(kursRepository).findById(1L);
        verify(kursMaterialRepository).save(kursMaterialCaptor.capture());
        verify(kursMaterialMapper).toDto(any(KursMaterial.class));

        KursMaterial savedMaterial = kursMaterialCaptor.getValue();
        assertEquals("testdokument.pdf", savedMaterial.getName());
        assertEquals("application/pdf", savedMaterial.getMimeType());
        assertEquals(KursMaterial.KursMaterialTyp.DOKUMENT, savedMaterial.getTyp());
        assertEquals(testKurs, savedMaterial.getKurs());
        assertArrayEquals("PDF Test Content".getBytes(), savedMaterial.getInhalt());
    }

    @Test
    @DisplayName("kursDateiHochladen sollte ein Bild erfolgreich hochladen")
    void kursDateiHochladen_ShouldUploadImage_Successfully() throws IOException {
        // Arrange
        KursMaterial imageMaterial = new KursMaterial();
        imageMaterial.setId(4L);
        imageMaterial.setName("testbild.jpg");
        imageMaterial.setMimeType("image/jpeg");
        imageMaterial.setTyp(KursMaterial.KursMaterialTyp.BILD);
        imageMaterial.setKurs(testKurs);

        KursMaterialDTO imageDTO = new KursMaterialDTO();
        imageDTO.setId(4L);
        imageDTO.setName("testbild.jpg");
        imageDTO.setMimeType("image/jpeg");
        imageDTO.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);

        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kursMaterialRepository.save(any(KursMaterial.class))).thenReturn(imageMaterial);
        when(kursMaterialMapper.toDto(any(KursMaterial.class))).thenReturn(imageDTO);

        // Act
        KursMaterialDTO result = kursMaterialService.kursDateiHochladen(1L, imageFile);

        // Assert
        assertNotNull(result);
        assertEquals(imageDTO, result);
        verify(kursRepository).findById(1L);
        verify(kursMaterialRepository).save(kursMaterialCaptor.capture());
        verify(kursMaterialMapper).toDto(any(KursMaterial.class));

        KursMaterial savedMaterial = kursMaterialCaptor.getValue();
        assertEquals("testbild.jpg", savedMaterial.getName());
        assertEquals("image/jpeg", savedMaterial.getMimeType());
        assertEquals(KursMaterial.KursMaterialTyp.BILD, savedMaterial.getTyp());
    }

    @Test
    @DisplayName("kursDateiHochladen sollte eine Exception werfen, wenn der Kurs nicht existiert")
    void kursDateiHochladen_ShouldThrowException_WhenCourseNotExists() {
        // Arrange
        when(kursRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kursMaterialService.kursDateiHochladen(999L, pdfFile)
        );

        assertThat(exception.getMessage()).contains("Kurs mit ID 999 wurde nicht gefunden");
        verify(kursRepository).findById(999L);
        verify(kursMaterialRepository, never()).save(any(KursMaterial.class));
    }

    @Test
    @DisplayName("kursDateiHochladen sollte eine Exception werfen, wenn der Dateityp ungültig ist")
    void kursDateiHochladen_ShouldThrowException_WhenFileTypeInvalid() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kursMaterialService.kursDateiHochladen(1L, invalidFile)
        );

        assertThat(exception.getMessage()).contains("Dateityp nicht erlaubt");
        verify(kursRepository).findById(1L);
        verify(kursMaterialRepository, never()).save(any(KursMaterial.class));
    }

    @Test
    @DisplayName("kurseinheitDateiHochladen sollte ein Dokument erfolgreich hochladen")
    void kurseinheitDateiHochladen_ShouldUploadDocument_Successfully() throws IOException {
        // Arrange
        KursMaterial kurseinheitMaterial = new KursMaterial();
        kurseinheitMaterial.setId(5L);
        kurseinheitMaterial.setName("testdokument.pdf");
        kurseinheitMaterial.setMimeType("application/pdf");
        kurseinheitMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        kurseinheitMaterial.setKurseinheit(testKurseinheit);

        KursMaterialDTO kurseinheitDTO = new KursMaterialDTO();
        kurseinheitDTO.setId(5L);
        kurseinheitDTO.setName("testdokument.pdf");
        kurseinheitDTO.setMimeType("application/pdf");
        kurseinheitDTO.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);

        when(kurseinheitRepository.findById(2L)).thenReturn(Optional.of(testKurseinheit));
        when(kursMaterialRepository.save(any(KursMaterial.class))).thenReturn(kurseinheitMaterial);
        when(kursMaterialMapper.toDto(any(KursMaterial.class))).thenReturn(kurseinheitDTO);

        // Act
        KursMaterialDTO result = kursMaterialService.kurseinheitDateiHochladen(2L, pdfFile);

        // Assert
        assertNotNull(result);
        assertEquals(kurseinheitDTO, result);
        verify(kurseinheitRepository).findById(2L);
        verify(kursMaterialRepository).save(kursMaterialCaptor.capture());
        verify(kursMaterialMapper).toDto(any(KursMaterial.class));

        KursMaterial savedMaterial = kursMaterialCaptor.getValue();
        assertEquals("testdokument.pdf", savedMaterial.getName());
        assertEquals("application/pdf", savedMaterial.getMimeType());
        assertEquals(KursMaterial.KursMaterialTyp.DOKUMENT, savedMaterial.getTyp());
        assertEquals(testKurseinheit, savedMaterial.getKurseinheit());
    }

    @Test
    @DisplayName("kurseinheitDateiHochladen sollte eine Exception werfen, wenn die Kurseinheit nicht existiert")
    void kurseinheitDateiHochladen_ShouldThrowException_WhenCourseUnitNotExists() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kursMaterialService.kurseinheitDateiHochladen(999L, pdfFile)
        );

        assertThat(exception.getMessage()).contains("Kurseinheit mit ID 999 wurde nicht gefunden");
        verify(kurseinheitRepository).findById(999L);
        verify(kursMaterialRepository, never()).save(any(KursMaterial.class));
    }

    @Test
    @DisplayName("loescheKursMaterial sollte ein Kursmaterial erfolgreich löschen")
    void loescheKursMaterial_ShouldDeleteMaterial_Successfully() {
        // Arrange
        when(kursMaterialRepository.findById(3L)).thenReturn(Optional.of(testKursMaterial));
        doNothing().when(kursMaterialRepository).deleteById(3L);

        // Act
        kursMaterialService.loescheKursMaterial(3L);

        // Assert
        verify(kursMaterialRepository).findById(3L);
        verify(kursMaterialRepository).deleteById(3L);
    }

    @Test
    @DisplayName("loescheKursMaterial sollte eine Exception werfen, wenn das Material nicht existiert")
    void loescheKursMaterial_ShouldThrowException_WhenMaterialNotExists() {
        // Arrange
        when(kursMaterialRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kursMaterialService.loescheKursMaterial(999L)
        );

        assertThat(exception.getMessage()).contains("Kursmaterial mit ID 999 existiert nicht");
        verify(kursMaterialRepository).findById(999L);
        verify(kursMaterialRepository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getKursMaterialById sollte ein Kursmaterial zurückgeben, wenn es existiert")
    void getKursMaterialById_ShouldReturnMaterial_WhenExists() {
        // Arrange
        when(kursMaterialRepository.findById(3L)).thenReturn(Optional.of(testKursMaterial));
        when(kursMaterialMapper.toDto(testKursMaterial)).thenReturn(testKursMaterialDTO);

        // Act
        KursMaterialDTO result = kursMaterialService.getKursMaterialById(3L);

        // Assert
        assertNotNull(result);
        assertEquals(testKursMaterialDTO, result);
        verify(kursMaterialRepository).findById(3L);
        verify(kursMaterialMapper).toDto(testKursMaterial);
    }

    @Test
    @DisplayName("getKursMaterialById sollte null zurückgeben, wenn kein Material mit der ID existiert")
    void getKursMaterialById_ShouldReturnNull_WhenNotExists() {
        // Arrange
        when(kursMaterialRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        KursMaterialDTO result = kursMaterialService.getKursMaterialById(999L);

        // Assert
        assertNull(result);
        verify(kursMaterialRepository).findById(999L);
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findBildByNameAndKursId sollte ein Bild zurückgeben, wenn es existiert")
    void findBildByNameAndKursId_ShouldReturnImage_WhenExists() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kursMaterialRepository.findImageByNameAndKurs("testbild.jpg", testKurs)).thenReturn(Optional.of(testBild));
        when(kursMaterialMapper.toDto(testBild)).thenReturn(testBildDTO);

        // Act
        KursMaterialDTO result = kursMaterialService.findBildByNameAndKursId(1L, "testbild.jpg");

        // Assert
        assertNotNull(result);
        assertEquals(testBildDTO, result);
        verify(kursRepository).findById(1L);
        verify(kursMaterialRepository).findImageByNameAndKurs("testbild.jpg", testKurs);
        verify(kursMaterialMapper).toDto(testBild);
    }

    @Test
    @DisplayName("findBildByNameAndKursId sollte null zurückgeben, wenn der Kurs nicht existiert")
    void findBildByNameAndKursId_ShouldReturnNull_WhenKursNotExists() {
        // Arrange
        when(kursRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        KursMaterialDTO result = kursMaterialService.findBildByNameAndKursId(999L, "testbild.jpg");

        // Assert
        assertNull(result);
        verify(kursRepository).findById(999L);
        verify(kursMaterialRepository, never()).findImageByNameAndKurs(anyString(), any(Kurs.class));
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findBildByNameAndKursId sollte null zurückgeben, wenn das Bild nicht existiert")
    void findBildByNameAndKursId_ShouldReturnNull_WhenImageNotExists() {
        // Arrange
        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kursMaterialRepository.findImageByNameAndKurs("nichtexistierendesbild.jpg", testKurs)).thenReturn(Optional.empty());

        // Act
        KursMaterialDTO result = kursMaterialService.findBildByNameAndKursId(1L, "nichtexistierendesbild.jpg");

        // Assert
        assertNull(result);
        verify(kursRepository).findById(1L);
        verify(kursMaterialRepository).findImageByNameAndKurs("nichtexistierendesbild.jpg", testKurs);
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findBildByNameAndKurseinheitId sollte ein Bild zurückgeben, wenn es existiert")
    void findBildByNameAndKurseinheitId_ShouldReturnImage_WhenExists() {
        // Arrange
        when(kurseinheitRepository.findById(2L)).thenReturn(Optional.of(testKurseinheit));
        when(kursMaterialRepository.findImageByNameAndKurseinheit("testbild.jpg", testKurseinheit)).thenReturn(Optional.of(testBild));
        when(kursMaterialMapper.toDto(testBild)).thenReturn(testBildDTO);

        // Act
        KursMaterialDTO result = kursMaterialService.findBildByNameAndKurseinheitId(2L, "testbild.jpg");

        // Assert
        assertNotNull(result);
        assertEquals(testBildDTO, result);
        verify(kurseinheitRepository).findById(2L);
        verify(kursMaterialRepository).findImageByNameAndKurseinheit("testbild.jpg", testKurseinheit);
        verify(kursMaterialMapper).toDto(testBild);
    }

    @Test
    @DisplayName("findBildByNameAndKurseinheitId sollte null zurückgeben, wenn die Kurseinheit nicht existiert")
    void findBildByNameAndKurseinheitId_ShouldReturnNull_WhenKurseinheitNotExists() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        KursMaterialDTO result = kursMaterialService.findBildByNameAndKurseinheitId(999L, "testbild.jpg");

        // Assert
        assertNull(result);
        verify(kurseinheitRepository).findById(999L);
        verify(kursMaterialRepository, never()).findImageByNameAndKurseinheit(anyString(), any(Kurseinheit.class));
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findAllBilderByKurseinheitId sollte eine Liste von Bildern zurückgeben")
    void findAllBilderByKurseinheitId_ShouldReturnImageList() {
        // Arrange
        KursMaterial bild1 = new KursMaterial();
        bild1.setId(4L);
        bild1.setName("bild1.jpg");
        bild1.setTyp(KursMaterial.KursMaterialTyp.BILD);

        KursMaterial bild2 = new KursMaterial();
        bild2.setId(5L);
        bild2.setName("bild2.jpg");
        bild2.setTyp(KursMaterial.KursMaterialTyp.BILD);

        KursMaterialDTO bildDTO1 = new KursMaterialDTO();
        bildDTO1.setId(4L);
        bildDTO1.setName("bild1.jpg");
        bildDTO1.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);

        KursMaterialDTO bildDTO2 = new KursMaterialDTO();
        bildDTO2.setId(5L);
        bildDTO2.setName("bild2.jpg");
        bildDTO2.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);

        when(kurseinheitRepository.findById(2L)).thenReturn(Optional.of(testKurseinheit));
        when(kursMaterialRepository.findByKurseinheitAndTyp(testKurseinheit, KursMaterial.KursMaterialTyp.BILD))
                .thenReturn(List.of(bild1, bild2));
        when(kursMaterialMapper.toDto(bild1)).thenReturn(bildDTO1);
        when(kursMaterialMapper.toDto(bild2)).thenReturn(bildDTO2);

        // Act
        List<KursMaterialDTO> result = kursMaterialService.findAllBilderByKurseinheitId(2L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertThat(result).containsExactly(bildDTO1, bildDTO2);
        verify(kurseinheitRepository).findById(2L);
        verify(kursMaterialRepository).findByKurseinheitAndTyp(testKurseinheit, KursMaterial.KursMaterialTyp.BILD);
        verify(kursMaterialMapper, times(2)).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findAllBilderByKurseinheitId sollte eine leere Liste zurückgeben, wenn die Kurseinheit nicht existiert")
    void findAllBilderByKurseinheitId_ShouldReturnEmptyList_WhenKurseinheitNotExists() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        List<KursMaterialDTO> result = kursMaterialService.findAllBilderByKurseinheitId(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(kurseinheitRepository).findById(999L);
        verify(kursMaterialRepository, never()).findByKurseinheitAndTyp(any(), any());
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findAllBilderByKursId sollte eine Liste von Bildern zurückgeben")
    void findAllBilderByKursId_ShouldReturnImageList() {
        // Arrange
        KursMaterial bild1 = new KursMaterial();
        bild1.setId(4L);
        bild1.setName("bild1.jpg");
        bild1.setTyp(KursMaterial.KursMaterialTyp.BILD);

        KursMaterial bild2 = new KursMaterial();
        bild2.setId(5L);
        bild2.setName("bild2.jpg");
        bild2.setTyp(KursMaterial.KursMaterialTyp.BILD);

        KursMaterialDTO bildDTO1 = new KursMaterialDTO();
        bildDTO1.setId(4L);
        bildDTO1.setName("bild1.jpg");
        bildDTO1.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);

        KursMaterialDTO bildDTO2 = new KursMaterialDTO();
        bildDTO2.setId(5L);
        bildDTO2.setName("bild2.jpg");
        bildDTO2.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);

        when(kursRepository.findById(1L)).thenReturn(Optional.of(testKurs));
        when(kursMaterialRepository.findByKursAndTyp(testKurs, KursMaterial.KursMaterialTyp.BILD))
                .thenReturn(List.of(bild1, bild2));
        when(kursMaterialMapper.toDto(bild1)).thenReturn(bildDTO1);
        when(kursMaterialMapper.toDto(bild2)).thenReturn(bildDTO2);

        // Act
        List<KursMaterialDTO> result = kursMaterialService.findAllBilderByKursId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertThat(result).containsExactly(bildDTO1, bildDTO2);
        verify(kursRepository).findById(1L);
        verify(kursMaterialRepository).findByKursAndTyp(testKurs, KursMaterial.KursMaterialTyp.BILD);
        verify(kursMaterialMapper, times(2)).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findAllBilderByKursId sollte eine leere Liste zurückgeben, wenn der Kurs nicht existiert")
    void findAllBilderByKursId_ShouldReturnEmptyList_WhenKursNotExists() {
        // Arrange
        when(kursRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        List<KursMaterialDTO> result = kursMaterialService.findAllBilderByKursId(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(kursRepository).findById(999L);
        verify(kursMaterialRepository, never()).findByKursAndTyp(any(), any());
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }

    @Test
    @DisplayName("findAllBilderForMarkdownEditor sollte Bilder aus Kurseinheit und Kurs zurückgeben")
    void findAllBilderForMarkdownEditor_ShouldReturnImagesFromKurseinheitAndKurs() {
        // Arrange
        KursMaterial kurseinheitBild = new KursMaterial();
        kurseinheitBild.setId(4L);
        kurseinheitBild.setName("kurseinheitBild.jpg");
        kurseinheitBild.setTyp(KursMaterial.KursMaterialTyp.BILD);
        kurseinheitBild.setKurseinheit(testKurseinheit);

        KursMaterial kursBild = new KursMaterial();
        kursBild.setId(5L);
        kursBild.setName("kursBild.jpg");
        kursBild.setTyp(KursMaterial.KursMaterialTyp.BILD);
        kursBild.setKurs(testKurs);

        KursMaterialDTO kurseinheitBildDTO = new KursMaterialDTO();
        kurseinheitBildDTO.setId(4L);
        kurseinheitBildDTO.setName("kurseinheitBild.jpg");
        kurseinheitBildDTO.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);
        kurseinheitBildDTO.setKurseinheitId(2L);

        KursMaterialDTO kursBildDTO = new KursMaterialDTO();
        kursBildDTO.setId(5L);
        kursBildDTO.setName("kursBild.jpg");
        kursBildDTO.setTyp(KursMaterialDTO.KursMaterialTyp.BILD);
        kursBildDTO.setKursId(1L);

        when(kurseinheitRepository.findById(2L)).thenReturn(Optional.of(testKurseinheit));
        
        // Direktes Mock für findAllBilderByKurseinheitId
        List<KursMaterialDTO> kurseinheitBilder = List.of(kurseinheitBildDTO);
        
        // Direktes Mock für findAllBilderByKursId
        List<KursMaterialDTO> kursBilder = List.of(kursBildDTO);
        
        // Wir verwenden Spies, um Methodenaufrufe innerhalb der Klasse zu mocken
        KursMaterialService spyService = spy(kursMaterialService);
        doReturn(kurseinheitBilder).when(spyService).findAllBilderByKurseinheitId(anyLong());
        doReturn(kursBilder).when(spyService).findAllBilderByKursId(anyLong());

        // Act
        List<KursMaterialDTO> result = spyService.findAllBilderForMarkdownEditor(2L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertThat(result).contains(kurseinheitBildDTO, kursBildDTO);
        verify(kurseinheitRepository).findById(2L);
        verify(spyService).findAllBilderByKurseinheitId(testKurseinheit.getId());
        verify(spyService).findAllBilderByKursId(testKurs.getId());
    }

    @Test
    @DisplayName("findAllBilderForMarkdownEditor sollte eine leere Liste zurückgeben, wenn die Kurseinheit nicht existiert")
    void findAllBilderForMarkdownEditor_ShouldReturnEmptyList_WhenKurseinheitNotExists() {
        // Arrange
        when(kurseinheitRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        List<KursMaterialDTO> result = kursMaterialService.findAllBilderForMarkdownEditor(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(kurseinheitRepository).findById(999L);
        verify(kursMaterialRepository, never()).findByKurseinheitAndTyp(any(), any());
        verify(kursMaterialRepository, never()).findByKursAndTyp(any(), any());
        verify(kursMaterialMapper, never()).toDto(any(KursMaterial.class));
    }
}