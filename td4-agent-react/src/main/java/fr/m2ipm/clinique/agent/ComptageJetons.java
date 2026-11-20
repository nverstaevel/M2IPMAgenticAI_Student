package fr.m2ipm.clinique.agent;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

/**
 * Additionne la consommation de CHAQUE appel au modèle fait par un agent du framework (une boucle d'outils en fait plusieurs).
 * L'advisor passe en dernier de la chaîne : le ToolCallingAdvisor relance la chaîne à chaque tour d'outils, donc il voit tous les appels.
 * Le cumul est rattaché au thread ; {@link SuiviOutils#ouvrir} l'ouvre et le publie sur le nœud de l'agent.
 */
public final class ComptageJetons {

    /** Cumul d'un agent : tokens d'entrée et de sortie, nombre d'appels au modèle. */
    public static final class Cumul {
        int entree;
        int sortie;
        int appels;
        boolean estime;

        public int entree() { return entree; }
        public int sortie() { return sortie; }
        public int appels() { return appels; }
        public int total() { return entree + sortie; }
        public boolean estime() { return estime; }
    }

    static final ThreadLocal<Cumul> COURANT = new ThreadLocal<>();

    private ComptageJetons() {}

    public static CallAdvisor advisor() {
        return new CallAdvisor() {
            @Override
            public String getName() { return "ComptageJetons"; }

            @Override
            public int getOrder() { return Ordered.LOWEST_PRECEDENCE - 1000; } // juste avant l'advisor terminal qui appelle le modèle

            @Override
            public ChatClientResponse adviseCall(ChatClientRequest requete, CallAdvisorChain chaine) {
                ChatClientResponse reponse = chaine.nextCall(requete);
                Cumul c = COURANT.get();
                if (c != null && reponse.chatResponse() != null) {
                    var r = reponse.chatResponse();
                    String texte = r.getResult() == null || r.getResult().getOutput() == null ? "" : r.getResult().getOutput().getText();
                    Jetons j = Jetons.de(r, requete.prompt(), texte);
                    c.entree += j.entree();
                    c.sortie += j.sortie();
                    c.appels++;
                    c.estime |= j.estime();
                }
                return reponse;
            }
        };
    }
}
