# Site d'aide aux TD

Site statique (GitHub Pages) : https://nverstaevel.github.io/M2IPMAgenticAI_Student/

Il est construit par `python site/build.py` (dépendances : `pip install markdown pygments`), résultat dans `site/_build/` (ignoré par Git). Le workflow `.github/workflows/pages.yml` le publie à chaque push sur `main`.

| Contenu | Où le modifier |
|---|---|
| Sujets des TD | `site/content/sujets/*.md` (copie de `supports/sujets` du dépôt enseignant, sans les notes enseignant) |
| Fiches Notions | `site/content/notions/*.md` (en-tête : `title`, `resume`, `cm`, `td`) |
| Recettes | `site/content/recettes/*.md` (en-tête : `title`, `resume`, `td`, `notions`) |
| Indices et liens par étape | `site/content/aides.json` (clé = TD puis numéro d'étape ; `avant` = « Avant la séance ») |
| Marqueurs TODO, carte du code | générés depuis les dossiers `tdN-…` |
| Style, comportement | `site/assets/site.css`, `site/assets/site.js` |

Prévisualiser : `python site/build.py && python -m http.server -d site/_build 8000`.

Les indices orientent sans donner la solution ; les exemples des recettes sont hors clinique pour ce que les étudiants doivent écrire.
