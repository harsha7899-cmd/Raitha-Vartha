package com.example.raitha_vartha

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.raitha_vartha.ui.theme.RaithaVarthaTheme
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.auth
import com.google.firebase.initialize
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class Screen { Login, SignUp, ForgotPassword, MainApp, Profile, SocialConnect, AdminDashboard }

class MainActivity : ComponentActivity() {
    private val firestoreRepository by lazy { FirestoreRepository(this) }
    
    private val tipViewModel: TipViewModel by viewModels {
        TipViewModel.Factory(firestoreRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.initialize(context = applicationContext)
        
        val firebaseAppCheck = Firebase.appCheck
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        
        tipViewModel.addSampleTips()

        setContent {
            var currentScreen by remember { mutableStateOf(Screen.Login) }
            var loggedInUser by remember { mutableStateOf<UserEntity?>(null) }
            val appLanguage by tipViewModel.appLanguage.collectAsState()
            val themeMode by tipViewModel.themeMode.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            RaithaVarthaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box {
                        when (currentScreen) {
                            Screen.Login -> LoginScreen(
                                firestoreRepository, 
                                onLoginSuccess = { user -> 
                                    android.util.Log.d("Login", "Login success. Email: ${user.email}, isAdmin: ${user.isAdmin}")
                                    // FORCE CHECK FOR HARDCODED ADMIN
                                    val finalUser = if (user.email == "admin@raithavartha.com") user.copy(isAdmin = true) else user
                                    loggedInUser = finalUser
                                    tipViewModel.setCurrentUser(finalUser.email)
                                    currentScreen = if (finalUser.isAdmin) Screen.AdminDashboard else Screen.MainApp
                                    android.util.Log.d("Login", "Current screen set to: $currentScreen")
                                },
                                onGoToSignUp = { currentScreen = Screen.SignUp },
                                onForgotPassword = { currentScreen = Screen.ForgotPassword },
                                language = appLanguage,
                                onLanguageChange = { tipViewModel.setLanguage(it) }
                            )
                            Screen.SignUp -> SignUpScreen(
                                firestoreRepository, 
                                onSignUpSuccess = { currentScreen = Screen.Login },
                                onBackToLogin = { currentScreen = Screen.Login },
                                language = appLanguage,
                                onLanguageChange = { tipViewModel.setLanguage(it) }
                            )
                            Screen.ForgotPassword -> ForgotPasswordScreen(
                                firestoreRepository, 
                                onResetSuccess = { currentScreen = Screen.Login },
                                onBackToLogin = { currentScreen = Screen.Login },
                                language = appLanguage,
                                onLanguageChange = { tipViewModel.setLanguage(it) }
                            )
                            Screen.MainApp -> loggedInUser?.let { user ->
                                MainAppContent(
                                    tipViewModel, firestoreRepository, snackbarHostState, user,
                                    onSignOut = { 
                                        loggedInUser = null
                                        tipViewModel.setCurrentUser(null)
                                        
                                        currentScreen = Screen.Login 
                                    },
                                    onUserUpdate = { loggedInUser = it },
                                    onOpenProfile = { currentScreen = Screen.Profile },
                                    onOpenSocial = { currentScreen = Screen.SocialConnect },
                                    onOpenDashboard = { currentScreen = Screen.AdminDashboard }
                                )
                            }
                            Screen.Profile -> loggedInUser?.let { user ->
                                ProfileScreen(
                                    user, 
                                    firestoreRepository, 
                                    onBack = { currentScreen = Screen.MainApp },
                                    onUpdate = { loggedInUser = it }
                                )
                            }
                            Screen.SocialConnect -> loggedInUser?.let { user ->
                                SocialConnectScreen(
                                    currentUser = user,
                                    repository = firestoreRepository,
                                    onBack = { currentScreen = Screen.MainApp }
                                )
                            }
                            Screen.AdminDashboard -> loggedInUser?.let {
                                AdminDashboardScreen(
                                    repository = firestoreRepository,
                                    onBack = { currentScreen = Screen.MainApp }
                                )
                            }
                        }
                        
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RaithaVarthaTitle(appLanguage: AppLanguage) {
    val annotatedString = buildAnnotatedString {
        val part1 = if (appLanguage == AppLanguage.KANNADA) "ರೈತ" else "Raitha"
        val part2 = if (appLanguage == AppLanguage.KANNADA) "ವಾರ್ತೆ" else "Vartha"
        
        withStyle(style = SpanStyle(color = Color(0xFFFF9933))) { // Saffron
            append(part1)
        }
        withStyle(style = SpanStyle(color = Color.White)) { // White dash for visibility
            append("-")
        }
        withStyle(style = SpanStyle(color = Color(0xFF128807))) { // Green
            append(part2)
        }
    }
    Text(
        text = annotatedString,
        fontSize = 28.sp, 
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        style = MaterialTheme.typography.headlineSmall.copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.4f),
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        )
    )
}

@Composable
fun LanguageHeader(currentLanguage: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, end = 24.dp, bottom = 16.dp), 
        horizontalArrangement = Arrangement.End, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            modifier = Modifier.height(48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shadowElevation = 2.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                LanguageOptionButton(
                    text = "ಕನ್ನಡ",
                    isSelected = currentLanguage == AppLanguage.KANNADA,
                    onClick = { onLanguageChange(AppLanguage.KANNADA) }
                )
                LanguageOptionButton(
                    text = "English",
                    isSelected = currentLanguage == AppLanguage.ENGLISH,
                    onClick = { onLanguageChange(AppLanguage.ENGLISH) }
                )
            }
        }
    }
}

@Composable
fun LanguageOptionButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")
    val alpha by animateFloatAsState(if (isPressed) 0.8f else 1f, label = "alpha")

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    isSelected -> MaterialTheme.colorScheme.primary
                    isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp 
        )
    }
}

