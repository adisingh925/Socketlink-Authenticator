package com.socketlink.android.authenticator

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel to manage OTP entries and their progress for TOTP codes.
 *
 * @param application Application context to access storage.
 */
class OtpViewModel(application: Application) : AndroidViewModel(application) {

    private val _otpEntries = MutableStateFlow<List<OtpEntry>>(emptyList())
    val otpEntries: StateFlow<List<OtpEntry>> = _otpEntries

    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressMap: StateFlow<Map<String, Float>> = _progressMap

    var selectedOtpEntries by mutableStateOf<List<OtpEntry>>(emptyList())

    val db by lazy { Firebase.firestore }
    val userId by lazy { Utils.sha256(Utils.getOrCreateUserId(getApplication())) }
    private var firestoreListener: ListenerRegistration? = null

    val isSyncing = MutableStateFlow(false)

    init {
        // Initialize Firebase App
        FirebaseApp.initializeApp(application.applicationContext)

        // Initialize DataStore securely
        OtpStorage.initialize(application.applicationContext)

        // Load data and start ticker
        viewModelScope.launch {
            loadOtpEntries()
            updateOtpCodes()
            startTicker()
        }.invokeOnCompletion {
            toggleCloudSync(Utils.isCloudSyncEnabled(application.applicationContext))
        }
    }

    /**
     * Loads OTP secrets from storage and updates the _otpEntries StateFlow.
     */
    private suspend fun loadOtpEntries() {
        val stored = OtpStorage.loadOtpList()
        Log.d("OtpViewModel", "Loaded ${stored.size} OTP entries from storage")
        _otpEntries.value = stored.map {
            it.copy(code = "") // empty code for now
        }
    }

    /**
     * Generates OTP codes for all entries.
     */
    private fun updateOtpCodes() {
        _otpEntries.value = _otpEntries.value.map { otp ->
            val code = OtpUtils.generateOtp(
                otp.secret,
                otp.digits,
                otp.algorithm,
                otp.period
            )
            otp.copy(code = code)
        }
    }

    /**
     * Periodically updates OTP codes and progress.
     */
    private fun startTicker() {
        viewModelScope.launch {
            val lastPeriodMap = mutableMapOf<String, Long>()

            while (true) {
                val now = System.currentTimeMillis()

                val updatedCodes = _otpEntries.value.map { otp ->
                    val periodMillis = otp.period * 1000L
                    val currentPeriod = now / periodMillis
                    val lastPeriod = lastPeriodMap[otp.id] ?: -1L

                    val newCode = if (currentPeriod != lastPeriod) {
                        OtpUtils.generateOtp(otp.secret, otp.digits, otp.algorithm, otp.period)
                    } else {
                        otp.code
                    }

                    lastPeriodMap[otp.id] = currentPeriod
                    otp.copy(code = newCode)
                }

                _otpEntries.value = updatedCodes

                val newProgressMap = updatedCodes.associate { otp ->
                    val periodMillis = otp.period * 1000L
                    val elapsedInPeriod = now % periodMillis
                    val progress = 1f - elapsedInPeriod.toFloat() / periodMillis
                    otp.id to progress.coerceIn(0f, 1f)
                }

                _progressMap.value = newProgressMap

                delay(10L)
            }
        }
    }

