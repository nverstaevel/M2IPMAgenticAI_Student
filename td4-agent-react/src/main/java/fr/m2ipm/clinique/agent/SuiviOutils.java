package fr.m2ipm.clinique.agent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Fait apparaître dans le graphe les outils appelés PAR un agent (vétérinaire, gardien, travailleurs) dont la boucle d'outils
 * est gérée par le framework. L'agent qui appelle ouvre un contexte ({@link #ouvrir}) ; tout outil enveloppé par
 * {@link #envelopper} appelé dans ce thread devient un nœud enfant du nœud de l'agent. Hors contexte, l'enveloppe est transparente.
 */
public final class SuiviOutils {

    private record Contexte(SuiviExecution suivi, String runId, String parent, AtomicInteger compteur, ComptageJetons.Cumul cumul) {}

    private static final ThreadLocal<Contexte> COURANT = new ThreadLocal<>();

    private SuiviOutils() {}

    /**
     * À utiliser dans un try-with-resources autour de l'appel de l'agent. À la fermeture, la consommation cumulée de tous
     * les appels au modèle faits dans le bloc est publiée sur le nœud de l'agent (entrée, sortie, nombre d'appels).
     */
    public static Portee ouvrir(SuiviExecution suivi, String runId, String parent) {
        Contexte precedent = COURANT.get();
        ComptageJetons.Cumul precedentCumul = ComptageJetons.COURANT.get();
        var cumul = new ComptageJetons.Cumul();
        COURANT.set(new Contexte(suivi, runId, parent, new AtomicInteger(), cumul));
        ComptageJetons.COURANT.set(cumul);
        return new Portee() {
            @Override
            public void close() {
                if (cumul.appels() > 0) {
                    suivi.jetons(runId, parent, cumul.entree(), cumul.sortie(), cumul.appels(), cumul.estime());
                }
                if (precedent == null) { COURANT.remove(); } else { COURANT.set(precedent); }
                if (precedentCumul == null) { ComptageJetons.COURANT.remove(); } else { ComptageJetons.COURANT.set(precedentCumul); }
            }

            @Override
            public int total() { return cumul.total(); }
        };
    }

    public interface Portee extends AutoCloseable {
        @Override
        void close();

        /** Tokens consommés dans ce bloc (0 si le client n'a pas l'advisor de comptage). */
        int total();
    }

    /**
     * Un appel au modèle rejeté par le fournisseur (appel d'outil mal formé, surcharge passagère) est tenté une seconde fois ;
     * la première erreur reste visible dans le détail du nœud. Un second échec est propagé.
     */
    public static <T> T reessayer(SuiviExecution suivi, String runId, String id, java.util.function.Supplier<T> appel) {
        try {
            return appel.get();
        } catch (RuntimeException premier) {
            suivi.maj(runId, id, Evenement.EN_COURS, "Tentative 1 échouée : " + premier.getMessage() + "\nNouvelle tentative…",
                    null, null, null);
            return appel.get();
        }
    }

    public static ToolCallback[] envelopper(ToolCallback... outils) {
        ToolCallback[] r = new ToolCallback[outils.length];
        for (int i = 0; i < outils.length; i++) {
            r[i] = new OutilSuivi(outils[i]);
        }
        return r;
    }

    private static final class OutilSuivi implements ToolCallback {

        private final ToolCallback delegue;

        OutilSuivi(ToolCallback delegue) {
            this.delegue = delegue;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegue.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegue.getToolMetadata();
        }

        @Override
        public String call(String entree) {
            return suivre(entree, () -> delegue.call(entree));
        }

        @Override
        public String call(String entree, ToolContext contexte) {
            return suivre(entree, () -> delegue.call(entree, contexte));
        }

        private String suivre(String entree, java.util.function.Supplier<String> appel) {
            Contexte c = COURANT.get();
            if (c == null) {
                return appel.get();
            }
            String id = c.parent() + "-o" + c.compteur().incrementAndGet();
            c.suivi().noeud(c.runId(), id, List.of(c.parent()), Evenement.OUTIL, delegue.getToolDefinition().name(),
                    Evenement.EN_COURS, entree, null, null, null);
            long debut = System.nanoTime();
            try {
                String resultat = appel.get();
                c.suivi().maj(c.runId(), id, Evenement.OK, null, tronquer(resultat), (System.nanoTime() - debut) / 1_000_000, null);
                return resultat;
            } catch (RuntimeException e) {
                c.suivi().maj(c.runId(), id, Evenement.ERREUR, null, String.valueOf(e.getMessage()),
                        (System.nanoTime() - debut) / 1_000_000, null);
                throw e;
            }
        }

        private static String tronquer(String s) {
            return s != null && s.length() > 500 ? s.substring(0, 500) + "…" : s;
        }
    }
}
