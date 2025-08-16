/**
 * Dieses Paket enthält die Sicherheitskomponenten der Anwendung.
 * Die Implementierung basiert auf Spring Security und enthält:
 * - Rollenbasierte Zugriffssteuerung mit den Rollen STUDENT und KURSBETREUER
 * - Getrennte Sicherheitskonfigurationen für verschiedene URL-Pfade
 * - Login- und Logout-Handling
 * - Passwort-Verschlüsselung mit BCrypt
 * - Absicherung der Ressourcen nach dem Prinzip "Deny by Default"
 */
package de.fuh.kn.webapp.security;