/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "Onboarding")
    object Setup : Screen("setup", "Setup")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Timetable : Screen("timetable", "Timetable", Icons.Default.CalendarToday)
    object Subjects : Screen("subjects", "Subjects", Icons.Default.Book)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object SubjectDetail : Screen("subject_detail/{subjectId}", "Subject Detail") {
        fun createRoute(subjectId: Long) = "subject_detail/$subjectId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Timetable, Subjects, Analytics)
    }
}
