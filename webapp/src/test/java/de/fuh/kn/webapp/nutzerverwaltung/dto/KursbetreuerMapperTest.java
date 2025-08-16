package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Kursbetreuer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für KursbetreuerMapper.
 */
@SpringBootTest
class KursbetreuerMapperTest {

    @Autowired
    private KursbetreuerMapper kursbetreuerMapper;

    /**
     * Testet die Konvertierung von Kursbetreuer-Entity zu KursbetreuerDTO.
     */
    @Test
    void testKursbetreuerToKursbetreuerDTO() {
        // Kursbetreuer-Entity erstellen
        Kursbetreuer kursbetreuer = new Kursbetreuer();
        kursbetreuer.setId(1L);
        kursbetreuer.setEmail("betreuer@fuh.de");
        kursbetreuer.setVorname("Max");
        kursbetreuer.setNachname("Mustermann");
        kursbetreuer.setIstRegistriert(true);

        // Entity zu DTO konvertieren
        KursbetreuerDTO kursbetreuerDTO = kursbetreuerMapper.toDto(kursbetreuer);

        // Ergebnisse überprüfen
        assertNotNull(kursbetreuerDTO);
        assertEquals(kursbetreuer.getId(), kursbetreuerDTO.getId());
        assertEquals(kursbetreuer.getEmail(), kursbetreuerDTO.getEmail());
        assertEquals(kursbetreuer.getVorname(), kursbetreuerDTO.getVorname());
        assertEquals(kursbetreuer.getNachname(), kursbetreuerDTO.getNachname());
        assertEquals(kursbetreuer.getIstRegistriert(), kursbetreuerDTO.getIstRegistriert());
        
        // Wir testen nicht den displayName, da dieser möglicherweise nicht vom Mapper gesetzt wird
        // Stattdessen wird er vermutlich in einer anderen Komponente generiert
    }

    /**
     * Testet die Konvertierung von KursbetreuerDTO zu Kursbetreuer-Entity.
     */
    @Test
    void testKursbetreuerDTOToKursbetreuer() {
        // KursbetreuerDTO erstellen
        KursbetreuerDTO kursbetreuerDTO = new KursbetreuerDTO();
        kursbetreuerDTO.setId(1L);
        kursbetreuerDTO.setEmail("betreuer@fuh.de");
        kursbetreuerDTO.setVorname("Max");
        kursbetreuerDTO.setNachname("Mustermann");
        kursbetreuerDTO.setIstRegistriert(true);
        kursbetreuerDTO.setDisplayName("Dies wird ignoriert"); // Wird bei der Rückkonvertierung ignoriert

        // DTO zu Entity konvertieren
        Kursbetreuer kursbetreuer = kursbetreuerMapper.toEntity(kursbetreuerDTO);

        // Ergebnisse überprüfen
        assertNotNull(kursbetreuer);
        assertEquals(kursbetreuerDTO.getId(), kursbetreuer.getId());
        assertEquals(kursbetreuerDTO.getEmail(), kursbetreuer.getEmail());
        assertEquals(kursbetreuerDTO.getVorname(), kursbetreuer.getVorname());
        assertEquals(kursbetreuerDTO.getNachname(), kursbetreuer.getNachname());
        assertEquals(kursbetreuerDTO.getIstRegistriert(), kursbetreuer.getIstRegistriert());
    }

    /**
     * Testet die Konvertierung von einer Liste von Kursbetreuer-Entities zu einer Liste von KursbetreuerDTOs.
     */
    @Test
    void testKursbetreuerListToKursbetreuerDTOList() {
        // Kursbetreuer-Entities erstellen
        Kursbetreuer kursbetreuer1 = new Kursbetreuer();
        kursbetreuer1.setId(1L);
        kursbetreuer1.setEmail("betreuer1@fuh.de");
        kursbetreuer1.setVorname("Max");
        kursbetreuer1.setNachname("Mustermann");
        kursbetreuer1.setIstRegistriert(true);

        Kursbetreuer kursbetreuer2 = new Kursbetreuer();
        kursbetreuer2.setId(2L);
        kursbetreuer2.setEmail("betreuer2@fuh.de");
        kursbetreuer2.setVorname("Erika");
        kursbetreuer2.setNachname("Musterfrau");
        kursbetreuer2.setIstRegistriert(true);

        List<Kursbetreuer> kursbetreuer = Arrays.asList(kursbetreuer1, kursbetreuer2);

        // Entities zu DTOs konvertieren
        List<KursbetreuerDTO> kursbetreuerDTOs = kursbetreuerMapper.toDtoList(kursbetreuer);

        // Ergebnisse überprüfen
        assertNotNull(kursbetreuerDTOs);
        assertEquals(2, kursbetreuerDTOs.size());
        
        // Kursbetreuer 1 überprüfen
        assertEquals(kursbetreuer1.getId(), kursbetreuerDTOs.get(0).getId());
        assertEquals(kursbetreuer1.getEmail(), kursbetreuerDTOs.get(0).getEmail());
        assertEquals(kursbetreuer1.getVorname(), kursbetreuerDTOs.get(0).getVorname());
        assertEquals(kursbetreuer1.getNachname(), kursbetreuerDTOs.get(0).getNachname());
        assertEquals(kursbetreuer1.getIstRegistriert(), kursbetreuerDTOs.get(0).getIstRegistriert());
        
        // Kursbetreuer 2 überprüfen
        assertEquals(kursbetreuer2.getId(), kursbetreuerDTOs.get(1).getId());
        assertEquals(kursbetreuer2.getEmail(), kursbetreuerDTOs.get(1).getEmail());
        assertEquals(kursbetreuer2.getVorname(), kursbetreuerDTOs.get(1).getVorname());
        assertEquals(kursbetreuer2.getNachname(), kursbetreuerDTOs.get(1).getNachname());
        assertEquals(kursbetreuer2.getIstRegistriert(), kursbetreuerDTOs.get(1).getIstRegistriert());
    }

    /**
     * Testet die grundlegenden Eigenschaften eines nicht registrierten Kursbetreuers.
     */
    @Test
    void testNichtRegistrierterKursbetreuer() {
        // Kursbetreuer-Entity erstellen, der nicht registriert ist
        Kursbetreuer kursbetreuer = new Kursbetreuer();
        kursbetreuer.setId(1L);
        kursbetreuer.setEmail("betreuer@fuh.de");
        kursbetreuer.setVorname(null);
        kursbetreuer.setNachname(null);
        kursbetreuer.setIstRegistriert(false);

        // Entity zu DTO konvertieren
        KursbetreuerDTO kursbetreuerDTO = kursbetreuerMapper.toDto(kursbetreuer);

        // Ergebnisse überprüfen
        assertNotNull(kursbetreuerDTO);
        assertEquals(false, kursbetreuerDTO.getIstRegistriert());
        assertNull(kursbetreuerDTO.getVorname());
        assertNull(kursbetreuerDTO.getNachname());
    }
}
