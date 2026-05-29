package com.kulhad.manager.di

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped audit identity provider.
 *
 * Starts with "System" as the anonymous default and should be updated to the
 * logged-in user's display name via [setCurrentUser] on successful login.
 *
 * Intentionally separate from [SessionManager] (which tracks the full user record
 * and numeric ID for FK purposes). [UserSessionManager] supplies the TEXT name
 * stamped in audit columns (audit_created_by / audit_updated_by) so those fields
 * remain human-readable even if the originating user record is later deleted.
 *
 * Usage in repositories:
 *   val user = userSessionManager.currentUser.value
 *   entity.copy(auditCreatedBy = user, auditCreatedAt = System.currentTimeMillis())
 */
@Singleton
class UserSessionManager @Inject constructor() {

    private val _currentUser = MutableStateFlow("System")

    /** The display name to stamp on new audit records. Initial value: "System". */
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    /**
     * Update the audit identity.  Call this on successful login.
     * Blank names are silently normalised back to "System".
     */
    fun setCurrentUser(name: String) {
        _currentUser.value = name.ifBlank { "System" }
    }

    /** Reset to the anonymous default. Call this on logout. */
    fun clear() {
        _currentUser.value = "System"
    }
}
