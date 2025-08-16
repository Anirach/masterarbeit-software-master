/**
 * Dieses Paket enthält alle Persistenz-bezogenen Komponenten der Anwendung.
 * Es umfasst die Entitäten, Repositories und Datenbankzugriffsfunktionalitäten.
 * Gemäß der Clean Architecture sind diese komplett von der Geschäftslogik getrennt
 * und werden primär von Services verwendet, nicht direkt von Controllern.
 * Die Persistenz basiert auf Spring Data JPA und ist nach dem Repository-Pattern
 * strukturiert.
 */
package de.fuh.kn.webapp.persistence;