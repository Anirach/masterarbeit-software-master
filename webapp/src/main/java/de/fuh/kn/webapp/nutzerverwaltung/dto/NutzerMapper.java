package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.persistence.entity.Nutzer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Nutzer-Entity und NutzerDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 * 
 * Hinweis: Da Nutzer eine abstrakte Klasse ist, kann die toEntity-Methode nicht 
 * implementiert werden. Verwenden Sie stattdessen die konkreten Mapper für Student 
 * oder Kursbetreuer.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NutzerMapper {

    /**
     * Konvertiert eine Nutzer-Entity in ein NutzerDTO.
     *
     * @param nutzer Die Nutzer-Entity, die konvertiert werden soll
     * @return Das resultierende NutzerDTO
     */
    @Mapping(source = "istRegistriert", target = "istRegistriert")
    NutzerDTO toDto(Nutzer nutzer);

    /**
     * Konvertiert eine Liste von Nutzer-Entities in eine Liste von NutzerDTOs.
     *
     * @param nutzer Die Liste von Nutzer-Entities
     * @return Die Liste von NutzerDTOs
     */
    List<NutzerDTO> toDtoList(List<Nutzer> nutzer);

}
