package de.fuh.kn.webapp.kursverwaltung.dto.export;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO für den Export von Kursmaterial-Daten.
 * Enthält alle für den Export relevanten Informationen eines Kursmaterials,
 * wobei binäre Inhalte als Base64-kodierter String gespeichert werden.
 */
@Getter
@Setter
public class KursMaterialExportDTO {
    
    /**
     * Die ID des Kursmaterials.
     */
    private Long id;
    
    /**
     * Der Name des Kursmaterials.
     */
    private String name;
    
    /**
     * Der Typ des Kursmaterials (DOKUMENT oder BILD).
     */
    private KursMaterialDTO.KursMaterialTyp typ;
    
    /**
     * Der MIME-Typ des Inhalts (z.B. "application/pdf", "image/png").
     */
    private String mimeType;
    
    /**
     * Die binären Inhaltsdaten des Kursmaterials als Base64-kodierter String.
     * Diese werden für Import/Export verwendet.
     */
    private String inhaltBase64;
    
    /**
     * Gibt an, ob das Material bereits im Vektorspeicher indexiert wurde.
     * Nur relevant für Dokumente (Typ = DOKUMENT).
     */
    private Boolean indexiert = false;
}