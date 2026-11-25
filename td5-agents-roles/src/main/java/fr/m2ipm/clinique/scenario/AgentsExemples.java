package fr.m2ipm.clinique.scenario;

import fr.m2ipm.clinique.agent.AgentReactService;
import fr.m2ipm.clinique.agent.ComptageJetons;
import fr.m2ipm.clinique.agent.MotifArret;
import fr.m2ipm.clinique.agent.SuiviOutils;
import fr.m2ipm.clinique.roles.*;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agents d'exemple, référencés par les scénarios fournis. Chaque bean est un {@link Agent} : c'est le modèle à imiter pour écrire
 * les vôtres (voir SCENARIOS.md). Règles à retenir :
 * <ul>
 *   <li>les outils d'un agent sont ceux que le scénario .md lui autorise : ils arrivent dans {@code entree.outils()} et se passent
 *       au client avec {@code .toolCallbacks(entree.outilsAutorises())} ; ne câblez pas d'outils en dur dans un agent qui doit être testable ;</li>
 *   <li>un client ChatClient construit avec {@code ComptageJetons.advisor()} voit ses appels et ses tokens comptés dans le graphe ;</li>
 *   <li>les outils reçus apparaissent comme nœuds enfants de l'agent, sans rien faire.</li>
 * </ul>
 *
 * 🧠 À COMPLÉTER dans ce fichier : 1 zone marquée « TODO » (TD5). Tout le reste est fourni et testé : lisez-le, ne le modifiez pas.
 */
@Configuration
public class AgentsExemples {

    // ---- le duo vétérinaire / gardien du TD5 -------------------------------------------------------------------------

    @Bean("veterinaire")
    Agent veterinaire(VeterinaireAgent veterinaire, CasBestiaireRepository cas) {
        return e -> {
            CasBestiaire c = cas(cas, e);
            ProfilActeur profil = ProfilActeur.valueOf(e.donnee("profil", "PRUDENT"));
            Diagnostic d = veterinaire.proposer(c, profil, e.retours(), e.outils());
            return Resultat.de("Diagnostic : " + d.diagnostic() + "\nTraitements : " + String.join(" ; ", d.traitements())
                    + "\nJustification : " + d.justification(), d);
        };
    }

    @Bean("gardien")
    Agent gardien(GardienDuBestiaire gardien, CasBestiaireRepository cas) {
        return e -> {
            CasBestiaire c = cas(cas, e);
            Diagnostic d = (Diagnostic) e.proposition().valeur();
            Critique critique = gardien.examiner(c.espece(), d, e.outils());
            Decision decision = critique.verdict() == null ? Decision.DOUTE : Decision.valueOf(critique.verdict().name());
            return Resultat.avis(decision, critique.objections()).avecValeur(critique);
        };
    }

    private static CasBestiaire cas(CasBestiaireRepository cas, Entree e) {
        int id = Integer.parseInt(e.donnee("cas", "1"));
        return cas.parId(id).orElseThrow(() -> new IllegalArgumentException("Cas inconnu : " + id));
    }

    // ---- l'agent ReAct du TD4 : il possède la boucle, le scénario lui donne le droit d'utiliser des outils --------------

    @Bean("react")
    Agent react(AgentReactService service) {
        return e -> {
            int max = Integer.parseInt(e.donnee("maxEtapes", "8"));
            var res = service.executerSous(e.runId(), e.id(), e.texte(), max, SuiviOutils.sansSuivi(e.outilsAutorises()));
            Resultat r = Resultat.texte(res.reponse());
            return res.motif() == MotifArret.REPONSE_FINALE ? r : r.avecEtiquette(res.motif().name());
        };
    }

    // ---- planificateur, travailleurs, synthèse : le superviseur du TD5 -------------------------------------------------

    /**
     * 🧠 À ÉCRIRE : le planificateur. Il reçoit la demande (e.texte()) et rend un plan : une liste ordonnée de sous-tâches,
     * chacune avec un travailleur (DOSSIER, PHARMACIE ou AGENDA) et une consigne autonome.
     *   1. construisez un ChatClient : builder.clone().defaultAdvisors(ComptageJetons.advisor()).defaultSystem(…).build() ;
     *   2. le prompt système est votre travail de conception : qui sont les trois travailleurs, ce que chacun sait faire,
     *      ce que le planificateur ne doit PAS faire (agir lui-même) ;
     *   3. l'agent renvoie : Plan plan = client.prompt().user(e.texte()).call().entity(Plan.class);
     *      puis Resultat.plan(plan.sousTaches().stream().map(t -> new Tache(t.travailleur().name(), t.consigne())).toList()).
     */
    @Bean("planificateur")
    Agent planificateur(ChatClient.Builder builder) {
        // TODO TD5 : à écrire (remplacez ce corps ; tant qu'il n'est pas écrit, seul le scénario superviseur échoue)
        return e -> { throw new UnsupportedOperationException("À écrire : planificateur"); };
    }

    /** Un travailleur générique : ses outils sont ceux que le .md lui autorise (« dossier = travailleur : outils dossier »). */
    @Bean("travailleur")
    Agent travailleur(ChatClient.Builder builder) {
        ChatClient client = builder.clone().defaultAdvisors(ComptageJetons.advisor()).build();
        return e -> Resultat.texte(client.prompt()
                .system("Tu es le travailleur « " + e.noeud() + " » de la clinique. Tu réponds avec tes outils, sans rien inventer ; "
                        + "si tu n'as pas l'outil qu'il faut, dis-le. Tu ne fais que PROPOSER une consultation ou un événement.")
                .user(e.texte())
                .toolCallbacks(e.outilsAutorises())
                .toolContext(Map.of("conversationId", e.runId(), "roles", "ROLE_PERSONNEL"))
                .call().content());
    }

    @Bean("synthese")
    Agent synthese(ChatClient.Builder builder) {
        ChatClient client = builder.clone().defaultAdvisors(ComptageJetons.advisor()).build();
        return e -> Resultat.texte(client.prompt().user("Demande : " + e.donnee("demande", e.texte()) + "\nRéponses des travailleurs :\n"
                + e.proposition().texte() + "Rédige la réponse finale pour l'utilisateur, sans inventer d'information.").call().content());
    }
}
