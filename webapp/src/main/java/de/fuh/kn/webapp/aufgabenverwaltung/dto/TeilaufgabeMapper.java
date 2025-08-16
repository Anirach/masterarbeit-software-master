package de.fuh.kn.webapp.aufgabenverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Teilaufgabe-Entity und TeilaufgabeDto.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeilaufgabeMapper extends EntityDtoMapper<Teilaufgabe, TeilaufgabeDto> {

    /**
     * Konvertiert eine Teilaufgabe-Entity in ein TeilaufgabeDto.
     * Die aufgabeId wird aus der Aufgabe-Entity extrahiert.
     *
     * @param teilaufgabe Die Teilaufgabe-Entity, die konvertiert werden soll
     * @return Das resultierende TeilaufgabeDto
     */
    @Mapping(source = "aufgabe.id", target = "aufgabeId")
    @Override
    TeilaufgabeDto toDto(Teilaufgabe teilaufgabe);

    /**
     * Konvertiert ein TeilaufgabeDto in eine Teilaufgabe-Entity.
     * Die Aufgabe-Entity wird nicht automatisch gesetzt, sondern muss separat gesetzt werden.
     *
     * @param teilaufgabeDto Das TeilaufgabeDto, das konvertiert werden soll
     * @return Die resultierende Teilaufgabe-Entity
     */
    @Mapping(target = "aufgabe", ignore = true)
    @Mapping(target = "loesungsVersuche", ignore = true)
    @Override
    Teilaufgabe toEntity(TeilaufgabeDto teilaufgabeDto);

    /**
     * Konvertiert eine Liste von Teilaufgabe-Entities in eine Liste von TeilaufgabeDtos.
     *
     * @param teilaufgaben Die Liste von Teilaufgabe-Entities
     * @return Die Liste von TeilaufgabeDtos
     */
    List<TeilaufgabeDto> toDtoList(List<Teilaufgabe> teilaufgaben);
}
