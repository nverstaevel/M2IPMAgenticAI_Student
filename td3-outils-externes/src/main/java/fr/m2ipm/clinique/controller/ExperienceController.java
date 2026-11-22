package fr.m2ipm.clinique.controller;

import fr.m2ipm.clinique.experience.ExperienceContexte;
import fr.m2ipm.clinique.experience.Mesure;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lance l'expérience « contexte » avec le modèle actif : dix questions, stratégies A et B, résultat dans mesures.csv
 * (à la racine du projet). Fourni : vous le lancez et vous analysez les chiffres.
 * Exemple : curl -u gribouille:gribouille -X POST "localhost:8080/api/experience/lancer?tailleMax=20000"
 */
@RestController
@RequestMapping("/api/experience")
public class ExperienceController {

    private final ObjectProvider<ChatModel> modele;

    public ExperienceController(ObjectProvider<ChatModel> modele) {
        this.modele = modele;
    }

    /** @param tailleMax nombre de caractères du corpus collés dans le prompt par la stratégie A */
    @PostMapping("/lancer")
    public Map<String, Object> lancer(@RequestParam(defaultValue = "20000") int tailleMax) {
        ChatModel m = modele.getIfAvailable();
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Aucun modèle actif : lancez un profil avec un modèle.");
        }
        ExperienceContexte experience = new ExperienceContexte(prompt -> m.call(prompt));
        List<Mesure> mesures = experience.lancer(tailleMax, Path.of("mesures.csv"));
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("fichier", "mesures.csv");
        resultat.put("lignes", mesures.size());
        resultat.put("tailleMaxStrategieA", tailleMax);
        return resultat;
    }
}
