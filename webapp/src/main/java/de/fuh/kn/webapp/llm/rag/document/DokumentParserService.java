package de.fuh.kn.webapp.llm.rag.document;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service für die Extraktion von Text aus verschiedenen Dokumenttypen.
 * Verwendet Spring AI's DocumentReader-Komponenten für die Textextraktion.
 */
@Service
@Slf4j
public class DokumentParserService {
    
    /**
     * Extrahiert Text aus einem Kursmaterial und gibt ihn als Spring AI Document-Objekte zurück.
     *
     * @param kursMaterial Das Kursmaterial, aus dem Text extrahiert werden soll.
     * @return Eine Liste von Document-Objekten mit extrahiertem Text und Metadaten.
     */
    public List<Document> extrahiereText(KursMaterialDTO kursMaterial) {
        if (kursMaterial == null || kursMaterial.getInhalt() == null) {
            log.warn("Ungültiges Kursmaterial oder leerer Inhalt");
            return Collections.emptyList();
        }
        
        String mimeType = kursMaterial.getMimeType();
        String dateiName = kursMaterial.getName();
        
        try {
            Resource resource = new ByteArrayResource(kursMaterial.getInhalt()) {
                @Override
                public String getFilename() {
                    return dateiName;
                }
            };
            
            List<Document> dokumente;
            
            // Metadaten anreichern
            Map<String, Object> customMetadata = new HashMap<>();
            customMetadata.put("kursMaterialId", kursMaterial.getId());
            customMetadata.put("source", dateiName);
            
            // Je nach MIME-Typ den entsprechenden DocumentReader verwenden
            if (mimeType.equals("application/pdf")) {
                dokumente = lesePdf(resource, customMetadata);
            } else {
                dokumente = leseMitTika(resource, mimeType, customMetadata);
            }
            
            log.info("Aus Kursmaterial {} ({}) wurden {} Dokumente extrahiert", 
                    kursMaterial.getId(), dateiName, dokumente.size());
            
            return dokumente;
        } catch (Exception e) {
            log.error("Fehler bei der Textextraktion aus Kursmaterial {}: {}", kursMaterial.getId(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Liest eine PDF-Datei mit verschiedenen Methoden, je nach Struktur der PDF.
     * Verwendet sowohl seitenweise als auch absatzbasierte Extraktion.
     *
     * @param resource Die PDF-Ressource.
     * @param customMetadata Benutzerdefinierte Metadaten für die erzeugten Dokumente.
     * @return Eine Liste von Document-Objekten.
     */
    private List<Document> lesePdf(Resource resource, Map<String, Object> customMetadata) {
        List<Document> dokumente = null;
        
        try {
            try {
                // Versuche zuerst absatzbasierte Extraktion
                ParagraphPdfDocumentReader paragraphReader = new ParagraphPdfDocumentReader(
                        resource,
                        PdfDocumentReaderConfig.builder()
                                .withPagesPerDocument(1)
                                .withPageExtractedTextFormatter(
                                        ExtractedTextFormatter.builder()
                                                .withNumberOfTopTextLinesToDelete(0)
                                                .build()
                                )
                                .build()
                );

                dokumente = paragraphReader.read();

                // Metadaten hinzufügen
                dokumente.forEach(document -> {
                    document.getMetadata().putAll(customMetadata);
                    document.getMetadata().put("documentType", "PDF");
                    document.getMetadata().put("extractionMethod", "paragraph");
                });
            }catch(Exception e){
                log.warn("Fehler bei PDF-Verarbeitung", e);
            }

            // Wenn keine Paragraphen gefunden wurden oder weniger als erwartet, 
            // versuche seitenweise Extraktion
            if (dokumente == null || dokumente.isEmpty() || dokumente.size() == 1) {
                PagePdfDocumentReader pageReader = new PagePdfDocumentReader(
                        resource,
                        PdfDocumentReaderConfig.builder()
                                .withPagesPerDocument(1)
                                .withPageTopMargin(0)
                                .withPageBottomMargin(0)
                                .withPageExtractedTextFormatter(
                                        ExtractedTextFormatter.builder()
                                                .withNumberOfTopTextLinesToDelete(0)
                                                .build()
                                )
                                .build()
                );

                dokumente = pageReader.read();

                // Metadaten hinzufügen
                dokumente.forEach(document -> {
                    document.getMetadata().putAll(customMetadata);
                    document.getMetadata().put("documentType", "PDF");
                    document.getMetadata().put("extractionMethod", "page");
                });
            }
            
            return dokumente;
        } catch (Exception e) {
            log.warn("PDF-Spezifische Extraktion fehlgeschlagen, verwende Tika: {}", e.getMessage());
            return leseMitTika(resource, "application/pdf", customMetadata);
        }
    }
    
    /**
     * Liest ein Dokument mit Apache Tika, das alle gängigen Formate unterstützt.
     *
     * @param resource Die Dokument-Ressource.
     * @param mimeType Der MIME-Typ des Dokuments.
     * @param customMetadata Benutzerdefinierte Metadaten für die erzeugten Dokumente.
     * @return Eine Liste von Document-Objekten.
     */
    private List<Document> leseMitTika(Resource resource, String mimeType, Map<String, Object> customMetadata) {
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        
        List<Document> documents = tikaReader.read();

        // Metadaten hinzufügen
        documents.forEach(document -> {
            document.getMetadata().putAll(customMetadata);
            document.getMetadata().put("documentType", mimeTypeToDocumentType(mimeType));
            document.getMetadata().put("extractionMethod", "tika");
        });

        return documents;
    }
    
    /**
     * Konvertiert einen MIME-Typ in einen lesbaren Dokumenttyp.
     *
     * @param mimeType Der MIME-Typ.
     * @return Der lesbare Dokumenttyp.
     */
    private String mimeTypeToDocumentType(String mimeType) {
        if (mimeType == null) {
            return "UNKNOWN";
        }
        
        return switch (mimeType) {
            case "application/pdf" -> "PDF";
            case "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "DOCX";
            case "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "PPTX";
            case "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "XLSX";
            case "text/plain" -> "TXT";
            default -> mimeType.toUpperCase();
        };
    }
}