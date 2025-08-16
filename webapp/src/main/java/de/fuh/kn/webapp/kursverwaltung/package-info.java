/**
 * Dieses Paket enthält alle Komponenten zur Kursverwaltung.
 * Es umfasst umfangreiche Funktionalitäten für Kursbetreuende zur Erstellung und
 * Verwaltung von Kursen, Kurseinheiten und Kursmaterialien. Die Komponenten ermöglichen:
 *
 * - Erstellung und Bearbeitung von Kursen mit Metadaten wie Titel, Beschreibung und Semester
 * - Strukturierung von Kursen in Kurseinheiten mit eigenen Lernzielen
 * - Verwaltung von Kursmaterialien in hierarchischer Struktur
 * - Upload und Organisation von Lernmaterialien unterschiedlicher Medientypen
 * - Dynamische Präsentation der Kursinhalte für Studierende
 *
 * Die Implementierung folgt einer klaren Trennung von Controller, Service und Repository
 * gemäß dem Clean-Architecture-Pattern und nutzt DTOs für die Datenkommunikation zwischen
 * den Schichten. Controller sind dabei in Standard- und HTMX-Controller unterteilt, um
 * sowohl vollständige Seitenladevorgänge als auch dynamische AJAX-Aktualisierungen zu unterstützen.
 */
package de.fuh.kn.webapp.kursverwaltung;