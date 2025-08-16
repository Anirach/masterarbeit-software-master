package de.fuh.kn.webapp.chat.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.ChatNachrichtReferenz;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen ChatNachrichtReferenz-Entity und ChatNachrichtReferenzDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatNachrichtReferenzMapper extends EntityDtoMapper<ChatNachrichtReferenz, ChatNachrichtReferenzDTO> {

    /**
     * Konvertiert eine ChatNachrichtReferenz-Entity in ein ChatNachrichtReferenzDTO.
     * Die IDs der verknüpften Entitäten werden extrahiert.
     *
     * @param referenz Die ChatNachrichtReferenz-Entity, die konvertiert werden soll
     * @return Das resultierende ChatNachrichtReferenzDTO
     */
    @Mapping(source = "chatNachricht.id", target = "chatNachrichtId")
    @Mapping(source = "kursMaterial.id", target = "kursMaterialId")
    @Mapping(source = "kursMaterial.name", target = "kursMaterialName")
    @Override
    ChatNachrichtReferenzDTO toDto(ChatNachrichtReferenz referenz);

    /**
     * Konvertiert ein ChatNachrichtReferenzDTO in eine ChatNachrichtReferenz-Entity.
     * Die verknüpften Entitäten werden nicht automatisch gesetzt,
     * sondern müssen separat gesetzt werden.
     *
     * @param referenzDTO Das ChatNachrichtReferenzDTO, das konvertiert werden soll
     * @return Die resultierende ChatNachrichtReferenz-Entity
     */
    @Mapping(target = "chatNachricht", ignore = true)
    @Mapping(target = "kursMaterial", ignore = true)
    @Override
    ChatNachrichtReferenz toEntity(ChatNachrichtReferenzDTO referenzDTO);

    /**
     * Konvertiert eine Liste von ChatNachrichtReferenz-Entities in eine Liste von ChatNachrichtReferenzDTOs.
     *
     * @param referenzen Die Liste von ChatNachrichtReferenz-Entities
     * @return Die Liste von ChatNachrichtReferenzDTOs
     */
    List<ChatNachrichtReferenzDTO> toDtoList(List<ChatNachrichtReferenz> referenzen);
}