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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * ViewModel to manage OTP entries and their progress for TOTP codes.
 *
 * @param application Application context to access storage.
 */
class OtpViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedTag = MutableStateFlow(Utils.ALL)
    val selectedTag: StateFlow<String> = _selectedTag

    private val _uniqueTags = MutableStateFlow<List<String>>(emptyList())
    val uniqueTags: StateFlow<List<String>> = _uniqueTags

    private val _otpEntries = MutableStateFlow<List<OtpEntry>>(emptyList())
    val otpEntries: StateFlow<List<OtpEntry>> = _selectedTag
        .combine(_otpEntries) { tag, entries ->
            if (tag.equals(Utils.ALL, ignoreCase = true)) {
                entries
            } else {
                entries.filter { it.tag == tag }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun onTagSelected(tag: String) {
        _selectedTag.value = tag
    }

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
        if (FirebaseApp.getApps(application).isEmpty()) {
            FirebaseApp.initializeApp(application.applicationContext)
        }

        /** Update OTPs */
        startTicker()

        /**
         * Set up an authentication state listener to respond to user sign-in/sign-out events.
         * This will trigger loading of OTP entries when a user is authenticated.
         */
        auth.addAuthStateListener { handleAuthStateChange() }
    }

    private fun handleAuthStateChange() {
        viewModelScope.launch(Dispatchers.IO) {
            loadOtpEntries()
            updateOtpCodes()
            if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                fetchAllFromCloudSafe()
            }
        }
    }

    /** Add OTP */
    fun addOtp(otpList: List<OtpEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOtp(otpList)
        }
    }

    /** Update OTP */
    fun updateOtp(otpEntry: OtpEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOtp(otpEntry)
        }
    }

    /** Delete OTP */
    fun deleteOtp(otpEntry: OtpEntry) {
        viewModelScope.launch(Dispatchers.IO) {
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
    internal suspend fun loadOtpEntries() = withContext(Dispatchers.IO) {
        val entries = repository.getAllOTPs(auth.currentUser?.email ?: "")
        _otpEntries.value = entries
        Log.d("OtpViewModel", "Loaded ${entries.size} OTP entries from storage")

        updateUniqueTags()
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

    private fun updateUniqueTags() {
        viewModelScope.launch(Dispatchers.IO) {
            val uniqueTagsFromEntries = _otpEntries.value
                .mapNotNull { it.tag.takeIf { tag -> tag.isNotBlank() } }
                .distinct()
                .filterNot { it == Utils.ALL }

            _uniqueTags.value = listOf(Utils.ALL) + uniqueTagsFromEntries
        }
    }

    fun addSecrets(secrets: List<OtpEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
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
        }.invokeOnCompletion {
            updateUniqueTags()
        }

        addOtp(secrets)

        viewModelScope.launch(Dispatchers.IO) {
            if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                uploadUpdatedOrNewOTPs(secrets)
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

    /**
     * Fetches all OTP entries from the cloud Firestore and merges them with local entries.
     * It updates local entries if the cloud has newer data and uploads local entries missing in the cloud.
     * This function runs safely inside a coroutine with proper error handling and sync state management.
     */
    suspend fun fetchAllFromCloudSafe() = withContext(Dispatchers.IO) {
        /** Abort if user is not authenticated */
        if (auth.currentUser == null) {
            Log.w("FirebaseSync", "Cannot fetch OTPs, user is not authenticated")
            return@withContext
        }

        /** Log the user ID being fetched */
        Log.d("FirebaseSync", "Fetching OTPs for user: ${auth.uid}")

        /** Begin sync process tracking */
        startSyncing()

        try {
            /** Fetch all OTP entries from the cloud Firestore */
            val snapshot = db.collection("users")
                .document(auth.uid!!)
                .collection("OTPs")
                .get()
                .await()

            /** Convert Firestore documents to OtpEntry objects */
            val cloudEntries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(OtpEntry::class.java)
            }

            /** Get current local OTP entries */
            val allLocalEntries = _otpEntries.value

            /** Create lookup maps for local and cloud entries */
            val cloudMap = cloudEntries.associateBy { it.id }
            val localMap = allLocalEntries.associateBy { it.id }

            /**
             * Identify entries that exist in the cloud but not locally,
             * or are updated in the cloud compared to local.
             */
            val updatedOrNewFromCloud = cloudEntries.filter { cloudEntry ->
                val local = localMap[cloudEntry.id]
                local == null || !cloudEntry.isContentEqual(local)
            }

            /**
             * Identify local entries that are missing in the cloud.
             * These need to be uploaded to keep the cloud in sync.
             */
            val missingInCloud = allLocalEntries.filter { localEntry ->
                cloudMap[localEntry.id] == null
            }

            /** Upload missing local entries to Firestore */
            if (missingInCloud.isNotEmpty()) {
                uploadUpdatedOrNewOTPs(missingInCloud)
            }

            /**
             * Merge local entries with cloud updates, remove duplicates,
             * and regenerate OTP codes for all entries.
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

            /** Update the StateFlow and persist merged entries locally */
            _otpEntries.value = merged
            addOtp(merged)
            updateUniqueTags()

            /** Log sync completion */
            Log.d(
                "FirebaseSync",
                "Fetch sync complete: ${updatedOrNewFromCloud.size} updated from cloud, ${missingInCloud.size} uploaded to cloud"
            )
        } catch (e: Exception) {
            /** Log any errors that occur during the fetch process */
            Log.e("FirebaseSync", "Failed to fetch OTPs from cloud", e)
        } finally {
            /** End sync process tracking */
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
                "tag" to otp.tag,
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

    override fun onCleared() {
        super.onCleared()
        stopListeningForCloudChanges()
    }
}



