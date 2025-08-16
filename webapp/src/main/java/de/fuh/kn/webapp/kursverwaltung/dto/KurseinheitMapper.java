package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Kurseinheit-Entity und KurseinheitDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {KursMaterialMapper.class})
public interface KurseinheitMapper extends EntityDtoMapper<Kurseinheit, KurseinheitDTO> {

    /**
     * Konvertiert eine Kurseinheit-Entity in ein KurseinheitDTO.
     * Die kursId wird aus der Kurs-Entity extrahiert.
     *
     * @param kurseinheit Die Kurseinheit-Entity, die konvertiert werden soll
     * @return Das resultierende KurseinheitDTO
     */
    @Mapping(source = "kurs.id", target = "kursId")
    @Override
    KurseinheitDTO toDto(Kurseinheit kurseinheit);

    /**
     * Konvertiert ein KurseinheitDTO in eine Kurseinheit-Entity.
     * Die Kurs-Entity wird nicht automatisch gesetzt, sondern muss separat gesetzt werden.
     *
     * @param kurseinheitDTO Das KurseinheitDTO, das konvertiert werden soll
     * @return Die resultierende Kurseinheit-Entity
     */
    @Mapping(target = "kurs", ignore = true)
    @Mapping(target = "aufgaben", ignore = true)
    @Override
    Kurseinheit toEntity(KurseinheitDTO kurseinheitDTO);

    /**
     * Konvertiert eine Liste von Kurseinheit-Entities in eine Liste von KurseinheitDTOs.
     *
     * @param kurseinheiten Die Liste von Kurseinheit-Entities
     * @return Die Liste von KurseinheitDTOs
     */
    List<KurseinheitDTO> toDtoList(List<Kurseinheit> kurseinheiten);
    
    /**
     * Fügt nach dem Mapping die Anzahl der Aufgaben hinzu.
     *
     * @param kurseinheit Die Kurseinheit-Entity
     * @param dto Das gemappte KurseinheitDTO
     */
    @AfterMapping
    default void addAufgabenCount(Kurseinheit kurseinheit, @MappingTarget KurseinheitDTO dto) {
        if (kurseinheit != null && kurseinheit.getAufgaben() != null) {
            dto.setAufgabenCount(kurseinheit.getAufgaben().size());
        } else {
            dto.setAufgabenCount(0);
        }
    }
}
