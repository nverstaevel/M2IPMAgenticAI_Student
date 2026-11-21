"""Copie les slides de cours depuis le dépôt enseignant vers cours/ (sans les notes de l'enseignant).

Usage : python3 site/import_cours.py /chemin/vers/m2ipmagenticai/supports/cm
"""
import pathlib, re, shutil, sys

src = pathlib.Path(sys.argv[1])
dst = pathlib.Path(__file__).resolve().parent.parent / "cours"
if (dst / "slides").exists(): shutil.rmtree(dst / "slides")
(dst / "slides").mkdir(parents=True)
n = 0
for f in sorted((src / "slides").glob("*.html")):
    t = re.sub(r"\s*<aside>.*?</aside>", "", f.read_text(encoding="utf-8"), flags=re.S)   # notes de l'enseignant : jamais publiées
    (dst / "slides" / f.name).write_text(t, encoding="utf-8"); n += 1
for nom in ("deck.css", "deck.json"): shutil.copy(src / nom, dst / nom)
print(n, "slides copiées dans", dst)
