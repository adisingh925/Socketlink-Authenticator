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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    internal val _otpEntries = MutableStateFlow<List<OtpEntry>>(emptyList())
    val otpEntries: StateFlow<List<OtpEntry>> = _selectedTag.combine(_otpEntries) { tag, entries ->
        if (tag.equals(Utils.ALL, ignoreCase = true)) {
            entries
        } else {
            entries.filter { it.tag == tag }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun onTagSelected(tag: String) {
        _selectedTag.value = tag
    }

    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressMap: StateFlow<Map<String, Float>> = _progressMap

    var selectedOtpEntries by mutableStateOf<List<OtpEntry>>(emptyList())

    val db by lazy { Firebase.firestore }
    private var firestoreListener: ListenerRegistration? = null
    val auth = Firebase.auth

    private val repository: OtpRepository = OtpRepository(application)

    private val authStateListener = FirebaseAuth.AuthStateListener {
        Log.d("Socketlink Authenticator", "Auth state changed: ${it.currentUser?.email ?: "No user"}")
        handleAuthStateChange()
    }

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
        auth.addAuthStateListener(authStateListener)
    }

    private fun handleAuthStateChange() {
        viewModelScope.launch(Dispatchers.IO) {
            loadOtpEntries()
        }
    }

    /** Add OTP */
    suspend fun addOtp(otpList: List<OtpEntry>) {
        repository.insertOtp(otpList)
    }

    /** Update OTP */
    fun updateOtp(otpEntry: OtpEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOtp(otpEntry)
        }
    }

    /** Delete OTP */
    suspend fun deleteOtp(otpEntry: OtpEntry) {
        repository.deleteOtp(otpEntry)
    }

    private val _activeSyncs = MutableStateFlow(0)
    val isSyncing = _activeSyncs.map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun startSyncing() {
        _activeSyncs.update { it + 1 }
    }

    fun stopSyncing() {
        _activeSyncs.update { count -> maxOf(0, count - 1) }
    }

    /**
     * Loads OTP secrets from storage and updates the _otpEntries StateFlow.
     */
    internal suspend fun loadOtpEntries() {
        val userEmail = auth.currentUser?.email.orEmpty()
        val userId = auth.uid.orEmpty()

        Log.d("Socketlink Authenticator", "Loading OTP entries for user: $userId : $userEmail")

        onTagSelected(Utils.ALL)

        val entries = repository.getAllOTPs(userEmail)

        /** If no change do nothing */
        if (_otpEntries.value != entries) {
            _otpEntries.value = entries

            withContext(Dispatchers.Default) {
                updateUniqueTags()
            }
        }

        Log.d("Socketlink Authenticator", "Loaded ${entries.size} entries from local repository")
        Log.d(
            "Socketlink Authenticator",
            "Updated StateFlow with ${_otpEntries.value.size} entries"
        )

        if (Utils.isCloudSyncEnabled(application.applicationContext)) {
            fetchAllFromCloudSafe(entries)
        }
    }

    /**
     * Periodically updates OTP codes and progress.
     */
    private fun startTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            val nextUpdateMap = mutableMapOf<String, Long>()

            while (true) {
                val now = System.currentTimeMillis()

                var hasChanged = false

                val updatedEntries = _otpEntries.value.map { otp ->
                    if(otp.otpType != Utils.TOTP) {
                        return@map otp // Skip HOTP entries, they don't change dynamically
                    }

                    val periodMillis = otp.period * 1000L
                    val nextUpdate = nextUpdateMap[otp.id] ?: 0L

                    if (now >= nextUpdate || otp.code.isEmpty()) {
                        nextUpdateMap[otp.id] = ((now / periodMillis) + 1) * periodMillis
                        hasChanged = true
                        val newCode =
                            OtpUtils.generateOtp(otp.secret, otp.digits, otp.algorithm, otp.period)
                        otp.copy(code = newCode)
                    } else {
                        otp
                    }
                }

                if (hasChanged) {
                    _otpEntries.value = updatedEntries
                }

                val newProgressMap = updatedEntries.associate { otp ->
                    if(otp.otpType != Utils.TOTP) {
                        return@associate otp.id to 0f // Skip HOTP entries, they don't have progress
                    }

                    val periodMillis = otp.period * 1000L
                    val elapsed = now % periodMillis
                    val progress = 1f - elapsed.toFloat() / periodMillis
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
        val uniqueTagsFromEntries = _otpEntries.value
            .mapNotNull { it.tag.takeIf { tag -> tag.isNotBlank() } }
            .distinct()
            .filterNot { it == Utils.ALL }

        _uniqueTags.value = listOf(Utils.ALL) + uniqueTagsFromEntries
    }

    fun addSecrets(secrets: List<OtpEntry>) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val newEntries = secrets.map { secret ->
                    if(secret.otpType != Utils.TOTP) {
                        return@map secret // Skip HOTP entries, they don't change dynamically
                    }

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
                updateUniqueTags()
            }

            withContext(Dispatchers.Default) {
                addOtp(secrets)
                if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                    uploadUpdatedOrNewOTPs(secrets)
                }
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
                this.updatedAt == other.updatedAt &&
                this.email == other.email &&
                this.tag == other.tag
    }

    /**
     * Fetches all OTP entries from the cloud Firestore and merges them with local entries.
     * It updates local entries if the cloud has newer data and uploads local entries missing in the cloud.
     * This function runs safely inside a coroutine with proper error handling and sync state management.
     */
    suspend fun fetchAllFromCloudSafe(localEntries: List<OtpEntry> = _otpEntries.value) {
        /** Abort if user is not authenticated */
        if (auth.currentUser == null) {
            Log.w("FirebaseSync", "Cannot fetch OTPs, user is not authenticated")
            return
        }

        /** Log the user ID being fetched */
        Log.d(
            "FirebaseSync",
            "Fetching OTPs from cloud for user: ${auth.uid} : ${auth.currentUser?.email}"
        )

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

            /** Create lookup maps for local and cloud entries */
            val cloudMap = cloudEntries.associateBy { it.id }
            val localMap = localEntries.associateBy { it.id }

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
            val missingInCloud = localEntries.filter { localEntry ->
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
            val merged = (localEntries + updatedOrNewFromCloud).distinctBy { it.id }

            /** Update the StateFlow and persist merged entries locally */
            if(merged.size != _otpEntries.value.size) {
                Log.d("FirebaseSync", "Merging entries, size changed from ${_otpEntries.value.size} to ${merged.size}")

                _otpEntries.value = merged
                addOtp(updatedOrNewFromCloud)

                withContext(Dispatchers.IO) {
                    updateUniqueTags()
                }
            }

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
                "otpType" to otp.otpType,
                "counter" to otp.counter,
                "code" to otp.code,
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

        Tasks.whenAllComplete(tasks).addOnCompleteListener { allTasks ->
            stopSyncing()

            val failedTasks = allTasks.result?.filter { !it.isSuccessful } ?: emptyList()
            if (failedTasks.isEmpty()) {
                Log.d("FirebaseSync", "All OTP uploads completed successfully")
            } else {
                Log.e("FirebaseSync", "Some OTP uploads failed: ${failedTasks.size} failures")
                failedTasks.forEach { task ->
                    Log.e("FirebaseSync", "Failure: ${task.exception}")
                }
            }
        }
    }

    fun deleteOtpFromFirebase(otpId: String) {
        if (auth.currentUser == null) {
            Log.w("FirebaseSync", "Cannot delete OTP, user is not authenticated")
            return
        }

        startSyncing()

        val otpDoc =
            db.collection("users").document(auth.uid.toString()).collection("OTPs").document(otpId)
        otpDoc.delete().addOnSuccessListener {
            Log.d("FirebaseSync", "Deleted OTP $otpId successfully")
        }.addOnFailureListener { e ->
            Log.e("FirebaseSync", "Failed to delete OTP $otpId", e)
        }.addOnCompleteListener {
            stopSyncing()
        }
    }

    /**
     * Deletes the given OTP and updates storage.
     */
    fun deleteSecret(otpToDelete: OtpEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                _otpEntries.value = _otpEntries.value.filterNot { it.id == otpToDelete.id }
                updateUniqueTags()
            }

            withContext(Dispatchers.IO) {
                deleteOtp(otpToDelete)
                if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                    deleteOtpFromFirebase(otpToDelete.id)
                }
            }
        }
    }

    fun generateHOTPCode(otp: OtpEntry) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val newCounter = otp.counter + 1

                val newCode = OtpUtils.generateHotp(
                    secret = otp.secret,
                    counter = newCounter,
                    digits = otp.digits,
                    algorithm = otp.algorithm
                )

                val updatedOtp = otp.copy(
                    code = newCode,
                    counter = newCounter
                )

                // Update in the list
                _otpEntries.update { list ->
                    list.map { if (it.id == otp.id) updatedOtp else it }
                }

                // Save in local storage
                withContext(Dispatchers.IO) {
                    updateOtp(updatedOtp) // Your DAO or repository method

                    if (Utils.isCloudSyncEnabled(application.applicationContext)) {
                        uploadUpdatedOrNewOTPs(listOf(updatedOtp)) // Firebase update
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForCloudChanges()
        auth.removeAuthStateListener(authStateListener)
    }
}



