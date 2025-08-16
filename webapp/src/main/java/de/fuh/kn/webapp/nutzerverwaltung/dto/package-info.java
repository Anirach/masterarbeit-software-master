/**
 * Dieses Paket enthält die DTOs (Data Transfer Objects) für die Nutzerverwaltung.
 * Diese DTOs dienen der Übertragung von Nutzerdaten zwischen den Schichten der Anwendung
 * und ermöglichen die strukturierte Darstellung von Nutzern in der Benutzeroberfläche.
 * Es umfasst:
 *
 * - BaseDTO: Abstrakte Basisklasse für alle DTOs mit gemeinsamen Feldern wie ID und
 *   Zeitstempel sowie Methoden zur Entitätsdarstellung.
 *
 * - NutzerDTO: Repräsentiert grundlegende Nutzerinformationen wie Name, E-Mail und Rolle.
 *
 * - StudentDTO und KursbetreuerDTO: Erweitern NutzerDTO mit rollenbezogenen Informationen.
 *
 * - ProfilDTO und PasswortAenderungDTO: Für die Bearbeitung von Nutzerprofilen und
 *   Passwörtern.
 *
 * - RegistrierungDTO: Für die Nutzerregistrierung mit Validierungsfunktionalität.
 *
 * - BelegungDTO: Für die Zuordnung von Studenten zu Kursen mit Status-Informationen.
 *
 * - Mapper-Klassen: Übernehmen die bidirektionale Konvertierung zwischen Entity-Objekten
 *   und DTOs gemäß dem Clean-Architecture-Pattern.
 */
package de.fuh.kn.webapp.nutzerverwaltung.dto;