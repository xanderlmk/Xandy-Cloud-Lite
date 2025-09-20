package com.xandy.lite.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kyant.taglib.TagProperty
import com.xandy.lite.controllers.shareMultipleAudios
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.ShowModalFor
import com.xandy.lite.models.ui.UpdateResult
import com.xandy.lite.ui.functions.AddIconButton
import com.xandy.lite.ui.functions.AddPlDialog
import com.xandy.lite.ui.functions.ChangePlNameDialog
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.LocalAudioOptions
import com.xandy.lite.ui.functions.PlayListOptions
import com.xandy.lite.ui.functions.SearchTextField
import com.xandy.lite.ui.functions.UpdateAudioListMetadata
import com.xandy.lite.ui.theme.GetUIStyle
import com.xandy.lite.views.player.controller.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomNavigationContent(val getUIStyle: GetUIStyle) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomNavigationTabBars(
        mainNavController: NavHostController,
        navVM: NavViewModel, getController: () -> Unit,
        content: @Composable () -> Unit
    ) {
        val coroutineScope = rememberCoroutineScope()
        val mediaController by navVM.mediaController.collectAsState()
        val route by navVM.route.collectAsStateWithLifecycle()
        val isSelecting by navVM.isSelecting.collectAsStateWithLifecycle()
        val isSearching by navVM.isSearching.collectAsStateWithLifecycle()
        val query by navVM.query.collectAsStateWithLifecycle()
        val querySet by navVM.querySet.collectAsStateWithLifecycle()
        val ci = ContentIcons(getUIStyle)
        val onNavigate: (String) -> Unit = { r ->
            if (route != r) {
                if (isSelecting && r != AddToPlDestination.route) navVM.endSelect()
                if (isSearching) navVM.resetSearch()
                mainNavController.navigate(r)
                navVM.updateRoute(r)
            }
        }
        val localTab by navVM.localTab.collectAsStateWithLifecycle()
        var dismissEnabled by rememberSaveable { mutableStateOf(true) }
        var showAddPlDialog by rememberSaveable { mutableStateOf(false) }
        var showRenamePlDialog by rememberSaveable { mutableStateOf(false) }
        var showMetadataDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
        val plWithAudio by navVM.plWithAudio.collectAsStateWithLifecycle()
        val selectedFolders by navVM.selectedFolders.collectAsStateWithLifecycle()
        val audioStates by navVM.audioStates.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var showImageModal by rememberSaveable { mutableStateOf<ShowModalFor>(ShowModalFor.Idle) }
        val localArtwork by navVM.artworkList.collectAsStateWithLifecycle()
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        var metadataString by rememberSaveable { mutableStateOf("") }
        val onUpdateMetadata: () -> Unit = {
            showMetadataDialog = Pair(false, ""); dismissEnabled = true; navVM.endSelect()
            metadataString = ""
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        val name = plWithAudio?.playlist?.name
                        if (route == LocalMusicDestination.route && isSearching) {
                            SearchTextField(
                                query = query, onValueChange = { navVM.updateQuery(it) },
                                onUpdateQuerySet = { navVM.updateRecentQuery(it) },
                                querySet = querySet, onTurnOff = { navVM.turnOffSearch() }, ci = ci
                            )
                        } else if (route == LocalPlDestination.route && name != null &&
                            !isSearching
                        ) {
                            Text(
                                text = name, textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp),
                            )
                        } else if (route == LocalPlDestination.route && isSearching) {
                            SearchTextField(
                                query = query, onValueChange = { navVM.updateQuery(it) },
                                onUpdateQuerySet = { navVM.updateRecentQuery(it) },
                                querySet = querySet, onTurnOff = { navVM.turnOffSearch() }, ci = ci
                            )
                        }
                    },
                    colors = TopAppBarColors(
                        containerColor = getUIStyle.topBarColor(),
                        navigationIconContentColor = getUIStyle.themedColor(),
                        titleContentColor = getUIStyle.themedColor(),
                        actionIconContentColor = getUIStyle.themedColor(),
                        scrolledContainerColor = getUIStyle.themedColor()
                    ),
                    actions = {
                        when (route) {
                            LocalMusicDestination.route -> {
                                LocalAudioOptions(
                                    audioStates = audioStates, getUIStyle = getUIStyle,
                                    onRefresh = {
                                        coroutineScope.launch { navVM.updateAudioFiles() }
                                    },
                                    onSearch = { navVM.turnOnSearch() },
                                    onUpdateSongOrder = { navVM.updateLocalALOrder(it) },
                                    onReverseSongOrder = { navVM.reverseALOrder() },
                                    onUpdatePlsOrder = { navVM.updateLocalPLOrder(it) },
                                    onReversePlsOrder = { navVM.reverseLocalPlsOrder() },
                                    onHideAudios = {
                                        coroutineScope.launch {
                                            val selectedSongIds = navVM.getSelectedSongIds()
                                            val result = navVM.hideAudios(selectedSongIds)
                                            if (!result) Toast.makeText(
                                                context,
                                                "Failed to hide songs", Toast.LENGTH_SHORT
                                            ).show()
                                            else navVM.endSelect()
                                        }
                                    },
                                    onShowAudios = {
                                        coroutineScope.launch {
                                            val selectedSongIds = navVM.getSelectedSongIds()
                                            val result = navVM.showAudios(selectedSongIds)
                                            if (!result) Toast.makeText(
                                                context,
                                                "Failed to unhide songs", Toast.LENGTH_SHORT
                                            ).show()
                                            else navVM.endSelect()
                                        }
                                    },
                                    onHideFolders = {
                                        coroutineScope.launch {
                                            val result = navVM.hideFolders(selectedFolders)
                                            if (!result) Toast.makeText(
                                                context,
                                                "Failed to hide folders", Toast.LENGTH_SHORT
                                            ).show()
                                            else navVM.endSelect()
                                        }
                                    },
                                    onAddSongs = { onNavigate(AddToPlDestination.route) },
                                    onShareSongs = {
                                        val selectedSongIds = navVM.getSelectedSongIds()
                                        shareMultipleAudios(context, selectedSongIds)
                                    },
                                    onToggleAutoUpdate = {
                                        navVM.toggleAutoUpdate(!audioStates.autoUpdate)
                                    },
                                    onUpdateMetadata = { showMetadataDialog = Pair(true, it) }
                                )
                            }

                            LocalPlDestination.route -> {
                                if (!isSearching) PlayListOptions(
                                    navVM, getUIStyle,
                                    onChangeArt = { showImageModal = ShowModalFor.LocalPl },
                                    onSearch = { navVM.turnOnSearch() },
                                    onChangeName = { showRenamePlDialog = true }
                                )
                            }

                        }
                    },
                    navigationIcon = {
                        if (route == LocalMusicDestination.route && !isSearching) {
                            if (localTab == LocalMusicTabs.PLAYLIST) {
                                AddIconButton(ci) { showAddPlDialog = true }
                            } else if (localTab == LocalMusicTabs.LIBRARY && isSelecting) {
                                IconButton(onClick = { navVM.endSelect() }) {
                                    ci.ContentIcon(Icons.Default.Close)
                                }
                            }
                        } else if (route == PickedSongDestination.route) {
                            IconButton(onClick = {
                                val route =
                                    mainNavController.previousBackStackEntry?.destination?.route
                                route?.let { navVM.updateRoute(it) }
                                navVM.stopCheckingPosition()
                                mainNavController.popBackStack()
                            }) {
                                ci.ContentIcon(Icons.Default.KeyboardArrowDown)
                            }
                        } else if (
                            route == LocalPlDestination.route && !isSearching && !isSelecting
                        ) {
                            IconButton(onClick = {
                                val route =
                                    mainNavController.previousBackStackEntry?.destination?.route
                                if (route != null) {
                                    navVM.updateRoute(route); mainNavController.popBackStack()
                                } else {
                                    val new = LocalMusicDestination.route
                                    navVM.updateRoute(new); mainNavController.navigate(new)
                                }
                            }) {
                                ci.ContentIcon(Icons.Default.KeyboardArrowDown)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                AddPlDialog(
                    showDialog = showAddPlDialog,
                    onDismiss = { if (dismissEnabled) showAddPlDialog = false },
                    onSubmit = { name ->
                        coroutineScope.launch {
                            dismissEnabled = false
                            val result = navVM.insertLocalPl(name)
                            when (result) {
                                InsertResult.Exists -> Toast.makeText(
                                    context, "Name already exists", Toast.LENGTH_SHORT
                                ).show()

                                InsertResult.Failure -> Toast.makeText(
                                    context, "Failed to add playlist", Toast.LENGTH_SHORT
                                ).show()

                                InsertResult.Success -> showAddPlDialog = false
                            }
                            dismissEnabled = true
                        }
                    },
                    enabled = dismissEnabled, getUIStyle = getUIStyle
                )
                ChangePlNameDialog(
                    showDialog = showRenamePlDialog, originalName = plWithAudio?.playlist?.name,
                    onDismiss = { if (dismissEnabled) showRenamePlDialog = false },
                    onSubmit = { newName ->
                        coroutineScope.launch {
                            val name = plWithAudio?.playlist?.name
                            if (name == null) {
                                Toast.makeText(
                                    context, "Undefined playlist name", Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            dismissEnabled = false
                            val result = navVM.changePlName(newName, name)
                            when (result) {
                                InsertResult.Exists -> Toast.makeText(
                                    context, "Name already exists", Toast.LENGTH_SHORT
                                ).show()

                                InsertResult.Failure -> Toast.makeText(
                                    context, "Failed to add playlist", Toast.LENGTH_SHORT
                                ).show()

                                InsertResult.Success -> showRenamePlDialog = false
                            }
                            dismissEnabled = true
                        }
                    },
                    enabled = dismissEnabled, getUIStyle = getUIStyle,
                )
                ImagePicker(
                    artworkList = localArtwork,
                    isLandscape = isLandscape,
                    onImagePicked = { img ->
                        coroutineScope.launch {
                            if (!dismissEnabled) return@launch
                            dismissEnabled = false
                            when (showImageModal) {
                                ShowModalFor.Idle -> {
                                    Toast.makeText(
                                        context, "Null", Toast.LENGTH_SHORT
                                    ).show()
                                }

                                ShowModalFor.LocalPl -> {
                                    plWithAudio?.let {
                                        navVM.updateLocalPlArtwork(it.playlist, img)
                                    }
                                }

                                ShowModalFor.RemotePl -> {
                                    Toast.makeText(
                                        context, "Todo()", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            showImageModal = ShowModalFor.Idle
                            dismissEnabled = true

                        }
                    },
                    showModal = showImageModal, getUIStyle = getUIStyle,
                    onDismiss = { showImageModal = ShowModalFor.Idle }
                )
                WithRequestLauncher(
                    onResultOk = {
                        coroutineScope.launch {
                            val selectedSongIds = navVM.getSelectedSongIds()
                            val string = metadataString.ifBlank {
                                Toast.makeText(
                                    context, "Null string", Toast.LENGTH_SHORT
                                ).show()
                                onUpdateMetadata()
                                return@launch
                            }
                            val result = updateTags(
                                showMetadataDialog.second, selectedSongIds, navVM, context, string
                            )
                            if (result is UpdateResult.Failure) Toast.makeText(
                                context, "Failed to update selected tag", Toast.LENGTH_SHORT
                            ).show()

                            onUpdateMetadata()
                        }
                    }
                ) { writeRequestLauncher ->
                    UpdateAudioListMetadata(
                        showMetadataDialog.first, getUIStyle = getUIStyle, enabled = dismissEnabled,
                        onDismiss = { showMetadataDialog = Pair(false, "") },
                        onSubmit = {
                            coroutineScope.launch {
                                dismissEnabled = false
                                metadataString = it
                                val selectedSongIds = navVM.getSelectedSongIds()
                                val result = updateTags(
                                    showMetadataDialog.second, selectedSongIds, navVM, context, it
                                )
                                when (val r = result) {
                                    is UpdateResult.FileException ->
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            requestWritePermission(writeRequestLauncher, r.request)
                                        }

                                    UpdateResult.Success -> onUpdateMetadata()
                                    else -> {
                                        Toast.makeText(
                                            context, "Something went wrong", Toast.LENGTH_SHORT
                                        ).show()
                                        onUpdateMetadata()
                                    }
                                }
                            }
                        },
                        metadataToUpdate = showMetadataDialog.second
                    )
                }
                content()
                val mc = mediaController
                if (mc != null) {
                    if (route != PickedSongDestination.route &&
                        route != EditAudioDestination.route && route != AddToPlDestination.route
                    ) {
                        PlayerController(
                            mc, navVM, getUIStyle = getUIStyle,
                            onClickSong = { onNavigate(PickedSongDestination.route) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .height(60.dp)
                        )
                    }
                } else {
                    var show by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(1_500L); show = true
                    }
                    if (show) Text(
                        text = "Null Controller, click here to reset.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(60.dp)
                            .clickable { getController() }
                    )
                }
            }
        }
    }
}

private fun requestWritePermission(
    writeRequestLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    intent: PendingIntent
) {
    writeRequestLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
}

private suspend fun updateTags(
    string: String, selectedSongIds: List<String>, navVM: NavViewModel, context: Context, it: String
): UpdateResult {
    return when (string) {
        TagProperty.ARTIST -> navVM.updateArtistsOfAL(selectedSongIds, it)

        TagProperty.ALBUM -> navVM.updateAlbumOfAL(selectedSongIds, it)

        TagProperty.GENRE -> navVM.updateGenreOfAL(selectedSongIds, it)

        else -> {
            Toast.makeText(context, "Unknown property", Toast.LENGTH_SHORT).show()
            UpdateResult.Failure
        }
    }
}