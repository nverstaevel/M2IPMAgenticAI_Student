"""Slides de cours : conversion des slides (x-shape, x-connector) en HTML/SVG standard et génération des pages."""
import html, json, pathlib, re

ROOT = pathlib.Path(__file__).resolve().parent.parent
COURS = ROOT / "cours"
CMS = {"cm1": ("CM1", 0, 92, "Appeler un modèle, mémoire, outils, confirmation des écritures"),
       "cm2": ("CM2", 92, 127, "Sources externes, contexte, boucle d'agent (ReAct), rôles et graphe en .md"),
       "cm3": ("CM3", 127, 142, "Le protocole MCP, le jeu de la clinique, architectures et mesure")}

def deck():
    d = json.loads((COURS / "deck.json").read_text(encoding="utf-8"))
    return d["order"], d.get("sections", {})

def attr(tag, nom, defaut=""):
    m = re.search(rf'\b{nom}="([^"]*)"', tag)
    return m.group(1) if m else defaut

def style_val(tag, prop, defaut):
    m = re.search(rf'{prop}:\s*([^;"]+)', attr(tag, "style"))
    return m.group(1).strip() if m else defaut

ARROWS = {"arrow-right": "polygon(0 30%,62% 30%,62% 0,100% 50%,62% 100%,62% 70%,0 70%)",
          "arrow-left": "polygon(100% 30%,38% 30%,38% 0,0 50%,38% 100%,38% 70%,100% 70%)",
          "arrow-down": "polygon(30% 0,70% 0,70% 62%,100% 62%,50% 100%,0 62%,30% 62%)"}

def convertir(section):
    """Remplace <x-shape> par un <div> à clip-path et regroupe les <x-connector> dans un SVG."""
    section = re.sub(r"\s*<aside>.*?</aside>", "", section, flags=re.S)
    section = re.sub(r'^<section id="[^"]*"', "<section", section.strip())   # l'identifiant est porté par la figure
    def forme(m):
        tag = m.group(0); k = attr(tag, "kind", "arrow-right")
        return f'<div class="xs" style="{attr(tag, "style")};clip-path:{ARROWS.get(k, ARROWS["arrow-right"])}"></div>'
    section = re.sub(r"<x-shape[^>]*>\s*</x-shape>", forme, section)
    traits = []; couleurs = set()
    def conn(m):
        tag = m.group(0)
        x1, y1, x2, y2 = (float(attr(tag, k, "0")) for k in ("x1", "y1", "x2", "y2"))
        route, head = attr(tag, "route"), attr(tag, "head", "end")
        pts = [(x1, y1), (x1, y2), (x2, y2)] if route == "vh" else [(x1, y1), (x2, y1), (x2, y2)] if route == "hv" else [(x1, y1), (x2, y2)]
        couleur = style_val(tag, "color", "#5F6B76"); w = style_val(tag, "border-width", "3px").replace("px", "")
        dash = ' stroke-dasharray="14 10"' if "dashed" in attr(tag, "style") else ""
        d = "M" + " L".join(f"{x:g} {y:g}" for x, y in pts)
        mid = "h" + re.sub(r"[^0-9A-Za-z]", "", couleur); couleurs.add(couleur)
        marks = (f' marker-end="url(#{mid})"' if head in ("end", "both") else "") + (f' marker-start="url(#{mid})"' if head == "both" else "")
        traits.append(f'<path d="{d}" fill="none" stroke="{couleur}" stroke-width="{w}"{dash}{marks}/>')
        return ""
    section = re.sub(r"<x-connector[^>]*>\s*</x-connector>", conn, section)
    if traits:
        defs = "<defs>" + "".join(f'<marker id="h{re.sub(chr(91)+"^0-9A-Za-z"+chr(93), "", c)}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="5" markerHeight="5" markerUnits="strokeWidth" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="{c}"/></marker>' for c in couleurs) + "</defs>"
        svg = f'<svg class="conns" viewBox="0 0 1920 1080" width="1920" height="1080">{defs}{"".join(traits)}</svg>'
        section = section.replace("</section>", svg + "</section>")
    return section.strip()

