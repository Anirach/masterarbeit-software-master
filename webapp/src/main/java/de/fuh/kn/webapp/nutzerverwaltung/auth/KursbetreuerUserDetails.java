package de.fuh.kn.webapp.nutzerverwaltung.auth;

import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementierung von UserDetails speziell für Kursbetreuer.
 * Enthält die grundlegenden Attribute eines Kursbetreuers.
 */
@Getter
public class KursbetreuerUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwort;
    private final String vorname;
    private final String nachname;

    /**
     * Erzeugt ein KursbetreuerUserDetails-Objekt aus einer KursbetreuerDTO und dem Passwort.
     *
     * @param kursbetreuerDTO Das KursbetreuerDTO
     * @param passwort Das verschlüsselte Passwort
     */
    public KursbetreuerUserDetails(KursbetreuerDTO kursbetreuerDTO, String passwort) {
        this.id = kursbetreuerDTO.getId();
        this.email = kursbetreuerDTO.getEmail();
        this.passwort = passwort;
        this.vorname = kursbetreuerDTO.getVorname();
        this.nachname = kursbetreuerDTO.getNachname();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_KURSBETREUER"));
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
