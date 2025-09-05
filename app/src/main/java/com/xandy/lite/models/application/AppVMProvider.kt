package com.xandy.lite.models.application

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xandy.lite.controllers.view.models.AddToLocalPlVM
import com.xandy.lite.controllers.view.models.LocalAlbumVM
import com.xandy.lite.controllers.view.models.LocalArtistVM
import com.xandy.lite.controllers.view.models.LocalFolderVM
import com.xandy.lite.controllers.view.models.LocalGenreVM
import com.xandy.lite.controllers.view.models.LocalMediaVM
import com.xandy.lite.controllers.view.models.LocalPLVM
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.navigation.NavViewModel

object AppVMProvider {
    val Factory = viewModelFactory {
        initializer {
            NavViewModel(
                this.createSavedStateHandle(),
                xandyCloudApplication().container.uiRepository,
                xandyCloudApplication().container.songRepository
            )
        }
        initializer {
            PickedSongVM(xandyCloudApplication().container.songRepository)
        }
        initializer {
            LocalMediaVM(
                xandyCloudApplication().container.songRepository,
                xandyCloudApplication().container.uiRepository
            )
        }
        initializer {
            LocalPLVM(
                xandyCloudApplication().container.songRepository,
                xandyCloudApplication().container.uiRepository
            )
        }
        initializer { AddToLocalPlVM(xandyCloudApplication().container.songRepository) }
        initializer {
            LocalAlbumVM(
                xandyCloudApplication().container.songRepository,
                xandyCloudApplication().container.uiRepository
            )
        }
        initializer {
            LocalArtistVM(
                xandyCloudApplication().container.songRepository,
                xandyCloudApplication().container.uiRepository
            )
        }
        initializer {
            LocalFolderVM(
                xandyCloudApplication().container.songRepository,
                xandyCloudApplication().container.uiRepository
            )
        }
        initializer {
            LocalGenreVM(
                xandyCloudApplication().container.songRepository,
                xandyCloudApplication().container.uiRepository
            )
        }
    }
}

fun CreationExtras.xandyCloudApplication(): XandyCloudApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as XandyCloudApplication)