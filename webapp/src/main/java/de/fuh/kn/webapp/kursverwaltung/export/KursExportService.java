package de.fuh.kn.webapp.kursverwaltung.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursExportDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursExportMapper;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursMaterialExportMapper;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service für den Export von Kursen.
 * Diese Klasse bietet Funktionalität zum Exportieren von Kursen mit allen zugehörigen Daten.
 */
@Service
public class KursExportService {

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
     * @param objectMapper Jackson ObjectMapper für die JSON-Serialisierung
     */
    @Autowired
    public KursExportService(KursService kursService,
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
     * Exportiert einen Kurs anhand seiner ID mit allen zugehörigen Daten.
     * Dazu gehören:
     * - Alle Kurseinheiten
     * - Alle Kursmaterialien (des Kurses und der Kurseinheiten)
     * - Alle Aufgaben aller Kurseinheiten
     *
     * @param kursId ID des zu exportierenden Kurses
     * @return Das Export-DTO des Kurses mit allen Daten
     * @throws IllegalArgumentException Wenn kein Kurs mit der angegebenen ID existiert
     */
    @Transactional(readOnly = true)
    public KursExportDTO exportiereKurs(Long kursId) {
        // Kurs mit allen Kurseinheiten laden
        KursDTO kursDTO = kursService.getKursByIdMitKurseinheiten(kursId);
        if (kursDTO == null) {
            throw new IllegalArgumentException("Kurs mit ID " + kursId + " nicht gefunden.");
        }

        // Export-DTO erstellen
        KursExportDTO kursExportDTO = kursExportMapper.toExportDto(kursDTO);

        // Alle Aufgaben sammeln
        List<AufgabeExportDTO> alleAufgaben = new ArrayList<>();
        for (KurseinheitDTO kurseinheit : kursDTO.getKurseinheiten()) {
            List<AufgabeDto> aufgaben = aufgabeService.getAufgabenByKurseinheitId(kurseinheit.getId());
            for (AufgabeDto aufgabe : aufgaben) {
                alleAufgaben.add(aufgabeExportMapper.toExportDto(aufgabe));
            }
        }
        kursExportDTO.setAufgaben(alleAufgaben);

        return kursExportDTO;
    }

    /**
     * Exportiert einen Kurs als JSON-String.
     *
     * @param kursExport Das Export-DTO des Kurses
     * @return Der Kurs als JSON-String
     * @throws RuntimeException Wenn die JSON-Serialisierung fehlschlägt
     */
    public String exportiereAlsJson(KursExportDTO kursExport) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kursExport);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Exportieren des Kurses als JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Exportiert einen Kurs als ZIP-Datei.
     * Die ZIP-Datei enthält:
     * - Die Kursdaten als JSON-Datei
     * - Alle Kursmaterialien als einzelne Dateien
     *
     * @param kursExport Das Export-DTO des Kurses
     * @return Die ZIP-Datei als Byte-Array
     * @throws RuntimeException Wenn die Erstellung der ZIP-Datei fehlschlägt
     */
    public byte[] exportiereAlsZip(KursExportDTO kursExport) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Kursdaten als JSON in die ZIP-Datei schreiben
            String kursJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kursExport);
            ZipEntry kursEntry = new ZipEntry("kurs.json");
            zos.putNextEntry(kursEntry);
            zos.write(kursJson.getBytes());
            zos.closeEntry();

            // Kursmaterialien des Kurses in die ZIP-Datei schreiben
            for (int i = 0; i < kursExport.getKursMaterialien().size(); i++) {
                var material = kursExport.getKursMaterialien().get(i);
                String materialFilename = "kurs_material_" + i + "_" + material.getName();
                addMaterialToZip(zos, material, materialFilename);
            }

            // Kursmaterialien der Kurseinheiten in die ZIP-Datei schreiben
            for (int i = 0; i < kursExport.getKurseinheiten().size(); i++) {
                var kurseinheit = kursExport.getKurseinheiten().get(i);
                for (int j = 0; j < kurseinheit.getKursMaterialien().size(); j++) {
                    var material = kurseinheit.getKursMaterialien().get(j);
                    String materialFilename = "kurseinheit_" + i + "_material_" + j + "_" + material.getName();
                    addMaterialToZip(zos, material, materialFilename);
                }
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Exportieren des Kurses als ZIP: " + e.getMessage(), e);
        }
    }

    /**
     * Fügt ein Kursmaterial zur ZIP-Datei hinzu.
     *
     * @param zos Der ZIP-OutputStream
     * @param material Das zu exportierende Kursmaterial
     * @param filename Der Dateiname innerhalb der ZIP-Datei
     * @throws IOException Wenn ein Fehler beim Schreiben in die ZIP-Datei auftritt
     */
    private void addMaterialToZip(ZipOutputStream zos, de.fuh.kn.webapp.kursverwaltung.dto.export.KursMaterialExportDTO material, String filename) throws IOException {
        if (material.getInhaltBase64() != null) {
            byte[] materialData = kursMaterialExportMapper.base64ToBytes(material.getInhaltBase64());
            if (materialData != null) {
                ZipEntry materialEntry = new ZipEntry(filename);
                zos.putNextEntry(materialEntry);
                zos.write(materialData);
                zos.closeEntry();
            }
        }
    }
}