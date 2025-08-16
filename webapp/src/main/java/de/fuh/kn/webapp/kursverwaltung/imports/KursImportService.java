package de.fuh.kn.webapp.kursverwaltung.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursExportDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursExportMapper;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursMaterialExportDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursMaterialExportMapper;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service für den Import von Kursen.
 * Diese Klasse bietet Funktionalität zum Importieren von Kursen mit allen zugehörigen Daten.
 */
@Service
@Slf4j
public class KursImportService {

    private final KursService kursService;
    private final KurseinheitService kurseinheitService;
    private final KursMaterialService kursMaterialService;
    private final AufgabeService aufgabeService;
    private final KursExportMapper kursExportMapper;
    private final AufgabeExportMapper aufgabeExportMapper;
    private final KursMaterialExportMapper kursMaterialExportMapper;
    private final ObjectMapper objectMapper;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services und Mapper.
     *
     * @param kursService Service für die Verwaltung von Kursen
     * @param kurseinheitService Service für die Verwaltung von Kurseinheiten
     * @param kursMaterialService Service für die Verwaltung von Kursmaterialien
     * @param aufgabeService Service für die Verwaltung von Aufgaben
     * @param kursExportMapper Mapper für die Konvertierung zwischen KursDTO und KursExportDTO
     * @param aufgabeExportMapper Mapper für die Konvertierung zwischen AufgabeDto und AufgabeExportDTO
     * @param kursMaterialExportMapper Mapper für die Konvertierung zwischen KursMaterialDTO und KursMaterialExportDTO
     * @param objectMapper Jackson ObjectMapper für die JSON-Deserialisierung
     */
    @Autowired
    public KursImportService(KursService kursService,
                           KurseinheitService kurseinheitService,
                           KursMaterialService kursMaterialService,
                           AufgabeService aufgabeService,
                           KursExportMapper kursExportMapper,
                           AufgabeExportMapper aufgabeExportMapper,
                           KursMaterialExportMapper kursMaterialExportMapper,
                           ObjectMapper objectMapper) {
        this.kursService = kursService;
        this.kurseinheitService = kurseinheitService;
        this.kursMaterialService = kursMaterialService;
        this.aufgabeService = aufgabeService;
        this.kursExportMapper = kursExportMapper;
        this.aufgabeExportMapper = aufgabeExportMapper;
        this.kursMaterialExportMapper = kursMaterialExportMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Importiert einen Kurs aus einer JSON-Datei.
     *
     * @param jsonFile Die zu importierende JSON-Datei
     * @return Das DTO des importierten Kurses
     * @throws IOException Wenn ein Fehler beim Lesen der Datei auftritt
     */
    @Transactional
    public KursDTO importiereAusJson(MultipartFile jsonFile) throws IOException {
        if (jsonFile == null || jsonFile.isEmpty()) {
            throw new IllegalArgumentException("Die JSON-Datei darf nicht leer sein.");
        }
        
        String jsonContent = new String(jsonFile.getBytes());
        return importiereAusJson(jsonContent);
    }

    /**
     * Importiert einen Kurs aus einem JSON-String.
     *
     * @param jsonString Der JSON-String mit dem zu importierenden Kurs
     * @return Das DTO des importierten Kurses
     * @throws IOException Wenn ein Fehler beim Parsen des JSON-Strings auftritt
     */
    @Transactional
    public KursDTO importiereAusJson(String jsonString) throws IOException {
        KursExportDTO kursExport = objectMapper.readValue(jsonString, KursExportDTO.class);

        log.info("Importiere Kurs: {}", kursExport.getName());
        log.info("Anzahl Kurseinheiten: {}", kursExport.getKurseinheiten().size());
        log.info("Anzahl Aufgaben: {}", kursExport.getAufgaben().size());

        return speichereImportiertenKurs(kursExport);
    }

    /**
     * Importiert einen Kurs aus einer ZIP-Datei als MultipartFile.
     *
     * @param zipFile Die zu importierende ZIP-Datei als MultipartFile
     * @return Das DTO des importierten Kurses
     * @throws IOException Wenn ein Fehler beim Lesen der ZIP-Datei auftritt
     */
    @Transactional
    public KursDTO importiereAusZip(MultipartFile zipFile) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Die ZIP-Datei darf nicht leer sein.");
        }

