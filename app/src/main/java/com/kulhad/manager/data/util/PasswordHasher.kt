package com.kulhad.manager.data.util

import java.security.MessageDigest

object PasswordHasher {
    /** Returns the hex SHA-256 of [plain]. */
    fun sha256(plain: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(plain.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
