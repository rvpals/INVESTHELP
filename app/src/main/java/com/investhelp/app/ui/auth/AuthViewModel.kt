package com.investhelp.app.ui.auth

import androidx.lifecycle.ViewModel
import com.investhelp.app.auth.AuthManager
import com.investhelp.app.auth.AuthState
import com.investhelp.app.auth.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    val biometricHelper: BiometricHelper
) : ViewModel() {

    val authState: StateFlow<AuthState> = authManager.authState

    init {
        if (authManager.authState.value is AuthState.Loading) {
            authManager.checkAuthState()
        }
    }

    fun setupPassword(password: String): Boolean {
        return authManager.setupPassword(password)
    }

    fun unlockWithPassword(password: String): Boolean {
        return authManager.unlockWithPassword(password)
    }

    fun unlockWithBiometric(): Boolean {
        return authManager.unlockWithBiometric()
    }

    fun lockScreen() {
        authManager.lockScreen()
    }
}
