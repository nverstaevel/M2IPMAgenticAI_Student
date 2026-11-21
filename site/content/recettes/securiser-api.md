---
title: Protéger l'API : rôles et CSRF
resume: Deux rôles, CSRF ignoré pour /api/**, et un endpoint réservé au personnel.
td: td1, td2
famille: spring
ordre: 4
notions: securite-basique, api-rest-validation, confirmation-ecritures
---
## Objectif
Qu'une API appelée par `curl` ou JavaScript fonctionne, tout en réservant certaines actions à un rôle.

## Squelette
```java
@Bean
SecurityFilterChain chaine(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/propositions/*/confirmer").hasRole("PERSONNEL")
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .httpBasic(Customizer.withDefaults())
        .formLogin(Customizer.withDefaults())
        .build();
}
```

## Vérifier
| Appel | Attendu |
|---|---|
| `curl localhost:8080/api/creatures` | 401 |
| `curl -u gribouille:gribouille …` | 200 |
| `POST` avec identifiants | pas de 403 CSRF |
| confirmer avec un rôle sans `PERSONNEL` | 403 |

## Pièges
- La règle la plus **spécifique** doit être écrite avant la plus générale.
- Ne vérifiez jamais un droit seulement dans l'interface.
