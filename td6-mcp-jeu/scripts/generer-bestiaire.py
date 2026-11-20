#!/usr/bin/env python3
"""Génère experience/grand-bestiaire.txt (corpus) et experience/questions-reference.csv.

Le corpus est un long texte de remplissage (créatures inventées, sans chiffres) dans lequel dix faits
sont cachés à des positions réparties. Déterministe : même graine, mêmes fichiers.
Usage : python3 scripts/generer-bestiaire.py
"""
import random
from pathlib import Path

GRAINE = 20261119
SORTIE = Path(__file__).resolve().parent.parent / "src/main/resources/experience"

CREATURES = ["grimpelet des marais", "souffleur de cendres", "fée-lanterne", "golem de mousse", "kobold pêcheur",
             "sylphe des tourbières", "bouc-lune", "hydre timide", "salamandre à voile", "tarasque des brumes",
             "gnome cartographe", "harpie des falaises", "naïade des gués", "troll jardinier", "farfadet comptable"]
LIEUX = ["au creux des vallées humides", "sur les crêtes battues par le vent", "à l'orée des forêts anciennes",
         "près des sources chaudes", "dans les galeries abandonnées", "au bord des lacs glacés",
         "sous les ponts de pierre", "dans les prairies hautes", "entre les ruines des tours", "au fond des grottes sonores"]
ACTIONS = ["rassemble des brindilles polies", "chante des comptines étranges", "collectionne les reflets dans l'eau",
           "tisse des filets de lumière", "répare les sentiers de passage", "classe les cailloux par couleur",
           "veille sur les nids abandonnés", "échange des histoires contre du pain", "trace des cartes sur l'écorce",
           "compte les étoiles filantes chaque soir"]
HABITUDES = ["Les anciens racontent qu'il évite les visiteurs bruyants.", "On le croise surtout à la tombée de la nuit.",
             "Les enfants du village lui laissent parfois des gâteaux.", "Il disparaît dès que la brume se lève.",
             "Son passage laisse une odeur de mousse fraîche.", "Les bergers affirment qu'il porte chance aux troupeaux.",
             "Les chroniqueurs ne s'accordent pas sur son âge.", "Il répond aux appels mais jamais aux questions.",
             "Les marchands ambulants lui doivent plusieurs détours.", "Sa démarche silencieuse est célèbre dans toute la région."]

# (question, réponse attendue, passage caché)
FAITS = [
 ("À quelle température un œuf de phénix éclot-il ?", "412 degrés",
  "Les couveuses de Cendrelune ont établi qu'un œuf de phénix n'éclot qu'à 412 degrés exactement, jamais en dessous."),
 ("Quel est le surnom du yéti de la vallée de Kharnak ?", "Grosse-Moufle",
  "Dans la vallée de Kharnak, le yéti local est surnommé Grosse-Moufle par les guides, à cause de ses énormes pattes."),
 ("Combien de dents possède un dragon adulte de la lignée des Brasiers ?", "142",
  "Un dragon adulte de la lignée des Brasiers possède 142 dents, renouvelées trois fois au cours de sa vie."),
 ("Quelle plante apaise le loup-garou les soirs de pleine lune ?", "valériane argentée",
  "Pour calmer un loup-garou les soirs de pleine lune, les guérisseurs lui font respirer de la valériane argentée."),
 ("En quelle année le premier vampire a-t-il obtenu son ombrelle réglementaire ?", "1887",
  "Le premier vampire à obtenir son ombrelle réglementaire l'a reçue en 1887, après une longue enquête municipale."),
 ("Quel métal ne doit jamais toucher la corne d'une licorne ?", "cuivre",
  "Le cuivre ne doit jamais toucher la corne d'une licorne : il la ternit et la rend cassante en quelques jours."),
 ("Combien de temps dort un dragon après un repas de montagne ?", "trois semaines",
  "Après un repas de montagne, un dragon dort trois semaines d'affilée, sans que rien puisse le réveiller."),
 ("Comment s'appelle le gardien du grand bestiaire de Brécilien ?", "Orfraie",
  "Le grand bestiaire de Brécilien est confié depuis toujours à Maître Orfraie, gardien sourcilleux des manuscrits."),
 ("Quel est le cri du phénix à l'aube ?", "do dièse",
  "À l'aube, le phénix lance un triple trille en do dièse que l'on entend à des lieues à la ronde."),
 ("À quelle distance un yéti détecte-t-il la chaleur d'une personne ?", "quarante mètres",
  "Un yéti détecte la chaleur d'une personne à quarante mètres, même à travers une tempête de neige."),
]

def paragraphe(rng):
    c = rng.choice(CREATURES)
    return (f"Le {c} vit {rng.choice(LIEUX)}, où il {rng.choice(ACTIONS)}. "
            f"{rng.choice(HABITUDES)} "
            f"Certains voyageurs jurent avoir vu un {rng.choice(CREATURES)} {rng.choice(ACTIONS)} {rng.choice(LIEUX)}.")

def main():
    rng = random.Random(GRAINE)
    total = 700
    paragraphes = [paragraphe(rng) for _ in range(total)]
    positions = [int(total * f) for f in (0.05, 0.15, 0.27, 0.38, 0.48, 0.55, 0.66, 0.77, 0.88, 0.96)]
    for pos, (_, _, passage) in zip(positions, FAITS):
        paragraphes[pos] = passage
    SORTIE.mkdir(parents=True, exist_ok=True)
    (SORTIE / "grand-bestiaire.txt").write_text("\n\n".join(paragraphes) + "\n", encoding="utf-8")
    lignes = ["id;question;attendu"] + [f"Q{i+1};{q};{a}" for i, (q, a, _) in enumerate(FAITS)]
    (SORTIE / "questions-reference.csv").write_text("\n".join(lignes) + "\n", encoding="utf-8")
    print(f"{total} paragraphes, {sum(len(p) for p in paragraphes)} caractères")

if __name__ == "__main__":
    main()
