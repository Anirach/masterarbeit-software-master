package de.fuh.kn.webapp.aufgabenverwaltung.dto.export;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen AufgabeDto und AufgabeExportDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {TeilaufgabeExportMapper.class})
public interface AufgabeExportMapper {

    /**
     * Konvertiert ein AufgabeDto in ein AufgabeExportDTO.
     *
     * @param aufgabeDto Das AufgabeDto, das konvertiert werden soll
     * @return Das resultierende AufgabeExportDTO
     */
    AufgabeExportDTO toExportDto(AufgabeDto aufgabeDto);

    /**
     * Konvertiert ein AufgabeExportDTO in ein AufgabeDto.
     *
     * @param exportDto Das AufgabeExportDTO, das konvertiert werden soll
     * @return Das resultierende AufgabeDto
     */
    AufgabeDto fromExportDto(AufgabeExportDTO exportDto);

    /**
     * Konvertiert eine Liste von AufgabeDto in eine Liste von AufgabeExportDTO.
     *
     * @param aufgabeDtos Die Liste von AufgabeDto
     * @return Die Liste von AufgabeExportDTO
     */
    List<AufgabeExportDTO> toExportDtoList(List<AufgabeDto> aufgabeDtos);

    /**
     * Konvertiert eine Liste von AufgabeExportDTO in eine Liste von AufgabeDto.
     *
     * @param exportDtos Die Liste von AufgabeExportDTO
     * @return Die Liste von AufgabeDto
     */
    List<AufgabeDto> fromExportDtoList(List<AufgabeExportDTO> exportDtos);
}