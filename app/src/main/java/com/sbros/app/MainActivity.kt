package com.sbros.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.domain.model.ThemePalette
import com.sbros.app.ui.navigation.SbrosAppShell
import com.sbros.app.ui.theme.SbrosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val palette by preferencesManager.themePaletteFlow.collectAsState(initial = ThemePalette.SAGE)
            val isDarkPref by preferencesManager.isDarkModeFlow.collectAsState(initial = null)
            val darkTheme = isDarkPref ?: isSystemInDarkTheme()

            SbrosTheme(palette = palette, darkTheme = darkTheme) {
                SbrosAppShell()
            }
        }
    }
}