        return importiereAusZip(zipFile.getBytes());
    }

    /**
     * Importiert einen Kurs aus einer ZIP-Datei als Byte-Array.
     *
     * @param zipData Die zu importierende ZIP-Datei als Byte-Array
     * @return Das DTO des importierten Kurses
     * @throws IOException Wenn ein Fehler beim Lesen der ZIP-Datei auftritt
     */
    @Transactional
    protected KursDTO importiereAusZip(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            // Kursdaten aus der ZIP-Datei extrahieren
            String kursJson = null;
            Map<String, byte[]> materialienData = new HashMap<>();
            
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String entryName = zipEntry.getName();
                
                if (entryName.equals("kurs.json")) {
                    // Kursdaten aus JSON laden
                    byte[] buffer = readAllBytes(zis);
                    kursJson = new String(buffer);
                } else {
                    // Materialdaten speichern
                    byte[] buffer = readAllBytes(zis);

                    // Extrahiere den reinen Dateinamen ohne Pfad
                    String fileName = entryName;
                    if (entryName.contains("/")) {
                        fileName = entryName.substring(entryName.lastIndexOf("/") + 1);
                    }

                    // Ignoriere verzeichnisse und leere Dateien
                    if (!fileName.isEmpty() && buffer.length > 0) {
                        materialienData.put(fileName, buffer);
                        log.info("Material aus ZIP-Datei extrahiert: {}. Größe: {} Bytes", fileName, buffer.length);
                    }
                }
                zis.closeEntry();
            }
            
            if (kursJson == null) {
                throw new IllegalArgumentException("Die ZIP-Datei enthält keine Kursdaten (kurs.json).");
            }
            
            // Kurs aus JSON importieren
            KursExportDTO kursExport = objectMapper.readValue(kursJson, KursExportDTO.class);
            
            // Materialien aus der ZIP-Datei ergänzen und in den kursExport integrieren
            ergaenzeMaterialienAusZip(kursExport, materialienData);

            return speichereImportiertenKurs(kursExport);
        }
    }

    /**
     * Hilfsmethode zum Lesen aller Bytes aus einem ZipInputStream.
     *
     * @param zis Der ZipInputStream
     * @return Die gelesenen Bytes
     * @throws IOException Wenn ein Fehler beim Lesen auftritt
     */
    private byte[] readAllBytes(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = zis.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Ergänzt die Materialien aus einer ZIP-Datei zum KursExportDTO.
     * Diese Methode ersetzt die Base64-kodierten Inhalte in den KursMaterialExportDTOs durch die
     * tatsächlichen Binärdaten aus der ZIP-Datei, wenn die entsprechenden Dateien vorhanden sind.
     *
     * @param kursExport Das KursExportDTO, das die Materialien enthält
     * @param materialienData Map mit Dateinamen und entsprechenden Binärdaten aus der ZIP-Datei
     */
    private void ergaenzeMaterialienAusZip(KursExportDTO kursExport, Map<String, byte[]> materialienData) {
        log.info("Beginne Ergänzung von Materialien aus ZIP-Datei");
        log.info("Anzahl Materialien in ZIP: {}", materialienData.size());
        log.info("Verfügbare Materialien in ZIP: {}", String.join(", ", materialienData.keySet()));
        log.info("Anzahl Kursmaterialien im Export: {}", kursExport.getKursMaterialien().size());

        // Prüfen, ob materialienData leer ist
        if (materialienData.isEmpty()) {
            log.warn("Keine Materialien in der ZIP-Datei gefunden.");
            return;
        }
        // 1. Kursmaterialien verarbeiten
        for (KursMaterialExportDTO material : kursExport.getKursMaterialien()) {
            String dateiname = material.getName();
            String dateinameOhnePfad = dateiname;

            // Extrahiere Dateinamen ohne Pfad für den Vergleich
            if (dateiname.contains("/")) {
                dateinameOhnePfad = dateiname.substring(dateiname.lastIndexOf("/") + 1);
            }

            // Versuche mit exaktem Namen zu finden
            if (materialienData.containsKey(dateiname)) {
                // Base64-Inhalt durch tatsächliche Binärdaten ersetzen
                material.setInhaltBase64(Base64.getEncoder().encodeToString(materialienData.get(dateiname)));
                log.info("Material '{}' aus ZIP-Datei ergänzt", dateiname);
            }
            // Versuche mit Namen ohne Pfad zu finden
            else if (materialienData.containsKey(dateinameOhnePfad)) {
                material.setInhaltBase64(Base64.getEncoder().encodeToString(materialienData.get(dateinameOhnePfad)));
                log.info("Material '{}' aus ZIP-Datei mit Dateinamen ohne Pfad '{}' ergänzt", dateiname, dateinameOhnePfad);
            } else {
                log.warn("Material '{}' konnte nicht in ZIP-Datei gefunden werden", dateiname);
            }
        }

        // 2. Materialien in Kurseinheiten verarbeiten
        for (var kurseinheit : kursExport.getKurseinheiten()) {
            for (KursMaterialExportDTO material : kurseinheit.getKursMaterialien()) {
                String dateiname = material.getName();
                String dateinameOhnePfad = dateiname;

                // Extrahiere Dateinamen ohne Pfad für den Vergleich
                if (dateiname.contains("/")) {
                    dateinameOhnePfad = dateiname.substring(dateiname.lastIndexOf("/") + 1);
                }

                // Versuche mit exaktem Namen zu finden
                if (materialienData.containsKey(dateiname)) {
                    // Base64-Inhalt durch tatsächliche Binärdaten ersetzen
                    material.setInhaltBase64(Base64.getEncoder().encodeToString(materialienData.get(dateiname)));
                    log.info("Material '{}' aus ZIP-Datei für Kurseinheit ergänzt", dateiname);
                }
                // Versuche mit Namen ohne Pfad zu finden
                else if (materialienData.containsKey(dateinameOhnePfad)) {
                    material.setInhaltBase64(Base64.getEncoder().encodeToString(materialienData.get(dateinameOhnePfad)));
                    log.info("Material '{}' aus ZIP-Datei mit Dateinamen ohne Pfad '{}' für Kurseinheit ergänzt", dateiname, dateinameOhnePfad);
                } else {
                    log.warn("Material '{}' für Kurseinheit konnte nicht in ZIP-Datei gefunden werden", dateiname);
                }
            }
        }
    }

    /**
     * Speichert einen importierten Kurs in der Datenbank.
     *
     * @param kursExport Das Export-DTO des Kurses
     * @return Das DTO des gespeicherten Kurses
     */
    @Transactional
    public KursDTO speichereImportiertenKurs(KursExportDTO kursExport) {
        // 1. Kurs importieren und speichern
        KursDTO kursDTO = kursExportMapper.fromExportDto(kursExport);
        
        // IDs zurücksetzen, damit neue Entitäten erstellt werden
        kursDTO.setId(null);
        kursDTO.setKurseinheiten(new ArrayList<>());
        kursDTO.setKursMaterialien(new ArrayList<>());
        
        // Kurs speichern
        log.info("Speichere importierten Kurs: {}", kursDTO.getName());
        KursDTO gespeicherterKurs = kursService.erstelleKurs(kursDTO);
        log.info("Kurs erfolgreich gespeichert mit ID: {}", gespeicherterKurs.getId());
        
        // 2. Kursmaterialien des Kurses speichern
        log.info("Beginne Import von {} Kursmaterialien", kursExport.getKursMaterialien().size());
        int kursMaterialCount = 0;
        for (KursMaterialExportDTO materialExport : kursExport.getKursMaterialien()) {
            KursMaterialDTO materialDTO = kursMaterialExportMapper.fromExportDto(materialExport);
            materialDTO.setId(null);
            materialDTO.setKursId(gespeicherterKurs.getId());
            KursMaterialDTO gespeichertesMaterial = kursMaterialService.erstelleKursMaterial(materialDTO);
            kursMaterialCount++;
            log.info("Kursmaterial '{}' (Typ: {}) erfolgreich gespeichert mit ID: {}",
                    gespeichertesMaterial.getName(), gespeichertesMaterial.getTyp(), gespeichertesMaterial.getId());
        }
        
        log.info("Insgesamt {} Kursmaterialien erfolgreich importiert", kursMaterialCount);

        // 3. Kurseinheiten mit Materialien speichern
        log.info("Beginne Import von {} Kurseinheiten", kursExport.getKurseinheiten().size());
        Map<Long, Long> alteZuNeueKurseinheitIds = new HashMap<>();
        
        for (int i = 0; i < kursExport.getKurseinheiten().size(); i++) {
            var kurseinheitExport = kursExport.getKurseinheiten().get(i);
            KurseinheitDTO kurseinheitDTO = new KurseinheitDTO();
            kurseinheitDTO.setName(kurseinheitExport.getName());
            kurseinheitDTO.setReihenfolge(kurseinheitExport.getReihenfolge());
            kurseinheitDTO.setKursId(gespeicherterKurs.getId());
            
            // Kurseinheit speichern
            KurseinheitDTO gespeicherteKurseinheit = kurseinheitService.erstelleKurseinheit(kurseinheitDTO);
            
            // Alte-zu-Neue ID-Mapping für später speichern
            alteZuNeueKurseinheitIds.put(kurseinheitExport.getId(), gespeicherteKurseinheit.getId());
            log.info("Kurseinheit '{}' erfolgreich gespeichert mit ID: {} (alte ID: {})",
                    gespeicherteKurseinheit.getName(), gespeicherteKurseinheit.getId(), kurseinheitExport.getId());
            
            // Kursmaterialien der Kurseinheit speichern
            int kurseinheitMaterialCount = 0;
            log.info("Importiere {} Materialien für Kurseinheit '{}'",
                    kurseinheitExport.getKursMaterialien().size(), gespeicherteKurseinheit.getName());
            for (KursMaterialExportDTO materialExport : kurseinheitExport.getKursMaterialien()) {
                KursMaterialDTO materialDTO = kursMaterialExportMapper.fromExportDto(materialExport);
                materialDTO.setId(null);
                materialDTO.setKurseinheitId(gespeicherteKurseinheit.getId());
                KursMaterialDTO gespeichertesMaterial = kursMaterialService.erstelleKursMaterial(materialDTO);
                kurseinheitMaterialCount++;
                log.info("Material '{}' (Typ: {}) für Kurseinheit '{}' gespeichert mit ID: {}",
                        gespeichertesMaterial.getName(), gespeichertesMaterial.getTyp(),
                        gespeicherteKurseinheit.getName(), gespeichertesMaterial.getId());
            }
        }
        
        log.info("Insgesamt {} Kurseinheiten erfolgreich importiert", kursExport.getKurseinheiten().size());

        // 4. Aufgaben speichern
        log.info("Beginne Import von {} Aufgaben", kursExport.getAufgaben().size());
        int importedCount = 0;
        for (AufgabeExportDTO aufgabeExport : kursExport.getAufgaben()) {
            // Aufgabe in AufgabeDto umwandeln
            AufgabeDto aufgabeDto = aufgabeExportMapper.fromExportDto(aufgabeExport);

            // ID zurücksetzen, damit eine neue Entität erstellt wird
            aufgabeDto.setId(null);

            // Wenn keine Teilaufgaben vorhanden sind, füge eine leere Liste hinzu
            if (aufgabeDto.getTeilaufgaben() == null) {
                aufgabeDto.setTeilaufgaben(new ArrayList<>());
            }

            // Teilaufgaben IDs zurücksetzen
            aufgabeDto.getTeilaufgaben().forEach(teilaufgabe -> teilaufgabe.setId(null));

            // Kurseinheit-ID für jede Aufgabe festlegen - verwende die erste Kurseinheit, wenn keine Zuordnung möglich
            Long alteKurseinheitId = aufgabeDto.getKurseinheitId();
            if (alteKurseinheitId != null && alteZuNeueKurseinheitIds.containsKey(alteKurseinheitId)) {
                aufgabeDto.setKurseinheitId(alteZuNeueKurseinheitIds.get(alteKurseinheitId));
            } else if (!kursExport.getKurseinheiten().isEmpty()) {
                // Wenn keine passende Kurseinheit-ID gefunden wurde, verwende die erste Kurseinheit
                Long neueKurseinheitId = alteZuNeueKurseinheitIds.get(kursExport.getKurseinheiten().get(0).getId());
                aufgabeDto.setKurseinheitId(neueKurseinheitId);
                log.warn("Keine passende Kurseinheit für Aufgabe '{}' gefunden. Verwende erste Kurseinheit mit ID {}",
                        aufgabeDto.getTitel(), neueKurseinheitId);
            } else {
                log.warn("Keine Kurseinheiten vorhanden. Überspringe Aufgabe: {}", aufgabeDto.getTitel());
                continue;
            }

            // Aufgabe speichern und zählen
            AufgabeDto gespeicherteAufgabe = aufgabeService.erstelleAufgabe(aufgabeDto);
            if (gespeicherteAufgabe != null) {
                importedCount++;
                log.info("Aufgabe '{}' erfolgreich importiert mit ID: {}",
                        gespeicherteAufgabe.getTitel(), gespeicherteAufgabe.getId());

                // Log Teilaufgaben
                if (gespeicherteAufgabe.getTeilaufgaben() != null && !gespeicherteAufgabe.getTeilaufgaben().isEmpty()) {
                    log.info("  - Mit {} Teilaufgaben", gespeicherteAufgabe.getTeilaufgaben().size());
                }
            }
        }

        log.info("Insgesamt {} Aufgaben erfolgreich importiert", importedCount);
        log.info("Kurs-Import abgeschlossen: {} (ID: {})", gespeicherterKurs.getName(), gespeicherterKurs.getId());

        return gespeicherterKurs;
    }

    /**
     * Prüft, ob eine Datei ein JSON ist.
     *
     * @param file Die zu prüfende Datei
     * @return true, wenn die Datei ein JSON ist, sonst false
     */
    public boolean isJsonFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        // Prüfe auf Content-Type
        boolean hasJsonContentType = contentType != null && 
                (contentType.equals("application/json") || contentType.equals("text/json"));
        
        // Prüfe auf Dateiendung
        boolean hasJsonExtension = fileName != null && fileName.toLowerCase().endsWith(".json");
        
        return hasJsonContentType || hasJsonExtension;
    }

    /**
     * Prüft, ob eine Datei ein ZIP ist.
     *
     * @param file Die zu prüfende Datei
     * @return true, wenn die Datei ein ZIP ist, sonst false
     */
    public boolean isZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        // Prüfe auf Content-Type
        boolean hasZipContentType = contentType != null && 
                (contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed"));
        
        // Prüfe auf Dateiendung
        boolean hasZipExtension = fileName != null && fileName.toLowerCase().endsWith(".zip");
        
        return hasZipContentType || hasZipExtension;
    }
}