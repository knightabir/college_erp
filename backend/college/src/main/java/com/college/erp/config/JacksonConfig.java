package com.college.erp.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * All JSON on the wire is snake_case (05-api-documentation.md §1: enrollment_number,
 * total_elements, request_id); Java fields stay camelCase. Two additional behaviours:
 *
 * <ul>
 *   <li><b>Flexible input</b> — request bodies are accepted in either snake_case
 *       ({@code college_name}) or camelCase ({@code collegeName}). Only serialization
 *       (responses) is locked to snake_case; deserialization accepts both. The
 *       {@link FlexibleInputNamingModule} handles this without any per-DTO annotations by
 *       registering a camelCase alias for every snake_case property at deserialization
 *       setup time. Jackson caches this per-class, so there is no per-request overhead.
 *   <li><b>Auto-trim</b> — all incoming {@link String} values are stripped of leading /
 *       trailing whitespace at the deserialization boundary so services never have to do
 *       it manually.
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .deserializerByType(String.class, new TrimmingStringDeserializer())
                .modulesToInstall(new FlexibleInputNamingModule());
    }

    // -------------------------------------------------------------------------
    // String trimmer
    // -------------------------------------------------------------------------

    /**
     * Strips leading/trailing whitespace from every incoming JSON string value so
     * services never receive untrimmed user input. Uses {@link String#strip()} (Java 11+)
     * which handles Unicode whitespace in addition to ASCII spaces/tabs.
     */
    static class TrimmingStringDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String value = p.getText();
            return value != null ? value.strip() : null;
        }
    }

    // -------------------------------------------------------------------------
    // Flexible input naming
    // -------------------------------------------------------------------------

    /**
     * Jackson module that makes every bean deserializer accept both snake_case and
     * camelCase property names in the request body, with no annotation changes needed on
     * individual DTOs. Works by adding a camelCase alias for each snake_case property
     * during {@link BeanDeserializerBuilder} setup. The existing snake_case property is
     * kept untouched; only an additional alias entry is added.
     */
    static class FlexibleInputNamingModule extends SimpleModule {

        FlexibleInputNamingModule() {
            super("FlexibleInputNamingModule");
        }

        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.addBeanDeserializerModifier(new BeanDeserializerModifier() {

                @Override
                public BeanDeserializerBuilder updateBuilder(DeserializationConfig config,
                        BeanDescription beanDesc, BeanDeserializerBuilder builder) {

                    List<SettableBeanProperty> aliases = new ArrayList<>();
                    Iterator<SettableBeanProperty> props = builder.getProperties();

                    while (props.hasNext()) {
                        SettableBeanProperty prop = props.next();
                        // prop.getName() is already snake_case at this point (naming strategy applied)
                        String camelName = snakeToCamel(prop.getName());
                        if (!camelName.equals(prop.getName())) {
                            aliases.add(prop.withName(new PropertyName(camelName)));
                        }
                    }

                    // Register camelCase aliases alongside the existing snake_case properties.
                    // addOrReplaceProperty with a new name simply inserts a second entry —
                    // the original snake_case property is unaffected.
                    aliases.forEach(alias -> builder.addOrReplaceProperty(alias, false));

                    return builder;
                }

                /** Converts {@code college_name} → {@code collegeName}. */
                private String snakeToCamel(String snake) {
                    if (!snake.contains("_")) {
                        return snake;
                    }
                    StringBuilder sb = new StringBuilder(snake.length());
                    boolean nextUpper = false;
                    for (int i = 0; i < snake.length(); i++) {
                        char c = snake.charAt(i);
                        if (c == '_') {
                            nextUpper = true;
                        } else if (nextUpper) {
                            sb.append(Character.toUpperCase(c));
                            nextUpper = false;
                        } else {
                            sb.append(c);
                        }
                    }
                    return sb.toString();
                }
            });
        }
    }
}

