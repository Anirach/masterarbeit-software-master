package de.fuh.kn.webapp.chat.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.ChatNachricht;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen ChatNachricht-Entity und ChatNachrichtDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {ChatNachrichtReferenzMapper.class})
public interface ChatNachrichtMapper extends EntityDtoMapper<ChatNachricht, ChatNachrichtDTO> {

    /**
     * Konvertiert eine ChatNachricht-Entity in ein ChatNachrichtDTO.
     * Die IDs der verknüpften Entitäten werden extrahiert.
     *
     * @param chatNachricht Die ChatNachricht-Entity, die konvertiert werden soll
     * @return Das resultierende ChatNachrichtDTO
     */
    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "referenzen", target = "referenzen")
    @Override
    ChatNachrichtDTO toDto(ChatNachricht chatNachricht);

    /**
     * Konvertiert ein ChatNachrichtDTO in eine ChatNachricht-Entity.
     * Die verknüpften Entitäten werden nicht automatisch gesetzt,
     * sondern müssen separat gesetzt werden.
     *
     * @param chatNachrichtDTO Das ChatNachrichtDTO, das konvertiert werden soll
     * @return Die resultierende ChatNachricht-Entity
     */
    @Mapping(target = "chat", ignore = true)
    @Mapping(target = "referenzen", ignore = true)
    @Override
    ChatNachricht toEntity(ChatNachrichtDTO chatNachrichtDTO);

    /**
     * Konvertiert eine Liste von ChatNachricht-Entities in eine Liste von ChatNachrichtDTOs.
     *
     * @param chatNachrichten Die Liste von ChatNachricht-Entities
     * @return Die Liste von ChatNachrichtDTOs
     */
    List<ChatNachrichtDTO> toDtoList(List<ChatNachricht> chatNachrichten);
}