def titre(section):
    m = re.search(r"<h[12][^>]*>(.*?)</h[12]>", section, flags=re.S)
    return html.unescape(re.sub(r"<[^>]+>", "", m.group(1))).strip() if m else ""

def eyebrow(section):
    m = re.search(r'<p class="eyebrow[^"]*">(.*?)</p>', section, flags=re.S)
    return html.unescape(re.sub(r"<[^>]+>", "", m.group(1))).strip() if m else ""

def css_scope():
    """deck.css limité au conteneur .deck pour ne pas perturber le style du site."""
    css = (COURS / "deck.css").read_text(encoding="utf-8")
    out = []
    for bloc in css.split("}"):
        if "{" not in bloc: continue
        sel, corps = bloc.split("{", 1)
        sels = []
        for s in sel.split(","):
            s = s.strip()
            if not s: continue
            sels.append(".deck" if s == ":root" else ".deck " + s)
        out.append(",".join(sels) + "{" + corps + "}")
    base = (".deck section{width:1920px;height:1080px;box-sizing:border-box;position:relative;overflow:hidden;margin:0}"
            ".deck .xs{flex:none}.deck .conns{position:absolute;left:0;top:0;pointer-events:none}"
            ".deck{font-size:16px;line-height:normal;color:#16222E}.deck section::after{content:var(--pg,'')}")
    return "\n".join(out) + "\n" + base

def css_shadow():
    """deck.css pour un Shadow DOM : isolé du CSS du site (rien n'entre, rien ne sort)."""
    css = (COURS / "deck.css").read_text(encoding="utf-8")
    out = []
    for bloc in css.split("}"):
        if "{" not in bloc: continue
        sel, corps = bloc.split("{", 1)
        sels = [(":host" if s.strip() == ":root" else s.strip()) for s in sel.split(",") if s.strip()]
        out.append(",".join(sels) + "{" + corps + "}")
    base = (":host{all:initial;display:block;width:1920px;height:1080px;overflow:hidden;font-family:'Inter',Arial,sans-serif;color:#16222E;font-size:16px;line-height:normal}"
            "*{box-sizing:border-box}"
            "section{width:1920px;height:1080px;box-sizing:border-box;position:relative;overflow:hidden;margin:0}"
            ".xs{flex:none}.conns{position:absolute;left:0;top:0;pointer-events:none}"
            "section::after{content:var(--pg,'')}")
    return "\n".join(out) + "\n" + base + "\n"

def slides(cm):
    nom, a, b, _ = CMS[cm]
    order, sections = deck()
    debuts = {order.index(v["start"]): v["description"] for v in sections.values() if v["start"] in order}
    res = []
    for i in range(a, b):
        f = COURS / "slides" / f"{order[i]}.html"
        if not f.exists(): continue
        res.append({"i": i, "id": order[i], "html": convertir(f.read_text(encoding="utf-8")), "debut": debuts.get(i)})
    for s in res: s["titre"] = titre(s["html"]); s["eyebrow"] = eyebrow(s["html"])
    return res

def titre_index():
    """titre normalisé d'une slide -> (cm, id) pour relier les notions aux slides."""
    idx = {}
    for cm in CMS:
        for s in slides(cm):
            idx.setdefault(norm(s["titre"]), (cm, s["id"]))
    return idx

def trouver(idx, texte):
    """Retrouve la slide d'un intitulé de notion : titre exact, partie après « : », puis intitulé de chapitre."""
    brut = re.sub(r"^CM\d\s*[—-]\s*", "", texte)
    for cand in (brut, brut.split(" : ")[-1]):
        if norm(cand) in idx: return idx[norm(cand)]
    order, sections = deck()
    n = norm(brut)
    for v in sections.values():
        if n and n in norm(v["description"]) and v["start"] in order:
            i = order.index(v["start"])
            for cm, (_, a, b, _r) in CMS.items():
                if a <= i < b: return (cm, v["start"])
    return None

def norm(t):
    t = html.unescape(t).lower().replace("’", "'")
    return re.sub(r"[^a-z0-9àâçéèêëîïôûùüÿœ]+", " ", t).strip()
