package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetReferenzHelper;
import de.fuh.kn.webapp.persistence.entity.Aktivitaet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * MapStruct-Mapper für die Konvertierung zwischen Aktivitaet-Entity und AktivitaetDTO.
 */
@Mapper(componentModel = "spring")
public abstract class AktivitaetMapper {

    @Autowired
    private AktivitaetReferenzHelper aktivitaetReferenzHelper;

    /**
     * Konvertiert eine Aktivitaet-Entity in ein AktivitaetDTO.
     *
     * @param entity Die zu konvertierende Aktivitaet-Entity
     * @return Das konvertierte AktivitaetDTO
     */
    @Mapping(source = "nutzer.id", target = "nutzerId")
    @Mapping(source = "nutzer.displayName", target = "nutzerName")
    @Mapping(source = "referenzTyp", target = "referenzTyp")
    @Mapping(source = "referenzId", target = "referenzId")
    @Mapping(source = ".", target = "referenzName", qualifiedByName = "resolveReferenzName")
    public abstract AktivitaetDTO toDTO(Aktivitaet entity);

    /**
     * Ermittelt den Anzeigenamen für eine Referenz basierend auf Typ und ID.
     *
     * @param aktivitaet Die Aktivität mit Referenztyp und ReferenzId
     * @return Der Anzeigename für das referenzierte Objekt
     */
    @Named("resolveReferenzName")
    public String resolveReferenzName(Aktivitaet aktivitaet) {
        return aktivitaetReferenzHelper.toString(aktivitaet.getReferenzTyp(), aktivitaet.getReferenzId());
    }

    public abstract List<AktivitaetDTO> toDtoList(List<Aktivitaet> kurse);

}
