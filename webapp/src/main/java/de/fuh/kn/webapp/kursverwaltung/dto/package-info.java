/**
 * Dieses Paket enthält die DTOs (Data Transfer Objects) für die Kursverwaltung.
 * Diese DTOs dienen der strukturierten Darstellung und Übertragung von Kursdaten
 * zwischen den Schichten der Anwendung. Es umfasst:
 *
 * - KursDTO: Repräsentiert einen vollständigen Kurs mit Titel, Beschreibung,
 *   Semester-Informationen und Kursbetreuenden.
 *
 * - KurseinheitDTO: Repräsentiert eine einzelne Kurseinheit innerhalb eines Kurses
 *   mit Titel, Beschreibung, Materialien und Aufgaben.
 *
 * - KursMaterialDTO: Repräsentiert Lernmaterialien für Kurseinheiten wie Texte,
 *   Präsentationen oder Bilder inklusive hierarchischer Strukturierung.
 *
 * - Mapper-Klassen: Übernehmen die bidirektionale Konvertierung zwischen Entity-Objekten
 *   und DTOs gemäß dem Clean-Architecture-Pattern.
 *
 * Diese DTOs werden sowohl in der Kursbetreueransicht (für die Verwaltung) als auch
 * in der Studentenansicht (für die Darstellung) verwendet.
 */
package de.fuh.kn.webapp.kursverwaltung.dto;