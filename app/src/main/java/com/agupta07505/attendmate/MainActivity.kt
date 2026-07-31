package com.agupta07505.attendmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.agupta07505.attendmate.data.preferences.UserPreferences
import com.agupta07505.attendmate.ui.navigation.NavGraph
import com.agupta07505.attendmate.ui.theme.AttendMateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AttendMateApplication
        val userPreferencesRepository = app.userPreferencesRepository

        setContent {
            val userPreferences by userPreferencesRepository.userPreferencesFlow
                .collectAsState(initial = UserPreferences())

            val scope = rememberCoroutineScope()

            AttendMateTheme(
                themeMode = userPreferences.themeMode,
                dynamicColor = userPreferences.dynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        userPreferences = userPreferences,
                        onCompleteOnboarding = {
                            scope.launch {
                                userPreferencesRepository.updateOnboardingCompleted(true)
                            }
                        }
                    )
                }
            }
        }
    }
}
