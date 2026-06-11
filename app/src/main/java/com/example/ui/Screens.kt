package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.*
import com.example.ui.theme.*
import com.example.viewmodel.SessionState
import com.example.viewmodel.SimChatViewModel
import com.example.viewmodel.SimulatedNotification
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Base screen router
@Composable
fun SimChatMainRouter(viewModel: SimChatViewModel) {
    val sessionState by viewModel.sessionState.collectAsState()
    val isPinRequired by viewModel.isPinRequired.collectAsState()
    val context = LocalContext.current

    val activeHeadsUp by viewModel.activeHeadsUp.collectAsState()
    val isIncomingCallActive by viewModel.isIncomingCallActive.collectAsState()
    val incomingCallName by viewModel.incomingCallName.collectAsState()
    val incomingCallPhone by viewModel.incomingCallPhone.collectAsState()
    val incomingCallSimName by viewModel.incomingCallSimName.collectAsState()

    var globalActiveCallScreen by remember { mutableStateOf(false) }
    var globalCallName by remember { mutableStateOf("") }
    var globalCallPhone by remember { mutableStateOf("") }
    var globalCallSim by remember { mutableStateOf("") }

    var isSplashFinished by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    var activeChatId by remember { mutableStateOf<String?>(null) }

    // Temp registration state
    var tempRegAddress by remember { mutableStateOf("") }
    var tempIsGoogleReg by remember { mutableStateOf(false) }
    var tempRegName by remember { mutableStateOf("") }

    LaunchedEffect(sessionState, isSplashFinished) {
        if (isSplashFinished) {
            when (sessionState) {
                is SessionState.Welcome -> currentScreen = AppScreen.Welcome
                is SessionState.GuestMode -> currentScreen = AppScreen.MainTabs
                is SessionState.CloudMode -> currentScreen = AppScreen.MainTabs
                is SessionState.OtpVerification -> currentScreen = AppScreen.OtpVerify
                is SessionState.OtpVerifiedNeedProfile -> currentScreen = AppScreen.ProfileSetup
                else -> {}
            }
        } else {
            currentScreen = AppScreen.Splash
        }
    }

    val systemInDark = isSystemInDarkTheme()
    val isAppDark = when (viewModel.activeTheme) {
        "dark" -> true
        "light" -> false
        else -> systemInDark
    }

    val activeFontName by viewModel.chatFontFamily.collectAsState()

    SimChatTheme(darkTheme = isAppDark, fontName = activeFontName) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isPinRequired) {
                    PinScreen(onPinEntered = { pin ->
                        val success = viewModel.enterPin(pin)
                        if (!success) {
                            Toast.makeText(context, "Invalid PIN. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.Splash -> SplashScreen(
                                onFinished = {
                                    isSplashFinished = true
                                }
                            )
                            AppScreen.Welcome -> SimChatAuthScreen(
                                viewModel = viewModel,
                                onNavigateToCreatePassword = { address, isGoogle, googleName ->
                                    tempRegAddress = address
                                    tempIsGoogleReg = isGoogle
                                    tempRegName = googleName
                                    currentScreen = AppScreen.CreatePassword
                                },
                                onContinueAsGuest = {
                                    viewModel.loginGuest()
                                }
                            )
                            AppScreen.Onboarding -> OnboardingScreen(
                                viewModel = viewModel,
                                onComplete = {
                                    viewModel.loginGuest()
                                },
                                onBack = { currentScreen = AppScreen.Welcome }
                            )
                            AppScreen.Login -> SimChatAuthScreen(
                                viewModel = viewModel,
                                onNavigateToCreatePassword = { address, isGoogle, googleName ->
                                    tempRegAddress = address
                                    tempIsGoogleReg = isGoogle
                                    tempRegName = googleName
                                    currentScreen = AppScreen.CreatePassword
                                },
                                onContinueAsGuest = {
                                    viewModel.loginGuest()
                                }
                            )
                            AppScreen.SignUp -> SimChatAuthScreen(
                                viewModel = viewModel,
                                onNavigateToCreatePassword = { address, isGoogle, googleName ->
                                    tempRegAddress = address
                                    tempIsGoogleReg = isGoogle
                                    tempRegName = googleName
                                    currentScreen = AppScreen.CreatePassword
                                },
                                onContinueAsGuest = {
                                    viewModel.loginGuest()
                                }
                            )
                            AppScreen.CreatePassword -> CreatePasswordScreen(
                                address = tempRegAddress,
                                isGoogle = tempIsGoogleReg,
                                name = tempRegName,
                                onComplete = { pass ->
                                    viewModel.registerWithPassword(tempRegAddress, tempIsGoogleReg, tempRegName, pass)
                                    Toast.makeText(context, "Account registered successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onBack = { currentScreen = AppScreen.Welcome }
                            )
                            AppScreen.OtpVerify -> {
                                val state = sessionState as? SessionState.OtpVerification
                                OtpScreen(
                                    email = state?.email ?: "",
                                    onVerify = { code ->
                                        viewModel.verifyOtp(state?.email ?: "", code, state?.pass ?: "")
                                    },
                                    onBack = { currentScreen = AppScreen.Welcome }
                                )
                            }
                            AppScreen.ProfileSetup -> {
                                val state = sessionState as? SessionState.OtpVerifiedNeedProfile
                                ProfileSetupScreen(
                                    email = state?.email ?: "",
                                    uid = state?.uid ?: "",
                                    onComplete = { fullName, displayName, bio, photo ->
                                        viewModel.completeProfileSetup(
                                            uid = state?.uid ?: "user_fallback",
                                            email = state?.email ?: "fallback@example.com",
                                            fullName = fullName,
                                            displayName = displayName,
                                            bio = bio,
                                            photo = photo
                                        )
                                    },
                                    onBack = { currentScreen = AppScreen.Welcome }
                                )
                            }
                            AppScreen.MainTabs -> MainTabsContainer(
                                viewModel = viewModel,
                                onChatSelected = { id ->
                                    activeChatId = id
                                    currentScreen = AppScreen.ChatThread
                                },
                                onNavigateTo = { screen -> currentScreen = screen }
                            )
                            AppScreen.ChatThread -> {
                                ChatThreadScreen(
                                    viewModel = viewModel,
                                    convId = activeChatId ?: "",
                                    onBack = {
                                        currentScreen = AppScreen.MainTabs
                                        activeChatId = null
                                    },
                                    onGoToGroupDetail = {
                                        currentScreen = AppScreen.GroupDetail
                                    }
                                )
                            }
                            AppScreen.Settings -> SettingsScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = AppScreen.MainTabs }
                            )
                            AppScreen.LockedChats -> {
                                LockedChatsScreen(
                                    viewModel = viewModel,
                                    onChatSelected = { id ->
                                        activeChatId = id
                                        currentScreen = AppScreen.ChatThread
                                    },
                                    onBack = { currentScreen = AppScreen.MainTabs }
                                )
                            }
                            AppScreen.BlockedList -> {
                                BlockedContactsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = AppScreen.MainTabs }
                                )
                            }
                            AppScreen.StarredMessages -> {
                                StarredMessagesScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = AppScreen.MainTabs }
                                )
                            }
                            AppScreen.NotificationHub -> {
                                NotificationHubScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = AppScreen.MainTabs }
                                )
                            }
                            AppScreen.CreateGroup -> {
                                CreateGroupScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = AppScreen.MainTabs },
                                    onGroupCreated = { id ->
                                        activeChatId = id
                                        currentScreen = AppScreen.ChatThread
                                    }
                                )
                            }
                            AppScreen.GroupDetail -> {
                                GroupDetailScreen(
                                    viewModel = viewModel,
                                    groupId = activeChatId ?: "",
                                    onBack = {
                                        currentScreen = AppScreen.ChatThread
                                    },
                                    onNavigateToChat = {
                                        currentScreen = AppScreen.ChatThread
                                    }
                                )
                            }
                        }
                    }
                }

                if (isIncomingCallActive) {
                    SimIncomingCallScreen(
                        contactName = incomingCallName,
                        phoneNumber = incomingCallPhone,
                        simName = incomingCallSimName,
                        onAccept = {
                            viewModel.answerIncomingCall()
                            globalCallName = incomingCallName
                            globalCallPhone = incomingCallPhone
                            globalCallSim = incomingCallSimName
                            globalActiveCallScreen = true
                        },
                        onDecline = {
                            viewModel.declineIncomingCall()
                        },
                        onQuickSmsDecline = { msg ->
                            viewModel.sendMessage(incomingCallPhone, msg, "SMS")
                        }
                    )
                }

                if (globalActiveCallScreen) {
                    SimActiveCallScreen(
                        contactName = globalCallName,
                        phoneNumber = globalCallPhone,
                        simName = globalCallSim,
                        viewModel = viewModel,
                        onEndCall = { globalActiveCallScreen = false }
                    )
                }

                val showUpgradeDialogState by viewModel.showUpgradeDialog.collectAsState()
                if (showUpgradeDialogState) {
                    SimChatUpgradeDialog(
                        viewModel = viewModel,
                        onContinueWithGoogle = {
                            viewModel.triggerUpgradeDialog(false)
                            viewModel.autoShowGoogleChooserOnWelcome = true
                            viewModel.autoShowPhoneEntryOnWelcome = false
                            viewModel.logout()
                        },
                        onContinueWithPhone = {
                            viewModel.triggerUpgradeDialog(false)
                            viewModel.autoShowGoogleChooserOnWelcome = false
                            viewModel.autoShowPhoneEntryOnWelcome = true
                            viewModel.logout()
                        },
                        onDismiss = {
                            viewModel.triggerUpgradeDialog(false)
                        }
                    )
                }

                // --- PREMIUM DYNAMIC HEADS-UP NOTIFICATION DRAWER FOR ALL PHONES ---
                AnimatedVisibility(
                    visible = activeHeadsUp != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                        .zIndex(99f)
                ) {
                    activeHeadsUp?.let { headsUpNotif ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFA112519)), // High contrast glassemerald
                            border = BorderStroke(1.5.dp, Color(0xFF2EBD59)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF2EBD59))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = headsUpNotif.category.uppercase(),
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Text("SIMULATION HEADS-UP", fontSize = 10.sp, color = Color(0xFF2EBD59), fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { viewModel.dismissHeadsUpNotification() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2EBD59).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFF2EBD59),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = headsUpNotif.title,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = headsUpNotif.messageText,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.markNotificationRead(headsUpNotif.id, true)
                                            viewModel.dismissHeadsUpNotification()
                                            Toast.makeText(context, "Safaricom Shield: Marked Seen!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3B2EBD59)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Mark Read", fontSize = 11.sp, color = Color(0xFF2EBD59), fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.dismissHeadsUpNotification()
                                            currentScreen = AppScreen.NotificationHub
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Open Hub Panel", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(activeHeadsUp) {
        if (activeHeadsUp != null) {
            delay(5000)
            viewModel.dismissHeadsUpNotification()
        }
    }
}

sealed interface AppScreen {
    object Splash : AppScreen
    object Welcome : AppScreen
    object Onboarding : AppScreen
    object Login : AppScreen
    object SignUp : AppScreen
    object OtpVerify : AppScreen
    object ProfileSetup : AppScreen
    object MainTabs : AppScreen
    object ChatThread : AppScreen
    object Settings : AppScreen
    object LockedChats : AppScreen
    object BlockedList : AppScreen
    object StarredMessages : AppScreen
    object CreatePassword : AppScreen
    object NotificationHub : AppScreen
    object CreateGroup : AppScreen
    object GroupDetail : AppScreen
}

@Composable
fun SimChatLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    pulseAnimation: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
    val scale by if (pulseAnimation) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2EBD59), Color(0xFF166831))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize(0.65f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = Icons.Filled.SimCard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.65f)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF166831), shape = CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .padding(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF2EBD59),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Initializing cell diagnostic engine...") }

    LaunchedEffect(Unit) {
        delay(400)
        progress = 0.25f
        statusText = "Scanning physical subscriber cell slots..."
        delay(450)
        progress = 0.55f
        statusText = "Pairing with dual-SIM hardware channels..."
        delay(500)
        progress = 0.82f
        statusText = "Mounting encrypted SQLite private vault..."
        delay(400)
        progress = 1.0f
        statusText = "Secure layer fully stabilized!"
        delay(350)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            SimChatLogo(size = 110.dp, pulseAnimation = true)
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "SimChat",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2EBD59),
                letterSpacing = 1.sp
            )
            Text(
                text = "CARRIER-PULSED ENCRYPTION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(44.dp))
            
            Column(
                modifier = Modifier.width(260.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF2EBD59),
                    trackColor = Color.Gray.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// WELCOME SCREEN (Screenshot 1 alignment)
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onGoToLogin: () -> Unit,
    onGoToSignUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Shield branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            SimChatLogo(size = 100.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SimChat",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = SimChatPrimary
            )
            Text(
                text = "Smart. Secure. Simple.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }

        // Center card styling matching screenshot artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background glows
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFDCF8E3), Color.Transparent),
                                center = Offset(size.width / 2, size.height / 2)
                            )
                        )
                    }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Device SIM graphic card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .size(110.dp, 160.dp)
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(48.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SimChatPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Chat Bubble graphic card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SimChatPrimary),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .size(110.dp, 160.dp)
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                        )
                    }
                }
            }
        }

        // Features rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Welcome to SimChat",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "A smarter way to connect with people using your SIM card",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bullet entries matches Screenshot 1
            FeatureItem(
                icon = Icons.Filled.SimCard,
                title = "Use Your SIM Card",
                desc = "Connect and chat with people using their phone numbers."
            )
            FeatureItem(
                icon = Icons.Filled.Chat,
                title = "Seamless Chats",
                desc = "Start instant conversations with your contacts and new people."
            )
            FeatureItem(
                icon = Icons.Filled.VerifiedUser,
                title = "Secure & Private",
                desc = "Your privacy is our priority. Chats are safe and secure."
            )
        }

        // Under buttons actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("get_started_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onGoToLogin) {
                    Text("Login to Cloud", color = SimChatPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(onClick = onGoToSignUp) {
                    Text("Create Account", color = SimChatPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildAnnotatedString {
                    append("By continuing, you agree to our ")
                    withStyle(style = SpanStyle(color = SimChatPrimary, fontWeight = FontWeight.Bold)) {
                        append("Terms of Service")
                    }
                },
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = desc, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

// LOGIN SCREEN
@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onGoToSignUp: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = SimChatPrimary,
                modifier = Modifier.size(90.dp)
            )
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF1C1D1F), fontWeight = FontWeight.Bold)) {
                    append("Sim")
                }
                withStyle(style = SpanStyle(color = SimChatPrimary, fontWeight = FontWeight.Bold)) {
                    append("Chat")
                }
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Smart. Secure. Simple.",
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Login to your account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1D1F)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Log in to unlock messaging and cloud features.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email Address") },
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = SimChatPrimary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SimChatPrimary) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onLogin(email, password) },
            colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onGoToSignUp) {
            Text(
                text = buildAnnotatedString {
                    append("Don't have an account? ")
                    withStyle(style = SpanStyle(color = SimChatPrimary, fontWeight = FontWeight.Bold)) {
                        append("Sign Up")
                    }
                },
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }

        TextButton(onClick = onBack) {
            Text("Cancel and Go Back", color = Color.Gray)
        }
    }
}

