package com.pim.wspolnemieszkanie.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FlatRepository {

    private val db = FirebaseFirestore.getInstance()

    fun createFlat(
        name: String,
        userId: String,
        userName: String,
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
        // Osobna kolekcja kodów pozwala znaleźć mieszkanie osobie, która jeszcze do niego nie należy.
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
                    "memberNames" to mapOf(userId to userName),
                ),
            )
            transaction.set(codeRef, hashMapOf("flatId" to flatRef.id))
            true
        }.addOnSuccessListener { created ->
            if (created) {
                onSuccess()
            } else {
                createFlat(name, userId, userName, onSuccess, onError, attemptsLeft - 1)
            }
        }.addOnFailureListener {
            onError()
        }
    }

    fun joinFlat(
        code: String,
        userId: String,
        userName: String,
        onSuccess: () -> Unit,
        onCodeNotFound: () -> Unit,
        onError: () -> Unit,
    ) {
        db.collection("inviteCodes").document(code).get()
            .addOnSuccessListener { codeDocument ->
                val flatId = codeDocument.getString("flatId")
                if (flatId == null) {
                    onCodeNotFound()
                    return@addOnSuccessListener
                }
                db.collection("flats").document(flatId)
                    .update(
                        mapOf(
                            "memberIds" to FieldValue.arrayUnion(userId),
                            "memberNames.$userId" to userName,
                        ),
                    )
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError() }
            }
            .addOnFailureListener { onError() }
    }

    fun leaveFlat(
        flat: Flat,
        userId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        val flatRef = db.collection("flats").document(flat.id)
        val codeRef = db.collection("inviteCodes").document(flat.inviteCode)

        // W transakcji czytamy aktualną listę, bo ktoś mógł w tym czasie dołączyć albo wyjść.
        db.runTransaction { transaction ->
            val current = transaction.get(flatRef).toObject(Flat::class.java)
                ?: return@runTransaction
            val remaining = current.memberIds - userId

            if (remaining.isEmpty()) {
                transaction.delete(flatRef)
                transaction.delete(codeRef)
            } else {
                val newOwnerId =
                    if (current.ownerId == userId) remaining.first() else current.ownerId
                transaction.update(
                    flatRef,
                    mapOf(
                        "memberIds" to remaining,
                        "memberNames.$userId" to FieldValue.delete(),
                        "ownerId" to newOwnerId,
                    ),
                )
            }
        }.addOnSuccessListener {
            onSuccess()
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
