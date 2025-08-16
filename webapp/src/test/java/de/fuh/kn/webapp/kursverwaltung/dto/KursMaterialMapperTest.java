package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für KursMaterialMapper.
 * Testet die Konvertierung zwischen KursMaterial-Entity und KursMaterialDTO.
 */
class KursMaterialMapperTest {

    private final KursMaterialMapper kursMaterialMapper = Mappers.getMapper(KursMaterialMapper.class);

    /**
     * Testet die Konvertierung von KursMaterial-Entity zu KursMaterialDTO.
     */
    @Test
    void testKursMaterialToKursMaterialDTO() {
        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = new Kurseinheit();
        kurseinheit.setId(2L);
        kurseinheit.setName("Einheit 1");
        kurseinheit.setReihenfolge(1);
        kurseinheit.setKurs(kurs);

        // KursMaterial-Entity erstellen
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(3L);
        kursMaterial.setName("Studienskript KE1");
        kursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        kursMaterial.setMimeType("application/pdf");
        kursMaterial.setInhalt("Test-Inhalt".getBytes());
        kursMaterial.setIndexiert(true);
        kursMaterial.setKurseinheit(kurseinheit);
        kursMaterial.setKurs(kurs);

        // Entity zu DTO konvertieren
        KursMaterialDTO kursMaterialDTO = kursMaterialMapper.toDto(kursMaterial);

        // Ergebnisse überprüfen
        assertEquals(kursMaterial.getId(), kursMaterialDTO.getId());
        assertEquals(kursMaterial.getName(), kursMaterialDTO.getName());
        assertEquals(KursMaterialDTO.KursMaterialTyp.DOKUMENT, kursMaterialDTO.getTyp());
        assertEquals(kursMaterial.getMimeType(), kursMaterialDTO.getMimeType());
        assertEquals(kurseinheit.getId(), kursMaterialDTO.getKurseinheitId());
        assertEquals(kurs.getId(), kursMaterialDTO.getKursId());
        assertEquals(kursMaterial.getIndexiert(), kursMaterialDTO.getIndexiert());
        assertArrayEquals(kursMaterial.getInhalt(), kursMaterialDTO.getInhalt());
    }

    /**
     * Testet die Konvertierung von KursMaterialDTO zu KursMaterial-Entity.
     */
    @Test
    void testKursMaterialDTOToKursMaterial() {
        // KursMaterialDTO erstellen
        KursMaterialDTO kursMaterialDTO = new KursMaterialDTO();
        kursMaterialDTO.setId(1L);
        kursMaterialDTO.setName("Studienskript KE1");
        kursMaterialDTO.setTyp(KursMaterialDTO.KursMaterialTyp.DOKUMENT);
        kursMaterialDTO.setMimeType("application/pdf");
        kursMaterialDTO.setKurseinheitId(2L);
        kursMaterialDTO.setKursId(3L);
        kursMaterialDTO.setIndexiert(true);
        kursMaterialDTO.setInhalt("Test-Inhalt".getBytes());

        // DTO zu Entity konvertieren
        KursMaterial kursMaterial = kursMaterialMapper.toEntity(kursMaterialDTO);

        // Ergebnisse überprüfen
        assertEquals(kursMaterialDTO.getId(), kursMaterial.getId());
        assertEquals(kursMaterialDTO.getName(), kursMaterial.getName());
        assertEquals(KursMaterial.KursMaterialTyp.DOKUMENT, kursMaterial.getTyp());
        assertEquals(kursMaterialDTO.getMimeType(), kursMaterial.getMimeType());
        assertEquals(kursMaterialDTO.getIndexiert(), kursMaterial.getIndexiert());
        // Bei der Rückkonvertierung werden kurs und kurseinheit nicht automatisch gesetzt
        assertNull(kursMaterial.getKurseinheit());
        assertNull(kursMaterial.getKurs());
        assertArrayEquals(kursMaterialDTO.getInhalt(), kursMaterial.getInhalt());
    }

