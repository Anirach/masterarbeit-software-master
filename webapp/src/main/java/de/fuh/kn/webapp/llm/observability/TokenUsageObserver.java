package de.fuh.kn.webapp.llm.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.model.ModelRequest;
import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Überwacht die Token-Nutzung bei LLM-Anfragen und berechnet die Kosten.
 */
@Slf4j
@Component
public class TokenUsageObserver implements ObservationHandler<ModelObservationContext<ModelRequest, ModelResponse>> {

    private static final ThreadLocal<CostContext> CURRENT_COST_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> CONTEXT_STACK_DEPTH = new ThreadLocal<>();
    
    /**
     * Startet einen neuen Kostenkontext für die Messung der Kosten über mehrere LLM-Aufrufe hinweg.
     * Unterstützt auch verschachtelte Kontexte (nur der äußerste Kontext erfasst die Kosten).
     * 
     * @param operationType Der Typ der Operation, für die Kosten erfasst werden sollen
     * @return Die ID des erstellten Kostenkontexts
     */
    public String startCostContext(OperationType operationType) {
        Integer depth = CONTEXT_STACK_DEPTH.get();
        
        if (depth == null) {
            // Erster Kontext
            String contextId = UUID.randomUUID().toString();
            CostContext costContext = new CostContext(contextId, operationType);
            CURRENT_COST_CONTEXT.set(costContext);
            CONTEXT_STACK_DEPTH.set(1);
            log.debug("Kostenkontext gestartet: {} (Typ: {})", contextId, operationType);
            return contextId;
        } else {
            // Verschachtelter Kontext
            CONTEXT_STACK_DEPTH.set(depth + 1);
            CostContext existingContext = CURRENT_COST_CONTEXT.get();
            log.debug("Verschachtelter Kostenkontext (Tiefe: {}, bestehender Kontext: {}, Typ: {})", 
                    depth + 1, existingContext.getId(), existingContext.getOperationType());
            return existingContext.getId();
        }
    }

