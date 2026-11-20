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
