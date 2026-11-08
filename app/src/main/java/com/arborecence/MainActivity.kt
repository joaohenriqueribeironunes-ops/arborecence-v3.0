package com.arborecence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.arborecence.core.domain.repository.SettingsRepository
import com.arborecence.core.ui.components.CalmGradientBackground
import com.arborecence.core.ui.components.GlassPill
import com.arborecence.core.ui.theme.ArborecenceTheme
import com.arborecence.core.ui.theme.LocalGlassPrefs
import com.arborecence.feature.assistant.AssistantRoute
import com.arborecence.feature.focus.FocusRoute
import com.arborecence.feature.home.HomeRoute
import com.arborecence.feature.learning.LearningRoute
import com.arborecence.feature.notes.NotesRoute
import com.arborecence.feature.onboarding.OnboardingRoute
import com.arborecence.feature.review.ReviewRoute
import com.arborecence.feature.settings.SettingsRoute
import com.arborecence.feature.voicedump.VoiceDumpRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startRoute = intent?.getStringExtra("route")
        setContent {
            val acc by settings.accessibility().collectAsState(initial = com.arborecence.core.domain.model.AccessibilitySettings())
            ArborecenceTheme(reduceMotion = acc.reduceMotion, reduceTransparency = acc.reduceTransparency, fontScale = acc.fontScale) {
                val nav = rememberNavController()
                val dark = isSystemInDarkTheme()
                LaunchedEffect(startRoute) {
                    when (startRoute) {
                        "assistant" -> nav.navigate("assistant") { launchSingleTop = true }
                        "dump" -> nav.navigate("dump") { launchSingleTop = true }
                        "learning" -> nav.navigate("learning") { launchSingleTop = true }
                    }
                }
                CalmGradientBackground(dark = dark, modifier = Modifier.fillMaxSize()) {
                    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, bottomBar = { BottomPill(nav) }) { pad ->
                        NavHost(nav, startDestination = "onboarding", modifier = Modifier.padding(pad)) {
                            composable("onboarding") { OnboardingRoute(onDone = { nav.navigate("home") { popUpTo("onboarding") { inclusive = true } } }) }
                            composable("home") { HomeRoute(onDump = { nav.navigate("dump") }, onAssistant = { nav.navigate("assistant") }, onFocus = { nav.navigate("focus") }, onNotes = { nav.navigate("notes") }, onLearning = { nav.navigate("learning") }) }
                            composable("dump") { VoiceDumpRoute(onBack = { nav.popBackStack() }) }
                            composable("notes") { NotesRoute(onReview = { nav.navigate("review") }, onBack = { nav.popBackStack() }) }
                            composable("review") { ReviewRoute(onBack = { nav.popBackStack() }) }
                            composable("focus") { FocusRoute(onBack = { nav.popBackStack() }) }
                            composable("learning") { LearningRoute(onStart = { com.arborecence.service.LearningForegroundService.start(this@MainActivity) }, onStop = { startService(android.content.Intent(this@MainActivity, com.arborecence.service.LearningForegroundService::class.java).setAction(com.arborecence.service.LearningForegroundService.ACTION_STOP)) }) }
                            composable("assistant") { AssistantRoute(onBack = { nav.popBackStack() }, startVoice = intent?.getBooleanExtra("start_voice", false) == true) }
                            composable("settings") { SettingsRoute(onBack = { nav.popBackStack() }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomPill(nav: androidx.navigation.NavController) {
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    if (route == "onboarding") return
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        GlassPill(Modifier.height(64.dp)) {
            NavigationBar(containerColor = androidx.compose.ui.graphics.Color.Transparent, tonalElevation = 0.dp) {
                val items = listOf("home" to Icons.Outlined.Home, "notes" to Icons.Outlined.AccountTree, "assistant" to Icons.Outlined.GraphicEq, "settings" to Icons.Outlined.Settings)
                items.forEach { (r, icon) ->
                    NavigationBarItem(selected = route == r, onClick = { nav.navigate(r) { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true } },
                        icon = { Icon(icon, r) }, label = null, alwaysShowLabel = false)
                }
            }
        }
    }
}
