package de.fuh.kn.webapp.common.aktivitaeten;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Testklasse für AktivitaetReferenzHelper
 */
@ExtendWith(MockitoExtension.class)
public class AktivitaetReferenzHelperTest {

    @Mock
    private NutzerService nutzerService;

    @Mock
    private KursService kursService;

    @Mock
    private KurseinheitService kurseinheitService;

    @Mock
    private KursMaterialService kursMaterialService;

    @InjectMocks
    private AktivitaetReferenzHelper aktivitaetReferenzHelper;

    @Test
    public void testToString_NullRefTypeOrId() {
        // Test wenn refType null ist
        String result1 = aktivitaetReferenzHelper.toString(null, 1L);
        assertEquals("ID: 1 (gelöscht)", result1);

        // Test wenn refId null ist
        String result2 = aktivitaetReferenzHelper.toString("Student", null);
        assertEquals("ID: null (gelöscht)", result2);

        // Test wenn beide null sind
        String result3 = aktivitaetReferenzHelper.toString(null, null);
        assertEquals("ID: null (gelöscht)", result3);
        
        // Verifizieren, dass keine Service-Methoden aufgerufen wurden
        verifyNoInteractions(nutzerService, kursService, kurseinheitService, kursMaterialService);
    }
    
    @Test
    public void testToString_Student() {
        // DTO mit Konstruktor erstellen
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(1L);
        studentDTO.setVorname("Max");
        studentDTO.setNachname("Mustermann");
        studentDTO.setDisplayName("Max Mustermann");
        
        // Mocking: nutzerService.getNutzerById sollte das DTO zurückgeben
        when(nutzerService.getNutzerById(1L)).thenReturn(Optional.of(studentDTO));
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString("Student", 1L);
        
        // Prüfen, ob das richtige Ergebnis zurückgegeben wurde
        assertEquals("Max Mustermann", result);
        
        // Verifizieren, dass die richtige Service-Methode aufgerufen wurde
        verify(nutzerService).getNutzerById(1L);
        verifyNoInteractions(kursService, kurseinheitService, kursMaterialService);
    }
    
    @Test
    public void testToString_Kursbetreuer() {
        // DTO mit Konstruktor erstellen
        KursbetreuerDTO betreuerDTO = new KursbetreuerDTO();
        betreuerDTO.setId(2L);
        betreuerDTO.setVorname("Petra");
        betreuerDTO.setNachname("Schmidt");
        betreuerDTO.setDisplayName("Dr. Petra Schmidt");
        
        // Mocking: nutzerService.getNutzerById sollte das DTO zurückgeben
        when(nutzerService.getNutzerById(2L)).thenReturn(Optional.of(betreuerDTO));
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString("Kursbetreuer", 2L);
        
        // Prüfen, ob das richtige Ergebnis zurückgegeben wurde
        assertEquals("Dr. Petra Schmidt", result);
        
        // Verifizieren, dass die richtige Service-Methode aufgerufen wurde
        verify(nutzerService).getNutzerById(2L);
        verifyNoInteractions(kursService, kurseinheitService, kursMaterialService);
    }
    
    @Test
    public void testToString_Kurs() {
        // DTO mit Konstruktor erstellen
        KursDTO kursDTO = new KursDTO();
        kursDTO.setId(3L);
        kursDTO.setName("Programmierung 1");
        
        // Mocking: kursService.getKursById sollte das DTO zurückgeben
        when(kursService.getKursById(3L)).thenReturn(kursDTO);
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString("Kurs", 3L);
        
        // Prüfen, ob das richtige Ergebnis zurückgegeben wurde
        assertEquals("Programmierung 1", result);
        
        // Verifizieren, dass die richtige Service-Methode aufgerufen wurde
        verify(kursService).getKursById(3L);
        verifyNoInteractions(nutzerService, kurseinheitService, kursMaterialService);
    }
    
