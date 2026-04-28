package com.keacs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.keacs.app.data.local.PreferencesManager
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.usecase.InitializeLocalDataUseCase
import com.keacs.app.ui.KeacsApp
import com.keacs.app.ui.theme.KeacsTheme
import com.keacs.app.ui.welcome.LoadingScreen
import com.keacs.app.ui.welcome.WelcomeScreen
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager.getInstance(applicationContext)
        val repository = LocalDataRepository(KeacsDatabase.getInstance(applicationContext))

        setContent {
            KeacsTheme {
                AppContent(repository, preferencesManager)
            }
        }
    }
}

@Composable
private fun AppContent(
    repository: LocalDataRepository,
    preferencesManager: PreferencesManager,
) {
    var hasWelcomed by remember { mutableStateOf<Boolean?>(null) }
    var localDataReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasWelcomed = preferencesManager.hasWelcomed.first()
        InitializeLocalDataUseCase(repository)()
        localDataReady = true
    }

    when {
        hasWelcomed == null -> LoadingScreen()
        hasWelcomed == false -> WelcomeScreen(
            onStartClick = {
                preferencesManager.setHasWelcomed()
                hasWelcomed = true
            },
        )
        localDataReady -> KeacsApp(
            repository = repository,
            preferencesManager = preferencesManager,
        )
        else -> LoadingScreen()
    }
}
