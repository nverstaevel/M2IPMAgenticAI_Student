package fr.m2ipm.clinique.roles;

import fr.m2ipm.clinique.ai.OutilsClinique;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** L'acteur : dossier, pharmacie et tarifs. Deux profils de prompt. Répond par sortie structurée. */
@Component
public class VeterinaireAgent {

    static final String BASE = """
            [ROLE:VETERINAIRE]
            Tu es le vétérinaire de la clinique des créatures fantastiques. Tu proposes un diagnostic et un traitement
            pour la créature décrite, en français. Tu peux consulter le dossier, la pharmacie et les tarifs avec tes outils.
            Tu réponds uniquement par le Diagnostic demandé (créature, diagnostic, traitements, justification).
            Les traitements sont 1 à 3 intitulés COURTS (moins de 80 caractères chacun, par exemple « tisane de valériane argentée »),
            sans phrase ni explication : les explications vont dans la justification, en deux phrases au plus.
            Si le gardien du bestiaire a formulé des objections, tu en tiens compte et tu proposes un traitement différent.
            """;

    static final String PRUDENT = BASE + """
            Profil PRUDENT : avant de proposer, tu vérifies dans le catalogue les contre-indications pour l'espèce
            et tu choisis un traitement adapté.
            """;

    static final String PRESSE = BASE + """
            Profil PRESSÉ : tu proposes sans vérifier le premier remède qui te vient, celui qui est indiqué comme piste habituelle.
            """;

    static final String OBSTINE = BASE + """
            Profil OBSTINÉ : tu es convaincu de la piste habituelle indiquée. Même si le gardien formule des objections,
            tu maintiens le même traitement, en reformulant seulement la justification.
            """;

    private final ChatClient client;
    /** Client SANS outils qui convertit l'analyse libre en Diagnostic (mode « deux temps », voir agents.sortie-en-deux-temps). */
    private final ChatClient structurant;
    /** Pour les scénarios : un client construit à la demande avec les seuls outils que le .md autorise (null dans les tests). */
    private final ChatClient.Builder base;

    @Autowired
    public VeterinaireAgent(ChatClient.Builder builder, OutilsClinique clinique, OutilsPharmacie pharmacie,
                            @org.springframework.beans.factory.annotation.Value("${agents.sortie-en-deux-temps:false}") boolean deuxTemps) {
        this(construire(builder, clinique, pharmacie), deuxTemps ? builder.clone().defaultAdvisors(fr.m2ipm.clinique.agent.ComptageJetons.advisor()).build() : null, builder);
    }

    private static ChatClient construire(ChatClient.Builder builder, OutilsClinique clinique, OutilsPharmacie pharmacie) {
        return builder.clone().defaultToolCallbacks(fr.m2ipm.clinique.agent.SuiviOutils.envelopper(
                org.springframework.ai.support.ToolCallbacks.from(clinique, pharmacie)))
                .defaultAdvisors(fr.m2ipm.clinique.agent.ComptageJetons.advisor()).build();
    }

    public VeterinaireAgent(ChatClient client) { this(client, null); }

    public VeterinaireAgent(ChatClient client, ChatClient structurant) { this(client, structurant, null); }

    private VeterinaireAgent(ChatClient client, ChatClient structurant, ChatClient.Builder base) {
        this.client = client;
        this.structurant = structurant;
        this.base = base;
    }

    public Diagnostic proposer(CasBestiaire cas, ProfilActeur profil, List<String> objections) {
        return proposer(cas, profil, objections, null);
    }

    /** @param outils les seuls outils autorisés (scénario .md) ; null = les outils par défaut du vétérinaire (dossier et pharmacie). */
    public Diagnostic proposer(CasBestiaire cas, ProfilActeur profil, List<String> objections, List<ToolCallback> outils) {
        ChatClient client = outils == null || base == null ? this.client
                : base.clone().defaultToolCallbacks(outils.toArray(new ToolCallback[0]))
                        .defaultAdvisors(fr.m2ipm.clinique.agent.ComptageJetons.advisor()).build();
        StringBuilder u = new StringBuilder();
        u.append("Cas ").append(cas.id()).append(" — créature : ").append(cas.creature()).append(" (").append(cas.espece())
                .append(") ; symptôme : ").append(cas.symptome()).append('\n');
        u.append("Profil : ").append(profil).append('\n');
        if (profil != ProfilActeur.PRUDENT) {
            u.append("Piste habituelle : ").append(cas.traitementPresse()).append('\n');
        }
        if (objections != null && !objections.isEmpty()) {
            u.append("Objections du gardien à prendre en compte : ").append(String.join(" ; ", objections)).append('\n');
        }
        String systeme = switch (profil) { case PRESSE -> PRESSE; case OBSTINE -> OBSTINE; default -> PRUDENT; };
        if (structurant == null) {
            return client.prompt().system(systeme).user(u.toString()).call().entity(Diagnostic.class);
        }
        // Deux temps : certains modèles (gpt-oss, qwen sur Groq) confondent « répondre en JSON » avec un appel d'outil nommé JSON
        // quand outils et sortie structurée sont demandés ensemble. On sépare : outils + texte libre, puis conversion sans outils.
        String analyse = client.prompt().system(systeme)
                .user(u + "\nRéponds en texte libre et BREF (moins de 120 mots) : diagnostic, 1 à 3 traitements en intitulés courts, justification en deux phrases, sans JSON.").call().content();
        return structurant.prompt()
                .system("Tu convertis une analyse vétérinaire en Diagnostic structuré (créature, diagnostic, traitements, justification). Les traitements sont 1 à 3 intitulés courts (moins de 80 caractères chacun, sans phrase) ; la justification tient en deux phrases.")
                .user("Créature : " + cas.creature() + "\nAnalyse :\n" + analyse).call().entity(Diagnostic.class);
    }
}
