#!/usr/bin/env python3
"""Génère le site (HTML statique) dans site/_build à partir de site/content et du code du dépôt.

Usage : python site/build.py          (depuis la racine du dépôt)
Dépendances : pip install markdown pygments
"""
import json, re, shutil, html, pathlib, datetime
import markdown
from markdown.extensions.toc import slugify_unicode
from pygments.formatters import HtmlFormatter

ROOT = pathlib.Path(__file__).resolve().parent.parent
SITE = ROOT / "site"
CONTENT = SITE / "content"
OUT = SITE / "_build"
GITHUB = "https://github.com/nverstaevel/M2IPMAgenticAI_Student"
SITE_URL = "https://nverstaevel.github.io/M2IPMAgenticAI_Student/"

TDS = {
    "td1": ("TD1", "Application Spring Boot classique", "td1-clinique", "TD1-clinique.md", "La clinique, sans IA"),
    "td2": ("TD2", "Un chat qui interroge l'application", "td2-chat", "TD2-chat.md", "Le Dr Gribouille et ses outils internes"),
    "td3": ("TD3", "Outils externes et contexte", "td3-outils-externes", "TD3-outils-externes.md", "Quatre outils et un tableau de mesures"),
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
    exts = ["tables", "fenced_code", "sane_lists", "attr_list", "codehilite", "toc"]
    cfg = {"codehilite": {"guess_lang": False, "css_class": "codehilite"},
           "toc": {"slugify": slugify_unicode, "permalink": "#", "permalink_title": "Lien vers cette section", "toc_depth": "2-3"}}
    m = markdown.Markdown(extensions=exts, extension_configs=cfg)
    h = m.convert(text)
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
NAV = [("index.html", "Accueil"), ("notions/index.html", "Notions"), ("recettes/index.html", "Recettes"), ("code.html", "Code"),
       ("depannage.html", "Dépannage"), ("ressources.html", "Ressources"), ("commun.html", "Infos communes")]

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
    head = (f'<header class="td-head"><p class="kicker">{num} · séance de 3 h · en binôme</p><h1>{esc(theme)}</h1>'
            f'<p class="livrable"><strong>Vous livrez :</strong> {esc(livrable)}</p>'
            f'<div class="progress" data-prefix="{td}-" data-total="{total}"><div class="bar"><i></i></div><span class="pct">0 %</span></div></header>')
    corps = head + fiche + f'<article class="sujet">{h}</article>' + pager
    side = toc_html(tokens)
    page(f"td/{td}.html", f"{num} — {theme}", corps, side=side, td_ctx=td)
    add_search(f"{num} — {theme}", f"td/{td}.html", body_md, "TD")
    return total

# ----------------------------------------------------------------------------- notions & recettes
def usage_html(uses, root):
    if not uses: return ""
    items = "".join(f'<li><a href="{root}td/{td}.html">{TDS[td][0]}</a>{" · étape " + s if s.isdigit() else (" · avant la séance" if s == "avant" else "")}</li>' for td, s in uses)
    return f'<ul class="usage">{items}</ul>'

def build_notions():
    cards = []
    for nid, n in NOTIONS.items():
        h, _ = md_render(n["body"])
        tds = sorted({td for td, _ in USAGE_N[nid]} | set(lst(n.get("td", ""))))
        rec = RECETTES_PAR_NOTION.get(nid, [])
        cm = "".join(f"<li>{esc(x.strip())}</li>" for x in re.split(r"(?<=\))\s*;\s*|\s*;\s*", n.get("cm", "")) if x.strip())
        corps = (f'<p class="kicker"><a href="index.html">Notions</a></p><h1>{esc(n["title"])}</h1><p class="lead">{esc(n.get("resume",""))}</p>'
                 f'<div class="grid2"><section class="card"><h2 class="nodot">Dans le cours</h2><ul>{cm}</ul>'
                 f'<p class="note">Support de CM distribué en séance : repérez ces titres de diapositives.</p></section>'
                 f'<section class="card"><h2 class="nodot">Où le retrouver en TD</h2>{usage_html(USAGE_N[nid], "../") or ""}'
                 + ("<h3>Recettes liées</h3><ul>" + "".join(f'<li><a href="../recettes/{r}.html">{esc(RECETTES[r]["title"])}</a></li>' for r in rec) + "</ul>" if rec else "")
                 + "</section></div>"
                 f'<article class="prose">{h}</article>')
        page(f"notions/{nid}.html", n["title"], corps)
        add_search(n["title"], f"notions/{nid}.html", n["body"] + " " + n.get("resume", ""), "Notion")
        cards.append((nid, n, tds))
    ordre = [("td1", "Spring Boot, en rappel (TD1)"), ("td2", "Parler à un modèle (TD2)"), ("td3", "Sources externes et contexte (TD3)"),
             ("td4", "Boucle d'agent (TD4)"), ("td5", "Agents à rôles (TD5)"), ("td6", "MCP et évaluation (TD6)")]
    sect = []
    for td, titre in ordre:
        cs = [c for c in cards if (c[2][0] if c[2] else "td6") == td]
        if not cs: continue
        sect.append(f'<h2 id="{td}">{titre}</h2><div class="cards">' + "".join(
            f'<a class="card link" href="{nid}.html"><h3>{esc(n["title"])}</h3><p>{esc(n.get("resume",""))}</p>'
            f'<p class="tags">{"".join(f"<span>{TDS[t][0]}</span>" for t in tds)}</p></a>' for nid, n, tds in cs) + "</div>")
    page("notions/index.html", "Notions du cours",
         '<h1>Notions du cours</h1><p class="lead">Chaque fiche rappelle l\'essentiel d\'une notion, dit où la retrouver dans les supports de CM, et renvoie aux étapes de TD qui la mobilisent.</p>' + "".join(sect))

def build_recettes():
    for rid, r in RECETTES.items():
        h, _ = md_render(r["body"])
        notions = lst(r.get("notions", ""))
        corps = (f'<p class="kicker"><a href="index.html">Recettes</a> · {" ".join(TDS[t][0] for t in lst(r.get("td","")) if t in TDS)}</p>'
                 f'<h1>{esc(r["title"])}</h1><p class="lead">{esc(r.get("resume",""))}</p>'
                 f'<article class="prose">{h}</article>'
                 + (f'<section class="card"><h2 class="nodot">Pour comprendre</h2><p>{chips(notions, "n", "../")}</p></section>' if notions else "")
                 + (f'<section class="card"><h2 class="nodot">Utilisée dans</h2>{usage_html(USAGE_R[rid], "../")}</section>' if USAGE_R[rid] else ""))
        page(f"recettes/{rid}.html", r["title"], corps)
        add_search(r["title"], f"recettes/{rid}.html", r["body"] + " " + r.get("resume", ""), "Recette")
    ordre = sorted(RECETTES.items(), key=lambda kv: (min([int(t[2:]) for t in lst(kv[1].get("td", "td9")) if t.startswith("td")] or [9]), kv[1]["title"]))
    cards = "".join(f'<a class="card link" href="{rid}.html"><h3>{esc(r["title"])}</h3><p>{esc(r.get("resume",""))}</p>'
                    f'<p class="tags">{"".join(f"<span>{TDS[t][0]}</span>" for t in lst(r.get("td","")) if t in TDS)}</p></a>' for rid, r in ordre)
    page("recettes/index.html", "Recettes",
         '<h1>Recettes</h1><p class="lead">Des marches à suivre courtes : ingrédients, squelette de code, vérification, pièges. Les exemples sont <strong>hors clinique</strong> quand ils touchent à ce que vous devez écrire : ils montrent le geste, pas la solution.</p>'
         f'<div class="cards">{cards}</div>')

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
         '<div class="card"><h2 class="nodot">Mettre en place son dossier</h2><ol><li>Clonez (ou forkez) le dépôt étudiant et ouvrez <strong>un dossier de TD</strong> dans l\'IDE (pas la racine).</li>'
         '<li>Lancez : <code>mvn spring-boot:run -Dspring-boot.run.profiles=…</code></li><li>Cherchez les marqueurs <code>// TODO TDn</code> (Eclipse : vue <em>Tasks</em> ; IntelliJ : fenêtre <em>TODO</em>).</li>'
         '<li>Un binôme en retard repart du dossier du TD suivant : rien n\'est perdu.</li></ol></div>' + "".join(lignes))
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
            lien = f'notions/index.html' if nom != "CM4" else "commun.html"
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
<div class="card"><h3>3 · Comprendre la notion</h3><p>Les <a href="notions/index.html">fiches Notions</a> rappellent l'essentiel et disent où la retrouver dans les supports de CM.</p></div>
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
    build_notions(); build_recettes(); build_depannage(); build_ressources(); build_commun(); build_code(); build_index(totaux)
    add_search("Accueil", "index.html", "accueil parcours TD CM progression", "Accueil")
    (OUT / "assets" / "search-index.json").write_text(json.dumps(SEARCH, ensure_ascii=False), encoding="utf-8")
    (OUT / ".nojekyll").write_text("")
    (OUT / "404.html").write_text('<meta http-equiv="refresh" content="0; url=index.html">', encoding="utf-8")
    print(f"site généré : {len(list(OUT.rglob('*.html')))} pages dans {OUT}")

if __name__ == "__main__":
    main()
