#!/usr/bin/env python3
"""Génère le site (HTML statique) dans site/_build à partir de site/content et du code du dépôt.

Usage : python site/build.py          (depuis la racine du dépôt)
Dépendances : pip install markdown pygments
"""
import json, re, shutil, html, pathlib, datetime
import markdown
import sys
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import figures
from markdown.extensions.toc import slugify_unicode
from pygments.formatters import HtmlFormatter

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import cours as CM
SITE = ROOT / "site"
CONTENT = SITE / "content"
OUT = SITE / "_build"
GITHUB = "https://github.com/nverstaevel/M2IPMAgenticAI_Student"
SITE_URL = "https://nverstaevel.github.io/M2IPMAgenticAI_Student/"

TDS = {
    "td1": ("TD1", "Application Spring Boot classique", "td1-clinique", "TD1-clinique.md", "La clinique, sans IA"),
    "td2": ("TD2", "Un chat qui interroge l'application", "td2-chat", "TD2-chat.md", "Le Dr Gribouille et ses outils internes"),
    "td3": ("TD3", "Outils externes et contexte", "td3-outils-externes", "TD3-outils-externes.md", "Des outils externes et un tableau de mesures"),
    "td4": ("TD4", "Agent ReAct", "td4-agent-react", "TD4-agent-react.md", "Une boucle d'agent et ses traces commentées"),
    "td5": ("TD5", "Agents à rôles", "td5-agents-roles", "TD5-agents-roles.md", "Un duo vétérinaire / gardien et un dossier PDF"),
    "td6": ("TD6", "Le jeu de la clinique en MCP", "td6-mcp-jeu", "TD6-jeu-mcp.md", "Des architectures d'agents comparées par leurs scores"),
}
SEANCES = [("CM1", None, "Appeler un modèle, mémoire, outils, confirmation des écritures"), ("TD1", "td1", None), ("TD2", "td2", None),
           ("CM2", None, "Sources externes, contexte, boucle d'agent (ReAct), rôles"), ("TD3", "td3", None), ("TD4", "td4", None),
           ("TD5", "td5", None), ("CM3", None, "Le protocole MCP et le jeu de la clinique"), ("TD6", "td6", None),
           ("CM4", None, "Évaluation de TD sur machine (1 h)")]

def esc(s): return html.escape(s, quote=True)

def md_render(text, toc=True):
    exts = ["tables", "fenced_code", "sane_lists", "attr_list", "md_in_html", "codehilite", "toc"]
    cfg = {"codehilite": {"guess_lang": False, "css_class": "codehilite"},
           "toc": {"slugify": slugify_unicode, "permalink": "#", "permalink_title": "Lien vers cette section", "toc_depth": "2-3"}}
    text = re.sub(r"^Q: (.*)\n^R: (.*)$", lambda mm: '<details class="qa" markdown="1"><summary>' + mm.group(1) + '</summary>\n\n' + mm.group(2) + '\n\n</details>\n', text, flags=re.M)
    text = re.sub(r"((?:^- .*\n)+)\{: \.(plus|moins) \}", lambda mm: '<div class="' + mm.group(2) + '" markdown="1">\n<p class="lbl">' + ("Avantages" if mm.group(2) == "plus" else "Limites") + '</p>\n\n' + mm.group(1) + '\n</div>\n', text, flags=re.M)
    m = markdown.Markdown(extensions=exts, extension_configs=cfg)
    h = m.convert(text)
    h = re.sub(r"<p>\{\{fig:(\w+)\}\}</p>", lambda mm: figures.render(mm.group(1)), h)
    return h, getattr(m, "toc_tokens", [])

def inline(text):
    h = markdown.markdown(text, extensions=["sane_lists"])
    return re.sub(r"^<p>(.*)</p>$", r"\1", h.strip(), flags=re.S)

def front(path):
    t = path.read_text(encoding="utf-8")
    meta = {}
    if t.startswith("---"):
        _, fm, body = t.split("---", 2)
        for ligne in fm.strip().splitlines():
            k, _, v = ligne.partition(":"); meta[k.strip()] = v.strip()
        t = body
    return meta, t.strip()

def lst(v): return [x.strip() for x in v.split(",") if x.strip()]

# ----------------------------------------------------------------------------- contenu
NOTIONS, RECETTES = {}, {}
for p in sorted((CONTENT / "notions").glob("*.md")):
    meta, body = front(p); meta["id"] = p.stem; meta["body"] = body; NOTIONS[p.stem] = meta
for p in sorted((CONTENT / "recettes").glob("*.md")):
    meta, body = front(p); meta["id"] = p.stem; meta["body"] = body; RECETTES[p.stem] = meta
PATTERNS = {}
for p in sorted((CONTENT / "patterns").glob("*.md")):
    meta, body = front(p); meta["id"] = p.stem; meta["body"] = body; PATTERNS[p.stem] = meta
AIDES = json.loads((CONTENT / "aides.json").read_text(encoding="utf-8"))

# références croisées : où une notion / recette est utilisée
USAGE_N, USAGE_R = {k: [] for k in NOTIONS}, {k: [] for k in RECETTES}
for td, steps in AIDES.items():
    for step, a in steps.items():
        for n in a.get("notions", []): USAGE_N[n].append((td, step))
        for r in a.get("recettes", []): USAGE_R[r].append((td, step))
RECETTES_PAR_NOTION = {k: [] for k in NOTIONS}
for rid, r in RECETTES.items():
    for n in lst(r.get("notions", "")): RECETTES_PAR_NOTION.setdefault(n, []).append(rid)

