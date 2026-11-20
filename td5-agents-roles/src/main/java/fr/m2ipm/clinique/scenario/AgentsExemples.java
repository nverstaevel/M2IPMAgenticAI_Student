package fr.m2ipm.clinique.scenario;

import fr.m2ipm.clinique.agent.AgentReactService;
import fr.m2ipm.clinique.agent.MotifArret;
import fr.m2ipm.clinique.agent.SuiviOutils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agents d'exemple : ici, seulement l'agent react (votre agent du TD4). À vous d'écrire les agents de vos rôles. Chaque bean est un {@link Agent} : c'est le modèle à imiter pour écrire
 * les vôtres (voir SCENARIOS.md). Règles à retenir :
 * <ul>
 *   <li>les outils d'un agent sont ceux que le scénario .md lui autorise : ils arrivent dans {@code entree.outils()} et se passent
 *       au client avec {@code .toolCallbacks(entree.outilsAutorises())} ; ne câblez pas d'outils en dur dans un agent qui doit être testable ;</li>
 *   <li>un client ChatClient construit avec {@code ComptageJetons.advisor()} voit ses appels et ses tokens comptés dans le graphe ;</li>
 *   <li>les outils reçus apparaissent comme nœuds enfants de l'agent, sans rien faire.</li>
 * </ul>
 */
@Configuration
public class AgentsExemples {

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
}
