package fr.m2ipm.clinique.agent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Une trace = un fichier {@code traces/{runId}.jsonl}, une ligne JSON par étape. */
@Component
public class TraceStore {

    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private final Path dossier;
    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    public TraceStore(@Value("${agent.traces.dir:traces}") String dossier) {
        this(Path.of(dossier));
    }

    public TraceStore(Path dossier) {
        this.dossier = dossier;
    }

    public void ajouter(String runId, Etape etape) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : ajouter");
    }

    /** Étapes d'une exécution, vide si le runId est inconnu. */
    public List<Etape> lire(String runId) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : lire");
    }

    private Path fichier(String runId) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : fichier");
    }
}
