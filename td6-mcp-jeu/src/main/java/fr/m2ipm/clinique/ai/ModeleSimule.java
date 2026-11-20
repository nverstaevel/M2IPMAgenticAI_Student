package fr.m2ipm.clinique.ai;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Faux modèle déterministe (profil « simule ») : tests et évaluation sans serveur de modèles.
 * <ul>
 *   <li>un message « outil:NOM:{json} » provoque l'appel de l'outil NOM avec ces arguments ;</li>
 *   <li>après le résultat de l'outil, il répond « Résultat des outils : … » ;</li>
 *   <li>une demande de sortie structurée (le prompt contient « JSON ») reçoit un triage fixe ;</li>
 *   <li>sinon il répond « Simulé : » suivi du message.</li>
 * </ul>
 */
@Component
@Profile("simule")
public class ModeleSimule implements ChatModel {

    static final String TRIAGE_JSON = "{\"espece\":\"Dragon\",\"urgence\":\"MOYENNE\","
            + "\"resume\":\"Rhume de dragon simulé\",\"symptomes\":[\"fumée verdâtre\",\"éternuements\"]}";

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        Message dernier = messages.get(messages.size() - 1);
        String systeme = messages.stream().filter(m -> m instanceof org.springframework.ai.chat.messages.SystemMessage)
                .map(Message::getText).findFirst().orElse("");
        String utilisateur = dernier.getText() == null ? "" : dernier.getText();
        if (systeme.contains("[ROLE:VETERINAIRE]")) return reponse(veterinaire(utilisateur));
        if (systeme.contains("[ROLE:GARDIEN]")) return reponse(gardien(utilisateur));
        if (systeme.contains("[ROLE:SUPERVISEUR]") && !utilisateur.startsWith("Demande : ")) return reponse(PLAN_JSON);
        if (dernier instanceof ToolResponseMessage retour) {
            String donnees = retour.getResponses().stream()
                    .map(ToolResponseMessage.ToolResponse::responseData)
                    .collect(Collectors.joining(" | "));
            return reponse("Résultat des outils : " + donnees);
        }
        String texte = dernier.getText() == null ? "" : dernier.getText();
        if (texte.startsWith("outil:")) {
            String[] morceaux = texte.split(":", 3);
            AssistantMessage appel = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(new AssistantMessage.ToolCall("appel-1", "function", morceaux[1], morceaux[2])))
                    .build();
            return new ChatResponse(List.of(new Generation(appel)));
        }
        if (texte.contains("JSON")) {
            return reponse(TRIAGE_JSON);
        }
        return reponse("Simulé : " + texte);
    }

    /**
     * Spring AI 2.0 : le ChatClient part de {@code chatModel.getOptions()}. Sans options « tool calling »,
     * le ToolCallingAdvisor ignore la requête et la boucle d'outils ne démarre pas.
     */
    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    static final String PLAN_JSON = "{\"sousTaches\":[{\"travailleur\":\"DOSSIER\",\"consigne\":\"Retrouver le dossier de la créature\"},"
            + "{\"travailleur\":\"PHARMACIE\",\"consigne\":\"Choisir un médicament en stock et chiffrer\"},"
            + "{\"travailleur\":\"AGENDA\",\"consigne\":\"Proposer un créneau\"}]}";

    /** Vétérinaire simulé : profil pressé = piste habituelle ; prudent ou après objections = traitement sûr. */
    private static String veterinaire(String u) {
        var nom = java.util.regex.Pattern.compile("créature : ([^(]+?) \\(").matcher(u);
        String creature = nom.find() ? nom.group(1) : "inconnue";
        var piste = java.util.regex.Pattern.compile("Piste habituelle : ([^\\n]+)").matcher(u);
        boolean objections = u.contains("Objections du gardien");
        String traitement = (!objections && piste.find()) ? piste.group(1).strip() : "infusion de valériane, repos et observation";
        return "{\"creature\":\"" + creature + "\",\"diagnostic\":\"Diagnostic simulé\",\"traitements\":[\""
                + traitement.replace("\"", "'") + "\"],\"justification\":\"Réponse simulée du vétérinaire\"}";
    }

    /** Gardien simulé, par règles : argent → REJETE, ail ou soleil → REJETE, sauna → DOUTE, sinon APPROUVE. */
    private static String gardien(String u) {
        String t = u.toLowerCase(java.util.Locale.ROOT);
        int i = t.indexOf("traitements proposés :");
        String traitements = i < 0 ? t : t.substring(i, t.indexOf('\n', i) < 0 ? t.length() : t.indexOf('\n', i));
        String wiki = "https://fr.wikipedia.org/wiki/";
        if (traitements.contains("argent")) {
            return "{\"verdict\":\"REJETE\",\"objections\":[\"L'argent est réputé nocif aux loups-garous\"],\"sources\":[\"" + wiki + "Loup-garou\"]}";
        }
        if (java.util.regex.Pattern.compile("\\bail\\b|soleil").matcher(traitements).find()) {
            return "{\"verdict\":\"REJETE\",\"objections\":[\"L'ail et le soleil sont réputés nocifs aux vampires\"],\"sources\":[\"" + wiki + "Vampire\"]}";
        }
        if (traitements.contains("sauna")) {
            return "{\"verdict\":\"DOUTE\",\"objections\":[\"Sources maigres sur le yéti et la chaleur\"],\"sources\":[\"" + wiki + "Yéti\"]}";
        }
        return "{\"verdict\":\"APPROUVE\",\"objections\":[],\"sources\":[\"" + wiki + "Dragon\"]}";
    }

    private static ChatResponse reponse(String texte) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(texte))));
    }
}
