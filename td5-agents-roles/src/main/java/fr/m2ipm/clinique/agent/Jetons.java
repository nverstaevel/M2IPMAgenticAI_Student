package fr.m2ipm.clinique.agent;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/** Consommation d'un appel au modèle : annoncée par le fournisseur, ou à défaut estimée (1 token pour 4 caractères). */
public record Jetons(int entree, int sortie, boolean estime) {

    public int total() { return entree + sortie; }

    public static Jetons de(ChatResponse reponse, Prompt prompt, String texte) {
        var usage = reponse == null || reponse.getMetadata() == null ? null : reponse.getMetadata().getUsage();
        Integer e = usage == null ? null : usage.getPromptTokens();
        Integer s = usage == null ? null : usage.getCompletionTokens();
        Integer t = usage == null ? null : usage.getTotalTokens();
        if (t != null && t > 0) {
            int entree = e == null ? 0 : e;
            int sortie = s == null ? Math.max(0, t - entree) : s;
            return new Jetons(entree, sortie, false);
        }
        int caracteres = prompt == null ? 0 : prompt.getInstructions().stream().mapToInt(m -> m.getText() == null ? 0 : m.getText().length()).sum();
        return new Jetons((caracteres + 3) / 4, ((texte == null ? 0 : texte.length()) + 3) / 4, true);
    }
}
