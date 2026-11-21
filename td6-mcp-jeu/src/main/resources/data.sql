INSERT INTO espece (nom, habitat) VALUES
  ('Dragon', 'montagne'), ('Licorne', 'forêt'), ('Yéti', 'glacier'),
  ('Loup-garou', 'forêt'), ('Vampire', 'château'), ('Phénix', 'volcan');

INSERT INTO proprietaire (nom, prenom, ville, telephone) VALUES
  ('Brumevent', 'Morgane', 'Brécilien', '0600000001'),
  ('Fumerolle', 'Gaspard', 'Pic-du-Dragon', '0600000002'),
  ('Clairdelune', 'Ondine', 'Val-des-Ombres', '0600000003');

INSERT INTO creature (nom, date_naissance, espece_id, proprietaire_id) VALUES
  ('Flammèche', '1824-03-02', (SELECT id FROM espece WHERE nom='Dragon'),     (SELECT id FROM proprietaire WHERE nom='Fumerolle')),
  ('Paillette', '1990-06-21', (SELECT id FROM espece WHERE nom='Licorne'),    (SELECT id FROM proprietaire WHERE nom='Brumevent')),
  ('Grognon', '1957-12-01',   (SELECT id FROM espece WHERE nom='Yéti'),       (SELECT id FROM proprietaire WHERE nom='Fumerolle')),
  ('Lunatique', '1988-10-31', (SELECT id FROM espece WHERE nom='Loup-garou'), (SELECT id FROM proprietaire WHERE nom='Clairdelune')),
  ('Monsieur Crépuscule', '1731-02-14', (SELECT id FROM espece WHERE nom='Vampire'), (SELECT id FROM proprietaire WHERE nom='Clairdelune')),
  ('Rebelote', '1203-04-01',  (SELECT id FROM espece WHERE nom='Phénix'),     (SELECT id FROM proprietaire WHERE nom='Brumevent'));

INSERT INTO soigneur (nom) VALUES ('Dr Gribouille'), ('Mme Frimas'), ('M. Vespéral');

INSERT INTO soigneur_espece (soigneur_id, espece_id) VALUES
  ((SELECT id FROM soigneur WHERE nom='Dr Gribouille'), (SELECT id FROM espece WHERE nom='Dragon')),
  ((SELECT id FROM soigneur WHERE nom='Dr Gribouille'), (SELECT id FROM espece WHERE nom='Licorne')),
  ((SELECT id FROM soigneur WHERE nom='Dr Gribouille'), (SELECT id FROM espece WHERE nom='Phénix')),
  ((SELECT id FROM soigneur WHERE nom='Dr Gribouille'), (SELECT id FROM espece WHERE nom='Loup-garou')),
  ((SELECT id FROM soigneur WHERE nom='Mme Frimas'),    (SELECT id FROM espece WHERE nom='Yéti')),
  ((SELECT id FROM soigneur WHERE nom='Mme Frimas'),    (SELECT id FROM espece WHERE nom='Dragon')),
  ((SELECT id FROM soigneur WHERE nom='M. Vespéral'),   (SELECT id FROM espece WHERE nom='Vampire')),
  ((SELECT id FROM soigneur WHERE nom='M. Vespéral'),   (SELECT id FROM espece WHERE nom='Loup-garou'));