SEARCH = []
def add_search(titre, url, texte, genre):
    texte = re.sub(r"```.*?```", " ", texte, flags=re.S)
    texte = re.sub(r"[#>*`|\[\]()_-]+", " ", texte)
    SEARCH.append({"t": titre, "u": url, "g": genre, "x": re.sub(r"\s+", " ", texte)[:1500]})

# ----------------------------------------------------------------------------- code : cartes des TODO
def scan_todos():
    res = {k: [] for k in TDS}
    for td, (_, _, dossier, _, _) in TDS.items():
        base = ROOT / dossier
        for f in sorted(base.rglob("*.java")):
            for i, l in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
                if "TODO TD" in l:
                    txt = re.sub(r"^\W*(//|/\*+|\*)?\s*TODO\s*TD\d\s*(\([^)]*\))?\s*:?\s*", "", l.strip()).rstrip("*/ ").strip()
                    ctx = re.search(r"\(([^)]*)\)", l)
                    res[td].append({"fichier": str(f.relative_to(ROOT)), "ligne": i, "texte": txt or "à écrire",
                                    "etape": ctx.group(1) if ctx and "étape" in ctx.group(1) else ""})
    return res
TODOS = scan_todos()

# ----------------------------------------------------------------------------- gabarit
NAV = [("index.html", "Accueil"), ("notions/index.html", "Notions"), ("recettes/index.html", "Recettes"), ("architectures/index.html", "Architectures"), ("cours/index.html", "Cours"), ("code.html", "Code"),
       ("depannage.html", "Dépannage"), ("ressources.html", "Ressources"), ("commun.html", "Infos")]

def page(rel, titre, corps, side="", extra_head="", td_ctx=None):
    depth = rel.count("/")
    root = "../" * depth
    tdmenu = "".join(f'<a href="{root}td/{k}.html">{v[0]} · {esc(v[1])}</a>' for k, v in TDS.items())
    nav = "".join(f'<a href="{root}{h}"{" aria-current=page" if h == rel else ""}>{t}</a>' for h, t in NAV[:1])
    nav += f'<details class="menu"><summary>TD</summary><div>{tdmenu}</div></details>'
    nav += "".join(f'<a href="{root}{h}"{" aria-current=page" if h == rel else ""}>{t}</a>' for h, t in NAV[1:])
    doc = f"""<!doctype html>
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{esc(titre)} · IA agentiques — aide aux TD</title>
<meta name="description" content="Aide à la réalisation des TD « IA agentiques avec Spring AI » (M2 MIAGE IPM)">
<script>try{{var t=localStorage.getItem('m2ipm:theme');if(t)document.documentElement.dataset.theme=t;}}catch(e){{}}</script>
<link rel="icon" href="data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 100 100%27%3E%3Ctext y=%27.9em%27 font-size=%2790%27%3E🐉%3C/text%3E%3C/svg%3E"><link rel="stylesheet" href="{root}assets/site.css"><link rel="stylesheet" href="{root}assets/pygments.css">{extra_head}
</head><body data-root="{root}"{f' data-td="{td_ctx}"' if td_ctx else ''}>
<a class="skip" href="#contenu">Aller au contenu</a>
<header class="top"><a class="brand" href="{root}index.html"><span class="logo">🐉</span><span>Clinique des créatures<small>IA agentiques · aide aux TD</small></span></a>
<nav>{nav}</nav>
<div class="tools"><button id="btn-search" title="Rechercher (touche /)" aria-label="Rechercher">🔍</button><button id="btn-theme" title="Thème clair / sombre" aria-label="Changer de thème">🌓</button></div></header>
<div id="search" hidden><div class="box"><input id="q" type="search" placeholder="Rechercher : outil, mémoire, MCP, 403…" autocomplete="off"><ul id="res"></ul><p class="hint">Échap pour fermer</p></div></div>
<div class="layout{' with-side' if side else ''}">{f'<aside class="side">{side}</aside>' if side else ''}<main id="contenu">{corps}</main></div>
<footer>Cours « IA agentiques avec Spring AI » · M2 MIAGE IPM · Université Toulouse Capitole ·
<a href="{GITHUB}">dépôt étudiant</a> · Version du {datetime.date.today().strftime('%d/%m/%Y')}</footer>
<script src="{root}assets/site.js" defer></script></body></html>"""
    p = OUT / rel; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(doc, encoding="utf-8")

def chips(ids, kind, root):
    base = {"n": "notions", "r": "recettes"}[kind]; src = NOTIONS if kind == "n" else RECETTES
    return "".join(f'<a class="chip {kind}" href="{root}{base}/{i}.html">{esc(src[i]["title"])}</a>' for i in ids if i in src)

def toc_html(tokens):
    out = ["<nav class='toc' aria-label='Sommaire'><p class='toc-t'>Sur cette page</p><ul>"]
    for t in tokens:
        out.append(f"<li><a href='#{t['id']}'>{esc(re.sub(r'<[^>]+>','',t['name']))}</a>")
        if t.get("children"):
            out.append("<ul>" + "".join(f"<li><a href='#{c['id']}'>{esc(c['name'])}</a></li>" for c in t["children"]) + "</ul>")
        out.append("</li>")
    out.append("</ul></nav>")
    return "".join(out)

# ----------------------------------------------------------------------------- pages TD
def step_key(titre):
    m = re.match(r"Étape (\d+)", titre)
    if m: return m.group(1)
    if titre.startswith("Avant la séance — "): return "avant"
    return None

