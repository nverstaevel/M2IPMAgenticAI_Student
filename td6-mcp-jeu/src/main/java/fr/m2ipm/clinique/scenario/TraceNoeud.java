package fr.m2ipm.clinique.scenario;

import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Implémentation de {@link Trace} : les nœuds de l'agent sont numérotés sous son propre nœud. */
final class TraceNoeud implements Trace {

    private final SuiviExecution suivi;
    private final String runId;
    private final String parent;
    private final AtomicInteger compteur = new AtomicInteger();

    TraceNoeud(SuiviExecution suivi, String runId, String parent) {
        this.suivi = suivi;
        this.runId = runId;
        this.parent = parent;
    }

    @Override
    public String noeud(String genre, String libelle, String detail) {
        String id = parent + "." + compteur.incrementAndGet();
        suivi.noeud(runId, id, List.of(parent), genre == null ? Evenement.CODE : genre, libelle, Evenement.EN_COURS, detail, null, null, null);
        return id;
    }

    @Override
    public void terminer(String id, boolean ok, String resultat) {
        suivi.maj(runId, id, ok ? Evenement.OK : Evenement.ERREUR, null, resultat, null, null);
    }
}
