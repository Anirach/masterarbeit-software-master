package de.fuh.kn.webapp.common.aktivitaeten;

import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation zur aspektorientierten Protokollierung von Aktivitäten.
 * Methoden, die mit dieser Annotation versehen sind, werden automatisch protokolliert.
 * Die Protokollierung erfolgt über einen AspectJ-Aspekt.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtokolliereAktivitaet {

    /**
     * Der Typ der zu protokollierenden Aktivität.
     * 
     * @return Der Aktivitätstyp aus dem AktivitaetsTyp-Enum
     */
    AktivitaetsTyp aktivitaetsTyp();
    
    /**
     * Template oder feste Beschreibung der Aktivität.
     * Kann Platzhalter für Methodenparameter enthalten (z.B. "Kurs {0} bearbeitet").
     * 
     * @return Die Beschreibung der Aktivität
     */
    String beschreibung() default "";
    
    /**
     * Flag, ob Methodenparameter in die Details aufgenommen werden sollen.
     * 
     * @return true, wenn Methodenparameter in Details aufgenommen werden sollen, sonst false
     */
    boolean mitParametern() default false;
}