def build_td(td):
    num, theme, dossier, fichier, livrable = TDS[td]
    meta_txt = (CONTENT / "sujets" / fichier).read_text(encoding="utf-8")
    body_md = re.sub(r"^# .*\n", "", meta_txt, count=1)
    h, tokens = md_render(body_md)
    root = "../"
    # cases à cocher
    n_chk = [0]
    def chk(m):
        n_chk[0] += 1
        return f'<li class="task"><label><input type="checkbox" data-k="{td}-c{n_chk[0]}"> {m.group(1)}</label></li>'
    h = re.sub(r"<li>\[ \] (.*?)</li>", chk, h, flags=re.S)
    # étapes : bloc d'aide + point de contrôle
    morceaux = re.split(r"(?=<h[23] id=)", h)
    out, n_steps = [], 0
    for ch in morceaux:
        m = re.match(r"<h3 id=\"([^\"]+)\">(.*?)<a class=\"headerlink\"", ch, flags=re.S)
        if m:
            k = step_key(re.sub(r"<[^>]+>", "", m.group(2)).strip())
            a = AIDES.get(td, {}).get(k) if k else None
            if a:
                n_steps += 1
                ind = "".join(f'<details class="indice"><summary>Indice {i}</summary><div>{inline(x)}</div></details>'
                              for i, x in enumerate(a.get("indices", []), 1))
                bloc = (f'<div class="aide"><p class="aide-t">🧭 Besoin d\'aide pour cette étape ?</p>{ind}'
                        + (f'<p class="aide-l"><span>Notions :</span> {chips(a.get("notions", []), "n", root)}</p>' if a.get("notions") else "")
                        + (f'<p class="aide-l"><span>Recettes :</span> {chips(a.get("recettes", []), "r", root)}</p>' if a.get("recettes") else "")
                        + "</div>")
                ch = re.sub(r"(</h3>)", r"\1" + bloc.replace("\\", "\\\\"), ch, count=1)
                ch += f'<p class="ctl"><label><input type="checkbox" data-k="{td}-s{k}"> Point de contrôle validé</label></p>'
        out.append(ch)
    h = "".join(out)
    total = n_chk[0] + n_steps
    # fiche « dossier de départ »
    todos = TODOS[td]
    par_fichier = {}
    for t in todos: par_fichier.setdefault(t["fichier"], []).append(t)
    def li_fichier(f, ts):
        court = f.split("/src/main/java/fr/m2ipm/clinique/")[-1]
        sous = "".join(
            '<li><a href="%s/blob/main/%s#L%d">l.%d</a> %s</li>' % (GITHUB, f, t["ligne"], t["ligne"], esc(t["texte"][:150]))
            for t in ts)
        return ('<li><a href="%s/blob/main/%s#L%d"><code>%s</code></a> <span class="cnt">%d point%s</span><ul>%s</ul></li>'
                % (GITHUB, f, ts[0]["ligne"], esc(court), len(ts), "s" if len(ts) > 1 else "", sous))
    lignes = "".join(li_fichier(f, ts) for f, ts in par_fichier.items())
    fiche = (f'<section class="depart"><h2 class="nodot">Dossier de départ</h2>'
             f'<p><a class="btn" href="{GITHUB}/tree/main/{dossier}">📁 Ouvrir <code>{dossier}</code> sur GitHub</a> '
             f'<a class="btn ghost" href="{GITHUB}/blob/main/{dossier}/README.md">README du dossier</a></p>'
             + (f'<details><summary><strong>{len(todos)} marqueurs <code>TODO {num}</code></strong> dans {len(par_fichier)} fichiers</summary><ul class="todos">{lignes}</ul>'
                f'<p class="note">Dans Eclipse : vue <em>Tasks</em> ; IntelliJ : fenêtre <em>TODO</em>. Les tests des squelettes échouent tant que les TODO ne sont pas traités : c\'est normal.</p></details>'
                if todos else '<p>Pas de squelette à compléter : vous écrivez votre code dans le dossier.</p>')
             + "</section>")
    prec = list(TDS)[list(TDS).index(td) - 1] if td != "td1" else None
    suiv = list(TDS)[list(TDS).index(td) + 1] if td != "td6" else None
    pager = '<nav class="pager">' + (f'<a href="{prec}.html">← {TDS[prec][0]} · {esc(TDS[prec][1])}</a>' if prec else "<span></span>") \
            + (f'<a href="{suiv}.html">{TDS[suiv][0]} · {esc(TDS[suiv][1])} →</a>' if suiv else "<span></span>") + "</nav>"
    pats_td = PAT_PAR_TD.get(td, [])
    chips_p = ('<p class="pats">🧩 Architectures de ce TD : ' + "".join(f'<a class="chip p" href="../architectures/{q}.html">{esc(PATTERNS[q]["title"])}</a>' for q in pats_td) + '</p>') if pats_td else ''
    head = (f'<header class="td-head"><p class="kicker">{num} · séance de 3 h · seul ou à deux</p><h1>{esc(theme)}</h1>'
            f'<p class="livrable"><strong>Vous livrez :</strong> {esc(livrable)}</p>{chips_p}'
            f'<div class="progress" data-prefix="{td}-" data-total="{total}"><div class="bar"><i></i></div><span class="pct">0 %</span></div></header>')
    corps = head + fiche + f'<article class="sujet">{h}</article>' + pager
    side = toc_html(tokens)
    page(f"td/{td}.html", f"{num} — {theme}", corps, side=side, td_ctx=td)
    add_search(f"{num} — {theme}", f"td/{td}.html", body_md, "TD")
    return total

# ----------------------------------------------------------------------------- notions, recettes, patterns
def usage_html(uses, root):
    if not uses: return ""
    items = "".join(f'<li><a href="{root}td/{td}.html">{TDS[td][0]}</a>{" · étape " + s if s.isdigit() else (" · avant la séance" if s == "avant" else "")}</li>' for td, s in uses)
    return f'<ul class="usage">{items}</ul>'

