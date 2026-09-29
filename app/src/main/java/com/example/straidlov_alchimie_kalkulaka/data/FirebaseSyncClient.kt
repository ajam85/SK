package com.example.straidlov_alchimie_kalkulaka.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.straidlov_alchimie_kalkulaka.model.CombinationStatus
import com.example.straidlov_alchimie_kalkulaka.model.GridCombination
import com.example.straidlov_alchimie_kalkulaka.model.GridUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class FirebaseSyncState {
    LOCAL_ONLY,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class FirebaseSyncLogEntry(
    val timestamp: Long,
    val combinationId: Int? = null,
    val success: Boolean,
    val message: String
)

/**
 * Optional Firebase transport. Local storage remains the source of truth for
 * the UI; this class only sends changed records and reports remote records.
 */
class FirebaseSyncClient(context: Context) {

    companion object {
        private const val TAG = "FirebaseSyncClient"
        private const val ROOT_PATH = "combinations"
    }

    private val appContext = context.applicationContext

    private val app: FirebaseApp? = runCatching {
        FirebaseApp.initializeApp(appContext)
    }.onFailure {
        Log.e(TAG, "Firebase initialization failed", it)
    }.getOrNull()

    private val auth: FirebaseAuth? = runCatching {
        app?.let { FirebaseAuth.getInstance(it) }
    }.onFailure {
        Log.e(TAG, "Firebase Auth initialization failed", it)
    }.getOrNull()

    private val database: FirebaseDatabase? = runCatching {
        app?.let { firebaseApp ->
            val urlResourceId = appContext.resources.getIdentifier(
                "firebase_database_url",
                "string",
                appContext.packageName
            )
            val databaseUrl = urlResourceId.takeIf { it != 0 }
                ?.let { resourceId -> appContext.getString(resourceId) }
                ?.takeIf { it.isNotBlank() }

            if (databaseUrl != null) {
                FirebaseDatabase.getInstance(firebaseApp, databaseUrl)
            } else {
                FirebaseDatabase.getInstance(firebaseApp)
            }
        }
    }.onFailure {
        Log.e(TAG, "Firebase Realtime Database initialization failed", it)
    }.getOrNull()

    private val combinationsRef: DatabaseReference? by lazy {
        database?.getReference(ROOT_PATH)
    }

    private var remoteHandler: ((GridCombination) -> Unit)? = null
    private var readyHandler: (() -> Unit)? = null
    private var listenerAttached = false
    private var readyNotified = false
    private val pendingWrites = linkedMapOf<Int, GridCombination>()

    private val _state = MutableStateFlow(FirebaseSyncState.CONNECTING)
    val state: StateFlow<FirebaseSyncState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val logPreferences = appContext.getSharedPreferences("firebase_sync_log", Context.MODE_PRIVATE)
    private val _log = MutableStateFlow(loadLog())
    val log: StateFlow<List<FirebaseSyncLogEntry>> = _log.asStateFlow()

    init {
        database?.let {
            try {
                it.setPersistenceEnabled(true)
            } catch (e: DatabaseException) {
                Log.w(TAG, "Firebase persistence was already configured", e)
            }
        }

        if (app == null) {
            Log.i(TAG, "Firebase is not configured; local-only mode is active")
            _state.value = FirebaseSyncState.LOCAL_ONLY
            addLog(null, false, "Firebase není nakonfigurován; pracuje se pouze lokálně")
        }
    }

    fun start(
        onRemoteCombination: (GridCombination) -> Unit,
        onReady: () -> Unit
    ) {
        remoteHandler = onRemoteCombination
        readyHandler = onReady
        if (auth == null || combinationsRef == null) {
            reportError("Firebase není správně nakonfigurován")
            return
        }
        _state.value = FirebaseSyncState.CONNECTING
        ensureAuthenticated()
    }

    fun currentUserId(): String = auth?.currentUser?.uid.orEmpty()

    fun publish(combination: GridCombination) {
        if (combination.status == CombinationStatus.UNTESTED) return
        if (auth == null || combinationsRef == null) return

        pendingWrites[combination.id] = combination
        ensureAuthenticated()
    }

    private fun ensureAuthenticated() {
        val firebaseAuth = auth ?: return
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            attachListenerIfNeeded()
            flushPendingWrites()
            _state.value = FirebaseSyncState.CONNECTED
            notifyReadyIfNeeded()
            return
        }

        _state.value = FirebaseSyncState.CONNECTING
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    attachListenerIfNeeded()
                    flushPendingWrites()
                    _state.value = FirebaseSyncState.CONNECTED
                    addLog(null, true, "Anonymní přihlášení k Firebase proběhlo úspěšně")
                    notifyReadyIfNeeded()
                } else {
                    reportError("Anonymní přihlášení Firebase selhalo")
                    Log.w(TAG, "Anonymous Firebase authentication failed", task.exception)
                }
        }
    }

    private fun notifyReadyIfNeeded() {
        if (readyNotified) return
        readyNotified = true
        readyHandler?.invoke()
    }

    private fun attachListenerIfNeeded() {
        val ref = combinationsRef ?: return
        if (listenerAttached) return

        listenerAttached = true
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                addLog(snapshot.key?.toIntOrNull(), true, "Načteno z databáze")
                notifyRemote(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                addLog(snapshot.key?.toIntOrNull(), true, "Aktualizováno z databáze")
                notifyRemote(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) = Unit

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(error: DatabaseError) {
                reportError("Firebase databáze odmítla čtení: ${error.message}")
                Log.w(TAG, "Firebase listener cancelled: ${error.message}")
            }
        })
    }

    private fun notifyRemote(snapshot: DataSnapshot) {
        val id = snapshot.key?.toIntOrNull() ?: return
        if (id !in 0 until GridUtils.TOTAL_COMBINATIONS) return
        val status = when (snapshot.child("status").getValue(String::class.java)) {
            "NEGATIVE" -> CombinationStatus.NONFUNCTIONAL
            "POSITIVE" -> CombinationStatus.FUNCTIONAL
            else -> return
        }
        val updatedAt = (snapshot.child("updatedAt").value as? Number)
            ?.toLong()
            ?.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val updatedBy = snapshot.child("updatedBy").getValue(String::class.java).orEmpty()

        remoteHandler?.invoke(
            GridCombination(
                id = id,
                status = status,
                note = snapshot.child("note").getValue(String::class.java).orEmpty(),
                author = snapshot.child("author").getValue(String::class.java).orEmpty(),
                updatedAt = updatedAt,
                updatedBy = updatedBy
            )
        )
    }

    private fun flushPendingWrites() {
        val ref = combinationsRef ?: return
        val uid = currentUserId()
        if (uid.isBlank() || pendingWrites.isEmpty()) return

        val writes = pendingWrites.values.toList()
        pendingWrites.clear()

        for (combination in writes) {
            ref.child(combination.id.toString())
                .setValue(toRemoteMap(combination, uid))
                .addOnSuccessListener {
                    _state.value = FirebaseSyncState.CONNECTED
                    _lastError.value = null
                    addLog(combination.id, true, "Zapsáno do databáze")
                }
                .addOnFailureListener { error ->
                    pendingWrites[combination.id] = combination
                    val message = "Zápis do Firebase selhal: ${error.message.orEmpty()}"
                    reportError(message, combination.id)
                    Log.w(TAG, "Firebase write failed for ${combination.id}", error)
                }
        }
    }

    private fun reportError(message: String, combinationId: Int? = null) {
        _state.value = FirebaseSyncState.ERROR
        _lastError.value = message
        addLog(combinationId, false, message)
    }

    private fun addLog(combinationId: Int?, success: Boolean, message: String) {
        val updated = (_log.value + FirebaseSyncLogEntry(
            timestamp = System.currentTimeMillis(),
            combinationId = combinationId,
            success = success,
            message = message
        )).takeLast(200)
        _log.value = updated

        val json = JSONArray()
        updated.forEach { entry ->
            json.put(
                JSONObject().apply {
                    put("timestamp", entry.timestamp)
                    put("combinationId", entry.combinationId ?: JSONObject.NULL)
                    put("success", entry.success)
                    put("message", entry.message)
                }
            )
        }
        logPreferences.edit().putString("entries", json.toString()).apply()
    }

    private fun loadLog(): List<FirebaseSyncLogEntry> {
        return runCatching {
            val json = JSONArray(logPreferences.getString("entries", "[]"))
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        FirebaseSyncLogEntry(
                            timestamp = item.optLong("timestamp"),
                            combinationId = if (item.isNull("combinationId")) null else item.optInt("combinationId"),
                            success = item.optBoolean("success"),
                            message = item.optString("message")
                        )
                    )
                }
            }.takeLast(200)
        }.getOrDefault(emptyList())
    }

    private fun toRemoteMap(combination: GridCombination, uid: String): Map<String, Any> {
        return mapOf(
            "status" to when (combination.status) {
                CombinationStatus.FUNCTIONAL -> "POSITIVE"
                CombinationStatus.NONFUNCTIONAL -> "NEGATIVE"
                CombinationStatus.UNTESTED -> "UNUSED"
            },
            "note" to combination.note,
            "author" to combination.author,
            "updatedAt" to combination.updatedAt,
            "updatedBy" to uid
        )
    }
}
