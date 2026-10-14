package com.madelmade.service;

import com.madelmade.domain.LoginCode;
import com.madelmade.repo.LoginCodeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Email sign-in: a 6-digit code that works for 10 minutes. No passwords anywhere. */
@Service
public class LoginCodeService {
    public static final Duration LIFETIME = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_CODES_PER_HOUR = 6;

    private final LoginCodeRepository repo;
    private final SecureRandom random = new SecureRandom();

    public LoginCodeService(LoginCodeRepository repo) {
        this.repo = repo;
    }

    public enum Outcome { OK, WRONG, EXPIRED, TOO_MANY }

    /** Creates a new code, or empty when this email asked for too many recently. */
    @Transactional
    public Optional<String> issue(String email) {
        String clean = email.trim().toLowerCase();
        if (repo.countByEmailAndCreatedAtAfter(clean, Instant.now().minus(Duration.ofHours(1))) >= MAX_CODES_PER_HOUR) {
            return Optional.empty();
        }
        repo.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(clean).ifPresent(old -> old.setUsed(true));
        String code = String.format("%06d", random.nextInt(1_000_000));
        LoginCode lc = new LoginCode();
        lc.setEmail(clean);
        lc.setCodeHash(hash(clean, code));
        lc.setExpiresAt(Instant.now().plus(LIFETIME));
        repo.save(lc);
        return Optional.of(code);
    }

    @Transactional
    public Outcome verify(String email, String code) {
        String clean = email.trim().toLowerCase();
        String digits = code == null ? "" : code.replaceAll("\\D", "");
        Optional<LoginCode> found = repo.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(clean);
        if (found.isEmpty()) return Outcome.EXPIRED;
        LoginCode lc = found.get();
        if (lc.getExpiresAt().isBefore(Instant.now())) return Outcome.EXPIRED;
        if (lc.getAttempts() >= MAX_ATTEMPTS) return Outcome.TOO_MANY;
        lc.setAttempts(lc.getAttempts() + 1);
        if (!MessageDigest.isEqual(lc.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(clean, digits).getBytes(StandardCharsets.UTF_8))) {
            return lc.getAttempts() >= MAX_ATTEMPTS ? Outcome.TOO_MANY : Outcome.WRONG;
        }
        lc.setUsed(true);
        return Outcome.OK;
    }

    @Transactional
    public void cleanUp() {
        repo.deleteByExpiresAtBefore(Instant.now().minus(Duration.ofDays(1)));
    }

    private static String hash(String email, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
