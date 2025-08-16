package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.common.mapper.EntityDtoMapper;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper zur Konvertierung zwischen Student-Entity und StudentDTO.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {BelegungMapper.class})
public interface StudentMapper extends EntityDtoMapper<Student, StudentDTO> {

    /**
     * Konvertiert eine Student-Entity in ein StudentDTO.
     * Extrahiert die IDs der Belegungen für das DTO.
     *
     * @param student Die Student-Entity, die konvertiert werden soll
     * @return Das resultierende StudentDTO
     */
    @Mapping(target = "belegungIds", expression = "java(student.getBelegungen().stream().map(belegung -> belegung.getId()).toList())")
    @Mapping(target = "displayName", expression = "java(getDisplayName(student))")
    @Override
    StudentDTO toDto(Student student);

    /**
     * Konvertiert ein StudentDTO in eine Student-Entity.
     * Belegungen werden nicht zurück gemappt, da sie separat verwaltet werden.
     *
     * @param studentDTO Das StudentDTO, das konvertiert werden soll
     * @return Die resultierende Student-Entity
     */
    @Override
    Student toEntity(StudentDTO studentDTO);

    /**
     * Konvertiert eine Liste von Student-Entities in eine Liste von StudentDTOs.
     *
     * @param studenten Die Liste von Student-Entities
     * @return Die Liste von StudentDTOs
     */
    List<StudentDTO> toDtoList(List<Student> studenten);
    
    /**
     * Hilfsmethode zur Erstellung eines Anzeigenamens für einen Studenten.
     * Überschreibt die Basisimplementierung im NutzerMapper, um auch die Matrikelnummer einzubeziehen.
     * 
     * @param student Der Student, für den der Anzeigename erstellt werden soll
     * @return Ein Anzeigename für den Studenten
     */
    default String getDisplayName(Student student) {
        if (student.getIstRegistriert() && student.getVorname() != null && student.getNachname() != null) {
            return student.getVorname() + " " + student.getNachname() + " (Matrikelnr. " + student.getMatrikelnummer() + ")";
        } else {
            return "Nicht registriert (Matrikelnr. " + student.getMatrikelnummer() + ")";
        }
    }
}