PAT_PAR_NOTION = {k: [] for k in NOTIONS}; PAT_PAR_RECETTE = {k: [] for k in RECETTES}; PAT_PAR_TD = {k: [] for k in TDS}
for pid, p in PATTERNS.items():
    for n in lst(p.get("notions", "")): PAT_PAR_NOTION.setdefault(n, []).append(pid)
    for r in lst(p.get("recettes", "")): PAT_PAR_RECETTE.setdefault(r, []).append(pid)
    for t in lst(p.get("td", "")): PAT_PAR_TD.setdefault(t, []).append(pid)

def liens_recettes(ids, root):
    return '<div class="liens">' + "".join(
        f'<a href="{root}recettes/{r}.html"><b>🍳 {esc(RECETTES[r]["title"])}</b><span>{esc(RECETTES[r].get("resume", ""))}</span></a>' for r in ids if r in RECETTES) + "</div>"

def liens_patterns(ids, root):
    return '<div class="liens">' + "".join(
        f'<a href="{root}architectures/{p}.html"><b>🧩 {esc(PATTERNS[p]["title"])}</b><span>{esc(PATTERNS[p].get("resume", ""))}</span></a>' for p in ids if p in PATTERNS) + "</div>"

_IDX = None
def lien_slide(texte, root="../"):
    """Un titre de notion « CM1 — Titre » devient un lien vers la diapositive du même titre, s'il existe."""
    global _IDX
    if _IDX is None: _IDX = CM.titre_index()
    hit = CM.trouver(_IDX, texte)
    if hit: return f'<a href="{root}cours/{hit[0]}.html#{hit[1]}">{esc(texte)}</a>'
    m = re.match(r"CM(\d)", texte)
    if m: return f'<a href="{root}cours/cm{m.group(1)}.html">{esc(texte)}</a>'
    return esc(texte)

def build_notions():
    cards = []
    for nid, n in NOTIONS.items():
        h, _ = md_render(n["body"])
        tds = sorted({td for td, _ in USAGE_N[nid]} | set(lst(n.get("td", ""))))
        rec = RECETTES_PAR_NOTION.get(nid, [])
        if not rec: print("ATTENTION : notion sans recette :", nid)
        cm = "".join(f"<li>{lien_slide(x.strip())}</li>" for x in re.split(r"\s*;\s*", n.get("cm", "")) if x.strip())
        haut = (f'<p class="aller">👉 <strong>Passer à la pratique :</strong> {chips(rec, "r", "../")}</p>' if rec else "")
        pratique = (f'<section class="pratique"><h2>Passer à la pratique</h2><p>Vous avez compris la notion ? Mettez-la en œuvre avec :</p>{liens_recettes(rec, "../")}</section>' if rec else "")
        pats = PAT_PAR_NOTION.get(nid, [])
        patblock = (f'<section class="card"><h2 class="nodot">Architectures qui s\'appuient sur cette notion</h2>{liens_patterns(pats, "../")}</section>' if pats else "")
        corps = (f'<p class="kicker"><a href="index.html">Notions</a></p><h1>{esc(n["title"])}</h1><p class="lead">{esc(n.get("resume",""))}</p>{haut}'
                 f'<div class="grid2"><section class="card"><h2 class="nodot">Dans le cours</h2><ul>{cm}</ul>'
                 f'<p class="note">Les titres cliquables ouvrent la diapositive correspondante dans la page <a href="../cours/index.html">Cours</a>.</p></section>'
                 f'<section class="card"><h2 class="nodot">Où le retrouver en TD</h2>{usage_html(USAGE_N[nid], "../") or ""}</section></div>'
                 f'<article class="prose">{h}</article>{pratique}{patblock}')
        page(f"notions/{nid}.html", n["title"], corps)
        add_search(n["title"], f"notions/{nid}.html", n["body"] + " " + n.get("resume", ""), "Notion")
        cards.append((nid, n, tds, rec))
    ordre = [("td1", "Spring Boot, en rappel (TD1)"), ("td2", "Parler à un modèle (TD2)"), ("td3", "Sources externes et contexte (TD3)"),
             ("td4", "Boucle d'agent (TD4)"), ("td5", "Agents à rôles (TD5)"), ("td6", "MCP et évaluation (TD6)")]
    sect = []
    for td, titre in ordre:
        cs = [c for c in cards if (c[2][0] if c[2] else "td6") == td]
        if not cs: continue
        sect.append(f'<h2 id="{td}">{titre}</h2><div class="cards">' + "".join(
            f'<a class="card link" href="{nid}.html"><h3>{esc(n["title"])}</h3><p>{esc(n.get("resume",""))}</p>'
            f'<p class="tags">{"".join(f"<span>{TDS[t][0]}</span>" for t in tds)}<span class="rec">🍳 {len(rec)} recette{"s" if len(rec) > 1 else ""}</span></p></a>' for nid, n, tds, rec in cs) + "</div>")
    page("notions/index.html", "Notions du cours",
         '<h1>Notions du cours</h1><p class="lead">Chaque fiche explique une notion avec un schéma, un exemple de la clinique, les erreurs fréquentes et un auto-test, puis vous envoie vers les <strong>recettes</strong> qui la mettent en pratique.</p>'
         '<p class="note">Parcours conseillé : <strong>notion</strong> (comprendre) → <strong>recette</strong> (faire) → <strong>architecture</strong> (savoir quand l\'utiliser).</p>' + "".join(sect))

