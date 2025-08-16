/**
 * Dieses Paket enthält alle Komponenten zur Aufgabenverwaltung.
 * Es umfasst umfangreiche Funktionalitäten für Kursbetreuende zur Erstellung und
 * Verwaltung von Aufgaben und Musterlösungen. Die Komponenten ermöglichen:
 *
 * - Erstellung und Bearbeitung von Aufgaben mit unterschiedlichen Schwierigkeitsgraden
 * - Strukturierung komplexer Aufgaben in Teilaufgaben mit individuellen Bewertungen
 * - Verwaltung von Musterlösungen und Bewertungskriterien
 * - Markdown-basierte Aufgabenerstellung mit erweiterten Formatierungsmöglichkeiten
 * - Import und Export von Aufgaben für die Wiederverwendung
 * - Integration mit der LLM-basierten automatischen Bewertung
 *
 * Die Implementierung nutzt einen erweiterten Markdown-Parser mit speziellen Erweiterungen
 * für interaktive Felder und Bilder, die in Aufgabenstellungen eingebettet werden können.
 * Es folgt einer klaren Trennung von Controller, Service und Repository gemäß dem
 * Clean-Architecture-Pattern.
 */
package de.fuh.kn.webapp.aufgabenverwaltung;