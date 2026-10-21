package com.madelmade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class CopyToLiveTest {
    @Test
    void copiesMenuAndSettingsButNotTestActivity() throws Exception {
        try (EmbeddedPostgres local = EmbeddedPostgres.start(); EmbeddedPostgres live = EmbeddedPostgres.start()) {
            DataSource src = local.getPostgresDatabase();
            Flyway.configure().dataSource(src).load().migrate();
            run(src, "insert into customer (email) values ('test@example.com')");
            run(src, "insert into category (name, slug) values ('Breads', 'breads')");
            run(src, "insert into product (category_id, name, slug) select id, 'Renamed loaf', 'loaf' from category");
            run(src, "insert into setting (key, value) values ('test.copied', 'yes')");
            String url = "postgresql://postgres:@localhost:" + live.getPort() + "/postgres";

            CopyToLive.run(src, url);

            DataSource dst = live.getPostgresDatabase();
            assertEquals(count(src, "product"), count(dst, "product"));
            assertEquals(1, count(dst, "product where name = 'Renamed loaf'"));
            assertEquals(1, count(dst, "setting where key = 'test.copied'"));
            assertEquals(0, count(dst, "customer"));
            // New rows on the live site get fresh ids after the copied ones.
            run(dst, "insert into category (name, slug) values ('New', 'new')");

            // Once the live site has real customers, it's never overwritten.
            run(dst, "insert into customer (email) values ('real@example.com')");
            assertThrows(IllegalStateException.class, () -> CopyToLive.run(src, url));
            assertEquals(1, count(dst, "customer"));
        }
    }

    @Test
    void readsRailwayUrl() {
        var live = CopyToLive.Live.parse("postgresql://postgres:p%40ss@host.proxy.rlwy.net:41234/railway");
        assertEquals("jdbc:postgresql://host.proxy.rlwy.net:41234/railway", live.jdbc());
        assertEquals("p@ss", live.password());
    }

    private static void run(DataSource ds, String sql) throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private static long count(DataSource ds, String from) throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("select count(*) from " + from)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
