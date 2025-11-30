package com.xandy.lite.navigation

import android.app.Activity
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xandy.lite.controllers.view.models.AddToLocalPlVM
import com.xandy.lite.controllers.view.models.EditAudioVM
import com.xandy.lite.controllers.view.models.LocalAlbumVM
import com.xandy.lite.controllers.view.models.LocalArtistVM
import com.xandy.lite.controllers.view.models.LocalFolderVM
import com.xandy.lite.controllers.view.models.LocalGenreVM
import com.xandy.lite.controllers.view.models.LocalMediaVM
import com.xandy.lite.controllers.view.models.LocalPLVM
import com.xandy.lite.controllers.view.models.LyricsEditorVM
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.application.PreferencesManager
import com.xandy.lite.models.ui.DeleteResult
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.SongToggle
import com.xandy.lite.models.ui.Transitions
import com.xandy.lite.models.ui.UpdateResult
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.views.AddToPlaylistView
import com.xandy.lite.views.LocalAlbumView
import com.xandy.lite.views.LocalArtistView
import com.xandy.lite.views.LocalFolderView
import com.xandy.lite.views.LocalGenreView
import com.xandy.lite.views.LocalMusicView
import com.xandy.lite.views.LocalPlaylistView
import com.xandy.lite.views.lyrics.LyricsListView
import com.xandy.lite.views.edit.audio.EditAudioView
import com.xandy.lite.views.lyrics.LyricIndex
import com.xandy.lite.views.lyrics.LyricsEditorView
import com.xandy.lite.views.picked.song.SongView
import com.xandy.lite.views.settings.SettingsView
import kotlinx.coroutines.launch


