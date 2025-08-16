package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für KurseinheitMapper.
 */
@SpringBootTest
class KurseinheitMapperTest {

    @Autowired
    private KurseinheitMapper kurseinheitMapper;

    /**
     * Testet die Konvertierung von Kurseinheit-Entity zu KurseinheitDTO.
     */
    @Test
    void testKurseinheitToKurseinheitDTO() {
        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = new Kurseinheit();
        kurseinheit.setId(1L);
        kurseinheit.setName("Einheit 1");
        kurseinheit.setReihenfolge(1);
        kurseinheit.setKurs(kurs);

        // Entity zu DTO konvertieren
        KurseinheitDTO kurseinheitDTO = kurseinheitMapper.toDto(kurseinheit);

        // Ergebnisse überprüfen
        assertNotNull(kurseinheitDTO);
        assertEquals(kurseinheit.getId(), kurseinheitDTO.getId());
        assertEquals(kurseinheit.getName(), kurseinheitDTO.getName());
        assertEquals(kurseinheit.getReihenfolge(), kurseinheitDTO.getReihenfolge());
        assertEquals(kurs.getId(), kurseinheitDTO.getKursId());
        assertNotNull(kurseinheitDTO.getKursMaterialien());
    }

    /**
     * Testet die Konvertierung von KurseinheitDTO zu Kurseinheit-Entity.
     */
    @Test
    void testKurseinheitDTOToKurseinheit() {
        // KurseinheitDTO erstellen
        KurseinheitDTO kurseinheitDTO = new KurseinheitDTO();
        kurseinheitDTO.setId(1L);
        kurseinheitDTO.setName("Einheit 1");
        kurseinheitDTO.setReihenfolge(1);
        kurseinheitDTO.setKursId(1L);

        // DTO zu Entity konvertieren
        Kurseinheit kurseinheit = kurseinheitMapper.toEntity(kurseinheitDTO);

        // Ergebnisse überprüfen
        assertNotNull(kurseinheit);
        assertEquals(kurseinheitDTO.getId(), kurseinheit.getId());
        assertEquals(kurseinheitDTO.getName(), kurseinheit.getName());
        assertEquals(kurseinheitDTO.getReihenfolge(), kurseinheit.getReihenfolge());
        // Bei der Rückkonvertierung wird kurs nicht automatisch gesetzt
        assertNull(kurseinheit.getKurs());
        assertNotNull(kurseinheit.getKursMaterialien());
        // Die Aufgaben werden ignoriert
        assertNotNull(kurseinheit.getAufgaben());
    }

    /**
     * Testet die Konvertierung von einer Liste von Kurseinheit-Entities zu einer Liste von KurseinheitDTOs.
     */
    @Test
    void testKurseinheitListToKurseinheitDTOList() {
        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Kurseinheit-Entities erstellen
        Kurseinheit kurseinheit1 = new Kurseinheit();
        kurseinheit1.setId(1L);
        kurseinheit1.setName("Einheit 1");
        kurseinheit1.setReihenfolge(1);
        kurseinheit1.setKurs(kurs);

        Kurseinheit kurseinheit2 = new Kurseinheit();
        kurseinheit2.setId(2L);
        kurseinheit2.setName("Einheit 2");
        kurseinheit2.setReihenfolge(2);
        kurseinheit2.setKurs(kurs);

        List<Kurseinheit> kurseinheiten = Arrays.asList(kurseinheit1, kurseinheit2);

        // Entities zu DTOs konvertieren
        List<KurseinheitDTO> kurseinheitDTOs = kurseinheitMapper.toDtoList(kurseinheiten);

        // Ergebnisse überprüfen
        assertNotNull(kurseinheitDTOs);
        assertEquals(2, kurseinheitDTOs.size());
        assertEquals(kurseinheit1.getId(), kurseinheitDTOs.get(0).getId());
        assertEquals(kurseinheit1.getName(), kurseinheitDTOs.get(0).getName());
        assertEquals(kurseinheit1.getReihenfolge(), kurseinheitDTOs.get(0).getReihenfolge());
        assertEquals(kurs.getId(), kurseinheitDTOs.get(0).getKursId());
        assertEquals(kurseinheit2.getId(), kurseinheitDTOs.get(1).getId());
        assertEquals(kurseinheit2.getName(), kurseinheitDTOs.get(1).getName());
        assertEquals(kurseinheit2.getReihenfolge(), kurseinheitDTOs.get(1).getReihenfolge());
        assertEquals(kurs.getId(), kurseinheitDTOs.get(1).getKursId());
    }