    @Test
    public void testToString_Kurseinheit() {
        // DTO mit Konstruktor erstellen
        KurseinheitDTO kurseinheitDTO = new KurseinheitDTO();
        kurseinheitDTO.setId(4L);
        kurseinheitDTO.setName("Einführung in Java");
        
        // Mocking: kurseinheitService.getKurseinheitById sollte das DTO zurückgeben
        when(kurseinheitService.getKurseinheitById(4L)).thenReturn(kurseinheitDTO);
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString("Kurseinheit", 4L);
        
        // Prüfen, ob das richtige Ergebnis zurückgegeben wurde
        assertEquals("Einführung in Java", result);
        
        // Verifizieren, dass die richtige Service-Methode aufgerufen wurde
        verify(kurseinheitService).getKurseinheitById(4L);
        verifyNoInteractions(nutzerService, kursService, kursMaterialService);
    }
    
    @Test
    public void testToString_Kursmaterial() {
        // DTO mit Konstruktor erstellen
        KursMaterialDTO kursmaterialDTO = new KursMaterialDTO();
        kursmaterialDTO.setId(5L);
        kursmaterialDTO.setName("Folien_Woche1.pdf");
        
        // Mocking: kursMaterialService.getKursMaterialById sollte das DTO zurückgeben
        when(kursMaterialService.getKursMaterialById(5L)).thenReturn(kursmaterialDTO);
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString("Kursmaterial", 5L);
        
        // Prüfen, ob das richtige Ergebnis zurückgegeben wurde
        assertEquals("Folien_Woche1.pdf", result);
        
        // Verifizieren, dass die richtige Service-Methode aufgerufen wurde
        verify(kursMaterialService).getKursMaterialById(5L);
        verifyNoInteractions(nutzerService, kursService, kurseinheitService);
    }
    
    @Test
    public void testToString_ObjectNotFound() {
        // Mocking: nutzerService.getNutzerById sollte ein leeres Optional zurückgeben
        when(nutzerService.getNutzerById(999L)).thenReturn(Optional.empty());
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString("Student", 999L);
        
        // Prüfen, ob die "gelöscht"-Nachricht zurückgegeben wurde
        assertEquals("ID: 999 (gelöscht)", result);
        
        // Verifizieren, dass die richtige Service-Methode aufgerufen wurde
        verify(nutzerService).getNutzerById(999L);
        verifyNoInteractions(kursService, kurseinheitService, kursMaterialService);
    }
    
    @Test
    public void testToString_UnknownRefType() {
        // Methode mit unbekanntem Referenztyp ausführen
        String result = aktivitaetReferenzHelper.toString("UnbekannterTyp", 1L);
        
        // Prüfen, ob die "gelöscht"-Nachricht zurückgegeben wurde
        assertEquals("ID: 1 (gelöscht)", result);
        
        // Verifizieren, dass keine Service-Methoden aufgerufen wurden
        verifyNoInteractions(nutzerService, kursService, kurseinheitService, kursMaterialService);
    }
    
    @Test
    public void testToString_BaseDTOObject() {
        // DTO mit Konstruktor erstellen
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(6L);
        nutzerDTO.setVorname("Hans");
        nutzerDTO.setNachname("Müller");
        nutzerDTO.setDisplayName("Hans Müller");
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString(nutzerDTO);
        
        // Prüfen, ob der Anzeigename vom DTO zurückgegeben wurde
        assertEquals("Hans Müller", result);
    }
    
    @Test
    public void testToString_RegularObject() {
        // Ein normales Objekt (hier: String) testen
        String testString = "Test-String";
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString(testString);
        
        // Prüfen, ob toString des Objekts zurückgegeben wurde
        assertEquals(testString, result);
    }
    
    @Test
    public void testToString_CustomObject() {
        // Ein benutzerdefiniertes Objekt mit überschriebenem toString erstellen
        Object customObject = new Object() {
            @Override
            public String toString() {
                return "Benutzerdefiniertes Objekt";
            }
        };
        
        // Methode ausführen
        String result = aktivitaetReferenzHelper.toString(customObject);
        
        // Prüfen, ob die überschriebene toString-Methode verwendet wurde
        assertEquals("Benutzerdefiniertes Objekt", result);
    }
}