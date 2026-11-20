package fr.m2ipm.clinique.roles;

/** PRUDENT vérifie avant de proposer ; PRESSE propose le premier remède qui lui vient (pour provoquer des erreurs) ;
 *  OBSTINE maintient son traitement malgré les objections du gardien (pour observer une boucle qui ne converge pas). */
public enum ProfilActeur { PRUDENT, PRESSE, OBSTINE }
