package com.thodoris.kotoufos.chatting.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.thodoris.kotoufos.chatting.data.firebase.FirebaseAuthManager
import javax.inject.Inject

class AuthViewModel @Inject constructor(private val authManager: FirebaseAuthManager) :
    ViewModel() {
    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        authManager.signIn(email, password, onResult)
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        authManager.signUp(email, password, onResult)
    }

    fun singOut() {
        authManager.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return authManager.isUserLoggedIn()
    }
}