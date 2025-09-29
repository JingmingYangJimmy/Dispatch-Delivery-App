// com/laioffer/deliver/security/store/impl/SessionStoreJdbc.java
package com.laioffer.deliver.security.store.impl;

import com.laioffer.deliver.security.store.SessionStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class SessionStoreJdbc implements SessionStore {
    private final JdbcTemplate jdbc;

    public SessionStoreJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void createSession(long userId, String sid, String refreshPlain, Instant expiresAt, String deviceInfo) {
        jdbc.update("""
           INSERT INTO refresh_sessions(user_id,sid,refresh_hash,expires_at,device_info)
           VALUES (?,?,?,?,?)
        """, userId, sid, sha256(refreshPlain), java.sql.Timestamp.from(expiresAt), deviceInfo);
    }

    @Override
    public void rotateSession(String oldSid, String newSid, String newRefreshPlain, Instant newExpiresAt) {
        jdbc.update("UPDATE refresh_sessions SET revoked=TRUE WHERE sid=?", oldSid);
        jdbc.update("""
           INSERT INTO refresh_sessions(user_id,sid,refresh_hash,expires_at)
           SELECT user_id, ?, ?, ? FROM refresh_sessions WHERE sid=? LIMIT 1
        """, newSid, sha256(newRefreshPlain), java.sql.Timestamp.from(newExpiresAt), oldSid);
    }

    @Override public void revokeBySid(String sid) { jdbc.update("UPDATE refresh_sessions SET revoked=TRUE WHERE sid=?", sid); }

    @Override public void revokeAll(long userId) { jdbc.update("UPDATE refresh_sessions SET revoked=TRUE WHERE user_id=? AND revoked=FALSE", userId); }

    @Override
    public boolean isRefreshValid(long userId, String sid, String refreshPlain) {
        Integer ok = jdbc.query("""
           SELECT 1 FROM refresh_sessions
            WHERE user_id=? AND sid=? AND revoked=FALSE AND refresh_hash=? AND expires_at>now()
            LIMIT 1
        """, ps -> {
            ps.setLong(1, userId);
            ps.setString(2, sid);
            ps.setString(3, sha256(refreshPlain));
        }, rs -> rs.next() ? 1 : 0);
        return ok != null && ok == 1;
    }
}
