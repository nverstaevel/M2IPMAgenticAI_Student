package fr.m2ipm.clinique.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Suivi en direct des exécutions d'agents : chaque appel au modèle, chaque outil, chaque délégation devient un nœud
 * du graphe de la page {@code /agents}. Les événements sont conservés en mémoire (les 50 dernières exécutions) pour
 * pouvoir rejouer, et diffusés en Server-Sent Events aux navigateurs abonnés.
 */
@Component
public class SuiviExecution {

    static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");
    static final int MAX_RUNS = 50;

    public record Resume(String runId, String titre, String statut, long debut) {}

    private static final class Run {
        final String runId;
        volatile String titre;
        volatile String statut = Evenement.EN_COURS;
        final long debut = System.currentTimeMillis();
        final List<Evenement> evenements = new ArrayList<>();
        final List<SseEmitter> abonnes = new CopyOnWriteArrayList<>();
        boolean termine;

        Run(String runId, String titre) {
            this.runId = runId;
            this.titre = titre;
        }
    }

    private final Map<String, Run> runs = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Run> eldest) {
            return size() > MAX_RUNS;
        }
    });

    /** Idempotent : le contrôleur peut déclarer le run avant que l'agent ne démarre. */
    public void demarrer(String runId, String titre) {
        Run existant = runs.get(runId);
        if (existant != null) {
            return;
        }
        Run run = new Run(runId, titre);
        runs.put(runId, run);
        publier(run, "demarrage", null, null, null, titre, Evenement.EN_COURS, null, null, null, null);
    }

    /** Crée un nœud (ou le remplace). */
    public void noeud(String runId, String id, List<String> parents, String genre, String libelle, String statut,
                      String detail, String resultat, Long dureeMs, Integer tokens) {
        Run run = runs.get(runId);
        if (run != null) {
            publier(run, "noeud", id, parents, genre, libelle, statut, detail, resultat, dureeMs, tokens);
        }
    }

    /** Met à jour un nœud existant : seuls les champs non nuls changent. */
    public void maj(String runId, String id, String statut, String detail, String resultat, Long dureeMs, Integer tokens) {
        Run run = runs.get(runId);
        if (run != null) {
            publier(run, "noeud", id, null, null, null, statut, detail, resultat, dureeMs, tokens);
        }
    }

    /** Consommation du modèle d'un nœud (entrée, sortie, nombre d'appels) ; le total devient le champ « tokens » du nœud. */
    public void jetons(String runId, String id, int entree, int sortie, int appels, boolean estime) {
        Run run = runs.get(runId);
        if (run != null) {
            synchronized (run) {
                Evenement e = new Evenement(run.evenements.size() + 1L, System.currentTimeMillis(), run.runId, "noeud", id, null,
                        null, null, null, null, null, null, entree + sortie, entree, sortie, appels, estime);
                run.evenements.add(e);
                diffuser(run, e);
            }
        }
    }

    /** Clôt l'exécution (motif : REPONSE_FINALE, PLAFOND_ETAPES, ERREUR…) et ferme les flux. */
    public void terminer(String runId, String motif) {
        Run run = runs.get(runId);
        if (run == null) {
            return;
        }
        synchronized (run) {
            if (run.termine) {
                return;
            }
            run.termine = true;
            run.statut = motif;
            Evenement e = nouvelEvenement(run, "fin", null, null, null, null, motif, null, null, null, null);
            run.evenements.add(e);
            diffuser(run, e);
            for (SseEmitter s : run.abonnes) {
                s.complete();
            }
            run.abonnes.clear();
        }
    }

    public List<Resume> liste() {
        List<Resume> l = new ArrayList<>();
        synchronized (runs) {
            for (Run r : runs.values()) {
                l.add(new Resume(r.runId, r.titre, r.statut, r.debut));
            }
        }
        Collections.reverse(l);
        return l;
    }

    public Optional<List<Evenement>> evenements(String runId) {
        Run run = runs.get(runId);
        if (run == null) {
            return Optional.empty();
        }
        synchronized (run) {
            return Optional.of(List.copyOf(run.evenements));
        }
    }

    /** Flux SSE : rejoue l'historique puis suit en direct. Vide si le runId est inconnu. */
    public Optional<SseEmitter> abonner(String runId) {
        Run run = runs.get(runId);
        if (run == null) {
            return Optional.empty();
        }
        SseEmitter emetteur = new SseEmitter(30 * 60 * 1000L);
        emetteur.onCompletion(() -> run.abonnes.remove(emetteur));
        emetteur.onTimeout(() -> run.abonnes.remove(emetteur));
        synchronized (run) {
            try {
                for (Evenement e : run.evenements) {
                    envoyer(emetteur, e);
                }
            } catch (IOException ex) {
                return Optional.of(emetteur);
            }
            if (run.termine) {
                emetteur.complete();
            } else {
                run.abonnes.add(emetteur);
            }
        }
        return Optional.of(emetteur);
    }

    public static boolean runIdValide(String runId) {
        return runId != null && RUN_ID.matcher(runId).matches();
    }

    private void publier(Run run, String type, String id, List<String> parents, String genre, String libelle, String statut,
                         String detail, String resultat, Long dureeMs, Integer tokens) {
        synchronized (run) {
            Evenement e = nouvelEvenement(run, type, id, parents, genre, libelle, statut, detail, resultat, dureeMs, tokens);
            run.evenements.add(e);
            diffuser(run, e);
        }
    }

    private static Evenement nouvelEvenement(Run run, String type, String id, List<String> parents, String genre, String libelle,
                                             String statut, String detail, String resultat, Long dureeMs, Integer tokens) {
        return new Evenement(run.evenements.size() + 1L, System.currentTimeMillis(), run.runId, type, id, parents, genre,
                libelle, statut, detail, resultat, dureeMs, tokens, null, null, null, null);
    }

    private void diffuser(Run run, Evenement e) {
        for (SseEmitter s : run.abonnes) {
            try {
                envoyer(s, e);
            } catch (IOException | IllegalStateException ex) {
                run.abonnes.remove(s);
            }
        }
    }

    private static void envoyer(SseEmitter s, Evenement e) throws IOException {
        s.send(SseEmitter.event().name("evt").id(String.valueOf(e.seq())).data(e, MediaType.APPLICATION_JSON));
    }
}
