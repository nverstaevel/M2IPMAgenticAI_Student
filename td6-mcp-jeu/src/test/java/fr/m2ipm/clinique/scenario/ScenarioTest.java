package fr.m2ipm.clinique.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class ScenarioTest {

    private static ScenarioDef def(String md) { return ScenarioLoader.lire(md, "test", "test.md"); }

    private static Orchestrateur orchestrateur(SuiviExecution suivi, Map<String, Agent> agents) {
        return new Orchestrateur(agents, suivi, new OutilsDisponibles(Map.of()));
    }

    private static List<String> ids(SuiviExecution suivi, String run) {
        return suivi.evenements(run).orElseThrow().stream().map(Evenement::id).filter(Objects::nonNull).distinct().toList();
    }

    private static final String DUO = """
            # Duo
            Description libre.
            ## Entrées
            - cas (Cas) : 1 = Cas 1 | 2 = Cas 2
            - note : "bonjour"
            ## Agents
            - prod
            - crit
            ## Graphe
            entrée -> prod -> crit
            crit -> prod : REJETE (max 2)
            crit -> humain : APPROUVE, DOUTE, REJETE
            humain -> fin
            """;

    // ---- lecture du fichier ----

    @Test
    void lectureDesTroisSections() {
        var d = def(DUO);
        assertThat(d.erreurs()).isEmpty();
        assertThat(d.id()).isEqualTo("test");
        assertThat(d.titre()).isEqualTo("Duo");
        assertThat(d.description()).isEqualTo("Description libre.");
        assertThat(d.entree()).extracting(ChampEntree::id).containsExactly("cas", "note");
        assertThat(d.entree().get(0).type()).isEqualTo("choix");
        assertThat(d.entree().get(0).defaut()).isEqualTo("1");
        assertThat(d.entree().get(0).options()).extracting(ChampEntree.Option::libelle).containsExactly("Cas 1", "Cas 2");
        assertThat(d.entree().get(1).defaut()).isEqualTo("bonjour");
        assertThat(d.agents()).extracting(ScenarioDef.Noeud::nom).containsExactly("prod", "crit");
        assertThat(d.arcs()).hasSize(5);
        var retour = d.arcs().stream().filter(a -> a.de().equals("crit") && a.vers().equals("prod")).findFirst().orElseThrow();
        assertThat(retour.etiquettes()).containsExactly("REJETE");
        assertThat(retour.max()).isEqualTo(2);
    }

    @Test
    void agentsEtOutilsEtDelegation() {
        var d = def("""
                ## Agents
                - plan
                - dossier = travailleur : outils dossier, chercherDansWikipedia
                ## Graphe
                Entrée -> plan
                plan => dossier
                """);
        assertThat(d.erreurs()).isEmpty();
        var w = d.noeud("dossier").orElseThrow();
        assertThat(w.bean()).isEqualTo("travailleur");
        assertThat(w.outils()).containsExactly("dossier", "chercherDansWikipedia");
        assertThat(d.arcs()).anyMatch(a -> a.delegation() && a.vers().equals("dossier"));
    }

    @Test
    void erreursDeSyntaxeSontSignaleesAvecLeurLigne() {
        assertThat(def("pas de graphe").erreurs()).anyMatch(m -> m.contains("Graphe"));
        assertThat(def("## Graphe\nseul").erreurs()).anyMatch(m -> m.contains("ligne 2"));
        assertThat(def("## Entrées\n???\n## Graphe\na -> b").erreurs()).anyMatch(m -> m.contains("Entrées, ligne 2"));
    }

    // ---- validation ----

    @Test
    void validationSignaleLesIncoherences() {
        var o = orchestrateur(new SuiviExecution(), Map.of("a", e -> Resultat.texte("a")));
        assertThat(o.valider(def("## Agents\n- x = absent\n## Graphe\nentrée -> x -> fin"))).anyMatch(m -> m.contains("absent"));
        assertThat(o.valider(def("## Agents\n- a\n## Graphe\nentrée -> a -> b -> fin"))).anyMatch(m -> m.contains("« b »"));
        assertThat(o.valider(def("## Agents\n- a : outils inexistant\n## Graphe\nentrée -> a -> fin"))).anyMatch(m -> m.contains("inexistant"));
        assertThat(o.valider(def("## Agents\n- a\n## Graphe\nentrée -> a\na -> a : OK"))).isEmpty();
        assertThat(o.valider(def("## Agents\n- a\n- b\n## Graphe\nentrée -> a -> b\nb -> a"))).anyMatch(m -> m.contains("étiquette"));
        assertThat(o.valider(def("## Agents\n- a\n## Graphe\nentrée -> a (max 2)"))).anyMatch(m -> m.contains("revient en arrière"));
        assertThat(o.valider(def("## Agents\n- a\n- b\n## Graphe\nentrée -> a"))).anyMatch(m -> m.contains("absent du graphe"));
        assertThat(o.valider(def("## Agents\n- a\n## Graphe\nentrée -> a -> fin\nentrée -> a : X (max 99)"))).isNotEmpty();
    }

    // ---- exécution ----

    @Test
    void boucleSArreteALApprobation() {
        var suivi = new SuiviExecution();
        AtomicInteger appels = new AtomicInteger();
        Agent prod = e -> Resultat.texte("v" + e.tour() + (e.retours().isEmpty() ? "" : " après " + e.retours().get(0)));
        Agent crit = e -> appels.incrementAndGet() < 2 ? Resultat.avis(Decision.REJETE, List.of("trop court")) : Resultat.avis(Decision.APPROUVE, List.of());
        var o = orchestrateur(suivi, Map.of("prod", prod, "crit", crit));
        var d = def(DUO);
        assertThat(o.valider(d)).isEmpty();
        o.executer("run-b", d, Map.of());
        assertThat(ids(suivi, "run-b")).containsExactly("demande", "prod", "crit", "prod-2", "crit-2", "humain", "fin");
        var ev = suivi.evenements("run-b").orElseThrow();
        assertThat(ev).filteredOn(x -> "prod-2".equals(x.id())).anyMatch(x -> x.parents() != null && x.parents().contains("crit"));
        assertThat(ev).filteredOn(x -> "prod-2".equals(x.id())).anyMatch(x -> x.detail() != null && x.detail().contains("trop court"));
        assertThat(ev).filteredOn(x -> "fin".equals(x.id())).anyMatch(x -> x.libelle() != null && x.libelle().contains("APPROUVEE"));
        assertThat(suivi.termine("run-b")).isTrue();
    }

    @Test
    void plafondDeBoucleEnvoieVersLHumain() {
        var suivi = new SuiviExecution();
        Agent prod = e -> Resultat.texte("v" + e.tour());
        Agent crit = e -> Resultat.avis(Decision.REJETE, List.of("non"));
        var o = orchestrateur(suivi, Map.of("prod", prod, "crit", crit));
        o.executer("run-n", def(DUO), Map.of());
        assertThat(ids(suivi, "run-n")).containsExactly("demande", "prod", "crit", "prod-2", "crit-2", "prod-3", "crit-3", "humain", "fin");
        assertThat(suivi.evenements("run-n").orElseThrow()).filteredOn(x -> "fin".equals(x.id()))
                .anyMatch(x -> x.libelle() != null && x.libelle().contains("NON_CONVERGENTE"));
    }

    @Test
    void sansArcDeSecoursLeMoteurClotLExecution() {
        var suivi = new SuiviExecution();
        Agent crit = e -> Resultat.avis(Decision.REJETE, List.of("non"));
        var o = orchestrateur(suivi, Map.of("a", e -> Resultat.texte("a"), "crit", crit));
        var d = def("## Agents\n- a\n- crit\n## Graphe\nentrée -> a -> crit\ncrit -> a : REJETE (max 1)\ncrit -> fin : APPROUVE");
        assertThat(o.valider(d)).isEmpty();
        o.executer("run-s", d, Map.of());
        assertThat(ids(suivi, "run-s")).containsExactly("demande", "a", "crit", "a-2", "crit-2", "fin");
        assertThat(suivi.evenements("run-s").orElseThrow()).filteredOn(x -> "fin".equals(x.id()))
                .anyMatch(x -> Evenement.ERREUR.equals(x.statut()));
    }

    @Test
    void delegationEtJonction() {
        var suivi = new SuiviExecution();
        Agent plan = e -> Resultat.plan(List.of(new Tache("A", "fais a"), new Tache("B", "fais b")));
        Agent w = e -> Resultat.texte("ok " + e.texte());
        Agent syn = e -> Resultat.texte("synthèse : " + e.proposition().texte());
        var o = orchestrateur(suivi, Map.of("plan", plan, "w", w, "syn", syn));
        var d = def("""
                ## Entrées
                - demande : "go"
                ## Agents
                - plan
                - a = w
                - b = w
                - c = w
                - syn
                ## Graphe
                entrée -> plan
                plan => a, b, c
                a, b, c -> syn -> fin
                """);
        assertThat(o.valider(d)).isEmpty();
        o.executer("run-d", d, Map.of());
        assertThat(ids(suivi, "run-d")).contains("plan", "a", "b", "syn", "fin").doesNotContain("c");
        var syntheseDetail = suivi.evenements("run-d").orElseThrow().stream()
                .filter(x -> "syn".equals(x.id()) && x.resultat() != null).map(Evenement::resultat).findFirst().orElseThrow();
        assertThat(syntheseDetail).contains("A : ok fais a").contains("B : ok fais b").doesNotContain("C :");
    }

    @Test
    void unRouteurChoisitSonArc() {
        var suivi = new SuiviExecution();
        Agent routeur = e -> Resultat.route("c'est technique", "TECHNIQUE");
        Agent t = e -> Resultat.texte("technicien");
        Agent adm = e -> Resultat.texte("administratif");
        var o = orchestrateur(suivi, Map.of("routeur", routeur, "t", t, "adm", adm));
        var d = def("## Agents\n- routeur\n- tech = t\n- admin = adm\n## Graphe\nentrée -> routeur\nrouteur -> tech : TECHNIQUE\nrouteur -> admin : ADMIN\ntech, admin -> fin");
        assertThat(o.valider(d)).isEmpty();
        o.executer("run-r", d, Map.of());
        assertThat(ids(suivi, "run-r")).contains("routeur", "tech", "fin").doesNotContain("admin");
    }

    @Test
    void unAgentNeRecoitQueLesOutilsAutorises() {
        var suivi = new SuiviExecution();
        List<String> vus = new ArrayList<>();
        Agent a = e -> { e.outils().forEach(c -> vus.add(c.getToolDefinition().name())); return Resultat.texte("ok"); };
        var outils = new OutilsDisponibles(Map.of("groupe", new ToolCallback[] {outil("un"), outil("deux")}));
        var o = new Orchestrateur(Map.of("a", a), suivi, outils);
        var d = def("## Agents\n- a : outils deux\n## Graphe\nentrée -> a -> fin");
        assertThat(o.valider(d)).isEmpty();
        o.executer("run-o", d, Map.of());
        assertThat(vus).containsExactly("deux");
        var tous = def("## Agents\n- a : outils groupe\n## Graphe\nentrée -> a -> fin");
        vus.clear();
        o.executer("run-o2", tous, Map.of());
        assertThat(vus).containsExactlyInAnyOrder("un", "deux");
        assertThat(o.valider(def("## Agents\n- a : outils troix\n## Graphe\nentrée -> a -> fin"))).anyMatch(m -> m.contains("troix"));
    }

    // ---- aperçu ----

    @Test
    void apercuDerouleLesBouclesJusquAuPlafond() {
        var o = orchestrateur(new SuiviExecution(), Map.of("prod", e -> Resultat.texte(""), "crit", e -> Resultat.texte("")));
        var ev = o.apercu(def(DUO));
        assertThat(ev).extracting(Evenement::id).containsExactly("demande", "prod", "crit", "prod-2", "crit-2", "prod-3", "crit-3", "humain", "fin");
        assertThat(ev).allMatch(x -> Evenement.PREVU.equals(x.statut()));
    }

    @Test
    void apercuDeLaDelegationMontreTousLesTravailleurs() {
        var o = orchestrateur(new SuiviExecution(), Map.of("p", e -> Resultat.texte(""), "w", e -> Resultat.texte("")));
        var d = def("## Agents\n- plan = p\n- a = w\n- b = w\n## Graphe\nentrée -> plan\nplan => a, b\na, b -> fin");
        assertThat(o.apercu(d)).extracting(Evenement::id).containsExactly("demande", "plan", "a", "b", "fin");
    }

    // ---- outils ----

    @Test
    void plafondDAppelsDOutils() {
        var suivi = new SuiviExecution();
        suivi.demarrer("run-p", "t");
        AtomicInteger reels = new AtomicInteger();
        var outil = new ToolCallback() {
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("x").description("d").inputSchema("{}").build();
            }
            public String call(String entree) { reels.incrementAndGet(); return "ok"; }
        };
        var enveloppe = fr.m2ipm.clinique.agent.SuiviOutils.envelopper(outil)[0];
        try (var p = fr.m2ipm.clinique.agent.SuiviOutils.ouvrir(suivi, "run-p", "n")) {
            for (int i = 0; i < 20; i++) { enveloppe.call("{}"); }
        }
        assertThat(reels.get()).isEqualTo(fr.m2ipm.clinique.agent.SuiviOutils.MAX_APPELS_OUTILS);
    }

    private static ToolCallback outil(String nom) {
        return new ToolCallback() {
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(nom).description("d").inputSchema("{}").build();
            }
            public String call(String entree) { return "ok"; }
        };
    }
}
