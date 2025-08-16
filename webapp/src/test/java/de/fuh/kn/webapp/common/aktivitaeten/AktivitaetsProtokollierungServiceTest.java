package de.fuh.kn.webapp.common.aktivitaeten;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.persistence.entity.Aktivitaet;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import de.fuh.kn.webapp.persistence.entity.Nutzer;
import de.fuh.kn.webapp.persistence.repository.AktivitaetRepository;
import de.fuh.kn.webapp.persistence.repository.NutzerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den AktivitaetsProtokollierungService
 */
@ExtendWith(MockitoExtension.class)
public class AktivitaetsProtokollierungServiceTest {

    @Mock
    private AktivitaetRepository aktivitaetRepository;

    @Mock
    private NutzerRepository nutzerRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AktivitaetMapper aktivitaetMapper;

    @InjectMocks
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    @Test
    public void testProtokolliereAktivitaet_MitNutzer() throws Exception {
        // Arrange
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(1L);
        nutzerDTO.setVorname("Max");
        nutzerDTO.setNachname("Mustermann");

        Nutzer nutzer = new Nutzer() {
            @Override
            public String getDisplayName() {
                return "Max Mustermann";
            }
        };
        nutzer.setId(1L);
        nutzer.setVorname("Max");
        nutzer.setNachname("Mustermann");

        AktivitaetsTyp aktivitaetsTyp = AktivitaetsTyp.LOGIN;
        String beschreibung = "Benutzer hat sich eingeloggt";
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("key1", "value1");
        detailsMap.put("key2", "value2");
        Boolean erfolg = true;
        String referenzTyp = "Session";
        Long referenzId = 12345L;

        Aktivitaet savedAktivitaet = new Aktivitaet();
        savedAktivitaet.setId(1L);
        savedAktivitaet.setNutzer(nutzer);
        savedAktivitaet.setAktivitaetsTyp(aktivitaetsTyp);
        savedAktivitaet.setBeschreibung(beschreibung);
        savedAktivitaet.setDetails("{\"key1\":\"value1\",\"key2\":\"value2\"}");
        savedAktivitaet.setErfolg(erfolg);
        savedAktivitaet.setReferenzTyp(referenzTyp);
        savedAktivitaet.setReferenzId(referenzId);
        savedAktivitaet.setZeitpunkt(java.time.LocalDateTime.now());

        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        aktivitaetDTO.setNutzerId(1L);
        aktivitaetDTO.setNutzerName("Max Mustermann");
        aktivitaetDTO.setAktivitaetsTyp(aktivitaetsTyp);
        aktivitaetDTO.setBeschreibung(beschreibung);
        aktivitaetDTO.setDetails("{\"key1\":\"value1\",\"key2\":\"value2\"}");
        aktivitaetDTO.setErfolg(erfolg);
        aktivitaetDTO.setReferenzTyp(referenzTyp);
        aktivitaetDTO.setReferenzId(referenzId);
        aktivitaetDTO.setZeitpunkt(savedAktivitaet.getZeitpunkt());

        // Mock-Verhalten definieren
        when(nutzerRepository.findById(1L)).thenReturn(java.util.Optional.of(nutzer));
        when(objectMapper.writeValueAsString(detailsMap)).thenReturn("{\"key1\":\"value1\",\"key2\":\"value2\"}");
        when(aktivitaetRepository.save(any(Aktivitaet.class))).thenReturn(savedAktivitaet);
        when(aktivitaetMapper.toDTO(savedAktivitaet)).thenReturn(aktivitaetDTO);

        // Act
        AktivitaetDTO result = aktivitaetsProtokollierungService.protokolliereAktivitaet(
                nutzerDTO, aktivitaetsTyp, beschreibung, detailsMap, erfolg, referenzTyp, referenzId);

        // Assert
        verify(nutzerRepository).findById(1L);

        // Capture the Aktivitaet object being saved to verify its properties
        ArgumentCaptor<Aktivitaet> aktivitaetCaptor = ArgumentCaptor.forClass(Aktivitaet.class);
        verify(aktivitaetRepository).save(aktivitaetCaptor.capture());

        Aktivitaet capturedAktivitaet = aktivitaetCaptor.getValue();
        assertEquals(nutzer, capturedAktivitaet.getNutzer());
        assertEquals(aktivitaetsTyp, capturedAktivitaet.getAktivitaetsTyp());
        assertEquals(beschreibung, capturedAktivitaet.getBeschreibung());
        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", capturedAktivitaet.getDetails());
        assertEquals(erfolg, capturedAktivitaet.getErfolg());
        assertEquals(referenzTyp, capturedAktivitaet.getReferenzTyp());
        assertEquals(referenzId, capturedAktivitaet.getReferenzId());
        assertNotNull(capturedAktivitaet.getZeitpunkt());

        verify(objectMapper).writeValueAsString(detailsMap);
        verify(aktivitaetMapper).toDTO(savedAktivitaet);
        
        assertEquals(aktivitaetDTO, result);
    }

