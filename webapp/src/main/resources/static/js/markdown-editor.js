/**
 * EasyMDE Markdown Editor Initialization
 * 
 * This script initializes EasyMDE for markdown editing textareas
 * and handles binding to the original textareas.
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize all EasyMDE instances on page load
    initializeEasyMDE();
    
    // Initialize auto-resize for textareas on page load
    initializeAutoResizeTextareas();
    
    // Listen for HTMX content swaps
    document.body.addEventListener('htmx:afterSettle', function(event) {
        // Initialize EasyMDE on any new textareas within the swapped content
        initializeEasyMDE(event.target);
        // Initialize auto-resize for any new textareas
        initializeAutoResizeTextareas();
    });
    
    // Add event listener for Bootstrap tab show event
    document.body.addEventListener('shown.bs.tab', function(event) {
        // Get the newly activated tab content
        const targetId = event.target.getAttribute('data-bs-target');
        if (targetId) {
            const tabContent = document.querySelector(targetId);
            if (tabContent) {
                // Find any EasyMDE instances in this tab and refresh them
                refreshEditorsInContainer(tabContent);
                // Initialize auto-resize for textareas in this tab
                initializeAutoResizeTextareas();
                // Adjust height for any existing textareas with content that may have been hidden
                adjustTextareasInContainer(tabContent);
            }
        }
    });
    
    // Ensure we have a modal container for image selection
    createImageSelectorModal();
    
    // Add delegated event handler for image selection
    document.body.addEventListener('click', function(event) {
        // Only handle clicks directly on image cards or their child elements
        const imageCard = event.target.closest('.image-card');
        if (imageCard && event.currentTarget.contains(imageCard)) {
            event.preventDefault();
            selectImageForMarkdown(imageCard);
        }
    });
    
    // Add form validation for duplicate field names
    document.body.addEventListener('submit', function(event) {
        const form = event.target;
        
        // Only apply this to forms that might contain markdown editors with fields
        if (form.querySelector('textarea.markdown-editor')) {
            // Find any duplicate field warnings
            const duplicateWarnings = form.querySelectorAll('.duplicate-fields-warning');
            
            // If we have warnings, prevent the form submission
            if (duplicateWarnings.length > 0) {
                event.preventDefault();
                
                // Scroll to the first warning
                if (duplicateWarnings[0]) {
                    duplicateWarnings[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
                    
                    // Flash the warning to get attention
                    duplicateWarnings.forEach(warning => {
                        warning.classList.add('animate__animated', 'animate__flash');
                        setTimeout(() => {
                            warning.classList.remove('animate__animated', 'animate__flash');
                        }, 1000);
                    });
                }
                
                // Show a Bootstrap alert at the top of the form
                const existingAlert = form.querySelector('.duplicate-fields-alert');
                if (!existingAlert) {
                    const alertDiv = document.createElement('div');
                    alertDiv.className = 'alert alert-danger duplicate-fields-alert';
                    alertDiv.innerHTML = '<strong>Fehler: Doppelte Feldnamen gefunden!</strong><br>' + 
                        'Bitte korrigieren Sie die doppelten Feldnamen, bevor Sie das Formular absenden.';
                    
                    // Add dismissible button
                    const closeButton = document.createElement('button');
                    closeButton.type = 'button';
                    closeButton.className = 'btn-close';
                    closeButton.setAttribute('data-bs-dismiss', 'alert');
                    closeButton.setAttribute('aria-label', 'Schließen');
                    alertDiv.appendChild(closeButton);
                    
                    // Insert at the top of the form
                    form.insertBefore(alertDiv, form.firstChild);
                }
            }
        }
    });
});

/**
 * Creates a modal container for the image selector if it doesn't exist yet
 */
function createImageSelectorModal() {
    if (!document.getElementById('imageSelectorModal')) {
        const modalHtml = `
        <div class="modal fade" id="imageSelectorModal" tabindex="-1" aria-labelledby="imageSelectorModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div id="imageSelectorModalContent">
                    <!-- Content will be loaded here via HTMX -->
                </div>
            </div>
        </div>
        `;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
    }
}