@Composable
fun ScalableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "buttonScale")

    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) containerColor.copy(alpha = 0.75f) else containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isPressed) BorderStroke(2.dp, containerColor.copy(alpha = 0.9f)) else null,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun LoginScreen(repository: FirestoreRepository, onLoginSuccess: (UserEntity) -> Unit, onGoToSignUp: () -> Unit, onForgotPassword: () -> Unit, language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val loginBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(loginBgColor, MaterialTheme.colorScheme.background)
    )

    Column(modifier = Modifier.fillMaxSize().background(gradientBrush)) {
        LanguageHeader(language, onLanguageChange)
        Column(modifier = Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Agriculture, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            RaithaVarthaTitle(language)
            Text(text = if (language == AppLanguage.KANNADA) "ನಿಮ್ಮ ಕೃಷಿ ಸಂಗಾತಿ" else "Your Farming Companion", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            
            Spacer(Modifier.height(48.dp))
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = if (language == AppLanguage.KANNADA) "ಲಾಗಿನ್" else "Login", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = identifier, 
                        onValueChange = { identifier = it }, 
                        label = { Text(if (language == AppLanguage.KANNADA) "ಇಮೇಲ್ ಅಥವಾ ಫೋನ್ ಸಂಖ್ಯೆ" else "Email or Phone Number") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                        enabled = !isLoggingIn
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password, 
                        onValueChange = { password = it }, 
                        label = { Text(if (language == AppLanguage.KANNADA) "ಪಾಸ್‌ವರ್ಡ್" else "Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !isLoggingIn
                    )
                    
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    ScalableButton(
                        onClick = { 
                            scope.launch { 
                                isLoggingIn = true
                                errorMessage = null
                                val result = repository.loginUser(identifier, password)
                                if (result.isSuccess) {
                                    onLoginSuccess(result.getOrThrow()) 
                                } else {
                                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Login failed. Please try again."
                                }
                                isLoggingIn = false
                            } 
                        }, 
                        modifier = Modifier.fillMaxWidth().height(56.dp), 
                        enabled = !isLoggingIn
                    ) { 
                        if (isLoggingIn) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(if (language == AppLanguage.KANNADA) "ಲಾಗಿನ್" else "Login", fontSize = 18.sp, fontWeight = FontWeight.Bold) 
                        }
                    }
                    
                    TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.CenterHorizontally), enabled = !isLoggingIn) { 
                        Text(if (language == AppLanguage.KANNADA) "ಪಾಸ್‌ವರ್ಡ್ ಮರೆತಿದ್ದೀರಾ?" else "Forgot Password?", color = MaterialTheme.colorScheme.primary) 
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Text(if (language == AppLanguage.KANNADA) "ಖಾತೆ ಇಲ್ಲವೇ?" else "Don't have an account?", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                TextButton(onClick = onGoToSignUp, enabled = !isLoggingIn) { 
                    Text(if (language == AppLanguage.KANNADA) "ನೋಂದಾಯಿಸಿ" else "Sign Up", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) 
                } 
            }
        }
    }
}

