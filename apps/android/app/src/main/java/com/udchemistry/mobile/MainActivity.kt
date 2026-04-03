package com.udchemistry.mobile

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.udchemistry.mobile.data.ChemistryRepository
import com.udchemistry.mobile.data.ThemeStore
import com.udchemistry.mobile.ui.ChemistryMobileApp
import com.udchemistry.mobile.ui.MainViewModel
import com.udchemistry.mobile.ui.MainViewModelFactory
import com.udchemistry.mobile.ui.theme.UDChemistryTheme
import com.udchemistry.mobile.ui.theme.shouldUseDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            repository = ChemistryRepository(applicationContext),
            themeStore = ThemeStore(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val useDarkTheme = shouldUseDarkTheme(uiState.themeMode)

            enableEdgeToEdge(
                statusBarStyle = if (useDarkTheme) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = if (useDarkTheme) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            )

            UDChemistryTheme(themeMode = uiState.themeMode) {
                ChemistryMobileApp(viewModel = viewModel)
            }
        }
    }
}
