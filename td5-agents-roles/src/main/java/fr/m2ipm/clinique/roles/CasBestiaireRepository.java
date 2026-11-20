package fr.m2ipm.clinique.roles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Les cinq cas du bestiaire, lus dans donnees/cas-bestiaire.csv. */
@Component
public class CasBestiaireRepository {

    private final List<CasBestiaire> cas;

    public CasBestiaireRepository() {
        try {
            String csv = new String(new ClassPathResource("donnees/cas-bestiaire.csv").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            cas = Arrays.stream(csv.split("\n")).skip(1).filter(l -> !l.isBlank()).map(l -> l.split(";"))
                    .map(c -> new CasBestiaire(Integer.parseInt(c[0]), c[1], c[2], c[3], c[4], c[5], Verdict.valueOf(c[6].strip())))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<CasBestiaire> tous() { return cas; }

    public Optional<CasBestiaire> parId(int id) { return cas.stream().filter(c -> c.id() == id).findFirst(); }
}
