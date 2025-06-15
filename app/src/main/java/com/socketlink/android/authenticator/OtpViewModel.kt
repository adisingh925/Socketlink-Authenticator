package com.socketlink.android.authenticator

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private var firestoreListener: ListenerRegistration? = null

    val isSyncing = MutableStateFlow(false)
    var runningSyncProcesses = 0
    val auth = Firebase.auth

    private val repository: OtpRepository = OtpRepository(application)

    init {
        /**
         * Initialize Firebase App with the application context.
         * This ensures Firebase services are ready to use.
         */
        FirebaseApp.initializeApp(application.applicationContext)

        /** Update OTPs */
        startTicker()

        /**
         * Set up an authentication state listener to respond to user sign-in/sign-out events.
         * This will trigger loading of OTP entries when a user is authenticated.
         */
        auth.addAuthStateListener {
            viewModelScope.launch(Dispatchers.IO) {
                loadOtpEntries()
                updateOtpCodes()
            }.invokeOnCompletion {
                toggleCloudSync(Utils.isCloudSyncEnabled(application.applicationContext))
            }
        }
    }

    /** Add OTP */
    fun addOtp(otpList: List<OtpEntry>) {
        viewModelScope.launch(Dispatchers.Main) {
            repository.insertOtp(otpList)
        }
    }

    /** Update OTP */
    fun updateOtp(otpEntry: OtpEntry) {
        viewModelScope.launch(Dispatchers.Main) {
            repository.updateOtp(otpEntry)
        }
    }

    /** Delete OTP */
    fun deleteOtp(otpEntry: OtpEntry) {
        viewModelScope.launch(Dispatchers.Main) {
            repository.deleteOtp(otpEntry)
        }
    }

    private fun startSyncing() {
        if (runningSyncProcesses > 0) {
            Log.w("OtpViewModel", "Already syncing, cannot start another process")
            return
        }

        runningSyncProcesses++
        isSyncing.value = true
    }

    private fun stopSyncing() {
        if (runningSyncProcesses <= 0) {
            Log.w("OtpViewModel", "stopSyncing called but no running sync processes")
            return
        }

        runningSyncProcesses--

        if (runningSyncProcesses > 0) {
            Log.d(
                "OtpViewModel",
                "Sync process stopped, still running $runningSyncProcesses processes"
            )
            return
        }

        isSyncing.value = false
    }

    /**
     * Loads OTP secrets from storage and updates the _otpEntries StateFlow.
     */
    internal suspend fun loadOtpEntries() {
        _otpEntries.value = repository.getAllOTPs(auth.currentUser?.email ?: "")
        Log.d("OtpViewModel", "Loaded ${_otpEntries.value.size} OTP entries from storage")
    }

    /**
     * Generates OTP codes for all entries.
     */
    internal fun updateOtpCodes() {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        addOtp(listOf(secret))

        viewModelScope.launch(Dispatchers.IO) {
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
        addOtp(_otpEntries.value)

        viewModelScope.launch(Dispatchers.IO) {
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

    fun fetchAllFromCloud() {
        if (auth.currentUser == null) {
            Log.w("FirebaseSync", "Cannot fetch OTPs, user is not authenticated")
            return
        } else {
            Log.d("FirebaseSync", "Fetching OTPs for user: ${auth.uid}")
        }

        startSyncing()

        /** Fetch all OTP entries from Firestore cloud collection */
        db.collection("users").document(auth.uid.toString()).collection("OTPs").get().addOnSuccessListener { snapshot ->
            /** Launch coroutine to handle data processing */
            viewModelScope.launch(Dispatchers.IO) {
                /** Map Firestore documents to OtpEntry objects, filtering out nulls */
                val cloudEntries = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(OtpEntry::class.java)
                }

                /** Load all local entries */
                val allLocalEntries = _otpEntries.value

                /** Create maps for quick lookup by ID */
                val cloudMap = cloudEntries.associateBy { it.id }
                val localMap = allLocalEntries.associateBy { it.id }

                /**
                 * Filter entries that are new or updated in the cloud compared to local data.
                 * These need to be merged locally.
                 */
                val updatedOrNewFromCloud = cloudEntries.filter { cloudEntry ->
                    val local = localMap[cloudEntry.id]
                    local == null || !cloudEntry.isContentEqual(local)
                }

                /**
                 * Filter local entries that are missing in the cloud.
                 * These should be uploaded to cloud to keep sync.
                 */
                val missingInCloud = allLocalEntries.filter { localEntry ->
                    cloudMap[localEntry.id] == null
                }

                /** Upload missing local entries to the cloud asynchronously */
                if (missingInCloud.isNotEmpty()) {
                    uploadUpdatedOrNewOTPs(missingInCloud)
                }

                /**
                 * Merge local entries with new/updated cloud entries,
                 * remove duplicates by ID, and regenerate OTP codes for each entry.
                 */
                val merged = (allLocalEntries + updatedOrNewFromCloud)
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

                /** Update the local entries state with merged data */
                _otpEntries.value = merged

                /** Persist merged OTP entries locally */
                addOtp(merged)

                /** Log syncing summary */
                Log.d(
                    "FirebaseSync",
                    "Fetch sync: ${updatedOrNewFromCloud.size} from cloud, ${missingInCloud.size} uploaded"
                )

                /** Mark syncing as completed */
                stopSyncing()
            }
        }.addOnFailureListener { e ->
            /** Log error if fetching fails */
            Log.e("FirebaseSync", "Failed to fetch OTPs from cloud", e)

            /** Mark syncing as completed, even though it failed */
            stopSyncing()
        }
    }

    fun uploadUpdatedOrNewOTPs(updatedOTPs: List<OtpEntry>) {
        if (auth.currentUser == null) {
            Log.w("FirebaseSync", "Cannot upload OTPs, user is not authenticated")
            return
        }

        startSyncing()

        val userDocRef = db.collection("users").document(auth.uid.toString())
        val collection = userDocRef.collection("OTPs")

        val tasks = updatedOTPs.map { otp ->
            val otpDoc = collection.document(otp.id)
            val data = mapOf(
                "id" to otp.id,
                "email" to otp.email,
                "codeName" to otp.codeName,
                "secret" to otp.secret,
                "digits" to otp.digits,
                "algorithm" to otp.algorithm,
                "period" to otp.period,
                "updatedAt" to otp.updatedAt,
            )
            otpDoc.set(data, SetOptions.merge())
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            stopSyncing()
            Log.d("FirebaseSync", "All OTP uploads completed")
        }
    }

    fun deleteOtpFromFirebase(otpId: String) {
        if (auth.currentUser == null) {
            Log.w("FirebaseSync", "Cannot delete OTP, user is not authenticated")
            return
        }

        startSyncing()

        val otpDoc = db.collection("users").document(auth.uid.toString()).collection("OTPs").document(otpId)
        otpDoc.delete().addOnSuccessListener {
            Log.d("FirebaseSync", "Deleted OTP $otpId successfully")
        }.addOnFailureListener { e ->
            Log.e("FirebaseSync", "Failed to delete OTP $otpId", e)
        }

        stopSyncing()
    }

    /**
     * Deletes the given OTP and updates storage.
     */
    fun deleteSecret(otpToDelete: OtpEntry) {
        _otpEntries.value = _otpEntries.value.filterNot { it.id == otpToDelete.id }
        deleteOtp(otpToDelete)

        viewModelScope.launch(Dispatchers.IO) {
            if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                deleteOtpFromFirebase(otpToDelete.id)
            }
        }
    }

    fun toggleCloudSync(enabled: Boolean) {
        if (enabled) {
            fetchAllFromCloud()
        } else {
            stopListeningForCloudChanges()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForCloudChanges()
    }
}



