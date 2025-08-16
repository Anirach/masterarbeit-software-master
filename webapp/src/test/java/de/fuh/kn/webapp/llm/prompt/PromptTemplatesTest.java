package de.fuh.kn.webapp.llm.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

class PromptTemplatesTest {


    @Test
    public void testRendering(){

        Map<String, Object> promptParameters = Map.of(
                "thema", "query.text()",
                "kursmaterial", "documentContext",
                "format", "jsonFormat",
                "beispielaufgaben", "aufgabenContext");

        PromptTemplate promptTemplate = PromptTemplate.builder().template(PromptTemplates.GENERATOR_TEMPLATE).build();
        promptTemplate.render(promptParameters);

    }

}