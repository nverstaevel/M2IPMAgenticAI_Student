"""Schémas SVG en ligne (themables via variables CSS). Référencés dans le Markdown par {{fig:nom}}."""
import html
W = 720
def e(s): return html.escape(s, quote=False)

def T(x, y, s, cls="t", anchor="middle"):
    lines = s.split("\n"); y0 = y - (len(lines) - 1) * 8
    return "".join(f'<text class="{cls}" x="{x}" y="{y0 + i * 16 + 4.5}" text-anchor="{anchor}">{e(l)}</text>' for i, l in enumerate(lines))

def B(x, y, w, h, s, cls=""):
    return f'<rect class="b {cls}" x="{x}" y="{y}" width="{w}" height="{h}" rx="9"/>' + T(x + w / 2, y + h / 2, s, "t " + ("tb" if "acc" in cls else ""))

def A(pts, label=None, cls="", lx=0, ly=-7, anchor="middle"):
    d = "M" + " L".join(f"{x} {y}" for x, y in pts)
    out = f'<path class="ar {cls}" d="{d}"/>'
    if label:
        (x1, y1), (x2, y2) = pts[len(pts) // 2 - 1], pts[len(pts) // 2]
        out += T((x1 + x2) / 2 + lx, (y1 + y2) / 2 + ly, label, "t s " + ("bad" if "bad" in cls else ""), anchor)
    return out

def N(x, y, s, anchor="middle", cls="t s"): return T(x, y, s, cls, anchor)

def svg(h, body, titre, legende=""):
    defs = '<defs><marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" class="mk"/></marker></defs>'
    cap = f"<figcaption>{e(legende)}</figcaption>" if legende else ""
    return f'<figure class="fig"><svg viewBox="0 0 {W} {h}" role="img" aria-label="{e(titre)}">{defs}{body}</svg>{cap}</figure>'

def seq(actors, msgs, titre, legende="", step=38, notes=()):
    """actors: [(nom, x)] ; msgs: [(i, j, texte, dash)] ; notes: [(k, texte)] annotation à droite du message k."""
    top = 10; y0 = top + 56; h = y0 + len(msgs) * step + 14
    b = ""
    for nom, x in actors:
        b += f'<line class="life" x1="{x}" y1="{top + 32}" x2="{x}" y2="{h - 6}"/>' + B(x - 60, top, 120, 32, nom, "acc")
    for k, (i, j, txt, dash) in enumerate(msgs):
        y = y0 + k * step; xi, xj = actors[i][1], actors[j][1]; d = 5 if xj > xi else -5
        b += A([(xi + d, y), (xj - d, y)], cls="dash" if dash else "")
        b += T((xi + xj) / 2, y - 9, txt, "t s")
    for k, txt in notes:
        b += T(W - 8, y0 + k * step + 12, txt, "t s note", "end")
    return svg(h, b, titre, legende)

FIG = {}
def fig(f): FIG[f.__name__] = f; return f

@fig
def couches():
    b = B(40, 8, 230, 40, "Navigateur, curl, page Thymeleaf") + B(40, 76, 230, 44, "controller\ntraduit HTTP ⇄ appels de service")
    b += B(40, 148, 230, 44, "service\nrègles métier + @Transactional", "acc") + B(40, 220, 230, 44, "repository\naccès aux données (Spring Data)")
    b += B(40, 292, 230, 36, "Base de données")
    for y1, y2 in [(48, 76), (120, 148), (192, 220), (264, 292)]: b += A([(155, y1), (155, y2)])
    b += B(420, 76, 270, 44, "domain : entités JPA\nCreature, Proprietaire, Consultation…", "warn")
    b += B(420, 148, 270, 44, "Outils du modèle (TD2)\n@Tool appelle le service", "acc")
    b += A([(420, 170), (270, 170)], "appelle", cls="")
    b += A([(205, 148), (205, 120)], cls="bad dash")
    b += N(214, 138, "✗ jamais", "start", "t s bad")
    b += N(555, 230, "Un service ne connaît pas HTTP.\nUn contrôleur ne contient pas de règle métier.", "middle")
    return svg(340, b, "Quatre couches Spring Boot", "Les flèches pleines vont toujours vers le bas ; la règle de chevauchement vit dans le service, donc protège l'API et l'agent.")

@fig
def tool_calling():
    ac = [("Utilisateur", 70), ("Application", 250), ("Modèle", 440), ("Outil Java", 640)]
    ms = [(0, 1, "Combien coûte une consultation ?", 0), (1, 2, "question + liste des outils", 0), (2, 1, "« appelle consulterTarif(…) »", 1),
          (1, 3, "exécute la méthode", 0), (3, 1, "« 80,00 € »", 1), (1, 2, "résultat de l'outil", 0), (2, 1, "rédige la réponse", 1), (1, 0, "« Une consultation coûte 80 € »", 1)]
    return seq(ac, ms, "Déroulé d'un appel d'outil", "Le modèle ne fait que demander : c'est l'application qui exécute la méthode, puis lui renvoie le résultat.")

@fig
def memoire():
    ac = [("Navigateur", 80), ("Appli + mémoire", 330), ("Modèle", 600)]
    ms = [(0, 1, "« Parle-moi de Flammèche »", 0), (1, 2, "[système] [Q1]", 0), (2, 1, "R1", 1), (1, 0, "R1", 1),
          (0, 1, "« Et son propriétaire ? »", 0), (1, 2, "[système] [Q1] [R1] [Q2]", 0), (2, 1, "R2 (comprend « son »)", 1), (1, 0, "R2", 1)]
    return seq(ac, ms, "La mémoire est côté application", "Le modèle est sans état : l'historique est renvoyé à chaque appel, d'où un coût qui croît avec la conversation.")

@fig
def contexte():
    segs = [("Prompt\nsystème", 90, "mut"), ("Descriptions\nd'outils", 110, "mut"), ("Historique", 130, ""), ("Résultats\nd'outils", 110, "warn"), ("Question", 70, "acc")]
    b = ""; x = 20
    for s, w, c in segs:
        b += B(x, 60, w, 52, s, c); x += w
    b += B(x + 10, 60, 120, 52, "Réponse", "acc")
    b += f'<path class="br" d="M20 124 L20 130 L{x} 130 L{x} 124"/>' + N((20 + x) / 2, 148, "tokens d'entrée : relus et facturés à CHAQUE appel")
    b += f'<path class="br" d="M{x + 10} 124 L{x + 10} 130 L{x + 130} 130 L{x + 130} 124"/>' + N(x + 70, 148, "tokens de sortie", cls="t s")
    b += f'<path class="br" d="M20 44 L20 38 L{x + 130} 38 L{x + 130} 44"/>' + N((20 + x + 130) / 2, 24, "fenêtre de contexte = entrée + sortie ≤ limite du modèle")
    b += N(360, 190, "Plus l'entrée est longue : plus c'est lent, plus c'est cher, et plus un passage utile peut être noyé.")
    return svg(205, b, "Fenêtre de contexte", "Tout ce que le modèle lit compte : instructions, outils, historique et résultats d'outils.")

@fig
def temperature():
    def bars(x0, vals, mots, titre):
        s = N(x0 + 120, 20, titre, cls="t tb")
        for i, (v, m) in enumerate(zip(vals, mots)):
            h = int(v * 130); x = x0 + 8 + i * 62
            s += f'<rect class="pbar" x="{x}" y="{170 - h}" width="34" height="{h}" rx="4"/>' + N(x + 17, 186, m, cls="t s") + N(x + 17, 164 - h, f"{int(v * 100)} %", cls="t s")
        return s
    mots = ["dragon", "créature", "bête", "monstre", "animal"]
    b = bars(30, [.72, .15, .07, .04, .02], mots, "Température basse (0,2)") + bars(390, [.30, .25, .20, .15, .10], mots, "Température haute (1,0)")
    return svg(204, b, "Effet de la température", "Probabilités du prochain mot (illustration) : basse = presque toujours le même choix, haute = plus de variété.")

@fig
def chatclient():
    b = B(20, 20, 190, 36, "defaultSystem(prompt)") + B(20, 66, 190, 36, "defaultAdvisors(mémoire)") + B(20, 112, 190, 36, "defaultTools(outils)")
    b += B(270, 52, 150, 60, "ChatClient\n(API fluide)", "acc") + B(470, 52, 110, 60, "ChatModel\n(bas niveau)")
    b += B(620, 12, 90, 30, "Ollama") + B(620, 52, 90, 30, "LM Studio") + B(620, 92, 90, 30, "Groq / API") + B(620, 132, 90, 30, "simulé")
    for y in (38, 84, 130): b += A([(210, y), (270, 82)])
    b += A([(420, 82), (470, 82)]) + A([(580, 82), (620, 27)]) + A([(580, 82), (620, 67)]) + A([(580, 82), (620, 107)]) + A([(580, 82), (620, 147)])
    b += N(345, 140, "prompt().user(…).call()", cls="t s") + N(345, 160, ".content() / .entity(X.class) / .stream()", cls="t s")
    return svg(180, b, "ChatClient", "Le ChatClient se construit une fois avec ses valeurs par défaut ; le profil Spring choisit le fournisseur derrière le ChatModel.")

@fig
def jpa():
    b = B(250, 10, 150, 44, "Espece\nid, nom") + B(30, 110, 150, 52, "Proprietaire\nid, nom, telephone") + B(250, 110, 150, 52, "Creature\nid, nom, age…", "acc")
    b += B(470, 110, 160, 52, "Consultation\ndate, durée, tarif…") + B(500, 10, 160, 44, "Soigneur\nid, nom")
    b += A([(180, 136), (250, 136)], "N → 1", ly=-6) + A([(325, 110), (325, 54)], "N → 1", lx=24, ly=0) + A([(400, 136), (470, 136)], "1 ← N", ly=-6)
    b += A([(500, 32), (400, 32)], "N ↔ N", ly=-9)
    b += N(360, 200, "@ManyToOne porte la clé étrangère : c'est la créature qui connaît son propriétaire et son espèce.")
    return svg(220, b, "Modèle de données de la clinique", "Chaque flèche se lit « N créatures ont 1 propriétaire ».")

@fig
def sortie_structuree():
    b = B(20, 40, 170, 60, "« Ma licorne boite\ndepuis ce matin… »") + B(250, 30, 200, 80, ".entity(Triage.class)\n+ schéma ajouté au prompt", "acc") + B(510, 20, 190, 100, "record Triage(\n  espece = Licorne,\n  symptome = boiterie,\n  urgence = MOYENNE)", "warn")
    b += A([(190, 70), (250, 70)]) + A([(450, 70), (510, 70)])
    b += N(350, 150, "texte libre → objet Java typé : plus besoin de parser une phrase")
    return svg(170, b, "Sortie structurée")

@fig
def proposer_confirmer():
    b = N(120, 14, "côté modèle", cls="t s tb") + N(560, 14, "côté personnel (humain)", cls="t s tb")
    b += B(20, 30, 130, 50, "Modèle") + B(200, 30, 170, 50, "outil proposer…\nvérifie, n'écrit rien", "acc") + B(420, 30, 120, 50, "Proposition\nid, expire 10 min", "warn")
    b += A([(150, 55), (200, 55)]) + A([(370, 55), (420, 55)])
    b += B(560, 30, 140, 50, "Interface\nConfirmer / Annuler")
    b += A([(540, 55), (560, 55)], cls="dash")
    b += B(560, 130, 140, 50, "POST …/confirmer\nrôle PERSONNEL", "acc") + B(360, 130, 150, 50, "Service métier\n(règles, transaction)") + B(180, 130, 130, 50, "Base de données")
    b += A([(630, 80), (630, 130)]) + A([(560, 155), (510, 155)]) + A([(360, 155), (310, 155)])
    b += f'<line class="wall" x1="20" y1="104" x2="700" y2="104"/>' + N(360, 118, "frontière : le modèle n'a AUCUN outil pour franchir cette ligne", cls="t s bad")
    b += A([(85, 80), (85, 155), (180, 155)], "✗", cls="bad dash", lx=-10, ly=-8)
    return svg(210, b, "Proposer puis confirmer", "Seul un humain, via un endpoint distinct, transforme une proposition en écriture.")

@fig
def port_adaptateur():
    b = B(10, 60, 170, 50, "Outil de l'agent\n(proposer…)") + B(240, 60, 170, 50, "«interface»\nAgendaPort", "acc")
    b += B(480, 10, 220, 50, 'GoogleAgendaAdapter\n@Profile("!simule")') + B(480, 120, 220, 50, 'AgendaSimule\n@Profile("simule")')
    b += A([(180, 85), (240, 85)]) + A([(410, 85), (445, 85), (445, 35), (480, 35)]) + A([(410, 85), (445, 85), (445, 145), (480, 145)])
    b += N(590, 80, "réseau + compte de service", cls="t s") + N(590, 190, "liste en mémoire : tests rapides, sans réseau", cls="t s")
    b += N(205, 140, "le reste du code ne connaît\nque le port", cls="t s")
    return svg(210, b, "Port et adaptateurs", "Un seul adaptateur est actif, choisi par le profil Spring.")

@fig
def strategies_contexte():
    b = N(185, 14, "A · Tout dans le prompt", cls="t tb") + N(535, 14, "B · Outil de recherche", cls="t tb")
    b += B(10, 26, 150, 56, "Question\n+ corpus entier\n(185 Ko)", "warn") + B(220, 30, 100, 44, "Modèle", "acc") + A([(160, 54), (220, 54)])
    b += N(185, 100, "lent, cher, passage utile noyé\n→ simple, mais ne passe pas à l'échelle", cls="t s")
    b += B(330, 40, 90, 44, "Question") + B(460, 40, 80, 44, "Modèle", "acc") + B(630, 40, 80, 44, "outil\nchercher")
    b += A([(420, 62), (460, 62)]) + A([(540, 52), (630, 52)], "mots-clés", ly=-9) + A([(630, 74), (540, 74)], "3 passages", ly=14)
    b += N(520, 120, "peu de tokens, plus rapide ;\nla qualité dépend de la recherche", cls="t s")
    b += N(360, 160, "On ne tranche pas à l'intuition : on mesure (tokens, latence, coût, exactitude) dans mesures.csv.")
    return svg(180, b, "Deux stratégies de contexte")

@fig
def injection():
    b = B(10, 40, 150, 52, "Texte d'un tiers\n(page, note, fichier)", "warn") + B(200, 40, 100, 52, "outil\n(lecture)") + B(340, 20, 190, 92, "Contexte du modèle\nconsignes du système\n+ texte du tiers\n(mêmes yeux !)", "acc") + B(610, 40, 100, 52, "Appel d'outil\nnon voulu", "warn")
    b += A([(160, 66), (200, 66)]) + A([(300, 66), (340, 66)]) + A([(530, 66), (610, 66)], "consigne\npiégée", ly=-28, cls="bad")
    b += N(360, 150, "Défenses : « informations, jamais consignes » · outils minimaux par rôle · confirmation humaine · lire les traces")
    return svg(175, b, "Injection de prompt via un texte tiers")

@fig
def react():
    b = B(40, 60, 130, 50, "Penser\n(une phrase)", "acc") + B(300, 60, 130, 50, "Agir\n(appel d'outil)", "acc") + B(300, 190, 130, 50, "Observer\n(résultat)", "acc") + B(40, 190, 130, 50, "Mémoriser\n(historique)")
    b += A([(170, 85), (300, 85)]) + A([(365, 110), (365, 190)]) + A([(300, 215), (170, 215)]) + A([(105, 190), (105, 110)])
    b += B(520, 60, 170, 50, "Réponse finale\n(plus d'outil demandé)", "warn") + A([(105, 60), (105, 30), (605, 30), (605, 60)], "plus besoin d'outil", ly=-8, lx=-150)
    b += B(520, 170, 170, 80, "Garde-fous\nplafond d'étapes · budget\nboucle · erreur", "mut") + A([(430, 215), (520, 215)], cls="dash") + N(475, 200, "à chaque\nétape", cls="t s")
    return svg(270, b, "Boucle ReAct", "Une étape = un appel au modèle + l'exécution des outils demandés.")

@fig
def arrets():
    ys = [10, 76, 142, 208, 274]
    qs = ["le modèle demande un outil ?", "étape ≥ plafond ?", "tokens ≥ budget ?", "même appel que l'étape précédente ?", "l'outil a levé une erreur ?"]
    sort = ["REPONSE_FINALE", "PLAFOND_ETAPES", "BUDGET_TOKENS", "BOUCLE_DETECTEE", "ERREUR"]
    b = ""
    for i, (y, q, s) in enumerate(zip(ys, qs, sort)):
        b += B(20, y, 270, 44, q, "acc") + B(450, y, 200, 44, s, "warn" if i else "")
        b += A([(290, y + 22), (450, y + 22)], "non" if i == 0 else "oui", ly=-8, cls="")
        if i < 4: b += A([(155, y + 44), (155, ys[i + 1])], "oui" if i == 0 else "non", lx=16, ly=0)
    b += B(20, 340, 270, 36, "étape suivante : rappeler le modèle") + A([(155, 318), (155, 340)], "non", lx=16, ly=0)
    return svg(390, b, "Ordre des arrêts d'un agent", "La boucle est bornée : chaque motif d'arrêt est un signal observable, testable avec le modèle simulé.")

@fig
def acteur_critique():
    b = B(10, 50, 110, 50, "Demande\n(cas 1)") + B(170, 50, 130, 50, "Acteur\nvétérinaire", "acc") + B(400, 50, 140, 50, "Critique\ngardien du bestiaire", "acc") + B(600, 50, 110, 50, "Humain\nvalide / refuse", "warn")
    b += A([(120, 75), (170, 75)]) + A([(300, 75), (400, 75)], "diagnostic", ly=-9) + A([(540, 75), (600, 75)], "APPROUVE\nDOUTE", ly=-30)
    b += A([(470, 100), (470, 150), (235, 150), (235, 100)], "REJETE + objections (max 2 tours)", cls="bad", ly=-9)
    b += N(470, 22, "chercherDansWikipedia\n(aucun outil d'écriture)", cls="t s") + N(235, 24, "outils : dossier,\npharmacie", cls="t s")
    return svg(185, b, "Acteur et critique", "Le critique vérifie avec d'autres sources et renvoie un verdict ; un rejet ramène à l'acteur avec les objections.")

@fig
def superviseur():
    b = B(20, 80, 130, 50, "Demande") + B(190, 80, 140, 50, "Planificateur\n(découpe en tâches)", "acc")
    for i, (n, y) in enumerate([("Travailleur A\noutils dossier", 10), ("Travailleur B\noutils pharmacie", 80), ("Travailleur C\noutils agenda", 150)]):
        b += B(400, y, 140, 50, n) + A([(330, 105), (400, y + 25)])
        b += A([(540, y + 25), (590, 105)])
    b += B(590, 80, 110, 50, "Synthèse\n(assemble)", "warn") + A([(150, 105), (190, 105)])
    b += N(465, 215, "un seul bean « travailleur », trois nœuds aux outils différents", cls="t s")
    return svg(230, b, "Superviseur et travailleurs")

@fig
def workflow():
    b = ""
    for i, (n, c) in enumerate([("Étape 1\nrésumer (modèle)", ""), ("Étape 2\nclasser (modèle)", ""), ("Étape 3\nrédiger (modèle)", ""), ("Résultat", "warn")]):
        x = 20 + i * 180; b += B(x, 40, 150, 52, n, c)
        if i < 3: b += A([(x + 150, 66), (x + 180, 66)], cls="")
    b += N(360, 130, "Le code Java décide de l'enchaînement ; le modèle ne fait que remplir chaque étape.")
    b += N(360, 150, "Prévisible, peu coûteux, facile à tester : la référence à laquelle comparer les agents (TD5).")
    return svg(170, b, "Chaîne de prompts (workflow classique)")

@fig
def mcp():
    b = B(20, 30, 270, 120, "Clinique (client MCP)\n\nagent · ToolCallbacks\nJeuMcp.appeler(nom, json)", "acc") + B(430, 30, 270, 120, "Jeu du bestiaire (serveur MCP)\n\nhuit outils : état, soigner,\ncommander, jour suivant…")
    b += A([(290, 60), (430, 60)], "1 · List tools", ly=-8) + A([(430, 84), (290, 84)], "outils + descriptions", ly=14, cls="dash") + A([(290, 128), (430, 128)], "2 · call(nom, json)", ly=-8)
    b += N(360, 12, "HTTP /mcp  (Streamable HTTP, port 8090)", cls="t s")
    b += N(360, 185, "Un outil MCP est un ToolCallback comme un autre : votre code d'agent ne change pas.")
    return svg(200, b, "Client et serveur MCP")

@fig
def spectre():
    b = f'<line class="ar" x1="40" y1="70" x2="680" y2="70"/>' + N(40, 40, "le code décide", "start", "t s tb") + N(680, 40, "le modèle décide", "end", "t s tb")
    items = [("Chaîne de\nprompts", 80), ("Routage /\ntriage", 210), ("Acteur–\ncritique", 340), ("Superviseur–\ntravailleurs", 470), ("ReAct", 600)]
    for n, x in items: b += B(x - 50, 90, 100, 48, n, "acc")
    b += N(40, 170, "← prévisible · testable · peu coûteux", "start") + N(680, 170, "autonome · flexible · variable · plus coûteux →", "end")
    b += N(360, 190, "Règle d'ingénieur : prenez le niveau le plus à gauche qui résout le problème.")
    return svg(210, b, "Du workflow à l'agent autonome")

@fig
def roles_outils():
    cols = [("Diagnosticien", "lecture seule", ["état du jeu", "dossier", "notes (texte tiers)"], "acc"), ("Gardien", "vérifie", ["bestiaire", "Wikipédia"], "acc"), ("Apothicaire", "écrit", ["soigner", "commander"], "warn")]
    b = ""
    for i, (n, sub, tools, c) in enumerate(cols):
        x = 20 + i * 235; b += B(x, 10, 210, 46, f"{n}\n{sub}", c)
        for k, t in enumerate(tools): b += f'<rect class="chip" x="{x + 20}" y="{72 + k * 34}" width="170" height="26" rx="13"/>' + N(x + 105, 85 + k * 34, t)
    b += A([(230, 33), (255, 33)]) + A([(465, 33), (490, 33)])
    b += N(360, 190, "Chaque rôle n'a que les outils dont il a besoin : le lecteur d'un texte tiers ne peut pas écrire.")
    return svg(210, b, "Moindre privilège par rôle")

@fig
def evaluation():
    b = B(20, 50, 130, 50, "Architecture A", "acc") + B(20, 120, 130, 50, "Architecture B", "acc") + B(220, 50, 150, 120, "Mêmes conditions\nmême graine\nmême modèle\nN rejeux") + B(420, 50, 150, 50, "Mesures\ntokens · coût · score") + B(420, 120, 150, 50, "Traces\npourquoi ?", "warn") + B(620, 85, 90, 50, "Conclusion\nnuancée", "acc")
    b += A([(150, 75), (220, 100)]) + A([(150, 145), (220, 130)]) + A([(370, 90), (420, 75)]) + A([(370, 130), (420, 145)]) + A([(570, 75), (620, 100)]) + A([(570, 145), (620, 120)])
    return svg(190, b, "Comparer des architectures", "Un essai ne prouve rien : on rejoue, on mesure la dispersion, on lit les traces.")

def render(name):
    f = FIG.get(name)
    return f() if f else f"<p>[figure inconnue : {e(name)}]</p>"
