"""Génère cours/CM1.pdf, CM2.pdf, CM3.pdf (une page par diapositive, 16:9) à partir de cours/slides.
Usage : python3 site/pdf_cours.py   (nécessite playwright et un Chrome/Chromium)"""
import pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import cours as CM
from playwright.sync_api import sync_playwright

def doc(cm):
    pages = []
    for s in CM.slides(cm):
        pages.append(f'<div class="deck" style="--pg:\'{s["i"] + 1}\'">{s["html"]}</div>')
    return ("<!doctype html><meta charset=utf-8><style>@page{size:1920px 1080px;margin:0}html,body{margin:0}"
            ".deck{page-break-after:always;break-after:page;width:1920px;height:1080px;overflow:hidden}"
            + CM.css_scope() + "</style>" + "".join(pages))

with sync_playwright() as p:
    nav = p.chromium.launch(executable_path="/usr/bin/google-chrome" if pathlib.Path("/usr/bin/google-chrome").exists() else None, args=["--no-sandbox"])
    for cm, (nom, *_r) in CM.CMS.items():
        h = CM.COURS / f"_{nom}.html"; h.write_text(doc(cm), encoding="utf-8")
        pg = nav.new_page(viewport={"width": 1920, "height": 1080}); pg.goto(h.as_uri()); pg.wait_for_timeout(500)
        pg.pdf(path=str(CM.COURS / f"{nom}.pdf"), width="1920px", height="1080px", print_background=True, margin={"top": "0", "right": "0", "bottom": "0", "left": "0"})
        h.unlink(); print(nom, (CM.COURS / f"{nom}.pdf").stat().st_size // 1024, "Ko")
    nav.close()
