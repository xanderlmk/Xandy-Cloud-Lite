package com.xandy.lite.models.ui.order.by

import android.os.Parcelable
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.db.tables.toPlaylistWithCount
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.models.ui.Album
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.models.ui.LocalPlUIState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
fun OrderSongsBy.toHiddenUIState(audioDao: AudioDao): Flow<AudioUIState>  = when (this) {
    OrderSongsBy.ArtistASC -> audioDao.getFlowOfHiddenSongsByArtistASC().map {
        AudioUIState(it)
    }
    OrderSongsBy.ArtistDESC -> audioDao.getFlowOfHiddenSongsByArtistDESC().map {
        AudioUIState(it)
    }
    OrderSongsBy.CreatedOnASC -> audioDao.getFlowOfHiddenSongsByCreatedOnASC().map {
        AudioUIState(it)
    }
    OrderSongsBy.CreatedOnDESC -> audioDao.getFlowOfHiddenSongsByCreatedOnDESC().map {
        AudioUIState(it)
    }
    OrderSongsBy.TitleASC -> audioDao.getFlowOfHiddenSongsByTitleASC().map {
        AudioUIState(it)
    }
    OrderSongsBy.TitleDESC -> audioDao.getFlowOfHiddenSongsByTitleDESC().map {
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


@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<OrderPlsBy>.toLocalPls(
    localAudioDao: PlaylistDao,
    appStrings: Flow<AppStrings>
): Flow<LocalPlUIState> =
    combine(this, appStrings) { order, strings ->
        order to strings
    }.flatMapLatest { (order, strings) ->
        when (order) {
            OrderPlsBy.CreatedOnASC ->
                localAudioDao.getFlowOfPlsWithSongsByCreatedOnASC()
            OrderPlsBy.CreatedOnDESC ->
                localAudioDao.getFlowOfPlsWithSongsByCreatedOnDESC()
            OrderPlsBy.NameASC ->
                localAudioDao.getFlowOfPlsWithSongsByNameASC()
            OrderPlsBy.NameDESC ->
                localAudioDao.getFlowOfPlsWithSongsByNameDESC()
        }.map { LocalPlUIState(it.toPlaylistWithCount(strings)) }
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

@Parcelize
sealed class OrderAlbumsBy : Parcelable {
    @Parcelize
    data object NameASC : OrderAlbumsBy()

    @Parcelize
    data object NameDESC : OrderAlbumsBy()

    @Parcelize
    data object ArtistASC : OrderAlbumsBy()

    @Parcelize
    data object ArtistDESC : OrderAlbumsBy()

    @Parcelize
    data object TrackCountASC : OrderAlbumsBy()

    @Parcelize
    data object TrackCountDESC : OrderAlbumsBy()

    @Parcelize
    data object Default : OrderAlbumsBy()
}

sealed class AlbumOrder {
    data object Name : AlbumOrder()
    data object Artist : AlbumOrder()
    data object TrackCount : AlbumOrder()
    data object Default : AlbumOrder()
}

fun AlbumOrder.toOrderedByClass(asc: Boolean): OrderAlbumsBy = when (this) {
    AlbumOrder.Artist -> if (asc) OrderAlbumsBy.ArtistASC else OrderAlbumsBy.ArtistDESC
    AlbumOrder.Name -> if (asc) OrderAlbumsBy.NameASC else OrderAlbumsBy.NameDESC
    AlbumOrder.TrackCount -> if (asc) OrderAlbumsBy.TrackCountASC else OrderAlbumsBy.TrackCountDESC
    AlbumOrder.Default -> OrderAlbumsBy.Default
}

fun OrderAlbumsBy.reverseSort(): OrderAlbumsBy = when (this) {
    OrderAlbumsBy.ArtistASC -> OrderAlbumsBy.ArtistDESC
    OrderAlbumsBy.ArtistDESC -> OrderAlbumsBy.ArtistASC
    OrderAlbumsBy.NameASC -> OrderAlbumsBy.NameDESC
    OrderAlbumsBy.NameDESC -> OrderAlbumsBy.NameASC
    OrderAlbumsBy.TrackCountASC -> OrderAlbumsBy.TrackCountDESC
    OrderAlbumsBy.TrackCountDESC -> OrderAlbumsBy.TrackCountASC
    OrderAlbumsBy.Default -> OrderAlbumsBy.Default
}

fun OrderAlbumsBy.toOrderedString(): String = when (this) {
    OrderAlbumsBy.ArtistASC -> OBS.ARTIST_ASC
    OrderAlbumsBy.ArtistDESC -> OBS.ARTIST_DESC
    OrderAlbumsBy.NameASC -> OBS.NAME_ASC
    OrderAlbumsBy.NameDESC -> OBS.NAME_DESC
    OrderAlbumsBy.TrackCountASC -> OBS.TRACK_COUNT_ASC
    OrderAlbumsBy.TrackCountDESC -> OBS.TRACK_COUNT_DESC
    OrderAlbumsBy.Default -> OBS.DEFAULT
}

fun String.toAlbumsOrderedByClass(): OrderAlbumsBy = when (this) {
    OBS.ARTIST_ASC -> OrderAlbumsBy.ArtistASC
    OBS.ARTIST_DESC -> OrderAlbumsBy.ArtistDESC
    OBS.NAME_DESC -> OrderAlbumsBy.NameDESC
    OBS.TRACK_COUNT_DESC -> OrderAlbumsBy.TrackCountDESC
    OBS.TRACK_COUNT_ASC -> OrderAlbumsBy.TrackCountASC
    OBS.DEFAULT -> OrderAlbumsBy.Default
    else -> OrderAlbumsBy.NameASC
}

fun OrderAlbumsBy.isAscending() =
    this is OrderAlbumsBy.TrackCountASC || this is OrderAlbumsBy.NameASC ||
            this is OrderAlbumsBy.ArtistASC

fun OrderAlbumsBy.comparator(): Comparator<Album> =
    Comparator { a, b ->
        when (this) {
            is OrderAlbumsBy.NameASC -> a.name.compareTo(b.name, ignoreCase = true)
            is OrderAlbumsBy.NameDESC -> b.name.compareTo(a.name, ignoreCase = true)
            is OrderAlbumsBy.ArtistASC -> a.artist.compareTo(b.artist, ignoreCase = true)
            is OrderAlbumsBy.ArtistDESC -> b.artist.compareTo(a.artist, ignoreCase = true)
            is OrderAlbumsBy.TrackCountASC -> a.songCount.compareTo(b.songCount)
            is OrderAlbumsBy.TrackCountDESC -> b.songCount.compareTo(a.songCount)
            is OrderAlbumsBy.Default -> 0
        }
    }

@Parcelize
sealed class OrderArtistBy : Parcelable {
    @Parcelize
    data object NameASC : OrderArtistBy()

    @Parcelize
    data object NameDESC : OrderArtistBy()

    @Parcelize
    data object TrackCountASC : OrderArtistBy()

    @Parcelize
    data object TrackCountDESC : OrderArtistBy()

    @Parcelize
    data object AlbumCountASC : OrderArtistBy()

    @Parcelize
    data object AlbumCountDESC : OrderArtistBy()

    @Parcelize
    data object Default : OrderArtistBy()
}

sealed class ArtistOrder {
    data object Name : ArtistOrder()
    data object TrackCount : ArtistOrder()
    data object AlbumCount : ArtistOrder()
    data object Default : ArtistOrder()
}

fun ArtistOrder.toOrderedByClass(asc: Boolean): OrderArtistBy = when (this) {
    ArtistOrder.AlbumCount -> if (asc) OrderArtistBy.AlbumCountASC else OrderArtistBy.AlbumCountDESC
    ArtistOrder.Name -> if (asc) OrderArtistBy.NameASC else OrderArtistBy.NameDESC
    ArtistOrder.TrackCount -> if (asc) OrderArtistBy.TrackCountASC else OrderArtistBy.TrackCountDESC
    ArtistOrder.Default -> OrderArtistBy.Default
}

fun OrderArtistBy.reverseSort(): OrderArtistBy = when (this) {
    OrderArtistBy.AlbumCountASC -> OrderArtistBy.AlbumCountDESC
    OrderArtistBy.AlbumCountDESC -> OrderArtistBy.AlbumCountASC
    OrderArtistBy.NameASC -> OrderArtistBy.NameDESC
    OrderArtistBy.NameDESC -> OrderArtistBy.NameASC
    OrderArtistBy.TrackCountASC -> OrderArtistBy.TrackCountDESC
    OrderArtistBy.TrackCountDESC -> OrderArtistBy.TrackCountASC
    OrderArtistBy.Default -> OrderArtistBy.Default
}

fun OrderArtistBy.toOrderedString(): String = when (this) {
    OrderArtistBy.AlbumCountASC -> OBS.ALBUM_COUNT_ASC
    OrderArtistBy.AlbumCountDESC -> OBS.ALBUM_COUNT_DESC
    OrderArtistBy.Default -> OBS.DEFAULT
    OrderArtistBy.NameASC -> OBS.NAME_ASC
    OrderArtistBy.NameDESC -> OBS.NAME_DESC
    OrderArtistBy.TrackCountASC -> OBS.TRACK_COUNT_ASC
    OrderArtistBy.TrackCountDESC -> OBS.TRACK_COUNT_DESC
}

fun String.toArtistOrderedByClass(): OrderArtistBy = when (this) {
    OBS.ALBUM_COUNT_ASC -> OrderArtistBy.AlbumCountASC
    OBS.ALBUM_COUNT_DESC -> OrderArtistBy.AlbumCountDESC
    OBS.NAME_ASC -> OrderArtistBy.NameASC
    OBS.NAME_DESC -> OrderArtistBy.NameDESC
    OBS.TRACK_COUNT_ASC -> OrderArtistBy.TrackCountASC
    OBS.TRACK_COUNT_DESC -> OrderArtistBy.TrackCountDESC
    else -> OrderArtistBy.Default
}

fun OrderArtistBy.isAscending() =
    this is OrderArtistBy.TrackCountASC || this is OrderArtistBy.AlbumCountASC ||
            this is OrderArtistBy.NameASC

fun OrderArtistBy.comparator(): Comparator<Artist> =
    Comparator { a, b ->
        when (this) {
            is OrderArtistBy.NameASC -> a.name.compareTo(b.name, ignoreCase = true)
            is OrderArtistBy.NameDESC -> b.name.compareTo(a.name, ignoreCase = true)
            is OrderArtistBy.TrackCountASC -> a.songCount.compareTo(b.songCount)
            is OrderArtistBy.TrackCountDESC -> b.songCount.compareTo(a.songCount)
            is OrderArtistBy.AlbumCountASC -> a.albumCount.compareTo(b.albumCount)
            is OrderArtistBy.AlbumCountDESC -> b.albumCount.compareTo(a.albumCount)
            is OrderArtistBy.Default -> 0
        }
    }


@Parcelize
sealed class OrderGenresBy : Parcelable {
    @Parcelize
    data object NameASC : OrderGenresBy()

    @Parcelize
    data object NameDESC : OrderGenresBy()

    @Parcelize
    data object TrackCountASC : OrderGenresBy()

    @Parcelize
    data object TrackCountDESC : OrderGenresBy()

    @Parcelize
    data object Default : OrderGenresBy()
}

sealed class GenreOrder {
    data object Name : GenreOrder()
    data object TrackCount : GenreOrder()
    data object Default : GenreOrder()
}

fun GenreOrder.toOrderedByClass(asc: Boolean): OrderGenresBy = when (this) {
    GenreOrder.Default -> OrderGenresBy.Default
    GenreOrder.Name -> if (asc) OrderGenresBy.NameASC else OrderGenresBy.NameDESC
    GenreOrder.TrackCount -> if (asc) OrderGenresBy.TrackCountASC else OrderGenresBy.TrackCountDESC
}

fun OrderGenresBy.reverseSort(): OrderGenresBy = when (this) {
    OrderGenresBy.Default -> OrderGenresBy.Default
    OrderGenresBy.NameASC -> OrderGenresBy.NameDESC
    OrderGenresBy.NameDESC -> OrderGenresBy.NameASC
    OrderGenresBy.TrackCountASC -> OrderGenresBy.TrackCountDESC
    OrderGenresBy.TrackCountDESC -> OrderGenresBy.TrackCountASC
}

fun OrderGenresBy.toOrderedString(): String = when (this) {
    OrderGenresBy.Default -> OBS.DEFAULT
    OrderGenresBy.NameASC -> OBS.NAME_ASC
    OrderGenresBy.NameDESC -> OBS.NAME_DESC
    OrderGenresBy.TrackCountASC -> OBS.TRACK_COUNT_ASC
    OrderGenresBy.TrackCountDESC -> OBS.TRACK_COUNT_DESC
}

fun String.toGenresOrderedByClass(): OrderGenresBy = when (this) {
    OBS.NAME_ASC -> OrderGenresBy.NameASC
    OBS.NAME_DESC -> OrderGenresBy.NameDESC
    OBS.TRACK_COUNT_ASC -> OrderGenresBy.TrackCountASC
    OBS.TRACK_COUNT_DESC -> OrderGenresBy.TrackCountDESC
    else -> OrderGenresBy.Default
}

fun OrderGenresBy.isAscending() =
    this is OrderGenresBy.NameASC || this is OrderGenresBy.TrackCountASC

fun OrderGenresBy.comparator(): Comparator<Genre> =
    Comparator { a, b ->
        when (this) {
            is OrderGenresBy.NameASC -> a.name.compareTo(b.name, ignoreCase = true)
            is OrderGenresBy.NameDESC -> b.name.compareTo(a.name, ignoreCase = true)
            is OrderGenresBy.TrackCountASC ->  a.songCount.compareTo(b.songCount)
            is OrderGenresBy.TrackCountDESC -> b.songCount.compareTo(a.songCount)
            is OrderGenresBy.Default -> 0
        }
    }