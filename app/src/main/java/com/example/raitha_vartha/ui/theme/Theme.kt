package com.example.raitha_vartha.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NatureGreen,
    secondary = EarthyBrown,
    tertiary = DeepForest,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = PureWhite,
    surfaceVariant = Color(0xFF2C2C2C),
    primaryContainer = Color(0xFF33691E),
    secondaryContainer = Color(0xFF4E342E)
)

private val LightColorScheme = lightColorScheme(
    primary = NatureGreen,
    secondary = EarthyBrown,
    tertiary = DeepForest,
    background = FreshMeadow,
    surface = Color(0xFFF7FAF2), // Slightly green-tinted white for better fit
    onPrimary = PureWhite,
    onBackground = EarthyBrown,
    onSurface = EarthyBrown,
    surfaceVariant = Color(0xFFE8F5E9),
    primaryContainer = Color(0xFFDCEDC8), // Light green container
    secondaryContainer = Color(0xFFD7CCC8) // Light brown container
)

@Composable
fun RaithaVarthaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize our custom Nature Green theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
