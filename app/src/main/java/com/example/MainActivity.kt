package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.ai.configureAppCheck
import com.example.data.local.KonozDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.media.ImageStorageManager
import com.example.data.repository.KonozRepository
import com.example.ui.KonozApp
import com.example.ui.viewmodel.KonozViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch Intent BEFORE ViewModels, Firebase, or UI
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val database = KonozDatabase.getInstance(applicationContext)
        val settingsStore = SettingsDataStore(applicationContext)
        val imageManager = ImageStorageManager(applicationContext)
        val repository = KonozRepository(
            context = applicationContext,
            dao = database.konozDao(),
            settingsStore = settingsStore,
            imageStorageManager = imageManager
        )

        val viewModel = ViewModelProvider(
            this,
            KonozViewModel.Factory(repository)
        )[KonozViewModel::class.java]

        setContent {
            KonozApp(viewModel = viewModel)
        }
    }
}
