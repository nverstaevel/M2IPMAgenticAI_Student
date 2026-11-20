package fr.m2ipm.clinique.scenario;

import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import fr.m2ipm.clinique.agent.SuiviOutils;
import fr.m2ipm.clinique.scenario.ScenarioDef.Arc;
import fr.m2ipm.clinique.scenario.ScenarioDef.Noeud;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Exécute le graphe décrit par un scénario : appelle les agents par leur nom de bean, suit les arcs d'après ce que chaque agent
 * rend, gère les boucles et alimente le suivi (graphe en direct) : un nœud par passage dans un agent, avec ses outils, ses tokens
 * et sa durée. Les agents ne s'occupent de rien de cela.
 *
 * <p>Règles de parcours :
 * <ul>
 *   <li>un arc sans étiquette se déclenche toujours (plusieurs arcs sortants = plusieurs suites) ;</li>
 *   <li>parmi les arcs étiquetés d'un nœud, le premier dont l'étiquette correspond au résultat se déclenche : l'étiquette du
 *       résultat ({@code Resultat.route}), sinon sa décision (APPROUVE, REJETE, DOUTE), sinon DOUTE ;</li>
 *   <li>un arc « => » (délégation) ne se déclenche que pour les agents que le plan du nœud source désigne ;</li>
 *   <li>un nœud attend que tous ses arcs entrants soient résolus (déclenchés ou éteints) : c'est une jonction ;</li>
 *   <li>un arc qui revient vers un nœud déjà parcouru est une boucle : plafonnée par « (max N) », 3 par défaut ; quand le plafond
 *       est atteint, l'arc est ignoré et le suivant (le premier arc étiqueté correspondant) prend le relais.</li>
 * </ul>
 */
@Service
public class Orchestrateur {

    private static final Logger LOG = LoggerFactory.getLogger(Orchestrateur.class);

    static final int MAX_DEFAUT = 3;
    static final int MAX_BOUCLE = 10;
    static final int MAX_EXECUTIONS = 40;

    private final Map<String, Agent> agents;
    private final SuiviExecution suivi;
    private final OutilsDisponibles outils;
    private final ExecutorService executeur = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    public Orchestrateur(Map<String, Agent> agents, SuiviExecution suivi, OutilsDisponibles outils) {
        this.agents = agents;
        this.suivi = suivi;
        this.outils = outils;
    }

    @PreDestroy
    void arreter() { executeur.shutdownNow(); }

    public List<String> agentsDisponibles() { return agents.keySet().stream().sorted().toList(); }

    public OutilsDisponibles outilsDisponibles() { return outils; }

    // ------------------------------------------------------------------ validation

