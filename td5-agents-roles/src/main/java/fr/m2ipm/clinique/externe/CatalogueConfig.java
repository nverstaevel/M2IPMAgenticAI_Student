package fr.m2ipm.clinique.externe;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Deux bases : la clinique (JPA, écriture) et le catalogue de médicaments (externe, lecture seule).
 * Dès qu'on déclare un DataSource soi-même, Spring Boot ne configure plus celui de la clinique :
 * on le déclare donc aussi, et on le marque @Primary pour que JPA, la mémoire de conversation et data.sql l'utilisent.
 */
@Configuration
public class CatalogueConfig {

    @Bean
    @Primary
    DataSource dataSource(@Value("${spring.datasource.url}") String url) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @Bean(destroyMethod = "shutdown")
    DataSource catalogueDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .setScriptEncoding("UTF-8")
                .addScript("classpath:donnees/catalogue-medicaments.sql")
                .build();
    }

    /** Client JDBC du catalogue. Volontairement pas un bean JdbcTemplate : il remplacerait celui de la clinique. */
    @Bean
    @Qualifier("catalogue")
    JdbcClient catalogueJdbc(@Qualifier("catalogueDataSource") DataSource ds) {
        return JdbcClient.create(ds);
    }
}
