package fr.m2ipm.clinique.roles;

import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import fr.m2ipm.clinique.dto.CreatureDto;
import fr.m2ipm.clinique.dto.SoigneurDto;
import fr.m2ipm.clinique.externe.AgendaPort;
import fr.m2ipm.clinique.externe.TarifsExcelReader;
import fr.m2ipm.clinique.service.CliniqueService;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Le même travail que le superviseur, en Java ordinaire et sans modèle : chemin d'exécution écrit à l'avance.
 * La table espèce + mot-clé du symptôme → médicament remplace le raisonnement du modèle.
 */
@Service
public class TraiterConsultationWorkflow {

    /** (espèce|mot-clé) → médicament. Ajouter une espèce ou un symptôme = ajouter une ligne ici. */
    static final Map<String, String> MEDICAMENTS = Map.of(
            "licorne|sabot", "Onguent de givre",
            "dragon|fievre", "Elixir de valeriane",
            "dragon|toux", "Baume ignifuge",
            "loup-garou|insomnie", "Elixir de valeriane",
            "yeti|allergie", "Onguent de givre",
            "vampire|fatigue", "Serum de sang-frais",
            "phenix|rhume", "Cendre de renaissance");

    public record Resultat(CreatureDto creature, SoigneurDto soigneur, String medicament, int stock,
                           double tarifConsultation, LocalDateTime creneau) {}

    public static class EchecWorkflow extends RuntimeException {
        public EchecWorkflow(String message) { super(message); }
    }

    private final CliniqueService clinique;
    private final TarifsExcelReader excel;
    private final AgendaPort agenda;
    private final SuiviExecution suivi;

    @org.springframework.beans.factory.annotation.Autowired
    public TraiterConsultationWorkflow(CliniqueService clinique, TarifsExcelReader excel, AgendaPort agenda, SuiviExecution suivi) {
        this.clinique = clinique;
        this.excel = excel;
        this.agenda = agenda;
        this.suivi = suivi;
    }

    public TraiterConsultationWorkflow(CliniqueService clinique, TarifsExcelReader excel, AgendaPort agenda) {
        this(clinique, excel, agenda, new SuiviExecution());
    }

    public Resultat traiter(String nomCreature, String symptome, LocalDate jour) {
        return traiter(UUID.randomUUID().toString(), nomCreature, symptome, jour);
    }

    /** Même chemin d'exécution, chaque étape étant un nœud « code Java » du graphe (aucun appel au modèle). */
    public Resultat traiter(String runId, String nomCreature, String symptome, LocalDate jour) {
        suivi.demarrer(runId, "Workflow : " + nomCreature + " · " + symptome);
        suivi.noeud(runId, "demande", List.of(), Evenement.UTILISATEUR, "Demande", Evenement.OK,
                "Créature : " + nomCreature + "\nSymptôme : " + symptome + "\nJour : " + jour, null, null, null);
        String[] precedent = {"demande"};
        try {
            CreatureDto creature = etape(runId, precedent, "creature", "1. Trouver la créature",
                    () -> clinique.rechercherProprietaires("").stream()
                            .flatMap(p -> clinique.creaturesDuProprietaire(p.id()).stream())
                            .filter(c -> c.nom().equalsIgnoreCase(nomCreature.trim())).findFirst()
                            .orElseThrow(() -> new EchecWorkflow("Créature inconnue : " + nomCreature)),
                    c -> c.nom() + " (" + c.espece() + ")");
            List<SoigneurDto> soigneurs = etape(runId, precedent, "soigneur", "2. Soigneur compétent", () -> {
                List<SoigneurDto> l = clinique.soigneursPourEspece(creature.espece());
                if (l.isEmpty()) throw new EchecWorkflow("Aucun soigneur compétent pour " + creature.espece());
                return l;
            }, l -> l.get(0).toString());
            String medicament = etape(runId, precedent, "medicament", "3. Médicament (table espèce + symptôme)",
                    () -> MEDICAMENTS.entrySet().stream()
                            .filter(e -> e.getKey().startsWith(plat(creature.espece()) + "|") && plat(symptome).contains(e.getKey().split("\\|")[1]))
                            .map(Map.Entry::getValue).findFirst()
                            .orElseThrow(() -> new EchecWorkflow("Pas de médicament connu pour " + creature.espece() + " / " + symptome)),
                    m -> m);
            int stock = etape(runId, precedent, "stock", "4. Stock en pharmacie", () -> {
                int st = excel.stock(medicament).orElse(0);
                if (st <= 0) throw new EchecWorkflow("Médicament en rupture : " + medicament);
                return st;
            }, st -> medicament + " : " + st + " en stock");
            double tarif = etape(runId, precedent, "tarif", "5. Tarif de consultation",
                    () -> excel.tarif("Consultation", creature.espece())
                            .orElseThrow(() -> new EchecWorkflow("Pas de tarif pour " + creature.espece())),
                    t -> t + " €");
            LocalTime premier = etape(runId, precedent, "creneau", "6. Premier créneau libre",
                    () -> agenda.creneauxLibres(jour).stream().findFirst()
                            .orElseThrow(() -> new EchecWorkflow("Aucun créneau libre le " + jour)),
                    c -> jour.atTime(c).toString());
            var resultat = new Resultat(creature, soigneurs.get(0), medicament, stock, tarif, jour.atTime(premier));
            suivi.noeud(runId, "fin", List.of(precedent[0]), Evenement.FIN, "Consultation planifiée", Evenement.OK, null,
                    resultat.toString(), null, null);
            suivi.terminer(runId, "REPONSE_FINALE");
            return resultat;
        } catch (EchecWorkflow e) {
            suivi.noeud(runId, "fin", List.of(precedent[0]), Evenement.FIN, "Échec du workflow", Evenement.ERREUR, e.getMessage(),
                    null, null, null);
            suivi.terminer(runId, "ERREUR");
            throw e;
        }
    }

    private <T> T etape(String runId, String[] precedent, String id, String libelle, Supplier<T> travail,
                        java.util.function.Function<T, String> resume) {
        suivi.noeud(runId, id, List.of(precedent[0]), Evenement.CODE, libelle, Evenement.EN_COURS, null, null, null, null);
        long debut = System.nanoTime();
        try {
            T r = travail.get();
            suivi.maj(runId, id, Evenement.OK, null, resume.apply(r), (System.nanoTime() - debut) / 1_000_000, null);
            precedent[0] = id;
            return r;
        } catch (RuntimeException e) {
            suivi.maj(runId, id, Evenement.ERREUR, null, e.getMessage(), (System.nanoTime() - debut) / 1_000_000, null);
            precedent[0] = id;
            throw e;
        }
    }

    private static String plat(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
