package com.agupta07505.attendmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agupta07505.attendmate.AttendMateApplication
import com.agupta07505.attendmate.ui.screens.analytics.AnalyticsViewModel
import com.agupta07505.attendmate.ui.screens.history.HistoryViewModel
import com.agupta07505.attendmate.ui.screens.home.HomeViewModel
import com.agupta07505.attendmate.ui.screens.settings.SettingsViewModel
import com.agupta07505.attendmate.ui.screens.subjectdetails.SubjectDetailViewModel
import com.agupta07505.attendmate.ui.screens.subjects.SubjectsViewModel
import com.agupta07505.attendmate.ui.screens.timetable.TimetableViewModel

@Composable
inline fun <reified T : ViewModel> getViewModel(
    crossinline creator: (app: AttendMateApplication) -> T
): T {
    val context = LocalContext.current.applicationContext as AttendMateApplication
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                return creator(context) as VM
            }
        }
    )
}
