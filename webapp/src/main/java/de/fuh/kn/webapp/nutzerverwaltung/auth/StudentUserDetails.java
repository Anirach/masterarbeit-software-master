package de.fuh.kn.webapp.nutzerverwaltung.auth;

import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementierung von UserDetails speziell für Studierende.
 * Enthält zusätzliche studentenspezifische Attribute wie die Matrikelnummer.
 */
@Getter
public class StudentUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwort;
    private final String vorname;
    private final String nachname;
    private final String matrikelnummer;

    /**
     * Erzeugt ein StudentUserDetails-Objekt aus einer StudentDTO und dem Passwort.
     *
     * @param studentDTO Das StudentDTO
     * @param passwort Das verschlüsselte Passwort
     */
    public StudentUserDetails(StudentDTO studentDTO, String passwort) {
        this.id = studentDTO.getId();
        this.email = studentDTO.getEmail();
        this.passwort = passwort;
        this.vorname = studentDTO.getVorname();
        this.nachname = studentDTO.getNachname();
        this.matrikelnummer = studentDTO.getMatrikelnummer();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    @Override
    public String getPassword() {
        return this.passwort;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
