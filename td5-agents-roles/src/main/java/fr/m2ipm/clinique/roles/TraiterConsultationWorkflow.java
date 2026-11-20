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
        // TODO TD5 : à écrire
        throw new UnsupportedOperationException("À écrire : traiter");
    }

    /** Même chemin d'exécution, chaque étape étant un nœud « code Java » du graphe (aucun appel au modèle). */
    public Resultat traiter(String runId, String nomCreature, String symptome, LocalDate jour) {
        // TODO TD5 : à écrire
        throw new UnsupportedOperationException("À écrire : traiter");
    }

    private <T> T etape(String runId, String[] precedent, String id, String libelle, Supplier<T> travail,
                        java.util.function.Function<T, String> resume) {
                            // TODO TD5 : à écrire
                            throw new UnsupportedOperationException("À écrire : etape");
                        }

    private static String plat(String s) {
        // TODO TD5 : à écrire
        throw new UnsupportedOperationException("À écrire : plat");
    }
}
