package fr.m2ipm.clinique;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Outil de test sans base de données. */
class OutilsBidon {
    int appels = 0;

    @Tool(description = "Renvoie le texte donné.")
    public String echo(@ToolParam(description = "texte") String texte) {
        appels++;
        return "echo:" + texte;
    }

    @Tool(description = "Renvoie un très long texte.")
    public String long_() {
        return "x".repeat(2000);
    }
}
