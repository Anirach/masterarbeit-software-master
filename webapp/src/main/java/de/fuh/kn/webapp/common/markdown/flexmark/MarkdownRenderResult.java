package de.fuh.kn.webapp.common.markdown.flexmark;

import lombok.*;

import java.util.List;

/**
 * DTO für das Ergebnis des Markdown-Renderings.
 * Enthält das gerenderte HTML und die extrahierten Input-Felder.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownRenderResult {
    
    /**
     * Das gerenderte HTML.
     */
    private String html;
    
    /**
     * Die extrahierten Input-Felder.
     */
    private List<InputFieldDto> inputFields;
}
