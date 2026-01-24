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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kyant.taglib.TagProperty
import com.xandy.lite.R
import com.xandy.lite.controllers.shareMultipleAudios
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.DeleteResult
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
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.LibraryRouteOptions
import com.xandy.lite.views.lyrics.LyricIndex
import com.xandy.lite.views.player.controller.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CustomNavigationContent(
    internal val getUIStyle: GetUIStyle,
    internal val navVM: NavViewModel
) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    internal fun CustomNavigationTabBars(
        mainNavController: NavHostController, getController: () -> Unit,
        content: @Composable () -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        val coroutineScope = rememberCoroutineScope()
        val mediaController by navVM.mediaController.collectAsState()
        val route by navVM.route.collectAsStateWithLifecycle()
        val isSelecting by navVM.isSelecting.collectAsStateWithLifecycle()
        val isSearching by navVM.isSearching.collectAsStateWithLifecycle()
        val isAdding by navVM.isAdding.collectAsStateWithLifecycle()

        val query by navVM.query.collectAsStateWithLifecycle()
        val querySet by navVM.querySet.collectAsStateWithLifecycle()
        val ci = ContentIcons(getUIStyle)
        val onNavigate: (String, String?) -> Unit = { r, extras ->
            if (route != r) {
                navVM.updateIndexListener(LyricIndex.UNAVAILABLE)
                if (isSelecting && r != AddToPlDestination.route) navVM.endSelect()
                if (isSearching) navVM.resetSearch()
                mainNavController.navigate(extras ?: r)
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
        val writingEnabled by navVM.writingEnabled.collectAsStateWithLifecycle()
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
        val modalContent = ModalContent(
            mainNavController, getUIStyle, navVM, route,
            onClose = { coroutineScope.launch { drawerState.close() } }
        )
        val toast = XCToast(context)

        val onPressDelete: (Boolean, (Boolean) -> Unit) -> Unit = { enabled, onToggle ->
            coroutineScope.launch {
                onDelete(
                    enabled, context, navVM = navVM,
                    onToggle = onToggle
                )
            }
        }
        val onPressTryDelete: (
            Boolean, (Boolean) -> Unit, () -> Unit,
            (ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>)
        ) -> Unit = { enabled, onToggle, onDismiss, it ->
            coroutineScope.launch {
                val selectedSongIds = navVM.getSelectedSongIds()
                onTryDelete(
                    enabled, context, audios = selectedSongIds, navVM = navVM,
                    onToggle = onToggle, onDismissModal = onDismiss, writeRequestLauncher = it
                )
            }
        }

        NavigationDrawer(
            gesturesEnabled = route != PickedSongDestination.route &&
                    route != LocalPlDestination.route &&
                    route != EditAudioDestination.route &&
                    route != AddToPlDestination.route &&
                    route != LyricsEditorDestination.route,
            drawerState = drawerState,
            modalContent = modalContent,
            autoUpdate = audioStates.autoUpdate, writingEnabled = writingEnabled
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            val name = plWithAudio?.playlist?.name
                            if ((route == LocalMusicDestination.route ||
                                        route == LocalPlDestination.route ||
                                        route.isLibraryBasedRoute()) && isSearching
                            ) {
                                SearchTextField(
                                    query = query,
                                    onValueChange = { navVM.updateQuery(it) },
                                    onUpdateQuerySet = { navVM.updateRecentQuery(it) },
                                    querySet = querySet,
                                    onTurnOff = { navVM.turnOffSearch() },
                                    ci = ci
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
                            } else if (route == LyricsEditorDestination.route) {
                                val idx by navVM.lyricEditorIdx.collectAsStateWithLifecycle()
                                if (idx >= LyricIndex.AVAILABLE) LyricScrollRow(idx)
                            }
                        },
                        colors = TopAppBarColors(
                            containerColor = getUIStyle.topBarColor(),
                            navigationIconContentColor = getUIStyle.themedOnContainerColor(),
                            titleContentColor = getUIStyle.themedOnContainerColor(),
                            actionIconContentColor = getUIStyle.themedOnContainerColor(),
                            scrolledContainerColor = getUIStyle.themedOnContainerColor()
                        ),
                        actions = {
                            when (route) {
                                LocalMusicDestination.route -> {
                                    var enabled by rememberSaveable { mutableStateOf(true) }
                                    var showModal by rememberSaveable { mutableStateOf(false) }
                                    val defaultMediaDir by
                                    navVM.defaultMediaDir.collectAsStateWithLifecycle()
                                    WithDeleteModalAndLauncher(
                                        onResultOk = { onPressDelete(enabled) { enabled = it } },
                                        onDismissRequest = { showModal = false },
                                        onDelete = {
                                            onPressTryDelete(
                                                enabled, { e -> enabled = e },
                                                { showModal = false }, it
                                            )
                                        },
                                        string = "the following ${navVM.getSelectedSongIds().size} items?",
                                        showModal = showModal
                                    ) { writeRequestLauncher ->
                                        LocalAudioOptions(
                                            navVM = navVM, getUIStyle = getUIStyle,
                                            defaultMediaDir = defaultMediaDir,
                                            onRefresh = {
                                                coroutineScope.launch { navVM.updateAudioFiles() }
                                            },
                                            onSearch = { navVM.turnOnSearch() },
                                            onUpdateSongOrder = { it, hidden ->
                                                if (hidden) navVM.updateHiddenOrder(it)
                                                else navVM.updateLocalALOrder(it)
                                            },
                                            onReverseSongOrder = {
                                                if (it) navVM.reverseHiddenOrder()
                                                else navVM.reverseALOrder()
                                            },
                                            onUpdatePlsOrder = { navVM.updateLocalPLOrder(it) },
                                            onReversePlsOrder = { navVM.reverseLocalPlsOrder() },
                                            onUpdateAlbumOrder = { navVM.updateAlbumOrder(it) },
                                            onReverseAlbumOrder = { navVM.reverseAlbumOrder() },
                                            onUpdateArtistOrder = { navVM.updateArtistOrder(it) },
                                            onReverseArtistOrder = { navVM.reverseArtistOrder() },
                                            onUpdateGenreOrder = { navVM.updateGenreOrder(it) },
                                            onReverseGenreOrder = { navVM.reverseGenreOrder() },
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
                                            onAddSongs = {
                                                onNavigate(
                                                    AddToPlDestination.route,
                                                    AddToPlDestination.createRoute(true)
                                                )
                                            },
                                            onShareSongs = {
                                                val selectedSongIds = navVM.getSelectedSongIds()
                                                shareMultipleAudios(context, selectedSongIds)
                                            },
                                            onUpdateMetadata = {
                                                showMetadataDialog = Pair(true, it)
                                            },
                                            onSelectAll = {
                                                navVM.selectAllSongs {
                                                    toast.makeMessage(toast.unableToGet2kPlusFiles)
                                                }
                                            },
                                            onDeleteSongs = { showModal = true }
                                        )
                                    }
                                }

                                LocalPlDestination.route -> {
                                    if (!isSearching) PlayListOptions(
                                        navVM, getUIStyle,
                                        onChangeArt = { showImageModal = ShowModalFor.LocalPl },
                                        onSearch = { navVM.turnOnSearch() },
                                        onChangeName = { showRenamePlDialog = true },
                                        onShareSongs = {
                                            val selectedSongIds = navVM.getSelectedSongIds()
                                            shareMultipleAudios(context, selectedSongIds)
                                        }
                                    )
                                }

                                PickedSongDestination.route -> {

                                }

                                in librarySet -> {
                                    var enabled by rememberSaveable { mutableStateOf(true) }
                                    var showModal by rememberSaveable { mutableStateOf(false) }
                                    WithDeleteModalAndLauncher(
                                        onResultOk = { onPressDelete(enabled) { enabled = it } },
                                        onDismissRequest = { showModal = false },
                                        onDelete = {
                                            onPressTryDelete(
                                                enabled, { e -> enabled = e },
                                                { showModal = false }, it
                                            )
                                        },
                                        string = "the following ${navVM.getSelectedSongIds().size} items?",
                                        showModal = showModal
                                    ) { writeRequestLauncher ->
                                        LibraryRouteOptions(
                                            routeFlow = navVM.route,
                                            onSearch = { navVM.turnOnSearch() },
                                            onShareSongs = {
                                                val selectedSongIds = navVM.getSelectedSongIds()
                                                shareMultipleAudios(context, selectedSongIds)
                                            },
                                            onAddSongs = {
                                                onNavigate(
                                                    AddToPlDestination.route,
                                                    AddToPlDestination.createRoute(true)
                                                )
                                            },
                                            onDeleteSongs = { showModal = true },
                                            getUIStyle = getUIStyle
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (route == LocalMusicDestination.route && !isSearching) {
                                if (localTab == LocalMusicTabs.PLAYLIST)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch { drawerState.open() }
                                            }
                                        ) { ci.ContentIcon(Icons.Filled.Menu) }
                                        AddIconButton(ci) { showAddPlDialog = true }
                                    }
                                else if (localTab.isSelectable() && isSelecting)
                                    IconButton(onClick = { navVM.endSelect() }) {
                                        ci.ContentIcon(Icons.Default.Close)
                                    }
                                else IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } }
                                ) { ci.ContentIcon(Icons.Filled.Menu) }
                            } else if (route == PickedSongDestination.route) IconButton(onClick = {
                                val route =
                                    mainNavController.previousBackStackEntry?.destination?.route
                                route?.let { navVM.updateRoute(it) }
                                navVM.stopCheckingPosition()
                                mainNavController.popBackStack()
                            }) { ci.ContentIcon(Icons.Default.KeyboardArrowDown) }
                            else if (route == EditAudioDestination.route) IconButton(onClick = {
                                val route =
                                    mainNavController.previousBackStackEntry?.destination?.route
                                route?.let { navVM.updateRoute(it) }
                                mainNavController.popBackStack()
                            }) { ci.ContentIcon(Icons.Default.Close) }
                            else if (route == AddToPlDestination.route ||
                                route == LyricsEditorDestination.route
                            ) IconButton(onClick = {
                                val route =
                                    mainNavController.previousBackStackEntry?.destination?.route
                                route?.let { navVM.updateRoute(it) }
                                navVM.resetSearch()
                                mainNavController.popBackStack()
                            }) {
                                ci.ContentIcon(Icons.Default.Close)
                            }
                            else if (
                                route == LocalPlDestination.route && !isSearching && !isSelecting
                            ) IconButton(onClick = {
                                val route =
                                    mainNavController.previousBackStackEntry?.destination?.route
                                if (route != null) {
                                    navVM.updateRoute(route); mainNavController.popBackStack()
                                } else {
                                    val new = LocalMusicDestination.route
                                    navVM.updateRoute(new); mainNavController.navigate(new)
                                }
                            }) { ci.ContentIcon(Icons.Default.KeyboardArrowDown) }
                            else if (route == LocalPlDestination.route && isAdding && !isSearching)
                                IconButton(onClick = { navVM.endSelect() }) {
                                    ci.ContentIcon(Icons.Default.Close)
                                }
                            else if (route == SettingsDestination.route) IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } }
                            ) { ci.ContentIcon(Icons.Filled.Menu) }
                            else if (route.isMediaBasedRoute()) {
                                if (!isSearching && !isSelecting) IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } }
                                ) { ci.ContentIcon(Icons.Filled.Menu) }
                                else if (!isSearching) IconButton(onClick = { navVM.endSelect() }) {
                                    ci.ContentIcon(Icons.Default.Close)
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
                                    InsertResult.Exists ->
                                        toast.makeMessage(toast.nameAlreadyExists)

                                    InsertResult.Failure ->
                                        toast.makeMessage(toast.failedToAddPl)

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
                                    InsertResult.Exists ->
                                        toast.makeMessage(toast.nameAlreadyExists)

                                    InsertResult.Failure -> Toast.makeText(
                                        context, "Failed to rename playlist", Toast.LENGTH_SHORT
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
                                    ShowModalFor.Idle -> toast.makeMessage(toast.nullType)

                                    ShowModalFor.LocalPl -> {
                                        plWithAudio?.let {
                                            navVM.updateLocalPlArtwork(it.playlist, img)
                                        }
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
                                    showMetadataDialog.second,
                                    selectedSongIds,
                                    navVM,
                                    context,
                                    string
                                )
                                if (result is UpdateResult.Failure) Toast.makeText(
                                    context, "Failed to update selected tag", Toast.LENGTH_SHORT
                                ).show()

                                onUpdateMetadata()
                            }
                        }
                    ) { writeRequestLauncher ->
                        UpdateAudioListMetadata(
                            showMetadataDialog.first,
                            getUIStyle = getUIStyle,
                            enabled = dismissEnabled,
                            onDismiss = { showMetadataDialog = Pair(false, "") },
                            onSubmit = {
                                coroutineScope.launch {
                                    dismissEnabled = false
                                    metadataString = it
                                    val selectedSongIds = navVM.getSelectedSongIds()
                                    val result = updateTags(
                                        showMetadataDialog.second,
                                        selectedSongIds,
                                        navVM,
                                        context,
                                        it
                                    )
                                    when (val r = result) {
                                        is UpdateResult.FileException ->
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                requestWritePermission(
                                                    writeRequestLauncher,
                                                    r.request
                                                )
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
                    mediaController?.let { mc ->
                        if (route.showPlayer()) {
                            PlayerController(
                                mc, navVM, getUIStyle = getUIStyle,
                                onClickSong = { onNavigate(PickedSongDestination.route, null) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .height(60.dp)
                            )
                        }
                    } ?: Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(60.dp)
                            .clickable { getController() },
                        contentAlignment = Alignment.Center
                    ) {
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    internal fun CustomNavigationTabBars(
        onReturnHome: () -> Unit, controller: NavHostController, content: @Composable () -> Unit
    ) {
        val audioStates by navVM.audioStates.collectAsStateWithLifecycle()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val writingEnabled by navVM.writingEnabled.collectAsStateWithLifecycle()
        val route by navVM.route.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        val modalContent = ModalContent(
            controller, getUIStyle, navVM, route,
            onClose = { coroutineScope.launch { drawerState.close() } },
            onNavigate = {
                if (it == LocalMusicDestination.route) onReturnHome()
                else if (route != it) {
                    navVM.updateIndexListener(LyricIndex.UNAVAILABLE)
                    navVM.updateRoute(it)
                    controller.navigate(it)
                    coroutineScope.launch { drawerState.close() }
                }
            }
        )
        val ci = ContentIcons(getUIStyle)
        NavigationDrawer(
            gesturesEnabled = true, drawerState = drawerState, modalContent = modalContent,
            autoUpdate = audioStates.autoUpdate, writingEnabled = writingEnabled
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            if (route == LyricsEditorDestination.route) {
                                val idx by navVM.lyricEditorIdx.collectAsStateWithLifecycle()
                                if (idx >= LyricIndex.AVAILABLE) LyricScrollRow(idx)
                            }
                        },
                        colors = TopAppBarColors(
                            containerColor = getUIStyle.topBarColor(),
                            navigationIconContentColor = getUIStyle.themedOnContainerColor(),
                            titleContentColor = getUIStyle.themedOnContainerColor(),
                            actionIconContentColor = getUIStyle.themedOnContainerColor(),
                            scrolledContainerColor = getUIStyle.themedOnContainerColor()
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } }
                            ) { ci.ContentIcon(Icons.Filled.Menu) }
                        }
                    )
                }
            ) { innerPadding ->
                Box(Modifier.padding(innerPadding)) { content() }
            }
        }
    }


    @Composable
    private fun NavigationDrawer(
        gesturesEnabled: Boolean, drawerState: DrawerState, modalContent: ModalContent,
        autoUpdate: Boolean, writingEnabled: Boolean,
        content: @Composable () -> Unit
    ) {
        ModalNavigationDrawer(
            gesturesEnabled = gesturesEnabled,
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxWidth(0.70f),
                    drawerContentColor = getUIStyle.themedOnContainerColor()
                ) {
                    modalContent.Home()
                    modalContent.LyricList()
                    modalContent.Settings()
                    modalContent.AutoUpdateEnabled(autoUpdate)
                    modalContent.IdWritingUpdateEnabled(writingEnabled)
                }
            },
        ) { content() }
    }


    @Composable
    private fun LyricScrollRow(idx: Int) =
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            LyricFilterChip(idx, LyricIndex.DESCRIPTION, stringResource(R.string.Description))
            LyricFilterChip(idx, LyricIndex.PLAIN, stringResource(R.string.Plain))
            LyricFilterChip(idx, LyricIndex.SYNCRYONIZED, stringResource(R.string.Syncryonized))
            LyricFilterChip(idx, LyricIndex.PRONUNCIATION, stringResource(R.string.Pronunciation))
            LyricFilterChip(idx, LyricIndex.TRANSLATION, stringResource(R.string.Translation))
        }

    @Composable
    private fun LyricFilterChip(idx: Int, lyricIndex: Int, text: String) {
        FilterChip(
            selected = idx == lyricIndex,
            onClick = { navVM.updateIndexListener(lyricIndex) },
            label = { Text(text) }, modifier = Modifier.padding(horizontal = 2.dp)
        )
    }

    private fun String.isMediaBasedRoute() =
        this == LyricsListDestination.route || this.isLibraryBasedRoute()

    private fun String.isLibraryBasedRoute() =
        this == LocalAlbumDestination.route || this == LocalBucketDestination.route ||
                this == LocalArtistDestination.route || this == LocalGenreDestination.route

    private val librarySet = setOf(
        LocalAlbumDestination.route, LocalBucketDestination.route,
        LocalArtistDestination.route, LocalGenreDestination.route
    )
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
    val toast = XCToast(context)
    return when (string) {
        TagProperty.ARTIST -> navVM.updateArtistsOfAL(selectedSongIds, it)

        TagProperty.ALBUM -> navVM.updateAlbumOfAL(selectedSongIds, it)

        TagProperty.GENRE -> navVM.updateGenreOfAL(selectedSongIds, it)

        else -> {
            toast.makeMessage(toast.unknownProperty)
            UpdateResult.Failure
        }
    }
}