/**
 * Handles selection of an image from the modal and insertion into the editor
 * @param {HTMLElement} imageCard - The clicked image card element
 */
function selectImageForMarkdown(imageCard) {
    const imageName = imageCard.getAttribute('data-image-name');
    const editorId = imageCard.getAttribute('data-editor-id');
    const altTextInput = document.getElementById('imageAltText');
    
    if (imageName && editorId) {
        const altText = altTextInput && altTextInput.value ? altTextInput.value.trim() : imageName;
        
        // Find the editor instance
        const textarea = document.getElementById(editorId);
        if (textarea && textarea._easymde) {
            const editor = textarea._easymde;
            
            // Create the markdown image syntax - URL encode the filename to handle spaces and special characters
            let encodedImageName = encodeURIComponent(imageName);
            const imageMarkdown = `![${altText}](${encodedImageName})`;
            
            // Insert the image markdown at the cursor position
            editor.codemirror.replaceSelection(imageMarkdown);
            
            // Close the modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('imageSelectorModal'));
            if (modal) {
                modal.hide();
            }
        }
    }
}

/**
 * Gets the CSRF token and header name from the meta tags
 * @returns {Object} - Object containing the CSRF token and header name
 */
function getCsrfTokenInfo() {
    const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const headerName = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
    return { token, headerName };
}

/**
 * Function to initialize EasyMDE on textareas with class "markdown-editor"
 * @param {HTMLElement} container - The container element to search for textareas (defaults to entire document)
 */
