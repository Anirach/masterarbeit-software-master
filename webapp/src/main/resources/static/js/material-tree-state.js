/**
 * Hilfsfunktionen zur Erhaltung des Zustands von expandierten Kurseinheiten im Material-Baum
 * beim HTMX-basierten Aktualisieren des DOM.
 */

document.addEventListener('DOMContentLoaded', function() {
    // Event-Listener für HTMX-Ereignisse registrieren
    document.body.addEventListener('htmx:beforeSwap', saveExpandedState);
    document.body.addEventListener('htmx:afterSettle', restoreExpandedState);
});

// Speichert den Status aller ausgeklappten Kurseinheiten vor einer HTMX-Aktualisierung
function saveExpandedState(event) {
    // Prüfen, ob es sich um eine Aktualisierung des Materialbaums handelt
    if (event.detail.target && 
        (event.detail.target.classList.contains('course-material-tree') || 
         event.detail.target.classList.contains('kurseinheit-material-list'))) {
        
        // Array für die IDs aller geöffneten Collapse-Elemente erstellen
        const expandedIds = [];
        
        // Alle Collapse-Elemente im Materialbaum finden, die geöffnet sind
        const expandedElements = document.querySelectorAll('.collapse.show');
        expandedElements.forEach(element => {
            expandedIds.push(element.id);
        });
        
        // Status im sessionStorage speichern
        window.sessionStorage.setItem('expandedMaterialNodes', JSON.stringify(expandedIds));
    }
}

// Stellt den Status aller ausgeklappten Kurseinheiten nach einer HTMX-Aktualisierung wieder her
function restoreExpandedState(event) {
    // Prüfen, ob es sich um eine Aktualisierung des Materialbaums handelt
    if (event.detail.target && 
        (event.detail.target.classList.contains('course-material-tree') || 
         event.detail.target.classList.contains('kurseinheit-material-list'))) {
        
        // Gespeicherte IDs aus dem sessionStorage abrufen
        const expandedIdsJSON = window.sessionStorage.getItem('expandedMaterialNodes');
        if (expandedIdsJSON) {
            const expandedIds = JSON.parse(expandedIdsJSON);
            
            // Für jede ID das zugehörige Element suchen und ausklappen
            expandedIds.forEach(id => {
                const element = document.getElementById(id);
                if (element) {
                    // Element manuell ausklappen - durch direkte Klassenzuweisung
                    element.classList.add('show');
                    
                    // Icon entsprechend anpassen (falls vorhanden)
                    const controlElement = document.querySelector(`[data-bs-target="#${id}"]`);
                    if (controlElement) {
                        // Aria-expanded auf true setzen
                        controlElement.setAttribute('aria-expanded', 'true');
                        
                        const icon = controlElement.querySelector('.collapse-icon');
                        if (icon) {
                            // Pfeil-Icon auf "ausgeklappt" setzen
                            icon.classList.remove('bi-chevron-right');
                            icon.classList.add('bi-chevron-down');
                        }
                    }
                }
            });
        }
    }
}
