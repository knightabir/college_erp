package com.college.erp.notification.template;

import java.util.Map;

import org.springframework.stereotype.Component;

/** 08-system-architecture.md's file tree: `template/TemplateRenderer.java` — variable substitution. */
@Component
public class TemplateRenderer {

    /** Replaces every {{key}} placeholder in the text with variables.get(key). Placeholders with
     *  no matching entry in variables are left as-is rather than silently blanked, so a missing
     *  variable is visible in the rendered output instead of disappearing. */
    public String render(String text, Map<String, String> variables) {
        if (text == null || variables == null || variables.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
