package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.persistence.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Mapper zur Konvertierung zwischen RegistrierungDTO und Student-Entity.
 * Verwendet MapStruct zur automatischen Generierung der Mapping-Funktionalität.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegistrierungMapper {

    /**
     * Konvertiert ein RegistrierungDTO in eine Student-Entity.
     * Das Passwort muss separat verschlüsselt werden, da dies außerhalb der MapStruct-Funktionalität liegt.
     *
     * @param registrierungDTO Das RegistrierungDTO, das konvertiert werden soll
     * @return Die resultierende Student-Entity (ohne verschlüsseltes Passwort)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwort", ignore = true) // Passwort wird separat verschlüsselt
    @Mapping(target = "belegungen", ignore = true)
    @Mapping(target = "loesungsVersuche", ignore = true)
    @Mapping(target = "chats", ignore = true)
    Student toStudent(RegistrierungDTO registrierungDTO);
    
    /**
     * Erstellt eine Student-Entity aus einem RegistrierungDTO mit verschlüsseltem Passwort.
     *
     * @param registrierungDTO Das RegistrierungDTO mit den Nutzerdaten
     * @param passwordEncoder Der PasswordEncoder zum Verschlüsseln des Passworts
     * @return Die Student-Entity mit verschlüsseltem Passwort
     */
    default Student toStudentWithEncodedPassword(RegistrierungDTO registrierungDTO, PasswordEncoder passwordEncoder) {
        Student student = toStudent(registrierungDTO);
        student.setPasswort(passwordEncoder.encode(registrierungDTO.getPasswort()));
        return student;
    }
}
