package fr.m2ipm.clinique.jeu;

import fr.m2ipm.clinique.scenario.Decision;
import fr.m2ipm.clinique.scenario.Resultat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un critique SANS modèle : il relit un plan de soins avec des règles écrites en Java. Il ne peut ni se tromper de fiche ni inventer
 * une source, et il ne coûte aucun jeton. Le plan attendu est une ligne par patient, au format « P03 : eau-de-glacier (raison) ».
 *
 * <p>Ce que vérifie le gardien : le patient existe ; le médicament existe ; il n'est pas toxique pour l'espèce ; c'est bien le remède
 * d'une maladie de l'espèce, et, si le signe distinctif est connu (le patient a été examiné), celui de SA maladie ; il y a du stock ;
 * il reste assez de points d'action. Un signe inconnu n'est pas une erreur mais un pari : verdict DOUTE.
 *
 * <p>La logique est une méthode statique pure ({@link #verifier}) : elle se teste sans jeu ni modèle. Le branchement sur les
 * outils MCP est dans {@code AgentsJeu#gardienRegles}.
 */
public final class GardienRegles {

    private GardienRegles() {}

    /** Un patient tel que le rend {@code lister_patients} ; {@code signe} est {@code null} tant qu'il n'a pas été examiné. */
    public record PatientVu(String id, String espece, String signe) {}

    /** Une maladie de la fiche d'espèce : son signe distinctif et son remède. */
    public record MaladieVue(String signe, String remede) {}

    /** La fiche d'une espèce : ses maladies et les médicaments toxiques pour elle. */
    public record FicheVue(List<MaladieVue> maladies, Set<String> toxiques) {}

    /** Une ligne du plan : « P03 : eau-de-glacier (raison) ». */
    public record Ligne(String patient, String medicament) {}

    private static final Pattern LIGNE = Pattern.compile("^\\W*(P\\d+)\\s*:\\s*([\\p{L}0-9][\\p{L}0-9\\-]*)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /** Les lignes « Pxx : médicament » d'un texte ; tout le reste (commentaires, verdict, lignes « à examiner ») est ignoré. */
    public static List<Ligne> lignes(String plan) {
        List<Ligne> r = new ArrayList<>();
        Matcher m = LIGNE.matcher(plan == null ? "" : plan);
        while (m.find()) { r.add(new Ligne(m.group(1).toUpperCase(), m.group(2).toLowerCase())); }
        return r;
    }

    /**
     * Le verdict : REJETE s'il y a une erreur (objections précises, une par problème), DOUTE s'il n'y a que des paris (patient non
     * examiné), APPROUVE sinon. Le texte rendu est le plan suivi du verdict, car l'agent suivant ne lit que cela.
     *
     * @param plan      le plan à relire
     * @param patients  les patients présents, par identifiant
     * @param fiches    les fiches d'espèce, par nom d'espèce (minuscules)
     * @param stock     le stock de la pharmacie, par médicament
     * @param points    les points d'action restants aujourd'hui
     */
    public static Resultat verifier(String plan, Map<String, PatientVu> patients, Map<String, FicheVue> fiches,
                                    Map<String, Integer> stock, int points) {
        List<String> erreurs = new ArrayList<>();
        List<String> paris = new ArrayList<>();
        List<Ligne> lignes = lignes(plan);
        if (lignes.isEmpty() && !patients.isEmpty()) {
            erreurs.add("Plan illisible ou vide : écrivez une ligne par patient au format « P03 : médicament (raison) ».");
        }
        if (lignes.size() > points) {
            erreurs.add("Le plan compte " + lignes.size() + " soins pour " + points + " point(s) d'action restant(s) : classez par urgence et retirez les derniers.");
        }
        Map<String, Integer> besoin = new HashMap<>();
        for (Ligne l : lignes) {
            PatientVu p = patients.get(l.patient());
            if (p == null) { erreurs.add(l.patient() + " : patient inconnu (déjà soigné, mort, ou identifiant inventé)."); continue; }
            if (!stock.containsKey(l.medicament())) { erreurs.add(l.patient() + " : « " + l.medicament() + " » n'existe pas à la pharmacie."); continue; }
            FicheVue f = fiches.get(p.espece().toLowerCase());
            if (f == null) { erreurs.add(l.patient() + " : fiche de l'espèce « " + p.espece() + " » non consultée."); continue; }
            if (f.toxiques().contains(l.medicament())) {
                erreurs.add(l.patient() + " : « " + l.medicament() + " » est TOXIQUE pour un " + p.espece() + ". Ne jamais le donner.");
                continue;
            }
            List<String> remedes = f.maladies().stream().map(MaladieVue::remede).toList();
            if (p.signe() != null) {
                String attendu = f.maladies().stream().filter(m -> m.signe().equalsIgnoreCase(p.signe())).map(MaladieVue::remede).findFirst().orElse(null);
                if (attendu != null && !attendu.equals(l.medicament())) {
                    erreurs.add(l.patient() + " : le signe « " + p.signe() + " » indique le remède « " + attendu + " », pas « " + l.medicament() + " ».");
                }
            } else if (!remedes.contains(l.medicament())) {
                erreurs.add(l.patient() + " : « " + l.medicament() + " » ne soigne aucune maladie d'un " + p.espece() + " (remèdes : " + String.join(", ", remedes) + ").");
            } else {
                paris.add(l.patient() + " : non examiné, le choix entre " + String.join(" et ", remedes) + " est un pari (une chance sur deux).");
            }
            besoin.merge(l.medicament(), 1, Integer::sum);
        }
        besoin.forEach((m, n) -> {
            if (stock.getOrDefault(m, 0) < n) { paris.add("Stock de « " + m + " » insuffisant (" + stock.getOrDefault(m, 0) + " pour " + n + ") : commander, le patient attendra demain."); }
        });
        Decision d = !erreurs.isEmpty() ? Decision.REJETE : (!paris.isEmpty() ? Decision.DOUTE : Decision.APPROUVE);
        List<String> remarques = new ArrayList<>(erreurs);
        remarques.addAll(paris);
        String texte = (plan == null ? "" : plan.strip()) + "\n\nVerdict du gardien : " + d
                + (remarques.isEmpty() ? "" : "\nObjections :\n- " + String.join("\n- ", remarques));
        return new Resultat(texte, null, d, remarques, List.of(), null);
    }
}
