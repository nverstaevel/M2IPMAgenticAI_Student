package fr.m2ipm.clinique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    public static final String PERSONNEL = "PERSONNEL";
    public static final String LECTURE = "LECTURE";

    @Bean
    SecurityFilterChain filtres(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(PERSONNEL, LECTURE)
                .requestMatchers("/api/**").hasRole(PERSONNEL)
                .requestMatchers(HttpMethod.POST, "/consultations").hasRole(PERSONNEL)
                .requestMatchers("/consultations/nouvelle").hasRole(PERSONNEL)
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults())
            // L'API est appelée par des clients non navigateurs : pas de jeton CSRF pour /api/**.
            .csrf(c -> c.ignoringRequestMatchers(request -> request.getRequestURI().startsWith("/api/")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Deux utilisateurs en mémoire, uniquement pour le TD (mots de passe = identifiants). */
    @Bean
    UserDetailsService utilisateurs(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("gribouille").password(encoder.encode("gribouille")).roles(PERSONNEL).build(),
                User.withUsername("visiteur").password(encoder.encode("visiteur")).roles(LECTURE).build());
    }
}
