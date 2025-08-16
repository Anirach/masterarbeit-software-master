package de.fuh.kn.webapp.kursverwaltung.dto.export;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportMapper;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen KursDTO und KursExportDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {KurseinheitExportMapper.class, KursMaterialExportMapper.class, AufgabeExportMapper.class})
public interface KursExportMapper {

    /**
     * Konvertiert ein KursDTO in ein KursExportDTO.
     * Die Aufgaben müssen separat gesetzt werden, da diese nicht im KursDTO enthalten sind.
     *
     * @param kursDTO Das KursDTO, das konvertiert werden soll
     * @return Das resultierende KursExportDTO
     */
    @Mapping(target = "aufgaben", ignore = true)
    KursExportDTO toExportDto(KursDTO kursDTO);

    /**
     * Konvertiert ein KursExportDTO in ein KursDTO.
     * Die Statistikfelder werden ignoriert, da diese neu berechnet werden müssen.
     *
     * @param exportDto Das KursExportDTO, das konvertiert werden soll
     * @return Das resultierende KursDTO
     */
    @Mapping(target = "anzahlBelegungen", ignore = true)
    @Mapping(target = "anzahlAktiveBelegungen", ignore = true)
    KursDTO fromExportDto(KursExportDTO exportDto);

    /**
     * Konvertiert eine Liste von AufgabeDto in eine Liste von AufgabeExportDTO.
     * Diese Methode wird für den Export der Aufgaben eines Kurses verwendet.
     *
     * @param aufgabeDto Die Liste von AufgabeDto
     * @return Die Liste von AufgabeExportDTO
     */
    List<AufgabeExportDTO> mapAufgaben(List<AufgabeDto> aufgabeDto);
}