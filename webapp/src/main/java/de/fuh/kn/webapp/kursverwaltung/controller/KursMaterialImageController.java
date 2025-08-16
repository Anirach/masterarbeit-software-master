package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/**
 * Controller für den Zugriff auf Bilder aus dem Kursmaterial.
 * Dieser Controller erlaubt Studenten und Kursbetreuern den Zugriff auf Bilder aus den Kursmaterialien.
 * Bei Studenten wird zusätzlich geprüft, ob der entsprechende Kurs belegt wird.
 */
@Controller
@Slf4j
public class KursMaterialImageController {

    private final KursMaterialService kursMaterialService;
    private final KursService kursService;
    private final KurseinheitService kurseinheitService;
    private final NutzerService nutzerService;
    private final BelegungService belegungService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param kursMaterialService Der Service für die Verwaltung von Kursmaterialien
     * @param kursService Der Service für die Verwaltung von Kursen
     * @param kurseinheitService Der Service für die Verwaltung von Kurseinheiten
     * @param nutzerService Der Service für die Verwaltung von Nutzern
     * @param belegungService Der Service für die Verwaltung von Kursbelegungen
     */
    @Autowired
    public KursMaterialImageController(
            KursMaterialService kursMaterialService,
            KursService kursService,
            KurseinheitService kurseinheitService,
            NutzerService nutzerService,
            BelegungService belegungService) {
        this.kursMaterialService = kursMaterialService;
        this.kursService = kursService;
        this.kurseinheitService = kurseinheitService;
        this.nutzerService = nutzerService;
        this.belegungService = belegungService;
    }

    /**
     * Gibt ein Bild aus einer Kurseinheit oder dem zugehörigen Kurs zurück.
     * Sucht zuerst in der Kurseinheit nach dem Bild und dann im zugehörigen Kurs.
     * Prüft für Studenten, ob der zugehörige Kurs belegt wird.
     *
     * @param kurseinheitId Die ID der Kurseinheit
     * @param bildName Der Name des Bildes
     * @return Das Bild als ResponseEntity
     */
    @GetMapping("/material/kurseinheit/{kurseinheitId}/bild")
    public ResponseEntity<byte[]> getKurseinheitBild(
            @PathVariable("kurseinheitId") Long kurseinheitId,
            @RequestParam("name") String bildName) {
        
        // Kurseinheit abrufen
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kurseinheit nicht gefunden");
        }
        
        // Zugehörigen Kurs laden
        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
        if (kurs == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kurs nicht gefunden");
        }
        
        // Eingeschriebenen Nutzer prüfen
        NutzerDTO nutzer = nutzerService.getAuthenticatedNutzer();
        
        // Für Studenten: Prüfen, ob der Kurs belegt wird
        if (nutzer instanceof StudentDTO student) {
            if (!belegungService.isStudentEnrolledInKurs(student, kurs)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kein Zugriff auf dieses Bild - Kurs nicht belegt");
            }
        } 
        // Für alle anderen Nutzer: Nur Kursbetreuer haben Zugriff
        else if (!(nutzer instanceof KursbetreuerDTO)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kein Zugriff auf dieses Bild");
        }
        
        // Decode URL parameters that might be encoded (spaces as %20 etc.)
        try {
            bildName = java.net.URLDecoder.decode(bildName, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Fehler beim URL-Decoding des Bildnamens: {}", e.getMessage());
            // Continue with the original bildName if decoding fails
        }
        
        // Zuerst in der Kurseinheit nach dem Bild suchen
        KursMaterialDTO bild = kursMaterialService.findBildByNameAndKurseinheitId(kurseinheit.getId(), bildName);
        
        // Wenn das Bild in der Kurseinheit nicht gefunden wurde, im zugehörigen Kurs suchen
        if (bild == null) {
            log.debug("Bild '{}' nicht in Kurseinheit {} gefunden, suche im Kurs {}", bildName, kurseinheit.getId(), kurs.getId());
            bild = kursMaterialService.findBildByNameAndKursId(kurs.getId(), bildName);
        }
        
        if (bild == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bild nicht gefunden");
        }
        
        return createImageResponse(bild);
    }
    
    /**
     * Erstellt eine HTTP-Response für ein Bild.
     *
     * @param bild Das Bild als DTO
     * @return ResponseEntity mit dem Bildinhalt und passenden HTTP-Headern
     */
    private ResponseEntity<byte[]> createImageResponse(KursMaterialDTO bild) {
        if (bild == null || bild.getInhalt() == null) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(bild.getMimeType()));
        headers.setContentDispositionFormData("inline", bild.getName());
        
        // Cache-Control-Header setzen
        headers.setCacheControl("max-age=86400"); // 24 Stunden cachen
        
        return new ResponseEntity<>(bild.getInhalt(), headers, HttpStatus.OK);
    }
}
