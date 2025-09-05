package com.xandy.lite.models.ui.order.by

import android.os.Parcelable
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.db.tables.toPlaylistWithCount
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.LocalPlUIState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class OrderSongsBy : Parcelable {
    @Parcelize
    data object TitleASC : OrderSongsBy()

    @Parcelize
    data object TitleDESC : OrderSongsBy()

    @Parcelize
    data object CreatedOnASC : OrderSongsBy()

    @Parcelize
    data object CreatedOnDESC : OrderSongsBy()

    @Parcelize
    data object ArtistASC : OrderSongsBy()

    @Parcelize
    data object ArtistDESC : OrderSongsBy()
}

sealed class SongOrder {
    data object Title : SongOrder()
    data object CreatedOn : SongOrder()
    data object Artist : SongOrder()
}

fun SongOrder.toOrderedByClass(asc: Boolean): OrderSongsBy = when (this) {
    is SongOrder.Title -> if (asc) OrderSongsBy.TitleASC else OrderSongsBy.TitleDESC
    is SongOrder.CreatedOn -> if (asc) OrderSongsBy.CreatedOnASC else OrderSongsBy.CreatedOnDESC
    is SongOrder.Artist -> if (asc) OrderSongsBy.ArtistASC else OrderSongsBy.ArtistDESC
}

fun OrderSongsBy.reverseSort(): OrderSongsBy = when (this) {
    OrderSongsBy.CreatedOnASC -> OrderSongsBy.CreatedOnDESC
    OrderSongsBy.CreatedOnDESC -> OrderSongsBy.CreatedOnASC
    OrderSongsBy.TitleASC -> OrderSongsBy.TitleDESC
    OrderSongsBy.TitleDESC -> OrderSongsBy.TitleASC
    OrderSongsBy.ArtistASC -> OrderSongsBy.ArtistDESC
    OrderSongsBy.ArtistDESC -> OrderSongsBy.ArtistASC
}

fun OrderSongsBy.toOrderedString(): String = when (this) {
    OrderSongsBy.CreatedOnASC -> OBS.CREATED_ON_ASC
    OrderSongsBy.CreatedOnDESC -> OBS.CREATED_ON_DESC
    OrderSongsBy.TitleASC -> OBS.TITLE_ASC
    OrderSongsBy.TitleDESC -> OBS.TITLE_DESC
    OrderSongsBy.ArtistASC -> OBS.ARTIST_ASC
    OrderSongsBy.ArtistDESC -> OBS.ARTIST_DESC
}

fun String.toSongsOrderedByClass(): OrderSongsBy = when (this) {
    OBS.TITLE_DESC -> OrderSongsBy.TitleDESC
    OBS.CREATED_ON_ASC -> OrderSongsBy.CreatedOnASC
    OBS.CREATED_ON_DESC -> OrderSongsBy.CreatedOnDESC
    OBS.ARTIST_ASC -> OrderSongsBy.ArtistASC
    OBS.ARTIST_DESC -> OrderSongsBy.ArtistDESC
    else -> OrderSongsBy.TitleASC
}

/** Whether the order is ascending. */
fun OrderSongsBy.isAscending() =
    this is OrderSongsBy.TitleASC || this is OrderSongsBy.ArtistASC || this is OrderSongsBy.CreatedOnASC

fun OrderSongsBy.toAudioUIState(audioDao: AudioDao): Flow<AudioUIState> =
    when (this) {
        OrderSongsBy.CreatedOnASC -> audioDao.getFlowOfSongsWithPlsByCreatedOnASC().map {
            AudioUIState(it)
        }

        OrderSongsBy.CreatedOnDESC -> audioDao.getFlowOfSongsWithPlsByCreatedOnDESC().map {
            AudioUIState(it)
        }

        OrderSongsBy.TitleASC -> audioDao.getFlowOfSongsWithPlsByTitleASC().map {
            AudioUIState(it)
        }

        OrderSongsBy.TitleDESC -> audioDao.getFlowOfSongsWithPlsByTitleDESC().map {
            AudioUIState(it)
        }

        OrderSongsBy.ArtistASC -> audioDao.getFlowOfSongsWithPlsByArtistASC().map {
            AudioUIState(it)
        }

        OrderSongsBy.ArtistDESC -> audioDao.getFlowOfSongsWithPlsByArtistDESC().map {
            AudioUIState(it)
        }
    }

sealed class OrderPlsBy {
    data object NameASC : OrderPlsBy()
    data object NameDESC : OrderPlsBy()
    data object CreatedOnASC : OrderPlsBy()
    data object CreatedOnDESC : OrderPlsBy()
}

sealed class PlaylistOrder {
    data object Name : PlaylistOrder()
    data object CreatedOn : PlaylistOrder()
}

fun PlaylistOrder.toOrderedByClass(asc: Boolean): OrderPlsBy = when (this) {
    is PlaylistOrder.Name -> if (asc) OrderPlsBy.NameASC else OrderPlsBy.NameDESC
    is PlaylistOrder.CreatedOn -> if (asc) OrderPlsBy.CreatedOnASC else OrderPlsBy.CreatedOnDESC
}

fun OrderPlsBy.reverseSort(): OrderPlsBy = when (this) {
    OrderPlsBy.CreatedOnASC -> OrderPlsBy.CreatedOnDESC
    OrderPlsBy.CreatedOnDESC -> OrderPlsBy.CreatedOnASC
    OrderPlsBy.NameASC -> OrderPlsBy.NameDESC
    OrderPlsBy.NameDESC -> OrderPlsBy.NameASC
}

