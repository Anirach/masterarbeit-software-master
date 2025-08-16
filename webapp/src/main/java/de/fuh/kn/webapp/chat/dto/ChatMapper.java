package de.fuh.kn.webapp.chat.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Chat-Entity und ChatDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {ChatNachrichtMapper.class})
public interface ChatMapper extends EntityDtoMapper<Chat, ChatDTO> {

    /**
     * Konvertiert eine Chat-Entity in ein ChatDTO.
     * Die IDs der verknüpften Entitäten werden extrahiert.
     * Die Nachrichten werden durch den ChatNachrichtMapper konvertiert.
     *
     * @param chat Die Chat-Entity, die konvertiert werden soll
     * @return Das resultierende ChatDTO
     */
    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "teilaufgabe.id", target = "teilaufgabeId")
    @Mapping(source = "teilaufgabe.aufgabe.titel", target = "aufgabenTitel")
    @Mapping(source = "teilaufgabe.reihenfolge", target = "teilaufgabeReihenfolge")
    @Override
    ChatDTO toDto(Chat chat);

    /**
     * Konvertiert ein ChatDTO in eine Chat-Entity.
     * Die verknüpften Entitäten werden nicht automatisch gesetzt,
     * sondern müssen separat gesetzt werden.
     *
     * @param chatDTO Das ChatDTO, das konvertiert werden soll
     * @return Die resultierende Chat-Entity
     */
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "teilaufgabe", ignore = true)
    @Mapping(target = "nachrichten", ignore = true)
    @Override
    Chat toEntity(ChatDTO chatDTO);

    /**
     * Konvertiert eine Liste von Chat-Entities in eine Liste von ChatDTOs.
     *
     * @param chats Die Liste von Chat-Entities
     * @return Die Liste von ChatDTOs
     */
    List<ChatDTO> toDtoList(List<Chat> chats);
}