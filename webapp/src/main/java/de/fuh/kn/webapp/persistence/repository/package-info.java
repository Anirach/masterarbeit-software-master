/**
 * Dieses Paket enthält alle Repository-Interfaces der Anwendung.
 * Die Repositories sind nach fachlichen Domänen gruppiert und basieren auf
 * Spring Data JPA. Sie bieten typsichere Abfragemethoden für:
 * - CRUD-Operationen
 * - Beziehungsbasierte Abfragen
 * - Filtermöglichkeiten
 * - Paginierung und Sortierung
 *
 * Gemäß dem Clean-Architecture-Pattern dürfen nur Services auf Repositories
 * zugreifen, nie direkt Controller oder andere Komponenten. Die Repositories
 * bilden die unterste Schicht der Anwendung und abstrahieren die Datenbankzugriffe.
 */
package de.fuh.kn.webapp.persistence.repository;