function initializeEasyMDE(container = document) {
    // Find all textareas with class "markdown-editor" that don't have EasyMDE initialized yet
    const textareas = container.querySelectorAll('textarea.markdown-editor:not(.easymde-initialized)');
    
    textareas.forEach(textarea => {
        // Mark this textarea as initialized to prevent double initialization
        textarea.classList.add('easymde-initialized');
        
        // Store the original textarea ID for reference
        const textareaId = textarea.id || `easymde-${Math.random().toString(36).substring(2, 9)}`;
        if (!textarea.id) {
            textarea.id = textareaId;
        }
        
        // Get data attributes
        const teilaufgabeId = textarea.getAttribute('data-teilaufgabe-id');
        const teilaufgabeIndex = textarea.getAttribute('data-teilaufgabe-index');

        const isAllgemein = textarea.hasAttribute('data-allgemein-aufgabenstellung');
        
        // Get the preview element ID
        let previewId;
        if (isAllgemein) {
            // For Allgemeine Aufgabenstellung, get the preview container
            const aufgabeId = textareaId.replace('aufgabe-aufgabenstellung', '');
            previewId = `aufgabe-vorschau${aufgabeId ? '-' + aufgabeId : ''}`;
        } else if (teilaufgabeId) {
            // For regular Teilaufgaben
            previewId = `teilaufgabe-vorschau-${teilaufgabeId}`;
        } else {
            // Fallback: Use the tab content
            const tabNavButton = document.querySelector(`button[data-bs-target="#${textareaId}-panel"]`);
            if (tabNavButton) {
                const previewButton = document.querySelector(`button[data-bs-target^="#${textareaId.replace('aufgabenstellung', 'vorschau')}"]`);
                if (previewButton) {
                    const previewTarget = previewButton.getAttribute('data-bs-target');
                    previewId = previewTarget.substring(1).replace('-panel', '');
                }
            }
        }
        
        // Get the preview element
        const previewElement = previewId ? document.getElementById(previewId) : null;
        
        // Build toolbar configuration
        const toolbar = [
            "bold", "italic", "heading", "|", 
            "unordered-list", "ordered-list", "|", 
            "link", "image", 
            // Add custom image selector button that uses our material images
            {
                name: "custom-image-selector",
                action: function() {
                    // Get the kurseinheitId
                    let kurseinheitId = "0";
                    const kurseinheitIdField = document.querySelector('input[name="kurseinheitId"]');
                    if (kurseinheitIdField && kurseinheitIdField.value) {
                        kurseinheitId = kurseinheitIdField.value;
                    }
                    
                    // Open the image selector modal with HTMX
                    openImageSelectorModal(kurseinheitId, textareaId);
                },
                className: "bi bi-images",
                title: "Bild aus Materialien einfügen"
            }, {
                name: "draw-table",
                action: EasyMDE.drawTable,
                className: "fa fa-table",
                title: "Insert Table",
            }
        ];
        
        // Only add the "insert field" button for teilaufgabe editors (not for general Aufgabenstellung)
        if (!isAllgemein) {
            toolbar.push({
                name: "insert-field",
                action: function() {
                    // Only add field expressions to teilaufgabe markdown editors
                    if (teilaufgabeId) {
                        insertFieldExpression(editor);
                    }
                },
                className: "bi bi-input-cursor-text",
                title: "Eingabefeld einfügen"
            });
        }
        
        // Add the remaining toolbar items
        toolbar.push("|", "preview", "side-by-side", "fullscreen");
        
        // Initialize EasyMDE
        const editor = new EasyMDE({
            element: textarea,
            autofocus: false,
            spellChecker: false, // Disable spell checking
            forceSync: true, // Always sync to textarea
            status: false, // Hide status bar
            lineNumbers: false,
            toolbar: toolbar,
            // Update preview live
            previewRender: isAllgemein ? 
                (markdown, previewElementEditor) => previewElement ? previewAllgemeinMarkdown(markdown, previewElementEditor) : markdown :
                (markdown, previewElementEditor) => previewElement ? previewTeilaufgabeMarkdown(markdown, previewElementEditor, teilaufgabeId, teilaufgabeIndex) : markdown
        });
        
        // Store the EasyMDE instance on the textarea for potential later reference
        textarea._easymde = editor;

        // Set up change event to update the textarea and trigger any form validation
        editor.codemirror.on('change', () => {
            // Make sure the textarea's value is updated (should be handled by forceSync, but just to be sure)
            textarea.value = editor.value();
            
            // Trigger change event on the textarea to activate any form validation
            const changeEvent = new Event('change', { bubbles: true });
            textarea.dispatchEvent(changeEvent);
            
            // Update preview if available
            if (previewElement) {
                if (isAllgemein) {
                    previewAllgemeinMarkdown(editor.value(), previewElement);
                } else if (teilaufgabeId) {
                    previewTeilaufgabeMarkdown(editor.value(), previewElement, teilaufgabeId, teilaufgabeIndex);
                }
            }
        });
        
        // Initial preview
        if (previewElement) {
            if (isAllgemein) {
                previewAllgemeinMarkdown(editor.value(), previewElement);
            } else if (teilaufgabeId) {
                previewTeilaufgabeMarkdown(editor.value(), previewElement, teilaufgabeId, teilaufgabeIndex);
            }
        }
    });
}

/**
 * Opens the image selector modal using HTMX
 * @param {string} kurseinheitId - The ID of the kurseinheit
 * @param {string} editorId - The ID of the editor
 */
function openImageSelectorModal(kurseinheitId, editorId) {
    // Make sure we have the modal container
    createImageSelectorModal();
    
    // Get the modal content element
    const modalContentElement = document.getElementById('imageSelectorModalContent');
    
    // Get CSRF token information
    const { token, headerName } = getCsrfTokenInfo();
    
    // Prepare the request using HTMX
    htmx.ajax('GET', `/kursbetreuer/htmx/image-selector-modal/${kurseinheitId}?editorId=${editorId}`, {
        target: '#imageSelectorModalContent',
        swap: 'innerHTML',
        headers: {
            [headerName]: token
        }
    });
    
    // Show the modal
    const modalElement = document.getElementById('imageSelectorModal');
    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}