def build_recettes():
    for rid, r in RECETTES.items():
        h, _ = md_render(r["body"])
        notions = [n for n in lst(r.get("notions", "")) if n in NOTIONS]
        if len(notions) < 2: print("ATTENTION : recette avec moins de 2 notions :", rid)
        mob = ('<section class="mobilise"><h2>🧠 Notions mobilisées</h2><ul>' + "".join(
            f'<li><a href="../notions/{n}.html"><strong>{esc(NOTIONS[n]["title"])}</strong></a> — {esc(NOTIONS[n].get("resume", ""))}</li>' for n in notions) + "</ul></section>") if notions else ""
        pats = PAT_PAR_RECETTE.get(rid, [])
        patblock = f'<section class="card"><h2 class="nodot">Architectures où cette recette sert</h2>{liens_patterns(pats, "../")}</section>' if pats else ""
        corps = (f'<p class="kicker"><a href="index.html">Recettes</a> · {" ".join(TDS[t][0] for t in lst(r.get("td","")) if t in TDS)}</p>'
                 f'<h1>{esc(r["title"])}</h1><p class="lead">{esc(r.get("resume",""))}</p>{mob}'
                 f'<article class="prose">{h}</article>{patblock}'
                 + (f'<section class="card"><h2 class="nodot">Utilisée dans</h2>{usage_html(USAGE_R[rid], "../")}</section>' if USAGE_R[rid] else ""))
        page(f"recettes/{rid}.html", r["title"], corps)
        add_search(r["title"], f"recettes/{rid}.html", r["body"] + " " + r.get("resume", ""), "Recette")
    ordre = sorted(RECETTES.items(), key=lambda kv: (min([int(t[2:]) for t in lst(kv[1].get("td", "td9")) if t.startswith("td")] or [9]), kv[1]["title"]))
    cards = "".join(f'<a class="card link" href="{rid}.html"><h3>{esc(r["title"])}</h3><p>{esc(r.get("resume",""))}</p>'
                    f'<p class="tags">{"".join(f"<span>{TDS[t][0]}</span>" for t in lst(r.get("td","")) if t in TDS)}'
                    f'<span class="rec">🧠 {len([n for n in lst(r.get("notions","")) if n in NOTIONS])} notions</span></p></a>' for rid, r in ordre)
    page("recettes/index.html", "Recettes",
         '<h1>Recettes</h1><p class="lead">Des marches à suivre courtes : objectif, ingrédients, squelette de code, vérification, pièges. Chaque recette dit <strong>quelles notions elle mobilise</strong>. Les exemples sont <strong>hors clinique</strong> quand ils touchent à ce que vous devez écrire : ils montrent le geste, pas la solution.</p>'
         f'<div class="cards">{cards}</div>')

FAMILLES = [("Outils et sources", "Donner au modèle de quoi lire ou agir"), ("Humain dans la boucle", "Garder la main sur les écritures"),
            ("Enchaînements et boucles", "Du workflow fixe à l'agent qui décide"), ("Plusieurs agents", "Répartir les rôles"), ("Interopérabilité", "Brancher des outils venus d'ailleurs")]

