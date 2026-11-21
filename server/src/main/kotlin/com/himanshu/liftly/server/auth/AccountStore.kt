package com.himanshu.liftly.server.auth

import com.himanshu.liftly.core.protocol.Account
import com.himanshu.liftly.server.Sql
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

enum class Provider(val column: String) { APPLE("apple_sub"), GOOGLE("google_sub") }

/** Outcome of presenting a refresh token. */
sealed interface RefreshResult {
    data class Ok(val userId: String) : RefreshResult

    /** Unknown, expired, logged out, or reused: the caller must sign in again. */
    data object Rejected : RefreshResult
}

/** Users and refresh tokens in PostgreSQL. Only refresh-token hashes are stored. */
class AccountStore(private val ds: DataSource) {

    /** The user for this provider subject, created on first sign-in. Keeps the email current. */
    suspend fun findOrCreate(provider: Provider, subject: String, email: String?, now: Long): Account = Sql.transaction(ds) { c ->
        val existing = c.prepareStatement("SELECT id, email FROM users WHERE ${provider.column} = ? FOR UPDATE").use { st ->
            st.setString(1, subject)
            st.executeQuery().use { rs -> if (rs.next()) Account(rs.getString(1), rs.getString(2)) else null }
        }
        when {
            existing == null -> {
                val id = UUID.randomUUID().toString()
                c.prepareStatement("INSERT INTO users (id, ${provider.column}, email, created_at) VALUES (?, ?, ?, ?)").use { st ->
                    st.setString(1, id)
                    st.setString(2, subject)
                    st.setString(3, email)
                    st.setLong(4, now)
                    st.executeUpdate()
                }
                Account(id, email)
            }

            email != null && email != existing.email -> {
                c.prepareStatement("UPDATE users SET email = ? WHERE id = ?").use { st ->
                    st.setString(1, email)
                    st.setString(2, existing.userId)
                    st.executeUpdate()
                }
                existing.copy(email = email)
            }

            else -> existing
        }
    }

    /** Dev auth only: makes sure a row exists for a header-supplied id. */
    suspend fun ensureDevUser(id: String, now: Long) = Sql.transaction(ds) { c ->
        c.prepareStatement("INSERT INTO users (id, created_at) VALUES (?, ?) ON CONFLICT DO NOTHING").use { st ->
            st.setString(1, id)
            st.setLong(2, now)
            st.executeUpdate()
        }
    }

    suspend fun account(userId: String): Account? = Sql.transaction(ds) { c ->
        c.prepareStatement("SELECT id, email FROM users WHERE id = ?").use { st ->
            st.setString(1, userId)
            st.executeQuery().use { rs -> if (rs.next()) Account(rs.getString(1), rs.getString(2)) else null }
        }
    }

    suspend fun saveRefreshToken(userId: String, hash: String, now: Long) = Sql.transaction(ds) { c -> insertRefresh(c, userId, hash, now) }

    /**
     * Single-use rotation: revokes [oldHash] and stores [newHash] for the same user.
     *
     * Presenting an already-rotated token means it may have been stolen, so every session of that
     * user is revoked, except within [REUSE_GRACE_MILLIS] of the rotation: a phone that lost the
     * response on a bad gym connection and retries gets a fresh token instead of being signed out.
     */
    suspend fun rotate(oldHash: String, newHash: String, now: Long): RefreshResult = Sql.transaction(ds) { c ->
        val row =
            c.prepareStatement("SELECT user_id, expires_at, revoked_at, revoke_reason FROM refresh_tokens WHERE token_hash = ? FOR UPDATE")
                .use { st ->
                    st.setString(1, oldHash)
                    st.executeQuery().use { rs ->
                        if (rs.next()) {
                            TokenRow(
                                rs.getString(1),
                                rs.getLong(2),
                                rs.getLong(3).takeUnless {
                                    rs.wasNull()
                                },
                                rs.getString(4),
                            )
                        } else {
                            null
                        }
                    }
                } ?: return@transaction RefreshResult.Rejected
        val revokedAt = row.revokedAt
        when {
            row.expiresAt <= now -> RefreshResult.Rejected

            revokedAt == null -> {
                revoke(c, "token_hash = ?", oldHash, now, "rotated")
                insertRefresh(c, row.userId, newHash, now)
                RefreshResult.Ok(row.userId)
            }

            row.reason == "rotated" && now - revokedAt <= REUSE_GRACE_MILLIS -> {
                insertRefresh(c, row.userId, newHash, now)
                RefreshResult.Ok(row.userId)
            }

            else -> {
                if (row.reason == "rotated") revoke(c, "user_id = ? AND revoked_at IS NULL", row.userId, now, "reuse")
                RefreshResult.Rejected
            }
        }
    }

    suspend fun logout(hash: String, now: Long) = Sql.transaction(ds) { c ->
        revoke(c, "token_hash = ? AND revoked_at IS NULL", hash, now, "logout")
    }

    /** Deletes the user; their records and refresh tokens go with them (ON DELETE CASCADE). */
    suspend fun delete(userId: String) = Sql.transaction(ds) { c ->
        // Waits for any in-flight push of this user (same lock as SyncStore.push).
        Sql.lockUser(c, userId)
        c.prepareStatement("DELETE FROM users WHERE id = ?").use { st ->
            st.setString(1, userId)
            st.executeUpdate()
        }
    }

    private fun insertRefresh(c: Connection, userId: String, hash: String, now: Long) {
        c.prepareStatement("DELETE FROM refresh_tokens WHERE user_id = ? AND expires_at <= ?").use { st ->
            st.setString(1, userId)
            st.setLong(2, now)
            st.executeUpdate()
        }
        c.prepareStatement("INSERT INTO refresh_tokens (token_hash, user_id, expires_at) VALUES (?, ?, ?)").use { st ->
            st.setString(1, hash)
            st.setString(2, userId)
            st.setLong(3, now + AccessTokens.REFRESH_TTL_MILLIS)
            st.executeUpdate()
        }
    }

    private fun revoke(c: Connection, where: String, arg: String, now: Long, reason: String) {
        c.prepareStatement("UPDATE refresh_tokens SET revoked_at = ?, revoke_reason = ? WHERE $where").use { st ->
            st.setLong(1, now)
            st.setString(2, reason)
            st.setString(3, arg)
            st.executeUpdate()
        }
    }

    private data class TokenRow(val userId: String, val expiresAt: Long, val revokedAt: Long?, val reason: String?)

    companion object {
        const val REUSE_GRACE_MILLIS = 2 * 60 * 1000L
    }
}