/**
 * Shows a modal dialog to insert a field expression
 * @param {EasyMDE} editor - The EasyMDE instance to insert the field expression into
 */
function insertFieldExpression(editor) {
    // Create field types
    const fieldTypes = [
        { id: 'text', name: 'Text', description: 'Textfeld für Freitext-Antworten', hasWidth: true },
        { id: 'num', name: 'Zahl', description: 'Numerisches Eingabefeld', hasWidth: true },
        { id: 'tex', name: 'LaTeX', description: 'Eingabefeld für mathematische Formeln', hasWidth: true },
        { id: 'multiline', name: 'Mehrzeilig', description: 'Mehrzeiliges Textfeld für längere Antworten', hasWidth: false }
    ];
    
    // Create a modal for field insertion
    const modalHtml = `
    <div class="modal fade" id="fieldExpressionModal" tabindex="-1" aria-labelledby="fieldExpressionModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="fieldExpressionModalLabel">Eingabefeld einfügen</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Schließen"></button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="mb-3">
                            <label for="fieldName" class="form-label">Feldname</label>
                            <input type="text" class="form-control" id="fieldName" placeholder="z.B. Antwort 1">
                            <div class="form-text">Der Name des Feldes für die Musterlösung</div>
                        </div>
                        <div class="mb-3">
                            <label for="fieldType" class="form-label">Feldtyp</label>
                            <select class="form-select" id="fieldType">
                                ${fieldTypes.map(type => `<option value="${type.id}">${type.name}</option>`).join('')}
                            </select>
                            <div class="form-text" id="fieldTypeDescription">${fieldTypes[0].description}</div>
                        </div>
                        <div class="mb-3" id="fieldWidthContainer">
                            <label for="fieldWidth" class="form-label">Feldbreite</label>
                            <input type="number" class="form-control" id="fieldWidth" min="1" max="100" value="20">
                            <div class="form-text">Die Breite des Eingabefeldes in Zeichen</div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Abbrechen</button>
                    <button type="button" class="btn btn-primary" id="insertFieldBtn">Einfügen</button>
                </div>
            </div>
        </div>
    </div>
    `;
    
    // Remove any existing modal
    const existingModal = document.getElementById('fieldExpressionModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // Add the modal to the document
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Initialize the Bootstrap modal
    const modalElement = document.getElementById('fieldExpressionModal');
    const modal = new bootstrap.Modal(modalElement);
    
    // Set up field type description changes
    const fieldTypeSelect = document.getElementById('fieldType');
    const fieldTypeDescription = document.getElementById('fieldTypeDescription');
    const fieldWidthContainer = document.getElementById('fieldWidthContainer');
    
    fieldTypeSelect.addEventListener('change', function() {
        const selectedType = fieldTypes.find(type => type.id === this.value);
        if (selectedType) {
            fieldTypeDescription.textContent = selectedType.description;
            
            // Show/hide field width based on field type
            if (selectedType.hasWidth) {
                fieldWidthContainer.style.display = 'block';
            } else {
                fieldWidthContainer.style.display = 'none';
            }
        }
    });
    
    // Set up insert button action
    const insertFieldBtn = document.getElementById('insertFieldBtn');
    insertFieldBtn.addEventListener('click', function() {
        const fieldName = document.getElementById('fieldName').value.trim();
        const fieldType = document.getElementById('fieldType').value;
        const fieldWidth = document.getElementById('fieldWidth').value;
        
        if (!fieldName) {
            alert('Bitte geben Sie einen Feldnamen ein.');
            return;
        }
        
        // Create the field expression based on type
        let fieldExpression;
        if (fieldType === 'multiline') {
            // Multiline fields use triple braces syntax
            fieldExpression = `{{{${fieldName}}}}`;
        } else {
            // Other fields use the standard syntax
            fieldExpression = `{${fieldName}:${fieldType}:${fieldWidth}}`;
        }
        
        // Insert the field expression at the cursor position
        editor.codemirror.replaceSelection(fieldExpression);
        
        // Close the modal
        modal.hide();
    });
    
    // Show the modal
    modal.show();
}

/**
 * Preview function for allgemeine Aufgabenstellung
 * @param {string} markdown - The markdown content to preview
 * @param {HTMLElement} previewElement - The element to render the preview in
 * @returns {string} - A placeholder message while loading
 */
function previewAllgemeinMarkdown(markdown, previewElement) {
    // Get CSRF token information
    const { token, headerName } = getCsrfTokenInfo();
    
    // Get the kurseinheitId from the hidden input field in the form
    let kurseinheitId = "0";  // Default fallback value
    const kurseinheitIdField = document.querySelector('input[name="kurseinheitId"]');
    if (kurseinheitIdField && kurseinheitIdField.value) {
        kurseinheitId = kurseinheitIdField.value;
    }
    
    // Always include kurseinheitId in the URL
    let url = `/kursbetreuer/htmx/markdown-preview?kurseinheitId=${kurseinheitId}`;
    
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'text/plain',
            [headerName]: token,
            'Accept': 'application/json'
        },
        body: markdown
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        // Update the preview element with the rendered HTML
        previewElement.innerHTML = data.outputHtml;
        
        // Render KaTeX expressions in the preview
        renderKaTeXInMarkdown(previewElement);
        
        // Check for duplicate field names
        const inputFields = data.inputFields || [];
        const duplicateFields = findDuplicateFieldNames(inputFields);
        
        // Get the textarea container to show warnings near the editor
        const textareaId = previewElement.id.replace('vorschau', 'aufgabenstellung');
        const textareaContainer = document.querySelector(`[id="${textareaId}-panel"]`);
        
        // Remove existing duplicate warning if any
        const existingWarning = textareaContainer ? textareaContainer.querySelector('.duplicate-fields-warning') : null;
        if (existingWarning) {
            existingWarning.remove();
        }
        
        // Show warning if duplicates found
        if (duplicateFields.length > 0) {
            // Create warning message
            const warningDiv = document.createElement('div');
            warningDiv.className = 'alert alert-warning duplicate-fields-warning mt-2';
            warningDiv.innerHTML = '<strong>Warnung: Doppelte Feldnamen gefunden!</strong><br>' + 
                'Die folgenden Feldnamen kommen mehrfach vor: ' + 
                '<span class="badge bg-danger">' + duplicateFields.join('</span>, <span class="badge bg-danger">') + '</span><br>' +
                'Bitte vergeben Sie eindeutige Feldnamen für eine korrekte Bewertung.';
            
            // Add to the editor panel after the textarea
            if (textareaContainer) {
                textareaContainer.appendChild(warningDiv);
            }
        }
    })
    .catch(error => {
        console.error('Error rendering preview:', error);
        previewElement.innerHTML = '<div class="alert alert-danger">Fehler beim Rendern der Vorschau</div>';
    });
    
    return 'Wird geladen...';
}

