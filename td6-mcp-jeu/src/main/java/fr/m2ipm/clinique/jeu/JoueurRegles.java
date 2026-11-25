package fr.m2ipm.clinique.jeu;

import fr.m2ipm.clinique.agent.ArretUrgence;
import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un joueur SANS modèle : il applique des règles fixes, avec les mêmes outils MCP que votre agent. Il sert de référence (un
 * agent qui ne fait pas mieux que ces règles ne sert à rien) et de démonstration quand aucun LLM n'est disponible. Chaque appel
 * d'outil apparaît dans le suivi comme pour un agent, avec la règle appliquée dans le champ « détail ».
 *
 * <p>Règles : les patients les plus fragiles d'abord ; examiner (le signe distinctif départage les deux maladies) ; lire la fiche
 * de l'espèce ; soigner avec le remède de la maladie reconnue ; si la pharmacie n'en a plus, en commander et passer au suivant.
 */
@Component
public class JoueurRegles {

    public static final String ID = "regles";

    private final JeuMcp jeu;
    private final SuiviExecution suivi;
    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public JoueurRegles(JeuMcp jeu, SuiviExecution suivi, @Value("${jeu.url:http://localhost:8090}") String urlJeu) {
        this.jeu = jeu;
        this.suivi = suivi;
        this.http = RestClient.builder().baseUrl(urlJeu).build();
    }

    public JoueurService.Bilan jouer(Long graine, JoueurService.Ecoute ecoute) {
        if (!jeu.connecte()) {
            throw new IllegalStateException("Le jeu n'est pas connecté : démarrez jeu-bestiaire puis lancez la clinique avec le profil « jeu ».");
        }
        long debut = System.currentTimeMillis();
        http.post().uri(graine == null ? "/api/nouvelle-partie" : "/api/nouvelle-partie?graine=" + graine).retrieve().toBodilessEntity();
        List<JoueurService.JourJoue> jours = new ArrayList<>();
        for (int i = 0; i < JoueurService.JOURNEES_MAX; i++) {
            JeuMcp.Etat avant = jeu.etat();
            if (avant.terminee() || ecoute.arretDemande()) { break; }
            String runId = UUID.randomUUID().toString();
            suivi.demarrer(runId, "Règles (sans modèle) · jour " + avant.jour());
            ecoute.jourDemarre(avant.jour(), runId);
            boolean arrete = false;
            try {
                journee(runId, ecoute);
                suivi.terminer(runId, "REPONSE_FINALE");
            } catch (RuntimeException e) {
                arrete = ArretUrgence.demande(runId);
                suivi.noeud(runId, "erreur", List.of("debut"), Evenement.FIN, arrete ? "Arrêt d'urgence" : "Erreur", Evenement.ERREUR,
                        arrete ? "Arrêt demandé par l'utilisateur" : String.valueOf(e.getMessage()), null, null, null);
                suivi.terminer(runId, arrete ? "ARRETE" : "ERREUR");
            }
            if (arrete) {
                Thread.interrupted();   // l'interruption a rempli son rôle : on peut de nouveau parler au jeu
                JoueurService.JourJoue stop = new JoueurService.JourJoue(avant.jour(), runId, false, jeu.etat().score());
                jours.add(stop);
                ecoute.jourTermine(stop);
                ArretUrgence.oublier(runId);
                break;
            }
            JeuMcp.Etat apres = jeu.etat();
            boolean forcee = !apres.terminee() && apres.jour() == avant.jour();
            if (forcee) { jeu.appeler("fin_de_journee", "{}"); }
            JoueurService.JourJoue joue = new JoueurService.JourJoue(avant.jour(), runId, forcee, jeu.etat().score());
            jours.add(joue);
            ecoute.jourTermine(joue);
            JoueurService.pause(3 * ecoute.pauseMs());
        }
        JeuMcp.Etat fin = jeu.etat();
        return new JoueurService.Bilan(ID, graine, fin.score(), fin.gueris(), fin.morts(), fin.tresorerie(), jours,
                System.currentTimeMillis() - debut);
    }

    // ------------------------------------------------------------------ une journée

