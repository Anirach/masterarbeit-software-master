package de.fuh.kn.webapp.kursverwaltung.dto.export;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KursMaterialExportMapperTest {

    private KursMaterialExportMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(KursMaterialExportMapper.class);
    }

    @Test
    @DisplayName("toExportDto - Konvertiert KursMaterialDTO zu KursMaterialExportDTO")
    void toExportDto_SollteKursMaterialDTOKorrektKonvertieren() {
        // Given
        KursMaterialDTO kursMaterialDTO = new KursMaterialDTO();
        kursMaterialDTO.setId(1L);
        kursMaterialDTO.setName("test-material.pdf");
        kursMaterialDTO.setInhalt("Test Content".getBytes());
        kursMaterialDTO.setKursId(10L);
        kursMaterialDTO.setKurseinheitId(20L);

        // When
        KursMaterialExportDTO result = mapper.toExportDto(kursMaterialDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("test-material.pdf");
        assertThat(result.getInhaltBase64()).isEqualTo(Base64.getEncoder().encodeToString("Test Content".getBytes()));
    }

    @Test
    @DisplayName("toExportDto - Null Inhalt")
    void toExportDto_MitNullInhalt_SollteNullBase64Zurückgeben() {
        // Given
        KursMaterialDTO kursMaterialDTO = new KursMaterialDTO();
        kursMaterialDTO.setId(1L);
        kursMaterialDTO.setName("test-material.pdf");
        kursMaterialDTO.setInhalt(null);

        // When
        KursMaterialExportDTO result = mapper.toExportDto(kursMaterialDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInhaltBase64()).isNull();
    }

    @Test
    @DisplayName("fromExportDto - Konvertiert KursMaterialExportDTO zu KursMaterialDTO")
    void fromExportDto_SollteKursMaterialExportDTOKorrektKonvertieren() {
        // Given
        KursMaterialExportDTO exportDTO = new KursMaterialExportDTO();
        exportDTO.setId(1L);
        exportDTO.setName("test-material.pdf");
        exportDTO.setInhaltBase64(Base64.getEncoder().encodeToString("Test Content".getBytes()));

        // When
        KursMaterialDTO result = mapper.fromExportDto(exportDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("test-material.pdf");
        assertThat(result.getInhalt()).isEqualTo("Test Content".getBytes());
        assertThat(result.getKursId()).isNull(); // Should be ignored
        assertThat(result.getKurseinheitId()).isNull(); // Should be ignored
    }

    @Test
    @DisplayName("fromExportDto - Null Base64")
    void fromExportDto_MitNullBase64_SollteNullInhaltZurückgeben() {
        // Given
        KursMaterialExportDTO exportDTO = new KursMaterialExportDTO();
        exportDTO.setId(1L);
        exportDTO.setName("test-material.pdf");
        exportDTO.setInhaltBase64(null);

        // When
        KursMaterialDTO result = mapper.fromExportDto(exportDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInhalt()).isNull();
    }

    @Test
    @DisplayName("toExportDtoList - Konvertiert Liste von KursMaterialDTO")
    void toExportDtoList_SollteListeKorrektKonvertieren() {
        // Given
        KursMaterialDTO dto1 = createKursMaterialDTO(1L, "material1.pdf", "Content 1");
        KursMaterialDTO dto2 = createKursMaterialDTO(2L, "material2.pdf", "Content 2");
        List<KursMaterialDTO> dtoList = Arrays.asList(dto1, dto2);

        // When
        List<KursMaterialExportDTO> result = mapper.toExportDtoList(dtoList);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("material1.pdf");
        assertThat(result.get(0).getInhaltBase64()).isEqualTo(Base64.getEncoder().encodeToString("Content 1".getBytes()));
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("material2.pdf");
        assertThat(result.get(1).getInhaltBase64()).isEqualTo(Base64.getEncoder().encodeToString("Content 2".getBytes()));
    }

    @Test
    @DisplayName("fromExportDtoList - Konvertiert Liste von KursMaterialExportDTO")
    void fromExportDtoList_SollteListeKorrektKonvertieren() {
        // Given
        KursMaterialExportDTO dto1 = createKursMaterialExportDTO(1L, "material1.pdf", "Content 1");
        KursMaterialExportDTO dto2 = createKursMaterialExportDTO(2L, "material2.pdf", "Content 2");
        List<KursMaterialExportDTO> exportDtoList = Arrays.asList(dto1, dto2);

        // When
        List<KursMaterialDTO> result = mapper.fromExportDtoList(exportDtoList);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("material1.pdf");
        assertThat(result.get(0).getInhalt()).isEqualTo("Content 1".getBytes());
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("material2.pdf");
        assertThat(result.get(1).getInhalt()).isEqualTo("Content 2".getBytes());
    }

    @Test
    @DisplayName("bytesToBase64 - Konvertiert Bytes zu Base64")
    void bytesToBase64_SollteKorrektKonvertieren() {
        // Given
        byte[] bytes = "Test Content".getBytes();

        // When
        String result = mapper.bytesToBase64(bytes);

        // Then
        assertThat(result).isEqualTo(Base64.getEncoder().encodeToString(bytes));
    }

    @Test
    @DisplayName("bytesToBase64 - Null Input")
    void bytesToBase64_MitNull_SollteNullZurückgeben() {
        // When
        String result = mapper.bytesToBase64(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("base64ToBytes - Konvertiert Base64 zu Bytes")
    void base64ToBytes_SollteKorrektKonvertieren() {
        // Given
        String base64 = Base64.getEncoder().encodeToString("Test Content".getBytes());

        // When
        byte[] result = mapper.base64ToBytes(base64);

        // Then
        assertThat(result).isEqualTo("Test Content".getBytes());
    }

    @Test
    @DisplayName("base64ToBytes - Null Input")
    void base64ToBytes_MitNull_SollteNullZurückgeben() {
        // When
        byte[] result = mapper.base64ToBytes(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toExportDto - Leeres Byte Array")
    void toExportDto_MitLeeremByteArray_SollteKorrektKonvertieren() {
        // Given
        KursMaterialDTO kursMaterialDTO = new KursMaterialDTO();
        kursMaterialDTO.setId(1L);
        kursMaterialDTO.setInhalt(new byte[0]);

        // When
        KursMaterialExportDTO result = mapper.toExportDto(kursMaterialDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInhaltBase64()).isEqualTo("");
    }

    // Helper methods
    private KursMaterialDTO createKursMaterialDTO(Long id, String name, String content) {
        KursMaterialDTO dto = new KursMaterialDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setInhalt(content.getBytes());
        dto.setKursId(10L);
        dto.setKurseinheitId(20L);
        return dto;
    }

    private KursMaterialExportDTO createKursMaterialExportDTO(Long id, String name, String content) {
        KursMaterialExportDTO dto = new KursMaterialExportDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setInhaltBase64(Base64.getEncoder().encodeToString(content.getBytes()));
        return dto;
    }
}