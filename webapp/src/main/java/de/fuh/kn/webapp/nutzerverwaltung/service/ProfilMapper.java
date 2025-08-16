package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.persistence.entity.Nutzer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper zur Konvertierung zwischen Nutzer-Entity und ProfilAenderungDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfilMapper {

    /**
     * Konvertiert eine Nutzer-DTO in ein ProfilAenderungDTO.
     *
     * @param nutzerDto Die Nutzer-DTO, die konvertiert werden soll
     * @return Das resultierende ProfilAenderungDTO
     */
    @Mapping(source = "email", target = "email")
    @Mapping(source = "vorname", target = "vorname")
    @Mapping(source = "nachname", target = "nachname")
    ProfilAenderungDTO toDto(NutzerDTO nutzerDto);

    /**
     * Aktualisiert eine Nutzer-Entity mit den Werten aus einem ProfilAenderungDTO.
     *
     * @param profilAenderungDTO Das DTO mit den neuen Werten
     * @param nutzer Die zu aktualisierende Nutzer-Entity
     */
    @Mapping(source = "email", target = "email")
    @Mapping(source = "vorname", target = "vorname")
    @Mapping(source = "nachname", target = "nachname")
    void updateNutzerFromDto(ProfilAenderungDTO profilAenderungDTO, @MappingTarget Nutzer nutzer);

    /**
     * Aktualisiert eine Nutzer-DTO mit den Werten aus einem ProfilAenderungDTO.
     *
     * @param profilAenderungDTO Das DTO mit den neuen Werten
     * @param nutzerDTO Die zu aktualisierende Nutzer-DTO
     */
    @Mapping(source = "email", target = "email")
    @Mapping(source = "vorname", target = "vorname")
    @Mapping(source = "nachname", target = "nachname")
    void updateNutzerFromDto(ProfilAenderungDTO profilAenderungDTO, @MappingTarget NutzerDTO nutzerDTO);
}