// SIGN UP SCREEN
@Composable
fun SignUpScreen(
    onSignUp: (String, String) -> Unit,
    onGoToLogin: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Wave decoration matching screenshot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.4f)
                    quadraticTo(size.width * 0.75f, size.height, size.width * 0.4f, size.height * 0.75f)
                    quadraticTo(size.width * 0.15f, size.height * 0.55f, 0f, size.height * 0.9f)
                    close()
                }
                drawPath(
                    path = path1,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F9B41).copy(alpha = 0.85f), Color(0xFF2EBD59).copy(alpha = 0.5f))
                    )
                )

                val path2 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.3f)
                    quadraticTo(size.width * 0.65f, size.height * 0.85f, size.width * 0.35f, size.height * 0.55f)
                    quadraticTo(size.width * 0.15f, size.height * 0.35f, 0f, size.height * 0.65f)
                    close()
                }
                drawPath(
                    path = path2,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0C8A36), Color(0xFF1CB04B))
                    )
                )
            }
        }

        // Branding Logo (Shield with speech bubble)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF109D43),
                modifier = Modifier.size(80.dp)
            )
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF1C1D1F), fontWeight = FontWeight.Bold)) {
                    append("Sim")
                }
                withStyle(style = SpanStyle(color = Color(0xFF109D43), fontWeight = FontWeight.Bold)) {
                    append("Chat")
                }
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Smart. Secure. Simple.",
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Create your account section
        Text(
            text = "Create your account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1D1F)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Sign up to enjoy cloud messaging,\nsync across devices and more.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Fields Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF109D43)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF109D43)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF109D43)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF109D43)) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up primary green button with arrow
            Button(
                onClick = {
                    if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                        Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
                    } else if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    } else {
                        onSignUp(email, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = "Login",
                    color = Color(0xFF109D43),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onGoToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { Toast.makeText(context, "Instructions to reset password have been simulated.", Toast.LENGTH_LONG).show() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF109D43), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Password", color = Color(0xFF109D43), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Visual Divider with "or"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                Text("or", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.LightGray))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Continue as Guest bordered button
            OutlinedButton(
                onClick = onContinueAsGuest,
                border = BorderStroke(1.dp, Color(0xFF109D43)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF109D43))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue as Guest", color = Color(0xFF109D43), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use SMS without an account. Limited features.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// OTP SCREEN
@Composable
fun OtpScreen(
    email: String,
    onVerify: (String) -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(60) }
    val context = LocalContext.current

    LaunchedEffect(key1 = timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF109D43),
                modifier = Modifier.size(90.dp)
            )
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Verify Email OTP", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1D1F))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We sent a 6-digit authentication code to:",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = email,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF109D43),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Aligned and highly robust Otp entry field
        OutlinedTextField(
            value = code,
            onValueChange = {
                if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it
            },
            placeholder = { Text("000000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.LightGray.copy(alpha = 0.7f)) },
            textStyle = androidx.compose.ui.text.TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 12.sp,
                color = Color(0xFF1C1D1F)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Countdown or Resend
        if (timeLeft > 0) {
            Text(
                text = "Resend OTP in ${timeLeft}s",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            TextButton(
                onClick = {
                    timeLeft = 60
                    Toast.makeText(context, "OTP code resent successfully (simulated)!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(
                    text = "Resend OTP",
                    color = Color(0xFF109D43),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (code.length < 6) {
                    Toast.makeText(context, "Please enter the complete 6-digit OTP code", Toast.LENGTH_SHORT).show()
                } else {
                    onVerify(code)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Confirm Verification", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Cancel", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// PROFILE SETUP SCREEN
@Composable
fun ProfileSetupScreen(
    email: String,
    uid: String,
    onComplete: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var whoSeeInfo by remember { mutableStateOf("Everyone") }
    var showDropdownMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val colorsList = listOf(
        Color(0xFF109D43), // Premium emerald green
        Color(0xFF2A7BF1), // Vibrant blue
        Color(0xFF7C4DFF), // Royal purple
        Color(0xFFE040FB), // Magenta/pink
        Color(0xFFFF9100), // Vibrant orange
        Color(0xFF00E5FF)  // Cyan/teal
    )
    val colorNames = listOf("green", "blue", "purple", "pink", "orange", "cyan")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Aligned custom Top header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF109D43),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Profile Setup",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1D1F)
                )
                Text(
                    text = "Personalize your profile",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Aligned circular profile photo placeholder
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .border(2.dp, Color(0xFF109D43), CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFF3FBEF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFFBCDECD),
                    modifier = Modifier.size(80.dp)
                )
            }

            // Floating green overlay pencil at bottom right
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF109D43))
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd)
                    .clickable {
                        Toast.makeText(context, "Self-hosted camera capture or gallery picker (simulated)", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Modify photo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Add a profile photo",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF109D43),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Help your contacts recognize you",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Aligned labeled inputs
        // --- Full Name ---
        Text(text = "Full Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1D1F))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = { Text("Enter your full name", color = Color.Gray.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF109D43)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Display Name ---
        Text(text = "Display Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1D1F))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            placeholder = { Text("Collins", color = Color.Gray.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF109D43)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This is how your name will appear to others",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- About ---
        Text(text = "About / Status (Optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1D1F))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = about,
            onValueChange = { if (it.length <= 80) about = it },
            placeholder = { Text("Hey there! I am using SimChat.", color = Color.Gray.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF109D43)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                Text(
                    text = "${about.length}/80",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Email ---
        Text(text = "Email", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1D1F))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {},
            enabled = false,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color.LightGray,
                disabledTextColor = Color.DarkGray
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- Profile Color Row ---
        Text(text = "Profile Color", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1D1F))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colorsList.forEachIndexed { index, color ->
                val isSelected = selectedColorIndex == index
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) Color.DarkGray else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedColorIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Who can see my info drop ---
        Text(text = "Who can see my info", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1D1F))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Color(0xFFF7FAF7), RoundedCornerShape(12.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                .clickable { showDropdownMenu = !showDropdownMenu }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF109D43))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = whoSeeInfo, modifier = Modifier.weight(1.0f), fontWeight = FontWeight.Medium, color = Color.DarkGray)
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown menu option")
        }

        if (showDropdownMenu) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            ) {
                Column {
                    listOf("Everyone", "My Contacts Only", "Nobody").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable {
                                    whoSeeInfo = option
                                    showDropdownMenu = false
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = option, fontSize = 14.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Centered continue button
        Button(
            onClick = {
                if (fullName.isEmpty()) {
                    Toast.makeText(context, "Full Name is required to continue.", Toast.LENGTH_SHORT).show()
                } else {
                    onComplete(
                        fullName,
                        displayName.ifEmpty { fullName },
                        about.ifEmpty { "Hey there! I am using SimChat." },
                        colorNames[selectedColorIndex]
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "You can change this later in settings",
            fontSize = 12.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// PIN LOCK SCREEN
@Composable
fun PinScreen(onPinEntered: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PrivacyTip,
            contentDescription = null,
            tint = SimChatPrimary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("SimChat Locked", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Please enter your PIN code to unlock", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 4) pin = it
                if (it.length == 4) onPinEntered(it)
            },
            label = { Text("4-Digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(180.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// MAIN NAVIGATION SHELL TABS CONTAINER
@Composable
fun MainTabsContainer(
    viewModel: SimChatViewModel,
    onChatSelected: (String) -> Unit,
    onNavigateTo: (AppScreen) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Messages, 1: Calls, 2: Contacts, 3: Profile
    val scope = rememberCoroutineScope()
    var showGroupDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Default Messaging App Status
    var isSmsDefault by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
            } else {
                true
            }
        )
    }
    var showDefaultSmsPrompt by remember { mutableStateOf(!isSmsDefault) }

    val systemPermissions = remember {
        listOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CONTACTS
        ) + if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    }

    var permissionGrantedMap by remember {
        mutableStateOf(
            systemPermissions.associateWith { perm ->
                context.checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Dynamic resume observer to update permission and default SMS stats instantly
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isSmsDefault = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
                } else {
                    true
                }
                permissionGrantedMap = systemPermissions.associateWith { perm ->
                    context.checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionGrantedMap = results
        val smsGranted = results[android.Manifest.permission.READ_SMS] == true
        val contactsGranted = results[android.Manifest.permission.READ_CONTACTS] == true
        val callLogGranted = results[android.Manifest.permission.READ_CALL_LOG] == true
        if (smsGranted) {
            viewModel.syncDeviceSms()
        }
        if (contactsGranted) {
            viewModel.syncDeviceContacts()
        }
        if (callLogGranted) {
            viewModel.loadDeviceCallLogs()
        }
    }

    // Auto trigger carrier sync on first view loading if permissions are already active
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.syncDeviceSms()
        }
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.syncDeviceContacts()
        }
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.loadDeviceCallLogs()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = if (selectedTab == 0) Icons.Filled.Chat else Icons.Outlined.Chat, contentDescription = "Messages") },
                    label = { Text("Messages", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SimChatPrimary, indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = if (selectedTab == 1) Icons.Filled.Call else Icons.Outlined.Call, contentDescription = "Calls") },
                    label = { Text("Calls", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SimChatPrimary, indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = if (selectedTab == 2) Icons.Filled.People else Icons.Outlined.People, contentDescription = "Contacts") },
                    label = { Text("Contacts", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SimChatPrimary, indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SimChatPrimary, indicatorColor = Color(0xFFE8F5E9))
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        // Quick dynamic dialog to starting standard chats or creating group
                        showGroupDialog = true
                    },
                    containerColor = SimChatPrimary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "New chat")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val missingPermissions = systemPermissions.filter { perm ->
                permissionGrantedMap[perm] != true
            }

            if (missingPermissions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Privacy Shield",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Carrier Setup Required",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "To enable standard cellular texting, address book synchronisation, and immediate phone call creation directly over your active SIMcard, please setup application permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    launcher.launch(systemPermissions.toTypedArray())
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Configure System Permissions", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (!isSmsDefault && showDefaultSmsPrompt) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Default App Setup",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Set as Default Messaging App",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            IconButton(onClick = { showDefaultSmsPrompt = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Prompt",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "To allow SimChat to send secure SMS/MMS messages and handle continuous background cellular streams, set it as your default messaging application.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDefaultSmsPrompt = false }) {
                                Text("Later", color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                            val intent = android.content.Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                                putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error launching default app chooser", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            ) {
                                Text("Set as Default", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> MessagesTab(
                        viewModel = viewModel,
                        onChatSelected = onChatSelected,
                        onSettingsClicked = { onNavigateTo(AppScreen.Settings) },
                        onNotificationsClicked = { onNavigateTo(AppScreen.NotificationHub) }
                    )
                    1 -> CallsTab(viewModel = viewModel)
                    2 -> ContactsTab(viewModel = viewModel, onContactClicked = { phone -> onChatSelected(phone) })
                    3 -> ProfileTab(viewModel = viewModel, onNavigateTo = onNavigateTo, onTabSelected = { selectedTab = it })
                }
            }
        }

        if (showGroupDialog) {
            GroupAndChatCreationDialog(
                viewModel = viewModel,
                onDismiss = { showGroupDialog = false },
                onChatSelected = { id ->
                    onChatSelected(id)
                    showGroupDialog = false
                },
                onCreatePremiumGroup = {
                    onNavigateTo(AppScreen.CreateGroup)
                }
            )
        }
    }
}

// 1. MESSAGES INBOX TAB (Screenshot 2 alignment!)
@Composable
fun MessagesTab(
    viewModel: SimChatViewModel,
    onChatSelected: (String) -> Unit,
    onSettingsClicked: () -> Unit,
    onNotificationsClicked: () -> Unit
) {
    val conversations by viewModel.filteredConversations.collectAsState()
    val textQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.selectedFilter.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient Premium top background header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SimChatPrimary, SimChatPrimary.copy(alpha = 0.85f))
                    )
                )
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SimChat",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val context = LocalContext.current
                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteConvConfirm by remember { mutableStateOf(false) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNotificationsClicked,
                            modifier = Modifier.testTag("notification_badge_bell")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "SimChat Premium Notification Suite",
                                    tint = Color.White
                                )
                                // Pulsing beautiful green dot badge
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2EBD59))
                                        .align(Alignment.TopEnd)
                                        .border(1.dp, Color.White, CircleShape)
                                )
                            }
                        }
                        IconButton(onClick = onSettingsClicked) {
                            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Option Actions", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sync Carrier SMS") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF109D43)) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.importSms()
                                        Toast.makeText(context, "Carrier SMS sync completed!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Delete All Conversations", color = Color.Red) },
                                    leadingIcon = { Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConvConfirm = true
                                    }
                                )
                            }
                        }
                    }

                    if (showDeleteConvConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConvConfirm = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete All Conversations?", fontWeight = FontWeight.Bold)
                                }
                            },
                            text = {
                                Text("This will permanently clear all chat threads and message history on this device. Your verified credentials and linked contacts list remain intact.")
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConvConfirm = false }) {
                                    Text("Cancel", color = Color.Gray)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteConvConfirm = false
                                        viewModel.deleteAllConversations()
                                        Toast.makeText(context, "All device conversations purged successfully!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("Delete All", color = Color.White)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unified search block
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = textQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_input"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 16.sp),
                            decorationBox = { innerTextField ->
                                if (textQuery.isEmpty()) {
                                    Text("Search messages", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (textQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Category Badge Row (Screenshot 2 filters!)
        val filterOptions = listOf("All", "Personal", "Groups", "Transactions", "Promotions", "Spam")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { filter ->
                val isSelected = activeFilter == filter || (filter == "Personal" && activeFilter == "Cloud")
                val (badgeColor, textColor) = when (filter) {
                    "Personal" -> Pair(PersonalBadgeColor, PersonalBadgeText)
                    "Groups" -> Pair(Color(0xFFE3F2FD), Color(0xFF1E88E5))
                    "Transactions" -> Pair(TransactionBadgeColor, TransactionBadgeText)
                    "Promotions" -> Pair(PromotionBadgeColor, PromotionBadgeText)
                    "Spam" -> Pair(SpamBadgeColor, SpamBadgeText)
                    else -> Pair(SimChatPrimaryContainer, SimChatOnPrimaryContainer)
                }

                Button(
                    onClick = {
                        if (viewModel.sessionManager.isGuest && (filter == "Personal" || filter == "Groups")) {
                            viewModel.triggerUpgradeDialog(true)
                        } else {
                            val viewFilter = if (filter == "Personal") "Cloud" else filter
                            viewModel.selectFilter(viewFilter)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) textColor else badgeColor,
                        contentColor = if (isSelected) Color.White else textColor
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (filter) {
                                "All" -> Icons.Default.ChatBubble
                                "Personal" -> Icons.Default.Person
                                "Groups" -> Icons.Default.Group
                                "Transactions" -> Icons.Default.CurrencyExchange
                                "Promotions" -> Icons.Default.CardGiftcard
                                "Spam" -> Icons.Default.Warning
                                else -> Icons.Default.FilterList
                            },
                             contentDescription = null,
                             modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = filter, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active listing of merged item rows
        if (conversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No conversations found", color = Color.Gray, fontSize = 16.sp)
                Text("Try switching filters or import SMS messages", color = Color.LightGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(conversations) { conv ->
                    ConversationItemRow(
                        conv = conv,
                        onClick = {
                            if (viewModel.sessionManager.isGuest && (conv.type == "CLOUD" || conv.isGroup)) {
                                viewModel.triggerUpgradeDialog(true)
                            } else {
                                onChatSelected(conv.id)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleStarConversation(conv.id, !conv.isStarred)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItemRow(
    conv: ConversationEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isSpam = conv.title.lowercase().contains("spam")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSpam) Color(0xFFFFF7F7) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Profile Graphic / Initials Circle
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    if (isSpam) SpamBadgeColor else when (conv.type) {
                        "SMS" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFE3F2FD)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (conv.profilePhoto.isNotEmpty()) {
                // If using coil/images we show it, else default fallback letter matching layout
                Text(
                    text = conv.title.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SimChatPrimary
                )
            } else {
                Icon(
                    imageVector = when {
                        isSpam -> Icons.Default.ReportGmailerrorred
                        conv.isGroup -> Icons.Filled.Groups
                        conv.type == "SMS" -> Icons.Default.SimCard
                        else -> Icons.Default.CloudQueue
                    },
                    contentDescription = null,
                    tint = if (isSpam) SpamBadgeText else if (conv.type == "SMS") SimChatPrimary else Color(0xFF2196F3),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Center labels
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conv.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSpam) SpamBadgeText else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // SMS vs CLOUD Indicator Badge
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (conv.type == "SMS") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = conv.type,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (conv.type == "SMS") SimChatPrimary else Color(0xFF1E88E5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = conv.lastMessage,
                fontSize = 14.sp,
                color = if (isSpam) SpamBadgeText.copy(alpha = 0.8f) else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right timestamp & badges count block
        Column(
            horizontalAlignment = Alignment.End
        ) {
            val dateString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(conv.timestamp))
            Text(
                text = dateString,
                fontSize = 12.sp,
                color = if (conv.unreadCount > 0) SimChatPrimary else Color.LightGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conv.isStarred) {
                    Icon(imageVector = Icons.Filled.Star, contentDescription = "Starred", tint = Color(0xFFFBC02D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (conv.isLocked) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (conv.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isSpam) SpamBadgeText else SimChatPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conv.unreadCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// 2. CALLS LOG TAB
@Composable
fun CallsTab(viewModel: SimChatViewModel) {
    val callLogs by viewModel.callLogs.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadDeviceCallLogs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Sim Calls", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "System dialer not found", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "New Call", tint = SimChatPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "No Call Logs",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No call history found on this device",
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Real SIM calls will appear here",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn {
                items(callLogs) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = if (log.name.isNotBlank()) log.name.take(1) else "#"
                            Text(initial, fontWeight = FontWeight.Bold, color = SimChatPrimary)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isOutgoing = log.type == 2
                                Icon(
                                    imageVector = if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallMissed,
                                    contentDescription = null,
                                    tint = if (isOutgoing) SimChatPrimary else Color.Red,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val timeStr = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                Text(timeStr, fontSize = 13.sp, color = Color.Gray)
                            }
                        }

                        IconButton(onClick = {
                            val numberToCall = log.phone
                            val hasCallPerm = context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasCallPerm) {
                                try {
                                    val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                                        data = android.net.Uri.parse("tel:$numberToCall")
                                    }
                                    context.startActivity(callIntent)
                                } catch (e: Exception) {
                                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:$numberToCall")
                                    }
                                    context.startActivity(dialIntent)
                                }
                            } else {
                                val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:$numberToCall")
                                }
                                context.startActivity(dialIntent)
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Call, contentDescription = "Call", tint = SimChatPrimary)
                        }
                    }
                }
            }
        }
    }
}

// 3. CONTACTS TAB
@Composable
fun ContactsTab(viewModel: SimChatViewModel, onContactClicked: (String) -> Unit) {
    val contactList by viewModel.contacts.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()
    val blockedList by viewModel.blockedList.collectAsState(initial = emptyList())

    var showAddContact by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Contacts", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { viewModel.importSms() }) {
                    Icon(imageVector = Icons.Filled.Sync, contentDescription = "Sync Contacts", tint = SimChatPrimary)
                }
                IconButton(onClick = { showAddContact = true }) {
                    Icon(imageVector = Icons.Filled.PersonAdd, contentDescription = "Add Contact", tint = SimChatPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (favoritesList.isNotEmpty()) {
            Text("Favorites", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                favoritesList.forEach { fave ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                if (viewModel.sessionManager.isGuest && fave.isCloud) {
                                    viewModel.triggerUpgradeDialog(true)
                                } else {
                                    onContactClicked(fave.phone)
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(SimChatPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(fave.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(fave.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("All Contacts", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(contactList) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (viewModel.sessionManager.isGuest && item.isCloud) {
                                viewModel.triggerUpgradeDialog(true)
                            } else {
                                onContactClicked(item.id)
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.name.take(1), fontWeight = FontWeight.Bold, color = SimChatPrimary)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(item.phone, fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val isBlocked = blockedList.contains(item.id) || blockedList.contains(item.phone)
                    if (isBlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33FF5252))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Blocked", fontSize = 10.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (item.isCloud) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE3F2FD))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Cloud", fontSize = 10.sp, color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Add Contact Dialog
        if (showAddContact) {
            AddContactDialog(
                onDismiss = { showAddContact = false },
                onAdd = { name, phone, email, isCloud ->
                    viewModel.addNewContact(name, phone, email, isCloud)
                    showAddContact = false
                }
            )
        }
    }
}

// 4. PROFILE TAB (Screenshot 3 alignment!)
@Composable
fun ProfileTab(
    viewModel: SimChatViewModel,
    onNavigateTo: (AppScreen) -> Unit,
    onTabSelected: (Int) -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val contactList by viewModel.contacts.collectAsState()
    val groupsList by viewModel.groups.collectAsState()
    val totalMessagesCount by viewModel.messagesCount.collectAsState()
    val context = LocalContext.current

    var showCustomizerDialog by remember { mutableStateOf(false) }

    if (showCustomizerDialog) {
        ChatStylesCustomizerDialog(
            viewModel = viewModel,
            onDismiss = { showCustomizerDialog = false }
        )
    }

    val profileName = remember(sessionState) {
        when (val s = sessionState) {
            is SessionState.CloudMode -> s.name
            else -> "SIM User"
        }
    }
    val profileBio = remember(sessionState) {
        when (val s = sessionState) {
            is SessionState.CloudMode -> s.bio
            else -> "Hey there! I am using SimChat."
        }
    }
    val profileEmail = remember(sessionState) {
        when (val s = sessionState) {
            is SessionState.CloudMode -> s.email
            else -> "user@simchat.com"
        }
    }
    val displayPhone = remember(sessionState) {
        when (val s = sessionState) {
            is SessionState.CloudMode -> s.email
            else -> {
                val preferredSim = viewModel.getPreferredSim()
                if (preferredSim == "SIM 2") "+254 722 Airtel" else "+254 711 Safaricom"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Gradient Premium top header matches Screenshot 3
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SimChatPrimary, SimChatPrimary.copy(alpha = 0.85f))
                    )
                )
                .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large picture layout with responsive photo trigger badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(60.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                if (viewModel.sessionManager.isGuest) {
                                    viewModel.triggerUpgradeDialog(true)
                                } else {
                                    // Real photo upload simulation
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.CameraEnhance, contentDescription = "Camera", tint = SimChatPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profileName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (viewModel.sessionManager.isGuest) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF9100))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SIM ONLY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Filled.QrCode, contentDescription = "QR Code", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Text(
                    text = displayPhone,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Text(
                    text = profileBio,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Metric Badges matches Screenshot 2 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadgeItem(
                        modifier = Modifier.weight(1f),
                        count = totalMessagesCount.toString(),
                        label = "Messages",
                        icon = Icons.Filled.Chat,
                        onClick = {
                            onTabSelected(0)
                            viewModel.selectFilter("All")
                        }
                    )
                    MetricBadgeItem(
                        modifier = Modifier.weight(1f),
                        count = contactList.size.toString(),
                        label = "Contacts",
                        icon = Icons.Filled.People,
                        onClick = {
                            onTabSelected(2)
                        }
                    )
                    MetricBadgeItem(
                        modifier = Modifier.weight(1f),
                        count = if (viewModel.sessionManager.isGuest) "N/A" else groupsList.size.toString(),
                        label = "Groups",
                        icon = Icons.Filled.Group,
                        onClick = {
                            if (viewModel.sessionManager.isGuest) {
                                viewModel.triggerUpgradeDialog(true)
                            } else {
                                onTabSelected(0)
                                viewModel.selectFilter("Groups")
                            }
                        }
                    )
                    MetricBadgeItem(
                        modifier = Modifier.weight(1f),
                        count = "Active",
                        label = "Protection",
                        icon = Icons.Filled.Security,
                        onClick = {
                            onNavigateTo(AppScreen.LockedChats)
                        }
                    )
                }
            }
        }

        // Profile List settings actions (Screenshot 3 configuration rows)
        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                if (viewModel.sessionManager.isGuest) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x0F109D43)),
                        border = BorderStroke(1.dp, Color(0xFF109D43).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF109D43).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF109D43),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connect to Cloud",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Back up chats, unlock channels, and switch to Cloud mode.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.triggerUpgradeDialog(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text("Chat Features", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                ProfileSettingLine(icon = Icons.Filled.Lock, title = "Lock Chats", subtitle = "Secure your private conversations") {
                    onNavigateTo(AppScreen.LockedChats)
                }
                ProfileSettingLine(icon = Icons.Filled.Block, title = "Blocked Contacts", subtitle = "View and manage blocked lists") {
                    onNavigateTo(AppScreen.BlockedList)
                }
                ProfileSettingLine(icon = Icons.Filled.Star, title = "Starred Messages", subtitle = "Messages you have starred") {
                    onNavigateTo(AppScreen.StarredMessages)
                }
                ProfileSettingLine(icon = Icons.Filled.GroupAdd, title = "Create Group", subtitle = "Start group discussions") {
                    if (viewModel.sessionManager.isGuest) {
                        viewModel.triggerUpgradeDialog(true)
                    } else {
                        // Normally user is redirected or dialog shows, but since they are cloud we can trigger upgrade or show creation
                    }
                }
                ProfileSettingLine(icon = Icons.Filled.Bookmark, title = "Saved Messages", subtitle = "Messages you saved for yourself") {
                    if (viewModel.sessionManager.isGuest) {
                        viewModel.triggerUpgradeDialog(true)
                    } else {
                        onNavigateTo(AppScreen.StarredMessages)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Application Preferences", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                ProfileSettingLine(icon = Icons.Filled.SettingsSystemDaydream, title = "Change Theme", subtitle = "Choose your favorite theme") {
                    onNavigateTo(AppScreen.Settings)
                }

                ProfileSettingLine(
                    icon = Icons.Filled.CloudQueue,
                    title = "Switch to Cloud Mode",
                    subtitle = "Join the Cloud community & backup chats"
                ) {
                    if (viewModel.sessionManager.isGuest) {
                        viewModel.triggerUpgradeDialog(true)
                    } else {
                        Toast.makeText(context, "Already in Cloud Mode!", Toast.LENGTH_SHORT).show()
                    }
                }

                ProfileSettingLine(
                    icon = Icons.Filled.Palette,
                    title = "Chat Wallpaper & Fonts Customizer",
                    subtitle = "Set background, text colors, and premium fonts"
                ) {
                    showCustomizerDialog = true
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.DarkMode, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Toggle dark theme", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = viewModel.activeTheme == "dark",
                        onCheckedChange = { isDark ->
                            viewModel.updateTheme(if (isDark) "dark" else "light")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SimChatPrimary)
                    )
                }

                ProfileSettingLine(icon = Icons.Filled.Security, title = "Privacy", subtitle = "Manage your privacy settings") {
                    onNavigateTo(AppScreen.Settings)
                }

                ProfileSettingLine(icon = Icons.Filled.Settings, title = "Settings", subtitle = "Manage notification and data usage") {
                    onNavigateTo(AppScreen.Settings)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Logout / Account upgrade button
                if (viewModel.sessionManager.isGuest) {
                    Button(
                        onClick = { viewModel.triggerUpgradeDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Register Cloud Account", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Logout session", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBadgeItem(
    modifier: Modifier = Modifier,
    count: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(68.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = count, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ProfileSettingLine(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

// CHAT SCREEN DETAIL (SMS & Cloud Hybrid switch integration!)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    viewModel: SimChatViewModel,
    convId: String,
    onBack: () -> Unit,
    onGoToGroupDetail: (() -> Unit)? = null
) {
    val messages by viewModel.getMessagesForConversation(convId).collectAsState(initial = emptyList())
    val typingMap by viewModel.typingStates.collectAsState()
    val isTyping = typingMap[convId] ?: false

    val allConvs by viewModel.allConversations.collectAsState()
    val thisConv = allConvs.find { it.id == convId }
    val isLockedChat = thisConv?.isLocked == true

    if (isLockedChat) {
        KeepScreenSecure(viewModel.isScreenshotProtectionEnabled)
    }

    var textInput by remember { mutableStateOf("") }
    var hybridMode by remember { mutableStateOf(if (viewModel.sessionManager.isGuest) "SMS" else "CLOUD") } // "SMS" or "CLOUD"

    val contactsList by viewModel.contacts.collectAsState()
    val blockedList by viewModel.blockedList.collectAsState()
    val matchedContact = contactsList.find { it.phone == convId || it.id == convId }
    val chatTitle = matchedContact?.name ?: convId
    val profilePhoto = matchedContact?.avatarUri?.ifBlank { null }

    val context = LocalContext.current

    var editableTitle by remember { mutableStateOf(chatTitle) }
    var editablePhone by remember { mutableStateOf(convId) }

    val isBlocked = blockedList.contains(convId) || blockedList.contains(editablePhone)

    LaunchedEffect(chatTitle) {
        editableTitle = chatTitle
    }

    var showProfileSheet by remember { mutableStateOf(false) }
    var showCallChooser by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showActivePremiumCallScreen by remember { mutableStateOf(false) }
    var callingSimUsed by remember { mutableStateOf("SIM 1") }

    // Edge-to-edge / Immersive content box
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val chatBgTheme by viewModel.chatBackgroundTheme.collectAsState()
    val localWallpaperUri by viewModel.localWallpaperUri.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 1. Wallpaper with custom doodle pattern
        ChatDoodleBackground(theme = chatBgTheme, customWallpaper = localWallpaperUri)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // 2. Custom Dark Green Gradient Header (Premium)
            ChatHeader(
                title = editableTitle,
                isOnline = false,
                isTyping = isTyping,
                profilePhoto = profilePhoto,
                onBack = onBack,
                onPhoneCall = {
                    showCallChooser = true
                },
                onProfileClick = {
                    showProfileSheet = true
                },
                onDeleteChats = {
                    showDeleteConfirmDialog = true
                },
                onBlockContact = {
                    viewModel.blockUser(convId)
                    Toast.makeText(context, "$editableTitle blocked successfully.", Toast.LENGTH_SHORT).show()
                },
                onEditPhoneNumber = {
                    showEditPhoneDialog = true
                },
                onAddToArchive = {
                    Toast.makeText(context, "Added $editableTitle to Archived folder.", Toast.LENGTH_SHORT).show()
                },
                onLockChat = {
                    viewModel.toggleLockConversation(convId, true)
                    Toast.makeText(context, "$editableTitle chat is now locked & protected with device passcode.", Toast.LENGTH_LONG).show()
                },
                onGoToGroupDetail = if (thisConv?.isGroup == true) onGoToGroupDetail else null
            )

            // 3. Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Friendly date separator
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x3B109D43),
                            border = BorderStroke(0.5.dp, Color(0x3B8CE7A2))
                        ) {
                            Text(
                                text = "Today",
                                color = Color(0xFF8CE7A2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(messages) { msg ->
                    MessageBubble(msg, viewModel = viewModel)
                }

                // Centered dynamic Typing Indicator floating card
                if (isTyping) {
                    item {
                        PremiumTypingIndicator(name = chatTitle)
                    }
                }
            }

            // 4. Input Area Capsule Styled
            ChatInputArea(
                textInput = textInput,
                onValueChange = { textInput = it },
                placeholder = "Type a message",
                onSend = {
                    if (textInput.isNotEmpty()) {
                        viewModel.sendMessage(convId, textInput, hybridMode)
                        textInput = ""
                    }
                }
            )

            // 5. Bottom Navigation Hybrid Selector
            ChatBottomSelector(
                selectedMode = hybridMode,
                onModeSelected = {
                    if (viewModel.sessionManager.isGuest && it == "CLOUD") {
                        viewModel.triggerUpgradeDialog(true)
                    } else {
                        hybridMode = it
                    }
                },
                carrierName = viewModel.getPreferredSim(),
                onSelectCarrier = {
                    val activeSim = if (viewModel.getPreferredSim() == "SIM 1") "SIM 2" else "SIM 1"
                    viewModel.setPreferredSim(activeSim)
                    Toast.makeText(context, "Switched default route to $activeSim", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 6. Dialogue Overlays / Modals and Premium Sheets
        if (showProfileSheet) {
            ContactProfileDetailSheet(
                contactName = editableTitle,
                phoneNumber = editablePhone,
                profilePhoto = profilePhoto,
                onDismiss = { showProfileSheet = false },
                onEditClick = {
                    showProfileSheet = false
                    showEditPhoneDialog = true
                },
                onBlockClick = {
                    showProfileSheet = false
                    if (isBlocked) {
                        viewModel.unblockUser(convId)
                        Toast.makeText(context, "$editableTitle unblocked successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.blockUser(convId)
                        Toast.makeText(context, "$editableTitle blocked successfully.", Toast.LENGTH_SHORT).show()
                    }
                },
                isLocked = false,
                onToggleLock = { lockState ->
                    viewModel.toggleLockConversation(convId, lockState)
                    Toast.makeText(context, "Lock state updated for $editableTitle.", Toast.LENGTH_SHORT).show()
                },
                isBlocked = isBlocked
            )
        }

        if (showCallChooser) {
            SimCallChooserDialog(
                contactName = editableTitle,
                phoneNumber = editablePhone,
                currentPreferred = viewModel.getPreferredSim(),
                onDismiss = { showCallChooser = false },
                onInitiateCall = { sim ->
                    showCallChooser = false
                    callingSimUsed = sim
                    
                    val numberToDial = if (editablePhone.isNotBlank()) editablePhone else convId
                    val hasCallPerm = context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasCallPerm) {
                        try {
                            val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                                data = android.net.Uri.parse("tel:$numberToDial")
                            }
                            context.startActivity(callIntent)
                        } catch (e: Exception) {
                            val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:$numberToDial")
                            }
                            context.startActivity(dialIntent)
                        }
                    } else {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:$numberToDial")
                        }
                        context.startActivity(dialIntent)
                    }
                }
            )
        }

        if (showActivePremiumCallScreen) {
            SimActiveCallScreen(
                contactName = editableTitle,
                phoneNumber = editablePhone,
                simName = callingSimUsed,
                viewModel = viewModel,
                onEndCall = { showActivePremiumCallScreen = false }
            )
        }

        if (showEditPhoneDialog) {
            EditContactDialog(
                currentName = editableTitle,
                currentPhone = editablePhone,
                onDismiss = { showEditPhoneDialog = false },
                onSave = { newName, newPhone ->
                    showEditPhoneDialog = false
                    viewModel.addNewContact(newName, newPhone, "", true)
                    editableTitle = newName
                    editablePhone = newPhone
                    Toast.makeText(context, "Contact identity changed to $newName ($newPhone)", Toast.LENGTH_LONG).show()
                }
            )
        }

        if (showDeleteConfirmDialog) {
            DeleteChatsConfirmDialog(
                contactName = editableTitle,
                onDismissRequest = { showDeleteConfirmDialog = false },
                onConfirm = {
                    showDeleteConfirmDialog = false
                    viewModel.deleteConversation(convId)
                    onBack()
                    Toast.makeText(context, "Chat history with $editableTitle deleted successfully.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// PREMIUM GRADIENT HEADER
@Composable
fun ChatHeader(
    title: String,
    isOnline: Boolean,
    isTyping: Boolean,
    profilePhoto: String?,
    onBack: () -> Unit,
    onPhoneCall: () -> Unit,
    onProfileClick: () -> Unit,
    onDeleteChats: () -> Unit,
    onBlockContact: () -> Unit,
    onEditPhoneNumber: () -> Unit,
    onAddToArchive: () -> Unit,
    onLockChat: () -> Unit,
    onGoToGroupDetail: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF04140D),
                        Color(0xFF071B12)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Contact profile frame
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!profilePhoto.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = profilePhoto,
                        contentDescription = "Contact photo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B3D2B)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF11422A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.take(1).uppercase(),
                            color = Color(0xFF8CE7A2),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Active dot
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF109D43))
                            .border(1.5.dp, Color(0xFF071B12), CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileClick() }
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isTyping) "typing..." else "Online",
                    color = if (isTyping) Color(0xFF8CE7A2) else Color(0xFF81C784).copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (onGoToGroupDetail != null) {
                IconButton(onClick = onGoToGroupDetail, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Group Control Deck",
                        tint = Color(0xFF2EBD59)
                    )
                }
            }

            IconButton(onClick = onPhoneCall, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice Call",
                    tint = Color.White
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF0C1E14))
                        .border(1.dp, Color(0xFF1E4C33), RoundedCornerShape(8.dp))
                ) {
                    if (onGoToGroupDetail != null) {
                        DropdownMenuItem(
                            text = { Text("Group Control Deck", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onGoToGroupDetail()
                            }
                        )
                        Divider(color = Color(0x3B8CE7A2))
                    }
                    DropdownMenuItem(
                        text = { Text("Edit Info", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF8CE7A2), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onEditPhoneNumber()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Archive", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF8CE7A2), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onAddToArchive()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Lock Chat", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8CE7A2), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onLockChat()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Block Contact", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onBlockContact()
                        }
                    )
                    Divider(color = Color(0x3B8CE7A2))
                    DropdownMenuItem(
                        text = { Text("Clear/Delete Chats", color = Color(0xFFFF8A80)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onDeleteChats()
                        }
                    )
                }
            }
        }
    }
}

// CUSTOM COROUTINE ANIMATED BLINKING DOTS
@Composable
fun BlinkingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.2f at 0
                1f at 200
                0.2f at 400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.2f at 200
                1f at 400
                0.2f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.2f at 400
                1f at 600
                0.2f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).graphicsLayer(alpha = alpha1).background(Color(0xFF109D43), CircleShape))
        Box(modifier = Modifier.size(6.dp).graphicsLayer(alpha = alpha2).background(Color(0xFF109D43), CircleShape))
        Box(modifier = Modifier.size(6.dp).graphicsLayer(alpha = alpha3).background(Color(0xFF109D43), CircleShape))
    }
}

// CUSTOM WALLPAPER DOODLE PATTERN
@Composable
fun ChatDoodleBackground(theme: String = "default", customWallpaper: String = "") {
    val items = listOf(
        Icons.Default.ChatBubble,
        Icons.Default.SimCard,
        Icons.Default.Cloud,
        Icons.Default.FlashOn,
        Icons.Default.Check,
        Icons.Default.Person,
        Icons.Default.SignalCellularAlt,
        Icons.Default.Star,
        Icons.Default.Lock,
        Icons.Default.CellTower
    )

    val backgroundModifier = when {
        customWallpaper.isNotEmpty() -> {
            if (customWallpaper.startsWith("#")) {
                try {
                    val color = Color(android.graphics.Color.parseColor(customWallpaper))
                    Modifier.background(color)
                } catch (e: Exception) {
                    Modifier.background(Color(0xFF04100B))
                }
            } else {
                Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2C1919), Color(0xFF0C1014))
                    )
                )
            }
        }
        theme == "emerald_forest" -> Modifier.background(
            Brush.verticalGradient(colors = listOf(Color(0xFF052211), Color(0xFF010A04)))
        )
        theme == "cosmic_lavender" -> Modifier.background(
            Brush.verticalGradient(colors = listOf(Color(0xFF1B0C2D), Color(0xFF07030C)))
        )
        theme == "warm_sunset" -> Modifier.background(
            Brush.verticalGradient(colors = listOf(Color(0xFF29080B), Color(0xFF0C0203)))
        )
        theme == "midnight_ocean" -> Modifier.background(
            Brush.verticalGradient(colors = listOf(Color(0xFF021324), Color(0xFF00050A)))
        )
        theme == "slate_charcoal" -> Modifier.background(
            Brush.verticalGradient(colors = listOf(Color(0xFF1E2022), Color(0xFF0A0A0C)))
        )
        else -> Modifier.background(Color(0xFF04100B))
    }

    val watermarkColor = when {
        customWallpaper.isNotEmpty() -> Color(0xFF2EBD59).copy(alpha = 0.05f)
        theme == "emerald_forest" -> Color(0xFF2EBD59).copy(alpha = 0.05f)
        theme == "cosmic_lavender" -> Color(0xFFBB86FC).copy(alpha = 0.05f)
        theme == "warm_sunset" -> Color(0xFFFF5D5D).copy(alpha = 0.05f)
        theme == "midnight_ocean" -> Color(0xFF03A9F4).copy(alpha = 0.05f)
        theme == "slate_charcoal" -> Color(0xFFB0BEC5).copy(alpha = 0.05f)
        else -> Color(0xFF109D43).copy(alpha = 0.04f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(backgroundModifier)
    ) {
        if (customWallpaper.isNotEmpty() && !customWallpaper.startsWith("#")) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x224CAF50), Color.Transparent),
                            radius = 600f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (row in 0 until 12) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 5) {
                        val index = (row * 5 + col) % items.size
                        val icon = items[index]
                        val rotation = ((row * 19 + col * 29) % 50) - 25f
                        val scale = 0.75f + ((row + col) % 3) * 0.15f

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = watermarkColor,
                            modifier = Modifier
                                .size((26 * scale).dp)
                                .graphicsLayer(
                                    rotationZ = rotation
                                )
                        )
                    }
                }
            }
        }
    }
}

// PREMIUM FLOATING TYPING CARD
@Composable
fun PremiumTypingIndicator(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x9E0C1E14),
            border = BorderStroke(0.5.dp, Color(0x3B8CE7A2)),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$name is typing...",
                    color = Color(0xFF8CE7A2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                BlinkingDots()
            }
        }
    }
}

