package de.fuh.kn.webapp.kursverwaltung.dto.export;

import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Base64;
import java.util.List;

/**
 * Mapper zur Konvertierung zwischen KursMaterialDTO und KursMaterialExportDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KursMaterialExportMapper {

    /**
     * Konvertiert ein KursMaterialDTO in ein KursMaterialExportDTO.
     * Die binären Inhalte werden dabei Base64-kodiert.
     *
     * @param kursMaterialDTO Das KursMaterialDTO, das konvertiert werden soll
     * @return Das resultierende KursMaterialExportDTO
     */
    @Mapping(source = "inhalt", target = "inhaltBase64", qualifiedByName = "bytesToBase64")
    KursMaterialExportDTO toExportDto(KursMaterialDTO kursMaterialDTO);

    /**
     * Konvertiert ein KursMaterialExportDTO in ein KursMaterialDTO.
     * Der Base64-kodierte Inhalt wird dabei wieder in binäre Daten umgewandelt.
     *
     * @param exportDto Das KursMaterialExportDTO, das konvertiert werden soll
     * @return Das resultierende KursMaterialDTO
     */
    @Mapping(source = "inhaltBase64", target = "inhalt", qualifiedByName = "base64ToBytes")
    @Mapping(target = "kurseinheitId", ignore = true)
    @Mapping(target = "kursId", ignore = true)
    KursMaterialDTO fromExportDto(KursMaterialExportDTO exportDto);

    /**
     * Konvertiert eine Liste von KursMaterialDTO in eine Liste von KursMaterialExportDTO.
     *
     * @param kursMaterialDTOs Die Liste von KursMaterialDTO
     * @return Die Liste von KursMaterialExportDTO
     */
    List<KursMaterialExportDTO> toExportDtoList(List<KursMaterialDTO> kursMaterialDTOs);

    /**
     * Konvertiert eine Liste von KursMaterialExportDTO in eine Liste von KursMaterialDTO.
     *
     * @param exportDtos Die Liste von KursMaterialExportDTO
     * @return Die Liste von KursMaterialDTO
     */
    List<KursMaterialDTO> fromExportDtoList(List<KursMaterialExportDTO> exportDtos);

    /**
     * Konvertiert ein Byte-Array in einen Base64-kodierten String.
     *
     * @param bytes Das zu kodierende Byte-Array
     * @return Der Base64-kodierte String oder null, wenn das Byte-Array null ist
     */
    @Named("bytesToBase64")
    default String bytesToBase64(byte[] bytes) {
        return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
    }

    /**
     * Konvertiert einen Base64-kodierten String in ein Byte-Array.
     *
     * @param base64 Der zu dekodierende Base64-String
     * @return Das dekodierte Byte-Array oder null, wenn der String null ist
     */
    @Named("base64ToBytes")
    default byte[] base64ToBytes(String base64) {
        return base64 != null ? Base64.getDecoder().decode(base64) : null;
    }
}