    /** Problèmes de définition, en plus de ceux de la lecture du fichier : agent absent, outil inconnu, graphe incohérent. */
    public List<String> valider(ScenarioDef d) {
        List<String> e = new ArrayList<>(d.erreurs());
        if (d.arcs().isEmpty()) { return e; }
        Set<String> declares = new LinkedHashSet<>();
        for (Noeud n : d.agents()) {
            declares.add(n.nom());
            if (ScenarioDef.reserve(n.nom())) {
                e.add("Agents, ligne " + n.ligne() + " : « " + n.nom() + " » est un nom réservé (entrée, fin, humain).");
            }
            if (!agents.containsKey(n.bean())) {
                e.add("Agents, ligne " + n.ligne() + " : aucun agent Java nommé « " + n.bean() + " » (disponibles : " + String.join(", ", agentsDisponibles()) + ").");
            }
            for (String o : n.outils()) {
                if (!outils.connu(o)) {
                    e.add("Agents, ligne " + n.ligne() + " : outil « " + o + " » inconnu (groupes : " + String.join(", ", outils.groupes())
                            + " ; outils : " + String.join(", ", outils.outils()) + ").");
                }
            }
        }
        Set<String> signales = new HashSet<>();
        boolean inconnu = false;
        for (Arc a : d.arcs()) {
            for (String nom : List.of(a.de(), a.vers())) {
                if (!ScenarioDef.reserve(nom) && !declares.contains(nom) && signales.add(nom)) {
                    inconnu = true;
                    e.add("Graphe, ligne " + a.ligne() + " : « " + nom + " » n'est pas déclaré dans la section « Agents » (déclarés : " + String.join(", ", declares) + ").");
                }
            }
            if (a.vers().equals(ScenarioDef.ENTREE)) { e.add("Graphe, ligne " + a.ligne() + " : aucun arc ne peut mener à « entrée »."); }
            if (a.de().equals(ScenarioDef.FIN)) { e.add("Graphe, ligne " + a.ligne() + " : « fin » n'a pas de suite."); }
        }
        if (inconnu || d.arcs().stream().anyMatch(a -> a.vers().equals(ScenarioDef.ENTREE))) { return e; }
        Graphe g = Graphe.de(d);
        if (g.sortants(ScenarioDef.ENTREE).isEmpty()) { e.add("Graphe : aucun arc ne part de « entrée »."); }
        for (Arc a : d.arcs()) {
            if (g.estRetour(a)) {
                if (!a.etiquete()) {
                    e.add("Graphe, ligne " + a.ligne() + " : « " + a.de() + " -> " + a.vers() + " » revient en arrière : donnez-lui une étiquette (par exemple « : REJETE ») et un plafond « (max N) ».");
                }
                if (a.max() == 0 || a.max() > MAX_BOUCLE) {
                    e.add("Graphe, ligne " + a.ligne() + " : le plafond « (max N) » d'une boucle est compris entre 1 et " + MAX_BOUCLE + ".");
                }
            } else if (a.max() >= 0) {
                e.add("Graphe, ligne " + a.ligne() + " : « (max N) » n'a de sens que sur un arc qui revient en arrière.");
            }
        }
        Set<String> accessibles = g.accessibles();
        for (String n : g.noeuds()) {
            if (!accessibles.contains(n)) { e.add("Graphe : « " + n + " » n'est atteint par aucun chemin depuis « entrée »."); }
        }
        for (Noeud n : d.agents()) {
            if (!g.noeuds().contains(n.nom())) { e.add("Agents, ligne " + n.ligne() + " : « " + n.nom() + " » est déclaré mais absent du graphe."); }
        }
        return e;
    }

    /** Résumé affiché dans la liste des scénarios : « 4 agents · 1 boucle ». */
    public String forme(ScenarioDef d) {
        if (d.arcs().isEmpty()) { return ""; }
        long boucles = Graphe.de(d).retours().size();
        return d.agents().size() + (d.agents().size() > 1 ? " agents" : " agent") + (boucles > 0 ? " · " + boucles + (boucles > 1 ? " boucles" : " boucle") : "");
    }

    // ------------------------------------------------------------------ aperçu

    /** Graphe prévu, sans rien exécuter : les boucles sont déroulées jusqu'à leur plafond, les délégations montrent tous leurs travailleurs. */
    public List<Evenement> apercu(ScenarioDef d) {
        List<Evenement> ev = new ArrayList<>();
        new Parcours(d, null, null, Map.of(), ev).parcourir();
        return ev;
    }

    // ------------------------------------------------------------------ lancement

    /** Lance l'exécution en arrière-plan et renvoie tout de suite le runId, pour que la page suive le graphe en direct. */
    public String lancer(ScenarioDef d, Map<String, String> saisie) {
        List<String> erreurs = valider(d);
        if (!erreurs.isEmpty()) {
            throw new ScenarioException("Scénario invalide : " + String.join(" ; ", erreurs));
        }
        String runId = UUID.randomUUID().toString();
        suivi.demarrer(runId, d.titre());
        executeur.submit(() -> {
            try {
                executer(runId, d, saisie);
            } catch (RuntimeException e) {
                LOG.info("Scénario {} terminé en erreur : {}", d.id(), e.getMessage());
            }
        });
        return runId;
    }

    /** Exécution synchrone (utilisée par {@link #lancer} et par les tests). Les erreurs sont visibles dans le graphe, puis relancées. */
    public void executer(String runId, ScenarioDef d, Map<String, String> saisie) {
        suivi.demarrer(runId, d.titre());
        Parcours p = new Parcours(d, runId, Graphe.de(d), completer(d, saisie), null);
        try {
            p.parcourir();
        } catch (RuntimeException e) {
            suivi.noeud(runId, "fin", List.of(p.dernierExecute()), Evenement.FIN, "Erreur", Evenement.ERREUR, String.valueOf(e.getMessage()), null, null, null);
            suivi.terminer(runId, "ERREUR");
            throw e;
        }
    }

    // ------------------------------------------------------------------ le parcours du graphe

    private enum Etat { ACTIF, MORT }

    private enum Pret { OUI, NON, MORT }

