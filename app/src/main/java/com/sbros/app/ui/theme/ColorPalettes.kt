package com.sbros.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.sbros.app.domain.model.ThemePalette

// =============================================================================
// 1. ШАЛФЕЙ (SAGE) — По умолчанию
// =============================================================================
private val SageLightColorScheme = lightColorScheme(
    primary = Color(0xFF356847),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0C6),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF4F6352),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8D5),
    onSecondaryContainer = Color(0xFF0E1F13),
    tertiary = Color(0xFF3A656E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4EBF2),
    onTertiaryContainer = Color(0xFF001F25),
    background = Color(0xFFE5E9E3),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF7FAF5),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDEE5DC),
    onSurfaceVariant = Color(0xFF4E584E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF4EC),
    surfaceContainer = Color(0xFFE8EFE5),
    surfaceContainerHigh = Color(0xFFDFEAD9),
    surfaceContainerHighest = Color(0xFFD7E2D1),
    outline = Color(0xFFC1CCC0),
    outlineVariant = Color(0xFFD8E1D7)
)

private val SageDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD3AC),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF1C5031),
    onPrimaryContainer = Color(0xFFB7F0C6),
    secondary = Color(0xFFB6CBB9),
    onSecondary = Color(0xFF223526),
    secondaryContainer = Color(0xFF394B3C),
    onSecondaryContainer = Color(0xFFD3E8D5),
    tertiary = Color(0xFFA2CED5),
    onTertiary = Color(0xFF05353C),
    tertiaryContainer = Color(0xFF204D54),
    onTertiaryContainer = Color(0xFFC4EBF2),
    background = Color(0xFF0B0E0B),
    onBackground = Color(0xFFE2E7E0),
    surface = Color(0xFF111511),
    onSurface = Color(0xFFE2E7E0),
    surfaceVariant = Color(0xFF434F43),
    onSurfaceVariant = Color(0xFF9BA79B),
    surfaceContainerLowest = Color(0xFF080B08),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1E251E),
    surfaceContainerHigh = Color(0xFF273027),
    surfaceContainerHighest = Color(0xFF323B32),
    outline = Color(0xFF364136),
    outlineVariant = Color(0xFF434F43)
)

// =============================================================================
// 2. ТЕРРАКОТА (CLAY / SAND)
// =============================================================================
private val ClayLightColorScheme = lightColorScheme(
    primary = Color(0xFF8C4A27),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF775748),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5DDD2),
    onSecondaryContainer = Color(0xFF2B160D),
    tertiary = Color(0xFF655F33),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEFE4B8),
    onTertiaryContainer = Color(0xFF211B00),
    background = Color(0xFFEDE6E1),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFDF8F5),
    onSurface = Color(0xFF221A16),
    surfaceVariant = Color(0xFFF4DDD3),
    onSurfaceVariant = Color(0xFF62534B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7EFEA),
    surfaceContainer = Color(0xFFF1E7E0),
    surfaceContainerHigh = Color(0xFFE9DDD5),
    surfaceContainerHighest = Color(0xFFE3D6CD),
    outline = Color(0xFFD6C4B9),
    outlineVariant = Color(0xFFE6D6CC)
)

private val ClayDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB690),
    onPrimary = Color(0xFF532203),
    primaryContainer = Color(0xFF6F3413),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFFE7BEAC),
    onSecondary = Color(0xFF442A1D),
    secondaryContainer = Color(0xFF5A4136),
    onSecondaryContainer = Color(0xFFF5DDD2),
    tertiary = Color(0xFFD2C89E),
    onTertiary = Color(0xFF363109),
    tertiaryContainer = Color(0xFF4F4623),
    onTertiaryContainer = Color(0xFFEFE4B8),
    background = Color(0xFF110D0B),
    onBackground = Color(0xFFEFE0D9),
    surface = Color(0xFF191310),
    onSurface = Color(0xFFEFE0D9),
    surfaceVariant = Color(0xFF54423A),
    onSurfaceVariant = Color(0xFFB39E94),
    surfaceContainerLowest = Color(0xFF0C0807),
    surfaceContainerLow = Color(0xFF221A16),
    surfaceContainer = Color(0xFF2B211C),
    surfaceContainerHigh = Color(0xFF362A24),
    surfaceContainerHighest = Color(0xFF42342D),
    outline = Color(0xFF4B3C35),
    outlineVariant = Color(0xFF5A4B43)
)

