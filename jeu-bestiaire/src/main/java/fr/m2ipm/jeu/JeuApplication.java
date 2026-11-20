package fr.m2ipm.jeu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Le jeu « Clinique du bestiaire » : une application indépendante, que l'on pilote par le protocole MCP (endpoint /mcp). */
@SpringBootApplication
public class JeuApplication {
    public static void main(String[] args) {
        SpringApplication.run(JeuApplication.class, args);
    }
}
