package com.socketlink.android.authenticator

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.socketlink.android.authenticator.OtpUtils.parseOtpAuthUri
import com.socketlink.android.authenticator.ui.theme.SocketlinkAuthenticatorTheme
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

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

                /** Progress map for each OTP entry (e.g., countdown animation progress) */
                val progressMap by otpViewModel.progressMap.collectAsState()

                /** Context for showing Toasts or launching settings */
                val context = LocalContext.current

                /** main lifecycle */
                val lifecycleOwner = LocalLifecycleOwner.current

                /** state to record the timestamp of when the app goes to background */
                var backgroundTimestamp by rememberSaveable { mutableStateOf<Long?>(null) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                /** Record time when going to background */
                                backgroundTimestamp = System.currentTimeMillis()
                            }

                            Lifecycle.Event.ON_START -> {
                                /** Check unlock option and elapsed time */
                                val unlockSeconds = Utils.getUnlockOption(context) // in seconds
                                val unlockMillis = unlockSeconds * 1000L
                                val now = System.currentTimeMillis()

                                if (unlockSeconds != -1 && backgroundTimestamp != null) {
                                    val timeInBackground = now - backgroundTimestamp!!

                                    if (timeInBackground >= unlockMillis) {
                                        if (Utils.isAppLockEnabled(context)) {
                                            if (navController.currentDestination?.route != "auth") {
                                                navController.navigate("auth")
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // In your Activity or Composable (remember to use rememberLauncherForActivityResult in Compose):
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val credential = Identity.getSignInClient(this)
                            .getSignInCredentialFromIntent(result.data)
                        val idToken = credential.googleIdToken
                        val email = credential.id
                        val photo = credential.profilePictureUri
                        // TODO: Use idToken and email to authenticate with your backend or Firebase
                        Log.d("GoogleSignIn", "ID Token: $idToken, Email: $email, Photo: $photo")
                        firebaseAuthWithGoogle(idToken.toString(), otpViewModel, this)
                    } else {
                        Log.d("GoogleSignIn", "Sign-in failed or cancelled")
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
                        startDestination = if (Utils.isAppLockEnabled(this@MainActivity)) {
                            "auth"
                        } else {
                            "main"
                        },
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
                                    if (!navController.popBackStack()) {
                                        // Nothing to pop (auth was the start), so navigate to main
                                        navController.navigate("main") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    }
                                },
                                onFailed = {
                                    /** Optional: handle failure */
                                }
                            )
                        }

                        /**
                         * Main screen composable:
                         * Displays list of OTPs and shows floating action buttons.
                         */
                        composable("main") {
                            /** Actual OTP UI list with progress indicators */
                            OtpScreen(
                                progressMap = progressMap,
                                otpViewModel = otpViewModel,
                                navController = navController,
                                launcher = launcher,
                                auth = otpViewModel.auth,
                            )
                        }

                        /**
                         * Add OTP screen:
                         * Allows entering OTP details manually and saving.
                         */
                        composable("add") {
                            AddOtpScreen(
                                onAdd = { codeName, secret, digits, algorithm, period, tag, otpType ->
                                    otpViewModel.addSecrets(
                                        listOf(
                                            OtpEntry(
                                                codeName = codeName,
                                                secret = secret,
                                                code = "",
                                                tag = tag,
                                                digits = digits,
                                                algorithm = algorithm,
                                                period = period,
                                                email = otpViewModel.auth.currentUser?.email ?: "",
                                                otpType = otpType
                                            )
                                        )
                                    )
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() },
                                initialTags = otpViewModel.uniqueTags.value
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
                                        parseOtpAuthUri(code, otpViewModel.auth.currentUser?.email ?: "")?.let { otpSecret ->
                                            otpViewModel.addSecrets(listOf(otpSecret))
                                        }
                                    }

                                    "import" -> {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val compressedBytes = Base64.decode(code, Base64.NO_WRAP)

                                            val inflater = Inflater()
                                            try {
                                                inflater.setInput(compressedBytes)

                                                val outputStream = ByteArrayOutputStream()
                                                val buffer = ByteArray(4096)

                                                while (!inflater.finished()) {
                                                    val count = inflater.inflate(buffer)
                                                    outputStream.write(buffer, 0, count)
                                                }

                                                val decompressedBytes = outputStream.toByteArray()
                                                val cborArray = CBORObject.DecodeFromBytes(decompressedBytes)

                                                val list = mutableListOf<OtpEntry>()
                                                for (i in 0 until cborArray.size()) {
                                                    val obj = cborArray.get(i)
                                                    val otpEntry = OtpEntry(
                                                        codeName = obj["l"].AsString(),
                                                        secret = obj["s"].AsString(),
                                                        algorithm = obj["t"].AsString(),
                                                        period = obj["p"].AsInt32(),
                                                        digits = obj["d"].AsInt32(),
                                                        email = otpViewModel.auth.currentUser?.email
                                                            ?: "",
                                                        tag = obj["g"].AsString() ?: Utils.ALL,
                                                    )

                                                    list.add(otpEntry)
                                                }

                                                otpViewModel.addSecrets(list)
                                            } finally {
                                                inflater.end()
                                            }
                                        }
                                    }
                                }

                                navController.navigate("main") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }

                        composable("settings") {
                            SettingsScreen(navController)
                        }

                        composable("transfer") {
                            TransferCodesScreenWithAuth(navController)
                        }

                        composable("selectCodes") {
                            SelectOtpForExportScreen(
                                navController = navController,
                                otpViewModel = otpViewModel
                            )
                        }

                        composable("exportSelection") {
                            ExportQRCodeScreen(
                                navController = navController,
                                otpEntries = otpViewModel.selectedOtpEntries
                            )
                        }
                    }
                }
            }
        }
    }
}

