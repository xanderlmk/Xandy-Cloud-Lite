package com.xandy.lite.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xandy.lite.controllers.view.models.LyricsEditorVM
import com.xandy.lite.controllers.view.models.LyricsVM
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.application.PreferencesManager
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.Transitions
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.LyricDialog
import com.xandy.lite.views.lyrics.LyricIndex
import com.xandy.lite.views.lyrics.LyricsEditorView
import com.xandy.lite.views.lyrics.LyricsListView
import com.xandy.lite.views.settings.SettingsView
import kotlinx.coroutines.launch

class NavHosts(val onRestartPlayer: () -> Unit, val onRecreate: () -> Unit) {
    @Composable
    fun XclfNavHost(
        navController: NavHostController, getUIStyle: GetUIStyle, pm: PreferencesManager,
        navVM: NavViewModel, lyrics: Lyrics?, onPopBackToHome: () -> Unit
    ) {
        val content = CustomNavigationContent(getUIStyle, navVM)
        val onNavigate: (String) -> Unit = {
            navController.navigate(it); navVM.updateRoute(it)
        }
        val onPopBackStack: () -> Unit = {
            val route = navController.previousBackStackEntry?.destination?.route
            route?.let { navVM.updateRoute(it) }
            navController.popBackStack()
        }
        val swipeInTransition = Transitions.swipeInTransition
        val swipeOutTransition = Transitions.swipeOutTransition
        val toast = XCToast(LocalContext.current)
        val coroutineScope = rememberCoroutineScope()
        content.CustomNavigationTabBars(onReturnHome = onPopBackToHome, navController) {
            NavHost(
                navController = navController, startDestination = LyricsListDestination.route
            ) {
                composable(SettingsDestination.route) {
                    BackHandler { onPopBackToHome() }
                    SettingsView(
                        controller = navVM.mediaController.collectAsStateWithLifecycle().value,
                        onRestartPlayer = onRestartPlayer, onRecreate = onRecreate, getUIStyle, pm
                    )
                }
                composable(
                    route = LyricsListDestination.route,
                    enterTransition = swipeInTransition, exitTransition = swipeOutTransition
                ) {
                    val lyricsVM: LyricsVM = viewModel(factory = AppVMProvider.Factory)
                    var show by rememberSaveable { mutableStateOf(true) }
                    var enabled by rememberSaveable { mutableStateOf(true) }
                    var replace by rememberSaveable { mutableStateOf(false) }
                    val onDone: () -> Unit = {
                        show = false; enabled = true; replace = false
                    }
                    LyricDialog(
                        show = show, getUIStyle = getUIStyle, enabled = enabled, l = lyrics,
                        replace = replace, onDismiss = onDone,
                        onImport = {
                            coroutineScope.launch {
                                val l = lyrics
                                if (l == null) {
                                    toast.makeMessage(toast.nullLyrics)
                                    return@launch
                                }
                                enabled = false
                                val result = lyricsVM.importLyrics(l)
                                when (result) {
                                    InsertResult.Exists -> replace = true
                                    InsertResult.Failure -> toast.makeMessage("Import Failed.")
                                    InsertResult.Success -> {
                                        toast.makeMessage("Imported Successfully!")
                                        show = false
                                    }
                                }
                                enabled = true
                            }
                        },
                        onReplace = {
                            coroutineScope.launch {
                                val l = lyrics
                                if (l == null) {
                                    toast.makeMessage(toast.nullLyrics)
                                    return@launch
                                }
                                val result = lyricsVM.updateLyrics(l)
                                if (!result) toast.makeMessage("Unable to overwrite lyrics")
                                else toast.makeMessage("Successfully overwrote lyrics")
                                onDone()
                            }
                        },
                    )
                    BackHandler { onPopBackToHome() }
                    LyricsListView(
                        lyricsVM = lyricsVM,
                        onEdit = {
                            navVM.updatePickedLyrics(it)
                            onNavigate(LyricsEditorDestination.route)
                        },
                        getUIStyle = getUIStyle
                    )
                }
                composable(
                    route = LyricsEditorDestination.route,
                    enterTransition = swipeInTransition, exitTransition = swipeOutTransition
                ) {
                    val lyricsEditorVM: LyricsEditorVM = viewModel(factory = AppVMProvider.Factory)
                    BackHandler {
                        navVM.updateIndexListener(LyricIndex.UNAVAILABLE)
                        navVM.stopCheckingPosition()
                        onPopBackStack()
                    }
                    LyricsEditorView(lyricsEditorVM, onUpsert = onPopBackStack, getUIStyle)
                }
            }
        }
    }
}