    private void journee(String runId, JoueurService.Ecoute ecoute) {
        Journee j = new Journee(runId, ecoute);
        suivi.noeud(runId, "debut", List.of(), Evenement.UTILISATEUR, "Joueur à règles (sans modèle)",
                Evenement.OK, "Règles : les plus fragiles d'abord ; examiner, lire la fiche, soigner avec le bon remède ; "
                        + "commander si la pharmacie est vide ; terminer la journée.", null, null, null);

        Map<?, ?> etat = (Map<?, ?>) j.outil("Je commence par l'état de la clinique : combien de points d'action aujourd'hui ?", "etat_clinique", "{}");
        int points = entier(etat.get("pointsAction"));
        List<?> pharmacie = (List<?>) j.outil("Quels médicaments ai-je en stock ?", "consulter_pharmacie", "{}");
        Map<String, Integer> stock = new HashMap<>();
        for (Object l : pharmacie) { stock.put(String.valueOf(((Map<?, ?>) l).get("medicament")), entier(((Map<?, ?>) l).get("stock"))); }
        List<Map<?, ?>> patients = new ArrayList<>();
        for (Object o : (List<?>) j.outil("Qui attend dans la salle ?", "lister_patients", "{}")) { patients.add((Map<?, ?>) o); }
        patients.sort(Comparator.comparingInt(p -> entier(p.get("pv"))));
        Map<String, Map<?, ?>> fiches = new HashMap<>();

        for (Map<?, ?> p : patients) {
            String id = String.valueOf(p.get("id"));
            String espece = String.valueOf(p.get("espece"));
            if (points < 2) {
                j.note("Plus assez de points", "Il me reste " + points + " point(s) : examiner puis soigner en coûte 2. Je ne tente pas de deviner "
                        + "sans examen. " + id + " attendra demain.");
                continue;
            }
            j.outil("Je commence par le plus fragile (" + p.get("pv") + " PV). Examiner révèle le signe distinctif qui départage "
                    + "les deux maladies possibles.", "examiner_patient", "{\"idPatient\":\"" + id + "\"}");
            points--;
            String signe = null;
            for (Object o : (List<?>) j.outil("Je relis le dossier pour lire le signe distinctif de " + id + ".", "lister_patients", "{}")) {
                Map<?, ?> m = (Map<?, ?>) o;
                if (id.equals(String.valueOf(m.get("id"))) && m.get("signeDistinctif") != null) { signe = String.valueOf(m.get("signeDistinctif")); }
            }
            Map<?, ?> fiche = fiches.get(espece);
            if (fiche == null) {
                fiche = (Map<?, ?>) j.outil("Je lis la fiche de l'espèce « " + espece + " » : maladies, signes, remèdes.", "fiche_espece",
                        "{\"espece\":\"" + espece + "\"}");
                fiches.put(espece, fiche);
            }
            String remede = null;
            String maladie = null;
            for (Object o : (List<?>) fiche.get("maladies")) {
                Map<?, ?> m = (Map<?, ?>) o;
                if (signe != null && signe.equals(String.valueOf(m.get("signeDistinctif")))) {
                    remede = String.valueOf(m.get("remede"));
                    maladie = String.valueOf(m.get("nom"));
                }
            }
            if (remede == null) {
                j.note("Maladie non reconnue", id + " : le signe « " + signe + " » ne correspond à aucune maladie de la fiche. Je ne soigne pas au hasard.");
                continue;
            }
            int dispo = stock.getOrDefault(remede, 0);
            if (dispo <= 0) {
                j.outil(id + " a « " + maladie + " » : il lui faut « " + remede + " », mais la pharmacie n'en a plus. "
                        + "Je commande (livré ce soir) et je passe au suivant.", "commander", "{\"medicament\":\"" + remede + "\",\"quantite\":2}");
                continue;
            }
            j.outil(id + " a « " + maladie + " » (signe : " + signe + ") : le remède de la fiche est « " + remede + " », en stock (" + dispo + ").",
                    "soigner", "{\"idPatient\":\"" + id + "\",\"medicament\":\"" + remede + "\"}");
            points--;
            stock.put(remede, dispo - 1);
        }
        j.outil("J'ai fait ce que je pouvais aujourd'hui : je termine la journée.", "fin_de_journee", "{}");
        suivi.noeud(runId, "fin", List.of(j.dernier), Evenement.FIN, "Fin de journée", Evenement.OK, j.n + " appel(s) d'outils", null, null, null);
    }

    /** Une journée en cours : numérote les nœuds et les enchaîne. */
    private final class Journee {
        final String runId;
        final JoueurService.Ecoute ecoute;
        String dernier = "debut";
        int n;

        Journee(String runId, JoueurService.Ecoute ecoute) { this.runId = runId; this.ecoute = ecoute; }

        /** Appelle un outil du jeu et publie un nœud (la règle appliquée dans « détail », la réponse dans « résultat »). */
        Object outil(String raison, String nom, String arguments) {
            ArretUrgence.verifier(runId);
            String id = "o" + (++n);
            suivi.noeud(runId, id, List.of(dernier), Evenement.OUTIL, nom + " " + arguments, Evenement.EN_COURS, raison, null, null, null);
            dernier = id;
            JoueurService.pause(ecoute.pauseMs());   // le nœud « en cours » est visible avant la réponse
            long debut = System.currentTimeMillis();
            Object reponse;
            try {
                reponse = deballer(jeu.appeler(nom, arguments));
            } catch (RuntimeException e) {
                suivi.maj(runId, id, Evenement.ERREUR, null, e.getClass().getSimpleName() + " : " + e.getMessage(), System.currentTimeMillis() - debut, null);
                throw e;
            }
            boolean refus = reponse instanceof Map<?, ?> m && Boolean.FALSE.equals(m.get("ok"));
            String texte = reponse instanceof Map<?, ?> m && m.get("message") != null ? String.valueOf(m.get("message")) : json.writeValueAsString(reponse);
            suivi.maj(runId, id, refus ? Evenement.ERREUR : Evenement.OK, null, texte, System.currentTimeMillis() - debut, null);
            JoueurService.pause(ecoute.pauseMs());
            return reponse;
        }

        void note(String titre, String detail) {
            String id = "o" + (++n);
            suivi.noeud(runId, id, List.of(dernier), Evenement.CODE, titre, Evenement.OK, detail, null, null, null);
            dernier = id;
            JoueurService.pause(ecoute.pauseMs());
        }
    }

    // ------------------------------------------------------------------ lecture des réponses MCP

    /** La réponse d'un outil est un JSON, parfois emballé dans une chaîne ou dans une liste de contenus {@code [{"text": "..."}]}. */
    Object deballer(String brut) {
        Object o = brut;
        for (int i = 0; i < 4; i++) {
            if (o instanceof String s) {
                try { o = json.readValue(s, Object.class); } catch (RuntimeException e) { return o; }
            } else if (o instanceof List<?> l && l.size() == 1 && l.get(0) instanceof Map<?, ?> m && m.get("text") != null && m.get("id") == null) {
                o = m.get("text");
            } else {
                return o;
            }
        }
        return o;
    }

    private static int entier(Object o) { return o instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(o)); }
}