    /**
     * Testet die Konvertierung von Kurseinheit-Entity mit KursMaterialien zu KurseinheitDTO.
     */
    @Test
    void testKurseinheitWithKursMaterialienToKurseinheitDTO() {
        // Kurs-Entity erstellen
        Kurs kurs = new Kurs();
        kurs.setId(1L);
        kurs.setName("Kommunikationsnetze");

        // Kurseinheit-Entity erstellen
        Kurseinheit kurseinheit = new Kurseinheit();
        kurseinheit.setId(1L);
        kurseinheit.setName("Einheit 1");
        kurseinheit.setReihenfolge(1);
        kurseinheit.setKurs(kurs);

        // KursMaterial-Entities erstellen
        KursMaterial kursMaterial1 = new KursMaterial();
        kursMaterial1.setId(1L);
        kursMaterial1.setName("Material 1");
        kursMaterial1.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        kursMaterial1.setMimeType("application/pdf");
        kursMaterial1.setInhalt("Test-Inhalt 1".getBytes());
        kursMaterial1.setKurseinheit(kurseinheit);

        KursMaterial kursMaterial2 = new KursMaterial();
        kursMaterial2.setId(2L);
        kursMaterial2.setName("Material 2");
        kursMaterial2.setTyp(KursMaterial.KursMaterialTyp.BILD);
        kursMaterial2.setMimeType("image/png");
        kursMaterial2.setInhalt("Test-Inhalt 2".getBytes());
        kursMaterial2.setKurseinheit(kurseinheit);

        kurseinheit.setKursMaterialien(Arrays.asList(kursMaterial1, kursMaterial2));

        // Entity zu DTO konvertieren
        KurseinheitDTO kurseinheitDTO = kurseinheitMapper.toDto(kurseinheit);

        // Ergebnisse überprüfen
        assertNotNull(kurseinheitDTO);
        assertEquals(kurseinheit.getId(), kurseinheitDTO.getId());
        assertEquals(kurseinheit.getName(), kurseinheitDTO.getName());
        assertEquals(kurseinheit.getReihenfolge(), kurseinheitDTO.getReihenfolge());
        assertEquals(kurs.getId(), kurseinheitDTO.getKursId());
        assertEquals(2, kurseinheitDTO.getKursMaterialien().size());
        
        // KursMaterial 1 überprüfen
        KursMaterialDTO kursMaterialDTO1 = kurseinheitDTO.getKursMaterialien().get(0);
        assertEquals(kursMaterial1.getId(), kursMaterialDTO1.getId());
        assertEquals(kursMaterial1.getName(), kursMaterialDTO1.getName());
        assertEquals(KursMaterialDTO.KursMaterialTyp.DOKUMENT, kursMaterialDTO1.getTyp());
        assertEquals(kursMaterial1.getMimeType(), kursMaterialDTO1.getMimeType());
        assertEquals(kurseinheit.getId(), kursMaterialDTO1.getKurseinheitId());
        assertArrayEquals(kursMaterial1.getInhalt(), kursMaterialDTO1.getInhalt());
        
        // KursMaterial 2 überprüfen
        KursMaterialDTO kursMaterialDTO2 = kurseinheitDTO.getKursMaterialien().get(1);
        assertEquals(kursMaterial2.getId(), kursMaterialDTO2.getId());
        assertEquals(kursMaterial2.getName(), kursMaterialDTO2.getName());
        assertEquals(KursMaterialDTO.KursMaterialTyp.BILD, kursMaterialDTO2.getTyp());
        assertEquals(kursMaterial2.getMimeType(), kursMaterialDTO2.getMimeType());
        assertEquals(kurseinheit.getId(), kursMaterialDTO2.getKurseinheitId());
        assertArrayEquals(kursMaterial2.getInhalt(), kursMaterialDTO2.getInhalt());
    }
}
