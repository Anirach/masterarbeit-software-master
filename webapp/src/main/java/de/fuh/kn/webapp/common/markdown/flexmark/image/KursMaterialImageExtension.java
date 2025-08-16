package de.fuh.kn.webapp.common.markdown.flexmark.image;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataHolder;

/**
 * Extension für den Markdown-Renderer, um Bilder zu transformieren.
 * Diese Extension registriert den {@link KursMaterialImageNodeRenderer}, der
 * Bild-URLs umschreibt, um sie vom KursMaterialImageController bereitzustellen.
 */
public class KursMaterialImageExtension implements HtmlRenderer.HtmlRendererExtension {
    /**
     * Erstellt die Extension.
     *
     * @return Die Extension
     */
    public static KursMaterialImageExtension create() {
        return new KursMaterialImageExtension();
    }

    @Override
    public void rendererOptions(MutableDataHolder options) {
        // Keine speziellen Options benötigt
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        rendererBuilder.nodeRendererFactory(new KursMaterialImageNodeRenderer.Factory());
    }
}