def build_patterns():
    comp = []
    for pid, p in PATTERNS.items():
        h, _ = md_render(p["body"])
        niv = int(p.get("niveau", "1") or 1)
        tds = lst(p.get("td", ""))
        notions = [n for n in lst(p.get("notions", "")) if n in NOTIONS]; recs = [r for r in lst(p.get("recettes", "")) if r in RECETTES]
        meta = (f'<div class="pattern-meta"><div><small>Famille</small>{esc(p.get("famille", ""))}</div><div><small>Complexité</small><span class="niv">{"●" * niv}{"○" * (3 - niv)}</span></div>'
                f'<div><small>Coût en tokens</small>{esc(p.get("cout", ""))}</div><div><small>Qui décide ?</small>{esc(p.get("controle", ""))}</div>'
                f'<div><small>Vu en</small>{" ".join(f"<a href=\"../td/{t}.html\">{TDS[t][0]}</a>" for t in tds if t in TDS)}</div></div>')
        aka = f'<p class="note">Aussi appelé : {esc(p["aka"])}</p>' if p.get("aka") else ""
        bas = (f'<section class="mobilise"><h2>🧠 Notions à connaître</h2><ul>' + "".join(
            f'<li><a href="../notions/{n}.html"><strong>{esc(NOTIONS[n]["title"])}</strong></a> — {esc(NOTIONS[n].get("resume",""))}</li>' for n in notions) + "</ul></section>") if notions else ""
        pra = f'<section class="pratique"><h2>Pour l\'implémenter</h2>{liens_recettes(recs, "../")}</section>' if recs else ""
        autres = [q for q in PATTERNS if q != pid and (set(lst(PATTERNS[q].get("notions", ""))) & set(lst(p.get("notions", ""))))][:4]
        voisins = f'<section class="card"><h2 class="nodot">Patterns voisins</h2>{liens_patterns(autres, "../")}</section>' if autres else ""
        corps = (f'<p class="kicker"><a href="index.html">Architectures</a> · design pattern</p><h1>{esc(p["title"])}</h1>{aka}<p class="lead">{esc(p.get("resume",""))}</p>{meta}'
                 f'<article class="prose">{h}</article>{bas}{pra}{voisins}')
        page(f"architectures/{pid}.html", p["title"], corps)
        add_search(p["title"] + " (design pattern)", f"architectures/{pid}.html", p["body"] + " " + p.get("resume", "") + " " + p.get("aka", ""), "Architecture")
        comp.append((pid, p, niv, tds))
    lignes = "".join(f'<tr><td><a href="{pid}.html"><strong>{esc(p["title"])}</strong></a></td><td>{esc(p.get("probleme", p.get("resume", "")))}</td><td>{esc(p.get("controle", ""))}</td><td>{esc(p.get("cout", ""))}</td>'
                     f'<td>{" ".join(TDS[t][0] for t in tds if t in TDS)}</td></tr>' for pid, p, niv, tds in comp)
    sect = ""
    for fam, sous in FAMILLES:
        cs = [c for c in comp if c[1].get("famille") == fam]
        if not cs: continue
        sect += f'<h2>{fam}</h2><p class="note">{sous}</p><div class="cards">' + "".join(
            f'<a class="card link" href="{pid}.html"><h3>{esc(p["title"])}</h3><p>{esc(p.get("resume", ""))}</p><p class="tags"><span class="niv">{"●" * niv}{"○" * (3 - niv)}</span>{"".join(f"<span>{TDS[t][0]}</span>" for t in tds if t in TDS)}</p></a>'
            for pid, p, niv, tds in cs) + "</div>"
    idx = ('<h1>Architectures d\'agents</h1><p class="lead">Les montages que nous explorons dans le cours, présentés comme des <strong>design patterns</strong> : une intention, un problème, une structure, une dynamique, des conséquences. Un pattern n\'est pas une recette de code : c\'est un choix d\'architecture que vous justifierez avec des mesures.</p>'
           '\n\n{{fig:spectre}}\n\n'
           '<h2>Choisir son pattern</h2><table><thead><tr><th>Votre besoin</th><th>Commencez par</th></tr></thead><tbody>'
           '<tr><td>Le modèle doit consulter ou modifier des données de l\'application</td><td><a href="assistant-a-outils.html">Assistant à outils</a></td></tr>'
           '<tr><td>Transformer un texte libre en décision exploitable par le code</td><td><a href="triage-structure.html">Triage structuré</a></td></tr>'
           '<tr><td>Une écriture ne doit jamais partir sans validation humaine</td><td><a href="confirmation-humaine.html">Proposer, puis confirmer</a></td></tr>'
           '<tr><td>Une source externe (API, agenda) rend les tests fragiles</td><td><a href="port-et-adaptateur.html">Port et adaptateur</a></td></tr>'
           '<tr><td>Un gros corpus ne tient pas, ou coûte trop cher, dans le prompt</td><td><a href="outil-de-recherche.html">Outil de recherche</a></td></tr>'
           '<tr><td>Le déroulé est connu à l\'avance</td><td><a href="chaine-de-prompts.html">Chaîne de prompts</a> (workflow)</td></tr>'
           '<tr><td>Le déroulé dépend de ce que l\'on découvre en route</td><td><a href="agent-react.html">Agent ReAct</a>, <strong>avec</strong> <a href="boucle-bornee.html">boucle bornée</a></td></tr>'
           '<tr><td>On veut limiter les erreurs d\'un agent</td><td><a href="acteur-critique.html">Acteur–critique</a></td></tr>'
           '<tr><td>Une tâche se découpe en sous-tâches indépendantes</td><td><a href="superviseur-travailleurs.html">Superviseur–travailleurs</a></td></tr>'
           '<tr><td>Un agent lit des textes écrits par des tiers</td><td><a href="moindre-privilege.html">Séparation des rôles et moindre privilège</a></td></tr>'
           '<tr><td>Les outils sont fournis par un autre programme</td><td><a href="serveur-d-outils-mcp.html">Serveur d\'outils (MCP)</a></td></tr>'
           '</tbody></table>' + sect +
           '<h2>Comparer d\'un coup d\'œil</h2><table><thead><tr><th>Pattern</th><th>Problème résolu</th><th>Qui décide ?</th><th>Coût en tokens</th><th>Vu en</th></tr></thead><tbody>' + lignes + '</tbody></table>'
           '<div class="callout"><strong>Règle d\'ingénieur.</strong> Prenez l\'architecture la plus simple qui résout le problème, puis <strong>mesurez</strong> avant d\'en changer. Au TD5, le workflow classique est la référence à laquelle comparer les agents ; au TD6, le score du jeu fait foi.</div>')
    h, _ = md_render(idx)
    page("architectures/index.html", "Architectures d'agents", h)

# ----------------------------------------------------------------------------- pages transverses
def collecter_sections(titre_h2):
    res = []
    for td, (num, theme, _, fichier, _) in TDS.items():
        t = (CONTENT / "sujets" / fichier).read_text(encoding="utf-8")
        m = re.search(rf"^## {titre_h2}\n(.*?)(?=^## |\Z)", t, flags=re.S | re.M)
        if m: res.append((td, num, theme, m.group(1).strip()))
    return res

def build_depannage():
    parts = ['<h1>Dépannage</h1><p class="lead">Les symptômes classiques de chaque TD, rassemblés. Commencez par la <a href="recettes/diagnostiquer-une-panne.html">recette de diagnostic en cinq minutes</a>.</p>']
    parts.append('<div class="card"><h2 class="nodot">Les trois vérifications universelles</h2><ol><li><code>curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele</code> doit répondre <code>"statut":"OK"</code>.</li>'
                 '<li>Le profil attendu est actif (log de démarrage : <em>The following profiles are active</em>).</li>'
                 '<li>Reproduisez avec le profil <code>simule</code> : si ça marche, c\'est le modèle ; sinon, c\'est votre code.</li></ol></div>')
    for td, num, theme, txt in collecter_sections("En cas de blocage"):
        h, _ = md_render(txt)
        parts.append(f'<section id="{td}"><h2><a href="td/{td}.html">{num}</a> · {esc(theme)}</h2>{h}</section>')
        add_search(f"Dépannage {num}", f"depannage.html#{td}", txt, "Dépannage")
    page("depannage.html", "Dépannage", "".join(parts))

