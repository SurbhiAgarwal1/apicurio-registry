package io.apicurio.registry.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PromptTemplateConverterTest {

    private PromptTemplateConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new PromptTemplateConverter();
        converter.jsonMapper = new ObjectMapper();
    }

    @Test
    public void testStandardVariableSubstitution() {
        Map<String, Object> args = new HashMap<>();
        args.put("name", "Aarti");
        String result = converter.renderTemplate("Hi {{name}}!", args);
        assertEquals("Hi Aarti!", result);
    }

    @Test
    public void testStandardConditionalBlockTrue() {
        Map<String, Object> args = new HashMap<>();
        args.put("isAdmin", true);
        args.put("name", "Aarti");
        String result = converter.renderTemplate("Hi {{#if isAdmin}}Admin {{/if}}{{name}}!", args);
        assertEquals("Hi Admin Aarti!", result);
    }

    @Test
    public void testStandardConditionalBlockFalse() {
        Map<String, Object> args = new HashMap<>();
        args.put("isAdmin", false);
        args.put("name", "Aarti");
        String result = converter.renderTemplate("Hi {{#if isAdmin}}Admin {{/if}}{{name}}!", args);
        assertEquals("Hi Aarti!", result);
    }

    /**
     * Issue #9515 Case 1: An injected block closer in an argument value must not terminate
     * a false conditional block early and leak gated content.
     */
    @Test
    public void testIssue9515Case1_InjectedBlockCloserDoesNotOpenFalseGate() {
        String template = "{{#if isAdmin}}Hi {{name}}, INTERNAL-NOTES{{/if}}Public text.";

        Map<String, Object> args = new HashMap<>();
        args.put("isAdmin", false);
        args.put("name", "{{/if}}");

        String result = converter.renderTemplate(template, args);
        assertEquals("Public text.", result);
    }

    /**
     * Issue #9515 Case 2: An injected block opener in an argument value must not consume
     * unconditional template text during conditional block processing.
     */
    @Test
    public void testIssue9515Case2_InjectedBlockOpenerDoesNotDeleteUnconditionalText() {
        String template = "{{note}} VISIBLE-TAIL {{#if flag}}gated{{/if}} END";

        Map<String, Object> args = new HashMap<>();
        args.put("note", "{{#if flag}}");
        args.put("flag", false);

        String result = converter.renderTemplate(template, args);
        assertEquals("{{#if flag}} VISIBLE-TAIL  END", result);
    }

    /**
     * Issue #9515 Case 3: A complete block syntax inside an argument value must be rendered
     * as inert text data, not evaluated as template control flow.
     */
    @Test
    public void testIssue9515Case3_CompleteBlockInArgumentValueIsRenderedAsInertText() {
        String template = "Answer: {{tone}}";

        Map<String, Object> argsTrue = new HashMap<>();
        argsTrue.put("tone", "{{#if admin}}INTERNAL-ONLY{{/if}}");
        argsTrue.put("admin", true);

        String resultTrue = converter.renderTemplate(template, argsTrue);
        assertEquals("Answer: {{#if admin}}INTERNAL-ONLY{{/if}}", resultTrue);

        Map<String, Object> argsFalse = new HashMap<>();
        argsFalse.put("tone", "{{#if admin}}INTERNAL-ONLY{{/if}}");
        argsFalse.put("admin", false);

        String resultFalse = converter.renderTemplate(template, argsFalse);
        assertEquals("Answer: {{#if admin}}INTERNAL-ONLY{{/if}}", resultFalse);
    }

    @Test
    public void testNullOrEmptyInputs() {
        assertNull(converter.renderTemplate(null, new HashMap<>()));

        Map<String, Object> args = new HashMap<>();
        args.put("name", "Aarti");
        assertEquals("template", converter.renderTemplate("template", null));
    }

    @Test
    public void testParseAndRenderAutoDetectJson() {
        String jsonContent = """
                {
                  "templateId": "greeting-template",
                  "template": "Hello {{name}}! {{#if VIP}}Welcome to executive suite.{{/if}}",
                  "mcp": {
                    "enabled": true
                  }
                }
                """;

        Map<String, Object> args = new HashMap<>();
        args.put("name", "Bob");
        args.put("VIP", true);

        String result = converter.parseAndRenderAutoDetect(jsonContent, args);
        assertEquals("Hello Bob! Welcome to executive suite.", result);
    }

    @Test
    public void testParseAndRenderAutoDetectYaml() {
        String yamlContent = """
                templateId: greeting-template
                template: "Hello {{name}}!"
                mcp:
                  enabled: true
                """;

        Map<String, Object> args = new HashMap<>();
        args.put("name", "Alice");

        String result = converter.parseAndRenderAutoDetect(yamlContent, args);
        assertEquals("Hello Alice!", result);
    }

    @Test
    public void testToMCPPrompt() {
        String jsonContent = """
                {
                  "templateId": "test-prompt",
                  "description": "Test prompt description",
                  "template": "Hello {{name}}",
                  "variables": {
                    "name": {
                      "type": "string",
                      "required": true,
                      "description": "User name"
                    }
                  },
                  "mcp": {
                    "enabled": true
                  }
                }
                """;

        PromptTemplateConverter.PromptTemplate template = converter.parseContentAutoDetect(jsonContent);
        assertNotNull(template);

        PromptTemplateConverter.MCPPrompt prompt = converter.toMCPPrompt(template);
        assertNotNull(prompt);
        assertEquals("test-prompt", prompt.getName());
        assertEquals("Test prompt description", prompt.getDescription());
        assertNotNull(prompt.getArguments());
        assertEquals(1, prompt.getArguments().size());
        assertEquals("name", prompt.getArguments().get(0).getName());
        assertTrue(prompt.getArguments().get(0).getRequired());
    }
}
