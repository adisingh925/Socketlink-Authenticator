package com.socketlink.android.authenticator

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.socketlink.android.authenticator.OtpUtils.parseOtpAuthUri
import com.socketlink.android.authenticator.ui.theme.SocketlinkAuthenticatorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity

class MainActivity : AppCompatActivity() {
    private val otpViewModel: OtpViewModel by viewModels()

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class,
        ExperimentalMaterial3Api::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("com.warrenstrange.googleauth.rng.algorithmProvider", "AndroidOpenSSL")
        enableEdgeToEdge()

        /** Block screenshots */
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            /**
             * Root theme wrapper for the application.
             */
            SocketlinkAuthenticatorTheme {
                /** NavController for handling screen navigation */
                val navController = rememberNavController()

                /** OTP entries collected from the ViewModel */
                val otpEntries by otpViewModel.otpEntries.collectAsState()

                /** Progress map for each OTP entry (e.g., countdown animation progress) */
                val progressMap by otpViewModel.progressMap.collectAsState()

                /** Camera permission handler */
                val cameraPermissionState =
                    rememberPermissionState(android.Manifest.permission.CAMERA)

                /** Coroutine scope for launching suspend functions */
                val coroutineScope = rememberCoroutineScope()

                /** Flag to indicate when the camera FAB is clicked */
                var cameraButtonClicked by remember { mutableStateOf(false) }

                /** Context for showing Toasts or launching settings */
                val context = LocalContext.current

                /**
                 * Handle camera permission result and navigate to the scanner screen
                 * only when permission is granted and the button was clicked.
                 */
                LaunchedEffect(cameraPermissionState.status.isGranted, cameraButtonClicked) {
                    if (cameraButtonClicked && cameraPermissionState.status.isGranted) {
                        cameraButtonClicked = false
                        navController.navigate("scanner?mode=scan")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    /**
                     * Navigation host with animated transitions for screen navigation.
                     */
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = "auth",

                        /** Enter transition when navigating forward */
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(500, easing = FastOutSlowInEasing))
                        },

                        /** Exit transition when navigating forward */
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(500, easing = FastOutSlowInEasing))
                        },

                        /** Pop enter transition (e.g., back stack) */
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(500, easing = FastOutSlowInEasing))
                        },

                        /** Pop exit transition (e.g., back stack) */
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(500, easing = FastOutSlowInEasing))
                        }
                    ) {
                        composable("auth") {
                            AuthenticationScreenWrapper(
                                onAuthenticated = {
                                    navController.navigate("main") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onFailed = {
                                    /** failed to authenticate */
                                }
                            )
                        }

                        /**
                         * Main screen composable:
                         * Displays list of OTPs and shows floating action buttons.
                         */
                        composable("main") {
                            Scaffold(
                                /** Floating buttons for camera and manual OTP */
                                floatingActionButton = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        /** FAB for launching camera scanner */
                                        FloatingActionButton(
                                            onClick = {
                                                cameraButtonClicked = true
                                                when {
                                                    cameraPermissionState.status.isGranted -> Unit
                                                    cameraPermissionState.status.shouldShowRationale -> {
                                                        coroutineScope.launch {
                                                            cameraPermissionState.launchPermissionRequest()
                                                        }
                                                    }

                                                    else -> {
                                                        Toast.makeText(
                                                            context,
                                                            "Please enable camera permission in app settings",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        openAppSettings(context as Activity)
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Scan Barcode"
                                            )
                                        }

                                        /** FAB to add OTP manually */
                                        FloatingActionButton(
                                            onClick = { navController.navigate("add") },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Keyboard,
                                                contentDescription = "Add OTP Manually"
                                            )
                                        }
                                    }
                                },

                                /** Main screen container */
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                /** Actual OTP UI list with progress indicators */
                                OtpScreen(
                                    otpEntries = otpEntries,
                                    progressMap = progressMap,
                                    modifier = Modifier.padding(innerPadding),
                                    otpViewModel = otpViewModel,
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToTransfer = { navController.navigate("transfer") },
                                    onNavigateToFeedback = {
                                        val reviewManager = ReviewManagerFactory.create(context)

                                        coroutineScope.launch {
                                            val request = reviewManager.requestReviewFlow()
                                            if (request.isSuccessful) {
                                                val reviewInfo = request.result
                                                reviewManager.launchReviewFlow(
                                                    this@MainActivity,
                                                    reviewInfo
                                                )
                                            } else {
                                                /** Fallback to play store */
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("market://details?id=${context.packageName}")
                                                ).apply {
                                                    setPackage("com.android.vending")  // Force open in Play Store app
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        /**
                         * Add OTP screen:
                         * Allows entering OTP details manually and saving.
                         */
                        composable("add") {
                            AddOtpScreen(
                                onAdd = { codeName, secret, digits, algorithm, period ->
                                    otpViewModel.addSecret(
                                        OtpEntry(
                                            codeName = codeName,
                                            secret = secret,
                                            code = "",
                                            digits = digits,
                                            algorithm = algorithm,
                                            period = period
                                        )
                                    )
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }

                        /**
                         * Scanner screen:
                         * Launches camera, detects QR code, and adds OTP.
                         */
                        composable(
                            route = "scanner?mode={mode}",
                            arguments = listOf(
                                navArgument("mode") {
                                    type = NavType.StringType
                                    defaultValue = "scan"
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "scan"
                            var scannedCode by remember { mutableStateOf<String?>(null) }

                            ScannerScreen(
                                onBarcodeDetected = { code ->
                                    if (scannedCode == null) {
                                        scannedCode = code
                                    }
                                },
                                onCancel = { navController.popBackStack() },
                            )

                            LaunchedEffect(scannedCode) {
                                val code = scannedCode ?: return@LaunchedEffect

                                when (mode) {
                                    "scan" -> {
                                        parseOtpAuthUri(code)?.let { otpSecret ->
                                            otpViewModel.addSecret(otpSecret)
                                        }
                                    }

                                    "import" -> {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val otpJsonArray = JSONArray(code)
                                                for (i in 0 until otpJsonArray.length()) {
                                                    val otpJson = otpJsonArray.getJSONObject(i)
                                                    val otpEntry = OtpEntry(
                                                        codeName = otpJson.getString("codeName"),
                                                        secret = otpJson.getString("secret"),
                                                        code = "",
                                                        digits = otpJson.getInt("digits"),
                                                        period = otpJson.getInt("period"),
                                                        algorithm = otpJson.getString("algorithm")
                                                    )
                                                    Log.d("Parsed OTP Entry", otpEntry.toString())
                                                    otpViewModel.addSecret(otpEntry)
                                                }
                                            } catch (e: Exception) {
                                                Log.e("OTP Import", "Failed to parse JSON", e)
                                            }
                                        }
                                    }
                                }

                                navController.popBackStack()
                            }
                        }

                        composable("settings") {
                            SettingsScreen(navController)
                        }

                        composable("transfer") {
                            TransferCodesScreenWithAuth(navController)
                        }

                        composable("export") {
                            ExportQRCodeScreen(
                                navController = navController,
                                otpEntries = otpEntries
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransferCodesScreenWithAuth(navController: NavController) {
    /**
     * State to trigger biometric authentication flow
     * when user taps on export button
     */
    var triggerAuth by remember { mutableStateOf(false) }

    /**
     * Callback invoked when user clicks "Export"
     * This sets the trigger to true to start biometric auth
     */
    val onExportClick = {
        triggerAuth = true  // Start biometric auth flow
    }

    /**
     * Callback invoked on successful biometric authentication
     * Resets trigger and navigates to export screen
     */
    val onAuthenticated = {
        navController.navigate("export") // Navigate only after successful auth
    }

    /**
     * Callback invoked if biometric authentication fails or cancelled
     * Resets trigger, you can add error handling here if needed
     */
    val onFailed = {
        // Optionally show error or feedback here
    }

    /**
     * Conditionally show the biometric authentication prompt
     * Only when triggerAuth is true
     */
    if (triggerAuth) {
        BiometricAuthenticator(
            trigger = triggerAuth,
            onAuthenticated = onAuthenticated,
            onFailed = onFailed,
            resetTrigger = { triggerAuth = false },
            heading = "Authentication Required",
            subheading = "Verify your identity to export your codes"
        )
    }

    /**
     * Render the main transfer codes screen UI
     * Pass the export and import click handlers
     */
    TransferCodesScreen(
        navController = navController,
        onExportClick = onExportClick,
        onImportClick = {
            navController.navigate("scanner?mode=import")
        }
    )
}

@Composable
fun BiometricAuthenticator(
    trigger: Boolean,
    heading: String = "Authenticate",
    subheading: String = "Please authenticate to continue",
    onAuthenticated: () -> Unit,
    onFailed: () -> Unit,
    resetTrigger: () -> Unit
) {
    /** Get the current context */
    val context = LocalContext.current

    /**
     * Extension function to recursively find FragmentActivity from any Context.
     * Returns null if no FragmentActivity is found.
     */
    fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }

    /** Attempt to find the hosting FragmentActivity or exit early */
    val activity = context.findFragmentActivity() ?: run {
        Log.d("BiometricAuthenticator", "No FragmentActivity found")
        resetTrigger()
        onFailed()
        return
    }

    /** Get BiometricManager instance to check biometric availability */
    val biometricManager = BiometricManager.from(context)

    /** Check biometric availability and handle fallback if unavailable */
    if (trigger) {
        when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit // proceed normally
            else -> {
                /** No biometric available, fallback to success */
                resetTrigger()
                onAuthenticated()
                return
            }
        }
    }

    /**
     * Authentication callback to handle biometric prompt results.
     * Uses 'remember' to avoid creating new instances on recomposition.
     */
    val callback = remember {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                resetTrigger()
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                resetTrigger()
                onFailed()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                /** Optional: handle authentication failure (e.g., show a toast) */
                resetTrigger()
                onFailed()
            }
        }
    }

    /**
     * Create BiometricPrompt instance tied to the FragmentActivity and main thread executor.
     * 'remember' ensures it persists across recompositions.
     */
    val biometricPrompt = remember {
        BiometricPrompt(activity, ContextCompat.getMainExecutor(context), callback)
    }

    /**
     * Build the prompt information for the biometric dialog.
     * 'remember' caches it unless input parameters change.
     */
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(heading)
            .setSubtitle(subheading)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ).build()
    }

    /** Launch biometric prompt whenever 'trigger' becomes true */
    LaunchedEffect(trigger) {
        if (trigger) {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}

/** Helper extension to get Activity from Context */
fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun AuthenticationScreenWrapper(
    /** Callback when authentication succeeds or is not required */
    onAuthenticated: () -> Unit,

    /** Callback when authentication fails */
    onFailed: () -> Unit
) {
    /** Access app lock preference */
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    val appLockEnabled = remember { prefs.getBoolean("app_lock_enabled", false) }

    /** If app lock is enabled, show authentication screen */
    if (appLockEnabled) {
        AuthenticationScreen(
            onAuthenticated = onAuthenticated,
            onFailed = onFailed
        )
    } else {
        // Skip authentication if app lock is disabled
        LaunchedEffect(Unit) {
            onAuthenticated()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    onAuthenticated: () -> Unit,
    onFailed: () -> Unit
) {
    /** Trigger state for authentication */
    var triggerAuth by remember { mutableStateOf(true) }

    /** Main screen */
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        /** Centered Box layout to align unlock button */
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            /** Unlock button */
            Button(
                onClick = { triggerAuth = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Unlock",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock")
            }
        }

        /** Trigger biometric/device credential auth */
        BiometricAuthenticator(
            trigger = triggerAuth,
            onAuthenticated = onAuthenticated,
            onFailed = onFailed,
            resetTrigger = { triggerAuth = false },
            heading = "Unlock Authenticator",
            subheading = "Use your screen lock to unlock Authenticator"
        )
    }
}

@Composable
fun rememberAppLockPreference(): Pair<Boolean, (Boolean) -> Unit> {
    /** Get current context to access SharedPreferences */
    val context = LocalContext.current

    /** Get SharedPreferences named "settings_prefs" in private mode */
    val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    /**
     * Remember the current app lock state.
     * Initialized from SharedPreferences or defaults to false
     */
    var appLockEnabled by remember {
        mutableStateOf(prefs.getBoolean("app_lock_enabled", false))
    }

    /**
     * Lambda to save the app lock setting to SharedPreferences and update state
     * @param enabled new boolean value indicating whether app lock is enabled
     */
    val saveAppLock: (Boolean) -> Unit = { enabled ->
        prefs.edit {
            putBoolean("app_lock_enabled", enabled) // Save the new value
        }
        appLockEnabled = enabled // Update Compose state to reflect change
    }

    /** Return current state and the function to update it */
    return appLockEnabled to saveAppLock
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    /** Get current App Lock setting and setter */
    val (appLockEnabled, setAppLockEnabled) = rememberAppLockPreference()

    /** UI state: pending toggle after auth */
    var togglePending by remember { mutableStateOf<Boolean?>(null) }

    /** Trigger biometric flow only when toggle is attempted */
    var triggerAuth by remember { mutableStateOf(false) }

    /** Scaffold with top app bar */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            /** App Lock toggle setting item */
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        togglePending = !appLockEnabled
                        triggerAuth = true
                    },
                headlineContent = { Text("App Lock") },
                supportingContent = {
                    Text("Access to this app will be restricted by your screen lock")
                },
                trailingContent = {
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = {
                            togglePending = it
                            triggerAuth = true
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.3f
                            )
                        )
                    )
                }
            )
        }

        /** Biometric authentication triggered here */
        if (triggerAuth && togglePending != null) {
            BiometricAuthenticator(
                trigger = triggerAuth,
                onAuthenticated = {
                    togglePending?.let { setAppLockEnabled(it) }
                    togglePending = null
                },
                onFailed = {
                    togglePending = null
                },
                resetTrigger = {
                    triggerAuth = false
                },
                heading = "Authentication Required",
                subheading = "Verify your identity to change this setting"
            )
        }
    }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun OtpScreen(
    otpEntries: List<OtpEntry>,
    progressMap: Map<String, Float>,
    modifier: Modifier = Modifier,
    otpViewModel: OtpViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {}
) {
    /** Focus manager to handle keyboard focus */
    val focusManager = LocalFocusManager.current

    /** State for drawer (open/closed) */
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    /** Coroutine scope for launching drawer open/close */
    val drawerScope = rememberCoroutineScope()

    /** State for search query input */
    var searchQuery by rememberSaveable { mutableStateOf("") }

    /** Filter OTP entries based on search query using derivedStateOf to optimize recompositions */
    val filteredEntries by remember(otpEntries, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) otpEntries
            else otpEntries.filter { otp ->
                otp.codeName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    /** State to hold OTP pending deletion (to show confirmation dialog) */
    var otpPendingDeletion by remember { mutableStateOf<OtpEntry?>(null) }

    /** If the drawer is open close it on back press */
    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch {
            drawerState.close()
        }
    }

    /** Modal navigation drawer wrapping the main content and drawer content */
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            /** Drawer sheet with status and navigation bars padding */
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(LocalConfiguration.current.screenWidthDp.dp * 0.8f)
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars))
            ) {
                /** Modifier for drawer items to apply consistent padding */
                val drawerItemModifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)

                /** Navigation drawer item: Transfer Codes */
                NavigationDrawerItem(
                    label = { Text("Transfer Codes") },
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Transfer Codes") },
                    selected = false,
                    onClick = {
                        drawerScope.launch {
                            drawerState.close()
                            /** Close drawer */
                            onNavigateToTransfer()
                            /** Trigger navigation callback */
                        }
                    },
                    modifier = drawerItemModifier
                )

                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                /** Navigation drawer item: Settings */
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    selected = false,
                    onClick = {
                        drawerScope.launch {
                            drawerState.close()
                            onNavigateToSettings()
                        }
                    },
                    modifier = drawerItemModifier
                )

                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                /** Navigation drawer item: Feedback */
                NavigationDrawerItem(
                    label = { Text("Feedback") },
                    icon = { Icon(Icons.Default.Feedback, contentDescription = "Feedback") },
                    selected = false,
                    onClick = {
                        drawerScope.launch {
                            drawerState.close()
                            onNavigateToFeedback()
                        }
                    },
                    modifier = drawerItemModifier
                )
            }
        }
    )
    {
        /** Main content column */
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                /** Detect taps outside text field to clear focus (hide keyboard) */
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            /** Search text field with leading menu icon and trailing clear icon */
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...") },
                singleLine = true,
                shape = RoundedCornerShape(34.dp),
                leadingIcon = {
                    /** Menu icon opens/closes the drawer */
                    IconButton(
                        onClick = {
                            drawerScope.launch {
                                if (drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                trailingIcon = {
                    /** Show clear icon only if searchQuery is not blank */
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )

            /** LazyColumn showing filtered OTP entries with swipe-to-dismiss */
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = filteredEntries,
                    key = { it.id }
                ) { otp ->
                    /** Progress value for the OTP */
                    val progress = progressMap[otp.id] ?: 1f

                    /** Remember dismiss state for swipe-to-dismiss functionality */
                    val dismissState = rememberDismissState(
                        confirmStateChange = { dismissValue ->
                            if (dismissValue == DismissValue.DismissedToStart) {
                                /** Instead of deleting immediately, show confirm dialog */
                                otpPendingDeletion = otp
                                /** Prevent automatic dismiss */
                                false
                            } else false
                        }
                    )

                    /** Swipe to dismiss composable wrapping each OTP card */
                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        dismissThresholds = { FractionalThreshold(0.8f) },
                        background = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .animateItemPlacement()
                    ) {
                        OtpCard(
                            otp = otp,
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    /** Confirmation dialog to delete OTP */
    if (otpPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { otpPendingDeletion = null },

            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.Center)
                    )
                }
            },

            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Are you sure you want to delete this OTP code?",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = otpPendingDeletion?.codeName ?: "NA",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFEF5350),
                    )

                    Text(
                        text = "Deleting this code means you will no longer be able to generate login codes for this account unless you re-add it manually.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        otpPendingDeletion?.let { otpViewModel.deleteSecret(it) }
                        otpPendingDeletion = null
                    }
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {
                TextButton(onClick = { otpPendingDeletion = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun OtpCard(
    otp: OtpEntry,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val codeName = remember(otp.codeName) {
        otp.codeName.ifBlank { "Unknown Issuer" }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = {
                        val codeWithoutSpaces = otp.code.replace(" ", "")
                        clipboardManager.setText(AnnotatedString(codeWithoutSpaces))
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT)
                            .show()
                    }
                )
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = codeName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = otp.algorithm.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val fontSize = MaterialTheme.typography.headlineMedium.fontSize * 1.2f
            val code = otp.code
            val splitIndex = code.length / 2

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = code.substring(0, splitIndex),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = code.substring(splitIndex),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportQRCodeScreen(
    navController: NavController,
    otpEntries: List<OtpEntry>
) {
    val gson = remember { Gson() }
    val otpJson = remember(otpEntries) { gson.toJson(otpEntries) }
    val chunkedJsonList = remember(otpJson) { splitStringIntoChunks(otpJson, maxChunkSize = 800) }

    var currentIndex by remember { mutableIntStateOf(0) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentIndex) {
        qrBitmap = withContext(Dispatchers.Default) {
            generateQRCodeBitmap(chunkedJsonList[currentIndex], 600, 600)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "OTP Export QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Text(
                        text = if (qrBitmap != null) "QR Code ${currentIndex + 1} of ${chunkedJsonList.size}" else "Generating QR code...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    modifier = Modifier.weight(1f),
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Previous")
                    Spacer(Modifier.width(8.dp))
                    Text("Previous")
                }

                Button(
                    onClick = { if (currentIndex < chunkedJsonList.size - 1) currentIndex++ },
                    modifier = Modifier.weight(1f),
                    enabled = currentIndex < chunkedJsonList.size - 1,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Next")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }
    }
}

/**
 * Screen to show QR code representing JSON of all OTP entries for export.
 */
fun splitStringIntoChunks(input: String, maxChunkSize: Int): List<String> {
    val chunks = mutableListOf<String>()
    var index = 0
    while (index < input.length) {
        val end = minOf(index + maxChunkSize, input.length)
        chunks.add(input.substring(index, end))
        index = end
    }
    return chunks
}

/** Helper function to generate a QR code bitmap from string content */
fun generateQRCodeBitmap(
    text: String,
    width: Int,
    height: Int
): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val bitmap = createBitmap(width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferCodesScreen(
    navController: NavController,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Codes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            /** Export Section */
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Export Codes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Generate a QR code with your OTPs that can be scanned from another device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onExportClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Export")
                        Spacer(Modifier.width(8.dp))
                        Text("Export")
                    }
                }
            }

            /** Import Section */
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Import Codes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Scan a QR code to import OTPs from another device. New entries will be added to your list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onImportClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Import")
                        Spacer(Modifier.width(8.dp))
                        Text("Import")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOtpScreen(
    onAdd: (codeName: String, secret: String, digits: Int, algorithm: String, period: Int) -> Unit,
    onCancel: () -> Unit
) {
    var codeName by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var digits by rememberSaveable { mutableStateOf("6") }
    var algorithm by rememberSaveable { mutableStateOf("SHA1") }
    var period by rememberSaveable { mutableStateOf("30") }

    val canAdd = codeName.isNotBlank() && secret.isNotBlank() && digits.toIntOrNull() != null &&
            algorithm.isNotBlank() && period.toIntOrNull() != null
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter details") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = codeName,
                onValueChange = { codeName = it },
                label = { Text("Code Name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3, // Allow up to 3 lines
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                )
            )

            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text("Secret Key") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3, // Allow up to 3 lines
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                )
            )

            val roundedShape = RoundedCornerShape(12.dp)
            val rotationDegree by animateFloatAsState(targetValue = if (showAdvanced) 180f else 0f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(roundedShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = { showAdvanced = !showAdvanced }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Settings",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotationDegree)
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    var digitsExpanded by remember { mutableStateOf(false) }
                    val digitOptions = listOf("6", "8")

                    ExposedDropdownMenuBox(
                        expanded = digitsExpanded,
                        onExpandedChange = { digitsExpanded = !digitsExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = digits,
                            shape = RoundedCornerShape(12.dp),  // <-- added rounded corners here
                            onValueChange = {},
                            label = { Text("Digits") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = digitsExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(), // required for proper menu alignment
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.1f
                                ),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.05f
                                )
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = digitsExpanded,
                            onDismissRequest = { digitsExpanded = false }
                        ) {
                            digitOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        digits = selectionOption
                                        digitsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val algorithmOptions = listOf("SHA1", "SHA256", "SHA512")
                    var algorithmExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = algorithmExpanded,
                        onExpandedChange = { algorithmExpanded = !algorithmExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = algorithm,
                            shape = RoundedCornerShape(12.dp),  // <-- added rounded corners here
                            onValueChange = {},
                            label = { Text("Algorithm") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = algorithmExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(), // Required for alignment
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.1f
                                ),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.05f
                                )
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = algorithmExpanded,
                            onDismissRequest = { algorithmExpanded = false }
                        ) {
                            algorithmOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        algorithm = selectionOption
                                        algorithmExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val options = listOf(10, 20, 30, 45, 60, 90, 120)
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = period,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Valid Period") },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("$option seconds") },
                                    onClick = {
                                        period = option.toString()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (canAdd) {
                            onAdd(
                                codeName.trim(),
                                secret.trim(),
                                digits.toInt(),
                                algorithm.trim(),
                                period.toInt()
                            )
                            focusManager.clearFocus()
                        }
                    },
                    enabled = canAdd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    val scanner = remember { BarcodeScanning.getClient() }
    val primaryColor = MaterialTheme.colorScheme.primary

    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val onBarcodeDetectedState by rememberUpdatedState(onBarcodeDetected)

    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val executor = ContextCompat.getMainExecutor(context)

        preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                barcodes.forEach { barcode ->
                                    barcode.rawValue?.let { code ->
                                        onBarcodeDetectedState(code)
                                        imageProxy.close()
                                        return@addOnSuccessListener
                                    }
                                }
                                imageProxy.close()
                            }
                            .addOnFailureListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            cameraControl = camera.cameraControl
        } catch (exc: Exception) {
            Log.e("ScannerScreen", "Use case binding failed", exc)
        }

        onDispose {
            imageAnalysis?.clearAnalyzer()
            cameraProvider.unbindAll()
            scanner.close()
        }
    }

    val scanBoxSize = 280.dp
    val strokeWidth = 4.dp
    val gapSize = 100.dp
    val cornerRadius = 24.dp

    val buttonBackground = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        ScannerOverlayWithRoundedCorners(
            modifier = Modifier
                .size(scanBoxSize)
                .align(Alignment.Center),
            color = primaryColor,
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            cornerRadius = cornerRadius
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onCancel,
                modifier = buttonBackground
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Scan", tint = Color.White)
            }

            Text(
                text = "Scan QR Code",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = {
                    isFlashOn = !isFlashOn
                    cameraControl?.enableTorch(isFlashOn)
                },
                modifier = buttonBackground
            ) {
                val flashIcon =
                    if (isFlashOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff
                Icon(
                    imageVector = flashIcon,
                    contentDescription = if (isFlashOn) "Turn off flash" else "Turn on flash",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ScannerOverlayWithRoundedCorners(
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidth: Dp,
    gapSize: Dp,
    cornerRadius: Dp
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val gapPx = with(LocalDensity.current) { gapSize.toPx() }
    val cornerPx = with(LocalDensity.current) { cornerRadius.toPx() }

    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val halfStroke = strokePx / 2f

        // Draw rounded corners (quarter arcs)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(cornerPx * 2, cornerPx * 2),
            style = Stroke(strokePx)
        )
        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(sizePx - cornerPx * 2 - halfStroke, halfStroke),
            size = Size(cornerPx * 2, cornerPx * 2),
            style = Stroke(strokePx)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(
                sizePx - cornerPx * 2 - halfStroke,
                sizePx - cornerPx * 2 - halfStroke
            ),
            size = Size(cornerPx * 2, cornerPx * 2),
            style = Stroke(strokePx)
        )
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(halfStroke, sizePx - cornerPx * 2 - halfStroke),
            size = Size(cornerPx * 2, cornerPx * 2),
            style = Stroke(strokePx)
        )

        // Draw edges with gaps (split each side into two lines with a gap in the middle)
        // Horizontal top
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset(cornerPx + halfStroke, halfStroke),
            end = Offset((sizePx / 2f) - (gapPx / 2f), halfStroke)
        )
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset((sizePx / 2f) + (gapPx / 2f), halfStroke),
            end = Offset(sizePx - cornerPx - halfStroke, halfStroke)
        )

        // Horizontal bottom
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset(cornerPx + halfStroke, sizePx - halfStroke),
            end = Offset((sizePx / 2f) - (gapPx / 2f), sizePx - halfStroke)
        )
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset((sizePx / 2f) + (gapPx / 2f), sizePx - halfStroke),
            end = Offset(sizePx - cornerPx - halfStroke, sizePx - halfStroke)
        )

        // Vertical left
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset(halfStroke, cornerPx + halfStroke),
            end = Offset(halfStroke, (sizePx / 2f) - (gapPx / 2f))
        )
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset(halfStroke, (sizePx / 2f) + (gapPx / 2f)),
            end = Offset(halfStroke, sizePx - cornerPx - halfStroke)
        )

        // Vertical right
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset(sizePx - halfStroke, cornerPx + halfStroke),
            end = Offset(sizePx - halfStroke, (sizePx / 2f) - (gapPx / 2f))
        )
        drawLine(
            color = color,
            strokeWidth = strokePx,
            start = Offset(sizePx - halfStroke, (sizePx / 2f) + (gapPx / 2f)),
            end = Offset(sizePx - halfStroke, sizePx - cornerPx - halfStroke)
        )
    }
}

















