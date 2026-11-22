package fr.m2ipm.clinique.controller;

import fr.m2ipm.clinique.ai.PropositionConsultation;
import fr.m2ipm.clinique.ai.PropositionService;
import fr.m2ipm.clinique.ai.TriageResultat;
import fr.m2ipm.clinique.dto.ConsultationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    public record ChatRequest(@NotBlank(message = "conversationId est obligatoire") String conversationId,
                              @NotBlank(message = "message est obligatoire") String message) {}

    public record ChatReponse(String reponse, List<PropositionConsultation> propositions) {}

    public record TriageRequest(@NotBlank(message = "texte est obligatoire") String texte) {}

    private final ChatClient chat;
    private final ChatClient triage;
    private final PropositionService propositions;
    private final ChatMemory memoire;

    public ChatController(ChatClient chat, @org.springframework.beans.factory.annotation.Qualifier("triageClient") ChatClient triage,
                          PropositionService propositions, ChatMemory memoire) {
        this.chat = chat;
        this.triage = triage;
        this.propositions = propositions;
        this.memoire = memoire;
    }

    @PostMapping("/chat")
    public ChatReponse chat(@Valid @RequestBody ChatRequest requete) {
        LOG.info("Question [conversation {}] : {}", requete.conversationId(), requete.message());
        long debut = System.nanoTime();
        String reponse;
        try {
            reponse = chat.prompt()
                    .user(requete.message())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, requete.conversationId()))
                    .toolContext(Map.of("conversationId", requete.conversationId()))
                    .call()
                    .content();
        } catch (RuntimeException e) {
            // Fournisseur de modèle injoignable, clé refusée, modèle inconnu, surcharge (503)… : on le dit au client.
            LOG.warn("Échec de l'appel au modèle après {} ms", (System.nanoTime() - debut) / 1_000_000, e);
            Throwable racine = e;
            while (racine.getCause() != null) {
                racine = racine.getCause();
            }
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Le modèle n'a pas répondu : " + e.getClass().getSimpleName() + " — " + racine.getMessage());
        }
        LOG.info("Réponse du modèle en {} ms ({} caractères)", (System.nanoTime() - debut) / 1_000_000,
                reponse == null ? 0 : reponse.length());
        return new ChatReponse(reponse, propositions.enAttente(requete.conversationId()));
    }

    /** Réinitialise le contexte : efface la mémoire du modèle et les propositions en attente de cette conversation. */
    @DeleteMapping("/chat/{conversationId}")
    public org.springframework.http.ResponseEntity<Void> reinitialiser(@PathVariable String conversationId) {
        memoire.clear(conversationId);
        propositions.oublier(conversationId);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    /** Même conversation en flux : Server-Sent Events, un fragment de texte par événement. */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> flux(@RequestParam String conversationId, @RequestParam String message) {
        return chat.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("conversationId", conversationId))
                .stream()
                .content();
    }

    /** Texte libre en entrée, record Java en sortie : le modèle remplit la structure. */
    @PostMapping("/triage")
    public TriageResultat triage(@Valid @RequestBody TriageRequest requete) {
        return triage.prompt()
                .user(requete.texte())
                .call()
                .entity(TriageResultat.class);
    }

    @GetMapping("/propositions")
    public List<PropositionConsultation> propositions(@RequestParam String conversationId) {
        return propositions.enAttente(conversationId);
    }

    /** Réservé au rôle PERSONNEL (toute écriture POST sous /api l'exige). C'est ici, et nulle part côté modèle, qu'on écrit. */
    @PostMapping("/propositions/{id}/confirmer")
    public ConsultationDto confirmer(@PathVariable String id) {
        return propositions.confirmer(id);
    }
}
