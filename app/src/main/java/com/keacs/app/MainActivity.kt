package com.keacs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.usecase.InitializeLocalDataUseCase
import com.keacs.app.ui.KeacsApp
import com.keacs.app.ui.theme.KeacsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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

        lifecycleScope.launch {
            InitializeLocalDataUseCase(
                LocalDataRepository(KeacsDatabase.getInstance(applicationContext)),
            )()
        }

        setContent {
            KeacsTheme {
                KeacsApp()
            }
        }
    }
}
