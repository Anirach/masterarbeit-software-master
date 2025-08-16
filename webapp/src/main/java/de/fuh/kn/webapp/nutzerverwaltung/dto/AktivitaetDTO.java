package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Objekt für Aktivitätsdaten.
 * Wird zur Übertragung von Aktivitätsinformationen zwischen den Anwendungsschichten verwendet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AktivitaetDTO extends BaseDTO {
    private LocalDateTime zeitpunkt;
    private Long nutzerId;
    private String nutzerName;
    private AktivitaetsTyp aktivitaetsTyp;
    private String beschreibung;
    private String details;
    private Boolean erfolg;
    private String referenzTyp;
    private Long referenzId;
    private String referenzName;
    
    // Transiente Felder für erweiterte Informationen
    private transient Long belegungId;
    private transient Long parentAufgabeId;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("Aktivitaet")
     */
    @Override
    public String getEntityTypeName() {
        return "Aktivitaet";
    }

    @Override
    public String getEntityDisplayName() {
        throw new UnsupportedOperationException();
    }
}
