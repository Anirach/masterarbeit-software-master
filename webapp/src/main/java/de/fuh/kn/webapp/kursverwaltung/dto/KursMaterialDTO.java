package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO zur Repräsentation von Kursmaterial-Daten in der Benutzeroberfläche.
 * Diese Klasse enthält alle relevanten Informationen eines Kursmaterials für die Anzeige
 * und Bearbeitung in der Kursmaterialübersicht und Detailansicht.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class KursMaterialDTO extends BaseDTO {
    
    /**
     * Der Name des Kursmaterials.
     */
    private String name;
    
    /**
     * Der Typ des Kursmaterials (DOKUMENT oder BILD).
     */
    private KursMaterialTyp typ;
    
    /**
     * Der MIME-Typ des Inhalts (z.B. "application/pdf", "image/png").
     */
    private String mimeType;
    
    /**
     * Die ID der zugehörigen Kurseinheit (kann null sein, wenn das Material zum Kurs gehört).
     */
    private Long kurseinheitId;
    
    /**
     * Die ID des zugehörigen Kurses (kann null sein, wenn das Material zur Kurseinheit gehört).
     */
    private Long kursId;
    
    /**
     * Die binären Inhaltsdaten des Kursmaterials.
     * Diese werden für die Download-Funktion verwendet und normalerweise nur bei Bedarf befüllt.
     */
    private byte[] inhalt;

    private Boolean indexiert;

    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("KursMaterial")
     */
    @Override
    public String getEntityTypeName() {
        return "KursMaterial";
    }

    @Override
    public String getEntityDisplayName() {
        return this.getName();
    }

    /**
     * Enum für den Typ des Kursmaterials.
     */
    public enum KursMaterialTyp {
        DOKUMENT,
        BILD
    }
}
