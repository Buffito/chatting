package com.thodoris.kotoufos.chatting.data.firebase

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
}