private suspend fun onDelete(
    enabled: Boolean, context: Context,
    onToggle: (Boolean) -> Unit, navVM: NavViewModel
) {
    val toast = XCToast(context)
    if (!enabled) return
    onToggle(false)
    val list = navVM.getSelectedSongIds().takeIf { it.isNotEmpty() }
    if (list == null) {
        toast.makeMessage(toast.emptyList)
        onToggle(true)
        return
    }
    val result = navVM.deleteAudios(list)
    if (result !is DeleteResult.Success)
        toast.makeMessage(toast.failedToDeleteTracks)
    else {
        toast.makeMessage(toast.deletedTracks(list.size)); navVM.endSelect()
    }
    onToggle(true)
}

private suspend fun onTryDelete(
    enabled: Boolean, context: Context, onToggle: (Boolean) -> Unit,
    audios: List<String>, navVM: NavViewModel, onDismissModal: () -> Unit,
    writeRequestLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
) {
    val toast = XCToast(context)

    if (!enabled) return
    onToggle(false)
    val list = audios.takeIf { it.isNotEmpty() }
    if (list == null) {
        toast.makeMessage(toast.emptyList)
        onToggle(true)
        return
    }
    val result = navVM.deleteAudios(list)
    when (val r = result) {
        is DeleteResult.FileException ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                requestWritePermission(writeRequestLauncher, r.request)

        is DeleteResult.Success -> toast.makeMessage(toast.deletedTracks(list.size))

        else -> toast.makeMessage(toast.failedToDeleteTracks)
    }
    onToggle(true); onDismissModal()
}

private fun String.showPlayer() =
    this != PickedSongDestination.route && this != EditAudioDestination.route
            && this != AddToPlDestination.route && this != SettingsDestination.route &&
            this != LyricsListDestination.route && this != LyricsEditorDestination.route