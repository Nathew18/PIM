package com.pim.wspolnemieszkanie.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FlatRepository {

    private val db = FirebaseFirestore.getInstance()

    fun createFlat(
        name: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
        attemptsLeft: Int = 5,
    ) {
        if (attemptsLeft == 0) {
            onError()
            return
        }

        val code = generateInviteCode()
        val codeRef = db.collection("inviteCodes").document(code)
        val flatRef = db.collection("flats").document()

        // Transakcja gwarantuje, że dwa mieszkania nie dostaną tego samego kodu.
        // Osobna kolekcja kodów pozwoli w #6 znaleźć mieszkanie osobie, która jeszcze do niego nie należy.
        db.runTransaction { transaction ->
            if (transaction.get(codeRef).exists()) {
                return@runTransaction false
            }
            transaction.set(
                flatRef,
                hashMapOf(
                    "name" to name,
                    "inviteCode" to code,
                    "ownerId" to userId,
                    "memberIds" to listOf(userId),
                ),
            )
            transaction.set(codeRef, hashMapOf("flatId" to flatRef.id))
            true
        }.addOnSuccessListener { created ->
            if (created) {
                onSuccess()
            } else {
                createFlat(name, userId, onSuccess, onError, attemptsLeft - 1)
            }
        }.addOnFailureListener {
            onError()
        }
    }

    fun observeUserFlat(
        userId: String,
        onChange: (Flat?) -> Unit,
        onError: () -> Unit,
    ): ListenerRegistration =
        db.collection("flats")
            .whereArrayContains("memberIds", userId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onError()
                    return@addSnapshotListener
                }
                val document = snapshot.documents.firstOrNull()
                onChange(document?.toObject(Flat::class.java)?.copy(id = document.id))
            }
}