INSERT INTO consultation (date_heure, creature_id, soigneur_id, motif, diagnostic, traitement, tarif) VALUES
  ('2026-01-12 14:00:00', (SELECT id FROM creature WHERE nom='Flammèche'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'),
     'Rhume : fumée verdâtre et éternuements', 'Rhume des montagnes', 'Tisane de thym et repos près d''un feu doux', 40.00),
  ('2026-02-03 10:30:00', (SELECT id FROM creature WHERE nom='Paillette'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'),
     'Sabot fêlé', 'Fissure du sabot avant gauche', 'Onguent à la poudre de perlimpinpin', 40.00),
  ('2026-02-20 22:00:00', (SELECT id FROM creature WHERE nom='Lunatique'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'),
     'Insomnie les soirs de pleine lune', 'Insomnie lunaire', 'Rideaux occultants et infusion de valériane', 40.00),
  ('2026-03-05 21:00:00', (SELECT id FROM creature WHERE nom='Monsieur Crépuscule'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'),
     'Fatigue et teint pâle', NULL, NULL, 40.00),
  ('2026-03-12 14:00:00', (SELECT id FROM creature WHERE nom='Rebelote'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'),
     'Troisième renaissance du mois', NULL, NULL, 40.00);

-- Jeu de données étendu : 10 propriétaires, 20 créatures, 4 soigneurs, 30 consultations.
INSERT INTO soigneur (nom) VALUES ('Mlle Plumeau');

INSERT INTO soigneur_espece (soigneur_id, espece_id) VALUES
  ((SELECT id FROM soigneur WHERE nom='Mlle Plumeau'), (SELECT id FROM espece WHERE nom='Licorne')),
  ((SELECT id FROM soigneur WHERE nom='Mlle Plumeau'), (SELECT id FROM espece WHERE nom='Yéti'));

INSERT INTO proprietaire (nom, prenom, ville, telephone) VALUES
  ('Ventdebise', 'Léonard', 'Hauts-Nuages', '0600000004'),
  ('Pailledor', 'Capucine', 'Source-Dorée', '0600000005'),
  ('Griffenoir', 'Hector', 'Roc-Sombre', '0600000006'),
  ('Lamarée', 'Salomé', 'Anse-aux-Sirènes', '0600000007'),
  ('Cendrelune', 'Barnabé', 'Mont-Cratère', '0600000008'),
  ('Mousseline', 'Perrine', 'Forêt-Profonde', '0600000009'),
  ('Tourbillon', 'Anselme', 'Glacier-Bleu', '0600000010');

INSERT INTO creature (nom, date_naissance, espece_id, proprietaire_id) VALUES
  ('Braisette', '1950-05-05', (SELECT id FROM espece WHERE nom='Dragon'), (SELECT id FROM proprietaire WHERE nom='Ventdebise')),
  ('Cumulus', '1688-09-09', (SELECT id FROM espece WHERE nom='Dragon'), (SELECT id FROM proprietaire WHERE nom='Griffenoir')),
  ('Arc-en-ciel', '2001-04-12', (SELECT id FROM espece WHERE nom='Licorne'), (SELECT id FROM proprietaire WHERE nom='Pailledor')),
  ('Étoile-du-soir', '1975-08-30', (SELECT id FROM espece WHERE nom='Licorne'), (SELECT id FROM proprietaire WHERE nom='Mousseline')),
  ('Glaçon', '1999-01-15', (SELECT id FROM espece WHERE nom='Yéti'), (SELECT id FROM proprietaire WHERE nom='Tourbillon')),
  ('Bouboule', '1923-11-11', (SELECT id FROM espece WHERE nom='Yéti'), (SELECT id FROM proprietaire WHERE nom='Tourbillon')),
  ('Croc-Mou', '2010-10-31', (SELECT id FROM espece WHERE nom='Loup-garou'), (SELECT id FROM proprietaire WHERE nom='Griffenoir')),
  ('Clair-de-Griffe', '1995-07-07', (SELECT id FROM espece WHERE nom='Loup-garou'), (SELECT id FROM proprietaire WHERE nom='Cendrelune')),
  ('Hurlevent', '1979-03-03', (SELECT id FROM espece WHERE nom='Loup-garou'), (SELECT id FROM proprietaire WHERE nom='Mousseline')),
  ('Comte Ténèbre', '1602-12-24', (SELECT id FROM espece WHERE nom='Vampire'), (SELECT id FROM proprietaire WHERE nom='Cendrelune')),
  ('Mademoiselle Pâleur', '1840-06-06', (SELECT id FROM espece WHERE nom='Vampire'), (SELECT id FROM proprietaire WHERE nom='Lamarée')),
  ('Phénoménal', '1500-01-01', (SELECT id FROM espece WHERE nom='Phénix'), (SELECT id FROM proprietaire WHERE nom='Lamarée')),
  ('Cendrillon', '1987-09-09', (SELECT id FROM espece WHERE nom='Phénix'), (SELECT id FROM proprietaire WHERE nom='Ventdebise')),
  ('Pétale', '2015-05-20', (SELECT id FROM espece WHERE nom='Licorne'), (SELECT id FROM proprietaire WHERE nom='Pailledor'));

INSERT INTO consultation (date_heure, creature_id, soigneur_id, motif, diagnostic, traitement, tarif) VALUES
  ('2026-04-02 09:00:00', (SELECT id FROM creature WHERE nom='Braisette'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Hoquet enflammé', 'Reflux de braises', 'Repas plus petits et eau tiède', 80.00),
  ('2026-04-02 09:30:00', (SELECT id FROM creature WHERE nom='Cumulus'), (SELECT id FROM soigneur WHERE nom='Mme Frimas'), 'Écailles ternes', 'Carence en soufre', 'Bain de soufre hebdomadaire', 80.00),
  ('2026-04-09 10:00:00', (SELECT id FROM creature WHERE nom='Arc-en-ciel'), (SELECT id FROM soigneur WHERE nom='Mlle Plumeau'), 'Corne qui pâlit', 'Fatigue arc-en-ciel', 'Repos et prairie fleurie', 60.00),
  ('2026-04-16 11:00:00', (SELECT id FROM creature WHERE nom='Étoile-du-soir'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Crinière emmêlée', 'Nœuds de crinière', 'Peigne en corne et huile d''argan', 60.00),
  ('2026-04-23 15:00:00', (SELECT id FROM creature WHERE nom='Glaçon'), (SELECT id FROM soigneur WHERE nom='Mme Frimas'), 'Fourrure qui fond par endroits', 'Surchauffe légère', 'Séjour en chambre froide d''altitude', 48.00),
  ('2026-05-04 16:00:00', (SELECT id FROM creature WHERE nom='Bouboule'), (SELECT id FROM soigneur WHERE nom='Mlle Plumeau'), 'Perte d''appétit', 'Nostalgie de la neige', 'Glace pilée et chants de blizzard', 48.00),
  ('2026-05-12 21:30:00', (SELECT id FROM creature WHERE nom='Croc-Mou'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'), 'Hurlements en retard', 'Rhume de pleine lune', 'Infusion de thym et couverture', 52.00),
  ('2026-05-12 22:00:00', (SELECT id FROM creature WHERE nom='Clair-de-Griffe'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Griffes cassantes', 'Carence en minéraux', 'Soin de griffes et poudre d''os', 52.00),
  ('2026-05-19 20:00:00', (SELECT id FROM creature WHERE nom='Hurlevent'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'), 'Poil terne', 'Mue décalée', 'Brossage nocturne quotidien', 52.00),
  ('2026-05-26 21:00:00', (SELECT id FROM creature WHERE nom='Comte Ténèbre'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'), 'Reflet absent du miroir', 'Défaut de reflet bénin', 'Aucun : surveillance', 40.00),
  ('2026-06-03 22:30:00', (SELECT id FROM creature WHERE nom='Mademoiselle Pâleur'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'), 'Soif de sang tiède', 'Anémie légère', 'Sérum de sang-frais', 40.00),
  ('2026-06-10 14:00:00', (SELECT id FROM creature WHERE nom='Phénoménal'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Plumes en cendres', 'Fin de cycle', 'Cendre de renaissance', 72.00),
  ('2026-06-10 14:30:00', (SELECT id FROM creature WHERE nom='Cendrillon'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Flamme vacillante', 'Courant d''air', 'Abri et fagots secs', 72.00),
  ('2026-06-17 10:30:00', (SELECT id FROM creature WHERE nom='Pétale'), (SELECT id FROM soigneur WHERE nom='Mlle Plumeau'), 'Sabot qui claque', 'Ferrure usée', 'Ferrure en argent poli', 60.00),
  ('2026-06-24 09:00:00', (SELECT id FROM creature WHERE nom='Braisette'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Contrôle après reflux', 'Guérison complète', 'Aucun', 80.00),
  ('2026-07-01 11:30:00', (SELECT id FROM creature WHERE nom='Cumulus'), (SELECT id FROM soigneur WHERE nom='Mme Frimas'), 'Aile gauche raide', 'Courbature', 'Massage et chaleur douce', 80.00),
  ('2026-07-08 17:00:00', (SELECT id FROM creature WHERE nom='Arc-en-ciel'), (SELECT id FROM soigneur WHERE nom='Mlle Plumeau'), 'Contrôle annuel', 'Bonne santé', 'Vaccin annuel', 60.00),
  ('2026-07-15 15:30:00', (SELECT id FROM creature WHERE nom='Glaçon'), (SELECT id FROM soigneur WHERE nom='Mme Frimas'), 'Éternuements de flocons', 'Allergie aux pollens', 'Antihistaminique givré', 48.00),
  ('2026-07-22 21:00:00', (SELECT id FROM creature WHERE nom='Croc-Mou'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'), 'Insomnie de pleine lune', 'Insomnie lunaire', 'Élixir de valériane', 52.00),
  ('2026-08-05 22:00:00', (SELECT id FROM creature WHERE nom='Comte Ténèbre'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'), 'Fatigue après un déménagement', 'Fatigue passagère', 'Repos en cave fraîche', 40.00),
  ('2026-08-19 14:00:00', (SELECT id FROM creature WHERE nom='Phénoménal'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Renaissance précoce', 'Cycle raccourci', 'Surveillance du bûcher', 72.00),
  ('2026-09-15 10:00:00', (SELECT id FROM creature WHERE nom='Étoile-du-soir'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Boiterie légère', 'Entorse du boulet', 'Bandage et repos', 60.00),
  ('2026-12-03 09:30:00', (SELECT id FROM creature WHERE nom='Bouboule'), (SELECT id FROM soigneur WHERE nom='Mlle Plumeau'), 'Contrôle de poids', NULL, NULL, 48.00),
  ('2026-12-10 16:00:00', (SELECT id FROM creature WHERE nom='Hurlevent'), (SELECT id FROM soigneur WHERE nom='M. Vespéral'), 'Mue d''hiver', NULL, NULL, 52.00),
  ('2026-12-17 11:00:00', (SELECT id FROM creature WHERE nom='Cendrillon'), (SELECT id FROM soigneur WHERE nom='Dr Gribouille'), 'Vaccin annuel', NULL, NULL, 72.00);
