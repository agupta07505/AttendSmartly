package com.agupta07505.attendmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
            val userPreferencesState by userPreferencesRepository.userPreferencesFlow
                .collectAsState(initial = null)

            val scope = rememberCoroutineScope()

            val prefs = userPreferencesState ?: UserPreferences()

            AttendMateTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (userPreferencesState == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    } else {
                        NavGraph(
                            userPreferences = prefs,
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
}