    /**
     * Beendet den aktuellen Kostenkontext und gibt ihn zurück.
     * Bei verschachtelten Kontexten wird nur der äußerste Kontext tatsächlich beendet.
     *
     * @return Der beendete Kostenkontext mit der Gesamtsumme oder null bei inneren Kontexten
     */
    public CostContext endCostContext() {
        Integer depth = CONTEXT_STACK_DEPTH.get();
        
        if (depth == null) {
            log.warn("Versuch, einen nicht existierenden Kostenkontext zu beenden");
            CostContext dummyContext = new CostContext("dummy", OperationType.OTHER);
            return dummyContext;
        }
        
        if (depth == 1) {
            // Äußerster Kontext wird tatsächlich beendet
            CostContext costContext = CURRENT_COST_CONTEXT.get();
            CURRENT_COST_CONTEXT.remove();
            CONTEXT_STACK_DEPTH.remove();
            log.debug("Kostenkontext beendet: {} (Typ: {})", 
                    costContext.getId(), costContext.getOperationType());
            return costContext;
        } else {
            // Innerer Kontext wird nur dekrementiert
            CONTEXT_STACK_DEPTH.set(depth - 1);
            CostContext currentContext = CURRENT_COST_CONTEXT.get();
            log.debug("Verschachtelter Kostenkontext beendet (verbleibende Tiefe: {}, Kontext: {}, Typ: {})", 
                    depth - 1, currentContext.getId(), currentContext.getOperationType());
            return currentContext;
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ModelObservationContext;
    }

    @Override
    public void onStart(ModelObservationContext<ModelRequest, ModelResponse> context) {
        // Prüfen, ob der Prompt zu lang ist
        if(context.getRequest() instanceof Prompt prompt) {
            if(prompt.getContents().length() >= 25000) {
                throw new IllegalStateException("Prompt zu lang: " + prompt.getContents().length());
            }
        }
    }

    @Override
    public void onStop(ModelObservationContext<ModelRequest, ModelResponse> context) {
        BigDecimal cost = null;
        Integer inputTokens = 0;
        Integer outputTokens = 0;
        String model = "unknown";

        // Embedding-Anfragen verarbeiten
        if(context.getRequest() instanceof EmbeddingRequest request && context.getResponse() instanceof EmbeddingResponse response) {
            EmbeddingResponseMetadata metadata = response.getMetadata();
            model = metadata.getModel();
            inputTokens = metadata.getUsage().getPromptTokens();

            // Text-embedding-3-small ($0.02 pro 1M Token)
            if(model.equals(OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_SMALL.getValue()) || 
               model.contains("text-embedding-3-small")) {
                cost = new BigDecimal("0.00000002");
                cost = cost.multiply(BigDecimal.valueOf(inputTokens));
            }
            // Text-embedding-3-large ($0.13 pro 1M Token)
            else if(model.equals(OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE.getValue()) || 
                    model.contains("text-embedding-3-large")) {
                cost = new BigDecimal("0.00000013");
                cost = cost.multiply(BigDecimal.valueOf(inputTokens));
            }
            // Fallback für andere Embedding-Modelle (höherer Preis als Annahme)
            else {
                cost = new BigDecimal("0.00000013");
                cost = cost.multiply(BigDecimal.valueOf(inputTokens));
            }

            log.info("Embedding mit Modell {} ausgeführt, {} Input-Token. Kosten: ${}",
                    model,
                    inputTokens,
                    cost != null ? cost.toPlainString() : "--"
            );
        } 
        // Chat-Anfragen verarbeiten
        else if(context.getRequest() instanceof Prompt request && context.getResponse() instanceof ChatResponse response) {
            ChatResponseMetadata metadata = response.getMetadata();
            model = metadata.getModel();
            inputTokens = metadata.getUsage().getPromptTokens();
            outputTokens = metadata.getUsage().getCompletionTokens();

            cost = BigDecimal.ZERO;
            
            // GPT-4.1 / GPT-4.1-2025-04-14 ($2.00/$0.50/$8.00)
            if (model.contains("gpt-4.1") && !model.contains("mini") && !model.contains("nano")) {
                // $2.00 pro 1M Token Input, $8.00 pro 1M Token Output
                // Cached Input ist preislich nicht relevant, da wir nicht wissen ob gecacht wird
                cost = cost.add(new BigDecimal("0.000002").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.000008").multiply(BigDecimal.valueOf(outputTokens)));
            }
            // GPT-4.1-mini / GPT-4.1-mini-2025-04-14 ($0.40/$0.10/$1.60)
            else if (model.contains("gpt-4.1-mini")) {
                // $0.40 pro 1M Token Input, $1.60 pro 1M Token Output
                cost = cost.add(new BigDecimal("0.0000004").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.0000016").multiply(BigDecimal.valueOf(outputTokens)));
            }
            // GPT-4.1-nano / GPT-4.1-nano-2025-04-14 ($0.10/$0.025/$0.40)
            else if (model.contains("gpt-4.1-nano")) {
                // $0.10 pro 1M Token Input, $0.40 pro 1M Token Output
                cost = cost.add(new BigDecimal("0.0000001").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.0000004").multiply(BigDecimal.valueOf(outputTokens)));
            }
            // GPT-4o / GPT-4o-2024-08-06 ($2.50/$1.25/$10.00)
            else if (model.contains("gpt-4o") && !model.contains("mini")) {
                // $2.50 pro 1M Token Input, $10.00 pro 1M Token Output
                cost = cost.add(new BigDecimal("0.0000025").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.00001").multiply(BigDecimal.valueOf(outputTokens)));
            }
            // GPT-4o-mini / GPT-4o-mini-2024-07-18 ($0.15/$0.075/$0.60)
            else if (model.contains("gpt-4o-mini")) {
                // $0.15 pro 1M Token Input, $0.60 pro 1M Token Output
                cost = cost.add(new BigDecimal("0.00000015").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.0000006").multiply(BigDecimal.valueOf(outputTokens)));
            }
            //Claude Sonnet 4
            else if(model.contains("claude-sonnet-4")){
                // $3 pro 1M Token Input, $15 pro 1M Token Output
                cost = cost.add(new BigDecimal("0.000003").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.000015").multiply(BigDecimal.valueOf(outputTokens)));
            }
            //Claude 3.5 Haiku
            else if(model.contains("claude-3-5-haiku")){
                // $0,8 pro 1M Token Input, $4 pro 1M Token Output
                cost = cost.add(new BigDecimal("0.0000008").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.000004").multiply(BigDecimal.valueOf(outputTokens)));
            }
            // Fallback für unbekannte Modelle (höchster Preis als Annahme)
            else {
                // Annahme: GPT-4o Preise als Obergrenze
                cost = cost.add(new BigDecimal("0.0000025").multiply(BigDecimal.valueOf(inputTokens)));
                cost = cost.add(new BigDecimal("0.00001").multiply(BigDecimal.valueOf(outputTokens)));
            }

            log.info("Chat-Modell {} ausgeführt mit {} Input-Token und {} Output-Token. Kosten: ${}", 
                    model, inputTokens, outputTokens, cost != null ? cost.toPlainString() : "--");
        } else {
            log.warn("Logger unterstützt diese Modelloperation nicht: {}", context.getRequest().getClass().getName());
        }

        // Kosten zum aktuellen Kontext hinzufügen
        if(cost != null) {
            CostContext costContext = CURRENT_COST_CONTEXT.get();
            if(costContext != null) {
                costContext.addCost(cost);
                costContext.setInputTokens(costContext.getInputTokens() + inputTokens);
                costContext.setOutputTokens(costContext.getOutputTokens() + outputTokens);
                costContext.setModel(model); // Letztes verwendetes Modell speichern
            }
        }else{
            log.warn("AI-Kosten außerhalb von CostContext");
        }
    }

    /**
     * Klasse zur Erfassung von Kosten über mehrere LLM-Aufrufe hinweg.
     */
    @Getter
    @Setter
    public static class CostContext {
        private final String id;
        private final OperationType operationType;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private int inputTokens = 0;
        private int outputTokens = 0;
        private String model;

        public CostContext(String id, OperationType operationType) {
            this.id = id;
            this.operationType = operationType;
        }

        public void addCost(BigDecimal cost) {
            totalCost = totalCost.add(cost);
        }

        /**
         * Only exists for backward compatibility. Always returns null.
         * User IDs should be tracked in the entity layer, not in the cost context.
         */
        public Long getUserId() {
            return null;
        }
    }
}