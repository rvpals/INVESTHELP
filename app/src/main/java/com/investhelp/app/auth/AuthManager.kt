package com.investhelp.app.auth

import com.investhelp.app.data.local.DatabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Loading : AuthState
    data object NeedsSetup : AuthState
    data object Locked : AuthState
    data object Unlocked : AuthState
    data class Error(val message: String) : AuthState
}

@Singleton
class AuthManager @Inject constructor(
    private val passwordManager: PasswordManager,
    private val databaseProvider: DatabaseProvider
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun checkAuthState() {
        _authState.value = if (passwordManager.isSetupComplete) {
            AuthState.Locked
        } else {
            AuthState.NeedsSetup
        }
    }

    fun setupPassword(password: String): Boolean {
        return try {
            val passphrase = passwordManager.setupPassword(password)
            passwordManager.cachePassphrase(passphrase)
            databaseProvider.open(passphrase)
            _authState.value = AuthState.Unlocked
            true
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Setup failed: ${e.message}")
            false
        }
    }

    fun unlockWithPassword(password: String): Boolean {
        val passphrase = passwordManager.validateAndGetPassphrase(password)
        return if (passphrase != null) {
            passwordManager.cachePassphrase(passphrase)
            databaseProvider.open(passphrase)
            _authState.value = AuthState.Unlocked
            true
        } else {
            _authState.value = AuthState.Error("Incorrect password")
            false
        }
    }

    fun unlockWithBiometric(): Boolean {
        val passphrase = passwordManager.getStoredPassphrase()
        return if (passphrase != null) {
            databaseProvider.open(passphrase)
            _authState.value = AuthState.Unlocked
            true
        } else {
            _authState.value = AuthState.Error("Biometric unlock unavailable. Please use password.")
            false
        }
    }

    fun lock() {
        databaseProvider.close()
        _authState.value = AuthState.Locked
    }
}
