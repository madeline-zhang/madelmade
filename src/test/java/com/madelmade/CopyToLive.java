package com.madelmade;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * Copies everything Madeline set up locally (menu, photos, gallery, pages, settings, promo codes) to the live
 * database, and leaves the live site with no orders, customers or feedback, so test orders don't go live.
 * Run with {@code mvn spring-boot:test-run -Dspring-boot.run.arguments="--copy-to-live=postgresql://..."}.
 */
public final class CopyToLive {
    /** Test activity that never goes live. */
    static final Set<String> CLEARED = Set.of("orders", "order_item", "order_event", "order_message", "customer",
            "login_code", "feedback");
    private static final String HISTORY = "flyway_schema_history";

    private CopyToLive() {
    }

    public static void run(DataSource local, String liveUrl) throws SQLException {
        Live live = Live.parse(liveUrl);
        System.out.println("Updating both databases to the latest layout...");
        Flyway.configure().dataSource(local).load().migrate();
        Flyway.configure().dataSource(live.jdbc, live.user, live.password).load().migrate();

        try (Connection from = local.getConnection();
             Connection to = DriverManager.getConnection(live.jdbc, live.user, live.password)) {
            // Only for launch day: once real customers have ordered, the live site is never overwritten.
            try (Statement s = to.createStatement();
                 ResultSet rs = s.executeQuery("select (select count(*) from orders) + (select count(*) from customer)")) {
                rs.next();
                if (rs.getLong(1) > 0) {
                    throw new IllegalStateException("The live site already has orders or customers, so nothing was copied. "
                            + "This copy is only for before launch.");
                }
            }
            List<String> tables = tablesInOrder(from);
            to.setAutoCommit(false);
            try (Statement s = to.createStatement()) {
                List<String> all = new ArrayList<>(tables);
                all.addAll(CLEARED);
                s.execute("truncate table " + String.join(", ", new LinkedHashSet<>(all)) + " restart identity cascade");
                s.execute("alter sequence orders_seq restart");
            }
            for (String t : tables) {
                if (CLEARED.contains(t)) continue;
                int n = copy(from, to, t);
                System.out.printf("  %-22s %d%n", t, n);
            }
            try (Statement s = to.createStatement()) {
                s.execute("update promo_code set times_used = 0");
                for (String t : tables) {
                    if (CLEARED.contains(t) || !hasId(from, t)) continue;
                    s.execute("select setval(seq, coalesce((select max(id) from " + t + "), 0) + 1, false) "
                            + "from (select pg_get_serial_sequence('" + t + "', 'id') seq) q where seq is not null");
                }
            }
            to.commit();
        }
        System.out.println("Done. The live site now has your menu and settings, with no test orders.");
    }

    private static boolean hasId(Connection c, String table) throws SQLException {
        try (ResultSet rs = c.getMetaData().getColumns(null, "public", table, "id")) {
            return rs.next();
        }
    }

    /** Tables in an order where every table comes after the ones it points to. */
    static List<String> tablesInOrder(Connection c) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        try (ResultSet rs = md.getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String t = rs.getString("TABLE_NAME");
                if (!t.equals(HISTORY)) deps.put(t, new LinkedHashSet<>());
            }
        }
        for (String t : deps.keySet()) {
            try (ResultSet rs = md.getImportedKeys(null, "public", t)) {
                while (rs.next()) {
                    String parent = rs.getString("PKTABLE_NAME");
                    if (!parent.equals(t)) deps.get(t).add(parent);
                }
            }
        }
        List<String> out = new ArrayList<>();
        while (out.size() < deps.size()) {
            boolean progress = false;
            for (var e : deps.entrySet()) {
                if (!out.contains(e.getKey()) && out.containsAll(e.getValue())) {
                    out.add(e.getKey());
                    progress = true;
                }
            }
            if (!progress) throw new IllegalStateException("Tables point at each other in a loop: " + deps);
        }
        return out;
    }

    private static int copy(Connection from, Connection to, String table) throws SQLException {
        int n = 0;
        try (Statement s = from.createStatement(); ResultSet rs = s.executeQuery("select * from " + table)) {
            ResultSetMetaData md = rs.getMetaData();
            List<String> cols = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) cols.add('"' + md.getColumnName(i) + '"');
            String sql = "insert into " + table + " (" + String.join(", ", cols) + ") values ("
                    + String.join(", ", cols.stream().map(x -> "?").toList()) + ")";
            try (PreparedStatement ins = to.prepareStatement(sql)) {
                while (rs.next()) {
                    for (int i = 1; i <= cols.size(); i++) ins.setObject(i, rs.getObject(i), md.getColumnType(i));
                    ins.addBatch();
                    if (++n % 50 == 0) ins.executeBatch();
                }
                ins.executeBatch();
            }
        }
        return n;
    }

    /** Railway's "Postgres connection URL", e.g. postgresql://postgres:secret@host.proxy.rlwy.net:12345/railway. */
    record Live(String jdbc, String user, String password) {
        static Live parse(String url) {
            URI u = URI.create(url.trim().replaceFirst("^postgres(ql)?://", "postgresql://"));
            if (u.getHost() == null || u.getUserInfo() == null) {
                throw new IllegalArgumentException("That doesn't look like a Postgres connection URL (postgresql://user:password@host:port/db).");
            }
            String[] info = u.getRawUserInfo().split(":", 2);
            String db = u.getPath() == null || u.getPath().length() < 2 ? "railway" : u.getPath().substring(1);
            int port = u.getPort() == -1 ? 5432 : u.getPort();
            return new Live("jdbc:postgresql://" + u.getHost() + ":" + port + "/" + db,
                    URLDecoder.decode(info[0], StandardCharsets.UTF_8),
                    info.length > 1 ? URLDecoder.decode(info[1], StandardCharsets.UTF_8) : "");
        }
    }
}
