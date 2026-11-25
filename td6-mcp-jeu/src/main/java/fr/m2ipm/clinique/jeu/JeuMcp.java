package fr.m2ipm.clinique.jeu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * La clinique vue comme CLIENT MCP. Le starter {@code spring-ai-starter-mcp-client} (profil « jeu ») se connecte au serveur du jeu
 * et publie ses outils comme un {@link ToolCallbackProvider} : à partir de là, un outil MCP est un {@link ToolCallback} comme
 * un autre, que l'on donne à un agent. Cette classe sert au code « hors agent » (lanceur de partie, tests) qui veut lire l'état.
 */
@Component
public class JeuMcp {

    private final ObjectProvider<ToolCallbackProvider> mcp;

    public JeuMcp(ObjectProvider<ToolCallbackProvider> mcp) { this.mcp = mcp; }

    /** Les outils publiés par le jeu (vide si le profil « jeu » n'est pas actif ou si le serveur est injoignable). */
    public ToolCallback[] outils() {
        return mcp.orderedStream().flatMap(p -> Arrays.stream(p.getToolCallbacks())).toArray(ToolCallback[]::new);
    }

    public boolean connecte() { return outils().length > 0; }

    /** Appelle un outil du jeu hors de tout agent. Les arguments sont un objet JSON : {@code "{}"} si l'outil n'en a pas. */
    public String appeler(String outil, String argumentsJson) {
        ToolCallback cb = Arrays.stream(outils()).filter(o -> o.getToolDefinition().name().equals(outil)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Outil du jeu introuvable : " + outil
                        + " (profil « jeu » actif ? serveur démarré sur le port 8090 ?)"));
        return cb.call(adapter(cb, argumentsJson));
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * Si le jeu a été compilé sans l'option {@code -parameters} (cas fréquent d'un IDE), ses outils déclarent leurs paramètres sous
     * les noms {@code arg0}, {@code arg1}… au lieu de {@code idPatient}, {@code medicament}… On les renomme alors dans l'ordre :
     * les arguments doivent être donnés dans l'ordre de la méthode Java. Sinon (cas normal), rien ne change.
     */
    static String adapter(ToolCallback cb, String arguments) {
        try {
            Object schema = JSON.readValue(cb.getToolDefinition().inputSchema(), Object.class);
            if (!(schema instanceof Map<?, ?> s) || !(s.get("properties") instanceof Map<?, ?> props) || props.isEmpty()) { return arguments; }
            List<String> noms = new ArrayList<>();
            for (Object k : props.keySet()) { noms.add(String.valueOf(k)); }
            if (!noms.stream().allMatch(n -> n.matches("arg\\d+"))) { return arguments; }
            noms.sort(Comparator.comparingInt(n -> Integer.parseInt(n.substring(3))));
            Object lus = JSON.readValue(arguments, Object.class);
            if (!(lus instanceof Map<?, ?> args) || args.isEmpty() || noms.containsAll(args.keySet())) { return arguments; }
            Map<String, Object> renommes = new LinkedHashMap<>();
            int i = 0;
            for (Object v : args.values()) { if (i < noms.size()) { renommes.put(noms.get(i++), v); } }
            return JSON.writeValueAsString(renommes);
        } catch (RuntimeException e) {
            return arguments;
        }
    }

    /** Résumé de l'état de la partie, lu dans la réponse JSON de {@code etat_clinique}. */
    public record Etat(int jour, int pointsAction, int tresorerie, int gueris, int morts, boolean terminee, int score) {}

    public Etat etat() {
        String t = appeler("etat_clinique", "{}");
        return new Etat(entier(t, "jour"), entier(t, "pointsAction"), entier(t, "tresorerie"), entier(t, "gueris"),
                entier(t, "morts"), booleen(t, "terminee"), entier(t, "score"));
    }

    // La réponse d'un outil MCP est un JSON, parfois emballé dans une chaîne : on lit les champs sans dépendre de l'emballage.
    static int entier(String texte, String champ) {
        Matcher m = Pattern.compile(champ + "[^\\w-]{1,6}(-?\\d+)").matcher(texte);
        if (!m.find()) { throw new IllegalStateException("Champ « " + champ + " » absent de : " + texte); }
        return Integer.parseInt(m.group(1));
    }

    static boolean booleen(String texte, String champ) {
        Matcher m = Pattern.compile(champ + "\\W{1,6}(true|false)").matcher(texte);
        return m.find() && Boolean.parseBoolean(m.group(1));
    }

    static Optional<Integer> premierEntier(String texte, String champ) {
        Matcher m = Pattern.compile(champ + "[^\\w-]{1,6}(-?\\d+)").matcher(texte);
        return m.find() ? Optional.of(Integer.parseInt(m.group(1))) : Optional.empty();
    }
}