@Composable
fun MainNavHost(
    mainNavController: NavHostController, getUIStyle: GetUIStyle, pm : PreferencesManager,
    getController: () -> Unit, onRestartPlayer: () -> Unit, navVM: NavViewModel
) {
    val content = CustomNavigationContent(getUIStyle, navVM)
    val context = LocalContext.current
    val sd by navVM.songDetails.collectAsStateWithLifecycle()
    val modifier =
        if (sd != null) Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp)
        else Modifier.fillMaxSize()
    val isSelecting by navVM.isSelecting.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val isSearching by navVM.isSearching.collectAsStateWithLifecycle()
    val onPopBackStack: () -> Unit = {
        val route = mainNavController.previousBackStackEntry?.destination?.route
        route?.let { navVM.updateRoute(it) }
        mainNavController.popBackStack()
    }
    val onPopBackToHome: () -> Unit = {
        navVM.updateRoute(LocalMusicDestination.route)
        mainNavController.navigate(LocalMusicDestination.route)
    }

    val onNavigate: (String) -> Unit = {
        mainNavController.navigate(it); navVM.updateRoute(it)
    }
    val onNavigateAndRemove: (String, String) -> Unit = { route, removable ->
        mainNavController.navigateAndRemoveFromStack(route, removable); navVM.updateRoute(route)
    }
    val onSpecialPopBack: () -> Unit = {
        val route = mainNavController.previousBackStackEntry?.destination?.route
        if (route != null) {
            navVM.updateRoute(route); mainNavController.popBackStack()
        } else {
            val new = LocalMusicDestination.route
            navVM.updateRoute(new); mainNavController.navigate(new)
        }
    }
    val requestEvents by navVM.requestEvents.collectAsStateWithLifecycle()
    val updateUUIDsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                val result = navVM.insertSongIdToMetadata()
                if (!result)
                    Toast.makeText(context, "Update failed.", Toast.LENGTH_SHORT).show()
            }
        } else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }
    key(requestEvents) {
        requestEvents?.let { updateUUIDsLauncher.launch(it.first) }
    }
    val enterTransition = Transitions.enterTransition
    val exitTransition = Transitions.exitTransition
    val swipeInTransition = Transitions.swipeInTransition
    val swipeOutTransition = Transitions.swipeOutTransition

    content.CustomNavigationTabBars(mainNavController, getController) {
        NavHost(navController = mainNavController, startDestination = LocalMusicDestination.route) {
            composable(
                route = PickedSongDestination.route,
                enterTransition = enterTransition, exitTransition = exitTransition
            ) {
                val songVM: PickedSongVM = viewModel(factory = AppVMProvider.Factory)
                var songToggle by rememberSaveable { mutableStateOf<SongToggle>(SongToggle.Details) }
                BackHandler {
                    if (songToggle !is SongToggle.Details) {
                        songToggle = SongToggle.Details
                    } else {
                        navVM.stopCheckingPosition()
                        onSpecialPopBack()
                    }
                }
                SongView(songVM, getUIStyle, { songToggle = it }, songToggle)
            }

            composable(LocalMusicDestination.route) {
                BackHandler {
                    if (isSelecting) {
                        if (isSearching) navVM.turnOffSearch()
                        else navVM.endSelect()
                    } else {
                        if (isSearching) navVM.turnOffSearch()
                        else (mainNavController.context as? Activity)?.finish()
                    }
                }
                var enabled by rememberSaveable { mutableStateOf(true) }
                var audioPair by rememberSaveable {
                    mutableStateOf(Pair<String?, String>(null, ""))
                }
                var showModal by rememberSaveable { mutableStateOf(false) }
                val localMediaVM: LocalMediaVM = viewModel(factory = AppVMProvider.Factory)

                WithDeleteModalAndLauncher(
                    onResultOk = {
                        coroutineScope.launch {
                            onDelete(
                                enabled, context, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, context, audioPair = audioPair,
                                onToggle = { t -> enabled = t },
                                onDismissModal = { showModal = false },
                                writeRequestLauncher = it,
                                navVM = navVM
                            )
                        }
                    }, string = audioPair.second.takeIf { str -> str.isNotEmpty() },
                    showModal = showModal
                ) { writeRequestLauncher ->
                    LocalMusicView(
                        modifier = modifier, getUIStyle = getUIStyle, localMediaVM = localMediaVM,
                        onNavToPl = {
                            navVM.updatePlUUID(it)
                            onNavigate(LocalPlDestination.route)
                        },
                        onNavToAlbum = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateAlbumName(it)
                            onNavigate(LocalAlbumDestination.route)
                        },
                        onNavToArtist = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateArtistName(it)
                            onNavigate(LocalArtistDestination.route)
                        },
                        onNavToGenre = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateGenreName(it)
                            onNavigate(LocalGenreDestination.route)
                        },
                        onNavToFolder = { s, l ->
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateBucketKey(s, l)
                            onNavigate(LocalBucketDestination.route)
                        },
                        onEdit = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateAudioUri(it)
                            onNavigate(EditAudioDestination.route)
                        },
                        onAdd = { id ->
                            if (isSearching) navVM.turnOffSearch()
                            navVM.toggleSong(id)
                            onNavigate(AddToPlDestination.route)
                        }, currentId = sd?.id ?: "",
                        onDelete = { audio ->
                            audioPair = Pair(audio.id, audio.title); showModal = true
                        }
                    )
                }
            }
            composable(
                route = LocalPlDestination.route,
                enterTransition = enterTransition, exitTransition = exitTransition
            ) {
                BackHandler {
                    if (isSelecting) {
                        if (isSearching) navVM.turnOffSearch()
                        else navVM.endSelect()
                    } else {
                        if (isSearching) navVM.turnOffSearch() else onSpecialPopBack()

                    }
                }

                val localPLVM: LocalPLVM = viewModel(factory = AppVMProvider.Factory)
                val pickedPlaylist by localPLVM.plWithAudio.collectAsStateWithLifecycle()
                pickedPlaylist?.let {
                    LocalPlaylistView(
                        localPLVM, currentId = sd?.id ?: "", getUIStyle, it,
                        onAdd = { id ->
                            navVM.toggleSong(id)
                            onNavigate(AddToPlDestination.route)
                        },
                        onEditSong = { songId ->
                            navVM.updateAudioUri(songId)
                            onNavigate(EditAudioDestination.route)
                        }
                    )
                } ?: Box(modifier = modifier)
            }
            composable(
                route = EditAudioDestination.route,
                enterTransition = swipeInTransition, exitTransition = swipeOutTransition
            ) {
                BackHandler { onPopBackStack() }
                val editAudioVM: EditAudioVM = viewModel(factory = AppVMProvider.Factory)
                val pickedAudio by editAudioVM.pickedAudio.collectAsStateWithLifecycle()
                val artworkList by editAudioVM.artworkList.collectAsStateWithLifecycle()
                var enabled by rememberSaveable { mutableStateOf(true) }
                var updatedAudioWithLyrics by rememberSaveable {
                    mutableStateOf(Pair(pickedAudio?.song, pickedAudio?.lyrics))
                }
                WithRequestLauncher(
                    onResultOk = {
                        coroutineScope.launch {
                            val audio = updatedAudioWithLyrics.first
                            val lyrics = updatedAudioWithLyrics.second
                            if (audio == null) {
                                Toast.makeText(
                                    context, "Null audio file", Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            val result = editAudioVM.updateAudioTags(audio, lyrics)
                            if (result is UpdateResult.Failure) Toast.makeText(
                                context, "Failed to update tags", Toast.LENGTH_SHORT
                            ).show()
                            else onPopBackStack()
                        }
                    }
                ) { writeRequestLauncher ->
                    pickedAudio?.let {
                        EditAudioView(it, enabled, getUIStyle, artworkList) { newAudio, lyrics ->
                            coroutineScope.launch {
                                updatedAudioWithLyrics = Pair(newAudio, lyrics)
                                enabled = false
                                val result = editAudioVM.updateAudioTags(newAudio, lyrics)
                                when (val r = result) {
                                    is UpdateResult.Failure -> Toast.makeText(
                                        context, "Failed to update tags", Toast.LENGTH_SHORT
                                    ).show()

                                    is UpdateResult.SecurityException ->
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            requestWritePermission(writeRequestLauncher, r.ex)
                                        }

                                    is UpdateResult.FileException -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            requestWritePermission(writeRequestLauncher, r.request)
                                        }
                                    }

                                    else -> onPopBackStack()
                                }
                                enabled = true
                            }
                        }
                    }
                }
            }
            composable(AddToPlDestination.route) {
                BackHandler { navVM.resetSearch(); onPopBackStack() }
                val vm: AddToLocalPlVM = viewModel(factory = AppVMProvider.Factory)
                var dismissEnabled by rememberSaveable { mutableStateOf(true) }
                val showDialog = rememberSaveable { mutableStateOf(false) }
                AddToPlaylistView(
                    getUIStyle = getUIStyle, modifier = modifier, vm = vm, enabled = dismissEnabled,
                    onAdd = {
                        dismissEnabled = false
                        coroutineScope.launch {
                            val result = vm.insertSongsToPl(navVM.getSelectedSongIds(), it)
                            if (result) {
                                navVM.endSelect(); navVM.resetSearch()
                                navVM.navToPlaylist(it)
                                onNavigateAndRemove(
                                    LocalPlDestination.route, AddToPlDestination.route
                                )
                            } else Toast.makeText(
                                context, "Failed to add songs", Toast.LENGTH_SHORT
                            ).show()
                            dismissEnabled = true
                        }
                    }, showDialog = showDialog,
                    onAddNew = {
                        dismissEnabled = false
                        coroutineScope.launch {
                            val (result, id) = vm.addPlWithSongs(navVM.getSelectedSongIds(), it)
                            when (result) {
                                InsertResult.Exists -> Toast.makeText(
                                    context, "Name already exists", Toast.LENGTH_SHORT
                                ).show()

                                InsertResult.Failure -> Toast.makeText(
                                    context, "Failed to add playlist", Toast.LENGTH_SHORT
                                ).show()

                                InsertResult.Success -> {
                                    navVM.endSelect(); navVM.resetSearch()
                                    showDialog.value = false
                                    navVM.navToPlaylist(id)
                                    onNavigateAndRemove(
                                        LocalPlDestination.route, AddToPlDestination.route
                                    )
                                }
                            }
                            dismissEnabled = true
                        }
                    }
                )
            }
            composable(LocalAlbumDestination.route) {
                BackHandler {
                    if (isSelecting) {
                        if (isSearching) navVM.turnOffSearch()
                        else navVM.endSelect()
                    } else {
                        if (isSearching) navVM.turnOffSearch()
                        else onPopBackStack()
                    }
                }
                val localAlbumVM: LocalAlbumVM = viewModel(factory = AppVMProvider.Factory)
                val album by localAlbumVM.album.collectAsStateWithLifecycle()
                val alIsLoading by localAlbumVM.localAudiosLoading.collectAsStateWithLifecycle()
                var enabled by rememberSaveable { mutableStateOf(true) }
                var audioPair by rememberSaveable {
                    mutableStateOf(Pair<String?, String>(null, ""))
                }
                var showModal by rememberSaveable { mutableStateOf(false) }
                WithDeleteModalAndLauncher(
                    onResultOk = {
                        coroutineScope.launch {
                            onDelete(
                                enabled, context, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, context, audioPair = audioPair,
                                onToggle = { t -> enabled = t },
                                onDismissModal = { showModal = false },
                                writeRequestLauncher = it,
                                navVM = navVM
                            )
                        }
                    }, string = audioPair.second.takeIf { str -> str.isNotEmpty() },
                    showModal = showModal
                ) { writeRequestLauncher ->
                    album?.let {
                        LocalAlbumView(
                            it, enabled = !alIsLoading, getUIStyle = getUIStyle, vm = localAlbumVM,
                            onEdit = { str ->
                                navVM.updateAudioUri(str)
                                onNavigate(EditAudioDestination.route)
                            },
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(AddToPlDestination.route)
                            },
                            onDelete = { audio ->
                                audioPair = Pair(audio.id, audio.title)
                                showModal = true
                            }, modifier = modifier, currentId = sd?.id ?: "",
                            onEnabled = { e -> enabled = e }
                        )
                    }
                }
            }
            composable(LocalArtistDestination.route) {
                BackHandler {
                    if (isSelecting) {
                        if (isSearching) navVM.turnOffSearch()
                        else navVM.endSelect()
                    } else {
                        if (isSearching) navVM.turnOffSearch()
                        else onPopBackStack()
                    }
                }
                val localArtistVM: LocalArtistVM = viewModel(factory = AppVMProvider.Factory)
                val artist by localArtistVM.artist.collectAsStateWithLifecycle()
                val alIsLoading by localArtistVM.localAudiosLoading.collectAsStateWithLifecycle()
                var enabled by rememberSaveable { mutableStateOf(true) }
                var audioPair by rememberSaveable {
                    mutableStateOf(Pair<String?, String>(null, ""))
                }
                var showModal by rememberSaveable { mutableStateOf(false) }
                WithDeleteModalAndLauncher(
                    onResultOk = {
                        coroutineScope.launch {
                            onDelete(
                                enabled, context, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, context, audioPair = audioPair,
                                onToggle = { t -> enabled = t },
                                onDismissModal = { showModal = false },
                                writeRequestLauncher = it,
                                navVM = navVM
                            )
                        }
                    }, string = audioPair.second.takeIf { str -> str.isNotEmpty() },
                    showModal = showModal
                ) { writeRequestLauncher ->
                    artist?.let {
                        LocalArtistView(
                            it, enabled = !alIsLoading, getUIStyle = getUIStyle, vm = localArtistVM,
                            onEdit = { str ->
                                navVM.updateAudioUri(str)
                                onNavigate(EditAudioDestination.route)
                            }, modifier = modifier,
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(AddToPlDestination.route)
                            },
                            onDelete = { audio ->
                                audioPair = Pair(audio.id, audio.title)
                                showModal = true
                            }, currentId = sd?.id ?: "",
                            onEnabled = { e -> enabled = e }
                        )
                    }
                }
            }
            composable(LocalBucketDestination.route) {
                BackHandler {
                    if (isSelecting) {
                        if (isSearching) navVM.turnOffSearch()
                        else navVM.endSelect()
                    } else {
                        if (isSearching) navVM.turnOffSearch()
                        else onPopBackStack()
                    }
                }
                var enabled by rememberSaveable { mutableStateOf(true) }
                var audioPair by rememberSaveable {
                    mutableStateOf(Pair<String?, String>(null, ""))
                }
                var showModal by rememberSaveable { mutableStateOf(false) }
                val localBucketVM: LocalFolderVM = viewModel(factory = AppVMProvider.Factory)
                val folder by localBucketVM.folder.collectAsStateWithLifecycle()
                val alIsLoading by localBucketVM.localAudiosLoading.collectAsStateWithLifecycle()

                WithDeleteModalAndLauncher(
                    onResultOk = {
                        coroutineScope.launch {
                            onDelete(
                                enabled, context, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, context, audioPair = audioPair,
                                onToggle = { t -> enabled = t },
                                onDismissModal = { showModal = false },
                                writeRequestLauncher = it,
                                navVM = navVM
                            )
                        }
                    }, string = audioPair.second.takeIf { str -> str.isNotEmpty() },
                    showModal = showModal
                ) { writeRequestLauncher ->
                    folder?.let { f ->
                        LocalFolderView(
                            f, enabled = !alIsLoading, getUIStyle = getUIStyle, vm = localBucketVM,
                            onEdit = { str ->
                                navVM.updateAudioUri(str)
                                onNavigate(EditAudioDestination.route)
                            }, modifier = modifier,
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(AddToPlDestination.route)
                            },
                            onDelete = { audio ->
                                audioPair = Pair(audio.id, audio.title)
                                showModal = true
                            }, currentId = sd?.id ?: "",
                            onEnabled = { e -> enabled = e }

                        )
                    }
                }
            }
            composable(LocalGenreDestination.route) {
                BackHandler {
                    if (isSelecting) {
                        if (isSearching) navVM.turnOffSearch()
                        else navVM.endSelect()
                    } else {
                        if (isSearching) navVM.turnOffSearch()
                        else onPopBackStack()
                    }
                }
                var enabled by rememberSaveable { mutableStateOf(true) }
                var audioPair by rememberSaveable {
                    mutableStateOf(Pair<String?, String>(null, ""))
                }
                var showModal by rememberSaveable { mutableStateOf(false) }
                val localGenreVM: LocalGenreVM = viewModel(factory = AppVMProvider.Factory)
                val alIsLoading by localGenreVM.localAudiosLoading.collectAsStateWithLifecycle()
                val genre by localGenreVM.genre.collectAsStateWithLifecycle()
                WithDeleteModalAndLauncher(
                    onResultOk = {
                        coroutineScope.launch {
                            onDelete(
                                enabled, context, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, context, audioPair = audioPair,
                                onToggle = { t -> enabled = t },
                                onDismissModal = { showModal = false },
                                writeRequestLauncher = it,
                                navVM = navVM
                            )
                        }
                    }, string = audioPair.second.takeIf { str -> str.isNotEmpty() },
                    showModal = showModal
                ) { writeRequestLauncher ->
                    genre?.let { g ->
                        LocalGenreView(
                            g, enabled = !alIsLoading, getUIStyle = getUIStyle, vm = localGenreVM,
                            onEdit = { str ->
                                navVM.updateAudioUri(str)
                                onNavigate(EditAudioDestination.route)
                            }, modifier = modifier,
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(AddToPlDestination.route)
                            },
                            onDelete = { audio ->
                                audioPair = Pair(audio.id, audio.title)
                                showModal = true
                            }, currentId = sd?.id ?: "",
                            onEnabled = { e -> enabled = e }
                        )
                    }
                }
            }
            composable(SettingsDestination.route) {
                BackHandler { onPopBackToHome() }
                SettingsView(
                    controller = navVM.mediaController.collectAsStateWithLifecycle().value,
                    onRestartPlayer = onRestartPlayer, getUIStyle, pm
                )
            }
            composable(LyricsListDestination.route) {
                BackHandler { onPopBackToHome() }
                LyricsListView(
                    onEdit = {
                        navVM.updatePickedLyrics(it)
                        onNavigate(LyricsEditorDestination.route)
                    },
                    getUIStyle = getUIStyle
                )
            }
            composable(LyricsEditorDestination.route) {
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

private suspend fun onDelete(
    enabled: Boolean, context: Context,
    onToggle: (Boolean) -> Unit, audioPair: Pair<String?, String>, navVM: NavViewModel
) {
    if (!enabled) return
    onToggle(false)
    val pair = audioPair.takeIf { pair -> !pair.first.isNullOrBlank() }
    if (pair == null) {
        Toast.makeText(
            context, "Null audio file", Toast.LENGTH_SHORT
        ).show()
        return
    }
    val result = navVM.deleteAudios(listOf(pair.first.toString()))
    if (result !is DeleteResult.Success)
        Toast.makeText(
            context, "Failed to delete song", Toast.LENGTH_SHORT
        ).show()
    else Toast.makeText(
        context, "Deleted ${pair.second}", Toast.LENGTH_SHORT
    ).show()
    onToggle(true)
}

private suspend fun onTryDelete(
    enabled: Boolean, context: Context, onToggle: (Boolean) -> Unit,
    audioPair: Pair<String?, String>, navVM: NavViewModel, onDismissModal: () -> Unit,
    writeRequestLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
) {
    if (!enabled) return
    onToggle(false)
    val pair =
        audioPair.takeIf { pair -> !pair.first.isNullOrBlank() }
    if (pair == null) {
        Toast.makeText(
            context, "Null audio file", Toast.LENGTH_SHORT
        ).show()
        return
    }
    val result =
        navVM.deleteAudios(listOf(pair.first.toString()))
    when (val r = result) {
        is DeleteResult.FileException ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                requestWritePermission(writeRequestLauncher, r.request)

        is DeleteResult.Success -> Toast.makeText(
            context,
            "Deleted ${pair.second}",
            Toast.LENGTH_SHORT
        ).show()

        else -> Toast.makeText(
            context, "Failed to delete song", Toast.LENGTH_SHORT
        ).show()
    }
    onToggle(true); onDismissModal()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun requestWritePermission(
    writeRequestLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    ex: RecoverableSecurityException
) = writeRequestLauncher.launch(
    IntentSenderRequest.Builder(ex.userAction.actionIntent.intentSender).build()
)

private fun requestWritePermission(
    writeRequestLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    intent: PendingIntent
) = writeRequestLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())

/**
 * @param route The route to navigate to
 * @param popUpToRoute The route to be removed*/
private fun NavHostController.navigateAndRemoveFromStack(
    route: String, popUpToRoute: String
) = this.navigate(route) {
    popUpTo(popUpToRoute) { inclusive = true }
    launchSingleTop = true
}
