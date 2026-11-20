-- Catalogue de médicaments : base externe, lecture seule côté application (second DataSource).
CREATE TABLE medicament (
    id INT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    principe_actif VARCHAR(200) NOT NULL,
    contre_indications VARCHAR(300) NOT NULL
);
CREATE TABLE medicament_espece (
    medicament_id INT NOT NULL REFERENCES medicament(id),
    espece VARCHAR(50) NOT NULL,
    dose VARCHAR(100) NOT NULL
);
INSERT INTO medicament VALUES (1, 'Baume ignifuge', 'aloès de cendre', 'Ne pas utiliser sur un Yéti (fond brutal).');
INSERT INTO medicament VALUES (2, 'Poudre de lune', 'poussière sélène', 'Déconseillée aux Vampires (brûlure à la lumière).');
INSERT INTO medicament VALUES (3, 'Élixir de valériane', 'valériane argentée', 'Aucune connue ; éviter avec un autre sédatif.');
INSERT INTO medicament VALUES (4, 'Sérum de sang-frais', 'plasma stabilisé', 'Réservé aux Vampires.');
INSERT INTO medicament VALUES (5, 'Onguent de givre', 'menthe glaciale', 'Ne pas utiliser sur un Dragon ou un Phénix.');
INSERT INTO medicament VALUES (6, 'Cendre de renaissance', 'cendre de phénix', 'Réservée aux Phénix.');
INSERT INTO medicament_espece VALUES (1, 'Dragon', '1 noix matin et soir');
INSERT INTO medicament_espece VALUES (1, 'Phénix', '1 noix par jour');
INSERT INTO medicament_espece VALUES (2, 'Loup-garou', '1 sachet les soirs de pleine lune');
INSERT INTO medicament_espece VALUES (2, 'Licorne', '1/2 sachet');
INSERT INTO medicament_espece VALUES (3, 'Loup-garou', '10 gouttes le soir');
INSERT INTO medicament_espece VALUES (3, 'Dragon', '20 gouttes le soir');
INSERT INTO medicament_espece VALUES (4, 'Vampire', '1 ampoule par semaine');
INSERT INTO medicament_espece VALUES (5, 'Yéti', '1 application par jour');
INSERT INTO medicament_espece VALUES (5, 'Licorne', '1 application tous les deux jours');
INSERT INTO medicament_espece VALUES (6, 'Phénix', '1 dose à chaque cycle');
