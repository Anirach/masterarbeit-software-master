package de.fuh.kn.webapp.common.aktivitaeten;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.common.dto.BaseDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Util-Klasse um aus referenzierten Entities oder DTOs vernüftige Strings zu machen
 */
@Component
public class AktivitaetReferenzHelper {

    @Autowired
    protected NutzerService nutzerService;

    @Autowired
    protected KursService kursService;

    @Autowired
    protected KurseinheitService kurseinheitService;

    @Autowired
    protected KursMaterialService kursMaterialService;
    @Autowired
    private AufgabeService aufgabeService;

    @Autowired
    private TeilaufgabeService teilaufgabeService;

    /**
     * Ermittelt den Anzeigenamen für eine Referenz basierend auf Typ und ID.
     *
     * @param refType Referenztyp aus einer Aktivität
     * @param refId   Referenz-ID aus einer Aktivität
     * @return Der Anzeigename für das referenzierte Objekt
     */
    public String toString(String refType, Long refId){
        if(refType == null || refId == null){
            return "ID: "+refId+" (gelöscht)";
        }
        Optional<? extends BaseDTO> dtoOptional = switch (refType) {
            case "Student", "Kursbetreuer" -> nutzerService.getNutzerById(refId);
            case "Kurs" -> Optional.ofNullable(kursService.getKursById(refId));
            case "Kurseinheit" -> Optional.ofNullable(kurseinheitService.getKurseinheitById(refId));
            case "Kursmaterial" -> Optional.ofNullable(kursMaterialService.getKursMaterialById(refId));
            case "Aufgabe" -> Optional.ofNullable(aufgabeService.getAufgabeById(refId));
            case "Teilaufgabe" -> {
                // Get the TeilaufgabeDto first
                TeilaufgabeDto teilaufgabeDto = teilaufgabeService.getTeilaufgabeById(refId);
                if (teilaufgabeDto != null) {
                    // Then get the parent Aufgabe
                    yield Optional.ofNullable(aufgabeService.getAufgabeById(teilaufgabeDto.getAufgabeId()));
                } else {
                    yield Optional.empty();
                }
            }
            default -> Optional.empty();
        };

        if(dtoOptional.isPresent()){
            return dtoOptional.get().getEntityDisplayName();
        }else{
            return "ID: "+refId+" (gelöscht)";
        }
    }

    /**
     * Wandelt ein BaseEntity oder BaseDTO oder normales Objekt zu einem anzeigbaren String um
     * @param object umzuwandelndes Objekt
     * @return Der Anzeigename
     */
    public String toString(Object object){
        if(object instanceof BaseDTO){
            return ((BaseDTO)object).getEntityDisplayName();
        }
        return object.toString();
    }



}