def build_ressources():
    parts = ['<h1>Ressources</h1><p class="lead">Documentations et lectures citées dans les sujets, par TD. Les extraits de code des sujets sont des repères : <strong>la documentation de la version fournie fait foi</strong>.</p>']
    for td, num, theme, txt in collecter_sections("Ressources"):
        h, _ = md_render(txt)
        parts.append(f'<section id="{td}"><h2><a href="td/{td}.html">{num}</a> · {esc(theme)}</h2>{h}</section>')
        add_search(f"Ressources {num}", f"ressources.html#{td}", txt, "Ressource")
    page("ressources.html", "Ressources", "".join(parts))

def build_cours():
    (OUT / "cours").mkdir(parents=True, exist_ok=True)
    (OUT / "assets" / "deck.css").write_text(CM.css_shadow(), encoding="utf-8")
    cartes = []
    for cm, (nom, a, b_, resume) in CM.CMS.items():
        sl = CM.slides(cm)
        pdf = ROOT / "cours" / f"{nom}.pdf"
        if pdf.exists(): shutil.copy(pdf, OUT / "cours" / pdf.name)
        btn_pdf = f'<a class="btn" href="{nom}.pdf">Télécharger le PDF ({pdf.stat().st_size // 1024} Ko)</a> ' if pdf.exists() else ""
        index, corps = [], []
        for s in sl:
            if s["debut"]:
                index.append(f'<li><a href="#{s["id"]}">{esc(s["debut"])}</a></li>')
                corps.append(f'<h2 class="sect">{esc(s["debut"])}</h2>')
            corps.append(f'<figure class="slide" id="{s["id"]}" data-n="{s["i"] + 1}"><div class="stage"><div class="deck" style="--pg:&quot;{s['i'] + 1}&quot;"><template shadowrootmode="open"><link rel="stylesheet" href="../assets/deck.css">{s["html"]}</template></div></div>'
                         f'<figcaption><a href="#{s["id"]}">#{s["i"] + 1}</a> · {esc(s["titre"] or s["id"])}</figcaption></figure>')
            add_search(f"{nom} · {s['titre'] or s['id']}", f"cours/{cm}.html#{s['id']}", s["eyebrow"] + " " + re.sub(r"<[^>]+>", " ", s["html"]), "Cours")
        side = f'<p class="sidetitle">{nom}</p><ul>{"".join(index)}</ul>'
        entete = (f'<p class="kicker"><a href="index.html">Cours</a></p><h1>{nom} · {esc(resume)}</h1>'
                  f'<p class="lead">{len(sl)} diapositives. Cliquez sur une diapositive pour la lire en plein écran (flèches ← → pour naviguer, Échap pour fermer).</p>'
                  f'<p>{btn_pdf}<a class="btn ghost" href="{GITHUB}/tree/main/cours">Sources sur GitHub</a></p>')
        page(f"cours/{cm}.html", nom, entete + "".join(corps), side=side,
             extra_head='<link rel="preconnect" href="https://fonts.googleapis.com">'
                        '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400..700&family=Inter:wght@400;600&family=JetBrains+Mono:wght@400;600&display=swap">'
                        '<script src="../assets/cours.js" defer></script>')
        cartes.append(f'<a class="card pick" href="{cm}.html"><h3>{nom}</h3><p>{esc(resume)}</p><p class="note">{len(sl)} diapositives</p></a>')
    page("cours/index.html", "Cours",
         '<h1>Cours</h1><p class="lead">Les diapositives des trois cours magistraux, à relire avant un TD ou pour retrouver une notion. Les notions du site renvoient directement aux diapositives concernées.</p>'
         f'<div class="grid3">{"".join(cartes)}</div>'
         '<p class="note">Les PDF sont aussi dans le dossier <code>cours/</code> du dépôt étudiant, avec les sources des diapositives.</p>')

def build_commun():
    t = (CONTENT / "sujets" / "00-informations-communes.md").read_text(encoding="utf-8")
    h, tokens = md_render(re.sub(r"^# .*\n", "", t, count=1))
    page("commun.html", "Informations communes", f'<h1>Informations communes</h1><article class="sujet">{h}</article>', side=toc_html(tokens))
    add_search("Informations communes (organisation, outils, Git, sécurité)", "commun.html", t, "Infos")

def build_code():
    lignes = []
    for td, (num, theme, dossier, fichier, _) in TDS.items():
        t = (CONTENT / "sujets" / fichier).read_text(encoding="utf-8")
        m = re.search(r"^\| Vous écrivez .*?\n\| ?-+.*?\n\| (.*?) \| (.*?) \|\s*$", t, flags=re.S | re.M)
        ecrit, fourni = (inline(m.group(1)), inline(m.group(2))) if m else ("", "")
        nb = len(TODOS[td])
        lignes.append(f'<section class="card" id="{td}"><h2 class="nodot"><a href="td/{td}.html">{num}</a> · <code>{dossier}</code></h2>'
                      f'<p><a class="btn" href="{GITHUB}/tree/main/{dossier}">Ouvrir sur GitHub</a> '
                      f'<span class="cnt">{nb} marqueurs TODO</span></p>'
                      f'<div class="grid2"><div><h3>Vous écrivez</h3><p>{ecrit}</p></div><div><h3>Fourni (ne pas modifier)</h3><p>{fourni}</p></div></div></section>')
    page("code.html", "Carte du code",
         '<h1>Carte du code</h1><p class="lead">Ce que contient chaque dossier de départ : ce que vous écrivez, ce qui est fourni. Chaque dossier est le corrigé du TD précédent, le code fourni du TD et des squelettes à compléter.</p>'
         '<div class="card"><h2 class="nodot">Mettre en place son dossier</h2><ol><li>Récupérez le dépôt étudiant (clone Git recommandé, ou téléchargement en ZIP : Git n\'est pas obligatoire) et ouvrez <strong>un dossier de TD</strong> dans l\'IDE (pas la racine).</li>'
         '<li>Lancez : <code>mvn spring-boot:run -Dspring-boot.run.profiles=…</code></li><li>Cherchez les marqueurs <code>// TODO TDn</code> (Eclipse : vue <em>Tasks</em> ; IntelliJ : fenêtre <em>TODO</em>).</li>'
         '<li>En retard ? Repartez du dossier du TD suivant : rien n\'est perdu.</li></ol></div>' + "".join(lignes))
    add_search("Carte du code : dossiers de départ, TODO", "code.html", "dossier de départ todo squelette fourni à écrire", "Code")

