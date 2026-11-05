package com.pim.wspolnemieszkanie.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Imiona współlokatorów z users/{uid} (pole "name", a gdy go brak, "email").
 *
 * Dokumenty users/{uid} zapisuje rejestracja (zadanie Osoby 1). Dopóki ich nie ma,
 * ekrany pokazują "Współlokator 1, 2…" zamiast imion.
 */
class MemberRepository {

    private val db = FirebaseFirestore.getInstance()

    fun observeNames(
        memberIds: List<String>,
        onChange: (Map<String, String>) -> Unit,
    ): ListenerRegistration? {
        // Firestore pozwala na maks. 30 wartości w zapytaniu "in", a pusta lista jest błędem.
        val ids = memberIds.distinct().take(30)
        if (ids.isEmpty()) {
            onChange(emptyMap())
            return null
        }
        return db.collection("users")
            .whereIn(FieldPath.documentId(), ids)
            .addSnapshotListener { snapshot, error ->
                // Brak dostępu albo kolekcji to nie błąd krytyczny, zostają nazwy zastępcze.
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name")?.takeIf { it.isNotBlank() }
                        ?: doc.getString("email")?.takeIf { it.isNotBlank() }
                    name?.let { doc.id to it }
                }.toMap())
            }
    }
}
