# Supports de cours

Diapositives des trois cours magistraux (CM1, CM2, CM3) de « IA agentiques avec Spring AI ».

| Fichier | Contenu |
| --- | --- |
| `CM1.pdf`, `CM2.pdf`, `CM3.pdf` | les diapositives, une page par diapositive, à lire ou à imprimer |
| `slides/*.html`, `deck.css`, `deck.json` | les sources (une diapositive par fichier, l'ordre est dans `deck.json`) |

Les mêmes diapositives sont lisibles en ligne, avec une navigation par chapitre, sur le site d'aide aux TD :
<https://nverstaevel.github.io/M2IPMAgenticAI_Student/cours/index.html>. Les fiches « Notions » du site renvoient directement à la diapositive concernée.

Les sources sont importées depuis le dépôt enseignant par `python3 site/import_cours.py <chemin>` (les notes de l'enseignant ne sont pas reprises), et les PDF régénérés par `python3 site/pdf_cours.py`.