def build_index(totaux):
    cards = []
    for nom, td, resume in SEANCES:
        if td:
            num, theme, _, _, livrable = TDS[td]
            cards.append(f'<a class="seance td" href="td/{td}.html" data-prefix="{td}-" data-total="{totaux[td]}"><span class="s-n">{num}</span><strong>{esc(theme)}</strong>'
                         f'<span class="s-l">{esc(livrable)}</span><span class="bar"><i></i></span><span class="pct">0 %</span></a>')
        else:
            cm = nom.lower()
            lien = f'cours/{cm}.html' if nom != "CM4" else "commun.html"
            cards.append(f'<a class="seance cm" href="{lien}"><span class="s-n">{nom}</span><strong>Cours magistral</strong><span class="s-l">{esc(resume)}</span></a>')
    corps = f"""<section class="hero"><p class="kicker">M2 MIAGE IPM · Université Toulouse Capitole</p>
<h1>Réussir les TD <em>IA agentiques</em> avec Spring AI</h1>
<p class="lead">Un seul endroit pour lire les sujets, retrouver la notion de cours qui manque, déplier un indice quand vous bloquez, et copier une recette qui marche. Fil rouge : la clinique des créatures fantastiques du Dr Gribouille.</p>
<p><a class="btn" href="td/td1.html">Commencer par le TD1</a> <a class="btn ghost" href="depannage.html">J'ai une erreur</a> <a class="btn ghost" href="commun.html">Règles et organisation</a></p></section>
<h2>Votre parcours</h2><div class="parcours">{"".join(cards)}</div>
<p class="note">Votre progression (cases cochées, points de contrôle) est enregistrée dans <strong>ce navigateur</strong> uniquement.</p>
<h2>Comment utiliser ce site pendant un TD</h2>
<div class="cards four">
<div class="card"><h3>1 · Lisez le sujet</h3><p>Le sujet complet de chaque TD est ici, avec un sommaire et des cases à cocher pour l'avant-séance.</p></div>
<div class="card"><h3>2 · Bloqué ? Un indice</h3><p>Chaque étape a un bloc <em>Besoin d'aide ?</em> : des indices <strong>progressifs</strong>, à déplier un par un. Ils orientent, ils ne donnent pas la solution.</p></div>
<div class="card"><h3>3 · Comprendre la notion</h3><p>Les <a href="notions/index.html">fiches Notions</a> rappellent l'essentiel et renvoient à la diapositive du <a href="cours/index.html">cours</a> qui en parle.</p></div>
<div class="card"><h3>4 · Copier le geste</h3><p>Les <a href="recettes/index.html">recettes</a> donnent le squelette, la vérification et les pièges. Vous adaptez, vous ne recopiez pas.</p></div></div>
<h2>Trois réflexes</h2>
<div class="cards three"><div class="card"><h3>Vérifier le modèle</h3><pre><code>curl -u gribouille:gribouille \\
  localhost:8080/api/diagnostic/modele</code></pre><p><a href="recettes/verifier-modele.html">Recette →</a></p></div>
<div class="card"><h3>Lancer avec un profil</h3><pre><code>mvn spring-boot:run \\
 -Dspring-boot.run.profiles=lmstudio</code></pre><p><a href="recettes/profils-lancement.html">Recette →</a></p></div>
<div class="card"><h3>Tenir le journal</h3><pre><code>JOURNAL.md
5 à 10 lignes par TD</code></pre><p><a href="recettes/journal.html">Recette →</a></p></div></div>
<h2>Règle d'or</h2><p class="callout">Les TD ne sont pas notés, mais l'<strong>évaluation de TD</strong> (CM4, 1 h sur machine) porte sur ce que vous y avez appris : corriger et câbler un outil, analyser une trace d'agent, rédiger une courte analyse critique. Vos livrables sont donc votre préparation. Un assistant IA est libre en TD (notez ce que vous lui demandez), <strong>interdit pendant les évaluations</strong>.</p>"""
    page("index.html", "Accueil", corps)

# ----------------------------------------------------------------------------- main
def main():
    if OUT.exists(): shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    shutil.copytree(SITE / "assets", OUT / "assets")
    (OUT / "assets" / "pygments.css").write_text(
        HtmlFormatter(style="default").get_style_defs(".codehilite") + "\n" +
        HtmlFormatter(style="monokai").get_style_defs('[data-theme="dark"] .codehilite'), encoding="utf-8")
    totaux = {td: build_td(td) for td in TDS}
    build_notions(); build_recettes(); build_patterns(); build_depannage(); build_ressources(); build_cours(); build_commun(); build_code(); build_index(totaux)
    add_search("Accueil", "index.html", "accueil parcours TD CM progression", "Accueil")
    (OUT / "assets" / "search-index.json").write_text(json.dumps(SEARCH, ensure_ascii=False), encoding="utf-8")
    (OUT / ".nojekyll").write_text("")
    (OUT / "404.html").write_text('<meta http-equiv="refresh" content="0; url=index.html">', encoding="utf-8")
    print(f"site généré : {len(list(OUT.rglob('*.html')))} pages dans {OUT}")

if __name__ == "__main__":
    main()
