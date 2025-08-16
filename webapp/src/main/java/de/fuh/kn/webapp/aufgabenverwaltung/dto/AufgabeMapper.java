package de.fuh.kn.webapp.aufgabenverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Aufgabe-Entity und AufgabeDto.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {TeilaufgabeMapper.class})
public interface AufgabeMapper extends EntityDtoMapper<Aufgabe, AufgabeDto> {

    /**
     * Konvertiert eine Aufgabe-Entity in ein AufgabeDto.
     * Die kurseinheitId wird aus der Kurseinheit-Entity extrahiert.
     *
     * @param aufgabe Die Aufgabe-Entity, die konvertiert werden soll
     * @return Das resultierende AufgabeDto
     */
    @Mapping(source = "kurseinheit.id", target = "kurseinheitId")
    @Mapping(target = "einfach", expression = "java(aufgabe.isEinfach())")
    @Override
    AufgabeDto toDto(Aufgabe aufgabe);

    /**
     * Konvertiert ein AufgabeDto in eine Aufgabe-Entity.
     * Die Kurseinheit-Entity und Teilaufgaben werden nicht automatisch gesetzt, 
     * sondern müssen separat gesetzt werden.
     *
     * @param aufgabeDto Das AufgabeDto, das konvertiert werden soll
     * @return Die resultierende Aufgabe-Entity
     */
    @Mapping(target = "kurseinheit", ignore = true)
    @Override
    Aufgabe toEntity(AufgabeDto aufgabeDto);

    /**
     * Konvertiert eine Liste von Aufgabe-Entities in eine Liste von AufgabeDtos.
     *
     * @param aufgaben Die Liste von Aufgabe-Entities
     * @return Die Liste von AufgabeDtos
     */
    List<AufgabeDto> toDtoList(List<Aufgabe> aufgaben);
}
