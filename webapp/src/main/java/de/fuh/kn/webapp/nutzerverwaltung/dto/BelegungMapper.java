package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Belegung;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Belegung-Entity und BelegungDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {LocalDate.class})
public interface BelegungMapper extends EntityDtoMapper<Belegung, BelegungDTO> {

    /**
     * Konvertiert eine Belegung-Entity in ein BelegungDTO.
     * Extrahiert zusätzliche Informationen für eine bessere Lesbarkeit im Frontend.
     *
     * @param belegung Die Belegung-Entity, die konvertiert werden soll
     * @return Das resultierende BelegungDTO
     */
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "matrikelnummer", source = "student.matrikelnummer")
    @Mapping(target = "studentName", expression = "java(getStudentDisplayName(belegung.getStudent()))")
    @Mapping(target = "kursId", source = "kurs.id")
    @Mapping(target = "kursName", source = "kurs.name")
    @Mapping(target = "aktiv", expression = "java(isAktiv(belegung))")
    @Mapping(target = "istRegistriert", source = "student.istRegistriert")
    @Override
    BelegungDTO toDto(Belegung belegung);

    /**
     * Konvertiert ein BelegungDTO in eine Belegung-Entity.
     * Student und Kurs müssen separat gesetzt werden, da sie nur als ID im DTO vorhanden sind.
     *
     * @param belegungDTO Das BelegungDTO, das konvertiert werden soll
     * @return Die resultierende Belegung-Entity
     */
    @Override
    Belegung toEntity(BelegungDTO belegungDTO);

    /**
     * Konvertiert eine Liste von Belegung-Entities in eine Liste von BelegungDTOs.
     *
     * @param belegungen Die Liste von Belegung-Entities
     * @return Die Liste von BelegungDTOs
     */
    List<BelegungDTO> toDtoList(List<Belegung> belegungen);
    
    /**
     * Hilfsmethode zur Bestimmung, ob eine Belegung aktuell aktiv ist.
     * Eine Belegung ist aktiv, wenn das aktuelle Datum nach dem Startdatum und vor dem Enddatum liegt
     * oder wenn kein Enddatum gesetzt ist.
     *
     * @param belegung Die zu prüfende Belegung
     * @return true, wenn die Belegung aktiv ist, sonst false
     */
    default boolean isAktiv(Belegung belegung) {
        LocalDate heute = LocalDate.now();
        boolean nachStartdatum = !heute.isBefore(belegung.getStartDatum());
        boolean vorEnddatum = belegung.getEndDatum() == null || !heute.isAfter(belegung.getEndDatum());
        return nachStartdatum && vorEnddatum;
    }
    
    /**
     * Hilfsmethode zur Erstellung eines Anzeigenamens für einen Studenten.
     * Berücksichtigt, dass Vor- und Nachname bei Dummy-Studenten null sein können.
     * 
     * @param student Der Student, für den der Anzeigename erstellt werden soll
     * @return Ein Anzeigename für den Studenten
     */
    default String getStudentDisplayName(de.fuh.kn.webapp.persistence.entity.Student student) {
        if (student.getIstRegistriert() && student.getVorname() != null && student.getNachname() != null) {
            return student.getVorname() + " " + student.getNachname() + " (Matrikelnr. " + student.getMatrikelnummer() + ")";
        } else {
            return "Nicht registriert (Matrikelnr. " + student.getMatrikelnummer() + ")";
        }
    }
}