    /** L'état d'une exécution (ou d'un aperçu) : quels arcs sont déclenchés, éteints ou en attente, et ce que chaque agent a rendu. */
    private final class Parcours {

        private final ScenarioDef d;
        private final String runId;
        private final Graphe g;
        private final boolean apercu;
        private final List<Evenement> evenements;
        private final Map<String, String> donnees;
        private final String demande;
        private final Map<String, Noeud> defs = new LinkedHashMap<>();
        private final Map<Arc, Etat> etat = new HashMap<>();
        private final Map<Arc, Integer> utilises = new HashMap<>();
        private final Map<String, Integer> passages = new HashMap<>();
        private final Map<String, Resultat> dernier = new HashMap<>();
        private final Map<String, String> dernierId = new HashMap<>();
        private final Map<String, List<String>> retours = new HashMap<>();
        private final Map<String, String> sourceRetour = new HashMap<>();
        private final Map<String, List<String>> consignes = new HashMap<>();
        private final Map<String, String> delegueDepuis = new HashMap<>();
        private final Set<String> faits = new HashSet<>();
        private String motif;
        private boolean finAtteint;
        private boolean epuise;
        private int total;
        private String dernierExecute = "demande";

        Parcours(ScenarioDef d, String runId, Graphe g, Map<String, String> donnees, List<Evenement> evenements) {
            this.d = d;
            this.runId = runId;
            this.apercu = evenements != null;
            this.g = g == null ? Graphe.de(d) : g;
            this.evenements = evenements;
            this.donnees = donnees;
            this.demande = donnees.getOrDefault("demande", "");
            d.agents().forEach(n -> defs.put(n.nom(), n));
        }

        String dernierExecute() { return dernierExecute; }

        void parcourir() {
            emettre("demande", List.of(), Evenement.UTILISATEUR, "Entrée", Evenement.OK,
                    apercu ? champsEnTexte(d) : (demande.isBlank() ? champsSaisis(donnees) : demande), null);
            Resultat depart = Resultat.texte(demande);
            dernier.put(ScenarioDef.ENTREE, depart);
            dernierId.put(ScenarioDef.ENTREE, "demande");
            faits.add(ScenarioDef.ENTREE);
            propager(ScenarioDef.ENTREE, depart);
            boolean progres = true;
            while (progres) {
                progres = false;
                for (String n : g.noeuds()) {
                    if (faits.contains(n)) { continue; }
                    Pret p = pret(n);
                    if (p == Pret.OUI) {
                        executer(n);
                        progres = true;
                    } else if (p == Pret.MORT) {
                        faits.add(n);
                        g.sortants(n).forEach(a -> etat.put(a, Etat.MORT));
                        progres = true;
                    }
                }
            }
            cloturer();
        }

        /** Un nœud est prêt quand tous ses arcs entrants « vers l'avant » sont résolus et qu'au moins un est déclenché. */
        private Pret pret(String n) {
            List<Arc> entrants = g.entrantsAvant(n);
            if (entrants.isEmpty()) { return Pret.NON; }
            boolean actif = false;
            for (Arc a : entrants) {
                Etat s = etat.get(a);
                if (s == null) { return Pret.NON; }
                if (s == Etat.ACTIF) { actif = true; }
            }
            return actif ? Pret.OUI : Pret.MORT;
        }