/**
 * Preview function for Teilaufgabe content
 * @param {string} markdown - The markdown content to preview
 * @param {HTMLElement} previewElement - The element to render the preview in
 * @param {string} teilaufgabeId - The ID of the Teilaufgabe
 * @returns {string} - A placeholder message while loading
 */
function previewTeilaufgabeMarkdown(markdown, previewElement, teilaufgabeId, teilaufgabeIndex) {
    // Get CSRF token information
    const { token, headerName } = getCsrfTokenInfo();

    // Get the kurseinheitId from the hidden input field in the form
    let kurseinheitId = "0";  // Default fallback value
    const kurseinheitIdField = document.querySelector('input[name="kurseinheitId"]');
    if (kurseinheitIdField && kurseinheitIdField.value) {
        kurseinheitId = kurseinheitIdField.value;
    }
    
    // Always include kurseinheitId in the URL
    let url = `/kursbetreuer/htmx/markdown-preview?kurseinheitId=${kurseinheitId}`;
    

    // Get the markdown preview from the server
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'text/plain',
            [headerName]: token,
            'Accept': 'application/json'
        },
        body: markdown
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        // Update the preview element with the rendered HTML
        previewElement.innerHTML = data.outputHtml;
        
        // Render KaTeX expressions in the preview
        renderKaTeXInMarkdown(previewElement);
        
        // Check for duplicate field names
        const inputFields = data.inputFields || [];
        const duplicateFields = findDuplicateFieldNames(inputFields);
        
        // Get the textarea container to show warnings near the editor
        const textareaId = previewElement.id.replace('vorschau', 'aufgabenstellung');
        const textareaContainer = document.querySelector(`[id="${textareaId}-panel"]`);
        
        // Remove existing duplicate warning if any
        const existingWarning = textareaContainer ? textareaContainer.querySelector('.duplicate-fields-warning') : null;
        if (existingWarning) {
            existingWarning.remove();
        }
        
        // Show warning if duplicates found
        if (duplicateFields.length > 0) {
            // Create warning message
            const warningDiv = document.createElement('div');
            warningDiv.className = 'alert alert-warning duplicate-fields-warning mt-2';
            warningDiv.innerHTML = '<strong>Warnung: Doppelte Feldnamen gefunden!</strong><br>' + 
                'Die folgenden Feldnamen kommen mehrfach vor: ' + 
                '<span class="badge bg-danger">' + duplicateFields.join('</span>, <span class="badge bg-danger">') + '</span><br>' +
                'Bitte vergeben Sie eindeutige Feldnamen für eine korrekte Bewertung.';
            
            // Add to the editor panel after the textarea
            if (textareaContainer) {
                textareaContainer.appendChild(warningDiv);
            }
        }
        
        // Get the musterloesung element if it exists
        const musterloesungElement = document.getElementById(`teilaufgabe-musterloesung-${teilaufgabeId}`);
        if (musterloesungElement) {
            // Clear current content
            musterloesungElement.innerHTML = '';
            
            // Add fields to the preview element
            if (inputFields.length === 0) {
                // No input fields found message
                const placeholderDiv = document.createElement('div');
                placeholderDiv.className = 'text-center py-2';
                placeholderDiv.innerHTML = '<span class="text-muted">Keine Eingabefelder im Markdown gefunden.</span>';
                musterloesungElement.appendChild(placeholderDiv);
            } else {
                // Add each field to the preview container
                inputFields.forEach(field => {
                    let fieldName = field.fieldName;
                    let fieldType = field.fieldType;
                    let fieldWidth = field.fieldSize;
                    if(field.multiline){
                        fieldType = "multiline";
                    }
                    
                    // Create a container for this field
                    const nodeDiv = document.createElement('div');
                    nodeDiv.className = 'mb-3';
                    
                    // Add a label with the field name from the InputFieldDTO
                    const nodeLabel = document.createElement('label');
                    nodeLabel.className = 'form-label fw-bold';
                    nodeLabel.textContent = fieldName;
                    
                    // If this field name is a duplicate, highlight it
                    if (duplicateFields.includes(fieldName)) {
                        nodeLabel.classList.add('text-danger');
                        const duplicateWarning = document.createElement('span');
                        duplicateWarning.className = 'ms-2 badge bg-danger';
                        duplicateWarning.textContent = 'Doppelt';
                        nodeLabel.appendChild(duplicateWarning);
                    }
                    
                    nodeDiv.appendChild(nodeLabel);
                    
                    // Create span container for the field
                    const span = document.createElement('span');
                    span.className = 'w-100 d-block';
                    
                    // Create appropriate input element based on field type
                    let questionElement;
                    switch (fieldType) {
                        case 'text':
                            questionElement = document.createElement('input');
                            questionElement.className = 'form-control question mb-2';
                            questionElement.style.width = Math.max(3, fieldWidth*0.8) + 'em';
                            break;
                        case 'num':
                            questionElement = document.createElement('input');
                            questionElement.type = 'number';
                            questionElement.className = 'form-control question mb-2';
                            questionElement.style.width = Math.max(3, fieldWidth*0.8) + 'em';
                            break;
                        case 'tex':
                            questionElement = document.createElement('input');
                            questionElement.className = 'form-control question mb-2';
                            questionElement.style.width = Math.max(3, fieldWidth*0.8) + 'em';
                            questionElement.setAttribute('data-field-type', 'tex');
                            break;
                        case 'multiline':
                            questionElement = document.createElement('textarea');
                            questionElement.className = 'form-control question auto-resize mb-1';
                            questionElement.rows = 3;
                            questionElement.style.width = '100%';
                            break;
                        default:
                            questionElement = document.createElement('input');
                            questionElement.type = 'text';
                            questionElement.className = 'form-control question mb-2';
                            questionElement.style.width = Math.min(100, fieldWidth * 10) + 'px';
                    }
                    
                    // Set common attributes for the question element
                    questionElement.setAttribute('data-field-name', fieldName);
                    
                    // If field name is duplicate, mark the input
                    if (duplicateFields.includes(fieldName)) {
                        questionElement.classList.add('is-invalid');
                    }
                    
                    // Look for a hidden field with the value 
                    const hiddenFieldId = `teilaufgabe-musterloesungs-feld-${teilaufgabeId}-${fieldName}`;
                    let hiddenField = document.getElementById(hiddenFieldId);
                    
                    // Get the container for hidden fields
                    const container = document.getElementById('hidden-musterloesungs-felder-container');
                    
                    // Get the current teilaufgabe index if we need to create a hidden field
                    let currentTeilaufgabeIndex = teilaufgabeIndex;
                    if (currentTeilaufgabeIndex === -1 || currentTeilaufgabeIndex === undefined) {
                        const teilaufgabeFieldsContainer = document.querySelector(`[data-teilaufgabe-id="${teilaufgabeId}"]`);
                        if (teilaufgabeFieldsContainer) {
                            currentTeilaufgabeIndex = teilaufgabeFieldsContainer.getAttribute('data-teilaufgabe-index');
                        }
                    }
                    
                    // If hidden field doesn't exist, create it
                    if (!hiddenField && container && currentTeilaufgabeIndex !== -1) {
                        hiddenField = document.createElement('input');
                        hiddenField.type = 'hidden';
                        hiddenField.id = hiddenFieldId;
                        hiddenField.name = `teilaufgaben[${currentTeilaufgabeIndex}].musterloesungFelder['${fieldName}']`;
                        hiddenField.value = ''; // Initial empty value
                        container.appendChild(hiddenField);
                    }
                    
                    // Set input value from hidden field if it exists
                    if (hiddenField) {
                        questionElement.value = hiddenField.value;
                    }
                    
                    // Add event listener to update hidden field when value changes
                    questionElement.addEventListener('input', function() {
                        // Get or create the hidden field again (might be needed if DOM changed)
                        let hiddenField = document.getElementById(hiddenFieldId);
                        
                        // If still no hidden field, create one
                        if (!hiddenField && container && currentTeilaufgabeIndex !== -1) {
                            hiddenField = document.createElement('input');
                            hiddenField.type = 'hidden';
                            hiddenField.id = hiddenFieldId;
                            hiddenField.name = `teilaufgaben[${currentTeilaufgabeIndex}].musterloesungFelder['${fieldName}']`;
                            container.appendChild(hiddenField);
                        }
                        
                        if (hiddenField) {
                            // Update the hidden field value
                            hiddenField.value = this.value;
                            
                            // Trigger change event
                            const changeEvent = new Event('change', { bubbles: true });
                            hiddenField.dispatchEvent(changeEvent);
                        }
                    });
                    
                    // Add to span container
                    span.prepend(questionElement);
                    
                    // Append the span to the container
                    nodeDiv.appendChild(span);
                    
                    // Add the field container to the musterloesung element
                    musterloesungElement.appendChild(nodeDiv);
                });
            }
            
            // Initialize auto-resize for any textareas
            initializeAutoResizeTextareas();
            
            // Initialize KaTeX rendering for tex fields
            initializeKaTeXRendering();
        }
    })
    .catch(error => {
        console.error('Error processing markdown:', error);
        previewElement.innerHTML = '<div class="alert alert-danger">Fehler beim Verarbeiten des Markdowns</div>';
    });
    
    return 'Wird geladen...';
}