// =============================================================================
// 3. ЛАВАНДА (LAVENDER / MIST)
// =============================================================================
private val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF585489),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0DFFF),
    onPrimaryContainer = Color(0xFF150E42),
    secondary = Color(0xFF605D71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E0F4),
    onSecondaryContainer = Color(0xFF191A2B),
    tertiary = Color(0xFF7D5262),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E8),
    onTertiaryContainer = Color(0xFF311021),
    background = Color(0xFFE6E5EC),
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFE5E0EE),
    onSurfaceVariant = Color(0xFF565462),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F0FA),
    surfaceContainer = Color(0xFFECE8F4),
    surfaceContainerHigh = Color(0xFFE4DFEE),
    surfaceContainerHighest = Color(0xFFDCD7E6),
    outline = Color(0xFFC8C4D4),
    outlineVariant = Color(0xFFDAD7E5)
)

private val LavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC2BFFF),
    onPrimary = Color(0xFF2A2658),
    primaryContainer = Color(0xFF403C6F),
    onPrimaryContainer = Color(0xFFE0DFFF),
    secondary = Color(0xFFC8C4DC),
    onSecondary = Color(0xFF313042),
    secondaryContainer = Color(0xFF444559),
    onSecondaryContainer = Color(0xFFE1E0F4),
    tertiary = Color(0xFFEEB8CD),
    onTertiary = Color(0xFF492534),
    tertiaryContainer = Color(0xFF5F3B4B),
    onTertiaryContainer = Color(0xFFFFD8E8),
    background = Color(0xFF0C0B10),
    onBackground = Color(0xFFE5E1EC),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1EC),
    surfaceVariant = Color(0xFF474454),
    onSurfaceVariant = Color(0xFF9E9BAE),
    surfaceContainerLowest = Color(0xFF08070B),
    surfaceContainerLow = Color(0xFF1B1A22),
    surfaceContainer = Color(0xFF22212B),
    surfaceContainerHigh = Color(0xFF2C2B37),
    surfaceContainerHighest = Color(0xFF373643),
    outline = Color(0xFF3F3D4C),
    outlineVariant = Color(0xFF4C4A5A)
)

// =============================================================================
// 4. ОКЕАН (OCEAN / TEAL)
// =============================================================================
private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF176678),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB9EAFA),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF4E6167),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6EE),
    onSecondaryContainer = Color(0xFF071E25),
    tertiary = Color(0xFF585C7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E2FF),
    onTertiaryContainer = Color(0xFF0A1B37),
    background = Color(0xFFE1E8EB),
    onBackground = Color(0xFF161D20),
    surface = Color(0xFFF5FAFC),
    onSurface = Color(0xFF161D20),
    surfaceVariant = Color(0xFFDCE6EA),
    onSurfaceVariant = Color(0xFF4B585E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFECF4F7),
    surfaceContainer = Color(0xFFE4EEF2),
    surfaceContainerHigh = Color(0xFFDCE7EC),
    surfaceContainerHighest = Color(0xFFD2E0E6),
    outline = Color(0xFFBCCACE),
    outlineVariant = Color(0xFFCEDCE0)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD0E4),
    onPrimary = Color(0xFF003541),
    primaryContainer = Color(0xFF004E5D),
    onPrimaryContainer = Color(0xFFB9EAFA),
    secondary = Color(0xFFB4C8D0),
    onSecondary = Color(0xFF1F3339),
    secondaryContainer = Color(0xFF354A52),
    onSecondaryContainer = Color(0xFFCFE6EE),
    tertiary = Color(0xFFBFC4EB),
    onTertiary = Color(0xFF292E4E),
    tertiaryContainer = Color(0xFF344665),
    onTertiaryContainer = Color(0xFFD8E2FF),
    background = Color(0xFF090D0F),
    onBackground = Color(0xFFDFE4E7),
    surface = Color(0xFF101518),
    onSurface = Color(0xFFDFE4E7),
    surfaceVariant = Color(0xFF3D494F),
    onSurfaceVariant = Color(0xFF94A3A9),
    surfaceContainerLowest = Color(0xFF050809),
    surfaceContainerLow = Color(0xFF161D20),
    surfaceContainer = Color(0xFF1C2529),
    surfaceContainerHigh = Color(0xFF263136),
    surfaceContainerHighest = Color(0xFF313D43),
    outline = Color(0xFF324046),
    outlineVariant = Color(0xFF404F56)
)

/**
 * Returns the ColorScheme for the given palette and dark/light mode.
 */
fun getSbrosColorScheme(palette: ThemePalette, darkTheme: Boolean): ColorScheme {
    return when (palette) {
        ThemePalette.SAGE -> if (darkTheme) SageDarkColorScheme else SageLightColorScheme
        ThemePalette.CLAY -> if (darkTheme) ClayDarkColorScheme else ClayLightColorScheme
        ThemePalette.LAVENDER -> if (darkTheme) LavenderDarkColorScheme else LavenderLightColorScheme
        ThemePalette.OCEAN -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
    }
}