        private void executer(String n) {
            if (++total > MAX_EXECUTIONS) {
                throw new ScenarioException("Plus de " + MAX_EXECUTIONS + " passages dans des agents : une boucle ne s'arrête pas (vérifiez les plafonds « (max N) »).");
            }
            int num = passages.merge(n, 1, Integer::sum);
            String id = n.equals(ScenarioDef.FIN) ? "fin" : (num == 1 ? n : n + "-" + num);
            List<Arc> actifs = g.entrantsAvant(n).stream().filter(a -> etat.get(a) == Etat.ACTIF).toList();
            String reprise = sourceRetour.remove(n);
            List<String> parents = reprise != null ? List.of(reprise)
                    : actifs.stream().map(a -> dernierId.get(a.de())).distinct().toList();
            if (n.equals(ScenarioDef.FIN)) {
                finAtteint = true;
                finir(parents, actifs);
                faits.add(n);
                return;
            }
            String texte;
            Resultat proposition;
            List<String> cons = consignes.remove(n);
            if (cons != null) {
                texte = cons.size() == 1 ? cons.get(0) : "- " + String.join("\n- ", cons);
                proposition = dernier.get(delegueDepuis.get(n));
            } else if (g.entrantsAvant(n).size() >= 2) {
                StringBuilder sb = new StringBuilder();
                for (String src : actifs.stream().map(Arc::de).distinct().toList()) {
                    Resultat r = dernier.get(src);
                    sb.append(src.toUpperCase(Locale.ROOT)).append(" : ").append(r == null ? "" : r.texte()).append('\n');
                }
                texte = demande;
                proposition = Resultat.texte(sb.toString());
            } else {
                String src = actifs.get(0).de();
                texte = src.equals(ScenarioDef.ENTREE) ? demande : dernier.get(src).texte();
                proposition = src.equals(ScenarioDef.ENTREE) ? null : dernier.get(src);
            }
            Resultat r;
            if (n.equals(ScenarioDef.HUMAIN)) {
                emettre(id, parents, Evenement.HUMAIN, "Validation humaine", Evenement.ATTENTE, "Aucune décision n'est appliquée sans le personnel.", null);
                r = proposition == null ? Resultat.texte(texte) : proposition;
            } else {
                Noeud def = defs.get(n);
                String libelle = n + (g.dansBoucle(n) ? " · tour " + num : "") + (def.bean().equals(n) ? "" : " (" + def.bean() + ")");
                String genre = genre(n);
                if (apercu) {
                    emettre(id, parents, genre, libelle, Evenement.PREVU, detailApercu(n, def), null);
                    r = Resultat.texte("");
                } else {
                    r = appeler(runId, id, parents, genre, libelle, def,
                            new Entree(runId, n, id, texte, donnees, retours.getOrDefault(n, List.of()), num, proposition, List.of(), null));
                }
            }
            dernier.put(n, r);
            dernierId.put(n, id);
            dernierExecute = id;
            faits.add(n);
            // Un agent qui s'arrête pour un motif particulier (ex. PLAFOND_ETAPES) et n'a aucun arc étiqueté : le motif devient celui de l'exécution.
            if (r.etiquette() != null && !apercu && g.sortants(n).stream().noneMatch(Arc::etiquete)) { motif = r.etiquette(); }
            propager(n, r);
        }

        /** Décide quels arcs sortants de {@code src} se déclenchent, et reprend la boucle si un arc retour est choisi. */
        private void propager(String src, Resultat r) {
            List<Arc> sortants = g.sortants(src);
            List<Tache> restantes = new ArrayList<>(r.taches());
            boolean delegue = false;
            for (Arc a : sortants) {
                if (!a.delegation()) { continue; }
                delegue = true;
                if (apercu) { etat.put(a, Etat.ACTIF); continue; }
                List<String> cs = new ArrayList<>();
                for (Iterator<Tache> it = restantes.iterator(); it.hasNext();) {
                    Tache t = it.next();
                    if (ScenarioLoader.norm(t.travailleur()).equals(a.vers())) { cs.add(t.consigne()); it.remove(); }
                }
                if (cs.isEmpty()) {
                    etat.put(a, Etat.MORT);
                } else {
                    etat.put(a, Etat.ACTIF);
                    consignes.put(a.vers(), cs);
                    delegueDepuis.put(a.vers(), src);
                }
            }
            if (delegue && !apercu) {
                int k = 0;
                for (Tache t : restantes) {
                    String id = dernierId.get(src) + "-inconnu" + (++k);
                    suivi.noeud(runId, id, List.of(dernierId.get(src)), Evenement.TRAVAILLEUR, String.valueOf(t.travailleur()), Evenement.ERREUR,
                            "Le plan demande un agent inconnu : « " + t.travailleur() + " » (la délégation « => » liste : "
                                    + String.join(", ", sortants.stream().filter(Arc::delegation).map(Arc::vers).toList()) + ").", null, null, null);
                }
            }
            for (Arc a : sortants) {
                if (!a.delegation() && !a.etiquete()) { etat.put(a, Etat.ACTIF); }
            }
            List<Arc> etiquetes = sortants.stream().filter(a -> !a.delegation() && a.etiquete()).toList();
            if (etiquetes.isEmpty()) { return; }
            Arc choisi = null;
            String etiquette = "";
            if (apercu) {
                for (Arc a : etiquetes) {
                    if (g.estRetour(a) && utilises.getOrDefault(a, 0) < limite(a)) { choisi = a; break; }
                }
                if (choisi == null) {
                    etiquetes.forEach(a -> etat.put(a, Etat.ACTIF));
                    return;
                }
            } else {
                Set<String> labels = etiquettes(r);
                for (Arc a : etiquetes) {
                    String commune = a.etiquettes().stream().filter(labels::contains).findFirst().orElse(null);
                    if (commune == null) { continue; }
                    if (g.estRetour(a) && utilises.getOrDefault(a, 0) >= limite(a)) { epuise = true; continue; }
                    choisi = a;
                    etiquette = commune;
                    break;
                }
            }
            for (Arc a : etiquetes) {
                if (!a.equals(choisi)) { etat.put(a, Etat.MORT); }
            }
            if (choisi == null) { return; }
            if (!apercu) { motif = versMotif(etiquette); }
            if (g.estRetour(choisi)) {
                utilises.merge(choisi, 1, Integer::sum);
                for (String n : g.corps(choisi)) {
                    faits.remove(n);
                    g.sortants(n).forEach(etat::remove);
                    if (!apercu) { retours.put(n, r.remarques()); }
                }
                sourceRetour.put(choisi.vers(), dernierId.get(src));
            } else {
                etat.put(choisi, Etat.ACTIF);
            }
        }

