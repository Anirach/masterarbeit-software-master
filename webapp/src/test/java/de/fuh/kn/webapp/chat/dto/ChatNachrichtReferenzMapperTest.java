package de.fuh.kn.webapp.chat.dto;

import de.fuh.kn.webapp.persistence.entity.ChatNachricht;
import de.fuh.kn.webapp.persistence.entity.ChatNachrichtReferenz;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatNachrichtReferenzMapperTest {

    private ChatNachrichtReferenzMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ChatNachrichtReferenzMapper.class);
    }

    @Test
    @DisplayName("toDto - Konvertiert ChatNachrichtReferenz zu DTO mit allen Feldern")
    void toDto_MitAllenFeldern_SollteKorrektKonvertieren() {
        // Given
        ChatNachrichtReferenz referenz = new ChatNachrichtReferenz();
        referenz.setId(1L);
        referenz.setSeitennummer(42);
        
        ChatNachricht chatNachricht = new ChatNachricht();
        chatNachricht.setId(100L);
        referenz.setChatNachricht(chatNachricht);
        
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(200L);
        kursMaterial.setName("Lehrbuch_Kapitel1.pdf");
        referenz.setKursMaterial(kursMaterial);

        // When
        ChatNachrichtReferenzDTO result = mapper.toDto(referenz);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getChatNachrichtId()).isEqualTo(100L);
        assertThat(result.getKursMaterialId()).isEqualTo(200L);
        assertThat(result.getKursMaterialName()).isEqualTo("Lehrbuch_Kapitel1.pdf");
        assertThat(result.getSeitennummer()).isEqualTo(42);
    }

    @Test
    @DisplayName("toDto - Ohne Seitennummer")
    void toDto_OhneSeitennummer_SollteNullSeitennummerHaben() {
        // Given
        ChatNachrichtReferenz referenz = new ChatNachrichtReferenz();
        referenz.setId(1L);
        
        ChatNachricht chatNachricht = new ChatNachricht();
        chatNachricht.setId(100L);
        referenz.setChatNachricht(chatNachricht);
        
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(200L);
        kursMaterial.setName("Lehrbuch.pdf");
        referenz.setKursMaterial(kursMaterial);

        // When
        ChatNachrichtReferenzDTO result = mapper.toDto(referenz);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSeitennummer()).isNull();
    }

    @Test
    @DisplayName("toDto - Null Entity")
    void toDto_MitNullEntity_SollteNullZurueckgeben() {
        // When
        ChatNachrichtReferenzDTO result = mapper.toDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toEntity - Konvertiert DTO zu Entity")
    void toEntity_SollteKorrektKonvertieren() {
        // Given
        ChatNachrichtReferenzDTO dto = new ChatNachrichtReferenzDTO();
        dto.setId(1L);
        dto.setChatNachrichtId(100L);
        dto.setKursMaterialId(200L);
        dto.setKursMaterialName("Lehrbuch.pdf");
        dto.setSeitennummer(42);

        // When
        ChatNachrichtReferenz result = mapper.toEntity(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSeitennummer()).isEqualTo(42);
        // ChatNachricht and KursMaterial should be ignored (null)
        assertThat(result.getChatNachricht()).isNull();
        assertThat(result.getKursMaterial()).isNull();
    }

    @Test
    @DisplayName("toEntity - Null DTO")
    void toEntity_MitNullDTO_SollteNullZurueckgeben() {
        // When
        ChatNachrichtReferenz result = mapper.toEntity(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDtoList - Konvertiert Liste von Entities")
    void toDtoList_SollteListeKorrektKonvertieren() {
        // Given
        ChatNachrichtReferenz referenz1 = createReferenz(1L, 100L, 200L, "Material1.pdf", 10);
        ChatNachrichtReferenz referenz2 = createReferenz(2L, 101L, 201L, "Material2.pdf", 20);
        List<ChatNachrichtReferenz> referenzen = Arrays.asList(referenz1, referenz2);

        // When
        List<ChatNachrichtReferenzDTO> result = mapper.toDtoList(referenzen);

        // Then
        assertThat(result).hasSize(2);
        
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getChatNachrichtId()).isEqualTo(100L);
        assertThat(result.get(0).getKursMaterialId()).isEqualTo(200L);
        assertThat(result.get(0).getKursMaterialName()).isEqualTo("Material1.pdf");
        assertThat(result.get(0).getSeitennummer()).isEqualTo(10);
        
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getChatNachrichtId()).isEqualTo(101L);
        assertThat(result.get(1).getKursMaterialId()).isEqualTo(201L);
        assertThat(result.get(1).getKursMaterialName()).isEqualTo("Material2.pdf");
        assertThat(result.get(1).getSeitennummer()).isEqualTo(20);
    }

    @Test
    @DisplayName("toDtoList - Leere Liste")
    void toDtoList_MitLeererListe_SollteLeereListeZurueckgeben() {
        // When
        List<ChatNachrichtReferenzDTO> result = mapper.toDtoList(Collections.emptyList());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("toDtoList - Null Liste")
    void toDtoList_MitNullListe_SollteNullZurueckgeben() {
        // When
        List<ChatNachrichtReferenzDTO> result = mapper.toDtoList(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DTO getEntityDisplayName - Mit Seitennummer")
    void getEntityDisplayName_MitSeitennummer_SollteKorrektFormatiertSein() {
        // Given
        ChatNachrichtReferenzDTO dto = new ChatNachrichtReferenzDTO();
        dto.setKursMaterialName("Lehrbuch.pdf");
        dto.setSeitennummer(42);

        // When
        String displayName = dto.getEntityDisplayName();

        // Then
        assertThat(displayName).isEqualTo("Lehrbuch.pdf (S. 42)");
    }

    @Test
    @DisplayName("DTO getEntityDisplayName - Ohne Seitennummer")
    void getEntityDisplayName_OhneSeitennummer_SollteNurNameSein() {
        // Given
        ChatNachrichtReferenzDTO dto = new ChatNachrichtReferenzDTO();
        dto.setKursMaterialName("Lehrbuch.pdf");

        // When
        String displayName = dto.getEntityDisplayName();

        // Then
        assertThat(displayName).isEqualTo("Lehrbuch.pdf");
    }

    @Test
    @DisplayName("DTO getEntityDisplayName - Ohne Material Name")
    void getEntityDisplayName_OhneMaterialName_SollteDefaultNameVerwenden() {
        // Given
        ChatNachrichtReferenzDTO dto = new ChatNachrichtReferenzDTO();
        dto.setSeitennummer(42);

        // When
        String displayName = dto.getEntityDisplayName();

        // Then
        assertThat(displayName).isEqualTo("Quelle (S. 42)");
    }

    @Test
    @DisplayName("DTO getEntityTypeName")
    void getEntityTypeName_SollteRichtigenTypZurueckgeben() {
        // Given
        ChatNachrichtReferenzDTO dto = new ChatNachrichtReferenzDTO();

        // When
        String typeName = dto.getEntityTypeName();

        // Then
        assertThat(typeName).isEqualTo("ChatNachrichtReferenz");
    }

    // Helper method
    private ChatNachrichtReferenz createReferenz(Long id, Long chatNachrichtId, Long kursMaterialId, 
                                                 String materialName, Integer seitennummer) {
        ChatNachrichtReferenz referenz = new ChatNachrichtReferenz();
        referenz.setId(id);
        referenz.setSeitennummer(seitennummer);
        
        ChatNachricht chatNachricht = new ChatNachricht();
        chatNachricht.setId(chatNachrichtId);
        referenz.setChatNachricht(chatNachricht);
        
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(kursMaterialId);
        kursMaterial.setName(materialName);
        referenz.setKursMaterial(kursMaterial);
        
        return referenz;
    }
}