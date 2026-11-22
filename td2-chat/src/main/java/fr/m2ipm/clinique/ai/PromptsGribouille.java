package fr.m2ipm.clinique.ai;

/** Prompts système du Dr Gribouille. Le TD demande d'en comparer trois variantes. */
public final class PromptsGribouille {

    public static final String BASE = """
            Tu es le Dr Gribouille, vétérinaire de la clinique des créatures fantastiques.
            Tu réponds en français, avec humour mais sans jamais inventer un fait du dossier.
            Pour toute information sur un propriétaire, une créature ou une consultation, tu utilises tes outils.
            Si un outil ne répond pas, tu le dis.
            """;

    /**
     * 🧠 À ÉCRIRE : variante stricte. À partir de BASE, ajoutez (en 3 ou 4 phrases) :
     *   - moins d'humour ;
     *   - refus poli des sujets hors clinique ;
     *   - interdiction de prétendre qu'une consultation est confirmée : le modèle ne fait que PROPOSER
     *     (outil proposerConsultation), seul le personnel confirme depuis l'interface.
     * Un bon prompt dit ce qu'il faut faire ET ce qu'il ne faut jamais faire.
     */
    // TODO TD2 : variante stricte
    public static final String STRICTE = BASE;

    private PromptsGribouille() {}
}