        private int limite(Arc a) { return a.max() > 0 ? a.max() : MAX_DEFAUT; }

        private Set<String> etiquettes(Resultat r) {
            if (r.etiquette() != null) { return Set.of(r.etiquette().toUpperCase(Locale.ROOT)); }
            if (r.decision() != null) { return Set.of(r.decision().name()); }
            return Set.of(Decision.DOUTE.name());
        }

        private String versMotif(String etiquette) {
            return switch (etiquette) {
                case "APPROUVE" -> "APPROUVEE";
                case "DOUTE" -> "VERS_HUMAIN";
                case "REJETE" -> "NON_CONVERGENTE";
                default -> etiquette;
            };
        }

        /** CRITIQUE si les arcs sortants parlent de décision, TRAVAILLEUR si le nœud est une cible de délégation, AGENT sinon. */
        private String genre(String n) {
            if (g.entrants(n).stream().anyMatch(Arc::delegation)) { return Evenement.TRAVAILLEUR; }
            boolean critique = g.sortants(n).stream().flatMap(a -> a.etiquettes().stream())
                    .anyMatch(e -> e.equals("APPROUVE") || e.equals("REJETE") || e.equals("DOUTE"));
            return critique ? Evenement.CRITIQUE : Evenement.AGENT;
        }

        private String detailApercu(String n, Noeud def) {
            StringBuilder sb = new StringBuilder("Agent : ").append(def.bean());
            sb.append("\nOutils autorisés : ").append(def.outils().isEmpty() ? "aucun" : String.join(", ", def.outils()));
            for (Arc a : g.sortants(n)) {
                if (g.estRetour(a)) {
                    sb.append("\nReprend « ").append(a.vers()).append(" » si ").append(String.join(" ou ", a.etiquettes()))
                            .append(" (au plus ").append(limite(a)).append(" fois).");
                } else if (a.delegation()) {
                    sb.append("\nPeut déléguer à « ").append(a.vers()).append(" » (selon son plan).");
                }
            }
            return sb.toString();
        }

        /** Nœud final : le motif vient de l'étiquette qui a mené jusqu'ici (APPROUVE → APPROUVEE, DOUTE → VERS_HUMAIN…). */
        private void finir(List<String> parents, List<Arc> actifs) {
            if (apercu) {
                emettre("fin", parents, Evenement.FIN, "Résultat", Evenement.PREVU, null, null);
                return;
            }
            String m = motif == null ? "REPONSE_FINALE" : motif;
            String texte = actifs.stream().map(a -> dernier.get(a.de())).filter(x -> x != null).map(Resultat::texte).reduce((x, y) -> y).orElse("");
            boolean ok = m.equals("REPONSE_FINALE") || m.equals("APPROUVEE");
            suivi.noeud(runId, "fin", parents, Evenement.FIN, m.equals("REPONSE_FINALE") ? "Réponse finale" : "Résultat : " + m,
                    ok ? Evenement.OK : Evenement.ERREUR, total + " passage(s) dans des agents", texte, null, null);
            suivi.terminer(runId, m);
            dernierExecute = "fin";
        }

