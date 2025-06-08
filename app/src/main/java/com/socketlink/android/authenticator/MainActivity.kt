package com.socketlink.android.authenticator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.socketlink.android.authenticator.OtpUtils.parseOtpAuthUri
import com.socketlink.android.authenticator.ui.theme.SocketlinkAuthenticatorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val otpViewModel: OtpViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
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
            SocketlinkAuthenticatorTheme {
                /** NavController to handle navigation */
                val navController = rememberNavController()

                /** Collect OTP entries and progress map from ViewModel */
                val otpEntries by otpViewModel.otpEntries.collectAsState()
                val progressMap by otpViewModel.progressMap.collectAsState()

                /** Camera permission state */
                val cameraPermissionState =
                    rememberPermissionState(android.Manifest.permission.CAMERA)

                /** Coroutine scope for launching suspend functions */
                val coroutineScope = rememberCoroutineScope()

                var cameraButtonClicked by remember { mutableStateOf(false) }

                LaunchedEffect(cameraPermissionState.status.isGranted, cameraButtonClicked) {
                    if (cameraButtonClicked && cameraPermissionState.status.isGranted) {
                        cameraButtonClicked = false
                        navController.navigate("scanner")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = "main",
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    ) {
                        /** Main screen with OTP list and FABs */
                        composable("main") {
                            Scaffold(
                                floatingActionButton = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        /** Camera scan FAB with streamlined permission logic */
                                        FloatingActionButton(
                                            onClick = {
                                                cameraButtonClicked = true

                                                when {
                                                    cameraPermissionState.status.isGranted -> {
                                                        /** do not navigate here */
                                                    }

                                                    cameraPermissionState.status.shouldShowRationale -> {
                                                        coroutineScope.launch {
                                                            cameraPermissionState.launchPermissionRequest()
                                                        }
                                                    }

                                                    else -> {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Please enable camera permission in app settings",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        openAppSettings(this@MainActivity)
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PhotoCamera,
                                                contentDescription = "Scan Barcode"
                                            )
                                        }

                                        /** Manual OTP add FAB */
                                        FloatingActionButton(
                                            onClick = { navController.navigate("add") },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Keyboard,
                                                contentDescription = "Add OTP Manually"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                OtpScreen(
                                    otpEntries = otpEntries,
                                    progressMap = progressMap,
                                    modifier = Modifier.padding(innerPadding),
                                    otpViewModel = otpViewModel
                                )
                            }
                        }

                        /** Add OTP screen */
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

                        /** Barcode scanner screen */
                        composable("scanner") {
                            var scannedCode by remember { mutableStateOf<String?>(null) }

                            ScannerScreen(
                                onBarcodeDetected = { if (scannedCode == null) scannedCode = it },
                                onCancel = { navController.popBackStack() }
                            )

                            scannedCode?.let { code ->
                                LaunchedEffect(code) {
                                    parseOtpAuthUri(code)?.let { otpSecret ->
                                        otpViewModel.addSecret(otpSecret)
                                        navController.popBackStack()
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
    otpViewModel: OtpViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    /** Obtain current focus manager to control keyboard focus */
    val focusManager = LocalFocusManager.current

    /** Holds the current search query string, saved across recompositions */
    var searchQuery by rememberSaveable { mutableStateOf("") }

    /** Filter otpEntries based on searchQuery; recompute only when otpEntries or searchQuery changes */
    val filteredEntries = remember(otpEntries, searchQuery) {
        otpEntries.filter { otp ->
            otp.codeName.contains(searchQuery, ignoreCase = true) || searchQuery.isBlank()
        }
    }

    /** Root column wrapping the entire screen UI */
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            /** Clear focus when clicking outside input fields */
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            }
    ) {
        /** Search input field with clear button */
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search...") },
            singleLine = true,
            shape = RoundedCornerShape(34.dp),
            trailingIcon = {
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

        /** List of OTP entries shown as lazy scrolling column */
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            /** Iterate over filtered OTP entries */
            items(
                items = filteredEntries,
                key = { it.id } // Unique key for efficient recomposition
            ) { otp ->

                /** Get progress value for current OTP or default to 1f */
                val progress = progressMap[otp.id] ?: 1f

                /**
                 * Remember DismissState for swipe-to-dismiss.
                 * confirmStateChange is called when swipe dismissal occurs.
                 * If dismissed to start (swipe left), trigger deletion and confirm change.
                 */
                val dismissState = rememberDismissState(
                    confirmStateChange = { dismissValue ->
                        if (dismissValue == DismissValue.DismissedToStart) {
                            otpViewModel.deleteSecret(otp) // Delete OTP from ViewModel
                            true // Confirm dismissal to update UI
                        } else {
                            false // Prevent dismissal for other swipe states
                        }
                    }
                )

                /** SwipeToDismiss UI wrapper enabling swipe-to-delete functionality */
                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(DismissDirection.EndToStart), // Swipe only left
                    dismissThresholds = { FractionalThreshold(0.8f) }, // Threshold before dismissal triggers
                    background = {
                        /** Background behind item while swiping, shows delete icon */
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface) // Keep background consistent
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
                        .animateItemPlacement() // Animate item repositioning smoothly after removal
                ) {
                    /** The OTP card UI displaying OTP details and progress */
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
    onCancel: () -> Unit
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

    // Common background for buttons
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


// Custom composable that draws the scanner border with rounded corners and gaps in edges
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

















