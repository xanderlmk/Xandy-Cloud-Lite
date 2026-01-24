package com.xandy.lite.navigation

import android.app.Activity
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.AddToLocalPlVM
import com.xandy.lite.controllers.view.models.EditAudioVM
import com.xandy.lite.controllers.view.models.LocalAlbumVM
import com.xandy.lite.controllers.view.models.LocalArtistVM
import com.xandy.lite.controllers.view.models.LocalFolderVM
import com.xandy.lite.controllers.view.models.LocalGenreVM
import com.xandy.lite.controllers.view.models.LocalMediaVM
import com.xandy.lite.controllers.view.models.LocalPLVM
import com.xandy.lite.controllers.view.models.LyricsEditorVM
import com.xandy.lite.controllers.view.models.LyricsVM
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.models.XCToast
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
fun NavHosts.MainNavHost(
    mainNavController: NavHostController, getUIStyle: GetUIStyle, pm: PreferencesManager,
    getController: () -> Unit, navVM: NavViewModel
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

    val onNavigate: (String, String?) -> Unit = { r, extras ->
        mainNavController.navigate(extras ?: r); navVM.updateRoute(r)
    }

    /** First string is route and Second string is route to remove */
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
    val toast = XCToast(context)

    val requestEvents by navVM.requestEvents.collectAsStateWithLifecycle()
    val updateUUIDsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                val result = navVM.insertSongIdToMetadata()
                if (!result)
                    toast.makeMessage(toast.updateFailed)
            }
        } else toast.makeMessage(toast.permissionDenied)
    }
    key(requestEvents) {
        requestEvents?.let { updateUUIDsLauncher.launch(it.first) }
    }
    val enterTransition = Transitions.enterTransition
    val exitTransition = Transitions.exitTransition
    val swipeInTransition = Transitions.swipeInTransition
    val swipeOutTransition = Transitions.swipeOutTransition

    content.CustomNavigationTabBars(mainNavController, getController) {
        NavHost(
            navController = mainNavController,
            startDestination = LocalMusicDestination.route
        ) {
            composable(
                route = PickedSongDestination.route,
                enterTransition = enterTransition, exitTransition = exitTransition
            ) {
                val songVM: PickedSongVM = viewModel(factory = AppVMProvider.Factory)
                var songToggle by rememberSaveable { mutableStateOf<SongToggle>(SongToggle.Details) }
                val isLandscape =
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

                var landscapeState by rememberSaveable { mutableStateOf(isLandscape) }
                LaunchedEffect(Unit) {
                    if (landscapeState != isLandscape) {
                        songVM.checkPosition()
                        landscapeState = isLandscape
                    }
                }
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
                                enabled, toast, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, audioPair = audioPair, toast = toast,
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
                        modifier = modifier, getUIStyle = getUIStyle, vm = localMediaVM,
                        onNavToPl = {
                            navVM.updatePlUUID(it)
                            onNavigate(LocalPlDestination.route, null)
                        },
                        onNavToAlbum = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateAlbumName(it)
                            onNavigate(LocalAlbumDestination.route, null)
                        },
                        onNavToArtist = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateArtistName(it)
                            onNavigate(LocalArtistDestination.route, null)
                        },
                        onNavToGenre = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateGenreName(it)
                            onNavigate(LocalGenreDestination.route, null)
                        },
                        onNavToFolder = { s, l ->
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateBucketKey(s, l)
                            onNavigate(LocalBucketDestination.route, null)
                        },
                        onEdit = {
                            if (isSearching) navVM.turnOffSearch()
                            navVM.updateAudioUri(it)
                            onNavigate(EditAudioDestination.route, null)
                        },
                        onAdd = { id ->
                            if (isSearching) navVM.turnOffSearch()
                            navVM.toggleSong(id)
                            onNavigate(AddToPlDestination.route, AddToPlDestination.createRoute())
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
                            onNavigate(AddToPlDestination.route, AddToPlDestination.createRoute())
                        },
                        onEditSong = { songId ->
                            navVM.updateAudioUri(songId)
                            onNavigate(EditAudioDestination.route, null)
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
                                toast.makeMessage(toast.nullTrack)
                                return@launch
                            }
                            val result = editAudioVM.updateAudioTags(audio, lyrics)
                            if (result is UpdateResult.Failure)
                                toast.makeMessage(toast.failedToUpdateTags)
                            else onPopBackStack()
                        }
                    }
                ) { writeRequestLauncher ->
                    pickedAudio?.let {
                        EditAudioView(it, enabled, getUIStyle, artworkList) { newAudio, lyrics ->
                            coroutineScope.launch {
                                updatedAudioWithLyrics = Pair(newAudio, lyrics)
                                if (newAudio.title.isBlank()) {
                                    toast.makeMessage(toast.titleCantBeBlank)
                                    return@launch
                                }
                                enabled = false
                                val result = editAudioVM.updateAudioTags(newAudio, lyrics)
                                when (val r = result) {
                                    is UpdateResult.Failure ->
                                        toast.makeMessage(toast.failedToUpdateTags)

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
            composable(
                route = AddToPlDestination.route,
                enterTransition = swipeInTransition,
                exitTransition = swipeOutTransition,
                arguments = listOf(
                    navArgument("showAdd") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val showAdd = backStackEntry.arguments?.getBoolean("showAdd") ?: false
                BackHandler { navVM.resetSearch(); onPopBackStack() }
                val vm: AddToLocalPlVM = viewModel(factory = AppVMProvider.Factory)
                var dismissEnabled by rememberSaveable { mutableStateOf(true) }
                val showDialog = rememberSaveable { mutableStateOf(false) }
                val addedToQueue = stringResource(R.string.added_tracks_to_queue)
                val tracksAlreadyInQueue =
                    stringResource(R.string.tracks_already_in_queue)
                val tracksAlreadyInPlayNext = stringResource(R.string.tracks_already_in_play_next)
                val tracksAddedToPlayNext = stringResource(R.string.tracks_added_to_play_next)
                val nullControllerMaybe =
                    stringResource(R.string.failed_to_add_tracks_null_controller)
                AddToPlaylistView(
                    getUIStyle = getUIStyle, modifier = modifier, vm = vm, enabled = dismissEnabled,
                    onAddToPl = {
                        dismissEnabled = false
                        coroutineScope.launch {
                            val result = vm.insertSongsToPl(navVM.getSelectedSongIds(), it)
                            if (result) {
                                navVM.endSelect(); navVM.resetSearch()
                                navVM.navToPlaylist(it)
                                onNavigateAndRemove(
                                    LocalPlDestination.route,
                                    AddToPlDestination.createRoute(showAdd)
                                )
                            } else toast.makeMessage(toast.failedToAddTracks)
                            dismissEnabled = true
                        }
                    }, showDialog = showDialog, showAdd = showAdd,
                    onAddNew = {
                        dismissEnabled = false
                        coroutineScope.launch {
                            val (result, id) = vm.addPlWithSongs(navVM.getSelectedSongIds(), it)
                            when (result) {
                                InsertResult.Exists -> toast.makeMessage(toast.nameAlreadyExists)

                                InsertResult.Failure -> toast.makeMessage(toast.failedToAddPl)

                                InsertResult.Success -> {
                                    navVM.endSelect(); navVM.resetSearch()
                                    showDialog.value = false
                                    navVM.navToPlaylist(id)
                                    onNavigateAndRemove(
                                        LocalPlDestination.route,
                                        AddToPlDestination.createRoute(showAdd)
                                    )
                                }
                            }
                            dismissEnabled = true
                        }
                    },
                    onAddToPlayNext = {
                        val result = vm.playNext()
                        when (result) {
                            InsertResult.Exists -> toast.makeMessage(tracksAlreadyInPlayNext)
                            InsertResult.Failure -> toast.makeMessage(nullControllerMaybe)
                            InsertResult.Success -> toast.makeMessage(tracksAddedToPlayNext)
                        }
                        val previousRoute =
                            mainNavController.previousBackStackEntry?.destination?.route
                                ?: LocalMusicDestination.route
                        onNavigateAndRemove(previousRoute, AddToPlDestination.createRoute(showAdd))
                        if (isSelecting) navVM.endSelect()
                        if (isSearching) navVM.resetSearch()
                    },
                    onEnqueue = {
                        val result = vm.addToQueue()
                        if (!result) toast.makeMessage(addedToQueue)
                        else toast.makeMessage(tracksAlreadyInQueue)
                        val previousRoute =
                            mainNavController.previousBackStackEntry?.destination?.route
                                ?: LocalMusicDestination.route
                        onNavigateAndRemove(previousRoute, AddToPlDestination.createRoute(showAdd))
                        if (isSelecting) navVM.endSelect()
                        if (isSearching) navVM.resetSearch()
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
                                enabled, toast, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, audioPair = audioPair, toast = toast,
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
                                onNavigate(EditAudioDestination.route, null)
                            },
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(
                                    AddToPlDestination.route, AddToPlDestination.createRoute()
                                )
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
                                enabled, toast,
                                onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, audioPair = audioPair, toast = toast,
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
                                onNavigate(EditAudioDestination.route, null)
                            }, modifier = modifier,
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(
                                    AddToPlDestination.route, AddToPlDestination.createRoute()
                                )
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
                                enabled, toast, onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, audioPair = audioPair, toast = toast,
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
                                onNavigate(EditAudioDestination.route, null)
                            }, modifier = modifier,
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(
                                    AddToPlDestination.route, AddToPlDestination.createRoute()
                                )
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
                                enabled, toast,
                                onToggle = { enabled = it }, audioPair, navVM
                            )
                        }
                    }, onDismissRequest = { showModal = false },
                    onDelete = {
                        coroutineScope.launch {
                            onTryDelete(
                                enabled, audioPair = audioPair, toast = toast,
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
                                onNavigate(EditAudioDestination.route, null)
                            }, modifier = modifier,
                            onAdd = { id ->
                                navVM.toggleSong(id)
                                onNavigate(
                                    AddToPlDestination.route, AddToPlDestination.createRoute()
                                )
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
                    onRestartPlayer = onRestartPlayer, onRecreate = onRecreate, getUIStyle, pm
                )
            }
            composable(
                route = LyricsListDestination.route,
                enterTransition = swipeInTransition, exitTransition = swipeOutTransition
            ) {
                val lyricsVM: LyricsVM = viewModel(factory = AppVMProvider.Factory)

                BackHandler { onPopBackToHome() }
                LyricsListView(
                    lyricsVM = lyricsVM,
                    onEdit = {
                        navVM.updatePickedLyrics(it)
                        onNavigate(LyricsEditorDestination.route, null)
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

private suspend fun onDelete(
    enabled: Boolean, toast: XCToast,
    onToggle: (Boolean) -> Unit, audioPair: Pair<String?, String>, navVM: NavViewModel
) {
    if (!enabled) return
    onToggle(false)
    val pair = audioPair.takeIf { pair -> !pair.first.isNullOrBlank() }
    if (pair == null) {
        toast.makeMessage(toast.nullTrack)
        return
    }
    val result = navVM.deleteAudios(listOf(pair.first.toString()))
    if (result !is DeleteResult.Success)
        toast.makeMessage(toast.failedToDeleteTrack)
    else toast.makeMessage(toast.deletedTrack(pair.second))
    onToggle(true)
}

private suspend fun onTryDelete(
    enabled: Boolean, onToggle: (Boolean) -> Unit, toast: XCToast,
    audioPair: Pair<String?, String>, navVM: NavViewModel, onDismissModal: () -> Unit,
    writeRequestLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
) {
    if (!enabled) return
    onToggle(false)
    val pair =
        audioPair.takeIf { pair -> !pair.first.isNullOrBlank() }
    if (pair == null) {
        toast.makeMessage(toast.nullTrack)
        return
    }
    val result =
        navVM.deleteAudios(listOf(pair.first.toString()))
    when (val r = result) {
        is DeleteResult.FileException ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                requestWritePermission(writeRequestLauncher, r.request)

        is DeleteResult.Success -> toast.makeMessage(toast.deletedTrack(pair.second))

        else -> toast.makeMessage(toast.failedToDeleteTrack)
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
