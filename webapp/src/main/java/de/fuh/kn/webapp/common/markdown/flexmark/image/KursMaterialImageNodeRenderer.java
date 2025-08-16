package de.fuh.kn.webapp.common.markdown.flexmark.image;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Node-Renderer für Image-Nodes, der die URLs von Bildern transformiert.
 * Bilder mit Dateinamen wie z.B. "bild.png" werden so umgeschrieben, dass sie
 * vom KursMaterialImageController bereitgestellt werden.
 */
public class KursMaterialImageNodeRenderer implements NodeRenderer {

    /**
     * DataKey zum Speichern der KurseinheitId im Renderer-Context
     */
    public static final DataKey<Long> KURSEINHEIT_ID = new DataKey<>("KURSEINHEIT_ID", 0L);

    private final Long kurseinheitId;
    
    public KursMaterialImageNodeRenderer(DataHolder options) {
        this.kurseinheitId = KURSEINHEIT_ID.get(options);
    }
    
    @Override
    public @NotNull Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        
        // Handler für Image-Nodes registrieren
        set.add(new NodeRenderingHandler<>(Image.class, this::render));
        
        return set;
    }
    
    /**
     * Rendert eine Image-Node mit transformierter URL
     */
    private void render(Image node, NodeRendererContext context, HtmlWriter html) {
        // Originale URL aus dem Markdown
        String url = node.getUrl().toString();
        
        // Nur lokale Dateien ohne komplette URLs transformieren
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/")) {
            // URL für den KursMaterialImageController erstellen
            // URL-Encoding des Dateinamens, damit Leerzeichen korrekt als %20 kodiert werden
            try {
                url = "/material/kurseinheit/" + kurseinheitId + "/bild?name=" + java.net.URLEncoder.encode(url, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // Fallback, falls URL-Encoding fehlschlägt
                url = "/material/kurseinheit/" + kurseinheitId + "/bild?name=" + url;
            }
        }
        
        // Bild-Tag erstellen
        html.attr("src", url);
        html.attr("alt", node.getText().toString());
        
        // Falls ein Titel gesetzt ist, als Tooltip verwenden
        if (node.getTitle().isNotNull()) {
            html.attr("title", node.getTitle().toString());
        }
        
        // Klasse für Styling hinzufügen
        html.attr("class", "markdown-image");
        
        // Leeres img-Tag ausgeben
        html.withAttr().tagVoid("img");
    }
    
    /**
     * Factory für den NodeRenderer
     */
    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer apply(final DataHolder options) {
            return new KursMaterialImageNodeRenderer(options);
        }
    }
}