package com.madelmade.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Photos and videos live in the database, and a replaced photo used to stay behind with its full-size original.
 * This finds the ones nothing uses any more and removes them, then gives the space back to the disk.
 */
@Service
public class StorageService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorageService.class);

    public record Usage(long databaseBytes, int photos, long photoBytes, int unusedPhotos, long unusedPhotoBytes,
                        int videos, long videoBytes, int unusedVideos, long unusedVideoBytes) {}

    /** shrinkError is why the space couldn't be handed back to the disk, or null when it was. */
    public record Cleanup(int photos, int videos, long bytesBefore, long bytesAfter, String shrinkError) {}

    private final JdbcTemplate jdbc;

    public StorageService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Usage usage() {
        long db = jdbc.queryForObject("select pg_database_size(current_database())", Long.class);
        Map<String, Object> p = jdbc.queryForMap("select count(*) n, coalesce(sum(coalesce(octet_length(original), 0) + octet_length(edited)), 0) b from photo");
        Map<String, Object> up = jdbc.queryForMap("select count(*) n, coalesce(sum(coalesce(octet_length(original), 0) + octet_length(edited)), 0) b from photo p where "
                + unused("photo", "p"));
        Map<String, Object> v = jdbc.queryForMap("select count(*) n, coalesce(sum(octet_length(data)), 0) b from video");
        Map<String, Object> uv = jdbc.queryForMap("select count(*) n, coalesce(sum(octet_length(data)), 0) b from video v where " + unused("video", "v"));
        return new Usage(db, n(p), b(p), n(up), b(up), n(v), b(v), n(uv), b(uv));
    }

    /** Every night, so replaced photos don't pile up. Photos are saved and attached in one step, so none is ever half-saved. */
    @Scheduled(cron = "0 0 3 * * *", zone = "${app.zone}")
    public void nightly() {
        clean();
    }

    /** Deletes photos and videos nothing points at, then rewrites their tables so the freed space goes back to the disk. */
    public Cleanup clean() {
        long before = jdbc.queryForObject("select pg_database_size(current_database())", Long.class);
        int photos = jdbc.update("delete from photo p where " + unused("photo", "p"));
        int videos = jdbc.update("delete from video v where " + unused("video", "v"));
        // Plain deletes only mark the space free inside Postgres; VACUUM FULL hands it back to the volume.
        // It writes a fresh copy of each table first, so on a nearly full disk it can fail. Then a plain
        // VACUUM still lets Postgres reuse the space for new photos, and gives back what it can.
        String error = null;
        for (String table : List.of("video", "photo")) {
            try {
                jdbc.execute("vacuum full " + table);
            } catch (RuntimeException e) {
                error = rootMessage(e);
                log.warn("VACUUM FULL {} failed: {}", table, error);
                try {
                    jdbc.execute("vacuum " + table);
                } catch (RuntimeException ignored) {
                    // nothing more to do
                }
            }
        }
        long after = jdbc.queryForObject("select pg_database_size(current_database())", Long.class);
        return new Cleanup(photos, videos, before, after, error);
    }

    /** A where clause matching rows of the table that no foreign key and no setting refers to. */
    private String unused(String table, String alias) {
        List<String> checks = new ArrayList<>();
        // Every column anywhere that points at this table, so new features are covered without changes here.
        List<Map<String, Object>> refs = jdbc.queryForList("""
                select c.conrelid::regclass::text as tbl, a.attname as col
                from pg_constraint c
                join pg_attribute a on a.attrelid = c.conrelid and a.attnum = any(c.conkey)
                where c.contype = 'f' and c.confrelid = ?::regclass""", table);
        for (Map<String, Object> r : refs) {
            checks.add("not exists (select 1 from " + r.get("tbl") + " x where x." + r.get("col") + " = " + alias + ".id)");
        }
        // Settings like about.photoId and cert.photoId hold ids as text.
        checks.add("not exists (select 1 from setting s where lower(s.key) like '%" + table + "id' and s.value = " + alias + ".id::text)");
        return checks.stream().collect(Collectors.joining(" and "));
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    private static int n(Map<String, Object> m) {
        return ((Number) m.get("n")).intValue();
    }

    private static long b(Map<String, Object> m) {
        return ((Number) m.get("b")).longValue();
    }

    public static String mb(long bytes) {
        return bytes < 1024 * 1024 ? (bytes / 1024) + " KB" : String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