    /**
     * Adds a new OTP entry and saves it securely.
     */
    fun addSecret(secret: OtpEntry) {
        val newEntry = secret.copy(
            code = OtpUtils.generateOtp(
                secret.secret,
                secret.digits,
                secret.algorithm,
                secret.period
            )
        )
        _otpEntries.value = _otpEntries.value + newEntry

        viewModelScope.launch(Dispatchers.IO) {
            OtpStorage.saveOtpList(_otpEntries.value)

            // If cloud sync is enabled, upload the new entry
            if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                uploadUpdatedOrNewOTPs(listOf(secret))
            }
        }
    }

    fun addSecrets(secrets: List<OtpEntry>) {
        val newEntries = secrets.map { secret ->
            secret.copy(
                code = OtpUtils.generateOtp(
                    secret.secret,
                    secret.digits,
                    secret.algorithm,
                    secret.period
                )
            )
        }

        _otpEntries.value = _otpEntries.value + newEntries

        viewModelScope.launch(Dispatchers.IO) {
            OtpStorage.saveOtpList(_otpEntries.value)

            // If cloud sync is enabled, upload the new entries
            if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                uploadUpdatedOrNewOTPs(newEntries)
            }
        }
    }

    fun stopListeningForCloudChanges() {
        firestoreListener?.remove()
        firestoreListener = null
        Log.d("FirebaseSync", "Firestore listener removed")
    }

    /**
     * Starts listening for real-time changes in the cloud OTP collection.
     * Updates local data only when entries are new or have changed,
     * and ignores entries that are identical between local and cloud.
     */
    fun startListeningForCloudChanges() {
        isSyncing.value = true

        if (firestoreListener != null) {
            isSyncing.value = false
            Log.d("FirebaseSync", "Listener already active")
            return
        }

        val collectionRef = db.collection("users").document(userId).collection("OTPs")

        firestoreListener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                isSyncing.value = false
                Log.e("FirebaseSync", "Snapshot listener error", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                viewModelScope.launch {
                    val cloudEntries = snapshot.documents.mapNotNull { it.toObject(OtpEntry::class.java) }

                    val localEntries = _otpEntries.value
                    val cloudMap = cloudEntries.associateBy { it.id }
                    val localMap = localEntries.associateBy { it.id }

                    /** Filter entries that are new or updated in cloud */
                    val updatedOrNewFromCloud = cloudEntries.filter { cloudEntry ->
                        val local = localMap[cloudEntry.id]
                        local == null || !cloudEntry.isContentEqual(local)
                    }

                    /** Filter local-only entries that are not in the cloud */
                    val missingInCloud = localEntries.filter { localEntry ->
                        cloudMap[localEntry.id] == null
                    }

                    /** Upload missing local entries to the cloud */
                    if (missingInCloud.isNotEmpty()) {
                        uploadUpdatedOrNewOTPs(missingInCloud)
                    }

                    /** Merge all entries */
                    val merged = (localEntries + updatedOrNewFromCloud)
                        .distinctBy { it.id }
                        .map {
                            it.copy(
                                code = OtpUtils.generateOtp(
                                    it.secret,
                                    it.digits,
                                    it.algorithm,
                                    it.period
                                )
                            )
                        }

                    _otpEntries.value = merged
                    OtpStorage.saveOtpList(merged)

                    Log.d(
                        "FirebaseSync",
                        "Realtime sync: ${updatedOrNewFromCloud.size} from cloud, ${missingInCloud.size} uploaded"
                    )
                }
            }

            isSyncing.value = false
        }
    }

    /**
     * Checks if the content of this OtpEntry equals the other OtpEntry.
     *
     * Compares all fields except for the dynamic 'code' field.
     *
     * @param other The other OtpEntry to compare with.
     * @return True if contents are the same, false otherwise.
     */
    fun OtpEntry.isContentEqual(other: OtpEntry): Boolean {
        return this.id == other.id &&
                this.codeName == other.codeName &&
                this.secret == other.secret &&
                this.digits == other.digits &&
                this.algorithm == other.algorithm &&
                this.period == other.period &&
                this.updatedAt == other.updatedAt
    }

    fun fetchAllFromCloud(onComplete: (List<OtpEntry>) -> Unit) {
        db.collection("users").document(userId).collection("OTPs").get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(OtpEntry::class.java)
                }
                onComplete(entries)
            }.addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to fetch OTPs from cloud", e)
                onComplete(emptyList())
            }
    }

    fun uploadUpdatedOrNewOTPs(updatedOTPs: List<OtpEntry>) {
        isSyncing.value = true

        val userDocRef = db.collection("users").document(userId)
        val collection = userDocRef.collection("OTPs")

        updatedOTPs.forEach { otp ->
            val otpDoc = collection.document(otp.id)
            val data = mapOf(
                "id" to otp.id,
                "codeName" to otp.codeName,
                "secret" to otp.secret,
                "digits" to otp.digits,
                "algorithm" to otp.algorithm,
                "period" to otp.period,
                "updatedAt" to otp.updatedAt,
            )

            otpDoc.set(data, SetOptions.merge()).addOnSuccessListener {
                isSyncing.value = false
                Log.d("FirebaseSync", "Uploaded OTP ${otp.id} successfully")
            }.addOnFailureListener { e ->
                isSyncing.value = false
                Log.e("FirebaseSync", "Failed to upload OTP ${otp.id}", e)
            }
        }
    }

    fun deleteOtpFromFirebase(otpId: String) {
        isSyncing.value = true

        val otpDoc = db.collection("users").document(userId).collection("OTPs").document(otpId)
        otpDoc.delete().addOnSuccessListener {
            isSyncing.value = false
            Log.d("FirebaseSync", "Deleted OTP $otpId successfully")
        }.addOnFailureListener { e ->
            isSyncing.value = false
            Log.e("FirebaseSync", "Failed to delete OTP $otpId", e)
        }
    }

    /**
     * Deletes the given OTP and updates storage.
     */
    fun deleteSecret(otpToDelete: OtpEntry) {
        // Update in-memory list by removing the entry
        _otpEntries.value = _otpEntries.value.filterNot { it.id == otpToDelete.id }

        viewModelScope.launch(Dispatchers.IO) {
            // Save the full updated list directly
            OtpStorage.saveOtpList(_otpEntries.value)

            // If cloud sync is enabled, delete the OTP from Firebase
            if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                deleteOtpFromFirebase(otpToDelete.id)
            }
        }
    }

    fun deleteAll() {
        _otpEntries.value = emptyList<OtpEntry>()

        viewModelScope.launch(Dispatchers.IO) {
            OtpStorage.saveOtpList(emptyList())
        }
    }

    fun toggleCloudSync(enabled: Boolean) {
        if (enabled) {
            startListeningForCloudChanges()
        } else {
            stopListeningForCloudChanges()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForCloudChanges()
    }
}



