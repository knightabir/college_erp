package com.college.erp.notification.template;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private final TemplateRenderer templateRenderer = new TemplateRenderer();

    @Test
    @DisplayName("render() — substitutes every {{key}} placeholder with its variable value")
    void render_substitutesAllPlaceholders() {
        // ARRANGE
        String template = "Hi {{name}}, your invoice {{invoiceNumber}} is due {{dueDate}}.";

        // ACT
        String result = templateRenderer.render(template,
                Map.of("name", "Arjun", "invoiceNumber", "INV-2026-001", "dueDate", "2026-08-15"));

        // ASSERT
        assertThat(result).isEqualTo("Hi Arjun, your invoice INV-2026-001 is due 2026-08-15.");
    }

    @Test
    @DisplayName("render() — a placeholder with no matching variable is left untouched")
    void render_missingVariable_leftAsIs() {
        // ARRANGE / ACT
        String result = templateRenderer.render("Hi {{name}}!", Map.of());

        // ASSERT
        assertThat(result).isEqualTo("Hi {{name}}!");
    }

    @Test
    @DisplayName("render() — null text or null/empty variables are returned unchanged, no NPE")
    void render_nullOrEmptyInputs_handledSafely() {
        assertThat(templateRenderer.render(null, Map.of("a", "b"))).isNull();
        assertThat(templateRenderer.render("Hi {{name}}", null)).isEqualTo("Hi {{name}}");
        assertThat(templateRenderer.render("Hi {{name}}", Map.of())).isEqualTo("Hi {{name}}");
    }
}