fun launchGoogleOneTapSignIn(
    context: Context,
    launcher: ActivityResultLauncher<IntentSenderRequest>
) {
    val signInClient = Identity.getSignInClient(context)

    val signInRequest = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build()
        ).build()

    signInClient.beginSignIn(signInRequest).addOnSuccessListener { result ->
        try {
            launcher.launch(
                IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e("GoogleSignIn", "Couldn't launch One Tap UI: ${e.localizedMessage}")
        }
    }.addOnFailureListener { e ->
        Log.e("GoogleSignIn", "One Tap sign-in failed: ${e.localizedMessage}")
    }
}

private fun firebaseAuthWithGoogle(idToken: String, otpViewModel: OtpViewModel, context: Context) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    otpViewModel.auth.signInWithCredential(credential).addOnSuccessListener {
        Log.d("FirebaseAuth", "Sign-in successful")
    }.addOnFailureListener { e ->
        Log.e("FirebaseAuth", "Sign-in failed: ${e.localizedMessage}")
        Toast.makeText(context, "Google sign-in failed: ${e.localizedMessage}", Toast.LENGTH_LONG)
            .show()
    }
}

@Composable
fun ExpandableFab(
    onScanClick: () -> Unit,
    onAddClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "FAB Rotation"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Scan Barcode",
                        modifier = Modifier.size(18.dp)
                    )
                }

                SmallFloatingActionButton(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Add OTP Manually",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (expanded) "Close" else "Open",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TransferCodesScreenWithAuth(navController: NavController) {
    /**
     * State to trigger biometric authentication flow
     * when user taps on export button
     */
    var triggerAuth by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var cameraButtonClicked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(cameraPermissionState.status.isGranted, cameraButtonClicked) {
        if (cameraButtonClicked && cameraPermissionState.status.isGranted) {
            cameraButtonClicked = false
            navController.navigate("scanner?mode=import")
        }
    }

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
        navController.navigate("selectCodes") // Navigate only after successful auth
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
            cameraButtonClicked = true
            if (Utils.isCameraPermissionRequested(context)) {
                when {
                    cameraPermissionState.status.isGranted -> Unit
                    cameraPermissionState.status.shouldShowRationale -> {
                        CoroutineScope(Dispatchers.IO).launch {
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
            } else {
                Utils.setCameraPermissionRequested(context, true)
                CoroutineScope(Dispatchers.IO).launch {
                    cameraPermissionState.launchPermissionRequest()
                }
            }
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
    /** Get the current context from Compose environment */
    val context = LocalContext.current

    /** Get the current lifecycle owner (usually the activity or fragment lifecycle) */
    val lifecycleOwner = LocalLifecycleOwner.current

    /**
     * Extension function to recursively find the FragmentActivity from any Context.
     * Returns null if no FragmentActivity is found.
     */
    fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }

    /** Attempt to find the hosting FragmentActivity or exit early with failure */
    val activity = context.findFragmentActivity() ?: run {
        Log.d("BiometricAuthenticator", "No FragmentActivity found")
        resetTrigger()
        onFailed()
        return
    }

    /** Obtain BiometricManager instance for biometric capability checks */
    val biometricManager = BiometricManager.from(context)

    /** Track if the lifecycle is currently in RESUMED state */
    var isResumed by remember { mutableStateOf(false) }

    /**
     * Add lifecycle observer to track lifecycle state changes.
     * Updates 'isResumed' to true when lifecycle reaches ON_RESUME,
     * and false on any other lifecycle event.
     */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /**
     * Create authentication callback to handle results from biometric prompt.
     * Use 'remember' to retain the same instance across recompositions.
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
                resetTrigger()
                onFailed()
            }
        }
    }

    /**
     * Create a BiometricPrompt instance tied to the FragmentActivity and
     * main thread executor. Remember it to avoid recreation on recomposition.
     */
    val biometricPrompt = remember {
        BiometricPrompt(activity, ContextCompat.getMainExecutor(context), callback)
    }

    val supportsBiometricPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /**
     * Build the prompt information for the biometric dialog.
     * Cache it with 'remember' so it doesn't get recreated unnecessarily.
     */
    val promptInfo = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API 30+
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(heading)
                .setSubtitle(subheading)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        } else {
            // For API 29 and below
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(heading)
                .setSubtitle(subheading)
                .setDeviceCredentialAllowed(true) // Deprecated but required for pre-API 30
                .build()
        }
    }

    /**
     * Trigger biometric authentication only when:
     * 1. The external 'trigger' flag is true, and
     * 2. The lifecycle is currently RESUMED (safe to show dialog).
     */
    LaunchedEffect(trigger, isResumed) {
        if (trigger && isResumed) {
            if (supportsBiometricPrompt) {
                // Check if biometric authentication or device credentials are available
                val canAuthenticate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    biometricManager.canAuthenticate() // Legacy call for API < 30
                }

                when (canAuthenticate) {
                    BiometricManager.BIOMETRIC_SUCCESS,
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                        // Launch biometric prompt
                        biometricPrompt.authenticate(promptInfo)
                    }

                    else -> {
                        // If device has no biometric or lock, auto succeed
                        resetTrigger()
                        onAuthenticated()
                    }
                }
            } else {
                // Fallback for devices without BiometricPrompt support
                resetTrigger()
                onAuthenticated()
            }
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
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val appLockEnabled = remember { prefs.getBoolean("app_lock_enabled", false) }
    val activity = (context as? Activity)

    BackHandler {
        activity?.moveTaskToBack(true)
        /** moves the app to background */
    }

    /** If app lock is enabled, show authentication screen */
    if (appLockEnabled) {
        AuthenticationScreen(
            onAuthenticated = onAuthenticated,
            onFailed = onFailed
        )
    } else {
        /** Skip authentication if app lock is disabled */
        LaunchedEffect(Unit) {
            onAuthenticated()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    /** Called when authentication is successful */
    onAuthenticated: () -> Unit,

    /** Called when authentication fails */
    onFailed: () -> Unit
) {
    /** Controls when biometric authentication should be triggered */
    var triggerAuth by remember { mutableStateOf(true) }

    /** Provides access to the lifecycle of the current composable */
    val lifecycleOwner = LocalLifecycleOwner.current

    /**
     * Observes lifecycle to automatically re-trigger authentication
     * when the app resumes and authentication has not yet been triggered.
     */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !triggerAuth) {
                /** Re-enable biometric authentication on resume */
                triggerAuth = true
            }
        }

        /** Add the observer to the lifecycle */
        lifecycleOwner.lifecycle.addObserver(observer)

        /** Remove the observer when the composable is disposed */
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /** Background surface for the screen */
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        /** Centered layout containing the unlock button */
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            /** Button to manually trigger authentication */
            Button(
                onClick = { triggerAuth = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(44.dp)
            ) {
                /** Lock icon inside the button */
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Unlock",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock")
            }
        }

        /** Biometric authentication logic */
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    var appLockEnabled by remember { mutableStateOf(Utils.isAppLockEnabled(context)) }
    var triggerAuth by remember { mutableStateOf(false) }
    var togglePending by remember { mutableStateOf(false) }

    val unlockOptions = listOf(
        "Immediately" to 0,
        "After 10 seconds" to 10,
        "After 1 minute" to 60,
        "After 10 minutes" to 600,
        "Never" to -1
    )

    var showDialog by rememberSaveable { mutableStateOf(false) }

    val unlockOption = remember { mutableIntStateOf(Utils.getUnlockOption(context)) }

    val selectedOptionText = unlockOptions.firstOrNull { it.second == unlockOption.intValue }?.first
        ?: unlockOptions[0].first

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (appLockEnabled) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialog = true },
                    headlineContent = { Text("Require unlock after app is invisible") },
                    supportingContent = {
                        Text(
                            text = selectedOptionText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }

        if (triggerAuth) {
            BiometricAuthenticator(
                trigger = triggerAuth,
                onAuthenticated = {
                    appLockEnabled = togglePending
                    Utils.setAppLockEnabled(context, togglePending)
                },
                onFailed = {},
                resetTrigger = { triggerAuth = false }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Require unlock after app is invisible") },
                text = {
                    Column {
                        unlockOptions.forEach { (label, value) ->
                            val selected = unlockOption.intValue == value
                            val interactionSource = remember { MutableInteractionSource() }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(36.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = LocalIndication.current
                                    ) {
                                        unlockOption.intValue = value
                                        Utils.setUnlockOption(context, value)
                                        showDialog = false
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        unlockOption.intValue = value
                                        Utils.setUnlockOption(context, value)
                                        showDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
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

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class
)
@Composable
fun OtpScreen(
    progressMap: Map<String, Float>,
    otpViewModel: OtpViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navController: NavController,
    launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    auth: FirebaseAuth
) {
    /** Focus manager to handle keyboard focus */
    val context = LocalContext.current

    /** State for drawer (open/closed) */
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    /** Coroutine scope for launching drawer open/close */
    val drawerScope = rememberCoroutineScope()

    /** search field text field state */
    val textFieldState = remember { TextFieldState() }

    /** State for search query input */
    val query = textFieldState.text.trim()

    /** Camera permission state */
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    /** Coroutine scope for launching suspend functions */
    val coroutineScope = rememberCoroutineScope()

    /** Flag to indicate when the camera FAB is clicked */
    var cameraButtonClicked by remember { mutableStateOf(false) }

    /** is search drawer expanded */
    var expanded by rememberSaveable { mutableStateOf(false) }

    val isSyncing by otpViewModel.isSyncing.collectAsState()

    val credentialManager = remember { CredentialManager.create(context) }

    val selectedTag by otpViewModel.selectedTag.collectAsState()

    val tags by otpViewModel.uniqueTags.collectAsState()

    val otpEntries by otpViewModel.otpEntries.collectAsState(initial = emptyList())

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

    /** Filter OTP entries based on search query using derivedStateOf to optimize recompositions */
    val filteredEntries = if (query.isBlank()) {
        otpEntries
    } else {
        otpEntries.filter {
            it.codeName.contains(query, ignoreCase = true)
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

    Scaffold(
        /** Floating buttons for camera and manual OTP */
        floatingActionButton = {
            if (!expanded) {
                ExpandableFab(
                    onScanClick = {
                        cameraButtonClicked = true
                        if (Utils.isCameraPermissionRequested(context)) {
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
                        } else {
                            Utils.setCameraPermissionRequested(
                                context,
                                true
                            )
                            coroutineScope.launch {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    },
                    onAddClick = {
                        navController.navigate("add")
                    }
                )
            }
        },

        /** Main screen container */
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val drawerItemModifier =
                                Modifier.padding(vertical = 8.dp, horizontal = 8.dp)

                            NavigationDrawerItem(
                                label = { Text("Transfer Codes") },
                                icon = {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = "Transfer Codes"
                                    )
                                },
                                selected = false,
                                onClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        navController.navigate("transfer")
                                    }
                                },
                                modifier = drawerItemModifier
                            )

                            Divider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            NavigationDrawerItem(
                                label = { Text("Settings") },
                                icon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                selected = false,
                                onClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        navController.navigate("settings")
                                    }
                                },
                                modifier = drawerItemModifier
                            )

                            Divider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            NavigationDrawerItem(
                                label = { Text("Feedback") },
                                icon = {
                                    Icon(
                                        Icons.Default.Feedback,
                                        contentDescription = "Feedback"
                                    )
                                },
                                selected = false,
                                onClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        val reviewManager = ReviewManagerFactory.create(context)
                                        reviewManager.requestReviewFlow()
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val reviewInfo = task.result
                                                    reviewManager.launchReviewFlow(
                                                        context as Activity,
                                                        reviewInfo
                                                    )
                                                } else {
                                                    val fallbackIntent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("market://details?id=${context.packageName}")
                                                    ).apply {
                                                        setPackage("com.android.vending")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(fallbackIntent)
                                                }
                                            }
                                    }
                                },
                                modifier = drawerItemModifier
                            )
                        }

                        if (auth.currentUser != null) {
                            NavigationDrawerItem(
                                label = { Text("Sign Out") },
                                icon = {
                                    Icon(
                                        Icons.Default.Logout,
                                        contentDescription = "Sign Out"
                                    )
                                },
                                selected = false,
                                onClick = {
                                    drawerScope.launch {
                                        auth.signOut()

                                        try {
                                            val clearRequest = androidx.credentials.ClearCredentialStateRequest()
                                            credentialManager.clearCredentialState(clearRequest)
                                            Log.d(
                                                "SignOut",
                                                "User credentials cleared successfully"
                                            )
                                        } catch (e: ClearCredentialException) {
                                            Log.e(
                                                "SignOut",
                                                "Couldn't clear user credentials: ${e.localizedMessage}"
                                            )
                                        }

                                        drawerState.close()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedIconColor = Color(0xFFD32F2F),
                                    unselectedTextColor = Color(0xFFD32F2F)
                                )
                            )
                        }
                    }
                }
            }
        )
        {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { isTraversalGroup = true }
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SearchBar(
                    modifier = Modifier
                        .zIndex(1f),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = textFieldState.text.toString(),
                            onQueryChange = { textFieldState.edit { replace(0, length, it) } },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            placeholder = { Text("Search") },
                            leadingIcon = {
                                IconButton(onClick = {
                                    if (expanded) {
                                        expanded = false
                                        textFieldState.edit { replace(0, length, "") }
                                    } else {
                                        drawerScope.launch {
                                            if (drawerState.isClosed) drawerState.open()
                                            else drawerState.close()
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ArrowBack else Icons.Default.Menu,
                                        contentDescription = if (expanded) "Back" else "Open drawer"
                                    )
                                }
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (textFieldState.text.isNotEmpty()) {
                                        IconButton(onClick = {
                                            textFieldState.edit { replace(0, length, "") }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear text"
                                            )
                                        }
                                    }
                                    if (!expanded) {
                                        IconButton(onClick = {
                                            launchGoogleOneTapSignIn(context, launcher)
                                        }) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(45.dp)
                                            ) {
                                                when {
                                                    otpViewModel.auth.currentUser == null -> {
                                                        CircularProgressIndicator(
                                                            progress = 1f,
                                                            strokeWidth = 2.dp,
                                                            color = Color(0xFFD32F2F),
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }

                                                    isSyncing -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.fillMaxSize(),
                                                            strokeWidth = 2.dp,
                                                            color = Color(0xFFFFA000)
                                                        )
                                                    }

                                                    else -> {
                                                        CircularProgressIndicator(
                                                            progress = 1f,
                                                            strokeWidth = 2.dp,
                                                            color = Color(0xFF388E3C),
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }

                                                if (otpViewModel.auth.currentUser != null) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(
                                                            ImageRequest.Builder(context)
                                                                .data(otpViewModel.auth.currentUser?.photoUrl)
                                                                .crossfade(300)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                                .build()
                                                        ),
                                                        contentDescription = "User Profile Picture",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(
                                                            ImageRequest.Builder(context)
                                                                .data("https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png")
                                                                .crossfade(300)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                                .build()
                                                        ),
                                                        contentDescription = "User Profile Picture",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    if (filteredEntries.isEmpty()) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "No OTP",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "It’s a little empty in here!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(filteredEntries, key = { it.id }) { otp ->
                                val userEmail = otpViewModel.auth.currentUser?.email

                                if (userEmail != null) {
                                    if (otp.email != userEmail) return@items // Skip entries not matching signed-in email
                                } else {
                                    if (otp.email.isNotBlank()) return@items // Skip if not anonymous
                                }

                                val progress = progressMap[otp.id] ?: 1f

                                OtpCard(
                                    otp = otp,
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expanded = false
                                        },
                                    SearchBarDefaults.colors().containerColor
                                )
                            }
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isSyncing,
                    onRefresh = {
                        CoroutineScope(Dispatchers.IO).launch {
                            otpViewModel.fetchAllFromCloudSafe()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tags.forEach { tag ->
                                val isSelected = tag == selectedTag

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { otpViewModel.onTagSelected(tag) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize(),
                        ) {
                            if (otpEntries.isEmpty() && otpViewModel.selectedTag.value == Utils.ALL) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = "No OTP",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "It’s a little empty in here!",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                        }
                                    }
                                }
                            } else if (otpEntries.isEmpty() && otpViewModel.selectedTag.value != Utils.ALL) {
                                otpViewModel.onTagSelected(Utils.ALL)
                            } else {
                                items(
                                    items = otpEntries,
                                    key = { it.id }
                                ) { otp ->
                                    val userEmail = otpViewModel.auth.currentUser?.email

                                    if (userEmail != null) {
                                        if (otp.email != userEmail) return@items // Skip entries not matching signed-in email
                                    } else {
                                        if (otp.email.isNotBlank()) return@items // Skip if not anonymous
                                    }

                                    val progress = progressMap[otp.id] ?: 1f
                                    val dismissState = rememberDismissState(
                                        confirmStateChange = { dismissValue ->
                                            if (dismissValue == DismissValue.DismissedToStart) {
                                                otpPendingDeletion = otp
                                                false
                                            } else false
                                        }
                                    )

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
                                            .animateItem()
                                    ) {
                                        if(otp.otpType == Utils.HOTP) {
                                            HotpCard(
                                                otp = otp,
                                                onGenerateClick = {
                                                    otpViewModel.generateHOTPCode(otp)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            OtpCard(
                                                otp = otp,
                                                progress = progress,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotpCard(
    otp: OtpEntry,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val codeName = remember(otp.codeName) {
        otp.codeName.ifBlank { "Unknown Issuer" }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

                IconButton(
                    onClick = onGenerateClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Generate new HOTP",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpCard(
    otp: OtpEntry,
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val codeName = remember(otp.codeName) {
        otp.codeName.ifBlank { "Unknown Issuer" }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
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

private fun encodeEntriesToBase64(entries: List<OtpEntry>): String {
    val cborArray = CBORObject.NewArray()
    entries.forEach {
        val obj = CBORObject.NewMap()
        obj.Add("l", it.codeName)
        obj.Add("s", it.secret)
        obj.Add("t", it.algorithm)
        obj.Add("p", it.period)
        obj.Add("d", it.digits)
        obj.Add("g", it.tag)
        cborArray.Add(obj)
    }

    val cborBytes = cborArray.EncodeToBytes()
    val deflater = Deflater()
    deflater.setInput(cborBytes)
    deflater.finish()

    val buffer = ByteArray(2048)
    val compressedSize = deflater.deflate(buffer)
    deflater.end()

    val compressed = buffer.copyOf(compressedSize)
    return Base64.encodeToString(compressed, Base64.NO_WRAP)
}

private fun createCompressedChunks(entries: List<OtpEntry>, maxBytes: Int): List<String> {
    val chunks = mutableListOf<List<OtpEntry>>()
    var currentChunk = mutableListOf<OtpEntry>()

    for (entry in entries) {
        val testChunk = currentChunk + entry
        val encoded = encodeEntriesToBase64(testChunk)
        if (encoded.toByteArray(Charsets.UTF_8).size <= maxBytes) {
            currentChunk.add(entry)
        } else {
            if (currentChunk.isNotEmpty()) {
                chunks.add(currentChunk)
            }
            currentChunk = mutableListOf(entry)
        }
    }

    if (currentChunk.isNotEmpty()) {
        chunks.add(currentChunk)
    }

    return chunks.map { encodeEntriesToBase64(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectOtpForExportScreen(
    navController: NavController,
    otpViewModel: OtpViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedSet = remember { mutableStateListOf<OtpEntry>() }
    var loadedOTPs by remember { mutableStateOf(emptyList<OtpEntry>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            loadedOTPs = otpViewModel._otpEntries.value
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Codes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoading && loadedOTPs.isNotEmpty()) {
                        val allSelected = selectedSet.size == loadedOTPs.size
                        IconButton(
                            onClick = {
                                if (allSelected) selectedSet.clear()
                                else {
                                    selectedSet.clear()
                                    selectedSet.addAll(loadedOTPs)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.ClearAll else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Clear All" else "Select All"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedSet.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("Export (${selectedSet.size})") },
                    icon = { Icon(Icons.Default.Upload, contentDescription = null) },
                    onClick = {
                        val entriesToExport = selectedSet.toList()
                        otpViewModel.selectedOtpEntries = entriesToExport
                        navController.navigate("exportSelection") {
                            popUpTo("main") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }

                        selectedSet.clear()
                    },
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        containerColor = colorScheme.background,
        contentColor = colorScheme.onBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                }

                loadedOTPs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Nothing to export",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nothing to export!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
                    ) {
                        items(loadedOTPs) { otp ->
                            val userEmail = otpViewModel.auth.currentUser?.email

                            if (userEmail != null) {
                                if (otp.email != userEmail) return@items // Skip entries not matching signed-in email
                            } else {
                                if (otp.email.isNotBlank()) return@items // Skip if not anonymous
                            }

                            val isSelected = otp in selectedSet
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSelected) selectedSet.remove(otp)
                                        else selectedSet.add(otp)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colorScheme.primary
                                        )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = otp.codeName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportQRCodeScreen(
    /** Navigation controller to handle back navigation */
    navController: NavController,

    /** List of OTP entries to be exported */
    otpEntries: List<OtpEntry>
) {
    /** Maximum byte size for each compressed QR chunk */
    val maxCompressedBytes = 500

    /** List of compressed QR code chunks */
    var compressedChunks by remember { mutableStateOf(emptyList<String>()) }

    /** Flag to indicate loading of chunks */
    var isLoading by remember { mutableStateOf(true) }

    /** Current index of the QR code being displayed */
    var currentIndex by remember { mutableIntStateOf(0) }

    /** Generated QR bitmap */
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    /** Material color scheme for theming */
    val colorScheme = MaterialTheme.colorScheme

    /** Check if chunks are loaded and available */
    val hasData = compressedChunks.isNotEmpty() && !isLoading

    /** Load and compress OTP entries in the background */
    LaunchedEffect(otpEntries) {
        val chunks = withContext(Dispatchers.Default) {
            createCompressedChunks(otpEntries, maxCompressedBytes)
        }
        compressedChunks = chunks
        isLoading = false
    }

    /** Generate QR code bitmap when data/index changes */
    LaunchedEffect(currentIndex, compressedChunks) {
        if (compressedChunks.isNotEmpty()) {
            qrBitmap = withContext(Dispatchers.Default) {
                generateQRCodeBitmap(compressedChunks[currentIndex], 600, 600)
            }
        }
    }

    /** Scaffold layout with app bar and body content */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = colorScheme.background,
        contentColor = colorScheme.onBackground
    ) { paddingValues ->

        /** Main column layout */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
        ) {

            /** QR content area */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    /** Show progress spinner while loading */
                    isLoading -> {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }

                    /** Show empty state if no data */
                    !hasData -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Outlined.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(120.dp)
                                    .padding(bottom = 16.dp)
                            )
                            Text(
                                "Nothing to export!",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    /** Show QR code and its index */
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            /** QR Code display box */
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .shadow(8.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                qrBitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Generating QR...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            /** Next / Previous buttons (always visible) */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                /** Previous button */
                Button(
                    onClick = { currentIndex-- },
                    modifier = Modifier.weight(1f),
                    enabled = hasData && currentIndex > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    Spacer(Modifier.width(8.dp))
                    Text("Previous")
                }

                /** Next button */
                Button(
                    onClick = { currentIndex++ },
                    modifier = Modifier.weight(1f),
                    enabled = hasData && currentIndex < compressedChunks.lastIndex,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("Next")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }
    }
}

/** Helper function to generate a QR code bitmap from string content */
fun generateQRCodeBitmap(
    text: String,
    width: Int,
    height: Int
): Bitmap? {
    if (text.isEmpty()) return null

    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                pixels[y * width + x] = color
            }
        }

        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    onAdd: (codeName: String, secret: String, digits: Int, algorithm: String, period: Int, tag: String, otpType: Int) -> Unit,
    onCancel: () -> Unit,
    initialTags: List<String> = Utils.defaultTags // Pass existing tags from outside, e.g., from ViewModel
) {
    var codeName by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var digits by rememberSaveable { mutableStateOf("6") }
    var algorithm by rememberSaveable { mutableStateOf("SHA1") }
    var otpType by rememberSaveable { mutableStateOf("Time Based") }
    var period by rememberSaveable { mutableStateOf("30") }

    var tags by remember { mutableStateOf(initialTags) }
    var selectedTag by rememberSaveable { mutableStateOf(Utils.ALL) }
    var addTagDialogVisible by remember { mutableStateOf(false) }
    var newTagText by rememberSaveable { mutableStateOf("") }

    val canAdd = codeName.isNotBlank() && secret.isNotBlank() && digits.toIntOrNull() != null &&
            algorithm.isNotBlank() && period.toIntOrNull() != null && selectedTag.isNotBlank()

    val focusManager = LocalFocusManager.current

    if (addTagDialogVisible) {
        AlertDialog(
            onDismissRequest = { addTagDialogVisible = false },
            title = { Text("Add New Tag") },
            text = {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    label = { Text("Tag Name") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.05f
                        )
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedTag = newTagText.trim()
                        if (trimmedTag.isNotEmpty() && !tags.contains(trimmedTag)) {
                            tags = tags + trimmedTag
                            selectedTag = trimmedTag
                        }
                        newTagText = ""
                        addTagDialogVisible = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newTagText = ""
                    addTagDialogVisible = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter details") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                maxLines = 3,
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
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                )
            )

            var otpTypeExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = otpTypeExpanded,
                onExpandedChange = { otpTypeExpanded = !otpTypeExpanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = otpType,
                    shape = RoundedCornerShape(12.dp),
                    onValueChange = {},
                    label = { Text("Type of key") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = otpTypeExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = MenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        ),
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
                    expanded = otpTypeExpanded,
                    onDismissRequest = { otpTypeExpanded = false }
                ) {
                    Utils.otpTypes.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                otpType = selectionOption
                                otpTypeExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedTag,
                        onValueChange = {},
                        label = { Text("Tag") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
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
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        tags.forEach { tagOption ->
                            DropdownMenuItem(
                                text = { Text(tagOption) },
                                onClick = {
                                    selectedTag = tagOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { addTagDialogVisible = true },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Tag",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

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

                    ExposedDropdownMenuBox(
                        expanded = digitsExpanded,
                        onExpandedChange = { digitsExpanded = !digitsExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = digits,
                            shape = RoundedCornerShape(12.dp),
                            onValueChange = {},
                            label = { Text("Digits") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = digitsExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                ),
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
                            Utils.digitOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption.toString()) },
                                    onClick = {
                                        digits = selectionOption.toString()
                                        digitsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var algorithmExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = algorithmExpanded,
                        onExpandedChange = { algorithmExpanded = !algorithmExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = algorithm,
                            shape = RoundedCornerShape(12.dp),
                            onValueChange = {},
                            label = { Text("Algorithm") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = algorithmExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                ),
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
                            Utils.algorithmOptions.forEach { selectionOption ->
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
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Utils.totpTimeIntervals.forEach { option ->
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
                                period.toInt(),
                                selectedTag,
                                if (otpType == "Time Based") Utils.TOTP else Utils.HOTP
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