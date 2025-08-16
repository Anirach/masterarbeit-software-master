/**
 * Dieses Paket enthält alle JPA-Entitäten der Anwendung.
 * Die Entitäten repräsentieren die Datenmodelle der verschiedenen Domänen:
 * - Nutzer (Studenten, Kursbetreuende)
 * - Kurse, Kurseinheiten und Kursmaterial
 * - Aufgaben und Teilaufgaben
 * - Lösungsversuche und Bewertungen
 * - Aktivitätsprotokollierung
 * - Chat-Nachrichten
 *
 * Alle Entitäten erben von BaseEntity für gemeinsame Felder wie ID und Zeitstempel.
 * Gemäß der Architektur sind die Entitäten nur für Repositories, Services und Mapper
 * zugänglich und werden nie direkt an die Präsentationsschicht weitergegeben.
 */
package de.fuh.kn.webapp.persistence.entity;