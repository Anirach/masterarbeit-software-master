package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen KursMaterial-Entity und KursMaterialDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KursMaterialMapper extends EntityDtoMapper<KursMaterial, KursMaterialDTO> {

    /**
     * Konvertiert eine KursMaterial-Entity in ein KursMaterialDTO.
     * Die kursId und kurseinheitId werden aus den entsprechenden Entities extrahiert.
     *
     * @param kursMaterial Die KursMaterial-Entity, die konvertiert werden soll
     * @return Das resultierende KursMaterialDTO
     */
    @Mapping(source = "kurs.id", target = "kursId")
    @Mapping(source = "kurseinheit.id", target = "kurseinheitId")
    @Mapping(source = "typ", target = "typ")
    @Override
    KursMaterialDTO toDto(KursMaterial kursMaterial);

    /**
     * Konvertiert ein KursMaterialDTO in eine KursMaterial-Entity.
     * Die Kurs- und Kurseinheit-Entities werden nicht automatisch gesetzt, 
     * sondern müssen separat gesetzt werden.
     *
     * @param kursMaterialDTO Das KursMaterialDTO, das konvertiert werden soll
     * @return Die resultierende KursMaterial-Entity
     */
    @Mapping(target = "kurs", ignore = true)
    @Mapping(target = "kurseinheit", ignore = true)
    @Mapping(source = "typ", target = "typ")
    @Override
    KursMaterial toEntity(KursMaterialDTO kursMaterialDTO);

    /**
     * Konvertiert eine Liste von KursMaterial-Entities in eine Liste von KursMaterialDTOs.
     *
     * @param kursMaterialien Die Liste von KursMaterial-Entities
     * @return Die Liste von KursMaterialDTOs
     */
    List<KursMaterialDTO> toDtoList(List<KursMaterial> kursMaterialien);
    
    /**
     * Konvertiert einen KursMaterialType-Enum von der Entity zum DTO.
     *
     * @param typ Der Entity-Enum-Typ
     * @return Der entsprechende DTO-Enum-Typ
     */
    default KursMaterialDTO.KursMaterialTyp mapTyp(KursMaterial.KursMaterialTyp typ) {
        if (typ == null) {
            return null;
        }
        
        switch (typ) {
            case DOKUMENT:
                return KursMaterialDTO.KursMaterialTyp.DOKUMENT;
            case BILD:
                return KursMaterialDTO.KursMaterialTyp.BILD;
            default:
                throw new IllegalArgumentException("Unbekannter KursMaterialTyp: " + typ);
        }
    }
    
    /**
     * Konvertiert einen KursMaterialType-Enum vom DTO zur Entity.
     *
     * @param typ Der DTO-Enum-Typ
     * @return Der entsprechende Entity-Enum-Typ
     */
    default KursMaterial.KursMaterialTyp mapTypReverse(KursMaterialDTO.KursMaterialTyp typ) {
        if (typ == null) {
            return null;
        }
        
        switch (typ) {
            case DOKUMENT:
                return KursMaterial.KursMaterialTyp.DOKUMENT;
            case BILD:
                return KursMaterial.KursMaterialTyp.BILD;
            default:
                throw new IllegalArgumentException("Unbekannter KursMaterialTyp: " + typ);
        }
    }
}
