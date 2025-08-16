package de.fuh.kn.webapp.kursverwaltung.dto.export;

import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen KurseinheitDTO und KurseinheitExportDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {KursMaterialExportMapper.class})
public interface KurseinheitExportMapper {

    /**
     * Konvertiert ein KurseinheitDTO in ein KurseinheitExportDTO.
     *
     * @param kurseinheitDTO Das KurseinheitDTO, das konvertiert werden soll
     * @return Das resultierende KurseinheitExportDTO
     */
    KurseinheitExportDTO toExportDto(KurseinheitDTO kurseinheitDTO);

    /**
     * Konvertiert ein KurseinheitExportDTO in ein KurseinheitDTO.
     * Die kursId wird nicht gesetzt und muss separat gesetzt werden.
     *
     * @param exportDto Das KurseinheitExportDTO, das konvertiert werden soll
     * @return Das resultierende KurseinheitDTO
     */
    @Mapping(target = "kursId", ignore = true)
    KurseinheitDTO fromExportDto(KurseinheitExportDTO exportDto);

    /**
     * Konvertiert eine Liste von KurseinheitDTO in eine Liste von KurseinheitExportDTO.
     *
     * @param kurseinheitDTOs Die Liste von KurseinheitDTO
     * @return Die Liste von KurseinheitExportDTO
     */
    List<KurseinheitExportDTO> toExportDtoList(List<KurseinheitDTO> kurseinheitDTOs);

    /**
     * Konvertiert eine Liste von KurseinheitExportDTO in eine Liste von KurseinheitDTO.
     *
     * @param exportDtos Die Liste von KurseinheitExportDTO
     * @return Die Liste von KurseinheitDTO
     */
    List<KurseinheitDTO> fromExportDtoList(List<KurseinheitExportDTO> exportDtos);
}