/**
 * Initialize auto-resize for textareas
 */
function initializeAutoResizeTextareas() {
    document.querySelectorAll('textarea.auto-resize').forEach(textarea => {
        if (!textarea.classList.contains('auto-resize-initialized')) {
            textarea.classList.add('auto-resize-initialized');
            
            // Set initial height
            adjustTextareaHeight(textarea);
            
            // Add event listener for input events
            textarea.addEventListener('input', function() {
                adjustTextareaHeight(this);
            });
        }
    });
}

/**
 * Adjust the height of a textarea to fit its content
 * @param {HTMLTextAreaElement} textarea - The textarea element to adjust
 */
function adjustTextareaHeight(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = (textarea.scrollHeight) + 'px';
}

/**
 * Adjust heights for all textareas in a container, especially useful when tabs are shown
 * @param {HTMLElement} container - The container element to search for textareas
 */
function adjustTextareasInContainer(container) {
    if (!container) return;
    
    // Find all textareas with auto-resize class in the container
    const textareas = container.querySelectorAll('textarea.auto-resize');
    
    textareas.forEach(textarea => {
        // Only adjust if the textarea has content or is visible
        if (textarea.value.trim() || textarea.offsetHeight > 0) {
            // Use a small delay to ensure the tab content is fully rendered
            setTimeout(() => adjustTextareaHeight(textarea), 10);
        }
    });
}

