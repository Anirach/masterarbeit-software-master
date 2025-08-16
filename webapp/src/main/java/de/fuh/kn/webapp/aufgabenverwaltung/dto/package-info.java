/**
 * Dieses Paket enthält die DTOs (Data Transfer Objects) für die Aufgabenverwaltung.
 * Diese DTOs dienen der Übertragung von Aufgabendaten zwischen den Schichten der Anwendung
 * und ermöglichen die strukturierte Darstellung von Aufgaben und Teilaufgaben in der UI.
 * Es umfasst:
 *
 * - AufgabeDto: Repräsentiert eine vollständige Aufgabe mit Titel, Aufgabentext und
 *   einer Liste von Teilaufgaben. Unterscheidet zwischen einfachen Aufgaben (eine Teilaufgabe)
 *   und komplexen Aufgaben (mehrere Teilaufgaben mit übergreifendem Aufgabentext).
 *
 * - TeilaufgabeDto: Repräsentiert eine einzelne Teilaufgabe mit Aufgabenstellung,
 *   Musterlösung und Bewertungskriterien.
 *
 * - Mapper-Klassen: Übernehmen die bidirektionale Konvertierung zwischen Entity-Objekten
 *   und DTOs gemäß dem Clean-Architecture-Pattern.
 *
 * Diese DTOs werden vor allem in der Kursbetreueransicht verwendet, um Aufgaben zu erstellen,
 * zu bearbeiten und zu verwalten.
 */
package de.fuh.kn.webapp.aufgabenverwaltung.dto;