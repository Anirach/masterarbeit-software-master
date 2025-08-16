package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstrakte Basisklasse für alle Nutzer des Systems.
 * Wird spezialisiert zu Kursbetreuer und Student.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "nutzer_typ")
@Getter
@Setter
public abstract class Nutzer extends BaseEntity {

    /**
     * Die E-Mail-Adresse des Nutzers.
     * Wird für die Anmeldung verwendet.
     * Kann für Dummy-Studenten zunächst leer sein.
     */
    @Column(unique = true)
    private String email;

    /**
     * Das verschlüsselte Passwort des Nutzers.
     * Muss auch für Dummy-Studenten gesetzt sein, kann aber ein Platzhalter sein.
     */
    @Column(nullable = false)
    private String passwort;

    /**
     * Der Vorname des Nutzers.
     * Kann für Dummy-Studenten zunächst leer sein.
     */
    @Column
    private String vorname;

    /**
     * Der Nachname des Nutzers.
     * Kann für Dummy-Studenten zunächst leer sein.
     */
    @Column
    private String nachname;
    
    /**
     * Flag, das angibt, ob der Nutzer vollständig registriert ist.
     * Für Dummy-Studenten ist dieser Wert false.
     */
    @Column(nullable = false)
    private Boolean istRegistriert = true;

    /**
     * Die Akvititäten des Nutzers
     */
    @OneToMany(mappedBy = "nutzer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Aktivitaet> aktivitaeten = new ArrayList<>();

    /**
     * Anzeigenamen des Nutzers (Vorname+Nachname), sowie Sonderfall für nicht-registrierte Nutzer
     * @return Anzeigename des Studenten
     */
    public String getDisplayName(){
        if (this.getIstRegistriert() && this.getVorname() != null && this.getNachname() != null) {
            return this.getVorname() + " " + this.getNachname();
        } else {
            // Bei Studenten würde hier die Matrikelnummer angezeigt werden
            // Bei allgemeinen Nutzern zeigen wir nur an, dass sie nicht registriert sind
            return "Nicht registriert";
        }
    }
}
