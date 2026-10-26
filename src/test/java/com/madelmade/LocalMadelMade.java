package com.madelmade;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.File;
import org.springframework.boot.SpringApplication;

/**
 * Runs the site on your own computer with its own small Postgres, so nothing needs installing:
 * {@code mvn spring-boot:test-run}, then open http://localhost:8080.
 * Data is kept in ~/.madelmade-local between runs; delete that folder to start fresh.
 */
public class LocalMadelMade {
    public static void main(String[] args) throws Exception {
        File dataDir = new File(System.getProperty("user.home"), ".madelmade-local/pgdata");
        dataDir.mkdirs();
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setDataDirectory(dataDir)
                .setCleanDataDirectory(false)
                .setPort(54329)
                // Some Macs take more than the default 10 seconds to start it.
                .setPGStartupWait(java.time.Duration.ofSeconds(60))
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pg.close();
            } catch (Exception ignored) {
                // Postgres is stopping with the app anyway.
            }
        }));
        for (String arg : args) {
            if (arg.startsWith("--copy-to-live=")) {
                CopyToLive.run(pg.getPostgresDatabase(), arg.substring("--copy-to-live=".length()));
                return;
            }
        }
        System.setProperty("spring.datasource.url", pg.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "");
        SpringApplication app = new SpringApplication(MadelMadeApplication.class);
        app.setAdditionalProfiles("dev");
        app.run(args);
    }
}
