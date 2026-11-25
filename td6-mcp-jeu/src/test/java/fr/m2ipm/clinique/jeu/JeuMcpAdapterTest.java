package fr.m2ipm.clinique.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/** Un jeu compilé sans -parameters déclare « arg0, arg1 » : les arguments par nom sont renommés dans l'ordre. */
class JeuMcpAdapterTest {

    private static ToolCallback outil(String schema) {
        return new ToolCallback() {
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("soigner").description("faux outil").inputSchema(schema).build();
            }
            public String call(String entree) { return entree; }
        };
    }

    @Test
    void lesParametresAnonymesSontRenommesDansLOrdre() {
        ToolCallback cb = outil("{\"type\":\"object\",\"properties\":{\"arg1\":{\"type\":\"string\"},\"arg0\":{\"type\":\"string\"}}}");
        assertThat(JeuMcp.adapter(cb, "{\"idPatient\":\"P01\",\"medicament\":\"eau-de-glacier\"}"))
                .isEqualTo("{\"arg0\":\"P01\",\"arg1\":\"eau-de-glacier\"}");
    }

    @Test
    void lesNomsNormauxNeChangentPas() {
        ToolCallback cb = outil("{\"type\":\"object\",\"properties\":{\"idPatient\":{\"type\":\"string\"}}}");
        assertThat(JeuMcp.adapter(cb, "{\"idPatient\":\"P01\"}")).isEqualTo("{\"idPatient\":\"P01\"}");
    }

    @Test
    void unSchemaSansProprieteOuDesArgumentsDejaAnonymesRestentIntacts() {
        assertThat(JeuMcp.adapter(outil("{\"type\":\"object\"}"), "{}")).isEqualTo("{}");
        ToolCallback cb = outil("{\"type\":\"object\",\"properties\":{\"arg0\":{\"type\":\"string\"}}}");
        assertThat(JeuMcp.adapter(cb, "{\"arg0\":\"P01\"}")).isEqualTo("{\"arg0\":\"P01\"}");
    }
}