@Composable
fun SignUpScreen(repository: FirestoreRepository, onSignUpSuccess: () -> Unit, onBackToLogin: () -> Unit, language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var fName by remember { mutableStateOf("") }; var lName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var idProof by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }; var confirmPass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.background)
    )

    Column(modifier = Modifier.fillMaxSize().background(gradientBrush)) {
        LanguageHeader(language, onLanguageChange)
        Column(modifier = Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (language == AppLanguage.KANNADA) "ಹೊಸ ಖಾತೆ ತೆರೆಯಿರಿ" else "Join Raitha-Vartha", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಮೊದಲ ಹೆಸರು" else "First Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = lName, onValueChange = { lName = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಕೊನೆಯ ಹೆಸರು" else "Last Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = phone, onValueChange = { if (it.length <= 13) phone = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಫೋನ್ ಸಂಖ್ಯೆ" else "Phone Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಇಮೇಲ್" else "Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(if (language == AppLanguage.KANNADA) "ನಗರ/ಪಟ್ಟಣ" else "City/Town") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಹಳ್ಳಿ" else "Village") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    Spacer(Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = age, onValueChange = { if (it.length <= 3) age = it }, label = { Text(if (language == AppLanguage.KANNADA) "ವಯಸ್ಸು" else "Age") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                        OutlinedTextField(value = experience, onValueChange = { if (it.length <= 2) experience = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಅನುಭವ (ವರ್ಷಗಳು)" else "Experience (Yrs)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = idProof, onValueChange = { idProof = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಆಧಾರ್/ವೋಟರ್ ಐಡಿ ಸಂಖ್ಯೆ" else "Aadhar/Voter ID Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pass, 
                        onValueChange = { pass = it }, 
                        label = { Text(if (language == AppLanguage.KANNADA) "ಪಾಸ್‌ವರ್ಡ್" else "Password") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { passVisible = !passVisible }) { Icon(if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isRegistering
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = confirmPass, onValueChange = { confirmPass = it }, label = { Text(if (language == AppLanguage.KANNADA) "ಪಾಸ್‌ವರ್ಡ್ ದೃಢೀಕರಿಸಿ" else "Confirm Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), enabled = !isRegistering)
                    
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    ScalableButton(
                        onClick = { 
                            val emailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
                            val ageInt = age.toIntOrNull() ?: 0
                            val expInt = experience.toIntOrNull() ?: 0
                            val maxPossibleExp = ageInt - 18
                            
                            when {
                                fName.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty() || age.isEmpty() || experience.isEmpty() || idProof.isEmpty() -> err = "Fill all fields"
                                !emailValid -> err = "Enter valid email"
                                pass != confirmPass -> err = "Passwords do not match"
                                ageInt <= 30 -> err = "Sorry your age is not eligible for expert status"
                                expInt !in 5..maxPossibleExp -> err = "Experience details do not match your age verification."
                                else -> {
                                    scope.launch {
                                        isRegistering = true
                                        err = null
                                        val isExpertUser = true // Based on conditions above
                                        val result = repository.registerUser(UserEntity(
                                            firstName = fName, 
                                            lastName = lName, 
                                            phoneNumber = phone, 
                                            email = email, 
                                            password = pass, 
                                            city = city, 
                                            village = village,
                                            age = ageInt,
                                            yearsOfExperience = expInt,
                                            idProofNumber = idProof,
                                            isExpert = isExpertUser
                                        ))
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Sign Up Successful! Please login.", Toast.LENGTH_LONG).show()
                                            onSignUpSuccess()
                                        } else {
                                            err = result.exceptionOrNull()?.localizedMessage ?: "Registration failed. Please check your network."
                                            isRegistering = false
                                        }
                                    }
                                }
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isRegistering
                    ) { 
                        if (isRegistering) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        else Text("Register", fontWeight = FontWeight.Bold) 
                    }
                }
            }
            TextButton(onClick = onBackToLogin, enabled = !isRegistering) { Text("Back to Login", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun ForgotPasswordScreen(repository: FirestoreRepository, onResetSuccess: () -> Unit, onBackToLogin: () -> Unit, language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var email by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LanguageHeader(language, onLanguageChange)
        Column(modifier = Modifier.weight(1f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Reset Password", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Registered Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("New Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(24.dp))
            ScalableButton(onClick = { scope.launch { repository.updatePassword(email, pass); onResetSuccess() } }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Reset") }
            TextButton(onClick = onBackToLogin) { Text("Back to Login", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun MainAppContent(viewModel: TipViewModel, repository: FirestoreRepository, snackbarHostState: SnackbarHostState, user: UserEntity, onSignOut: () -> Unit, onUserUpdate: (UserEntity) -> Unit, onOpenProfile: () -> Unit, onOpenSocial: () -> Unit, onOpenDashboard: () -> Unit) {
    val tips by viewModel.tips.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val hasPendingPost by viewModel.hasPendingPost(user.email).collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val context = LocalContext.current
    
    var showDiseaseResult by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var adminTipToEdit by remember { mutableStateOf<TipEntity?>(null) }
    var storyToEdit by remember { mutableStateOf<TipEntity?>(null) }
    var showAddStory by remember { mutableStateOf(false) }
    var showAddExpertTip by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var previousTipsSize by remember { mutableIntStateOf(tips.size) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationsEnabled(isGranted)
    }

    LaunchedEffect(tips) {
        if (notificationsEnabled && tips.size > previousTipsSize && previousTipsSize != 0) {
            val newestTip = tips.maxByOrNull { it.id }
            if (newestTip != null) {
                if (newestTip.id.startsWith("story_")) {
                    val farmerName = newestTip.title.split("|").last().trim().replace("Farmer Success: ", "")
                    showNotification(context, "Raitha-Vartha Inspiration! 🌟", "Farmer $farmerName just shared a new success journey. Tap to see their achievement!")
                } else if (newestTip.id.startsWith("expert_")) {
                    val authName = newestTip.authorName
                    showNotification(context, "New Expert Insight! 🌾", "$authName shared a verified tip for ${newestTip.category}. Tap to learn more!")
                }
            }
        }
        previousTipsSize = tips.size
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        if (uri != null) {
            scope.launch { 
                isProcessingImage = true
                delay(2000) 
                isProcessingImage = false
                
                val uriStr = uri.toString().lowercase()
                val result = when {
                    uriStr.contains("tomato") -> "Tomato Late Blight" to "Detected Late Blight. Summary: Use copper fungicides. Remove infected leaves."
                    uriStr.contains("paddy") || uriStr.contains("rice") -> "Rice Blast" to "Detected Rice Blast. Summary: Avoid over-fertilizing with Nitrogen."
                    uriStr.contains("leaf") -> "Leaf Spot" to "Fungal infection detected on leaf. Summary: Spray 2% Neem oil solution for control."
                    uriStr.contains("seed") -> "Seed Borne Infection" to "Potential seed infection. Summary: Treat seeds with fungicide before sowing."
                    else -> "Common Crop Issue" to "Detected typical nutrient deficiency. Summary: Balanced NPK fertilizer application recommended."
                }
                showDiseaseResult = result
            }
        } 
    }
    
    if (showDiseaseResult != null) {
        AlertDialog(
            onDismissRequest = { showDiseaseResult = null },
            confirmButton = { Button(onClick = { showDiseaseResult = null }) { Text("Close") } },
            title = { Text(showDiseaseResult!!.first, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = { Text(showDiseaseResult!!.second) }
        )
    }
    
    if (adminTipToEdit != null) {
        EditTipDialog(
            tip = adminTipToEdit!!,
            onDismiss = { adminTipToEdit = null },
            onConfirm = { updatedTip ->
                scope.launch {
                    repository.insertTip(updatedTip)
                    adminTipToEdit = null
                    snackbarHostState.showSnackbar("Crop updated successfully.")
                }
            }
        )
    }

    if (showAddStory || storyToEdit != null) {
        AddSuccessStoryDialog(
            user = user,
            existingStory = storyToEdit,
            onDismiss = { showAddStory = false; storyToEdit = null },
            onConfirm = { title, summary, imageUri ->
                if (storyToEdit != null) {
                    viewModel.updateTip(storyToEdit!!.copy(title = title, instruction = summary, imageUrl = imageUri))
                    storyToEdit = null
                    scope.launch { 
                        snackbarHostState.showSnackbar("Success story updated.")
                    }
                } else {
                    viewModel.addSuccessStory(title, summary, imageUri)
                    showAddStory = false
                    scope.launch { 
                        snackbarHostState.showSnackbar("Success story added successfully.")
                    }
                }
            }
        )
    }

    if (showAddExpertTip) {
        PostExpertTipDialog(
            onDismiss = { showAddExpertTip = false },
            onConfirm = { title, category, content, imageUri ->
                viewModel.postExpertTip(user, title, content, category, imageUri)
                showAddExpertTip = false
                scope.launch { snackbarHostState.showSnackbar("Your tip has been posted and is pending verification for authenticity.") }
            }
        )
    }

    if (showVerificationDialog) {
        FarmerVerificationDialog(
            onDismiss = { showVerificationDialog = false },
            onVerified = { ageVal, expVal, idNum, seedName, docUri ->
                scope.launch {
                    try {
                        snackbarHostState.showSnackbar("Uploading documents... please wait")
                        // Upload image first to get public URL
                        val uploadedUrl = repository.uploadImage(docUri.toUri(), "verification_docs")
                        
                        val updatedUser = user.copy(
                            age = ageVal,
                            yearsOfExperience = expVal,
                            idProofNumber = idNum,
                            seedName = seedName,
                            verificationDocumentUri = uploadedUrl,
                            isPendingExpert = true
                        )
                        repository.updateUser(updatedUser)
                        onUserUpdate(updatedUser)
                        showVerificationDialog = false
                        snackbarHostState.showSnackbar("Verification request sent successfully!")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Upload failed: ${e.message}")
                    }
                }
            },
            onFailed = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                    showVerificationDialog = false
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeOptionRow("Light", themeMode == ThemeMode.LIGHT) {
                        viewModel.setThemeMode(ThemeMode.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Dark", themeMode == ThemeMode.DARK) {
                        viewModel.setThemeMode(ThemeMode.DARK)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("System default", themeMode == ThemeMode.SYSTEM) {
                        viewModel.setThemeMode(ThemeMode.SYSTEM)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Notifications Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeOptionRow("On", notificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setNotificationsEnabled(true)
                            }
                        } else {
                            viewModel.setNotificationsEnabled(true)
                        }
                        showNotificationDialog = false
                    }
                    ThemeOptionRow("Off", !notificationsEnabled) {
                        viewModel.setNotificationsEnabled(false)
                        showNotificationDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState, 
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF424242), // Grey Background
                drawerShape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxHeight().width(310.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B5E20)) 
                            .padding(start = 24.dp, top = 32.dp, bottom = 16.dp)) {
                            Column {
                                GlideImage(
                                    model = user.profileImageUri,
                                    contentDescription = "User Profile",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentScale = ContentScale.Crop,
                                    loading = placeholder { Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(100.dp), tint = Color.LightGray) },
                                    requestBuilderTransform = { it.diskCacheStrategy(DiskCacheStrategy.ALL) }
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.firstName + " " + user.lastName, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 22.sp, 
                                        color = Color.White
                                    )
                                    if (user.isExpert) {
                                        Icon(Icons.Default.Verified, "Expert", tint = Color.Yellow, modifier = Modifier.padding(start = 8.dp).size(20.dp))
                                    }
                                }
                                Text(
                                    text = user.village.ifEmpty { user.city.ifEmpty { "Bengaluru" } }, 
                                    fontSize = 17.sp, 
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        Spacer(Modifier.height(8.dp))
                        
                        DrawerItemSimple("Home", Icons.Outlined.Home) { scope.launch { drawerState.close() } }
                        Spacer(Modifier.height(4.dp))
                        
                        if (user.isAdmin) {
                            DrawerItemSimple("Dashboard", Icons.Outlined.Dashboard) {
                                scope.launch {
                                    drawerState.close()
                                    onOpenDashboard()
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        } else {
                            DrawerItemSimple("Verify Post Card", Icons.Outlined.VerifiedUser) {
                                scope.launch {
                                    drawerState.close()
                                    when {
                                        user.isExpert -> snackbarHostState.showSnackbar("You are already a verified expert.")
                                        user.isPendingExpert -> snackbarHostState.showSnackbar("Your verification is pending admin approval.")
                                        else -> showVerificationDialog = true
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))

                            DrawerItemSimple("Add Post Card", Icons.Outlined.PostAdd) {
                                scope.launch {
                                    drawerState.close()
                                    when {
                                        !user.isExpert -> snackbarHostState.showSnackbar("Verify to add post cards")
                                        hasPendingPost -> snackbarHostState.showSnackbar("Wait for admin approval of your previous post.")
                                        else -> showAddExpertTip = true
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            
                            DrawerItemSimple("Add Success Story", Icons.Outlined.Description) { scope.launch { drawerState.close(); showAddStory = true } }
                            Spacer(Modifier.height(4.dp))
                            DrawerItemSimple("Social Connect", Icons.Outlined.Forum) { scope.launch { drawerState.close(); onOpenSocial() } }
                            Spacer(Modifier.height(4.dp))
                            DrawerItemSimple("Notifications", Icons.Outlined.Notifications) { scope.launch { drawerState.close(); showNotificationDialog = true } }
                            Spacer(Modifier.height(4.dp))
                            DrawerItemSimple("Theme", Icons.Outlined.WbSunny) { scope.launch { drawerState.close(); showThemeDialog = true } }
                            Spacer(Modifier.height(4.dp))
                            DrawerItemSimple("Profile", Icons.Outlined.Person) { scope.launch { drawerState.close(); onOpenProfile() } }
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    DrawerItemRed("Sign Out", Icons.AutoMirrored.Filled.ExitToApp) { scope.launch { drawerState.close(); onSignOut() } }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White, 
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black,
                        actionIconContentColor = Color.Black
                    ),
                    navigationIcon = { 
                        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null) }
                    },
                    title = { RaithaVarthaTitle(appLanguage) },
                    actions = {
                        if (user.isAdmin) {
                            IconButton(onClick = onOpenDashboard) {
                                Icon(Icons.Default.Dashboard, contentDescription = "Admin Dashboard", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF5F5F5),
                            modifier = Modifier.padding(end = 8.dp).height(38.dp),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                val kInteractionSource = remember { MutableInteractionSource() }
                                val kPressed by kInteractionSource.collectIsPressedAsState()
                                val kScale by animateFloatAsState(if (kPressed) 0.9f else 1f, label = "kScale")

                                Text(
                                    text = "ಕನ್ನಡ", 
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = kScale; scaleY = kScale }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (appLanguage == AppLanguage.KANNADA) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable(
                                            interactionSource = kInteractionSource,
                                            indication = ripple(),
                                            onClick = { viewModel.setLanguage(AppLanguage.KANNADA) }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    color = if (appLanguage == AppLanguage.KANNADA) Color.White else Color.Gray, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                
                                val eInteractionSource = remember { MutableInteractionSource() }
                                val ePressed by eInteractionSource.collectIsPressedAsState()
                                val eScale by animateFloatAsState(if (ePressed) 0.9f else 1f, label = "eScale")

                                Text(
                                    text = "EN", 
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = eScale; scaleY = eScale }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (appLanguage == AppLanguage.ENGLISH) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable(
                                            interactionSource = eInteractionSource,
                                            indication = ripple(),
                                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    color = if (appLanguage == AppLanguage.ENGLISH) Color.White else Color.Gray, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { launcher.launch("image/*") }, 
                    containerColor = MaterialTheme.colorScheme.primary, 
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp, end = 8.dp)
                ) { 
                    if (isProcessingImage) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.CameraAlt, null) 
                }
            }
        ) { p ->
            Box(modifier = Modifier.padding(p).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CategoryFilters(categories, selectedCategory) { viewModel.selectCategory(it) }
                    
                    if (isLoading) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (tips.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No content found.")
                        }
                    } else {
                        val pagerState = rememberPagerState { tips.size }
                        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), pageSpacing = 16.dp) { page ->
                            TipCard(
                                tip = tips[page], 
                                lang = appLanguage, 
                                isAdmin = user.isAdmin,
                                isOwner = tips[page].authorEmail == user.email,
                                onGrow = { viewModel.toggleMyCrop(tips[page]) },
                                onEdit = { 
                                    if (user.isAdmin && !it.id.startsWith("story_")) {
                                        adminTipToEdit = it
                                    } else {
                                        storyToEdit = it 
                                    }
                                },
                                onDelete = { 
                                    viewModel.deleteTip(it.id)
                                    scope.launch { snackbarHostState.showSnackbar("Tip deleted successfully.") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun showNotification(context: Context, title: String, message: String) {
    val channelId = "success_story_notifications"
    val notificationId = System.currentTimeMillis().toInt()
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Farmer Success Stories"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = "Notifications for new success stories shared by farmers"
            enableLights(true)
            lightColor = android.graphics.Color.GREEN
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    try {
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    } catch (e: SecurityException) { 
        e.printStackTrace()
    }
}

@Composable
fun DrawerItemSimple(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "drawerScale")
    val bgColor = if (isPressed || isHovered) Color.White.copy(alpha = 0.1f) else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
            Spacer(Modifier.width(16.dp))
            Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
fun DrawerItemRed(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "drawerScale")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color(0xFFEF5350))
            Spacer(Modifier.width(16.dp))
            Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialConnectScreen(currentUser: UserEntity, repository: FirestoreRepository, onBack: () -> Unit) {
    val users by repository.getAllUsers().collectAsState(initial = emptyList())
    
    val villageFarmers = users.filter { 
        it.email != currentUser.email && 
        it.village.isNotBlank() && 
        it.village.equals(currentUser.village, ignoreCase = true) 
    }
    
    val cityFarmers = users.filter { 
        it.email != currentUser.email && 
        it.city.isNotBlank() && 
        it.city.equals(currentUser.city, ignoreCase = true) &&
        !villageFarmers.contains(it)
    }
    
    val otherFarmers = users.filter { 
        it.email != currentUser.email && 
        !villageFarmers.contains(it) && 
        !cityFarmers.contains(it)
    }.shuffled()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social Connect", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).fillMaxSize().background(Color(0xFFF1F8E9))) {
            if (villageFarmers.isNotEmpty()) {
                item { Text("Farmers in your Village (${currentUser.village})", modifier = Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
                items(villageFarmers) { FarmerProfileCard(it) }
            }
            if (cityFarmers.isNotEmpty()) {
                item { Text("Farmers in your City/Town (${currentUser.city})", modifier = Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)) }
                items(cityFarmers) { FarmerProfileCard(it) }
            }
            item { Text("Connect with Farmers Worldwide", modifier = Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2)) }
            items(otherFarmers) { FarmerProfileCard(it) }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FarmerProfileCard(farmer: UserEntity) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GlideImage(
                model = farmer.profileImageUri,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray),
                contentScale = ContentScale.Crop,
                loading = placeholder { Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(60.dp)) },
                requestBuilderTransform = { it.diskCacheStrategy(DiskCacheStrategy.ALL) }
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${farmer.firstName} ${farmer.lastName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (farmer.isExpert) {
                        Icon(Icons.Default.Verified, "Verified", tint = Color.Yellow, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                    }
                }
                Text("${farmer.village.ifEmpty { "Rural" }}, ${farmer.city.ifEmpty { "Town" }}", fontSize = 12.sp, color = Color.Gray)
                Text(farmer.bio, fontSize = 14.sp, color = Color.DarkGray, maxLines = 1)
            }
            IconButton(onClick = { }, modifier = Modifier.background(Color(0xFFE3F2FD), CircleShape)) {
                Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF2196F3))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostExpertTipDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Paddy") }
    var customCategory by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf("Paddy", "Coconut", "Areca nut", "Tomato", "Custom")
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imageUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Expert Agricultural Tip", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Share your experience with other farmers. Your post will be verified for authenticity.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tip Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                Text("Select Crop Category:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                    }
                }
                
                if (selectedCategory == "Custom") {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Enter Crop Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Detailed Instructions") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageUri == null) "Add Crop Image" else "Image Selected ✓")
                }
                error?.let { Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            ScalableButton(onClick = {
                val finalCategory = if (selectedCategory == "Custom") customCategory else selectedCategory
                if (title.isBlank() || content.length < 50 || imageUri == null || (selectedCategory == "Custom" && customCategory.isBlank())) {
                    error = "Please provide a title, crop name, image, and at least 50 chars of content."
                } else {
                    onConfirm(title, finalCategory, content, imageUri!!)
                }
            }) { Text("Post Tip") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddSuccessStoryDialog(user: UserEntity, existingStory: TipEntity? = null, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf(existingStory?.title ?: "") }
    var summary by remember { mutableStateOf(existingStory?.instruction ?: "") }
    var imageUri by remember { mutableStateOf(existingStory?.imageUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var isProcessingFinal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val englishOptions = remember { TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.KANNADA).setTargetLanguage(TranslateLanguage.ENGLISH).build() }
    val kannadaOptions = remember { TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.KANNADA).build() }
    val toEnglishTranslator = remember { Translation.getClient(englishOptions) }
    val toKannadaTranslator = remember { Translation.getClient(kannadaOptions) }

    LaunchedEffect(Unit) {
        val conditions = DownloadConditions.Builder().requireWifi().build()
        toEnglishTranslator.downloadModelIfNeeded(conditions)
        toKannadaTranslator.downloadModelIfNeeded(conditions)
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) } }

    val recognizerListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { isListening = true; isProcessingFinal = false }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { isListening = false; partialText = "" }
        override fun onError(error: Int) { isListening = false; partialText = ""; isProcessingFinal = false }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty() && !isProcessingFinal) {
                isProcessingFinal = true; partialText = ""; translateAndAppend(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty() && !isProcessingFinal) { partialText = matches[0] }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}

        private fun translateAndAppend(text: String) {
            isTranslating = true
            var enRes = text; var knRes = text; var enDone = false; var knDone = false
            fun finalize() { if (enDone && knDone) { val entry = "$knRes | $enRes"; summary = if (summary.isEmpty()) entry else "$summary\n$entry"; isTranslating = false; isProcessingFinal = false } }
            toEnglishTranslator.translate(text).addOnCompleteListener { task -> if (task.isSuccessful) enRes = task.result ?: text; enDone = true; finalize() }
            toKannadaTranslator.translate(text).addOnCompleteListener { task -> if (task.isSuccessful) knRes = task.result ?: text; knDone = true; finalize() }
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognizerListener)
        onDispose { speechRecognizer.destroy(); toEnglishTranslator.close(); toKannadaTranslator.close() }
    }

    val pLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) speechRecognizer.startListening(speechIntent) }
    val imgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) imageUri = uri.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(if (existingStory != null) "Edit Story/Tip" else "Share Your Success Story", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); if (isTranslating) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Share details and inspire others.", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { imgLauncher.launch("image/*") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp)); Text(if (imageUri == null) "Photo" else "Added") }
                    IconButton(onClick = { if (isListening) speechRecognizer.stopListening() else { if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) speechRecognizer.startListening(speechIntent) else pLauncher.launch(android.Manifest.permission.RECORD_AUDIO) } }, modifier = Modifier.size(48.dp).background(if (isListening) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer, CircleShape).border(1.dp, if (isListening) Color.Red else Color.Transparent, CircleShape)) { Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, null, tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = if (partialText.isNotEmpty()) (summary.trim() + "\n" + partialText.trim()).trim() else summary, onValueChange = { summary = it; error = null }, label = { Text("How did you get success?") }, modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(12.dp))
                error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
            }
        },
        confirmButton = { ScalableButton(onClick = { if (summary.length < 80) error = "It should be at least 2-3 lines long." else if (imageUri == null) error = "Please add a photo." else onConfirm(title.ifBlank { (existingStory?.title ?: "Farmer Success: ${user.firstName}") }, summary, imageUri!!) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ProfileScreen(user: UserEntity, repository: FirestoreRepository, onBack: () -> Unit, onUpdate: (UserEntity) -> Unit) {
    var editMode by remember { mutableStateOf(false) }
    var fName by remember { mutableStateOf(user.firstName) }
    var lName by remember { mutableStateOf(user.lastName) }
    var phone by remember { mutableStateOf(user.phoneNumber) }
    var city by remember { mutableStateOf(user.city) }
    var village by remember { mutableStateOf(user.village) }
    var bio by remember { mutableStateOf(user.bio) }
    var ageVal by remember { mutableIntStateOf(user.age) }
    var expVal by remember { mutableIntStateOf(user.yearsOfExperience) }
    var profileUri by remember { mutableStateOf(user.profileImageUri) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) profileUri = uri.toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (!editMode) IconButton(onClick = { editMode = true }) { Icon(Icons.Default.Edit, null) }
                    else IconButton(onClick = { scope.launch { val updated = user.copy(firstName = fName, lastName = lName, phoneNumber = phone, profileImageUri = profileUri, city = city, village = village, bio = bio, age = ageVal, yearsOfExperience = expVal); repository.updateUser(updated); onUpdate(updated); Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show(); editMode = false } }) { Icon(Icons.Default.Check, null) }
                }
            )
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier.size(150.dp).clickable(enabled = editMode) { launcher.launch("image/*") }) {
                GlideImage(model = profileUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentScale = ContentScale.Crop, loading = placeholder { Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(100.dp)) }, requestBuilderTransform = { it.diskCacheStrategy(DiskCacheStrategy.ALL) })
                if (editMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clip(CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, null, tint = Color.White) }
            }
            Spacer(Modifier.height(32.dp))
            Column(modifier = Modifier.padding(16.dp)) {
                if (!editMode) {
                    ProfileField("Name", "${user.firstName} ${user.lastName}")
                    ProfileField("Phone", user.phoneNumber)
                    ProfileField("Email", user.email)
                    ProfileField("Village", user.village)
                    ProfileField("Age", user.age.toString())
                    ProfileField("Experience", "${user.yearsOfExperience} Years")
                } else {
                    OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = lName, onValueChange = { lName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ageVal.toString(), onValueChange = { ageVal = it.toIntOrNull() ?: ageVal }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = expVal.toString(), onValueChange = { expVal = it.toIntOrNull() ?: expVal }, label = { Text("Experience") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilters(cats: List<String>, sel: String?, onSel: (String?) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cats) { c ->
            val isSelected = sel == c || (c == "All" && sel == null)
            FilterChip(selected = isSelected, onClick = { onSel(if (c == "All") null else c) }, label = { Text(c) })
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TipCard(tip: TipEntity, lang: AppLanguage, isAdmin: Boolean, isOwner: Boolean, onGrow: () -> Unit, onEdit: (TipEntity) -> Unit = {}, onDelete: (TipEntity) -> Unit = {}) {
    val title = if (lang == AppLanguage.KANNADA) tip.title.split("|")[0] else tip.title.split("|").getOrElse(1){tip.title}
    val desc = if (lang == AppLanguage.KANNADA) tip.instruction.split("|")[0] else tip.instruction.split("|").getOrElse(1){tip.instruction}
    
    ElevatedCard(modifier = Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column {
            Box(modifier = Modifier.weight(1.3f).fillMaxWidth()) {
                GlideImage(model = tip.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, loading = placeholder { CircularProgressIndicator() }, requestBuilderTransform = { it.diskCacheStrategy(DiskCacheStrategy.ALL) })
                if (tip.isVerified) Icon(Icons.Default.Verified, null, tint = Color.Yellow, modifier = Modifier.padding(16.dp).align(Alignment.TopEnd))
            }
            Column(modifier = Modifier.padding(16.dp).weight(1f).verticalScroll(rememberScrollState())) {
                if (tip.category.isNotBlank()) {
                    Text(
                        text = tip.category.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title.trim(), modifier = Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    // Admin can always edit/delete. 
                    // Owners can only edit/delete if it's NOT a Post Card (Success Stories etc.)
                    // Experts cannot edit/delete their Post Cards once submitted for verification.
                    val canEditOrDelete = isAdmin || (isOwner && !tip.isPostCard)
                    
                    if (canEditOrDelete) {
                        IconButton(onClick = { onEdit(tip) }) { Icon(Icons.Default.Edit, null) }
                        IconButton(onClick = { onDelete(tip) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                }
                Text(desc.trim(), fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                ScalableButton(onClick = onGrow, modifier = Modifier.fillMaxWidth()) { Icon(if (tip.isUserCrop) Icons.Default.Check else Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(if (tip.isUserCrop) "Added to My Crops" else "Add to My Crops") }
            }
        }
    }
}

@Composable
fun ThemeOptionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(Modifier.width(12.dp)); Text(label)
    }
}

@Composable
fun FarmerVerificationDialog(onDismiss: () -> Unit, onVerified: (Int, Int, String, String, String) -> Unit, onFailed: (String) -> Unit) {
    var idNum by remember { mutableStateOf("") }
    var ageVal by remember { mutableStateOf("") }
    var expVal by remember { mutableStateOf("") }
    var seedName by remember { mutableStateOf("") }
    var docUri by remember { mutableStateOf<String?>(null) }
    var otpEntered by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    
    val imgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) docUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Farmer Identity & Crop Verification", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Please provide details for admin verification to become an expert.", fontSize = 14.sp, color = Color.Gray)
                
                OutlinedTextField(value = seedName, onValueChange = { seedName = it }, label = { Text("Crop/Seed Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                OutlinedTextField(value = ageVal, onValueChange = { ageVal = it }, label = { Text("Your Current Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                OutlinedTextField(value = expVal, onValueChange = { expVal = it }, label = { Text("Years of Farming Experience") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                OutlinedTextField(
                    value = idNum, 
                    onValueChange = { if (it.length <= 12) idNum = it }, 
                    label = { Text("12-Digit Aadhar Number") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isOtpSent
                )

                if (!isOtpSent) {
                    Button(
                        onClick = { 
                            if (idNum.length == 12) {
                                isOtpSent = true 
                                // In real app, call API to send OTP
                            } else {
                                // show error for aadhar length
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = idNum.length == 12,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send OTP to Linked Mobile")
                    }
                } else {
                    OutlinedTextField(
                        value = otpEntered, 
                        onValueChange = { otpEntered = it }, 
                        label = { Text("Enter OTP") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("OTP sent to mobile linked with $idNum", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { imgLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (docUri == null) "Upload Crop Document (Bill/Cert)" else "Document Attached ✓")
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = {
                    val aInt = ageVal.toIntOrNull() ?: 0
                    val eInt = expVal.toIntOrNull() ?: 0
                    when {
                        seedName.isBlank() -> onFailed("Please enter seed/crop name.")
                        aInt <= 30 -> onFailed("Sorry your age is not eligible for this posting")
                        eInt > (aInt - 18) || eInt < 5 -> onFailed("Experience details do not match.")
                        idNum.length != 12 -> onFailed("Valid 12-digit Aadhar required.")
                        !isOtpSent -> onFailed("Please send and verify OTP.")
                        otpEntered != "1234" -> onFailed("Invalid OTP. Use 1234 for demo.") // Simulated OTP check
                        docUri == null -> onFailed("Please upload crop documentation.")
                        else -> onVerified(aInt, eInt, idNum, seedName, docUri!!)
                    }
                },
                enabled = isOtpSent && otpEntered.length >= 4
            ) { 
                Text("Verify & Submit") 
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun AdminDashboardScreen(repository: FirestoreRepository, onBack: () -> Unit) {
    val users by repository.getAllUsers().collectAsState(initial = emptyList())
    val tips by repository.getAllTips().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Zoom Overlay Dialog
    if (zoomedImageUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { zoomedImageUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { zoomedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                GlideImage(
                    model = zoomedImageUrl,
                    contentDescription = "Zoomed Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { zoomedImageUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
                Text(
                    "Tap anywhere to close", 
                    color = Color.White.copy(alpha = 0.5f), 
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                    fontSize = 12.sp
                )
            }
        }
    }

    val pendingExperts = users.filter { it.isPendingExpert && !it.isExpert }
    val pendingTips = tips.filter { it.isPostCard && !it.isAdminApproved }

    LaunchedEffect(users) {
        users.forEach { 
            android.util.Log.d("AdminDashboard", "User: ${it.email}, isPending: ${it.isPendingExpert}, docUri: ${it.verificationDocumentUri}") 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Approval Center", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { p ->
        Surface(
            modifier = Modifier.padding(p).fillMaxSize(),
            color = Color.Black // Total Black Background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    divider = { HorizontalDivider(color = Color.DarkGray) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Users (${pendingExperts.size})", fontWeight = FontWeight.Bold, color = if(selectedTab == 0) Color.White else Color.Gray) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Post Cards (${pendingTips.size})", fontWeight = FontWeight.Bold, color = if(selectedTab == 1) Color.White else Color.Gray) }
                    )
                }

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (pendingExperts.isEmpty()) {
                            item { 
                                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No pending user verifications.", color = Color.LightGray)
                                }
                            }
                        } else {
                            items(pendingExperts) { user ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E1E1E)) // Dark Card
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text("Expert Request", fontSize = 14.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                        Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
                                        Spacer(Modifier.height(12.dp))
                                        
                                        AdminInfoRow(Icons.Default.Email, user.email)
                                        AdminInfoRow(Icons.Default.History, "${user.yearsOfExperience} Years Experience")
                                        AdminInfoRow(Icons.Default.Agriculture, "Specialization: ${user.seedName}")
                                        
                                        if (!user.verificationDocumentUri.isNullOrEmpty()) {
                                            Spacer(Modifier.height(16.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Verification Proof:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Spacer(Modifier.weight(1f))
                                                IconButton(
                                                    onClick = { zoomedImageUrl = user.verificationDocumentUri },
                                                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.ZoomIn, "Zoom", tint = Color.White)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(250.dp)
                                                    .padding(top = 8.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                                                    .clickable { zoomedImageUrl = user.verificationDocumentUri }
                                            ) {
                                                GlideImage(
                                                    model = user.verificationDocumentUri,
                                                    contentDescription = "Document",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit,
                                                    loading = placeholder { 
                                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            CircularProgressIndicator(color = Color.White)
                                                        }
                                                    }
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.height(16.dp))
                                            Text("No document attached.", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        Spacer(Modifier.height(24.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(
                                                onClick = { scope.launch { repository.updateUser(user.copy(isExpert = true, isPendingExpert = false)) } },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text("Approve", fontWeight = FontWeight.Bold) }
                                            
                                            OutlinedButton(
                                                onClick = { scope.launch { repository.updateUser(user.copy(isPendingExpert = false)) } },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.5.dp, Color.Red)
                                            ) { Text("Reject", fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (pendingTips.isEmpty()) {
                            item { 
                                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No pending post cards.", color = Color.LightGray)
                                }
                            }
                        } else {
                            items(pendingTips) { tip ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E1E1E)) // Dark Card
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(tip.category.uppercase(), fontSize = 12.sp, color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold)
                                        Text(tip.title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
                                        Text("By ${tip.authorName}", fontSize = 14.sp, color = Color.LightGray)
                                        
                                        Spacer(Modifier.height(16.dp))
                                        Text(tip.instruction, fontSize = 16.sp, lineHeight = 22.sp, color = Color.White)
                                        
                                        if (tip.imageUrl.isNotEmpty()) {
                                            Spacer(Modifier.height(16.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Image Preview:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Spacer(Modifier.weight(1f))
                                                IconButton(onClick = { zoomedImageUrl = tip.imageUrl }) {
                                                    Icon(Icons.Default.ZoomIn, "Zoom", tint = Color.White)
                                                }
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)).clickable { zoomedImageUrl = tip.imageUrl }) {
                                                GlideImage(
                                                    model = tip.imageUrl, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        
                                        Spacer(Modifier.height(24.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(
                                                onClick = { scope.launch { repository.insertTip(tip.copy(isAdminApproved = true, isVerified = true)) } },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text("Approve Post", fontWeight = FontWeight.Bold) }
                                            
                                            OutlinedButton(
                                                onClick = { scope.launch { repository.deleteTip(tip.id) } },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.5.dp, Color.Red)
                                            ) { Text("Reject", fontWeight = FontWeight.Bold) }
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
fun AdminInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.LightGray)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 15.sp, color = Color.White)
    }
}

@Composable
fun EditTipDialog(tip: TipEntity, onDismiss: () -> Unit, onConfirm: (TipEntity) -> Unit) {
    var title by remember { mutableStateOf(tip.title) }
    var instruction by remember { mutableStateOf(tip.instruction) }
    var imageUrl by remember { mutableStateOf(tip.imageUrl) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imageUrl = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Crop/Tip", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = instruction, onValueChange = { instruction = it }, label = { Text("Content") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Image")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tip.copy(title = title, instruction = instruction, imageUrl = imageUrl)) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