    @Test
    public void testProtokolliereAktivitaet_OhneNutzer() throws Exception {
        // Arrange
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(999L); // ID eines nicht existierenden Nutzers

        AktivitaetsTyp aktivitaetsTyp = AktivitaetsTyp.AUFGABE_ERSTELLEN;
        String beschreibung = "Kurs wurde erstellt";
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("key1", "value1");
        Boolean erfolg = true;
        String referenzTyp = "Kurs";
        Long referenzId = 10L;

        Aktivitaet savedAktivitaet = new Aktivitaet();
        savedAktivitaet.setId(2L);
        savedAktivitaet.setNutzer(null); // Kein Nutzer
        savedAktivitaet.setAktivitaetsTyp(aktivitaetsTyp);
        savedAktivitaet.setBeschreibung(beschreibung);
        savedAktivitaet.setDetails("{\"key1\":\"value1\"}");
        savedAktivitaet.setErfolg(erfolg);
        savedAktivitaet.setReferenzTyp(referenzTyp);
        savedAktivitaet.setReferenzId(referenzId);
        savedAktivitaet.setZeitpunkt(java.time.LocalDateTime.now());

        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(2L);
        aktivitaetDTO.setNutzerId(null);
        aktivitaetDTO.setNutzerName(null);
        aktivitaetDTO.setAktivitaetsTyp(aktivitaetsTyp);
        aktivitaetDTO.setBeschreibung(beschreibung);
        aktivitaetDTO.setDetails("{\"key1\":\"value1\"}");
        aktivitaetDTO.setErfolg(erfolg);
        aktivitaetDTO.setReferenzTyp(referenzTyp);
        aktivitaetDTO.setReferenzId(referenzId);
        aktivitaetDTO.setZeitpunkt(savedAktivitaet.getZeitpunkt());

        // Mock-Verhalten definieren
        when(nutzerRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        when(objectMapper.writeValueAsString(detailsMap)).thenReturn("{\"key1\":\"value1\"}");
        when(aktivitaetRepository.save(any(Aktivitaet.class))).thenReturn(savedAktivitaet);
        when(aktivitaetMapper.toDTO(savedAktivitaet)).thenReturn(aktivitaetDTO);

        // Act
        AktivitaetDTO result = aktivitaetsProtokollierungService.protokolliereAktivitaet(
                nutzerDTO, aktivitaetsTyp, beschreibung, detailsMap, erfolg, referenzTyp, referenzId);

        // Assert
        verify(nutzerRepository).findById(999L);

        // Capture the Aktivitaet object being saved and verify the key properties
        ArgumentCaptor<Aktivitaet> aktivitaetCaptor = ArgumentCaptor.forClass(Aktivitaet.class);
        verify(aktivitaetRepository).save(aktivitaetCaptor.capture());

        Aktivitaet capturedAktivitaet = aktivitaetCaptor.getValue();
        assertNull(capturedAktivitaet.getNutzer()); // Nutzer sollte null sein
        assertEquals(aktivitaetsTyp, capturedAktivitaet.getAktivitaetsTyp());
        assertEquals(beschreibung, capturedAktivitaet.getBeschreibung());
        assertEquals("{\"key1\":\"value1\"}", capturedAktivitaet.getDetails());
        assertEquals(erfolg, capturedAktivitaet.getErfolg());
        assertEquals(referenzTyp, capturedAktivitaet.getReferenzTyp());
        assertEquals(referenzId, capturedAktivitaet.getReferenzId());
        assertNotNull(capturedAktivitaet.getZeitpunkt());

        verify(objectMapper).writeValueAsString(detailsMap);
        verify(aktivitaetMapper).toDTO(savedAktivitaet);
        
        assertEquals(aktivitaetDTO, result);
    }

    @Test
    public void testProtokolliereAktivitaet_MitJsonProcessingException() throws Exception {
        // Arrange
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(1L);

        Nutzer nutzer = new Nutzer() {
            @Override
            public String getDisplayName() {
                return "Test Nutzer";
            }
        };
        nutzer.setId(1L);

        AktivitaetsTyp aktivitaetsTyp = AktivitaetsTyp.AUFGABE_BEARBEITEN;
        String beschreibung = "Aufgabe bearbeitet";
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("ungültiger_key", new Object() { // Ein Objekt, das nicht serialisiert werden kann
            @Override
            public String toString() {
                return "Nicht-serialisierbares Objekt";
            }
        });
        Boolean erfolg = false;
        String referenzTyp = "Aufgabe";
        Long referenzId = 42L;

        Aktivitaet savedAktivitaet = new Aktivitaet();
        savedAktivitaet.setId(3L);
        savedAktivitaet.setNutzer(nutzer);
        savedAktivitaet.setAktivitaetsTyp(aktivitaetsTyp);
        savedAktivitaet.setBeschreibung(beschreibung);
        savedAktivitaet.setDetails("Fehler bei Details-Serialisierung"); // Fehlerhafte Details
        savedAktivitaet.setErfolg(erfolg);
        savedAktivitaet.setReferenzTyp(referenzTyp);
        savedAktivitaet.setReferenzId(referenzId);
        savedAktivitaet.setZeitpunkt(java.time.LocalDateTime.now());

        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(3L);
        aktivitaetDTO.setNutzerId(1L);
        aktivitaetDTO.setNutzerName("Test Nutzer");
        aktivitaetDTO.setAktivitaetsTyp(aktivitaetsTyp);
        aktivitaetDTO.setBeschreibung(beschreibung);
        aktivitaetDTO.setDetails("Fehler bei Details-Serialisierung");
        aktivitaetDTO.setErfolg(erfolg);
        aktivitaetDTO.setReferenzTyp(referenzTyp);
        aktivitaetDTO.setReferenzId(referenzId);
        aktivitaetDTO.setZeitpunkt(savedAktivitaet.getZeitpunkt());

        // Mock-Verhalten definieren
        when(nutzerRepository.findById(1L)).thenReturn(java.util.Optional.of(nutzer));
        when(objectMapper.writeValueAsString(detailsMap))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Fehler bei der Serialisierung") {});
        when(aktivitaetRepository.save(any(Aktivitaet.class))).thenReturn(savedAktivitaet);
        when(aktivitaetMapper.toDTO(savedAktivitaet)).thenReturn(aktivitaetDTO);

        // Act
        AktivitaetDTO result = aktivitaetsProtokollierungService.protokolliereAktivitaet(
                nutzerDTO, aktivitaetsTyp, beschreibung, detailsMap, erfolg, referenzTyp, referenzId);

        // Assert
        verify(nutzerRepository).findById(1L);
        verify(objectMapper).writeValueAsString(detailsMap);
        verify(aktivitaetRepository).save(any(Aktivitaet.class));
        verify(aktivitaetMapper).toDTO(savedAktivitaet);
        
        // Die Aktivität sollte trotz JSON-Fehler gespeichert werden
        ArgumentCaptor<Aktivitaet> aktivitaetCaptor = ArgumentCaptor.forClass(Aktivitaet.class);
        verify(aktivitaetRepository).save(aktivitaetCaptor.capture());
        
        Aktivitaet capturedAktivitaet = aktivitaetCaptor.getValue();
        assertEquals("Fehler bei Details-Serialisierung", capturedAktivitaet.getDetails());
        
        assertEquals(aktivitaetDTO, result);
    }

    @Test
    public void testProtokolliereAktivitaet_NullNutzerDTO() throws Exception {
        // Arrange
        NutzerDTO nutzerDTO = null; // Null-DTO
        AktivitaetsTyp aktivitaetsTyp = AktivitaetsTyp.LOGIN;
        String beschreibung = "System-Aktion durchgeführt";
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("key", "value");
        Boolean erfolg = true;
        String referenzTyp = null;
        Long referenzId = null;

        Aktivitaet savedAktivitaet = new Aktivitaet();
        savedAktivitaet.setId(4L);
        savedAktivitaet.setNutzer(null);
        savedAktivitaet.setAktivitaetsTyp(aktivitaetsTyp);
        savedAktivitaet.setBeschreibung(beschreibung);
        savedAktivitaet.setDetails("{\"key\":\"value\"}");
        savedAktivitaet.setErfolg(erfolg);
        savedAktivitaet.setReferenzTyp(referenzTyp);
        savedAktivitaet.setReferenzId(referenzId);
        savedAktivitaet.setZeitpunkt(java.time.LocalDateTime.now());

        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(4L);
        aktivitaetDTO.setNutzerId(null);
        aktivitaetDTO.setNutzerName(null);
        aktivitaetDTO.setAktivitaetsTyp(aktivitaetsTyp);
        aktivitaetDTO.setBeschreibung(beschreibung);
        aktivitaetDTO.setDetails("{\"key\":\"value\"}");

        // Mock-Verhalten definieren
        when(objectMapper.writeValueAsString(detailsMap)).thenReturn("{\"key\":\"value\"}");
        when(aktivitaetRepository.save(any(Aktivitaet.class))).thenReturn(savedAktivitaet);
        when(aktivitaetMapper.toDTO(savedAktivitaet)).thenReturn(aktivitaetDTO);

        // Act
        AktivitaetDTO result = aktivitaetsProtokollierungService.protokolliereAktivitaet(
                nutzerDTO, aktivitaetsTyp, beschreibung, detailsMap, erfolg, referenzTyp, referenzId);

        // Assert
        verify(aktivitaetRepository).save(any(Aktivitaet.class));
        verify(aktivitaetMapper).toDTO(savedAktivitaet);
        
        // NutzerRepository sollte nicht aufgerufen werden
        verifyNoInteractions(nutzerRepository);
        
        ArgumentCaptor<Aktivitaet> aktivitaetCaptor = ArgumentCaptor.forClass(Aktivitaet.class);
        verify(aktivitaetRepository).save(aktivitaetCaptor.capture());
        
        Aktivitaet capturedAktivitaet = aktivitaetCaptor.getValue();
        assertNull(capturedAktivitaet.getNutzer());
        assertEquals(aktivitaetsTyp, capturedAktivitaet.getAktivitaetsTyp());
        assertEquals(beschreibung, capturedAktivitaet.getBeschreibung());
        
        assertEquals(aktivitaetDTO, result);
    }
}