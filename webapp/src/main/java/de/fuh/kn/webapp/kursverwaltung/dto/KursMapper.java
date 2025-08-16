package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Kurs-Entity und KursDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {KurseinheitMapper.class, KursMaterialMapper.class})
public interface KursMapper extends EntityDtoMapper<Kurs, KursDTO> {

    /**
     * Konvertiert eine Kurs-Entity in ein KursDTO.
     *
     * @param kurs Die Kurs-Entity, die konvertiert werden soll
     * @return Das resultierende KursDTO
     */
    @Override
    KursDTO toDto(Kurs kurs);

    /**
     * Konvertiert ein KursDTO in eine Kurs-Entity.
     *
     * @param kursDTO Das KursDTO, das konvertiert werden soll
     * @return Die resultierende Kurs-Entity
     */
    @Override
    Kurs toEntity(KursDTO kursDTO);

    /**
     * Konvertiert eine Liste von Kurs-Entities in eine Liste von KursDTOs.
     *
     * @param kurse Die Liste von Kurs-Entities
     * @return Die Liste von KursDTOs
     */
    List<KursDTO> toDtoList(List<Kurs> kurse);
}
