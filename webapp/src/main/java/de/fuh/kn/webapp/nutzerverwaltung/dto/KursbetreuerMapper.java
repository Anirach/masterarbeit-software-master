package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Kursbetreuer;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Kursbetreuer-Entity und KursbetreuerDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KursbetreuerMapper extends EntityDtoMapper<Kursbetreuer, KursbetreuerDTO> {

    /**
     * Konvertiert eine Kursbetreuer-Entity in ein KursbetreuerDTO.
     *
     * @param kursbetreuer Die Kursbetreuer-Entity, die konvertiert werden soll
     * @return Das resultierende KursbetreuerDTO
     */
    @Override
    KursbetreuerDTO toDto(Kursbetreuer kursbetreuer);

    /**
     * Konvertiert ein KursbetreuerDTO in eine Kursbetreuer-Entity.
     *
     * @param kursbetreuerDTO Das KursbetreuerDTO, das konvertiert werden soll
     * @return Die resultierende Kursbetreuer-Entity
     */
    @Override
    Kursbetreuer toEntity(KursbetreuerDTO kursbetreuerDTO);

    /**
     * Konvertiert eine Liste von Kursbetreuer-Entities in eine Liste von KursbetreuerDTOs.
     *
     * @param kursbetreuer Die Liste von Kursbetreuer-Entities
     * @return Die Liste von KursbetreuerDTOs
     */
    List<KursbetreuerDTO> toDtoList(List<Kursbetreuer> kursbetreuer);
}
