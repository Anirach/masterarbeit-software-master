package de.fuh.kn.webapp.aufgabenverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.LoesungsVersuch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen LoesungsVersuch-Entity und LoesungsVersuchDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoesungsVersuchMapper extends EntityDtoMapper<LoesungsVersuch, LoesungsVersuchDTO> {

    /**
     * Konvertiert eine LoesungsVersuch-Entity in ein LoesungsVersuchDTO.
     * Die IDs der verknüpften Entitäten werden extrahiert.
     *
     * @param loesungsVersuch Die LoesungsVersuch-Entity, die konvertiert werden soll
     * @return Das resultierende LoesungsVersuchDTO
     */
    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "teilaufgabe.id", target = "teilaufgabeId")
    @Override
    LoesungsVersuchDTO toDto(LoesungsVersuch loesungsVersuch);

    /**
     * Konvertiert ein LoesungsVersuchDTO in eine LoesungsVersuch-Entity.
     * Die verknüpften Entitäten werden nicht automatisch gesetzt,
     * sondern müssen separat gesetzt werden.
     *
     * @param loesungsVersuchDTO Das LoesungsVersuchDTO, das konvertiert werden soll
     * @return Die resultierende LoesungsVersuch-Entity
     */
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "teilaufgabe", ignore = true)
    @Override
    LoesungsVersuch toEntity(LoesungsVersuchDTO loesungsVersuchDTO);

    /**
     * Konvertiert eine Liste von LoesungsVersuch-Entities in eine Liste von LoesungsVersuchDTOs.
     *
     * @param loesungsVersuche Die Liste von LoesungsVersuch-Entities
     * @return Die Liste von LoesungsVersuchDTOs
     */
    List<LoesungsVersuchDTO> toDtoList(List<LoesungsVersuch> loesungsVersuche);
}