fun OrderPlsBy.toOrderedString(): String = when (this) {
    OrderPlsBy.CreatedOnASC -> OBS.CREATED_ON_ASC
    OrderPlsBy.CreatedOnDESC -> OBS.CREATED_ON_DESC
    OrderPlsBy.NameASC -> OBS.NAME_ASC
    OrderPlsBy.NameDESC -> OBS.NAME_DESC
}

fun String.toPlsOrderedByClass(): OrderPlsBy = when (this) {
    OBS.CREATED_ON_ASC -> OrderPlsBy.CreatedOnASC
    OBS.CREATED_ON_DESC -> OrderPlsBy.CreatedOnDESC
    OBS.NAME_DESC -> OrderPlsBy.NameDESC
    else -> OrderPlsBy.NameASC
}

fun OrderPlsBy.isAscending() =
    this is OrderPlsBy.CreatedOnASC || this is OrderPlsBy.NameASC



fun OrderPlsBy.toLocalPls(localAudioDao: PlaylistDao): Flow<LocalPlUIState> =
    when (this) {
        OrderPlsBy.CreatedOnASC -> localAudioDao.getFlowOfPlsWithSongsByCreatedOnASC().map {
            LocalPlUIState(it.toPlaylistWithCount())
        }

        OrderPlsBy.CreatedOnDESC -> localAudioDao.getFlowOfPlsWithSongsByCreatedOnDESC().map {
            LocalPlUIState(it.toPlaylistWithCount())
        }

        OrderPlsBy.NameASC -> localAudioDao.getFlowOfPlsWithSongsByNameASC().map {
            LocalPlUIState(it.toPlaylistWithCount())
        }

        OrderPlsBy.NameDESC -> localAudioDao.getFlowOfPlsWithSongsByNameDESC().map {
            LocalPlUIState(it.toPlaylistWithCount())
        }
    }

object OBS {
    const val CREATED_ON_ASC = "created_on_asc"
    const val CREATED_ON_DESC = "created_on_desc"
    const val TITLE_ASC = "title_asc"
    const val TITLE_DESC = "title_desc"
    const val NAME_ASC = "name_asc"
    const val NAME_DESC = "name_desc"
    const val ARTIST_ASC = "artist_asc"
    const val ARTIST_DESC = "artist_desc"
    const val DEFAULT = "default_order"
}

@Parcelize
sealed class OrderQueueBy : Parcelable {
    @Parcelize
    data object TitleASC : OrderQueueBy()

    @Parcelize
    data object TitleDESC : OrderQueueBy()

    @Parcelize
    data object CreatedOnASC : OrderQueueBy()

    @Parcelize
    data object CreatedOnDESC : OrderQueueBy()

    @Parcelize
    data object ArtistASC : OrderQueueBy()

    @Parcelize
    data object ArtistDESC : OrderQueueBy()

    @Parcelize
    data object Default : OrderQueueBy()
}

sealed class QueueOrder {
    data object Title : QueueOrder()
    data object CreatedOn : QueueOrder()
    data object Artist : QueueOrder()
    data object Default : QueueOrder()
}

fun QueueOrder.toOrderedByClass(asc: Boolean): OrderQueueBy = when (this) {
    is QueueOrder.Title -> if (asc) OrderQueueBy.TitleASC else OrderQueueBy.TitleDESC
    is QueueOrder.CreatedOn -> if (asc) OrderQueueBy.CreatedOnASC else OrderQueueBy.CreatedOnDESC
    is QueueOrder.Artist -> if (asc) OrderQueueBy.ArtistASC else OrderQueueBy.ArtistDESC
    is QueueOrder.Default -> OrderQueueBy.Default
}

fun OrderQueueBy.reverseSort(): OrderQueueBy = when (this) {
    OrderQueueBy.CreatedOnASC -> OrderQueueBy.CreatedOnDESC
    OrderQueueBy.CreatedOnDESC -> OrderQueueBy.CreatedOnASC
    OrderQueueBy.TitleASC -> OrderQueueBy.TitleDESC
    OrderQueueBy.TitleDESC -> OrderQueueBy.TitleASC
    OrderQueueBy.ArtistASC -> OrderQueueBy.ArtistDESC
    OrderQueueBy.ArtistDESC -> OrderQueueBy.ArtistASC
    OrderQueueBy.Default -> OrderQueueBy.Default
}

fun OrderQueueBy.toOrderedString(): String = when (this) {
    OrderQueueBy.CreatedOnASC -> OBS.CREATED_ON_ASC
    OrderQueueBy.CreatedOnDESC -> OBS.CREATED_ON_DESC
    OrderQueueBy.TitleASC -> OBS.TITLE_ASC
    OrderQueueBy.TitleDESC -> OBS.TITLE_DESC
    OrderQueueBy.ArtistASC -> OBS.ARTIST_ASC
    OrderQueueBy.ArtistDESC -> OBS.ARTIST_DESC
    OrderQueueBy.Default -> OBS.DEFAULT
}

fun String.toQueueOrderedByClass(): OrderQueueBy = when (this) {
    OBS.TITLE_ASC -> OrderQueueBy.TitleASC
    OBS.TITLE_DESC -> OrderQueueBy.TitleDESC
    OBS.CREATED_ON_ASC -> OrderQueueBy.CreatedOnASC
    OBS.CREATED_ON_DESC -> OrderQueueBy.CreatedOnDESC
    OBS.ARTIST_ASC -> OrderQueueBy.ArtistASC
    OBS.ARTIST_DESC -> OrderQueueBy.ArtistDESC
    else -> OrderQueueBy.Default
}

fun OrderQueueBy.isAscending() =
    this is OrderQueueBy.CreatedOnASC || this is OrderQueueBy.TitleASC ||
            this is OrderQueueBy.ArtistASC
