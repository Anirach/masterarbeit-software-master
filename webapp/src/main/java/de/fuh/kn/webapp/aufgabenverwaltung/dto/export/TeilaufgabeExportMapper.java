package de.fuh.kn.webapp.aufgabenverwaltung.dto.export;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen TeilaufgabeDto und TeilaufgabeExportDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeilaufgabeExportMapper {

    /**
     * Konvertiert ein TeilaufgabeDto in ein TeilaufgabeExportDTO.
     *
     * @param teilaufgabeDto Das TeilaufgabeDto, das konvertiert werden soll
     * @return Das resultierende TeilaufgabeExportDTO
     */
    TeilaufgabeExportDTO toExportDto(TeilaufgabeDto teilaufgabeDto);

    /**
     * Konvertiert ein TeilaufgabeExportDTO in ein TeilaufgabeDto.
     * Die aufgabeId wird nicht gesetzt und muss separat gesetzt werden.
     *
     * @param exportDto Das TeilaufgabeExportDTO, das konvertiert werden soll
     * @return Das resultierende TeilaufgabeDto
     */
    @Mapping(target = "aufgabeId", ignore = true)
    TeilaufgabeDto fromExportDto(TeilaufgabeExportDTO exportDto);

    /**
     * Konvertiert eine Liste von TeilaufgabeDto in eine Liste von TeilaufgabeExportDTO.
     *
     * @param teilaufgabeDtos Die Liste von TeilaufgabeDto
     * @return Die Liste von TeilaufgabeExportDTO
     */
    List<TeilaufgabeExportDTO> toExportDtoList(List<TeilaufgabeDto> teilaufgabeDtos);

    /**
     * Konvertiert eine Liste von TeilaufgabeExportDTO in eine Liste von TeilaufgabeDto.
     *
     * @param exportDtos Die Liste von TeilaufgabeExportDTO
     * @return Die Liste von TeilaufgabeDto
     */
    List<TeilaufgabeDto> fromExportDtoList(List<TeilaufgabeExportDTO> exportDtos);
}