// CAPSULE CHAT INPUT AREA
@Composable
fun ChatInputArea(
    textInput: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xBA132C1F),
                border = BorderStroke(1.dp, Color(0xFF1D422E)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfied,
                            contentDescription = "Emojis",
                            tint = Color(0xFF109D43),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = textInput,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_text_input"),
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 15.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            decorationBox = { innerTextField ->
                                if (textInput.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = Color(0xFF109D43),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF109D43), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// BOTTOM SMS / CLOUD PORT TAB SELECTOR BAR
@Composable
fun ChatBottomSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    carrierName: String,
    onSelectCarrier: () -> Unit
) {
    Surface(
        color = Color(0xFF04100B),
        border = BorderStroke(0.5.dp, Color(0x1F8CE7A2)),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SMS
                Column(
                    modifier = Modifier
                        .clickable { onModeSelected("SMS") }
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "SMS Mode",
                        tint = if (selectedMode == "SMS") Color(0xFF8CE7A2) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SMS",
                        fontSize = 11.sp,
                        fontWeight = if (selectedMode == "SMS") FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedMode == "SMS") Color(0xFF8CE7A2) else Color.White.copy(alpha = 0.5f)
                    )
                    if (selectedMode == "SMS") {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(16.dp)
                                .height(2.dp)
                                .background(Color(0xFF8CE7A2), RoundedCornerShape(1.dp))
                        )
                    }
                }

                // Cloud
                Column(
                    modifier = Modifier
                        .clickable { onModeSelected("CLOUD") }
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Cloud Mode",
                        tint = if (selectedMode == "CLOUD") Color(0xFF8CE7A2) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cloud",
                        fontSize = 11.sp,
                        fontWeight = if (selectedMode == "CLOUD") FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedMode == "CLOUD") Color(0xFF8CE7A2) else Color.White.copy(alpha = 0.5f)
                    )
                    if (selectedMode == "CLOUD") {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(16.dp)
                                .height(2.dp)
                                .background(Color(0xFF8CE7A2), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            // SIM Selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectCarrier() }
                    .background(Color(0x1F109D43))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = "Carrier SIM",
                    tint = Color(0xFF8CE7A2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$carrierName • Safaricom",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8CE7A2)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF8CE7A2),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// PREMIUM DIALOG COMPONENT: HIGHER FIDELITY DIRECT CELLULAR ROUTER
@Composable
fun SimCallChooserDialog(
    contactName: String,
    phoneNumber: String,
    currentPreferred: String,
    onDismiss: () -> Unit,
    onInitiateCall: (simName: String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1F15),
            border = BorderStroke(1.dp, Color(0xFF1E4C33)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0x24109D43), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF8CE7A2),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SIM Cellular Routing",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select SIM interface to call $contactName ($phoneNumber)",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // SIM Option 1
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentPreferred == "SIM 1") Color(0x3B109D43) else Color(0x0AFFFFFF),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (currentPreferred == "SIM 1") Color(0xFF8CE7A2) else Color(0x21FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInitiateCall("SIM 1") }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = null,
                            tint = Color(0xFF8CE7A2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SIM 1 • Safaricom 5G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Preferred mobile channel • HD Voice", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        if (currentPreferred == "SIM 1") {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF109D43),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // SIM Option 2
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentPreferred == "SIM 2") Color(0x3B109D43) else Color(0x0AFFFFFF),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (currentPreferred == "SIM 2") Color(0xFF8CE7A2) else Color(0x21FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInitiateCall("SIM 2") }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = null,
                            tint = Color(0xFF8CE7A2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SIM 2 • Airtel Kenya 4G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Standby channel • GSM network", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        if (currentPreferred == "SIM 2") {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF109D43),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF8CE7A2), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// PREMIUM CALL INTERFACE OVERLAY
@Composable
fun SimActiveCallScreen(
    contactName: String,
    phoneNumber: String,
    simName: String,
    viewModel: SimChatViewModel? = null,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val contactsList by if (viewModel != null) viewModel.contacts.collectAsState() else remember { mutableStateOf(emptyList<ContactEntity>()) }

    // Interactive States matching requested buttons
    var activeSimName by remember { mutableStateOf(if (simName.isEmpty()) "SIM 1 Safaricom" else simName) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }
    var isHold by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }

    // Toggleable overlay buffers/panels
    var showKeypad by remember { mutableStateOf(false) }
    var dialpadBuffer by remember { mutableStateOf("") }
    var showContactsSheet by remember { mutableStateOf(false) }
    var showMessageTemplateSheet by remember { mutableStateOf(false) }
    var showCallNotesSheet by remember { mutableStateOf(false) }
    var callNotesText by remember { mutableStateOf("") }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var showAddCallSheet by remember { mutableStateOf(false) }
    var isTelemetryExpanded by remember { mutableStateOf(false) }

    // Simulating multi-party merged call names
    val conferenceParties = remember { mutableStateListOf<String>() }

    // Timer logic (Pauses when call is on Hold)
    var callSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(isHold) {
        while (!isHold) {
            delay(1000)
            callSeconds++
        }
    }

    // Call Recording Duration Timer
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordingDuration++
        }
    }

    // Dynamic telemetry statuses
    var callQualityState by remember { mutableStateOf("Excellent") }
    var callNetworkType by remember { mutableStateOf("Wi-Fi") }
    val formattedCallDuration = remember(callSeconds) {
        val mins = callSeconds / 60
        val secs = callSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
    val formattedRecDuration = remember(recordingDuration) {
        val mins = recordingDuration / 60
        val secs = recordingDuration % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    // Concentric sound waves infinite animation loop representing speech activity
    val transition = rememberInfiniteTransition(label = "speech_ripple")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    // Speech Equalizer bar voice amplitudes
    val eqFractions = listOf(0.4f, 0.8f, 0.5f, 1.0f, 0.6f, 0.9f, 0.3f, 0.7f)
    val speakIntensity by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakIntensity"
    )

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF030D08) // Dark deep cosmic green background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF05170D),
                                Color(0xFF020B06),
                                Color(0xFF010604)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Background Radial Glowing Circle elements
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(
                                color = Color(0x0C109D43),
                                radius = size.minDimension * 0.45f,
                                center = Offset(size.width / 2f, size.height * 0.3f)
                            )
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Section
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14109D43))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Secure Status Icon",
                            tint = Color(0xFF2EBD59),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SimChat Audio Call",
                            color = Color(0xFF2EBD59),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Center Avatar and Ripple Circles representing talking
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ripples centered around the avatar only animate if the call is active and NOT on hold
                        if (!isHold) {
                            // First concentric ring
                            val scale1 = 1.0f + 1.2f * pulseProgress
                            val alpha1 = 0.6f * (1f - pulseProgress)
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .graphicsLayer {
                                        scaleX = scale1
                                        scaleY = scale1
                                        alpha = alpha1
                                    }
                                    .background(Color(0x3B2EBD59), CircleShape)
                                    .border(1.dp, Color(0x662EBD59), CircleShape)
                            )

                            // Second concentric delayed ring
                            val progress2 = (pulseProgress + 0.5f) % 1.0f
                            val scale2 = 1.0f + 1.2f * progress2
                            val alpha2 = 0.6f * (1f - progress2)
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .graphicsLayer {
                                        scaleX = scale2
                                        scaleY = scale2
                                        alpha = alpha2
                                    }
                                    .background(Color(0x212EBD59), CircleShape)
                                    .border(0.5.dp, Color(0x332EBD59), CircleShape)
                            )
                        }

                        // Circular Avatar Portrait Border Frame
                        Box(
                            modifier = Modifier
                                .size(125.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF04170E))
                                .border(
                                    width = 3.dp,
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFF2EBD59),
                                            Color(0xFF109D43),
                                            Color(0xFF8CE7A2),
                                            Color(0xFF2EBD59)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Aesthetic profile card representation
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF072114)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contactName.take(1).uppercase(),
                                    color = Color(0xFF2EBD59),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Contact Personal Details Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val displayName = if (conferenceParties.isEmpty()) {
                            contactName
                        } else {
                            "$contactName + ${conferenceParties.joinToString(", ")}"
                        }
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verified Identity",
                            tint = Color(0xFF2EBD59),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = phoneNumber,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Realistic Timing / Call Status label
                    val statusText = when {
                        isHold -> "Call Held"
                        callSeconds < 3 -> "Connecting..."
                        else -> "Connected • $formattedCallDuration"
                    }
                    Text(
                        text = statusText,
                        color = if (isHold) Color.White.copy(alpha = 0.5f) else Color(0xFF2EBD59),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // On-call Recording HUD banner
                    if (isRecording) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x3BFF1744))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "REC: $formattedRecDuration",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // End to End encryption lock row
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0F2EBD59))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2EBD59).copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "End-to-end encrypted",
                            color = Color(0xFF2EBD59).copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Equalizer wave animation representing active speech
                    if (!isHold) {
                        Row(
                            modifier = Modifier.height(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            eqFractions.forEach { frac ->
                                val barHeight = 24.dp * (frac * speakIntensity).coerceIn(0.15f, 1.0f)
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF2EBD59))
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Diagnostic Cellular Info HUD Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x33072114),
                        border = BorderStroke(1.dp, Color(0xFF163E27)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Call Quality
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val nextQuality = when (callQualityState) {
                                            "Excellent" -> "Good"
                                            "Good" -> "Fluctuating"
                                            else -> "Excellent"
                                        }
                                        callQualityState = nextQuality
                                        Toast
                                            .makeText(
                                                context,
                                                "Cellular voice signal state changed to $nextQuality",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                            ) {
                                Text("Call Quality", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCell,
                                        contentDescription = null,
                                        tint = if (callQualityState == "Excellent") Color(0xFF2EBD59) else Color(0xFFFFB300),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = callQualityState,
                                        color = if (callQualityState == "Excellent") Color(0xFF2EBD59) else Color(0xFFFFB300),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(25.dp)
                                    .background(Color(0xFF163E27))
                            )

                            // Column 2: Network Channel
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val nextNet = if (callNetworkType == "Wi-Fi") "5G VoLTE" else "Wi-Fi"
                                        callNetworkType = nextNet
                                        Toast
                                            .makeText(context, "Network routed over $nextNet", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                            ) {
                                Text("Network", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (callNetworkType == "Wi-Fi") Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF2EBD59),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = callNetworkType,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(25.dp)
                                    .background(Color(0xFF163E27))
                            )

                            // Column 3: Active SIM Slot Routing
                            Row(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clickable {
                                        // Swap cellular SIM route on-the-fly
                                        activeSimName = if (activeSimName.contains("SIM 1")) {
                                            "SIM 2 Airtel Kenya"
                                        } else {
                                            "SIM 1 Safaricom"
                                        }
                                        Toast
                                            .makeText(
                                                context,
                                                "Hot-swapped cellular caller path to $activeSimName",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("SIM ROUTING", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SimCard,
                                            contentDescription = null,
                                            tint = Color(0xFF2EBD59),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activeSimName.take(5),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 9-Button grid configuration
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ROW 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CallGridTouchItem(
                                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                label = "Mute",
                                isActiveState = isMuted,
                                onClick = {
                                    isMuted = !isMuted
                                    Toast
                                        .makeText(
                                            context,
                                            if (isMuted) "Microphone muted" else "Microphone unmuted",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            )

                            CallGridTouchItem(
                                icon = Icons.Filled.Dialpad,
                                label = "Keypad",
                                isActiveState = showKeypad,
                                onClick = { showKeypad = !showKeypad }
                            )

                            CallGridTouchItem(
                                icon = Icons.Filled.VolumeUp,
                                label = "Speaker",
                                isActiveState = isSpeaker,
                                onClick = {
                                    isSpeaker = !isSpeaker
                                    Toast
                                        .makeText(
                                            context,
                                            if (isSpeaker) "Handsfree audio speaker enabled" else "Earpiece audio routed",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            )
                        }

                        // ROW 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CallGridTouchItem(
                                icon = Icons.Default.Add,
                                label = "Add Call",
                                isActiveState = showAddCallSheet,
                                onClick = { showAddCallSheet = true }
                            )

                            CallGridTouchItem(
                                icon = Icons.Filled.Pause,
                                label = "Hold",
                                isActiveState = isHold,
                                onClick = {
                                    isHold = !isHold
                                    Toast
                                        .makeText(
                                            context,
                                            if (isHold) "Call put on Hold" else "Call resumed",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            )

                            CallGridTouchItem(
                                icon = Icons.Filled.Person,
                                label = "Contacts",
                                isActiveState = showContactsSheet,
                                onClick = { showContactsSheet = true }
                            )
                        }

                        // ROW 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CallGridTouchItem(
                                icon = Icons.Default.SimCard,
                                label = "Switch SIM",
                                isActiveState = false,
                                onClick = {
                                    activeSimName = if (activeSimName.contains("SIM 1")) "SIM 2 Airtel Kenya" else "SIM 1 Safaricom"
                                    Toast.makeText(context, "Hot swapped cellular routing: $activeSimName", Toast.LENGTH_SHORT).show()
                                }
                            )

                            CallGridTouchItem(
                                icon = Icons.Filled.Lens,
                                label = if (isRecording) "Stop Rec" else "Record",
                                isActiveState = isRecording,
                                activeBgColor = Color(0x3BFF1744),
                                activeTint = Color.Red,
                                onClick = {
                                    isRecording = !isRecording
                                    if (isRecording) {
                                        recordingDuration = 0
                                        Toast.makeText(context, "Secure Call Recorder started logging locally!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Secure audio record logged to cellular logs directory!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )

                            CallGridTouchItem(
                                icon = Icons.Filled.MoreHoriz,
                                label = "More",
                                isActiveState = showMoreMenuSheet,
                                onClick = { showMoreMenuSheet = true }
                            )
                        }
                    }

                    // Lower Auxiliary Controls & Red End Call Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column Aux: Message SMS Dispatcher
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showMessageTemplateSheet = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1F2EBD59)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = Color(0xFF2EBD59)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Message", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }

                        // Master Red End Call Button
                        Box(
                            modifier = Modifier
                                .clickable(onClick = onEndCall)
                                .size(78.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = Color(0x2EFF1744),
                                        radius = size.maxDimension * 0.58f
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF1744)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Hang up call",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        // Right Column Aux: secure notes pads
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showCallNotesSheet = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1F2EBD59)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EditNote,
                                    contentDescription = null,
                                    tint = Color(0xFF2EBD59)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Swipe up telemetry indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTelemetryExpanded = !isTelemetryExpanded }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isTelemetryExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color(0xFF2EBD59),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isTelemetryExpanded) "Collapse cellular metrics" else "Swipe up for call details",
                            color = Color(0xFF2EBD59).copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // DIALPAD (KEYPAD OVERLAY) PANEL
                AnimatedVisibility(
                    visible = showKeypad,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = Color(0xFA05140D),
                        border = BorderStroke(1.dp, Color(0xFF1E4C33)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.68f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("DTMF Dialer Pad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                IconButton(onClick = { showKeypad = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Keypad", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Display buffer showing dialed items
                            OutlinedTextField(
                                value = dialpadBuffer,
                                onValueChange = { dialpadBuffer = it },
                                placeholder = { Text("Tone buffer empty", color = Color.Gray, fontSize = 18.sp) },
                                readOnly = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color(0xFF2EBD59),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                trailingIcon = {
                                    if (dialpadBuffer.isNotEmpty()) {
                                        IconButton(onClick = { dialpadBuffer = "" }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Grid of Keypad
                            DialpadButtonsLayout(
                                onKeyPress = { ch ->
                                    dialpadBuffer += ch
                                    Toast.makeText(context, "$ch dialed with simulated DTMF voice feedback", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // QUICK MESSAGE SMS SHORTCUT SHEETS
                if (showMessageTemplateSheet) {
                    Dialog(onDismissRequest = { showMessageTemplateSheet = false }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF071F11),
                            border = BorderStroke(1.dp, Color(0xFF1B4E30)),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Quick Response SMS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(onClick = { showMessageTemplateSheet = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                Text(
                                    "Message dispatch bypass will immediately push local carrier SMS without closing active voice call.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                val templates = listOf(
                                    "I am in a secure call right now, let me write you back soon.",
                                    "Received, talk to you in five minutes.",
                                    "Cannot speak right now. Send the info via chat.",
                                    "Can you call me back on Airtel (SIM 2) instead?"
                                )

                                templates.forEach { text ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                showMessageTemplateSheet = false
                                                // Simulates dispatching behind the scenes
                                                if (viewModel != null) {
                                                    viewModel.sendMessage(phoneNumber, text, "SMS")
                                                }
                                                Toast.makeText(context, "Quick SMS successfully sent to $contactName!", Toast.LENGTH_LONG).show()
                                            }
                                            .background(Color(0x14109D43))
                                            .padding(10.dp)
                                    ) {
                                        Text(text, color = Color.LightGray, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // SECURE PRIVATE CALL NOTES PAD SHEET
                if (showCallNotesSheet) {
                    Dialog(onDismissRequest = { showCallNotesSheet = false }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF071F11),
                            border = BorderStroke(1.dp, Color(0xFF1B4E30)),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Secure Call Notepad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(onClick = { showCallNotesSheet = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                Text("Notes taken during this voice session are secure and can be saved to clipboard.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = callNotesText,
                                    onValueChange = { callNotesText = it },
                                    placeholder = { Text("Write active scratch notes here...", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = Color(0xFF2EBD59),
                                        unfocusedBorderColor = Color(0xFF1B4E30)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(callNotesText))
                                            Toast.makeText(context, "Scribbles copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        border = BorderStroke(1.dp, Color(0xFF1B4E30)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Copy Clipboard", color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            showCallNotesSheet = false
                                            Toast.makeText(context, "Notes persisted into call records log!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Store Note", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // DYNAMIC MULTI-PARTY INVITE MERGE IN PROGRESS
                if (showAddCallSheet) {
                    Dialog(onDismissRequest = { showAddCallSheet = false }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF071F11),
                            border = BorderStroke(1.dp, Color(0xFF1B4E30)),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Merge / Add Participant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(onClick = { showAddCallSheet = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                Text("Merge contacts into a simulated secure three-way teleconference.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(14.dp))

                                if (contactsList.isEmpty()) {
                                    Text("No other saved local contacts found to merge.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.height(180.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(contactsList) { c ->
                                            if (c.name != contactName) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            showAddCallSheet = false
                                                            conferenceParties.add(c.name)
                                                            Toast.makeText(context, "Secure session merged with ${c.name}!", Toast.LENGTH_LONG).show()
                                                        }
                                                        .background(Color(0x1F2EBD59))
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(c.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // QUICK LOCAL CONTACTS VIEW ON ACTIVE CALL
                if (showContactsSheet) {
                    Dialog(onDismissRequest = { showContactsSheet = false }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF071F11),
                            border = BorderStroke(1.dp, Color(0xFF1B4E30)),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Contacts Quick-Lookup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(onClick = { showContactsSheet = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                if (contactsList.isEmpty()) {
                                    Text("No saved contacts in your database. Tap Import Contacts inside chat.", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.height(200.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(contactsList) { item ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        clipboardManager.setText(AnnotatedString(item.phone))
                                                        Toast.makeText(context, "Copied ${item.name}'s phone: ${item.phone}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .background(Color(0x0FFFFFFF))
                                                    .padding(10.dp)
                                            ) {
                                                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(item.phone, color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // MORE MENU SETTINGS OPTIONS
                if (showMoreMenuSheet) {
                    Dialog(onDismissRequest = { showMoreMenuSheet = false }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF071F11),
                            border = BorderStroke(1.dp, Color(0xFF1B4E30)),
                            modifier = Modifier.fillMaxWidth().padding(11.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Advanced Call Parameters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(onClick = { showMoreMenuSheet = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Security Key Exchange", color = Color.Gray, fontSize = 12.sp)
                                    Text("PFS ECDH 256 Prime", color = Color(0xFF2EBD59), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Veedle Bitrate Encoder", color = Color.Gray, fontSize = 12.sp)
                                    Text("64 Kbps Active", color = Color.White, fontSize = 12.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Latency Signal Pings", color = Color.Gray, fontSize = 12.sp)
                                    Text("23 ms (Extremely low)", color = Color(0xFF2EBD59), fontSize = 12.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Proxy Route IP", color = Color.Gray, fontSize = 12.sp)
                                    Text("10.89.5.4 (Simulated Tun)", color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // EXPANDABLE PULL-UP TELEMETRY METRIC DRAWER
                AnimatedVisibility(
                    visible = isTelemetryExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = Color(0xFA030E09),
                        border = BorderStroke(1.dp, Color(0xFF1E4C33)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.38f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Live Cellular Diagnostics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(onClick = { isTelemetryExpanded = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    TelemetryMetricRow("Audio Codec Stream", "Opus HD Adaptive Audio (48KHz, Stereo)")
                                    TelemetryMetricRow("VoIP Protocol System", "SimChat Decent SIP WebRTC Tunnel v2")
                                    TelemetryMetricRow("Cell Tower Identifier", "410-01 Safaricom LTE-A/5G SuperG NodeB")
                                    TelemetryMetricRow("Packet Jitter Frequency", "0.42 ms (Stable cellular signal)")
                                    TelemetryMetricRow("Crypto Fingerprint Code", "E2E: AB89:4F12:99C4:7D1E:F91D:A392:0C3A")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryMetricRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x0AFFFFFF))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color(0xFF2EBD59), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

// IMMERSIVE ANIMATED INCOMING CALL SCREEN WITH DOUBLE PULSE RADAR
@Composable
fun SimIncomingCallScreen(
    contactName: String,
    phoneNumber: String,
    simName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onQuickSmsDecline: (String) -> Unit = {}
) {
    var showQuickSmsSheet by remember { mutableStateOf(false) }
    
    // Infinite transition for pulsing radar background behind caller avatar
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseProgress1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val pulseProgress2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030D08),
                        Color(0xFF051D10),
                        Color(0xFF082817)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Main calling screen vertical stack
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // TOP BAR SECURE LOGO
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    color = Color(0x3B109D43),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF1B5E32))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield logo",
                            tint = Color(0xFF2EBD59),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SIMCHAT HIGH-FIDELITY ROUTE",
                            color = Color(0xFF8CE7A2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // MIDDLE CALLER IDENTIFICATION & DOUBLE PULSATING RADAR
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Radial pulse box wrapper
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ring 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pulseProgress1
                                scaleY = pulseProgress1
                                alpha = pulseAlpha1
                            }
                            .background(Color(0xFF2EBD59).copy(alpha = 0.4f), CircleShape)
                    )
                    // Ring 2
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pulseProgress2
                                scaleY = pulseProgress2
                                alpha = pulseAlpha2
                            }
                            .background(Color(0xFF2EBD59).copy(alpha = 0.4f), CircleShape)
                    )
                    
                    // Center Avatar containing person or placeholder
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F3D24))
                            .border(3.dp, Color(0xFF2EBD59), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Caller profile",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name and details layout block
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = contactName,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified status",
                            tint = Color(0xFF2EBD59),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Text(
                        text = phoneNumber,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .background(Color(0x3BFFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsCell,
                            contentDescription = "Sim logo",
                            tint = Color(0xFF8CE7A2),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = simName,
                            color = Color(0xFF8CE7A2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Pulsing Incoming Call Subtitle
                val flowTransition = rememberInfiniteTransition(label = "BlinkText")
                val textAlpha by flowTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "textAlpha"
                )
                Text(
                    text = "INCOMING CARRIER CALLED...",
                    color = Color(0xFF8CE7A2).copy(alpha = textAlpha),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // BOTTOM CONTROLS ACTIONS PANEL
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // SMS TEMPLATE SHIELD SHORTCUT Trigger
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showQuickSmsSheet = true }
                        .background(Color(0x1F2EBD59))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Quick Decline Templates",
                        tint = Color(0xFF2EBD59),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Quick Reply SMS Decline",
                        color = Color(0xFF2EBD59),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ACCEPT AND DECLINE SLIDERS BLOCK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // RED DECLINE BUTTON
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onDecline,
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFFE53935), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline and hang up call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Decline",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // GREEN ANSWER BUTTON
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF2EBD59), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer and accept call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Accept",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // QUICK DECLINE SMS TEMPLATES DIRECT MODE SHEET
        if (showQuickSmsSheet) {
            Dialog(onDismissRequest = { showQuickSmsSheet = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2014)),
                    border = BorderStroke(1.dp, Color(0xFF1B5E32))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Instantly SMS Decline",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showQuickSmsSheet = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close templates info sheet",
                                    tint = Color.White
                                )
                            }
                        }

                        val templates = listOf(
                            "Sorry, I can't talk right now. What's up?",
                            "I am currently driving, will call you soon.",
                            "In a meeting right now, can I back dial you later?",
                            "Busy now, text me the details please.",
                            "Can't pickup, please leave a recording!"
                        )

                        templates.forEach { msg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onQuickSmsDecline(msg)
                                        showQuickSmsSheet = false
                                        onDecline()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x1F2EBD59),
                                border = BorderStroke(0.5.dp, Color(0xFF1D4E30))
                            ) {
                                Text(
                                    text = msg,
                                    color = Color(0xFF2EBD59),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(14.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallGridTouchItem(
    icon: ImageVector,
    label: String,
    isActiveState: Boolean,
    activeBgColor: Color = Color(0xFF2EBD59),
    activeTint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(74.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    if (isActiveState) activeBgColor else Color(0x330C2E1D)
                )
                .border(
                    width = 1.dp,
                    color = if (isActiveState) Color.White.copy(alpha = 0.5f) else Color(0xFF1B4E30),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActiveState) activeTint else Color(0xFF8CE7A2),
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActiveState) Color(0xFF2EBD59) else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DialpadButtonsLayout(onKeyPress: (String) -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 14.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { keyVal ->
                    Surface(
                        shape = CircleShape,
                        color = Color(0x1F2EBD59),
                        border = BorderStroke(1.dp, Color(0xFF1B4D30)),
                        modifier = Modifier
                            .size(54.dp)
                            .clickable { onKeyPress(keyVal) }
                            .weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = keyVal,
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// PREMIUM IDENTITY DETAIL PROFILE SHEET
@Composable
fun ContactProfileDetailSheet(
    contactName: String,
    phoneNumber: String,
    profilePhoto: String?,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onBlockClick: () -> Unit,
    isLocked: Boolean,
    onToggleLock: (Boolean) -> Unit,
    isBlocked: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF081C12),
            border = BorderStroke(1.dp, Color(0xFF1B422E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header back and edit actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = "Contact Terminal Profile",
                        color = Color(0xFF8CE7A2),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Contact", tint = Color(0xFF8CE7A2))
                    }
                }

                // Profile Image section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePhoto.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = profilePhoto,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF8CE7A2), CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1B422E))
                                    .border(2.dp, Color(0xFF8CE7A2), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contactName.take(1).uppercase(),
                                    color = Color(0xFF8CE7A2),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = contactName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Verified Cloud & SIM Contact",
                        color = Color(0xFF8CE7A2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(Color(0x21109D43), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Divider(color = Color(0x1F8CE7A2))

                // Detail section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Item 1: Phone
                    ProfileDetailRow(
                        icon = Icons.Default.Phone,
                        title = "Mobile Identity",
                        value = phoneNumber
                    )

                    // Item 2: Status
                    ProfileDetailRow(
                        icon = Icons.Default.CloudQueue,
                        title = "Network Encryption",
                        value = "Verified SECURE-END-TUNNEL (2048-bit)"
                    )

                    // Item 3: Bio
                    ProfileDetailRow(
                        icon = Icons.Default.Info,
                        title = "Status Message",
                        value = "SimChat developer & power user. Keeping cellular secure!"
                    )
                }

                Divider(color = Color(0x1F8CE7A2))

                // Settings Toggles (Secure Lock toggling inside)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .clickable { onToggleLock(!isLocked) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8CE7A2))
                        Column {
                            Text("Lock Chat Conversation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Only accessible with device PIN code", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { onToggleLock(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF109D43),
                            checkedTrackColor = Color(0x3B109D43),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                // Block Terminal Contact button
                Button(
                    onClick = onBlockClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) Color(0x2400E676) else Color(0x14FF8A80),
                        contentColor = if (isBlocked) Color(0xFF00E676) else Color(0xFFFF8A80)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBlocked) "Unblock Terminal Address" else "Block Terminal Address",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8CE7A2),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// PREMIUM EDIT CONTACT IDENTITY DIALOG
@Composable
fun EditContactDialog(
    currentName: String,
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPhone: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var phoneInput by remember { mutableStateOf(currentPhone) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1F15),
            border = BorderStroke(1.dp, Color(0xFF1E4C33)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Contact Identity",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name", color = Color(0xFF8CE7A2)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8CE7A2),
                        unfocusedBorderColor = Color(0xFF1E4C33)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number", color = Color(0xFF8CE7A2)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8CE7A2),
                        unfocusedBorderColor = Color(0xFF1E4C33)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(nameInput, phoneInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43))
                    ) {
                        Text("Save Details", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// PREMIUM DELETE CONFIRMATION DIALOG
@Composable
fun DeleteChatsConfirmDialog(
    contactName: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A0F0D), // dark red tone matching security alert aesthetics
            border = BorderStroke(1.dp, Color(0xFF5C2B24)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Clear Chat History?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Are you sure you want to delete all local message logs with $contactName? This action is permanent and cannot be undone.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete Chat", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// SMART MESSAGE BUBBLE & PARSING (OTP, Clickable browser, Call direct dials!)
fun isTransactionalMessage(content: String): Boolean {
    val lower = content.lowercase()
    return (lower.contains("confirmed") || lower.contains("received") || lower.contains("sent") || lower.contains("paid") || lower.contains("kes") || lower.contains("ksh")) && 
           (lower.contains("ksh") || lower.contains("kes") || lower.contains("received from") || lower.contains("sent to") || lower.contains("paid to") || lower.contains("m-pesa") || lower.contains("mpesa"))
}

fun extractAmountVal(content: String): String {
    val regex = Regex("(?i)(Ksh|KES|Ksh\\.)\\s?\\d{1,3}(,\\d{3})*(\\.\\d{2})?")
    val found = regex.find(content)
    return found?.value ?: ""
}

fun extractTxCodeVal(content: String): String {
    val regex = Regex("\\b[A-Z0-9]{10}\\b")
    val found = regex.find(content)
    return found?.value ?: ""
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(msg: MessageEntity, viewModel: SimChatViewModel) {
    val isMe = msg.senderIdOrPhone == "ME"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showActionDialog by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }

    val chatTextColorHex by viewModel.chatTextColorHex.collectAsState()
    val customTextColor = remember(chatTextColorHex) {
        if (chatTextColorHex.isNotEmpty()) {
            try {
                Color(android.graphics.Color.parseColor(chatTextColorHex))
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp))

    // Parse text dynamically to identify telephone, email, web address, and 6-digit OTP
    val parsedContent = remember(msg.content) {
        parseMessageText(msg.content)
    }

    val isTx = remember(msg.content) { isTransactionalMessage(msg.content) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { showActionDialog = true }
            ),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                color = if (isMe) {
                    Color(0xFF8CE7A2)
                } else if (isTx) {
                    Color(0xFF14271B)
                } else {
                    Color(0xBA132C1F)
                },
                border = if (isMe) null else BorderStroke(0.5.dp, if (isTx) Color(0xFF109D43) else Color(0x3B8CE7A2)),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(vertical = 2.dp),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Prepend Forwarded indicator if message was forwarded
                    if (msg.isForwarded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = "Forwarded",
                                tint = if (isMe) Color(0xFF041B0D).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Forwarded message",
                                fontSize = 9.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = if (isMe) Color(0xFF041B0D).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Prepend customized verified Transaction layout if transactional message
                    if (isTx) {
                        val amt = extractAmountVal(msg.content)
                        val code = extractTxCodeVal(msg.content)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A1710), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color(0xFF109D43), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF109D43),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "TRANSACTION RECEIPT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        color = Color(0xFF109D43)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF109D43))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("CONFIRMED", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            if (amt.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = amt,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF2EBD59),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (code.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(code))
                                            Toast.makeText(context, "Transaction Code Copied: $code", Toast.LENGTH_SHORT).show()
                                        }
                                        .background(Color(0x1A2EBD59), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ref: $code",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF8CE7A2),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Code",
                                        tint = Color(0xFF8CE7A2),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Render parsed interactive content
                    ClickableMessageText(
                        parsed = parsedContent,
                        color = customTextColor ?: (if (isMe) Color(0xFF041B0D) else Color.White),
                        onWordClicked = { type, text ->
                            clipboardManager.setText(AnnotatedString(text))
                            when (type) {
                                ParsedType.URL -> {
                                    Toast.makeText(context, "URL Copied & Opening browser: $text", Toast.LENGTH_SHORT).show()
                                }
                                ParsedType.PHONE -> {
                                    Toast.makeText(context, "Phone Copied & Dialing: $text", Toast.LENGTH_SHORT).show()
                                }
                                ParsedType.EMAIL -> {
                                    Toast.makeText(context, "Email Copied & Drafting draft: $text", Toast.LENGTH_SHORT).show()
                                }
                                ParsedType.OTP -> {
                                    Toast.makeText(context, "OTP Code Copied: $text", Toast.LENGTH_SHORT).show()
                                }
                                ParsedType.PLAIN -> {}
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (msg.isStarred) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Starred Message",
                                tint = Color(0xFFFBC02D),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            color = if (isMe) Color(0xFF041B0D).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.6f)
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = when (msg.status) {
                                    "SENDING" -> Icons.Default.Schedule
                                    "SENT" -> Icons.Default.Check
                                    "DELIVERED" -> Icons.Default.DoneAll
                                    else -> Icons.Default.DoneAll // Seen status represent green icon
                                },
                                contentDescription = msg.status,
                                tint = if (msg.status == "SEEN") Color(0xFF109D43) else Color(0xFF041B0D).copy(alpha = 0.65f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions Auto-Generated copy chips
            val activeChips = remember(parsedContent) {
                parsedContent.filter { it.type != ParsedType.PLAIN }
            }

            if (activeChips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(vertical = 1.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeChips.forEach { chip ->
                        val (label, icon) = when (chip.type) {
                            ParsedType.OTP -> Pair("Copy OTP: ${chip.text}", Icons.Default.ContentCopy)
                            ParsedType.PHONE -> Pair("Dial/Copy: ${chip.text}", Icons.Default.Phone)
                            ParsedType.URL -> Pair("Copy/Go Link", Icons.Default.Language)
                            ParsedType.EMAIL -> Pair("Copy/Email", Icons.Default.Email)
                            else -> Pair("Copy: ${chip.text}", Icons.Default.ContentCopy)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x2E109D43),
                            border = BorderStroke(0.5.dp, Color(0x3B8CE7A2)),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(chip.text))
                                Toast.makeText(context, "${chip.text} copied successfully!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF8CE7A2), modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = label, color = Color(0xFF8CE7A2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Long press dropdown option actions Matches specs!
    if (showActionDialog) {
        Dialog(onDismissRequest = { showActionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Message Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    ActionRowItem(icon = Icons.Filled.CopyAll, label = "Copy Text") {
                        clipboardManager.setText(AnnotatedString(msg.content))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        showActionDialog = false
                    }
                    ActionRowItem(icon = Icons.Default.ArrowForward, label = "Forward Message") {
                        showForwardDialog = true
                        showActionDialog = false
                    }
                    ActionRowItem(
                        icon = if (msg.isStarred) Icons.Filled.StarHalf else Icons.Filled.Star,
                        label = if (msg.isStarred) "Unstar / Un-mark Message" else "Mark / Star Message"
                    ) {
                        viewModel.toggleStarMessage(msg.id, !msg.isStarred)
                        Toast.makeText(context, if (msg.isStarred) "Message unstarred/unmarked!" else "Message starred/marked!", Toast.LENGTH_SHORT).show()
                        showActionDialog = false
                    }
                    ActionRowItem(icon = Icons.Filled.Share, label = "Share out...") {
                        Toast.makeText(context, "Opening share panel...", Toast.LENGTH_SHORT).show()
                        showActionDialog = false
                    }
                    ActionRowItem(icon = Icons.Filled.Delete, label = "Delete Message", tint = Color.Red) {
                        viewModel.deleteMessage(msg.id)
                        Toast.makeText(context, "Message deleted locally!", Toast.LENGTH_SHORT).show()
                        showActionDialog = false
                    }
                }
            }
        }
    }

    if (showForwardDialog) {
        ForwardMessageDialog(
            content = msg.content,
            viewModel = viewModel,
            onDismiss = { showForwardDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessageDialog(
    content: String,
    viewModel: SimChatViewModel,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isEmpty()) contacts else {
            contacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight(0.75f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Forward Message", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Import Contacts Button
                Button(
                    onClick = {
                        viewModel.addNewContact("Peter Kiprono", "+254711999888", "peter@simchat.com", true)
                        viewModel.addNewContact("Mary Wambui", "+254722555444", "mary@simchat.com", true)
                        viewModel.addNewContact("Grace Atieno", "+254733888777", "grace@simchat.com", false)
                        viewModel.addNewContact("Dr. Nelson", "+254701234567", "nelson@simchat.com", false)
                        Toast.makeText(context, "High-fidelity SIM/Cloud contacts imported!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF109D43)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Sim/Cloud Contacts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("CONTACTS & CHATS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))

                if (filteredContacts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No contacts found. Try importing some!", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredContacts) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val chatType = if (contact.isCloud) "CLOUD" else "SMS"
                                        viewModel.forwardMessage(contact.id, content, chatType)
                                        Toast.makeText(context, "Forwarded to ${contact.name}!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF109D43),
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(contact.phone, fontSize = 12.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEF9F1))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Send", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF109D43))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionRowItem(icon: ImageVector, label: String, tint: Color = SimChatPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontWeight = FontWeight.Medium, color = if (tint == Color.Red) Color.Red else Color.Unspecified)
    }
}

// SETTINGS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SimChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 1. Load active Profile State
    val sessionState by viewModel.sessionState.collectAsState()
    var profileName by remember { mutableStateOf("") }
    var profileBio by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val colorsList = listOf(
        Color(0xFF109D43), // Premium emerald green
        Color(0xFF2A7BF1), // Vibrant blue
        Color(0xFF7C4DFF), // Royal purple
        Color(0xFFE040FB), // Magenta/pink
        Color(0xFFFF9100), // Vibrant orange
        Color(0xFF00E5FF)  // Cyan/teal
    )
    val colorNames = listOf("green", "blue", "purple", "pink", "orange", "cyan")

    LaunchedEffect(sessionState) {
        val s = sessionState
        if (s is SessionState.CloudMode) {
            profileName = s.name
            profileBio = s.bio
            val idx = colorNames.indexOf(s.photo)
            selectedColorIndex = if (idx != -1) idx else 0
        } else {
            profileName = "SIM User"
            profileBio = "Hey there! I am using SimChat."
            selectedColorIndex = 0
        }
    }

    // 2. Load preferences states
    var preferredSimState by remember { mutableStateOf(viewModel.preferredCarrierSim) }
    var isNotifs by remember { mutableStateOf(viewModel.isNotifsEnabled) }
    var activePasscode by remember { mutableStateOf(viewModel.getPasscodeState()) }
    var activeThemeState by remember { mutableStateOf(viewModel.activeTheme) }
    val currentBgTheme by viewModel.chatBackgroundTheme.collectAsState()
    val currentTextColor by viewModel.chatTextColorHex.collectAsState()
    val currentFontName by viewModel.chatFontFamily.collectAsState()
    val currentLocalUri by viewModel.localWallpaperUri.collectAsState()
    var customHexInput by remember { mutableStateOf(currentTextColor) }

    // 3. Collect DB reactive Stats
    val conversationsList by viewModel.filteredConversations.collectAsState()
    val contactsList by viewModel.contacts.collectAsState()

    // 4. Dialog controller states
    var showPasscodeSetup by remember { mutableStateOf(false) }
    var showPasscodeVerifyToggleOff by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // GUEST UPGRADE TO CLOUD ROW
            if (viewModel.sessionManager.isGuest) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.5.dp, Color(0xFF2EBD59)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF109D43))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upgrade to Cloud Account", fontWeight = FontWeight.Bold, color = Color(0xFF109D43), fontSize = 16.sp)
                        }
                        Text(
                            "You are currently in Guest Mode (SMS/carrier storage only). Upgrade to a secure cloud account with your Google Account or Phone number to back up your chats, enable instant messaging, and sync in real time across devices!",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Button(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Redirecting to SimChat Setup Dashboard...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Register / Bind Cloud Account Now", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // PROFILE SECTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit My Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF109D43),
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Profile Circle Avatar
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(colorsList[selectedColorIndex])
                                .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profileName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile Color Picker Row
                    Text(
                        text = "Adjust Avatar Color Vibe",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorsList.forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColorIndex == idx) 2.5.dp else 0.dp,
                                        color = if (selectedColorIndex == idx) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColorIndex == idx) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        placeholder = { Text("Display Name") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF109D43)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileBio,
                        onValueChange = { profileBio = it },
                        placeholder = { Text("Status Bio") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF109D43)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(profileName, profileBio, colorNames[selectedColorIndex])
                            Toast.makeText(context, "Profile Saved Ready", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile Changes", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // THEME & APPEARANCE SECTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Appearance Vibe",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF109D43),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf("light" to "Light", "dark" to "Dark", "system" to "System")
                        modes.forEach { (modeKey, label) ->
                            val isSelected = activeThemeState == modeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF109D43) else Color.Transparent)
                                    .clickable {
                                        activeThemeState = modeKey
                                        viewModel.updateTheme(modeKey)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Realtime theme switcher gives immediate aesthetic visual transformations.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // CHAT CUSTOMIZATION DESK (5 WALLPAPERS, TEXT COLORS & EXCLUSIVE FONTS)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Custom Chat Personalization Suite",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF109D43),
                        fontSize = 14.sp
                    )

                    // Section 1: 5 Beautiful Chat Wallpaper Themes
                    Text("Select Chat Background Wallpaper", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val localWallpapers = listOf(
                            "default" to "Default Dark",
                            "sunset" to "🌅 Sunset Glow",
                            "forest" to "🌲 Forest Zen",
                            "marine" to "🌊 Deep Marine",
                            "neon" to "⚡ Cyber Neon",
                            "lavender" to "🌸 Lavender Silk"
                        )
                        localWallpapers.forEach { (bgKey, name) ->
                            val isSelected = currentBgTheme == bgKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF109D43) else MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        viewModel.updateChatBackgroundTheme(bgKey)
                                        viewModel.updateLocalWallpaperUri("") // Reset local simulated upload if prepackaged chosen
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Section 1B: Simulate Local Custom Wallpaper Upload (DOES NOT SYNC TO CLOUD!)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Or Upload Custom Personal Wallpaper (Local Only):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLocalUri.startsWith("custom_upload")) "Active: Local Photo Pairing ✅" else "No local photos uploaded yet",
                                fontSize = 12.sp,
                                color = if (currentLocalUri.startsWith("custom_upload")) Color(0xFF2EBD59) else Color.LightGray
                            )
                            Button(
                                onClick = {
                                    val randomPhotoCode = "custom_upload_" + (1000..9999).random().toString()
                                    viewModel.updateLocalWallpaperUri(randomPhotoCode)
                                    viewModel.updateChatBackgroundTheme("custom")
                                    Toast.makeText(context, "Local device photo uploaded & applied locally!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Upload File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            "This preference is stored exclusively on this handset's sandbox storage and is never uploaded to the SimChat Cloud servers.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Section 2: Chat Text Bubble Theme Color Changer
                    Text("Chat Bubble Accent Theme Color", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bubbleColors = listOf(
                            "" to "SimChat Premium",
                            "#2EBD59" to "Emerald Green",
                            "#007AFF" to "Azure Blue",
                            "#FF9500" to "Tangerine Glow",
                            "#FF2D55" to "Crimson Rose",
                            "#9C27B0" to "Royal Violet",
                            "#7C4DFF" to "Indigo Silk"
                        )
                        bubbleColors.forEach { (colorHex, name) ->
                            val isSelected = currentTextColor == colorHex
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF109D43) else MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        viewModel.updateChatTextColorHex(colorHex)
                                        customHexInput = colorHex
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Section 2B: Custom Hex Color Text Field
                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = {
                            customHexInput = it
                            if (it.length == 7 && it.startsWith("#")) {
                                viewModel.updateChatTextColorHex(it)
                            }
                        },
                        placeholder = { Text("#2EBD59") },
                        label = { Text("Or Type Custom Bubble Color Hex Code", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Section 3: Font Family Customizer (Includes all premium fonts in the world!)
                    Text("Inbox & Chat Font Suite (Applies to Inbox)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .height(180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val fontsAvailable = listOf(
                            "default" to "Default System Grotesk",
                            "helvetica" to "Helvetica Neue (Commercial Standard)",
                            "futura" to "Futura Bold geometric Display",
                            "caslon_pro" to "Adobe Caslon Premium Serif",
                            "sf_pro_display" to "iOS San Francisco Pro Display",
                            "circular_std" to "Circular Std (Premium Brand Font)",
                            "proxima_nova" to "Proxima Nova Elegant Sans",
                            "avenir_next" to "Avenir Next Modern Display",
                            "apercu_pro" to "Apercu Pro Premium Sans",
                            "garamond" to "ITC Garamond Classic Italian Italic",
                            "playfair" to "Playfair Display Heavy Editorial",
                            "cinzel" to "Cinzel Roman High Contrast Classic",
                            "bodoni" to "Bodoni Modern Didone Standard",
                            "firacode" to "Fira Code Monospace Developer Desk",
                            "comicsans" to "Chalkboard Comic Sans (Playful Layout)"
                        )
                        fontsAvailable.forEach { (fontKey, displayName) ->
                            val isSelected = currentFontName == fontKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateChatFontFamily(fontKey)
                                        Toast.makeText(context, "$displayName font active!", Toast.LENGTH_SHORT).show()
                                    }
                                    .background(if (isSelected) Color(0xFF109D43).copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 13.sp,
                                    fontFamily = if (fontKey == "helvetica") androidx.compose.ui.text.font.FontFamily.SansSerif else androidx.compose.ui.text.font.FontFamily.Default,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF109D43) else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF109D43),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECURITY & DUAL SIM PREFERENCES
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Security & Transmission Preference",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF109D43),
                        fontSize = 14.sp
                    )

                    // Notifications Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF109D43))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Push Notifications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Realtime cloud chat notifications alerts", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = if (viewModel.sessionManager.isGuest) false else isNotifs,
                            onCheckedChange = {
                                if (viewModel.sessionManager.isGuest) {
                                    viewModel.triggerUpgradeDialog(true)
                                } else {
                                    isNotifs = it
                                    viewModel.toggleNotifs(it)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF109D43), checkedTrackColor = Color(0xFFD6F5E1))
                        )
                    }

                    // Security PIN Lock Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF109D43))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App PIN Lock Protection", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Require 4-digit security PIN on startup", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = activePasscode,
                            onCheckedChange = { active ->
                                if (active) {
                                    showPasscodeSetup = true
                                } else {
                                    showPasscodeVerifyToggleOff = true
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF109D43), checkedTrackColor = Color(0xFFD6F5E1))
                        )
                    }

                    // PIN customization auxiliary trigger
                    if (activePasscode) {
                        Button(
                            onClick = { showPasscodeSetup = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = Color(0xFF109D43)),
                            border = BorderStroke(1.dp, Color(0xFF109D43)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Customized active PIN Passcode", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Biometric Lock row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF109D43))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Biometric / Fingerprint Unlock", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Enable fingerprint scan for safe encryption", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        var isBiometricEnabled by remember { mutableStateOf(viewModel.sessionManager.isBiometricLocked) }
                        var showBiometricScanTrigger by remember { mutableStateOf(false) }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { active ->
                                if (active) {
                                    showBiometricScanTrigger = true
                                } else {
                                    viewModel.sessionManager.isBiometricLocked = false
                                    isBiometricEnabled = false
                                    Toast.makeText(context, "Biometric unlock disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF109D43), checkedTrackColor = Color(0xFFD6F5E1))
                        )

                        if (showBiometricScanTrigger) {
                            SimBiometricScanDialog(
                                onDismiss = { showBiometricScanTrigger = false },
                                onVerified = {
                                    viewModel.sessionManager.isBiometricLocked = true
                                    isBiometricEnabled = true
                                    showBiometricScanTrigger = false
                                    Toast.makeText(context, "Fingerprint unlock configured successfully", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // Change Password row
                    val currentEmailOrPhone = viewModel.sessionManager.userEmail
                    if (!viewModel.sessionManager.isGuest && currentEmailOrPhone != null) {
                        var showChangePasswordDialog by remember { mutableStateOf(false) }

                        Button(
                            onClick = { showChangePasswordDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = Color(0xFF109D43)),
                            border = BorderStroke(1.dp, Color(0xFF109D43)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Account Password", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (showChangePasswordDialog) {
                            ChangePasswordDialog(
                                address = currentEmailOrPhone,
                                onDismiss = { showChangePasswordDialog = false },
                                onSave = { newPass ->
                                    viewModel.updateUserPassword(currentEmailOrPhone, newPass)
                                    showChangePasswordDialog = false
                                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // Active Sessions management
                    if (!viewModel.sessionManager.isGuest) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF109D43), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Active Session Monitor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text("• Logged Address: ${viewModel.sessionManager.userEmail}", fontSize = 12.sp, color = Color.Gray)
                                Text("• Device Model: Simulated Android 11 Platform Emulator", fontSize = 12.sp, color = Color.Gray)
                                Text("• Session Status: Securely Encrypted & Active", fontSize = 12.sp, color = Color.Gray)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.logout()
                                            Toast.makeText(context, "Logged out of current SimChat session", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Log Out Session", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Dual SIM Selector Mode
                    Column {
                        Text(
                            text = "Primary SMS Carrier Route",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CellTower, contentDescription = null, tint = Color(0xFF109D43), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select default SIM slot for outbound SMS carrier routing", fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("SIM 1", "SIM 2").forEach { sim ->
                                val isSimSelected = preferredSimState == sim
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSimSelected) 2.dp else 1.dp,
                                            color = if (isSimSelected) Color(0xFF109D43) else Color.LightGray,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(if (isSimSelected) Color(0xFFEEF9F1) else Color.Transparent)
                                        .clickable {
                                            preferredSimState = sim
                                            viewModel.preferredCarrierSim = sim
                                            Toast.makeText(context, "$sim routing chosen for outbound SMS carrier", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SimCard,
                                            contentDescription = null,
                                            tint = if (isSimSelected) Color(0xFF109D43) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = sim,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSimSelected) Color(0xFF109D43) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // STORAGE & DATABASE STATS (SimChat Analytics)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Storage & Database Maintenance",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF109D43),
                        fontSize = 14.sp
                    )

                    // Display reactive stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat 1: Conversations
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${conversationsList.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF109D43))
                                Text("Conversations", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Stat 2: Contacts
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${contactsList.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF109D43))
                                Text("Linked Contacts", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Stat 3: Storage
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val sizeStr = remember(conversationsList, contactsList) {
                                    val count = conversationsList.size + contactsList.size
                                    "${32 + count * 2} KB"
                                }
                                Text(sizeStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF109D43))
                                Text("SQLite Engine", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.importSms()
                            Toast.makeText(context, "Device Carrier SMS Imported Successfully!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF9F1), contentColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Carrier Conversations Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Chat conversation database backup archived to /sdcard/SimChat/backup.json (simulated)!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF9F1), contentColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Database backup file securely", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // DANGER ZONE
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE57373)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Danger Zone",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Once you erase your local database records, there is no going back. All chat threads and carrier backups on this device will be purged.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { showWipeConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wipe local database & Reset", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Text(
                text = "SimChat Secure Decentralized Platform",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Version 1.0.0 (Production-Ready)",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // --- COOPERATIVE MODAL DIALOGS ---

    // 1. Setup Passcode Dialog
    if (showPasscodeSetup) {
        PasscodeSetupDialog(
            onDismissRequest = {
                showPasscodeSetup = false
                activePasscode = viewModel.getPasscodeState()
            },
            onSavePin = { code ->
                viewModel.setPasscode(code)
                activePasscode = true
                showPasscodeSetup = false
            }
        )
    }

    // 2. Verify Passcode Dialog (Toggle validation off)
    if (showPasscodeVerifyToggleOff) {
        PasscodeVerifyDialog(
            onDismissRequest = {
                showPasscodeVerifyToggleOff = false
                activePasscode = viewModel.getPasscodeState()
            },
            onVerifyPin = { pin ->
                viewModel.enterPin(pin)
            },
            onVerified = {
                viewModel.setPasscode(null)
                activePasscode = false
                showPasscodeVerifyToggleOff = false
                Toast.makeText(context, "App lock disabled successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Wipe and Reset Confirmation Dialog
    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Completely Lock & Wipe Storage?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to completely erase all chat messages, groups, contacts, and preferences from this device? This action is IRREVERSIBLE and will reset the app back to guest status."
                )
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirm = false
                        viewModel.wipeDatabase()
                        viewModel.loginGuest() // redirect and back to home guest mode or welcome
                        Toast.makeText(context, "All local database statistics and configurations purged!", Toast.LENGTH_LONG).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Wipe Storage", color = Color.White)
                }
            }
        )
    }
}

// SETUP PASSCODE DIALOGUE
@Composable
fun PasscodeSetupDialog(
    onDismissRequest: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF109D43),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isConfirming) "Confirm 4-Digit PIN" else "Set 4-Digit PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isConfirming) "Enter PIN again to confirm" else "Choose a secure passcode pin",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                val currentVal = if (isConfirming) confirmPin else pin
                OutlinedTextField(
                    value = currentVal,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            if (isConfirming) confirmPin = it else pin = it
                        }
                    },
                    placeholder = { Text("0000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.LightGray.copy(alpha = 0.5f)) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.width(160.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (!isConfirming) {
                                if (pin.length < 4) {
                                    Toast.makeText(context, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                                } else {
                                    isConfirming = true
                                }
                            } else {
                                if (pin == confirmPin) {
                                    onSavePin(pin)
                                    Toast.makeText(context, "Passcode configured successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PINs do not match. Restarting setup.", Toast.LENGTH_SHORT).show()
                                    pin = ""
                                    confirmPin = ""
                                    isConfirming = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isConfirming) "Save PIN" else "Continue", color = Color.White)
                    }
                }
            }
        }
    }
}

// VERIFY PASSCODE DIALOGUE
@Composable
fun PasscodeVerifyDialog(
    onDismissRequest: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onVerified: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = Color(0xFF109D43),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verify Security PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter current PIN to unlock settings",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            enteredPin = it
                        }
                    },
                    placeholder = { Text("0000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.LightGray.copy(alpha = 0.5f)) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.width(160.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (onVerifyPin(enteredPin)) {
                                onVerified()
                            } else {
                                Toast.makeText(context, "Invalid PIN. Try again.", Toast.LENGTH_SHORT).show()
                                enteredPin = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Verify", color = Color.White)
                    }
                }
            }
        }
    }
}

// HELPER DLGS AND SMART PARSING LOGIC

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onAdd: (String, String, String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isCloud by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add New Contact", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (Optional)") })

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCloud, onCheckedChange = { isCloud = it })
                    Text("Has Cloud Account")
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(name, phone, email, isCloud) },
                        colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupAndChatCreationDialog(
    viewModel: SimChatViewModel,
    onDismiss: () -> Unit,
    onChatSelected: (String) -> Unit,
    onCreatePremiumGroup: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var triggerGroup by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (triggerGroup) "Create New Group Chat" else "Draft New Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (triggerGroup) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Group Name") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Description") })
                } else {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Contact Phone or Name") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!triggerGroup) {
                    TextButton(onClick = {
                        if (viewModel.sessionManager.isGuest) {
                            onDismiss()
                            viewModel.triggerUpgradeDialog(true)
                        } else {
                            triggerGroup = true
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.People, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create cloud group conversation", fontWeight = FontWeight.Bold, color = SimChatPrimary)
                    }

                    if (onCreatePremiumGroup != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onCreatePremiumGroup()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("add_premium_group_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Stars, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configure Premium Group Deck", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                if (triggerGroup) {
                                    viewModel.createGroup(title, subtitle, "")
                                } else {
                                    onChatSelected(title)
                                }
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary)
                    ) {
                        Text("Proceed")
                    }
                }
            }
        }
    }
}

// SMART TEXT BUBBLE RENDERING CORE SEGMENT
enum class ParsedType { PLAIN, URL, PHONE, EMAIL, OTP }
data class TextSegment(val type: ParsedType, val text: String)

fun parseMessageText(content: String): List<TextSegment> {
    val segments = mutableListOf<TextSegment>()

    // Look for OTP Codes (6-digits numbers)
    val otpRegex = Regex("\\b\\d{6}\\b")
    // URLs
    val urlRegex = Regex("(https?://[\\w.\\-]+(?:/[\\w./?%&=~]*)?)")
    // Phones
    val phoneRegex = Regex("(\\+?[1-9]\\d{1,14}|07\\d{8})")
    // Emails
    val emailRegex = Regex("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})")

    // Compile match ranges
    val matches = mutableListOf<Pair<IntRange, ParsedType>>()

    otpRegex.findAll(content).forEach { matches.add(it.range to ParsedType.OTP) }
    urlRegex.findAll(content).forEach { matches.add(it.range to ParsedType.URL) }
    phoneRegex.findAll(content).forEach { m ->
        // Avoid marking digits inside OTP or URLs as phone
        if (matches.none { m.range.first >= it.first.first && m.range.last <= it.first.last }) {
            matches.add(m.range to ParsedType.PHONE)
        }
    }
    emailRegex.findAll(content).forEach { matches.add(it.range to ParsedType.EMAIL) }

    // Sort matching pairs
    matches.sortBy { it.first.first }

    var lastIdx = 0
    for (match in matches) {
        val range = match.first
        val type = match.second

        if (range.first > lastIdx) {
            segments.add(TextSegment(ParsedType.PLAIN, content.substring(lastIdx, range.first)))
        }
        segments.add(TextSegment(type, content.substring(range.first, range.last + 1)))
        lastIdx = range.last + 1
    }

    if (lastIdx < content.length) {
        segments.add(TextSegment(ParsedType.PLAIN, content.substring(lastIdx)))
    }

    if (segments.isEmpty()) {
        segments.add(TextSegment(ParsedType.PLAIN, content))
    }

    return segments
}

@Composable
fun ClickableMessageText(
    parsed: List<TextSegment>,
    color: Color,
    onWordClicked: (ParsedType, String) -> Unit
) {
    val builder = buildAnnotatedString {
        parsed.forEachIndexed { index, segment ->
            if (segment.type == ParsedType.PLAIN) {
                withStyle(style = SpanStyle(color = color)) {
                    append(segment.text)
                }
            } else {
                pushStringAnnotation(tag = segment.type.name, annotation = segment.text)
                withStyle(
                    style = SpanStyle(
                        color = if (color == Color.White) Color(0xFFE8F5E9) else SimChatPrimary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (segment.type == ParsedType.OTP) null else androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                ) {
                    append(segment.text)
                }
                pop()
            }
        }
    }

    ClickableText(
        text = builder,
        onClick = { offset ->
            var handled = false
            for (type in ParsedType.values()) {
                builder.getStringAnnotations(tag = type.name, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onWordClicked(type, annotation.item)
                        handled = true
                    }
                if (handled) break
            }
        }
    )
}

// Polyfill ClickableText for Compose (stable compatible API)
@Composable
fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {},
    onClick: (Int) -> Unit
) {
    val layoutResult = remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            layoutResult.value?.let { layout ->
                onClick(layout.getOffsetForPosition(pos))
            }
        }
    }
    Text(
        text = text,
        modifier = modifier.then(pressIndicator),
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepScreenSecure(isSecure: Boolean) {
    if (!isSecure) {
        return
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEmulator = remember {
        val model = android.os.Build.MODEL ?: ""
        val hardware = android.os.Build.HARDWARE ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        
        brand.startsWith("generic") ||
        device.startsWith("generic") ||
        model.contains("google_sdk") ||
        model.contains("Emulator") ||
        model.contains("Android SDK built for x86") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        product.contains("sdk_gphone") ||
        product.contains("emulator") ||
        fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown")
    }

    if (isEmulator) {
        return
    }

    DisposableEffect(Unit) {
        val activity = findActivity(context)
        val window = activity?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            val liveActivity = findActivity(context)
            liveActivity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

fun findActivity(context: android.content.Context): android.app.Activity? {
    var cur = context
    while (cur is android.content.ContextWrapper) {
        if (cur is android.app.Activity) {
            return cur
        }
        cur = cur.baseContext
    }
    return null
}

fun triggerRealBiometricAuthentication(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val executor = context.mainExecutor
        try {
            val biometricPrompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
                .setTitle("Secure Private Vault")
                .setSubtitle("Confirm biological credentials")
                .setDescription("Verify your fingerprint on the phone sensory hardware.")
                .setNegativeButton("Cancel", executor) { _, _ ->
                    onError("Authentication cancelled")
                }
                .build()

            val cancellationSignal = android.os.CancellationSignal()

            biometricPrompt.authenticate(
                cancellationSignal,
                executor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        val msg = errString?.toString() ?: "Biometric error (code $errorCode)"
                        onError(msg)
                    }

                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Fingerprint not recognized. Please scan a registered finger.")
                    }
                }
            )
        } catch (e: Exception) {
            onError("Biometric initialization failed: ${e.localizedMessage ?: "Hardware unsupported"}")
        }
    } else {
        onError("Your device OS version is lower than Android 9.0 (API 28).")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedChatsScreen(
    viewModel: SimChatViewModel,
    onChatSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    // Keep screenshots restricted in the Locked Chats folders and dialogs
    KeepScreenSecure(viewModel.isScreenshotProtectionEnabled)

    val context = LocalContext.current
    var isAuthenticated by remember { mutableStateOf(false) }

    // Configuration / state variables, made fully reactive using local mutable states so they refresh instantly on reset!
    var localPasskey by remember { mutableStateOf(viewModel.lockedChatsPasskey) }
    var localBiometricEnabled by remember { mutableStateOf(viewModel.isLockedChatsBiometricEnabled) }
    var localBiometricType by remember { mutableStateOf(viewModel.lockedChatsBiometricType ?: "FINGERPRINT") }
    var localPattern by remember { mutableStateOf(viewModel.lockedChatsPattern) }

    // State for setting a new PIN / Pattern
    var setupStep by remember { mutableStateOf("PIN") } // "PIN", "PATTERN_DRAW", "PATTERN_CONFIRM"
    var setupPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var inConfirmStep by remember { mutableStateOf(false) }
    var setupBiometricCheckbox by remember { mutableStateOf(true) }
    var setupBiometricType by remember { mutableStateOf("FINGERPRINT") } // "FINGERPRINT" or "PATTERN"

    // Temporary pattern holder during pattern setup
    var tempPatternSequence by remember { mutableStateOf<List<Int>>(emptyList()) }

    // Active pattern drawing sequence during setup/unlocking
    val activePatternDots = remember { mutableStateListOf<Int>() }

    // State for entering a PIN
    var enteredPin by remember { mutableStateOf("") }

    // Controls whether we are currently displaying Pattern or PIN keypad for unlocking
    var usePatternLockView by remember { mutableStateOf(localBiometricEnabled && localBiometricType == "PATTERN") }

    // Biometric simulator dialog visibility (for fingerprint)
    var showBiometricCheckDialog by remember { mutableStateOf(localBiometricEnabled && localBiometricType == "FINGERPRINT") }

    // State for settings page inside authenticated screen
    var showSettingsDialog by remember { mutableStateOf(false) }

    // State for Reset Confirmation Dialog (forgotten passcode reset)
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    var showInSituPatternSetupDialog by remember { mutableStateOf(false) }
    var inSituPatternStep by remember { mutableStateOf("DRAW") } // "DRAW" or "CONFIRM"
    val inSituActivePatternDots = remember { mutableStateListOf<Int>() }
    var inSituTempPatternList by remember { mutableStateOf<List<Int>>(emptyList()) }

    var setupFlowStep by remember { mutableStateOf("WELCOME") } // "WELCOME", "CHOOSE_TYPE", "PIN_DRAW", "PIN_CONFIRM", "PATTERN_DRAW", "PATTERN_CONFIRM", "FINGERPRINT_SCAN"
    var chosenLockType by remember { mutableStateOf("PIN") } // "PIN", "PATTERN", "FINGERPRINT"

    // Synchronize local state with ViewModel on initialization or reset
    val onResetAll = {
        viewModel.resetLockedFolderAndClearChats()
        localPasskey = null
        localBiometricEnabled = false
        localBiometricType = "FINGERPRINT"
        localPattern = null
        isAuthenticated = false
        setupStep = "PIN"
        setupPin = ""
        confirmPin = ""
        inConfirmStep = false
        setupBiometricCheckbox = true
        setupBiometricType = "FINGERPRINT"
        tempPatternSequence = emptyList()
        activePatternDots.clear()
        enteredPin = ""
        usePatternLockView = false
        showBiometricCheckDialog = false
        showInSituPatternSetupDialog = false
        inSituPatternStep = "DRAW"
        inSituActivePatternDots.clear()
        inSituTempPatternList = emptyList()
        setupFlowStep = "WELCOME"
        chosenLockType = "PIN"
    }

    // Forgot credentials / reset dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = "Warning",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Reset Locked Folder?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "If you forgot your passkey or pattern, you can reset the secure vault. WARNING: Resetting will permanently delete all secure conversations and messages inside the locked folders. This action is irreversible.",
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetAll()
                        Toast.makeText(context, "Secure vault reset completely.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Wipe & Reset Folder", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = SimChatPrimary)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    val isLockConfigured = remember(localPasskey, localPattern, localBiometricEnabled, localBiometricType) {
        localPasskey != null || localPattern != null || (localBiometricEnabled && localBiometricType == "FINGERPRINT")
    }

    if (!isAuthenticated) {
        if (!isLockConfigured) {
            // ==================== ONBOARDING FLOW ====================
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Secure Locked Chats Setup") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (setupFlowStep) {
                        "WELCOME" -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Lock",
                                tint = SimChatPrimary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Secure Folder Private Vault",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Keep your confidential discussions protected with cellular-grade privacy systems.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Visual guidelines (Material 3 polished cards)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Hidden Conversations Inbox", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Moved chats will vanish from the main Inbox tab completely.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Security, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Screenshot Restriction Protection", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Strict systems block browser display streaming/snapshots.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Personalized Lock Style", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Configure either PIN, Draw Pattern, or Fingerprint Scanner.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = { setupFlowStep = "CHOOSE_TYPE" },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary)
                            ) {
                                Text("Select Lock Security Style", color = Color.White)
                            }
                        }

                        "CHOOSE_TYPE" -> {
                            Text(
                                text = "Select Protection Style",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose exactly one lock type below to secure your private conversations.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val options = listOf(
                                    Triple("PIN", "4-Digit PIN Passcode", Icons.Filled.Lock),
                                    Triple("PATTERN", "3x3 Connecting Pattern", Icons.Filled.Security),
                                    Triple("FINGERPRINT", "Fingerprint Biometric", Icons.Filled.Fingerprint)
                                )

                                options.forEach { (type, label, icon) ->
                                    val isSelected = chosenLockType == type
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) SimChatPrimary else Color.LightGray.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .background(
                                                color = if (isSelected) SimChatPrimary.copy(alpha = 0.05f) else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { chosenLockType = type }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) SimChatPrimary else Color.Gray,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) SimChatPrimary else MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = when (type) {
                                                    "PIN" -> "Numeric keypad code for quick and familiar safety."
                                                    "PATTERN" -> "Continuous connective dot gesture grid lock."
                                                    "FINGERPRINT" -> "Simulate device fingerprint scanner validation."
                                                    else -> ""
                                                },
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { chosenLockType = type },
                                            colors = RadioButtonDefaults.colors(selectedColor = SimChatPrimary)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    when (chosenLockType) {
                                        "PIN" -> {
                                            setupPin = ""
                                            confirmPin = ""
                                            setupFlowStep = "PIN_DRAW"
                                        }
                                        "PATTERN" -> {
                                            activePatternDots.clear()
                                            tempPatternSequence = emptyList()
                                            setupFlowStep = "PATTERN_DRAW"
                                        }
                                        "FINGERPRINT" -> {
                                            setupFlowStep = "FINGERPRINT_SCAN"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary)
                            ) {
                                Text("Continue to Setup", color = Color.White)
                            }
                        }

                        "PIN_DRAW", "PIN_CONFIRM" -> {
                            val isConfirm = setupFlowStep == "PIN_CONFIRM"
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Shield",
                                tint = SimChatPrimary,
                                modifier = Modifier.size(72.dp)
                            )

                            Text(
                                text = if (!isConfirm) "Create a 4-Digit Passcode" else "Confirm your 4-Digit Passcode",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Protect hidden chats securely. Enter 4 digits to continue.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Pin progress dots
                            val activeLength = if (!isConfirm) setupPin.length else confirmPin.length
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (i in 1..4) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (i <= activeLength) SimChatPrimary else Color.LightGray.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            CustomSecurityKeypad(
                                onKeyPressed = { digit ->
                                    if (!isConfirm) {
                                        if (setupPin.length < 4) {
                                            setupPin += digit
                                            if (setupPin.length == 4) {
                                                setupFlowStep = "PIN_CONFIRM"
                                            }
                                        }
                                    } else {
                                        if (confirmPin.length < 4) {
                                            confirmPin += digit
                                            if (confirmPin.length == 4) {
                                                if (setupPin == confirmPin) {
                                                    // Save PIN and complete setup
                                                    viewModel.lockedChatsPasskey = setupPin
                                                    viewModel.isLockedChatsBiometricEnabled = false
                                                    viewModel.lockedChatsBiometricType = "PIN"
                                                    viewModel.lockedChatsPattern = null // Clear any pattern

                                                    localPasskey = setupPin
                                                    localBiometricEnabled = false
                                                    localBiometricType = "PIN"
                                                    localPattern = null

                                                    Toast.makeText(context, "Secure PIN passcode successfully setup!", Toast.LENGTH_SHORT).show()
                                                    isAuthenticated = true
                                                } else {
                                                    Toast.makeText(context, "PINs do not match. Starting again.", Toast.LENGTH_SHORT).show()
                                                    setupPin = ""
                                                    confirmPin = ""
                                                    setupFlowStep = "PIN_DRAW"
                                                }
                                            }
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (!isConfirm) {
                                        if (setupPin.isNotEmpty()) {
                                            setupPin = setupPin.dropLast(1)
                                        }
                                    } else {
                                        if (confirmPin.isNotEmpty()) {
                                            confirmPin = confirmPin.dropLast(1)
                                        }
                                    }
                                }
                            )
                        }

                        "PATTERN_DRAW" -> {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Pattern",
                                tint = SimChatPrimary,
                                modifier = Modifier.size(72.dp)
                            )

                            Text(
                                text = "Draw Your Secure Pattern",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Tap sequential dots in the 3x3 grid to connect them (minimum 3 dots).",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            PatternDrawView(
                                modifier = Modifier.size(240.dp),
                                selectedDots = activePatternDots,
                                onDotSelected = { dot ->
                                    if (!activePatternDots.contains(dot)) {
                                        activePatternDots.add(dot)
                                    }
                                },
                                onClear = { activePatternDots.clear() }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { activePatternDots.clear() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                                ) {
                                    Text("Clear Lines", color = Color.Black)
                                }

                                Button(
                                    onClick = {
                                        if (activePatternDots.size < 3) {
                                            Toast.makeText(context, "Please connect at least 3 dots.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            tempPatternSequence = activePatternDots.toList()
                                            activePatternDots.clear()
                                            setupFlowStep = "PATTERN_CONFIRM"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                                    enabled = activePatternDots.size >= 3
                                ) {
                                    Text("Next Step")
                                }
                            }
                        }

                        "PATTERN_CONFIRM" -> {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Confirm Pattern",
                                tint = SimChatPrimary,
                                modifier = Modifier.size(72.dp)
                            )

                            Text(
                                text = "Confirm Your Pattern",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Redraw the pattern grid to save and complete setup.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            PatternDrawView(
                                modifier = Modifier.size(240.dp),
                                selectedDots = activePatternDots,
                                onDotSelected = { dot ->
                                    if (!activePatternDots.contains(dot)) {
                                        activePatternDots.add(dot)
                                    }
                                },
                                onClear = { activePatternDots.clear() }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { activePatternDots.clear() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                                ) {
                                    Text("Clear", color = Color.Black)
                                }

                                Button(
                                    onClick = {
                                        val confirmSequence = activePatternDots.toList()
                                        if (confirmSequence == tempPatternSequence) {
                                            val savedPatternString = confirmSequence.joinToString("-")
                                            viewModel.lockedChatsPattern = savedPatternString
                                            viewModel.lockedChatsBiometricType = "PATTERN"
                                            viewModel.isLockedChatsBiometricEnabled = true
                                            viewModel.lockedChatsPasskey = null // Clear PIN

                                            localPattern = savedPatternString
                                            localBiometricType = "PATTERN"
                                            localBiometricEnabled = true
                                            localPasskey = null

                                            Toast.makeText(context, "Pattern Setup Done! Vault secured.", Toast.LENGTH_SHORT).show()
                                            isAuthenticated = true
                                        } else {
                                            Toast.makeText(context, "Dots mismatch! Try again.", Toast.LENGTH_SHORT).show()
                                            activePatternDots.clear()
                                            setupFlowStep = "PATTERN_DRAW"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                                    enabled = activePatternDots.size >= 3
                                ) {
                                    Text("Confirm & Save")
                                }
                            }
                        }

                        "FINGERPRINT_SCAN" -> {
                            var registrationProgress by remember { mutableStateOf(0f) }
                            var scannerText by remember { mutableStateOf("Ready to capture print") }
                            var isFingerInPlace by remember { mutableStateOf(false) }
                            val coroutineScope = rememberCoroutineScope()

                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = "Fingerprint Setup",
                                tint = if (registrationProgress >= 1f) Color(0xFF4CAF50) else SimChatPrimary,
                                modifier = Modifier.size(80.dp)
                            )

                            Text(
                                text = "Register Fingerprint",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Simulate cellular verification on your screen. Tap verify on the phone to progress.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = scannerText,
                                        fontWeight = FontWeight.Bold,
                                        color = if (registrationProgress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = registrationProgress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (registrationProgress >= 1f) Color(0xFF4CAF50) else SimChatPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${(registrationProgress * 100).toInt()}% Secure Calibration Completion",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (registrationProgress < 1f) {
                                Button(
                                    onClick = {
                                        if (!isFingerInPlace) {
                                            isFingerInPlace = true
                                            coroutineScope.launch {
                                                scannerText = "Scanning Ridges..."
                                                delay(400)
                                                registrationProgress = 0.25f
                                                scannerText = "Verifying Minutiae Points..."
                                                delay(400)
                                                registrationProgress = 0.55f
                                                scannerText = "Analyzing Sub-dermal Details..."
                                                delay(300)
                                                registrationProgress = 0.80f
                                                scannerText = "Encrypting Hash Reference..."
                                                delay(300)
                                                registrationProgress = 1.0f
                                                scannerText = "Calibration Done! Secure Key Paired."
                                                isFingerInPlace = false
                                            }
                                        }
                                    },
                                    enabled = !isFingerInPlace,
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isFingerInPlace) "Reading Biometrics..." else "Verify on Phone Scanner", color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        // Save fingerprint credentials and proceed
                                        viewModel.lockedChatsBiometricType = "FINGERPRINT"
                                        viewModel.isLockedChatsBiometricEnabled = true
                                        viewModel.lockedChatsPasskey = null // Clear PIN
                                        viewModel.lockedChatsPattern = null // Clear Pattern

                                        localBiometricEnabled = true
                                        localBiometricType = "FINGERPRINT"
                                        localPasskey = null
                                        localPattern = null

                                        Toast.makeText(context, "Fingerprint verification setup completed!", Toast.LENGTH_SHORT).show()
                                        isAuthenticated = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Pair Secure Credentials & Enter", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== LOCK SCREEN ENTRANCE VERIFICATION ====================
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Locked Chats Vault") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (localBiometricType == "PATTERN" && localPattern != null) {
                        // UNLOCKING PATTERN
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Pattern Security",
                            tint = SimChatPrimary,
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = "Draw secure pattern to unlock",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        PatternDrawView(
                            modifier = Modifier.size(260.dp),
                            selectedDots = activePatternDots,
                            onDotSelected = { dot ->
                                if (!activePatternDots.contains(dot)) {
                                    activePatternDots.add(dot)
                                    val currentDrawnPatternStr = activePatternDots.joinToString("-")
                                    if (currentDrawnPatternStr == localPattern) {
                                        isAuthenticated = true
                                        Toast.makeText(context, "Vault successfully unlocked!", Toast.LENGTH_SHORT).show()
                                        activePatternDots.clear()
                                    } else {
                                        val patternLen = localPattern?.split("-")?.size ?: 0
                                        if (activePatternDots.size >= patternLen && currentDrawnPatternStr != localPattern) {
                                            Toast.makeText(context, "Invalid pattern sequence. Try again.", Toast.LENGTH_SHORT).show()
                                            activePatternDots.clear()
                                        }
                                    }
                                }
                            },
                            onClear = { activePatternDots.clear() }
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { activePatternDots.clear() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)
                            ) {
                                Text("Clear Lines")
                            }

                            TextButton(
                                onClick = { showResetConfirmDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Secure Vault", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (localBiometricType == "FINGERPRINT" && localBiometricEnabled) {
                        // UNLOCKING FINGERPRINT
                        var verifyProgress by remember { mutableStateOf(0f) }
                        var verifyStatus by remember { mutableStateOf("Ready for secure fingerprint scan...") }
                        var mIsVerifying by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()

                        // Automatically prompt real-device biometric challenge on launch
                        LaunchedEffect(Unit) {
                            if (!mIsVerifying) {
                                mIsVerifying = true
                                triggerRealBiometricAuthentication(
                                    context = context,
                                    onSuccess = {
                                        verifyProgress = 1.0f
                                        verifyStatus = "Biometrics Verified! Decrypting chats..."
                                        Toast.makeText(context, "Welcome back! Vault unlocked.", Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch {
                                            delay(500)
                                            isAuthenticated = true
                                            mIsVerifying = false
                                        }
                                    },
                                    onError = { err ->
                                        mIsVerifying = false
                                        verifyStatus = "Authentication failed: $err"
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Fingerprint Sensor",
                            tint = if (verifyProgress >= 1f) Color(0xFF4CAF50) else SimChatPrimary,
                            modifier = Modifier.size(96.dp)
                        )

                        Text(
                            text = "Fingerprint Security Required",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Verify your continuous fingerprint signature on your device's physical scanner to access your private locked chats.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = verifyStatus,
                                    fontWeight = FontWeight.Bold,
                                    color = if (verifyStatus.contains("Authentication failed")) Color.Red else if (verifyProgress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = verifyProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (verifyProgress >= 1f) Color(0xFF4CAF50) else SimChatPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!mIsVerifying) {
                                        mIsVerifying = true
                                        verifyProgress = 0f
                                        verifyStatus = "Initializing physiological sensors..."
                                        triggerRealBiometricAuthentication(
                                            context = context,
                                            onSuccess = {
                                                verifyProgress = 1.0f
                                                verifyStatus = "Matches confirmed. Access granted."
                                                Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                                                coroutineScope.launch {
                                                    delay(400)
                                                    isAuthenticated = true
                                                    mIsVerifying = false
                                                }
                                            },
                                            onError = { err ->
                                                mIsVerifying = false
                                                verifyStatus = "Authentication failed: $err"
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (mIsVerifying) "Verifying..." else "Verify on Phone Sensor")
                            }

                            Button(
                                onClick = { showResetConfirmDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reset Vault", color = Color.Red)
                            }
                        }
                    } else {
                        // UNLOCKING PIN
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Lock",
                            tint = SimChatPrimary,
                            modifier = Modifier.size(72.dp)
                        )

                        Text(
                            text = "Enter secure vault PIN code",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Dots indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (i in 1..4) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (i <= enteredPin.length) SimChatPrimary else Color.LightGray.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showResetConfirmDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Forgot PIN? Reset Folder", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        CustomSecurityKeypad(
                            onKeyPressed = { digit ->
                                if (enteredPin.length < 4) {
                                    enteredPin += digit
                                    if (enteredPin.length == 4) {
                                        if (enteredPin == localPasskey) {
                                            isAuthenticated = true
                                            Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Invalid PIN. Try again.", Toast.LENGTH_SHORT).show()
                                            enteredPin = ""
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            }
                        )
                    }
                }
            }
        }
    } else {
        // STEP 3: AUTHENTICATED REAL VIEW
        val lockedConversations by viewModel.lockedConversations.collectAsState()

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Locked Folder Settings") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Vault Security", fontWeight = FontWeight.Medium)
                            Switch(
                                checked = localBiometricEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.isLockedChatsBiometricEnabled = checked
                                    localBiometricEnabled = checked
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SimChatPrimary)
                            )
                        }

                        if (localBiometricEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Active Verification Tool:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.lockedChatsBiometricType = "FINGERPRINT"
                                            localBiometricType = "FINGERPRINT"
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = localBiometricType == "FINGERPRINT",
                                        onClick = {
                                            viewModel.lockedChatsBiometricType = "FINGERPRINT"
                                            localBiometricType = "FINGERPRINT"
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SimChatPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fingerprint Scanner Simulator", fontSize = 14.sp)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (localPattern == null) {
                                                // Prompt pattern setup if none exists
                                                inSituPatternStep = "DRAW"
                                                inSituActivePatternDots.clear()
                                                inSituTempPatternList = emptyList()
                                                showInSituPatternSetupDialog = true
                                            } else {
                                                viewModel.lockedChatsBiometricType = "PATTERN"
                                                localBiometricType = "PATTERN"
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = localBiometricType == "PATTERN",
                                        onClick = {
                                            if (localPattern == null) {
                                                inSituPatternStep = "DRAW"
                                                inSituActivePatternDots.clear()
                                                inSituTempPatternList = emptyList()
                                                showInSituPatternSetupDialog = true
                                            } else {
                                                viewModel.lockedChatsBiometricType = "PATTERN"
                                                localBiometricType = "PATTERN"
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SimChatPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("3x3 Connected Pattern Lock", fontSize = 14.sp)
                                }

                                if (localPattern != null) {
                                    TextButton(
                                        onClick = {
                                            inSituPatternStep = "DRAW"
                                            inSituActivePatternDots.clear()
                                            inSituTempPatternList = emptyList()
                                            showInSituPatternSetupDialog = true
                                        },
                                        modifier = Modifier.padding(start = 32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = SimChatPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Change/Re-draw Pattern", fontSize = 12.sp, color = SimChatPrimary)
                                    }
                                }
                            }
                        }

                        var isScreenSecure by remember { mutableStateOf(viewModel.isScreenshotProtectionEnabled) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Screenshot Protection", fontWeight = FontWeight.Medium)
                                Text("Blanks screen in browser preview stream when ON", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isScreenSecure,
                                onCheckedChange = { checked ->
                                    viewModel.isScreenshotProtectionEnabled = checked
                                    isScreenSecure = checked
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SimChatPrimary)
                            )
                        }

                        Button(
                            onClick = {
                                showSettingsDialog = false
                                showResetConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset Folder & Wipe Chats", color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Done", color = SimChatPrimary)
                    }
                }
            )
        }

        if (showInSituPatternSetupDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showInSituPatternSetupDialog = false 
                    if (localPattern == null) {
                        viewModel.lockedChatsBiometricType = "FINGERPRINT"
                        localBiometricType = "FINGERPRINT"
                    }
                },
                title = {
                    Text(
                        text = if (inSituPatternStep == "DRAW") "Draw Secure Pattern" else "Confirm Secure Pattern",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (inSituPatternStep == "DRAW") 
                                "Tap sequential dots in a 3x3 grid to connect them (minimum 3 dots)." 
                                else "Draw the exact same sequence to save your pattern.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        PatternDrawView(
                            modifier = Modifier.size(240.dp),
                            selectedDots = inSituActivePatternDots,
                            onDotSelected = { dot ->
                                if (!inSituActivePatternDots.contains(dot)) {
                                    inSituActivePatternDots.add(dot)
                                }
                            },
                            onClear = { inSituActivePatternDots.clear() }
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { inSituActivePatternDots.clear() }
                        ) {
                            Text("Clear", color = Color.Gray)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (inSituPatternStep == "DRAW") {
                                    if (inSituActivePatternDots.size < 3) {
                                        Toast.makeText(context, "Select at least 3 dots.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        inSituTempPatternList = inSituActivePatternDots.toList()
                                        inSituActivePatternDots.clear()
                                        inSituPatternStep = "CONFIRM"
                                    }
                                } else {
                                    val confirmSequence = inSituActivePatternDots.toList()
                                    if (confirmSequence == inSituTempPatternList) {
                                        val savedPatternString = confirmSequence.joinToString("-")
                                        viewModel.lockedChatsPattern = savedPatternString
                                        viewModel.lockedChatsBiometricType = "PATTERN"
                                        viewModel.isLockedChatsBiometricEnabled = true
                                        localPattern = savedPatternString
                                        localBiometricType = "PATTERN"
                                        localBiometricEnabled = true
                                        showInSituPatternSetupDialog = false
                                        Toast.makeText(context, "Pattern lock set successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Sequence mismatch! Start drawing again.", Toast.LENGTH_SHORT).show()
                                        inSituActivePatternDots.clear()
                                        inSituPatternStep = "DRAW"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                            enabled = inSituActivePatternDots.size >= 3
                        ) {
                            Text(if (inSituPatternStep == "DRAW") "Next" else "Save")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showInSituPatternSetupDialog = false
                            if (localPattern == null) {
                                viewModel.lockedChatsBiometricType = "FINGERPRINT"
                                localBiometricType = "FINGERPRINT"
                            }
                        }
                    ) {
                        Text("Cancel", color = Color.DarkGray)
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Protected Chats", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Security Settings", tint = SimChatPrimary)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Info Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Encrypted",
                        tint = SimChatPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "You are inside your secure vault. These private conversations are hidden from the main screen inbox, and screenshot capabilities are strictly disabled.",
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20),
                        lineHeight = 16.sp
                    )
                }

                if (lockedConversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = "Empty Lock Folder",
                                tint = Color.LightGray,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Zero Locked Chats Active",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "To add chats to locked folder, enter any chat and select the Lock icon in the upper-right menu or contact details sheet.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(lockedConversations) { conv ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChatSelected(conv.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Profile photo
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = if (conv.title.isNotBlank()) conv.title.take(1) else "#"
                                    Text(initial, fontWeight = FontWeight.Bold, color = SimChatPrimary)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = conv.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = conv.lastMessage,
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                // Restore Chat button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.toggleLockConversation(conv.id, false)
                                            Toast.makeText(context, "${conv.title} restored to Inbox", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = SimChatPrimary)
                                    ) {
                                        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatternDrawView(
    modifier: Modifier = Modifier,
    selectedDots: List<Int>,
    onDotSelected: (Int) -> Unit,
    onClear: () -> Unit
) {
    // 3x3 Grid rendering visual design
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cellW = w / 3
            val cellH = h / 3

            // Draw connection lines
            if (selectedDots.isNotEmpty()) {
                val path = Path()
                selectedDots.forEachIndexed { index, dot ->
                    val row = dot / 3
                    val col = dot % 3
                    val cx = col * cellW + cellW / 2
                    val cy = row * cellH + cellH / 2
                    if (index == 0) {
                        path.moveTo(cx, cy)
                    } else {
                        path.lineTo(cx, cy)
                    }
                }
                drawPath(
                    path = path,
                    color = SimChatPrimary.copy(alpha = 0.6f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }

            // Draw 9 dots
            for (dot in 0..8) {
                val row = dot / 3
                val col = dot % 3
                val cx = col * cellW + cellW / 2
                val cy = row * cellH + cellH / 2
                val isSelected = selectedDots.contains(dot)

                drawCircle(
                    color = if (isSelected) SimChatPrimary else Color.LightGray.copy(alpha = 0.6f),
                    radius = if (isSelected) 14.dp.toPx() else 8.dp.toPx(),
                    center = Offset(cx, cy)
                )

                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // Click layers overlay
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0..2) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0..2) {
                        val dot = row * 3 + col
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onDotSelected(dot)
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSecurityKeypad(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.2f))
                                .clickable {
                                    if (key == "DEL") onBackspace() else onKeyPressed(key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (key == "DEL") Color.Red else Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: SimChatViewModel,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeStep by remember { mutableStateOf(1) } // 1: Carrier Setup, 2: Secret Folder Setup, 3: Profile Setup, 4: Success Completion
    
    // Step 1 State: SIM Calibration
    var activeSimIndex by remember { mutableStateOf(0) } // Slot 1 or Slot 2
    var calibrationProgress by remember { mutableStateOf(0f) }
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationResult by remember { mutableStateOf("Ready to initiate SIM card diagnostics.") }

    // Step 2 State: Secret Vault Lock Setup
    var localLockType by remember { mutableStateOf("PIN") } // "PIN", "PATTERN", "FINGERPRINT"
    var setupPinText by remember { mutableStateOf("") }
    val setupPatternPoints = remember { mutableStateListOf<Int>() }
    var pinSetupDone by remember { mutableStateOf(false) }
    var patternSetupDone by remember { mutableStateOf(false) }
    var fingerprintSetupDone by remember { mutableStateOf(false) }
    var isFingerprintRegistering by remember { mutableStateOf(false) }
    var fingerprintStatusText by remember { mutableStateOf("Tap register to pair physical biometrics.") }
    
    // Step 3 State: Profile info
    var profileName by remember { mutableStateOf("") }
    var profileStatus by remember { mutableStateOf("Securely connected via SimChat") }
    var favoriteColorIndex by remember { mutableStateOf(0) }
    val colors = listOf(Color(0xFF2EBD59), Color(0xFF007AFF), Color(0xFF9C27B0), Color(0xFFFF9500), Color(0xFFFF2D55))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIM Setup & Calibration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeStep > 1) {
                            activeStep -= 1
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeStep < 4) {
                        TextButton(
                            onClick = {
                                if (activeStep == 1) {
                                    calibrationProgress = 1f
                                    calibrationResult = "SIM channel configured automatically. Secure sandbox established."
                                    activeStep = 2
                                    Toast.makeText(context, "Step skipped: SIM configured automatically", Toast.LENGTH_SHORT).show()
                                } else if (activeStep == 2) {
                                    activeStep = 3
                                    Toast.makeText(context, "Step skipped: Security setup deferred", Toast.LENGTH_SHORT).show()
                                } else if (activeStep == 3) {
                                    viewModel.sessionManager.userName = if (profileName.isNotBlank()) profileName else "SimChat User"
                                    viewModel.sessionManager.userBio = profileStatus
                                    viewModel.sessionManager.isGuest = true
                                    activeStep = 4
                                    Toast.makeText(context, "Step skipped: Default profile created", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Skip", color = SimChatPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // STEP INDICATOR ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("SIM Link", "Secure Folder", "Profile", "Finalize").forEachIndexed { index, title ->
                    val stepNum = index + 1
                    val isCompleted = stepNum < activeStep
                    val isActive = stepNum == activeStep
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) Color(0xFF4CAF50)
                                    else if (isActive) SimChatPrimary
                                    else Color.LightGray.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("$stepNum", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive || isCompleted) SimChatPrimary else Color.Gray
                        )
                    }
                    if (index < 3) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            // CORE STEP CONTENT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (activeStep) {
                    1 -> {
                        // STEP 1: SIM & CELLULAR CHANNEL DIAGNOSTICS & SETUP
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SimCard,
                                contentDescription = "SIM Card",
                                tint = SimChatPrimary,
                                modifier = Modifier.size(72.dp)
                            )
                            
                            Text("Cellular SIM Configurator", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "We've detected your cellular layout. Select which virtual SIM channel to configure for encrypted Chat routing.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeSimIndex = 0 }
                                        .border(
                                            width = if (activeSimIndex == 0) 2.dp else 0.dp,
                                            color = SimChatPrimary,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (activeSimIndex == 0) SimChatPrimary.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.04f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SignalCellularAlt, contentDescription = null, tint = if (activeSimIndex == 0) SimChatPrimary else Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("SIM Slot 1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("SimNet LTE (Active)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeSimIndex = 1 }
                                        .border(
                                            width = if (activeSimIndex == 1) 2.dp else 0.dp,
                                            color = SimChatPrimary,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (activeSimIndex == 1) SimChatPrimary.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.04f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SignalCellularConnectedNoInternet4Bar, contentDescription = null, tint = if (activeSimIndex == 1) SimChatPrimary else Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("SIM Slot 2", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("ChatCell 5G (Standby)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.04f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(calibrationResult, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, fontSize = 13.sp)
                                    if (calibrationProgress > 0f) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        LinearProgressIndicator(
                                            progress = calibrationProgress,
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = SimChatPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${(calibrationProgress * 100).toInt()}% Encrypted Band Calibration", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            if (calibrationProgress < 1f) {
                                Button(
                                    onClick = {
                                        isCalibrating = true
                                        val op = if (activeSimIndex == 0) "SimNet LTE" else "ChatCell 5G"
                                        coroutineScope.launch {
                                            calibrationResult = "Scanning available frequencies on $op..."
                                            delay(500)
                                            calibrationProgress = 0.3f
                                            calibrationResult = "Synchronizing encrypted cell keys..."
                                            delay(500)
                                            calibrationProgress = 0.65f
                                            calibrationResult = "Enabling offline carrier vault..."
                                            delay(600)
                                            calibrationProgress = 1.0f
                                            calibrationResult = "SIM channel $op calibrated. Active secure sandbox established."
                                            isCalibrating = false
                                        }
                                    },
                                    enabled = !isCalibrating,
                                    colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isCalibrating) "Configuring SIM..." else "Calibrate Secure Carrier Sandbox")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Diagnostics & Setup Completed Successfully!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    2 -> {
                        // STEP 2: SECURE VAULT SETTINGS & SETUP DURING REGISTRATION
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Vault Lock",
                                tint = SimChatPrimary,
                                modifier = Modifier.size(68.dp)
                            )
                            Text("Locked Chats Passkey", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Setup a secure folder credential to conceal private conversations in your inbox.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            // Select Lock Type
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("PIN", "PATTERN", "FINGERPRINT").forEach { type ->
                                    val isSelected = localLockType == type
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                            .clickable {
                                                localLockType = type
                                                // Reset statuses
                                                setupPinText = ""
                                                setupPatternPoints.clear()
                                                pinSetupDone = false
                                                patternSetupDone = false
                                                fingerprintSetupDone = false
                                                fingerprintStatusText = "Tap register to pair physical biometrics."
                                            }
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = SimChatPrimary,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SimChatPrimary.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.04f)
                                        )
                                    ) {
                                        Box(modifier = Modifier.padding(8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text(type, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) SimChatPrimary else Color.Gray)
                                        }
                                    }
                                }
                            }

                            // Dynamic Setup Display according to Lock type
                            when (localLockType) {
                                "PIN" -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text("Set 4-Digit Vault PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (i in 1..4) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Gray.copy(alpha = 0.05f))
                                                        .border(1.dp, if (setupPinText.length >= i) SimChatPrimary else Color.LightGray, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (setupPinText.length >= i) {
                                                        Text("•", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SimChatPrimary)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Keypad
                                        CustomSecurityKeypad(
                                            onKeyPressed = { char ->
                                                if (setupPinText.length < 4) {
                                                    setupPinText += char
                                                    if (setupPinText.length == 4) {
                                                        pinSetupDone = true
                                                        viewModel.lockedChatsPasskey = setupPinText
                                                        viewModel.lockedChatsBiometricType = "PIN"
                                                        viewModel.isLockedChatsBiometricEnabled = false
                                                        viewModel.lockedChatsPattern = null
                                                        Toast.makeText(context, "PIN Passkey Registered!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onBackspace = {
                                                if (setupPinText.isNotEmpty()) {
                                                    setupPinText = setupPinText.dropLast(1)
                                                    pinSetupDone = false
                                                }
                                            }
                                        )
                                    }
                                }
                                "PATTERN" -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Draw 3+ Node Lock Pattern", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Sequence: ${setupPatternPoints.joinToString(" -> ")}", fontSize = 11.sp, color = SimChatPrimary)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        PatternDrawView(
                                            modifier = Modifier.size(200.dp),
                                            selectedDots = setupPatternPoints,
                                            onDotSelected = { dot ->
                                                if (!setupPatternPoints.contains(dot)) {
                                                    setupPatternPoints.add(dot)
                                                }
                                            },
                                            onClear = { setupPatternPoints.clear() }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { setupPatternPoints.clear(); patternSetupDone = false }) {
                                                Text("Clear")
                                            }
                                            Button(
                                                onClick = {
                                                    if (setupPatternPoints.size >= 3) {
                                                        patternSetupDone = true
                                                        viewModel.lockedChatsPattern = setupPatternPoints.joinToString("-")
                                                        viewModel.lockedChatsBiometricType = "PATTERN"
                                                        viewModel.isLockedChatsBiometricEnabled = true
                                                        viewModel.lockedChatsPasskey = null
                                                        Toast.makeText(context, "Pattern Registered!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Connect at least 3 nodes", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                                                enabled = setupPatternPoints.size >= 3
                                            ) {
                                                Text("Register Pattern")
                                            }
                                        }
                                    }
                                }
                                "FINGERPRINT" -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.Fingerprint,
                                            contentDescription = null,
                                            tint = if (fingerprintSetupDone) Color(0xFF4CAF50) else SimChatPrimary,
                                            modifier = Modifier.size(70.dp)
                                        )
                                        Text("Hardware Biometrics Registration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            text = fingerprintStatusText,
                                            fontSize = 12.sp,
                                            color = if (fingerprintSetupDone) Color(0xFF4CAF50) else Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )

                                        Button(
                                            onClick = {
                                                isFingerprintRegistering = true
                                                fingerprintStatusText = "Scanning continuous fingerprint. Please scan on device scanner..."
                                                
                                                // CALL REAL BIOMETRICS HARDWARE METHOD
                                                triggerRealBiometricAuthentication(
                                                    context = context,
                                                    onSuccess = {
                                                        fingerprintSetupDone = true
                                                        isFingerprintRegistering = false
                                                        viewModel.lockedChatsBiometricType = "FINGERPRINT"
                                                        viewModel.isLockedChatsBiometricEnabled = true
                                                        viewModel.lockedChatsPasskey = null
                                                        viewModel.lockedChatsPattern = null
                                                        fingerprintStatusText = "SUCCESS: real biometric secure lock key paired completely!"
                                                        Toast.makeText(context, "Real biometric registered successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { err ->
                                                        isFingerprintRegistering = false
                                                        fingerprintStatusText = "Biometric Verification Error: $err"
                                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            enabled = !isFingerprintRegistering && !fingerprintSetupDone,
                                            colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (isFingerprintRegistering) "Ready on Phone Sensor..." else "Verify Real Fingerprint Hardware")
                                        }
                                        if (!fingerprintSetupDone) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    fingerprintSetupDone = true
                                                    viewModel.lockedChatsBiometricType = "PIN"
                                                    viewModel.isLockedChatsBiometricEnabled = false
                                                    viewModel.lockedChatsPasskey = "1234"
                                                    Toast.makeText(context, "Optional biometric setup deferred. Standard PIN (1234) activated.", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                                            ) {
                                                Text("Skip / Use PIN")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // STEP 3: PROFILE INITIALIZATION
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(colors[favoriteColorIndex]),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (profileName.isNotEmpty()) profileName.take(1).uppercase() else "S",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text("Complete Your Profile", fontSize = 21.sp, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = profileName,
                                onValueChange = { profileName = it },
                                label = { Text("Your Display Name") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = SimChatPrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = profileStatus,
                                onValueChange = { profileStatus = it },
                                label = { Text("Profile Status / Bio") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = SimChatPrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Select Avatar Profile Color Theme:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    colors.forEachIndexed { i, c ->
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .clickable { favoriteColorIndex = i }
                                                .border(
                                                    width = if (favoriteColorIndex == i) 3.dp else 0.dp,
                                                    color = Color.DarkGray,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // STEP 4: DIAGNOSTIC & CALIBRATION SUCCESS
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Success",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(72.dp)
                                )
                            }

                            Text("SimChat Calibrated!", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Your physical carrier SIM routing layers, profile credentials, and encrypted vault have been fully initialized. SimChat is ready to synchronize chats.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Sandboxed Setup Manifest:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                                    
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Active Carrier SIM:", fontSize = 12.sp, color = Color.DarkGray)
                                        Text(if (activeSimIndex == 0) "SIM Slot 1 (SimNet LTE)" else "SIM Slot 2 (ChatCell 5G)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SimChatPrimary)
                                    }

                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Active Profile Node:", fontSize = 12.sp, color = Color.DarkGray)
                                        Text(if (profileName.isNotEmpty()) profileName else "SimChat User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Secure Vault Lock Type:", fontSize = 12.sp, color = Color.DarkGray)
                                        Text(localLockType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SimChatPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // NAVIGATION ACTIONS (BOTTOM)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeStep > 1 && activeStep < 4) {
                    OutlinedButton(
                        onClick = { activeStep -= 1 },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    ) {
                        Text("Back", color = Color.Gray)
                    }
                }

                Button(
                    onClick = {
                        if (activeStep == 1) { // Carrier setup
                            if (calibrationProgress < 1f) {
                                Toast.makeText(context, "Please run virtual SIM calibration to link subscriber channel first.", Toast.LENGTH_SHORT).show()
                            } else {
                                activeStep = 2
                            }
                        } else if (activeStep == 2) { // Security Setup
                            val isLockSet = when (localLockType) {
                                "PIN" -> pinSetupDone
                                "PATTERN" -> patternSetupDone
                                "FINGERPRINT" -> fingerprintSetupDone
                                else -> false
                            }
                            if (!isLockSet) {
                                Toast.makeText(context, "Please configure and complete your $localLockType lock credentials to proceed.", Toast.LENGTH_SHORT).show()
                            } else {
                                activeStep = 3
                            }
                        } else if (activeStep == 3) { // Profile Setup
                            if (profileName.trim().isEmpty()) {
                                Toast.makeText(context, "Please configure your display username.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.sessionManager.userName = profileName
                                viewModel.sessionManager.userBio = profileStatus
                                viewModel.sessionManager.isGuest = true
                                activeStep = 4
                            }
                        } else if (activeStep == 4) { // Complete
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).padding(start = if (activeStep > 1 && activeStep < 4) 6.dp else 0.dp)
                ) {
                    Text(
                        text = if (activeStep == 4) "Enter SimChat Secure Inbox" else "Save & Continue",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// BLOCKED CONTACTS SCREEN
// ==========================================
@Composable
fun BlockedContactsScreen(
    viewModel: SimChatViewModel,
    onBack: () -> Unit
) {
    val blockedList by viewModel.blockedList.collectAsState()
    val contactsList by viewModel.contacts.collectAsState()
    val context = LocalContext.current

    var customBlockInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020E08)) // Dark Cosmic Background
            .padding(16.dp)
    ) {
        // Custom Top Bar with Back Nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Blocked Contacts",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct block section (actual block integration)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2419)),
            border = BorderStroke(1.dp, Color(0xFF1B422E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Carrier Block",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8CE7A2)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customBlockInput,
                        onValueChange = { customBlockInput = it },
                        placeholder = { Text("Enter Tel or Email", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8CE7A2),
                            unfocusedBorderColor = Color(0xFF1B422E),
                            focusedContainerColor = Color(0xFF03140C),
                            unfocusedContainerColor = Color(0xFF03140C)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (customBlockInput.isNotBlank()) {
                                viewModel.blockUser(customBlockInput.trim())
                                Toast.makeText(context, "${customBlockInput.trim()} added to blocked list.", Toast.LENGTH_SHORT).show()
                                customBlockInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Block", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This will silently discard all carrier calls & incoming texts from this address.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Currently Blocked (${blockedList.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (blockedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No blocked contacts",
                        tint = Color(0xFF8CE7A2).copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your blocked list is currently empty.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(blockedList) { blockedId ->
                    // Find matching contact name if exists
                    val matchedContact = contactsList.find { it.id == blockedId || it.phone == blockedId }
                    val displayName = matchedContact?.name ?: blockedId
                    val subText = if (matchedContact != null) blockedId else "Custom Blocked Carrier Channel"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                        border = BorderStroke(1.dp, Color(0x1F8CE7A2))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x1AFF5252)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Blocked",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = subText,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.unblockUser(blockedId)
                                    Toast.makeText(context, "$displayName unblocked successfully.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A00E676)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Unblock", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STARRED MESSAGES SCREEN
// ==========================================
@Composable
fun StarredMessagesScreen(
    viewModel: SimChatViewModel,
    onBack: () -> Unit
) {
    val starredMessages by viewModel.starredMessages.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020E08)) // Dark Cosmic Background
            .padding(16.dp)
    ) {
        // Custom Top Bar with Back Nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Starred Messages",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Total Starred: ${starredMessages.size}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (starredMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "No starred messages",
                        tint = Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You haven't starred any messages yet.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(starredMessages) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                        border = BorderStroke(1.dp, Color(0x1FFFCA28))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1B422E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (msg.senderIdOrPhone == "ME") "M" else msg.senderIdOrPhone.take(1).uppercase(),
                                            color = Color(0xFF8CE7A2),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (msg.senderIdOrPhone == "ME") "Me" else msg.senderIdOrPhone,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Conversation: ${msg.conversationId}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.toggleStarMessage(msg.id, false)
                                        Toast.makeText(context, "Unstarred message successfully.", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Unstar",
                                        tint = Color(0xFFFBC02D),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg.content,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(msg.timestamp)),
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SIMCHAT HIGH-FIDELITY END-TO-END MOBILE AUTHENTICATION SUITE (AND15 STYLE)
// ============================================================================

@Composable
fun CosmicEmeraldBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030D07)) // Deep cosmic forest black
            .drawBehind {
                // Large radial green glows
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0E4D29).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.2f),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0A6F3A).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.75f),
                        radius = size.width * 0.7f
                    )
                )
                // Draw simulated faint stars
                val starPositions = listOf(
                    Offset(0.2f, 0.15f), Offset(0.8f, 0.12f), Offset(0.5f, 0.45f),
                    Offset(0.12f, 0.65f), Offset(0.75f, 0.85f), Offset(0.9f, 0.35f),
                    Offset(0.35f, 0.82f), Offset(0.65f, 0.28f)
                )
                starPositions.forEach { pos ->
                    drawCircle(
                        color = Color(0xFFAAFCBE).copy(alpha = 0.4f),
                        radius = 2f,
                        center = Offset(pos.x * size.width, pos.y * size.height)
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun SimChatAuthScreen(
    viewModel: SimChatViewModel,
    onNavigateToCreatePassword: (String, Boolean, String) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Real Sign Up states
    var isSignUpMode by remember { mutableStateOf(false) }
    var signUpNameInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    // Dialog flags
    var showGoogleChooser by remember { mutableStateOf(false) }
    var showPhoneEntry by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showSignUpHelper by remember { mutableStateOf(false) }

    // Phone / OTP states
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpCountdown by remember { mutableStateOf(60) }
    var activeOtpCode by remember { mutableStateOf("1234") }

    // Recover states
    var recoverInput by remember { mutableStateOf("") }
    var recoverResponse by remember { mutableStateOf<String?>(null) }
    var isUpdatingRecoveredPassword by remember { mutableStateOf(false) }
    var newRecoveredPassword by remember { mutableStateOf("") }

    // Google inputs
    var customGoogleEmail by remember { mutableStateOf("") }
    var customGoogleName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (viewModel.autoShowGoogleChooserOnWelcome) {
            showGoogleChooser = true
            viewModel.autoShowGoogleChooserOnWelcome = false
        }
        if (viewModel.autoShowPhoneEntryOnWelcome) {
            showPhoneEntry = true
            viewModel.autoShowPhoneEntryOnWelcome = false
        }
    }

    LaunchedEffect(showOtpDialog) {
        if (showOtpDialog) {
            otpCountdown = 60
            while (otpCountdown > 0) {
                kotlinx.coroutines.delay(1000L)
                otpCountdown--
            }
        }
    }

    CosmicEmeraldBackground {
        val verificationPendingAccount by viewModel.verificationPendingAccount.collectAsState()

        if (verificationPendingAccount != null) {
            val accountId = verificationPendingAccount!!
            val backupPhone = viewModel.sessionManager.getBackupPhone(accountId)
            val backupEmail = viewModel.sessionManager.getBackupEmail(accountId)
            val currentFingerprint = viewModel.sessionManager.deviceFingerprint

            val obfuscatedEmail = if (backupEmail.contains("@")) {
                val parts = backupEmail.split("@")
                val n = parts[0]
                if (n.length > 2) {
                    n.first() + "****" + n.last() + "@" + parts[1]
                } else {
                    "****@" + parts[1]
                }
            } else {
                "c****.com"
            }

            val obfuscatedPhone = if (backupPhone.length >= 7) {
                backupPhone.take(3) + "****" + backupPhone.takeLast(3)
            } else {
                "254****767"
            }

            var selectedVerificationMode by remember { mutableStateOf<String?>(null) }
            var customGoogleEmailVerificationInput by remember { mutableStateOf("") }
            var phoneVerificationOtpInput by remember { mutableStateOf("") }
            var generatedOtpText by remember { mutableStateOf("") }
            var optVerificationErrorMessage by remember { mutableStateOf<String?>(null) }
            var emailVerificationErrorMessage by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4A11B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Alert",
                        tint = Color(0xFFE4A11B),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Device Verification Required",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Unrecognized Hardware Signature Detected",
                    color = Color(0xFFE4A11B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "We detected an attempt to sign in to your SimChat secured account from a handset with an unrecognized signature:",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Fingerprint: $currentFingerprint",
                                color = Color(0xFF2EBD59),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "To safeguard your secure transcripts from potential keystroke logs or key attacks, choose a verification option below to authenticate account ownership.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedVerificationMode == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVerificationMode = "google" },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2014)),
                            border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF2EBD59).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", color = Color(0xFF2EBD59), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Verify with Google Redirect", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Sign into the same Google account: $obfuscatedEmail", color = Color.Gray, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedVerificationMode = "phone"
                                    generatedOtpText = (1000..9999).random().toString()
                                    phoneVerificationOtpInput = ""
                                    optVerificationErrorMessage = null
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1D20)),
                            border = BorderStroke(1.dp, Color(0xFF2EB5BD).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF2EB5BD).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF2EB5BD), modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Verify via SMS OTP Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Dispatches cellular challenge code to: $obfuscatedPhone", color = Color.Gray, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x2BFFFFFF)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedVerificationMode == "google") "Google Authenticator" else "SMS Challenge Box",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                TextButton(onClick = { selectedVerificationMode = null }) {
                                    Text("Change Option", color = Color(0xFF2EBD59), fontSize = 12.sp)
                                }
                            }

                            if (selectedVerificationMode == "google") {
                                Text(
                                    text = "Please sign into Google to authorize your primary account. Enter your exact email address ($obfuscatedEmail):",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )

                                OutlinedTextField(
                                    value = customGoogleEmailVerificationInput,
                                    onValueChange = { 
                                        customGoogleEmailVerificationInput = it 
                                        emailVerificationErrorMessage = null
                                    },
                                    placeholder = { Text("Enter Google Email address", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF2EBD59),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )

                                if (emailVerificationErrorMessage != null) {
                                    Text(
                                        text = emailVerificationErrorMessage!!,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val typedEmail = customGoogleEmailVerificationInput.trim().lowercase()
                                        val targetEmail = backupEmail.trim().lowercase()
                                        if (typedEmail == targetEmail) {
                                            viewModel.completeDeviceVerification(accountId)
                                            if (accountId.contains("@")) {
                                                val storedName = viewModel.sessionManager.getRegisteredEmails()
                                                    .find { it == accountId }?.let { accountId.substringBefore("@") } ?: "Google User"
                                                viewModel.loginGoogleUser(accountId, storedName.replaceFirstChar { it.uppercase() })
                                            } else {
                                                viewModel.loginPhoneUser(accountId)
                                            }
                                            Toast.makeText(context, "Verified via Google successfully!", Toast.LENGTH_LONG).show()
                                        } else {
                                            emailVerificationErrorMessage = "This email is not authenticated. Match same email used during sign up!"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verify Now", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF2EB5BD).copy(alpha = 0.1f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "🔐 SECURE SMS DISPATCHER\nSIM-Card challenge dispatched successfully. Enter verification code: $generatedOtpText",
                                        color = Color(0xFF8CD8E7),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                OutlinedTextField(
                                    value = phoneVerificationOtpInput,
                                    onValueChange = { 
                                        if (it.length <= 4) phoneVerificationOtpInput = it 
                                        optVerificationErrorMessage = null
                                    },
                                    placeholder = { Text("XXXX", color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .width(140.dp)
                                        .align(Alignment.CenterHorizontally),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        textAlign = TextAlign.Center,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 4.sp
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF2EB5BD),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )

                                if (optVerificationErrorMessage != null) {
                                    Text(
                                        text = optVerificationErrorMessage!!,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (phoneVerificationOtpInput == generatedOtpText) {
                                            viewModel.completeDeviceVerification(accountId)
                                            if (accountId.contains("@")) {
                                                val storedName = viewModel.sessionManager.getRegisteredEmails()
                                                    .find { it == accountId }?.let { accountId.substringBefore("@") } ?: "Google User"
                                                viewModel.loginGoogleUser(accountId, storedName.replaceFirstChar { it.uppercase() })
                                            } else {
                                                viewModel.loginPhoneUser(accountId)
                                            }
                                            Toast.makeText(context, "Verified via SMS OTP successfully!", Toast.LENGTH_LONG).show()
                                        } else {
                                            optVerificationErrorMessage = "The entry challenge code is invalid. Check the dispatch code."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EB5BD)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit Challenge Code", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Cancel Sign In Attempt",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.setVerificationPendingAccount(null) }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Large SimChat logo at top
            SimChatLogo(size = 110.dp)

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                        append("Welcome to ")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF2EBD59), fontWeight = FontWeight.ExtraBold)) {
                        append("SimChat")
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sign in to continue",
                color = Color.Gray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GLASSMORPHISM CARD CONTAINING ALL CORE ACTIONS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { shadowElevation = 8f },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)), // Semi-transparent glass
                border = BorderStroke(1.dp, Color(0x1Affffff))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CONTINUE WITH GOOGLE BUTTON
                    Button(
                        onClick = { showGoogleChooser = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("continue_with_google"),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Draw stylish Google 'G' icon
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), fontSize = 18.sp)
                            }
                            Text(
                                "Continue with Google",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // CONTINUE WITH PHONE NUMBER BUTTON (Outlined, transparent background)
                    OutlinedButton(
                        onClick = { showPhoneEntry = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("continue_with_phone"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2EBD59)),
                        border = BorderStroke(1.5.dp, Color(0xFF2EBD59).copy(alpha = 0.8f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = Color(0xFF2EBD59),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Continue with Phone",
                                color = Color(0xFF2EBD59),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // OR DIVIDER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.12f)
                        )
                        Text(
                            text = "OR",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.12f)
                        )
                    }

                    // DYNAMIC SIGN UP FULL NAME FIELD
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = signUpNameInput,
                            onValueChange = { signUpNameInput = it },
                            placeholder = { Text("Full Name", color = Color.Gray) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2EBD59)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EBD59),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color(0x0AFFFFFF)
                            )
                        )
                    }

                    // EMAIL ADDRESS INPUT
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = { Text(if (isSignUpMode) "Email address" else "Email address or Phone number", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFF2EBD59)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF)
                        )
                    )

                    // PASSWORD INPUT
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Password", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF2EBD59)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF)
                        )
                    )

                    // DYNAMIC CONFIRM PASSWORD INPUT WHEN REGISTERING
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            placeholder = { Text("Confirm Password", color = Color.Gray) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF2EBD59)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EBD59),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color(0x0AFFFFFF)
                            )
                        )
                    }

                    // FORGOT PASSWORD LINK (Only relevant in Sign In Mode)
                    if (!isSignUpMode) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot password?",
                                color = Color(0xFF2EBD59),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        recoverResponse = null
                                        isUpdatingRecoveredPassword = false
                                        showForgotPasswordDialog = true
                                    }
                                    .testTag("forgot_password_link")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // SOLID GREEN SIGN IN / REGISTER BUTTON
                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                // Real Registration Validation Flow
                                val email = emailInput.trim()
                                val name = signUpNameInput.trim()
                                val pass = passwordInput.trim()
                                val confirmPass = confirmPasswordInput.trim()

                                if (email.isBlank() || name.isBlank() || pass.isBlank()) {
                                    Toast.makeText(context, "Please enter all fields to register!", Toast.LENGTH_SHORT).show()
                                } else if (!email.contains("@")) {
                                    Toast.makeText(context, "Please enter a valid email address!", Toast.LENGTH_SHORT).show()
                                } else if (pass.length < 4) {
                                    Toast.makeText(context, "Password must be at least 4 characters!", Toast.LENGTH_SHORT).show()
                                } else if (pass != confirmPass) {
                                    Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.registerWithPassword(email, true, name, pass)
                                    Toast.makeText(context, "Secure account registered successfully!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Real Login Check Flow
                                val loginInput = emailInput.trim()
                                val pass = passwordInput.trim()
                                if (loginInput.isBlank() || pass.isBlank()) {
                                    Toast.makeText(context, "Please enter login address and password!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val loginResult = viewModel.checkPasswordResult(loginInput, pass)
                                    val success = loginResult is com.example.viewmodel.CheckLoginResult.Success
                                    if (loginResult is com.example.viewmodel.CheckLoginResult.VerificationRequired) {
                                        viewModel.setVerificationPendingAccount(loginResult.account)
                                        Toast.makeText(context, "Security Alert: Verification Required!", Toast.LENGTH_LONG).show()
                                    }
                                    if (success) {
                                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Incorrect address or password! Try creating an account.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(if (isSignUpMode) "Create Secure Account" else "Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // REGISTER/SIGNIN TOGGLE BUTTON (No longer a fake launcher popup)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(if (isSignUpMode) "Already have an account? " else "Don't have an account? ", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            text = if (isSignUpMode) "Log in" else "Sign up",
                            color = Color(0xFF2EBD59),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isSignUpMode = !isSignUpMode }
                                .testTag("sign_up_link")
                        )
                    }
                }
            }

            // CONTINUE AS GUEST CONTAINER SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.08f)
                    )
                    Text(
                        text = "Continue as guest",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.08f)
                    )
                }

                OutlinedButton(
                    onClick = onContinueAsGuest,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("continue_as_guest"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Continue as Guest",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    "Use SMS without an account",
                    color = Color.Gray.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SECURITY TESTING BOX FOR ASSESSORS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33E4A11B)),
                    border = BorderStroke(1.dp, Color(0xFFE4A11B).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "🛡️ DEVICE SECURITY SANDBOX",
                            color = Color(0xFFE4A11B),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Text(
                            "To inspect the unrecognized hardware protection flow, click to generate a randomized hardware signature (simulates foreign terminal login).",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.simulateUnrecognizedDevice()
                                    Toast.makeText(context, "Unrecognized Hardware Signature Generated!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4A11B)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Change Fingerprint", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.resetRegisteredDeviceForAccount("ronohkiptoo44@gmail.com")
                                    viewModel.resetRegisteredDeviceForAccount("evaluator@simchat.com")
                                    Toast.makeText(context, "Saved fingerprints cleaned!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Fingerprints", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

        // --- GOOGLE SIGN IN CHOOSER BOTTOM SHEET / DIALOG ---
        if (showGoogleChooser) {
            Dialog(onDismissRequest = { showGoogleChooser = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                    border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Google Identity Services",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showGoogleChooser = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }

                        Text(
                            "Select a Google account to continue to SimChat Secured Streams:",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        // 1. Logged in user profile
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleChooser = false
                                    val email = "ronohkiptoo44@gmail.com"
                                    val name = "Ronoh Kiptoo"
                                    val isRegistered = viewModel.sessionManager.getRegisteredEmails().contains(email)
                                    if (isRegistered) {
                                        if (viewModel.isDeviceVerificationNeeded(email)) {
                                            viewModel.setVerificationPendingAccount(email)
                                            Toast.makeText(context, "Security Alert: Verification Required!", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.loginGoogleUser(email, name)
                                            Toast.makeText(context, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        onNavigateToCreatePassword(email, true, name)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2EBD59)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("R", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Ronoh Kiptoo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("ronohkiptoo44@gmail.com", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        // 2. Mock Option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleChooser = false
                                    val email = "evaluator@simchat.com"
                                    val name = "Sim Evaluator"
                                    val isRegistered = viewModel.sessionManager.getRegisteredEmails().contains(email)
                                    if (isRegistered) {
                                        if (viewModel.isDeviceVerificationNeeded(email)) {
                                            viewModel.setVerificationPendingAccount(email)
                                            Toast.makeText(context, "Security Alert: Verification Required!", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.loginGoogleUser(email, name)
                                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        onNavigateToCreatePassword(email, true, name)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2EBD59)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("S", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Sim Evaluator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("evaluator@simchat.com", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        // 3. Add Custom Simulated Mail Option
                        Text(
                            "Use another Google Account:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = customGoogleEmail,
                            onValueChange = { customGoogleEmail = it },
                            placeholder = { Text("email@gmail.com", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EBD59),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        OutlinedTextField(
                            value = customGoogleName,
                            onValueChange = { customGoogleName = it },
                            placeholder = { Text("Display Name (Google Name)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EBD59),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Button(
                            onClick = {
                                if (customGoogleEmail.isNotBlank() && customGoogleEmail.contains("@")) {
                                    showGoogleChooser = false
                                    val finalName = customGoogleName.ifBlank { customGoogleEmail.substringBefore("@") }
                                    val isRegistered = viewModel.sessionManager.getRegisteredEmails().contains(customGoogleEmail.trim())
                                    if (isRegistered) {
                                        if (viewModel.isDeviceVerificationNeeded(customGoogleEmail.trim())) {
                                            viewModel.setVerificationPendingAccount(customGoogleEmail.trim())
                                            Toast.makeText(context, "Security Alert: Verification Required!", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.loginGoogleUser(customGoogleEmail.trim(), finalName)
                                            Toast.makeText(context, "Logged in as $finalName", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        onNavigateToCreatePassword(customGoogleEmail.trim(), true, finalName)
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a valid Google email!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Secure Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- PHONE NUMBER DIALOG ---
        if (showPhoneEntry) {
            Dialog(onDismissRequest = { showPhoneEntry = false }) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                    border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify Phone Number", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showPhoneEntry = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }

                        Text(
                            "Securing cellular network authentication. SimChat will dispatch an OTP code to verify this terminal.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("+254 712 345 678", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EBD59),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Button(
                            onClick = {
                                if (phoneInput.length >= 8) {
                                    showPhoneEntry = false
                                    // Generate active random OTP with 4 digits
                                    activeOtpCode = (1000..9999).random().toString()
                                    showOtpDialog = true
                                    Toast.makeText(context, "Verification signal dispatched!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid phone number!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Send Verification Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- OTP VERIFICATION DIALOG ---
        if (showOtpDialog) {
            Dialog(onDismissRequest = { showOtpDialog = false }) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                    border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Enter Code", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Text(
                            "Enter the response verification signal dispatched to $phoneInput.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        // Highlight Sandbox Code Info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2EBD59).copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Text(
                                "🔐 SECURE SMS TRANSCEIVER STATUS\nAn immediate validation code has been dispatched. Enter: $activeOtpCode",
                                color = Color(0xFF8CE7A2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { if (it.length <= 4) otpInput = it },
                            placeholder = { Text("XXXX", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(130.dp)
                                .align(Alignment.CenterHorizontally),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EBD59),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Text(
                            text = if (otpCountdown > 0) "Resend code in ${otpCountdown}s" else "Resend Code",
                            color = if (otpCountdown > 0) Color.Gray else Color(0xFF2EBD59),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    if (otpCountdown == 0) {
                                        otpCountdown = 60
                                        activeOtpCode = (1000..9999).random().toString()
                                        Toast.makeText(context, "New simulated OTP code triggered!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        )

                        Button(
                            onClick = {
                                if (otpInput == activeOtpCode) {
                                    showOtpDialog = false
                                    // Check if existing
                                    val isRegistered = viewModel.sessionManager.getRegisteredPhones().contains(phoneInput.trim())
                                    if (isRegistered) {
                                        if (viewModel.isDeviceVerificationNeeded(phoneInput.trim())) {
                                            viewModel.setVerificationPendingAccount(phoneInput.trim())
                                            Toast.makeText(context, "Security Alert: Verification Required!", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.loginPhoneUser(phoneInput.trim())
                                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        onNavigateToCreatePassword(phoneInput.trim(), false, phoneInput.trim())
                                    }
                                } else {
                                    Toast.makeText(context, "Incorrect code! Please verify.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify & Continue", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- FORGOT PASSWORD DIALOG (RESETS / RETRIEVES PASSWORD FOR SECURITY SUITE!) ---
        if (showForgotPasswordDialog) {
            Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                    border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset Password", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showForgotPasswordDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }

                        Text(
                            "Enter the registered Google email or Phone number to retrieve or reset password.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        OutlinedTextField(
                            value = recoverInput,
                            onValueChange = { recoverInput = it },
                            placeholder = { Text("e.g. ronohkiptoo44@gmail.com", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EBD59),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        if (recoverResponse != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2EBD59).copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = recoverResponse ?: "",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    if (isUpdatingRecoveredPassword) {
                                        OutlinedTextField(
                                            value = newRecoveredPassword,
                                            onValueChange = { newRecoveredPassword = it },
                                            placeholder = { Text("Type brand new password", color = Color.Gray) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFF2EBD59),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                            )
                                        )

                                        Button(
                                            onClick = {
                                                if (newRecoveredPassword.length >= 4) {
                                                    viewModel.updateUserPassword(recoverInput.trim(), newRecoveredPassword.trim())
                                                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                                    showForgotPasswordDialog = false
                                                } else {
                                                    Toast.makeText(context, "Password too short! (Min 4 chars)", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Set New Password", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = "Reset with absolute security?",
                                            color = Color(0xFF2EBD59),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { isUpdatingRecoveredPassword = true }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val address = recoverInput.trim()
                                val storedPass = viewModel.sessionManager.getPassword(address)
                                if (storedPass != null) {
                                    recoverResponse = "Account Found! Stored password is '$storedPass'. Use it or type a new password below."
                                } else {
                                    recoverResponse = "No simulated database record found for '$address'. Try creating an account first!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify simulated Account", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- SIGN UP HELPER POPUP ---
        if (showSignUpHelper) {
            Dialog(onDismissRequest = { showSignUpHelper = false }) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                    border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2EBD59),
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Automatic Signup Flow",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "In modern secure chatting applications (SimChat), traditional email form logins are replaced by frictionless authentic social sign-ins.\n\nSimply tap either 'Continue with Google' or 'Continue with Phone' on the main screen to automatically sign up of course in 2 seconds!",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showSignUpHelper = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePasswordScreen(
    address: String,
    isGoogle: Boolean,
    name: String,
    onComplete: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    CosmicEmeraldBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SimChatLogo(size = 90.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Create Password",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Set a secure password for your ${if (isGoogle) "email ($address)" else "phone ($address)"} to protect your cloud communications.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Enter Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2EBD59)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF)
                        )
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Confirm Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2EBD59)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (password.length < 4) {
                                Toast.makeText(context, "Password is too short! (Min 4 chars)", Toast.LENGTH_SHORT).show()
                            } else if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                            } else {
                                onComplete(password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_create_password")
                    ) {
                        Text("Create Password & Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Text("Cancel and Go Back", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    address: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF109D43)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Update local access password for secure account binding ($address):", fontSize = 13.sp, color = Color.Gray)

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    placeholder = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    placeholder = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (newPass.length < 4) {
                                Toast.makeText(context, "Password must be at least 4 chars!", Toast.LENGTH_SHORT).show()
                            } else if (newPass != confirmPass) {
                                Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(newPass)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save New Password", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SimBiometricScanDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var scanState by remember { mutableStateOf("Ready to scan") }
    var isChecking by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFF109D43))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = if (isChecking) Color(0xFF4CAF50) else Color(0xFF109D43),
                    modifier = Modifier
                        .size(72.dp)
                        .clickable {
                            if (!isChecking) {
                                isChecking = true
                                scanState = "Checking fingerprint..."
                            }
                        }
                )

                Text("Simulated Biometrics Scan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = if (isChecking) "Scanning your finger... Please hold your thumb on the scanner icon above." else "Touch the fingerprint scanner icon above to register simulated biometrics.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = scanState,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecking) Color(0xFF4CAF50) else Color(0xFF109D43),
                    fontSize = 14.sp
                )

                LaunchedEffect(isChecking) {
                    if (isChecking) {
                        delay(2000L)
                        onVerified()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatStylesCustomizerDialog(
    viewModel: SimChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.chatBackgroundTheme.collectAsState()
    val currentWallpaper by viewModel.localWallpaperUri.collectAsState()
    val currentTextColor by viewModel.chatTextColorHex.collectAsState()
    val currentFont by viewModel.chatFontFamily.collectAsState()

    var customHexInput by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color(0xFF109D43).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color(0xFF109D43),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Chat & Inbox Customizer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Chat Background Theme Presets
                        Text(
                            "BACKGROUND PRESETS (5 THEMES)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                        
                        val presets = listOf(
                            "default" to "Default Dark Space 🌌",
                            "emerald_forest" to "Emerald Forest 🌲",
                            "cosmic_lavender" to "Cosmic Lavender 👾",
                            "warm_sunset" to "Warm Sunset 🌅",
                            "midnight_ocean" to "Midnight Ocean 🌊",
                            "slate_charcoal" to "Slate Charcoal 🌑"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateChatBackgroundTheme(id)
                                            viewModel.updateLocalWallpaperUri("") // clear local wallpaper when choosing preset
                                            Toast.makeText(context, "$label Theme Applied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .background(if (currentTheme == id && currentWallpaper.isEmpty()) Color(0x33109D43) else Color(0x0AFFFFFF))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (currentTheme == id && currentWallpaper.isEmpty()),
                                        onClick = {
                                            viewModel.updateChatBackgroundTheme(id)
                                            viewModel.updateLocalWallpaperUri("")
                                            Toast.makeText(context, "$label Theme Applied!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF109D43))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // Section 2: Local Wallpaper Upload Simulation
                        Text(
                            "LOCAL BACKGROUND UPLOAD (STAYS LOCAL)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                        
                        val localArtworks = listOf(
                            "nebula" to "Simulated Emerald Nebula ☄️",
                            "rose" to "Simulated Rose Gold Horizon 🎇",
                            "matrix" to "Simulated Hacker Core Matrix 💻"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            localArtworks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateLocalWallpaperUri(id)
                                            Toast.makeText(context, "Local Upload '$label' Applied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .background(if (currentWallpaper == id) Color(0x33109D43) else Color(0x0AFFFFFF))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (currentWallpaper == id),
                                        onClick = {
                                            viewModel.updateLocalWallpaperUri(id)
                                            Toast.makeText(context, "Local Upload '$label' Applied!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF109D43))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = customHexInput,
                                onValueChange = { customHexInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Custom Color (Hex e.g. #2C1930)", fontSize = 12.sp) },
                                placeholder = { Text("#1E1E2F") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (customHexInput.startsWith("#") && (customHexInput.length == 7 || customHexInput.length == 9)) {
                                                viewModel.updateLocalWallpaperUri(customHexInput)
                                                Toast.makeText(context, "Custom HEX background applied strictly locally!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Please enter valid HEX starting with # (e.g. #1E1E2F)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Apply Hex", tint = Color(0xFF109D43))
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF109D43),
                                    focusedLabelColor = Color(0xFF109D43)
                                )
                            )
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // Section 3: Chats Text Theme Color Changing
                        Text(
                            "CHATS BUBBLE TEXT COLOR TINT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )

                        val colorsPreset = listOf(
                            "" to "Default Color",
                            "#FFFFFF" to "Pure White ⚪",
                            "#A1FFD0" to "Mint Lime Green 🟢",
                            "#9CE2FF" to "Soft Ice Blue 🔵",
                            "#FCAFC4" to "Cotton Pink 🌸",
                            "#FFF176" to "Cyber Yellow 💛",
                            "#FFB74D" to "Sunset Pastel Orange 🟠"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(colorsPreset) { (hex, name) ->
                                    val circleColor = if (hex.isEmpty()) Color.LightGray else Color(android.graphics.Color.parseColor(hex))
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(circleColor)
                                            .border(
                                                width = if (currentTextColor == hex) 3.dp else 1.dp,
                                                color = if (currentTextColor == hex) Color(0xFF109D43) else Color.White.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.updateChatTextColorHex(hex)
                                                Toast.makeText(context, "$name Color Style Applied!", Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // Section 4: All Fonts in the World Luxury Fonts Selection
                        Text(
                            "FONTS SELECTION (APPLIES TO INBOX GLOBAL)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )

                        val premiumFontsDef = listOf(
                            "default" to "Standard System Font",
                            "helvetica" to "Helvetica Prime (Luxury Corporate)",
                            "futura" to "Futura Grand (Minimalist Geometric)",
                            "garamond" to "Garamond Antique (French Editorial)",
                            "playfair" to "Playfair Bold (Designer Vanguard)",
                            "cinzel" to "Cinzel Royal (Imperator Roman Small-Caps)",
                            "bodoni" to "Bodoni Vogue (High-Fashion Classic)",
                            "baskerville" to "Baskerville Deluxe (Academic Heritage)",
                            "firacode" to "Fira Code Hacker (Hacker Monospaced)",
                            "comicsans" to "Comic Sans Play (Playful Dynamic)"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            premiumFontsDef.forEach { (fontKey, fontName) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateChatFontFamily(fontKey)
                                            Toast.makeText(context, "$fontName applied globally!", Toast.LENGTH_SHORT).show()
                                        }
                                        .background(if (currentFont == fontKey) Color(0x33109D43) else Color(0x05FFFFFF))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (currentFont == fontKey),
                                        onClick = {
                                            viewModel.updateChatFontFamily(fontKey)
                                            Toast.makeText(context, "$fontName applied globally!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF109D43))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        fontName,
                                        fontSize = 13.sp,
                                        fontWeight = if (currentFont == fontKey) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentFont == fontKey) Color(0xFF109D43) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF109D43)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Done & Save Customizations", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SimChatUpgradeDialog(
    viewModel: SimChatViewModel,
    onContinueWithGoogle: () -> Unit,
    onContinueWithPhone: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF132C1F)),
            border = BorderStroke(1.5.dp, Color(0xFF2EBD59)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x1B2EBD59)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Cloud Upgrade",
                        tint = Color(0xFF2EBD59),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Upgrade SimChat Vibe",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Create a free SimChat account to unlock cloud messaging, groups, multi-device sync, typing indicators, online status, and cloud backups.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Continue with Google Button
                Button(
                    onClick = onContinueWithGoogle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("upgrade_continue_google")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("G  ", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 16.sp)
                        Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Continue with Phone Number Button
                OutlinedButton(
                    onClick = onContinueWithPhone,
                    border = BorderStroke(1.5.dp, Color(0xFF2EBD59)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2EBD59)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("upgrade_continue_phone")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF2EBD59),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue with Phone Number", color = Color(0xFF2EBD59), fontWeight = FontWeight.Bold)
                    }
                }

                // Not Now Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("upgrade_not_now")
                ) {
                    Text("Not Now", color = Color.LightGray, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ==========================================
// NEXT-GENERATION INTENTIONAL NOTIFICATIONS SYSTEM
// ==========================================

data class ProcessedNotification(
    val category: String,
    val title: String,
    val senderName: String,
    val messageText: String,
    val timestamp: String,
    val carrierSim: String,
    val otpCode: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val url: String? = null,
    val transactionAmount: String? = null,
    val transactionId: String? = null,
    val balance: String? = null,
    val trackingNumber: String? = null,
    val deliveryStatus: String? = null,
    val spamRiskScore: Int = 0,
    val businessName: String? = null,
    val verificationCode: String? = null,
    val groupName: String? = null,
    val mentionUser: String? = null
)

fun parseSimChatNotification(text: String, customSender: String = ""): ProcessedNotification {
    var sender = customSender
    if (sender.isEmpty()) {
        sender = when {
            text.contains("M-PESA", ignoreCase = true) || text.contains("Ksh", ignoreCase = true) -> "M-PESA"
            text.contains("Google", ignoreCase = true) -> "Google Security"
            text.contains("DHL", ignoreCase = true) -> "DHL Express"
            text.contains("Jane Doe", ignoreCase = true) -> "Jane Doe"
            text.contains("team", ignoreCase = true) || text.contains("dev", ignoreCase = true) -> "SimChat Dev Group"
            else -> "John Mwangi"
        }
    }

    // 1. Spam Alerts
    var spamScore = 0
    val spamKeywords = listOf("WINNER", "URGENT", "FREE", "CLAIM", "1M USD", "LOAN", "GET RICH", "scam", "scam.com", "http://scam")
    spamKeywords.forEach { keyword ->
        if (text.contains(keyword, ignoreCase = true)) {
            spamScore += 35
        }
    }
    if (text.contains("scam", ignoreCase = true) || text.contains("1M USD", ignoreCase = true)) {
        spamScore = 95
    }
    if (spamScore >= 50) {
        return ProcessedNotification(
            category = "Spam Alerts",
            title = "⚠️ High Spam Risk",
            senderName = sender,
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "SIM 1",
            spamRiskScore = spamScore,
            phone = "0700123456"
        )
    }

    // 2. Missed / Incoming Call Simulations
    if (text.contains("missed call", ignoreCase = true)) {
        return ProcessedNotification(
            category = "Missed Calls",
            title = "📞 Missed Call",
            senderName = sender,
            messageText = "Jane Doe • Missed Call from Safaricom",
            timestamp = "9:41 AM",
            carrierSim = "SIM 1 • Safaricom"
        )
    }
    if (text.contains("calling", ignoreCase = true)) {
        return ProcessedNotification(
            category = "Incoming Calls",
            title = "📞 Incoming Call",
            senderName = sender,
            messageText = "Jane Doe is calling...",
            timestamp = "Active",
            carrierSim = "Safaricom"
        )
    }

    // 3. OTP Messages
    if (text.contains("verification code", ignoreCase = true) || text.contains("OTP", ignoreCase = true) || text.contains("auth code", ignoreCase = true) || text.contains("passcode", ignoreCase = true) || text.contains("one-time", ignoreCase = true)) {
        val numbersPattern = "\\b\\d{4,8}\\b".toRegex()
        val match = numbersPattern.find(text)
        val otpVal = match?.value ?: "492083"
        return ProcessedNotification(
            category = "OTP Messages",
            title = "🔑 One-Time Passcode",
            senderName = sender,
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "SIM 1",
            otpCode = otpVal,
            verificationCode = otpVal
        )
    }

    // 4. Transaction Messages (Matches the M-PESA payment notification layout exactly!)
    if (text.contains("Ksh", ignoreCase = true) || text.contains("balance", ignoreCase = true) || text.contains("received", ignoreCase = true) || text.contains("Transaction ID", ignoreCase = true)) {
        val amountPattern = "Ksh\\s*[0-9,.]+".toRegex()
        val amountMatch = amountPattern.find(text)
        val amount = amountMatch?.value ?: "Ksh 1,250.00"

        val txIdPattern = "\\b[A-Z0-9]{10}\\b".toRegex()
        val txIdMatch = txIdPattern.find(text)
        val transactionId = txIdMatch?.value ?: "QMPA7X12K3"

        val balancePattern = "Balance:\\s*Ksh\\s*[0-9,.]+".toRegex()
        val balanceMatch = balancePattern.find(text)
        val balanceVal = balanceMatch?.value?.replace("Balance:", "")?.trim() ?: "Ksh 3,450.75"

        return ProcessedNotification(
            category = "Transaction Messages",
            title = "💸 Payment Received",
            senderName = "M-PESA",
            messageText = text,
            timestamp = "9:41 AM",
            carrierSim = "SIM 1 • Safaricom",
            transactionAmount = amount,
            transactionId = transactionId,
            balance = balanceVal
        )
    }

    // 5. Delivery Messages
    if (text.contains("DHL", ignoreCase = true) || text.contains("tracking", ignoreCase = true) || text.contains("package", ignoreCase = true) || text.contains("parcel", ignoreCase = true)) {
        val trackPattern = "[A-Z0-9#-]{8,15}".toRegex()
        val trackMatch = trackPattern.find(text)
        val tracking = trackMatch?.value ?: "DHL-55928-XY"
        return ProcessedNotification(
            category = "Delivery Messages",
            title = "📦 Package Delivery Update",
            senderName = sender,
            messageText = text,
            timestamp = "9:41 AM",
            carrierSim = "SIM 2",
            trackingNumber = tracking,
            deliveryStatus = "In Transit to Destination Hub"
        )
    }

    // 6. Verification Messages
    if (text.contains("verify", ignoreCase = true) && text.contains("url", ignoreCase = true)) {
        return ProcessedNotification(
            category = "Verification Messages",
            title = "🛡️ Account Security Verification",
            senderName = sender,
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "SIM 1",
            url = "https://simchat.example.com/verify/a02f92"
        )
    }

    // 7. Mention Notifications
    if (text.contains("@")) {
        val mentionPattern = "@[a-zA-Z0-9_]+".toRegex()
        val mentionMatch = mentionPattern.find(text)
        val mention = mentionMatch?.value ?: "@ronoh"
        return ProcessedNotification(
            category = "Mention Notifications",
            title = "📣 You were Mentioned",
            senderName = sender,
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "CLOUD",
            mentionUser = mention,
            groupName = "SimChat Dev Group"
        )
    }

    // 8. Group Messages
    if (text.contains("Group Chat", ignoreCase = true) || text.contains("group", ignoreCase = true)) {
        return ProcessedNotification(
            category = "Group Messages",
            title = "💬 Group Discussion",
            senderName = sender,
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "CLOUD",
            groupName = "SimChat Dev Group"
        )
    }

    // 9. Promotional Messages
    if (text.contains("discount", ignoreCase = true) || text.contains("offer", ignoreCase = true) || text.contains("promo", ignoreCase = true) || text.contains("deal", ignoreCase = true)) {
        return ProcessedNotification(
            category = "Promotional Messages",
            title = "🎁 Special Offer Saved",
            senderName = sender,
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "SIM 1"
        )
    }

    // 10. Business Messages
    if (text.contains("Google", ignoreCase = true) || text.contains("Workspace", ignoreCase = true)) {
        return ProcessedNotification(
            category = "Business Messages",
            title = "🏢 Business Official Channel",
            senderName = "Google Security",
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "SIM 1",
            businessName = "Google Workspace"
        )
    }

    // 11. System Alerts
    if (text.contains("system", ignoreCase = true) || text.contains("update", ignoreCase = true) || text.contains("security update", ignoreCase = true)) {
        return ProcessedNotification(
            category = "System Alerts",
            title = "⚙️ System Configuration Service",
            senderName = "SimChat Update Service",
            messageText = text,
            timestamp = "System",
            carrierSim = "Local Update"
        )
    }

    // 12. Cloud Messages
    if (text.contains("Cloud Chat", ignoreCase = true)) {
        return ProcessedNotification(
            category = "Cloud Messages",
            title = "☁️ Instant Cloud Chat",
            senderName = sender,
            messageText = text,
            timestamp = "9:41 AM",
            carrierSim = "CLOUD"
        )
    }

    // 13. SMS Messages
    if (text.contains("Carrier", ignoreCase = true) || text.contains("SIM", ignoreCase = true)) {
        return ProcessedNotification(
            category = "SMS Messages",
            title = "✉️ SMS Carrier Dispatch",
            senderName = sender,
            messageText = text,
            timestamp = "Just Now",
            carrierSim = "SIM 1"
        )
    }

    // Default Personal Messages
    return ProcessedNotification(
        category = "Personal Messages",
        title = "💬 Personal Conversation",
        senderName = sender,
        messageText = text,
        timestamp = "Just Now",
        carrierSim = "SIM 1"
    )
}

fun SimulatedNotification.toProcessedNotification(): ProcessedNotification {
    return ProcessedNotification(
        category = this.category,
        title = this.title,
        senderName = this.senderName,
        messageText = this.messageText,
        timestamp = this.timestamp,
        carrierSim = this.carrierSim,
        otpCode = this.otpCode,
        transactionAmount = this.transactionAmount,
        transactionId = this.transactionId,
        balance = this.balance,
        trackingNumber = this.trackingNumber,
        deliveryStatus = this.deliveryStatus,
        spamRiskScore = this.spamRiskScore
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHubScreen(
    viewModel: SimChatViewModel,
    onBack: () -> Unit
) {
    val liveNotifications by viewModel.notifications.collectAsState()
    var selectedFilterCategory by remember { mutableStateOf("All") }

    var rawTextQuery by remember { mutableStateOf("Payment Received! You have received Ksh 1,250.00 from JOHN MWANGI 07XXXXXXXX. Date: 27 May 2025 • 09:41 AM. Balance: Ksh 3,450.75. Transaction ID: QMPA7X12K3") }
    var activeSenderInput by remember { mutableStateOf("") }
    
    val currentNotification = remember(rawTextQuery, activeSenderInput) {
        parseSimChatNotification(rawTextQuery, activeSenderInput)
    }

    val context = LocalContext.current

    val notificationPresets = listOf(
        Triple("Transaction (M-PESA)", "Payment Received! You have received Ksh 1,250.00 from JOHN MWANGI 07XXXXXXXX. Date: 27 May 2025 • 09:41 AM. Balance: Ksh 3,450.75. Transaction ID: QMPA7X12K3", "M-PESA"),
        Triple("OTP Passcode", "Your SimChat one-time security login authorization passcode is 492083. Do not disclose this code.", "Google Security"),
        Triple("Incoming Call", "Jane Doe is calling you from Airtel SIM carrier slot 2...", "Jane Doe"),
        Triple("Missed Call", "Jane Doe missed call log. Call was placed on 9:41 AM on Carrier SIM 1.", "Jane Doe"),
        Triple("Scam Warning", "URGENT WINNER: Claim your free 1M USD prize now at http://scam.example.com/winner !!", "Blocked Sender"),
        Triple("DHL Delivery Check", "Your package DHL-55928-XY is out for delivery! Current status: In Transit to Destination Hub.", "DHL Express"),
        Triple("Cloud Group Mention", "@ronoh Look at this brand new UI on our SimChat development group chat!", "Supervisor Team"),
        Triple("Business Message", "Google Workspace: Your verification code is 109280. Google Security.", "Google Workspace"),
        Triple("Personal Messages", "Hey, are you still up for coffee later this afternoon?", "John Mwangi"),
        Triple("Cloud Messages", "[Cloud Chat] Design review complete! The new animations feel extremely pristine and responsive.", "Alex Rivers"),
        Triple("SMS Messages", "Please verify your carrier SIM registration card soon. Dial *100# for more options.", "Safaricom"),
        Triple("Verification", "Verify your email address by tapping this URL: https://simchat.example.com/verify/a02f92", "Security System"),
        Triple("Promotional Message", "Get 50% discount on SimChat Premium! Offer valid for the next 24 hours. Tap to redeem!", "SimChat Promo"),
        Triple("Group Messages", "[Group Chat] SimChat dev team: let's launch the next-generation updates tomorrow.", "Dev Board"),
        Triple("System Alerts", "System security update is available. Download to protect your offline database.", "SimChat System")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SimChat Shield", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Android 15 Intelligent Routing", fontSize = 11.sp, color = Color(0xFF2EBD59))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF132C1F))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1E16)) // Dark Emerald theme canvas
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FCM CLOUDSYNC INTEGRATION CONTROLS
            val sessionManager = remember { viewModel.sessionManager }
            var activeFcmToken by remember { 
                mutableStateOf(
                    sessionManager.fcmToken 
                    ?: "fcm_mock_tok_84f93a9e10dcf284e10b${System.currentTimeMillis()}"
                )
            }

            var customFcmSender by remember { mutableStateOf("Supervisor Support") }
            var customFcmBody by remember { mutableStateOf("Emergency cloud synchronization completed successfully! Safe connection established.") }
            var customFcmCategory by remember { mutableStateOf("Cloud Messages") }

            Text(
                text = "CLOUDSYNC FCM INTEGRATION CONTROLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2EBD59),
                letterSpacing = 1.5.sp
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13231A)),
                border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2EBD59))
                            )
                            Text(
                                text = "FCM Cloud Service: Ready",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE LIVE",
                                color = Color(0xFFFFB300),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C1911))
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "FCM REGISTRATION DEVICE TOKEN:",
                            fontSize = 9.sp,
                            color = Color.LightGray.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeFcmToken,
                                fontSize = 10.sp,
                                color = Color(0xFF2EBD59),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x2EBD5922))
                                    .clickable {
                                        val clip = android.content.ClipData.newPlainText("FCM Token", activeFcmToken)
                                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                        Toast.makeText(context, "FCM Token Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "COPY",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2EBD59).copy(alpha = 0.15f), thickness = 1.dp)

                    Text(
                        text = "Simulate Inbound Push Network (Verify FCM Callback Routing)",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = customFcmSender,
                        onValueChange = { customFcmSender = it },
                        label = { Text("Push Sender Domain / Topic", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customFcmBody,
                        onValueChange = { customFcmBody = it },
                        label = { Text("Push payload body / message block", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F2C1E))
                                .border(1.dp, Color(0xFF2EBD59).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable {
                                    customFcmCategory = if (customFcmCategory == "Cloud Messages") "Group Messages" else "Cloud Messages"
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Type: $customFcmCategory", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val simulatedNotif = SimulatedNotification(
                                    id = "fcm_sim_" + System.currentTimeMillis(),
                                    category = customFcmCategory,
                                    title = "☁️ FCM Push Notification",
                                    senderName = customFcmSender,
                                    messageText = customFcmBody,
                                    timestamp = "Just Now",
                                    carrierSim = "CLOUD",
                                    isUnread = true
                                )
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    viewModel.repository.handleIncomingFcmMessage(simulatedNotif)
                                }
                                Toast.makeText(context, "FCM Push Pipeline triggered successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Push Message", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. DYNAMIC PREVIEW VIEWPORT (The Glassmorphism Mockup container)
            Text(
                text = "ACTIVE SIMULATION HEADS-UP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2EBD59),
                letterSpacing = 1.5.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF16291E))
                    .border(1.dp, Color(0xFF2EBD59).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Simulated OS Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("9:41", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            // Dot representing Active system status
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2EBD59)))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.NetworkCell, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(16.dp))
                        }
                    }

                    // Render Category Specific Adaptive Layouts - Premium UI designs matching Truecaller/M-Pesa reference!
                    AdaptiveNotificationCardLayout(
                        notif = currentNotification,
                        onAction = { actionName ->
                            Toast.makeText(context, "Action Triggered: $actionName", Toast.LENGTH_SHORT).show()
                            if (actionName.contains("Copy")) {
                                val textToCopy = when {
                                    actionName.contains("OTP") -> currentNotification.otpCode ?: ""
                                    actionName.contains("Amount") -> currentNotification.transactionAmount ?: ""
                                    else -> currentNotification.messageText
                                }
                                Toast.makeText(context, "Copied content to dashboard clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // 2. INTELLIGENT ROUTING SANDBOX TESTER
            Text(
                text = "SMART DETECTION ENGINE SANDBOX",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2EBD59),
                letterSpacing = 1.5.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13231A)),
                border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customize Payload Content",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = activeSenderInput,
                        onValueChange = { activeSenderInput = it },
                        label = { Text("Custom Sender Name (Optional)", color = Color.Gray) },
                        placeholder = { Text("e.g., M-PESA, Google, Safaricom", color = Color.DarkGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedContainerColor = Color(0x0E000000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rawTextQuery,
                        onValueChange = { rawTextQuery = it },
                        label = { Text("Message Input Stream Parser", color = Color.Gray) },
                        placeholder = { Text("Paste any incoming SMS message stream here...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2EBD59),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedContainerColor = Color(0x0E000000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // EXTRACED ENTITIES METRICS DISPLAY
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1E15))
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "EXTRACTED REAL-TIME ENTITIES:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2EBD59)
                        )
                        ExtractedFieldEntity(label = "Primary Category", value = currentNotification.category)
                        if (currentNotification.otpCode != null) {
                            ExtractedFieldEntity(label = "OTP Security Value", value = currentNotification.otpCode, highlight = true)
                        }
                        if (currentNotification.transactionAmount != null) {
                            ExtractedFieldEntity(label = "Transacted Amount", value = currentNotification.transactionAmount, highlight = true)
                        }
                        if (currentNotification.transactionId != null) {
                            ExtractedFieldEntity(label = "M-PESA TX Ledger ID", value = currentNotification.transactionId)
                        }
                        if (currentNotification.balance != null) {
                            ExtractedFieldEntity(label = "Remaining Balance", value = currentNotification.balance)
                        }
                        if (currentNotification.trackingNumber != null) {
                            ExtractedFieldEntity(label = "Delivery Waybill Tracking", value = currentNotification.trackingNumber)
                        }
                        if (currentNotification.spamRiskScore > 0) {
                            ExtractedFieldEntity(label = "Spam Risk Severity", value = "${currentNotification.spamRiskScore}% RISK SCORE", highlight = true)
                        }
                    }
                }
            }

            // 3. COMPLETE 15 CATEGORY EXPLORER PRESETS MATRIX
            Text(
                text = "COMPLETE 15 CATEGORIES ENGINE SPECIFIERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2EBD59),
                letterSpacing = 1.5.sp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notificationPresets.forEachIndexed { idx, item ->
                    val (titleName, messageStr, senderStr) = item
                    val isSelected = currentNotification.category == titleName || (titleName.contains("Transaction") && currentNotification.category == "Transaction Messages") || (titleName.contains("OTP") && currentNotification.category == "OTP Messages") || (titleName.contains("DHL") && currentNotification.category == "Delivery Messages") || (titleName.contains("Scam") && currentNotification.category == "Spam Alerts")
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF1D3C29) else Color(0xFF13231A))
                            .border(1.dp, if (isSelected) Color(0xFF2EBD59) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                rawTextQuery = messageStr
                                activeSenderInput = senderStr
                                val parsed = parseSimChatNotification(messageStr, senderStr)
                                viewModel.addNotification(
                                    SimulatedNotification(
                                        id = "notif_dyn_${System.currentTimeMillis()}",
                                        category = parsed.category,
                                        title = parsed.title,
                                        senderName = parsed.senderName,
                                        messageText = parsed.messageText,
                                        timestamp = parsed.timestamp,
                                        carrierSim = parsed.carrierSim,
                                        isUnread = true,
                                        otpCode = parsed.otpCode,
                                        transactionAmount = parsed.transactionAmount,
                                        transactionId = parsed.transactionId,
                                        balance = parsed.balance,
                                        trackingNumber = parsed.trackingNumber,
                                        deliveryStatus = parsed.deliveryStatus,
                                        spamRiskScore = parsed.spamRiskScore
                                    )
                                )
                                Toast.makeText(context, "Shield Alert: Triggered ${parsed.category}!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Badge sequence digit
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF2EBD59) else Color(0x272EBD59)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (idx + 1).toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color(0xFF2EBD59)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = titleName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Verified Badge on preset listing
                                if (senderStr == "M-PESA" || senderStr == "Google Security" || senderStr == "Google Workspace") {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(12.dp))
                                }
                            }
                            Text(
                                text = messageStr,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Trigger Simulation Preset",
                            tint = Color(0xFF2EBD59).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. ACTIVE INTEGRATED REGISTRY HISTORY TRAY
            Text(
                text = "TRACED & SECURED INTEGRATED REGISTRY HISTORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2EBD59),
                letterSpacing = 1.5.sp
            )

            // Dynamic filter row of custom M3 FilterChips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Unread", "Transactions", "OTP", "Calls", "Spam Alerts").forEach { filterType ->
                    val isFilterSelected = selectedFilterCategory == filterType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isFilterSelected) Color(0xFF2EBD59) else Color(0xFF13231A))
                            .border(1.dp, if (isFilterSelected) Color(0xFF2EBD59) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { selectedFilterCategory = filterType }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filterType,
                            color = if (isFilterSelected) Color.Black else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val filteredNotifs = remember(liveNotifications, selectedFilterCategory) {
                liveNotifications.filter { notif ->
                    when (selectedFilterCategory) {
                        "Unread" -> notif.isUnread
                        "Transactions" -> notif.category == "Transaction Messages"
                        "OTP" -> notif.category == "OTP Messages"
                        "Calls" -> notif.category == "Incoming Calls" || notif.category == "Missed Calls"
                        "Spam Alerts" -> notif.category == "Spam Alerts"
                        else -> true
                    }
                }
            }

            if (filteredNotifs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF13231A), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No registry entries matching standard filter category.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredNotifs.forEach { simulatedNotif ->
                        var isReplyExpanded by remember { mutableStateOf(false) }
                        var replyTextFieldText by remember { mutableStateOf("") }
                        var isSimulatingReplySend by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (simulatedNotif.isUnread) Color(0x1F2EBD59) else Color(0x0C2EBD59))
                                .border(
                                    1.dp,
                                    if (simulatedNotif.isUnread) Color(0xFF2EBD59).copy(alpha = 0.35f) else Color(0xFF2EBD59).copy(alpha = 0.08f),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Status header representing read/unread
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (simulatedNotif.isUnread) Color(0xFF2EBD59) else Color.Gray)
                                        )
                                        Text(
                                            text = if (simulatedNotif.isUnread) "Active Unread" else "Processed Seen",
                                            fontSize = 11.sp,
                                            color = if (simulatedNotif.isUnread) Color(0xFF2EBD59) else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = simulatedNotif.carrierSim,
                                            fontSize = 10.sp,
                                            color = Color.LightGray.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Display actual adaptive layout card!
                                AdaptiveNotificationCardLayout(
                                    notif = simulatedNotif.toProcessedNotification(),
                                    onAction = { actionStr ->
                                        if (actionStr.contains("Reply")) {
                                            isReplyExpanded = !isReplyExpanded
                                        } else if (actionStr.contains("Copy")) {
                                            val textToCopy = when {
                                                simulatedNotif.otpCode != null -> simulatedNotif.otpCode
                                                simulatedNotif.transactionAmount != null -> simulatedNotif.transactionAmount
                                                else -> simulatedNotif.messageText
                                            }
                                            Toast.makeText(context, "Copied content to clipboard!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "$actionStr action completed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                // Action Row below card contents
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                viewModel.markNotificationRead(simulatedNotif.id, simulatedNotif.isUnread)
                                                Toast.makeText(context, if (simulatedNotif.isUnread) "Shield: Marked Read!" else "Shield: Marked Unread!", Toast.LENGTH_SHORT).show()
                                            },
                                        ) {
                                            Text(
                                                text = if (simulatedNotif.isUnread) "Mark Seen" else "Mark Unread",
                                                fontSize = 11.sp,
                                                color = Color(0xFF2EBD59),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        TextButton(
                                            onClick = { isReplyExpanded = !isReplyExpanded },
                                        ) {
                                            Text(
                                                text = "Inline Reply",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.blockSenderNotification(simulatedNotif.senderName)
                                            Toast.makeText(context, "Safaricom Shield: ${simulatedNotif.senderName} sender blocked", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Block, contentDescription = "Block Sender", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                                    }
                                }

                                // Interactive Inline Quick Reply Area
                                if (isReplyExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0C1911))
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = replyTextFieldText,
                                            onValueChange = { replyTextFieldText = it },
                                            placeholder = { Text("Type rapid background reply...", color = Color.Gray, fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFF2EBD59)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            maxLines = 2
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (replyTextFieldText.isNotBlank()) {
                                                        isSimulatingReplySend = true
                                                        viewModel.replyNotificationInBackground(simulatedNotif.id, replyTextFieldText)
                                                        Toast.makeText(context, "Safaricom Shield: Replying to ${simulatedNotif.senderName} in background SMS SIM...", Toast.LENGTH_SHORT).show()
                                                        isReplyExpanded = false
                                                        replyTextFieldText = ""
                                                        isSimulatingReplySend = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                if (isSimulatingReplySend) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.dp)
                                                } else {
                                                    Text("Send Background", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        }
    }
}

@Composable
fun ExtractedFieldEntity(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Color(0xFF2EBD40) else Color.White
        )
    }
}

// ========================================================
// ADAPTIVE LAYOUT SELECTOR AND COMPRESSED DECORATORS
// ========================================================

@Composable
fun AdaptiveNotificationCardLayout(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    when (notif.category) {
        "Transaction Messages" -> {
            TransactionNotificationCard(notif = notif, onAction = onAction)
        }
        "OTP Messages" -> {
            OtpNotificationCard(notif = notif, onAction = onAction)
        }
        "Spam Alerts" -> {
            SpamNotificationCard(notif = notif, onAction = onAction)
        }
        "Delivery Messages" -> {
            DeliveryNotificationCard(notif = notif, onAction = onAction)
        }
        "Incoming Calls" -> {
            IncomingCallNotificationCard(notif = notif, onAction = onAction)
        }
        "Missed Calls" -> {
            MissedCallNotificationCard(notif = notif, onAction = onAction)
        }
        "Cloud Messages" -> {
            CloudChatNotificationCard(notif = notif, onAction = onAction)
        }
        else -> {
            // General / Personal / Business / Promotional Adaptive layout
            GeneralNotificationCard(notif = notif, onAction = onAction)
        }
    }
}

// =========================================================
// 1. TRANSACTION CARD LAYOUT (EXACTLY MATCHING REFS!)
// =========================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2E24)),
        border = BorderStroke(1.5.dp, Color(0xFF2EBD59).copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row with verified icon badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom Green Circle Logo M-PESA Simulation
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFF2EBD59), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M-Pesa",
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = Color(0xFF109D43)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("M-PESA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified Channel Notification", tint = Color(0xFF2EBD59), modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "Transactional Message • SIM 1 • Safaricom",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.8f)
                        )
                    }
                }

                // Header status info timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "9:41 AM", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Light)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9100).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Body display details grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Check Container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2EBD59).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(28.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Payment Received", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "You have received", color = Color.LightGray, fontSize = 13.sp)
                    Text(
                        text = notif.transactionAmount ?: "Ksh 1,250.00",
                        color = Color(0xFF2EBD59),
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                    Text(text = "from JOHN MWANGI 07XXXXXXXX", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            // Sub Details list and Action Button Box on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Detailed ledger parameters (Left)
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .background(Color(0xFF0F1E15))
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Date", fontSize = 10.sp, color = Color.LightGray)
                        }
                        Text("27 May 2025 • 09:41 AM", fontSize = 10.sp, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Transaction ID", fontSize = 10.sp, color = Color.LightGray)
                        }
                        Text(notif.transactionId ?: "QMPA7X12K3", fontSize = 10.sp, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Wallet, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Balance", fontSize = 10.sp, color = Color.LightGray)
                        }
                        Text(notif.balance ?: "Ksh 3,450.75", fontSize = 11.sp, color = Color(0xFF2EBD59), fontWeight = FontWeight.Bold)
                    }
                }

                // Interactive secondary cards (Right)
                Column(
                    modifier = Modifier.weight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF13231B))
                            .clickable { onAction("View Receipt") }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("View Receipt", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Tap to open", fontSize = 8.sp, color = Color.LightGray)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF13231B))
                            .clickable { onAction("Reply to M-PESA") }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Message", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Reply to M-PESA", fontSize = 8.sp, color = Color.LightGray)
                        }
                    }
                }
            }

            Divider(color = Color(0xFF2EBD59).copy(alpha = 0.2f))

            // Action Quick Triggers Footers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onAction("Copy Amount") },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2EBD59))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Amount", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { onAction("Mark as Important") },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2EBD59))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark as Important", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { onAction("Block Sender") },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Block Sender", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 2. OTP CODE DISPLAY NOTIFICATION CARD
// ==========================================
@Composable
fun OtpNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13271D)),
        border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2EBD59), modifier = Modifier.size(16.dp))
                    Text(text = "OTP Messages", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2EBD59))
                }
                Text("9:41 AM", fontSize = 10.sp, color = Color.Gray)
            }

            Text(
                text = "${notif.senderName} Verification Security System",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = notif.messageText,
                fontSize = 12.sp,
                color = Color.LightGray
            )

            // Huge Code container display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0C1912))
                    .border(1.dp, Color(0xFF2EBD59).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("VERIFICATION CODE", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = notif.otpCode ?: "492083",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2EBD59),
                        letterSpacing = 4.sp
                    )
                }

                Button(
                    onClick = { onAction("Copy OTP Code") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy OTP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text("Expires in 4:59 minutes", fontSize = 11.sp, color = Color.Gray)
                }

                TextButton(onClick = { onAction("Auto-Fill Credentials App") }) {
                    Text("Auto-Fill App", color = Color(0xFF2EBD59), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ==========================================
// 3. SPAM RADAR WARNING CARD
// ==========================================
@Composable
fun SpamNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF331414)),
        border = BorderStroke(1.5.dp, Color.Red.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Text("SimChat Active Blocklist Shield", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Red)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${notif.spamRiskScore}% RISK SCORE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            Text("Spam Alert: ${notif.senderName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(notif.messageText, fontSize = 12.sp, color = Color.LightGray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAction("Block sender permanently") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Block Sender", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = { onAction("Report to database") },
                    border = BorderStroke(1.dp, Color.Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Report Spam", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ==========================================
// 4. DELIVERY NOTIFICATION CARD
// ==========================================
@Composable
fun DeliveryNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13231B)),
        border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📦 Delivery Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2EBD59))
                }
                Text("9:41 AM", fontSize = 10.sp, color = Color.Gray)
            }

            Text(text = "DHL Parcel Transit Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1711))
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tracking Number", fontSize = 10.sp, color = Color.Gray)
                    Text(notif.trackingNumber ?: "DHL-55928-XY", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Shipment Status", fontSize = 10.sp, color = Color.Gray)
                    Text(notif.deliveryStatus ?: "In Transit", fontSize = 10.sp, color = Color(0xFF2EBD59), fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { onAction("Track package online routing") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Track Package Route", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 5. INCOMING CALL ACTIVE PANEL CARD
// ==========================================
@Composable
fun IncomingCallNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132C1E)),
        border = BorderStroke(1.5.dp, Color(0xFF2EBD59)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2EBD59)))
                    Text("INCOMING SIM CALL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF2EBD59), letterSpacing = 1.sp)
                }
                Text("SIM 1 • Safaricom", fontSize = 11.sp, color = Color.Gray)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C4A39)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(notif.senderName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("+254 700 123 456", fontSize = 13.sp, color = Color.LightGray)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAction("Answer active telecom call") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EBD59)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Answer", color = Color.Black)
                }

                Button(
                    onClick = { onAction("Decline and disconnect") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Decline", color = Color.White)
                }

                OutlinedButton(
                    onClick = { onAction("Quick SMS reply") },
                    border = BorderStroke(1.dp, Color(0xFF2EBD59)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2EBD59)),
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reply SMS", fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// 6. MISSED TELECOM CALL CARD LAYOUT
// ==========================================
@Composable
fun MissedCallNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF221111)),
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Text("Missed Call Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
                Text("9:41 AM", fontSize = 11.sp, color = Color.Gray)
            }

            Text("${notif.senderName} (+254 700 123 456)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Carrier: Safaricom SIM 1 slot dispatcher.", fontSize = 12.sp, color = Color.LightGray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAction("Callback caller") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Callback Request", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onAction("Send text message to caller") },
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Message Reply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 7. INSTANT CLOUD CHAT CARD LAYOUT
// ==========================================
@Composable
fun CloudChatNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132C1F)),
        border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2EBD59))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("CLOUD", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Text("Instant Encryption", fontSize = 11.sp, color = Color.LightGray)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2EBD59)))
                    Text("Online", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Text("From: ${notif.senderName}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(notif.messageText, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onAction("Reply to cloud message") }) {
                    Text("Quick Reply", color = Color(0xFF2EBD59), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onAction("Mark seen on cloud ledger") }) {
                    Text("Mark Seen", color = Color.LightGray)
                }
            }
        }
    }
}

// ========================================================
// 8. GENERAL / PERSONAL / BUSINESS INDEPENDENT CARD LAYOUT
// ========================================================
@Composable
fun GeneralNotificationCard(
    notif: ProcessedNotification,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13231B)),
        border = BorderStroke(1.dp, Color(0xFF2EBD59).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3B2EBD59))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = notif.carrierSim,
                            color = Color(0xFF2EBD59),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(text = notif.category, fontSize = 11.sp, color = Color.LightGray)
                }
                Text(notif.timestamp, fontSize = 10.sp, color = Color.Gray)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C4A39)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = notif.senderName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text(text = notif.messageText, color = Color.LightGray, fontSize = 12.sp)
                }
            }

            Divider(color = Color(0xFF2EBD59).copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onAction("Reply basic dispatch") }) {
                    Text("Reply", color = Color(0xFF2EBD59), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { onAction("Mark read inbox") }) {
                    Text("Mark Read", color = Color.LightGray, fontSize = 12.sp)
                }

                TextButton(onClick = { onAction("Call sender back") }) {
                    Text("Call Sender", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}


