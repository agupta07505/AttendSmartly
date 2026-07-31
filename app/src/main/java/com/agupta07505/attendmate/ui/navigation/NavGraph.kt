package com.agupta07505.attendmate.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.agupta07505.attendmate.data.preferences.UserPreferences
import com.agupta07505.attendmate.ui.screens.analytics.AnalyticsScreen
import com.agupta07505.attendmate.ui.screens.analytics.AnalyticsViewModel
import com.agupta07505.attendmate.ui.screens.history.AttendanceHistoryScreen
import com.agupta07505.attendmate.ui.screens.history.HistoryViewModel
import com.agupta07505.attendmate.ui.screens.home.HomeScreen
import com.agupta07505.attendmate.ui.screens.home.HomeViewModel
import com.agupta07505.attendmate.ui.screens.onboarding.OnboardingScreen
import com.agupta07505.attendmate.ui.screens.onboarding.SetupScreen
import com.agupta07505.attendmate.ui.screens.settings.SettingsScreen
import com.agupta07505.attendmate.ui.screens.settings.SettingsViewModel
import com.agupta07505.attendmate.ui.screens.subjectdetails.SubjectDetailScreen
import com.agupta07505.attendmate.ui.screens.subjectdetails.SubjectDetailViewModel
import com.agupta07505.attendmate.ui.screens.subjects.SubjectsScreen
import com.agupta07505.attendmate.ui.screens.subjects.SubjectsViewModel
import com.agupta07505.attendmate.ui.screens.timetable.TimetableScreen
import com.agupta07505.attendmate.ui.screens.timetable.TimetableViewModel
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    userPreferences: UserPreferences,
    onCompleteOnboarding: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let { Icon(it, contentDescription = screen.title) }
                            },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (!userPreferences.onboardingCompleted) Screen.Onboarding.route else Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = { navController.navigate(Screen.Setup.route) },
                    onSkip = {
                        onCompleteOnboarding()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Setup.route) {
                val settingsVm: SettingsViewModel = getViewModel { app -> SettingsViewModel(app.repository, app.userPreferencesRepository) }
                val subjectsVm: SubjectsViewModel = getViewModel { app -> SubjectsViewModel(app.repository) }

                SetupScreen(
                    onSaveSetup = { targetPercent, _, _, firstSubject ->
                        settingsVm.updateDefaultTargetAttendance(targetPercent)
                        firstSubject?.let { subjectsVm.addSubject(it) }
                        onCompleteOnboarding()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    },
                    onSkipSetup = {
                        onCompleteOnboarding()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = getViewModel { app -> HomeViewModel(app.repository, app.userPreferencesRepository) }
                HomeScreen(
                    viewModel = homeVm,
                    onNavigateToAddSubject = { navController.navigate(Screen.Subjects.route) },
                    onNavigateToAddTimetable = { navController.navigate(Screen.Timetable.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Timetable.route) {
                val timetableVm: TimetableViewModel = getViewModel { app -> TimetableViewModel(app.repository) }
                TimetableScreen(viewModel = timetableVm)
            }

            composable(Screen.Subjects.route) {
                val subjectsVm: SubjectsViewModel = getViewModel { app -> SubjectsViewModel(app.repository) }
                SubjectsScreen(
                    viewModel = subjectsVm,
                    onNavigateToSubjectDetail = { subId ->
                        navController.navigate(Screen.SubjectDetail.createRoute(subId))
                    }
                )
            }

            composable(
                route = Screen.SubjectDetail.route,
                arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: 0L
                val detailVm: SubjectDetailViewModel = getViewModel { app -> SubjectDetailViewModel(app.repository, subjectId) }
                SubjectDetailScreen(
                    viewModel = detailVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Analytics.route) {
                val analyticsVm: AnalyticsViewModel = getViewModel { app -> AnalyticsViewModel(app.repository, app.userPreferencesRepository) }
                AnalyticsScreen(viewModel = analyticsVm)
            }

            composable(Screen.History.route) {
                val historyVm: HistoryViewModel = getViewModel { app -> HistoryViewModel(app.repository) }
                AttendanceHistoryScreen(viewModel = historyVm)
            }

            composable(Screen.Settings.route) {
                val settingsVm: SettingsViewModel = getViewModel { app -> SettingsViewModel(app.repository, app.userPreferencesRepository) }
                SettingsScreen(viewModel = settingsVm)
            }
        }
    }
}