    /**
     * Testet die Konvertierung von einer Liste von KursMaterial-Entities zu einer Liste von KursMaterialDTOs.
     */
    @Test
    void testKursMaterialListToKursMaterialDTOList() {
        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = new Kurseinheit();
        kurseinheit.setId(2L);
        kurseinheit.setName("Einheit 1");
        kurseinheit.setReihenfolge(1);
        kurseinheit.setKurs(kurs);

        // KursMaterial-Entities erstellen
        KursMaterial kursMaterial1 = new KursMaterial();
        kursMaterial1.setId(3L);
        kursMaterial1.setName("Material 1");
        kursMaterial1.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        kursMaterial1.setMimeType("application/pdf");
        kursMaterial1.setInhalt("Test-Inhalt 1".getBytes());
        kursMaterial1.setIndexiert(true);
        kursMaterial1.setKurseinheit(kurseinheit);

        KursMaterial kursMaterial2 = new KursMaterial();
        kursMaterial2.setId(4L);
        kursMaterial2.setName("Material 2");
        kursMaterial2.setTyp(KursMaterial.KursMaterialTyp.BILD);
        kursMaterial2.setMimeType("image/png");
        kursMaterial2.setInhalt("Test-Inhalt 2".getBytes());
        kursMaterial2.setIndexiert(false);
        kursMaterial2.setKurs(kurs);

        List<KursMaterial> kursMaterialien = Arrays.asList(kursMaterial1, kursMaterial2);

        // Entities zu DTOs konvertieren
        List<KursMaterialDTO> kursMaterialDTOs = kursMaterialMapper.toDtoList(kursMaterialien);

        // Ergebnisse überprüfen
        assertEquals(2, kursMaterialDTOs.size());
        
        // KursMaterial 1 überprüfen
        KursMaterialDTO kursMaterialDTO1 = kursMaterialDTOs.get(0);
        assertEquals(kursMaterial1.getId(), kursMaterialDTO1.getId());
        assertEquals(kursMaterial1.getName(), kursMaterialDTO1.getName());
        assertEquals(KursMaterialDTO.KursMaterialTyp.DOKUMENT, kursMaterialDTO1.getTyp());
        assertEquals(kursMaterial1.getMimeType(), kursMaterialDTO1.getMimeType());
        assertEquals(kurseinheit.getId(), kursMaterialDTO1.getKurseinheitId());
        assertEquals(kursMaterial1.getIndexiert(), kursMaterialDTO1.getIndexiert());
        assertArrayEquals(kursMaterial1.getInhalt(), kursMaterialDTO1.getInhalt());
        
        // KursMaterial 2 überprüfen
        KursMaterialDTO kursMaterialDTO2 = kursMaterialDTOs.get(1);
        assertEquals(kursMaterial2.getId(), kursMaterialDTO2.getId());
        assertEquals(kursMaterial2.getName(), kursMaterialDTO2.getName());
        assertEquals(KursMaterialDTO.KursMaterialTyp.BILD, kursMaterialDTO2.getTyp());
        assertEquals(kursMaterial2.getMimeType(), kursMaterialDTO2.getMimeType());
        assertEquals(kurs.getId(), kursMaterialDTO2.getKursId());
        assertEquals(kursMaterial2.getIndexiert(), kursMaterialDTO2.getIndexiert());
        assertArrayEquals(kursMaterial2.getInhalt(), kursMaterialDTO2.getInhalt());
    }

    /**
     * Testet die Konvertierung von KursMaterialTyp Enums.
     */
    @Test
    void testKursMaterialTypMapping() {
        // Entity zu DTO
        assertEquals(
            KursMaterialDTO.KursMaterialTyp.DOKUMENT, 
            kursMaterialMapper.mapTyp(KursMaterial.KursMaterialTyp.DOKUMENT)
        );
        assertEquals(
            KursMaterialDTO.KursMaterialTyp.BILD, 
            kursMaterialMapper.mapTyp(KursMaterial.KursMaterialTyp.BILD)
        );
        assertNull(kursMaterialMapper.mapTyp(null));
        
        // DTO zu Entity
        assertEquals(
            KursMaterial.KursMaterialTyp.DOKUMENT, 
            kursMaterialMapper.mapTypReverse(KursMaterialDTO.KursMaterialTyp.DOKUMENT)
        );
        assertEquals(
            KursMaterial.KursMaterialTyp.BILD, 
            kursMaterialMapper.mapTypReverse(KursMaterialDTO.KursMaterialTyp.BILD)
        );
        assertNull(kursMaterialMapper.mapTypReverse(null));
    }
}
