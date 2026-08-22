package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryColor,
    background = BackgroundColor,
    surface = CardWhite,
    onPrimary = CardWhite,
    onBackground = OnSurfaceText,
    onSurface = OnSurfaceText,
    surfaceVariant = BackgroundColor,
    onSurfaceVariant = OnSurfaceText,
    outline = OutlineColor
  )

private val DarkColorScheme = LightColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
