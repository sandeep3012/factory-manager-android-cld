package com.kulhad.manager.di

import com.kulhad.manager.data.local.KulhadDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-scoped current-user holder. Cleared on logout. */
@Singleton
class SessionManager @Inject constructor() {

    data class CurrentUser(val id: Long, val name: String, val email: String)

    private val _user = MutableStateFlow<CurrentUser?>(null)
    val user: StateFlow<CurrentUser?> = _user.asStateFlow()

    /** Returns the demo user id (1) when no real user has logged in yet. */
    val currentUserId: Long get() = _user.value?.id ?: 1L
    val currentUserName: String get() = _user.value?.name ?: KulhadDatabase.DEMO_NAME

    fun setUser(id: Long, name: String, email: String) {
        _user.value = CurrentUser(id, name, email)
    }

    fun clear() {
        _user.value = null
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    // SessionManager is provided implicitly via @Inject constructor.
}
