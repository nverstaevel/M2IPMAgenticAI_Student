package fr.m2ipm.clinique.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnostic : appelle directement le modèle actif (sans mémoire, sans outils) avec un message minimal et
 * un délai maximal. Permet de savoir si le fournisseur répond, en combien de temps, et sinon pourquoi.
 */
@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    private final ObjectProvider<ChatModel> modele;

    public DiagnosticController(ObjectProvider<ChatModel> modele) {
        this.modele = modele;
    }

    @GetMapping("/modele")
    public Map<String, Object> tester() {
        Map<String, Object> resultat = new LinkedHashMap<>();
        ChatModel m = modele.getIfAvailable();
        if (m == null) {
            resultat.put("statut", "aucun modèle actif (profil sans ChatModel)");
            return resultat;
        }
        resultat.put("modele", m.getClass().getSimpleName());
        long debut = System.nanoTime();
        try {
            String reponse = CompletableFuture.supplyAsync(() -> m.call("Réponds uniquement par le mot : ok"))
                    .get(30, TimeUnit.SECONDS);
            resultat.put("statut", "OK");
            resultat.put("reponse", reponse);
        } catch (TimeoutException e) {
            resultat.put("statut", "TIMEOUT : pas de réponse du fournisseur après 30 s");
        } catch (Exception e) {
            Throwable racine = e;
            while (racine.getCause() != null) {
                racine = racine.getCause();
            }
            resultat.put("statut", "ERREUR");
            resultat.put("exception", racine.getClass().getName());
            resultat.put("message", String.valueOf(racine.getMessage()));
        }
        resultat.put("dureeMs", (System.nanoTime() - debut) / 1_000_000);
        return resultat;
    }
}
