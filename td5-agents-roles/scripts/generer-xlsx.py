#!/usr/bin/env python3
"""Génère src/main/resources/donnees/tarifs-et-pharmacie.xlsx (feuilles Tarifs et Pharmacie)."""
from pathlib import Path
from openpyxl import Workbook

SORTIE = Path(__file__).resolve().parent.parent / "src/main/resources/donnees/tarifs-et-pharmacie.xlsx"
ESPECES = ["Dragon", "Licorne", "Yéti", "Loup-garou", "Vampire", "Phénix"]
BASE = {"Consultation": 40, "Vaccin": 25, "Soin de griffes": 30, "Examen de nuit": 55}
COEF = {"Dragon": 2.0, "Licorne": 1.5, "Yéti": 1.2, "Loup-garou": 1.3, "Vampire": 1.0, "Phénix": 1.8}

wb = Workbook()
t = wb.active
t.title = "Tarifs"
t.append(["Acte", "Espèce", "Prix (€)"])
for acte, prix in BASE.items():
    for e in ESPECES:
        t.append([acte, e, round(prix * COEF[e], 2)])
p = wb.create_sheet("Pharmacie")
p.append(["Médicament", "Stock", "Unité"])
for ligne in [("Baume ignifuge", 12, "pots"), ("Poudre de lune", 0, "sachets"), ("Élixir de valériane", 7, "flacons"),
              ("Sérum de sang-frais", 3, "ampoules"), ("Onguent de givre", 20, "tubes"), ("Cendre de renaissance", 2, "doses")]:
    p.append(list(ligne))
SORTIE.parent.mkdir(parents=True, exist_ok=True)
wb.save(SORTIE)
print("écrit", SORTIE)
