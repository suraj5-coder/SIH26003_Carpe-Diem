package org.mindmate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val CalmColors = lightColorScheme(
    primary = Color(0xFF174D3C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5E7),
    onPrimaryContainer = Color(0xFF08291F),
    secondary = Color(0xFF704C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1A3),
    onSecondaryContainer = Color(0xFF241A00),
    error = Color(0xFF9D1B1B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFFFF9F0),
    onBackground = Color(0xFF1D1C18),
    surface = Color(0xFFFFF9F0),
    onSurface = Color(0xFF1D1C18),
    outline = Color(0xFF57564F),
)

private val AccessibleTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun MindMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CalmColors, typography = AccessibleTypography, content = content)
}