/**
 * Gets the EasyMDE editor instance from a textarea
 * @param {HTMLTextAreaElement} textarea - The textarea element
 * @returns {EasyMDE|null} - The EasyMDE instance or null if not found
 */
function getEditorInstanceFromTextarea(textarea) {
    if (!textarea) return null;
    
    // First try to get the editor from the _easymde property
    if (textarea._easymde) {
        return textarea._easymde;
    }

    return null;
}

/**
 * Finds duplicate field names in a list of input fields
 * @param {Array} inputFields - Array of InputFieldDto objects
 * @returns {Array} - Array of duplicate field names
 */
function findDuplicateFieldNames(inputFields) {
    if (!inputFields || inputFields.length === 0) {
        return [];
    }
    
    // Create a map to count occurrences of each field name
    const fieldNameCounts = {};
    
    // Count occurrences
    inputFields.forEach(field => {
        const fieldName = field.fieldName;
        if (fieldName) {
            fieldNameCounts[fieldName] = (fieldNameCounts[fieldName] || 0) + 1;
        }
    });
    
    // Return field names that appear more than once
    return Object.entries(fieldNameCounts)
        .filter(([name, count]) => count > 1)
        .map(([name]) => name);
}

/**
 * Refreshes all EasyMDE editors within a container
 * @param {HTMLElement} container - The container element to search for editors
 */
function refreshEditorsInContainer(container) {
    if (!container) return;
    
    // Find all textareas with class "markdown-editor" in the container
    const textareas = container.querySelectorAll('textarea.markdown-editor.easymde-initialized');
    
    textareas.forEach(textarea => {
        // Get the editor instance from the textarea
        const editor = getEditorInstanceFromTextarea(textarea);
        
        if (editor) {
            // Access the CodeMirror instance and refresh it
            const cm = editor.codemirror;
            if (cm) {
                // Give the browser a moment to finish rendering the tab content
                setTimeout(() => {
                    cm.refresh();
                    
                    // Also ensure content is synced from the textarea to the editor
                    if (cm.getValue() !== textarea.value) {
                        cm.setValue(textarea.value);
                    }
                }, 10);
            }
        }
    });
}