        /** Si aucun arc n'a mené à « fin » (plafond de boucle atteint, branche sans suite), le moteur clôt l'exécution. */
        private void cloturer() {
            if (finAtteint) { return; }
            if (apercu) {
                if (!d.arcs().stream().anyMatch(a -> a.vers().equals(ScenarioDef.FIN))) {
                    emettre("fin", List.of(dernierExecute), Evenement.FIN, "Résultat", Evenement.PREVU, null, null);
                }
                return;
            }
            String m = motif != null ? motif : (epuise ? "NON_CONVERGENTE" : "SANS_SUITE");
            suivi.noeud(runId, "fin", List.of(dernierExecute), Evenement.FIN, "Résultat : " + m, Evenement.ERREUR,
                    "Aucun arc ne mène à « fin » : " + (epuise ? "le plafond d'une boucle est atteint." : "aucune étiquette ne correspond au résultat du dernier agent."),
                    null, null, null);
            suivi.terminer(runId, m);
        }

        private void emettre(String id, List<String> parents, String genre, String libelle, String statut, String detail, String resultat) {
            if (apercu) {
                evenements.add(new Evenement(evenements.size() + 1L, 0L, "apercu", "noeud", id, parents, genre, libelle,
                        Evenement.PREVU, detail, null, null, null, null, null, null, null));
            } else {
                suivi.noeud(runId, id, parents, genre, libelle, statut, detail, resultat, null, null);
            }
        }
    }

    /** Un appel d'agent = un nœud : EN_COURS, puis OK/ERREUR avec durée et résultat. Tokens et outils sont ajoutés par le suivi. */
    private Resultat appeler(String runId, String id, List<String> parents, String genre, String libelle, Noeud def, Entree e) {
        ToolCallback[] autorises = SuiviOutils.envelopper(outils.resoudre(def.outils()));
        String detail = "Agent : " + def.bean() + "\nOutils autorisés : " + (def.outils().isEmpty() ? "aucun" : String.join(", ", def.outils()))
                + (e.retours().isEmpty() ? "" : "\nRemarques à prendre en compte :\n- " + String.join("\n- ", e.retours()));
        suivi.noeud(runId, id, parents, genre, libelle, Evenement.EN_COURS, detail, null, null, null);
        long debut = System.nanoTime();
        Entree avec = e.avecTraceEtOutils(new TraceNoeud(suivi, runId, id), List.of(autorises));
        Resultat r;
        try (var p = SuiviOutils.ouvrir(suivi, runId, id)) {
            r = SuiviOutils.reessayer(suivi, runId, id, () -> agent(def.bean()).executer(avec));
        } catch (RuntimeException ex) {
            suivi.maj(runId, id, Evenement.ERREUR, null, ex.getClass().getSimpleName() + " : " + ex.getMessage(), ms(debut), null);
            throw ex;
        }
        if (r == null) { r = Resultat.texte(""); }
        boolean refus = r.decision() != null && r.decision() != Decision.APPROUVE;
        suivi.maj(runId, id, refus ? Evenement.ERREUR : Evenement.OK, null, r.texte(), ms(debut), null);
        return r;
    }

    private Agent agent(String nom) {
        Agent a = agents.get(nom);
        if (a == null) { throw new ScenarioException("Aucun agent nommé « " + nom + " »."); }
        return a;
    }

    // ------------------------------------------------------------------ utilitaires

    private static Map<String, String> completer(ScenarioDef d, Map<String, String> saisie) {
        Map<String, String> m = new LinkedHashMap<>();
        for (ChampEntree c : d.entree()) {
            String v = saisie == null ? null : saisie.get(c.id());
            m.put(c.id(), v == null || v.isBlank() ? c.defaut() : v.trim());
        }
        if (saisie != null) { saisie.forEach((k, v) -> m.putIfAbsent(k, v)); }
        return m;
    }

    private static String champsEnTexte(ScenarioDef d) {
        return d.entree().isEmpty() ? null : d.entree().stream().map(c -> c.libelle() + " : " + (c.defaut().isBlank() ? "(à saisir)" : c.defaut()))
                .reduce((a, b) -> a + "\n" + b).orElse(null);
    }

    private static String champsSaisis(Map<String, String> donnees) {
        return donnees.entrySet().stream().map(en -> en.getKey() + " : " + en.getValue()).reduce((a, b) -> a + "\n" + b).orElse("(aucune saisie)");
    }

    private static long ms(long debutNanos) { return (System.nanoTime() - debutNanos) / 1_000_000; }
}
