/**
 * KaTeX rendering functionality
 * Handles rendering of LaTeX expressions in the document
 */

// Initialize KaTeX rendering for all elements with tex-field attribute when page loads
document.addEventListener('DOMContentLoaded', function() {
    initializeKaTeXRendering();
});

// Re-initialize KaTeX after HTMX content swaps
document.body.addEventListener('htmx:afterSwap', function() {
    initializeKaTeXRendering();
});

// Re-initialize KaTeX after Bootstrap tab show event
document.body.addEventListener('shown.bs.tab', function() {
    initializeKaTeXRendering();
});

// Re-initialize KaTeX after Bootstrap modal show event
document.body.addEventListener('shown.bs.modal', function() {
    initializeKaTeXRendering();
});

/**
 * Initialize KaTeX rendering for all elements with tex-field attribute
 */
function initializeKaTeXRendering() {
    // Find all tex inputs in regular forms
    const texInputs = document.querySelectorAll("input[data-field-type='tex']");
    
    texInputs.forEach(input => {
        // Check if this input has already been initialized
        if (input.dataset.texInitialized === 'true') {
            return;
        }

        // Mark as initialized
        input.dataset.texInitialized = 'true';

        // Create the output element next to the input
        let outputId;
        if(input.id){
            outputId = input.id + "-tex-preview";
        }else{
            outputId = input.getAttribute("data-field-name")+"-named-tex-preview";
        }

        let outputElement = document.getElementById(outputId);
        
        // If output element doesn't exist, create one after the input
        if (!outputElement) {
            outputElement = document.createElement('div');
            outputElement.id = outputId;
            outputElement.className = 'katex-preview mt-2 p-2 border rounded bg-light';
            
            // Insert after the input
            if (input.nextElementSibling) {
                input.parentNode.insertBefore(outputElement, input.nextElementSibling);
            } else {
                input.parentNode.appendChild(outputElement);
            }
        }

        // Initial rendering
        renderLatex(input, outputElement);

        // Add event listener for input changes
        input.addEventListener('input', function() {
            renderLatex(input, outputElement);
        });
    });

    // Find all solution fields with TeX content
    const texSolutionFields = document.querySelectorAll("div[data-field-type='tex']");
    
    texSolutionFields.forEach(div => {
        // Check if this div has already been initialized
        if (div.dataset.texInitialized === 'true') {
            return;
        }

        // Mark as initialized
        div.dataset.texInitialized = 'true';

        // Render KaTeX directly in the div
        renderLatexInDiv(div);
    });
    
    // Render KaTeX in any container that might have markdown content
    const containers = document.querySelectorAll('.musterloesungs-container, .aufgabenstellung-content, .feedback-content, .message-content');
    containers.forEach(container => {
        renderKaTeXInMarkdown(container);
    });
}

/**
 * Render LaTeX expression from input to output element
 * @param {HTMLElement} inputElement - Input element containing LaTeX
 * @param {HTMLElement} outputElement - Output element to render to
 */
function renderLatex(inputElement, outputElement) {
    const latex = inputElement.value;

    try {
        // Clear previous content
        outputElement.innerHTML = '';

        // Render the LaTeX expression
        katex.render(latex, outputElement, {
            throwOnError: false,
            displayMode: true
        });
    } catch (error) {
        outputElement.textContent = `Error: ${error.message}`;
    }
}

/**
 * Render LaTeX expression directly in a div element
 * @param {HTMLElement} divElement - Div element containing LaTeX content
 */
function renderLatexInDiv(divElement) {
    const latex = divElement.textContent || divElement.innerText;
    
    try {
        // Clear previous content
        divElement.innerHTML = '';
        
        // Render the LaTeX expression directly in the div
        katex.render(latex, divElement, {
            throwOnError: false,
            displayMode: false
        });
    } catch (error) {
        divElement.textContent = `Error: ${error.message}`;
    }
}

/**
 * Add KaTeX rendering to markdown preview content
 * @param {HTMLElement} container - Container element with markdown content
 */
function renderKaTeXInMarkdown(container) {
    if (!container) return;
    
    // Process all dollar sign delimited math expressions
    renderMathInElement(container, {
        delimiters: [
            {left: '$$', right: '$$', display: true},
            {left: '$', right: '$', display: false}
        ],
        throwOnError: false
    });
}

