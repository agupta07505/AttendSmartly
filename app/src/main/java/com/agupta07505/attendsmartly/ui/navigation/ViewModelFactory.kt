/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agupta07505.attendsmartly.AttendSmartlyApplication
import com.agupta07505.attendsmartly.ui.screens.analytics.AnalyticsViewModel
import com.agupta07505.attendsmartly.ui.screens.history.HistoryViewModel
import com.agupta07505.attendsmartly.ui.screens.home.HomeViewModel
import com.agupta07505.attendsmartly.ui.screens.settings.SettingsViewModel
import com.agupta07505.attendsmartly.ui.screens.subjectdetails.SubjectDetailViewModel
import com.agupta07505.attendsmartly.ui.screens.subjects.SubjectsViewModel
import com.agupta07505.attendsmartly.ui.screens.timetable.TimetableViewModel

@Composable
inline fun <reified T : ViewModel> getViewModel(
    crossinline creator: (app: AttendSmartlyApplication) -> T
): T {
    val context = LocalContext.current.applicationContext as AttendSmartlyApplication
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                return creator(context) as VM
            }
        }
    )
}
