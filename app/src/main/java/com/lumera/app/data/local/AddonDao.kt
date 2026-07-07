package com.lumera.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lumera.app.data.model.AddonEntity
import com.lumera.app.data.model.CatalogConfigEntity
import com.lumera.app.data.model.HubRowEntity
import com.lumera.app.data.model.HubRowItemEntity
import com.lumera.app.data.model.HubRowWithItems
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.model.SeriesNextUpEntity
import com.lumera.app.data.model.ThemeEntity
import com.lumera.app.data.model.WatchHistoryEntity
import com.lumera.app.data.model.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddonDao {

    @Query("SELECT * FROM addons ORDER BY sortOrder ASC")
    fun getAllAddons(): Flow<List<AddonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddon(addon: AddonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddons(addons: List<AddonEntity>)

    @Query("DELETE FROM addons WHERE transportUrl = :url")
    suspend fun deleteAddonByUrl(url: String)

    @Query("DELETE FROM addons")
    suspend fun clearAddons()

    @Query("SELECT * FROM addons WHERE transportUrl = :transportUrl")
    suspend fun getAddon(transportUrl: String): AddonEntity?

    @Query("SELECT * FROM catalog_configs")
    fun getAllCatalogConfigs(): Flow<List<CatalogConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCatalogConfig(config: CatalogConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCatalogConfigs(configs: List<CatalogConfigEntity>)

    @Query("DELETE FROM catalog_configs WHERE transportUrl = :url")
    suspend fun deleteCatalogConfigs(url: String)

    @Query("DELETE FROM catalog_configs")
    suspend fun clearCatalogConfigs()

    @Query("SELECT * FROM catalog_configs WHERE uniqueId = :uniqueId")
    suspend fun getCatalogConfig(uniqueId: String): CatalogConfigEntity?

    @Query("SELECT * FROM profiles")
    fun getProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getProfileFlow(id: Int): Flow<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: Int)

    // ── Watch History / Continue Watching ──

    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history")
    suspend fun getAllWatchHistoryOnce(): List<WatchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoryItems(items: List<WatchHistoryEntity>)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    @Query("SELECT * FROM watch_history WHERE id = :id")
    suspend fun getHistoryItem(id: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE id LIKE :prefix || '%'")
    suspend fun getHistoryItemsByPrefix(prefix: String): List<WatchHistoryEntity>

    @Query(
        "SELECT * FROM watch_history " +
            "WHERE type = 'series' AND id LIKE :episodePrefix " +
            "ORDER BY lastWatched DESC LIMIT 1"
    )
    suspend fun getLatestSeriesEpisodeHistory(episodePrefix: String): WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: String)

    @Query("SELECT * FROM watch_history WHERE type = 'series' AND id LIKE :episodePrefix")
    suspend fun getSeriesEpisodeHistory(episodePrefix: String): List<WatchHistoryEntity>

    @Query("DELETE FROM watch_history WHERE type = 'series' AND id LIKE :episodePrefix")
    suspend fun deleteSeriesHistory(episodePrefix: String)

    @Query("SELECT * FROM watch_history WHERE scrobbled = 1 AND watched = 0")
    suspend fun getScrobbledInProgressItems(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE scrobbled = 1 AND watched = 1")
    suspend fun getScrobbledWatchedItems(): List<WatchHistoryEntity>

    @Query("SELECT id FROM watch_history WHERE watched = 1")
    fun getWatchedIds(): Flow<List<String>>

    @Query("UPDATE watch_history SET poster = :poster, background = :background, logo = :logo WHERE id = :id")
    suspend fun updateHistoryImages(id: String, poster: String?, background: String?, logo: String?)

    // ── Themes ──

    @Query("SELECT * FROM themes")
    fun getAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun getThemeById(id: String): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity)

    @Delete
    suspend fun deleteTheme(theme: ThemeEntity)

    // ── Hub Rows ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRow(row: HubRowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRows(rows: List<HubRowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRowItems(items: List<HubRowItemEntity>)

    @Transaction
    suspend fun insertHubRowWithItems(row: HubRowEntity, items: List<HubRowItemEntity>) {
        insertHubRow(row)
        insertHubRowItems(items)
    }

    @Query("SELECT * FROM hub_rows ORDER BY homeOrder ASC, createdAt ASC")
    fun getAllHubRows(): Flow<List<HubRowEntity>>

    @Query("SELECT * FROM hub_row_items ORDER BY itemOrder ASC")
    fun getAllHubRowItems(): Flow<List<HubRowItemEntity>>

    @Query("DELETE FROM hub_rows WHERE id = :hubRowId")
    suspend fun deleteHubRow(hubRowId: String)

    @Query("DELETE FROM hub_row_items WHERE hubRowId = :hubRowId")
    suspend fun deleteHubRowItems(hubRowId: String)

    @Query("DELETE FROM hub_row_items")
    suspend fun clearHubRowItems()

    @Query("DELETE FROM hub_rows")
    suspend fun clearHubRows()

    @Transaction
    suspend fun deleteHubRowWithItems(hubRowId: String) {
        deleteHubRowItems(hubRowId)
        deleteHubRow(hubRowId)
    }

    @Query("UPDATE hub_row_items SET customImageUrl = :imageUrl WHERE hubRowId = :hubRowId AND configUniqueId = :configUniqueId")
    suspend fun updateHubItemImage(hubRowId: String, configUniqueId: String, imageUrl: String?)

    @Transaction
    @Query("SELECT * FROM hub_rows ORDER BY homeOrder ASC, createdAt ASC")
    fun getHubRowsWithItems(): Flow<List<HubRowWithItems>>

    @Query("SELECT MAX(homeOrder) FROM hub_rows")
    suspend fun getMaxHubHomeOrder(): Int?

    @Query("SELECT MAX(moviesOrder) FROM hub_rows")
    suspend fun getMaxHubMoviesOrder(): Int?

    @Query("SELECT MAX(seriesOrder) FROM hub_rows")
    suspend fun getMaxHubSeriesOrder(): Int?

    @Update
    suspend fun updateHubRow(row: HubRowEntity)

    @Update
    suspend fun updateHubRows(rows: List<HubRowEntity>)

    @Query("DELETE FROM hub_row_items WHERE hubRowId = :hubRowId AND configUniqueId = :configUniqueId")
    suspend fun deleteHubRowItem(hubRowId: String, configUniqueId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRowItem(item: HubRowItemEntity)

    @Query("SELECT MAX(itemOrder) FROM hub_row_items WHERE hubRowId = :hubRowId")
    suspend fun getMaxHubItemOrder(hubRowId: String): Int?

    @Update
    suspend fun updateHubRowItem(item: HubRowItemEntity)

    @Update
    suspend fun updateHubRowItems(items: List<HubRowItemEntity>)

    // ── Watchlist ──

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE type = :type ORDER BY addedAt DESC")
    fun getWatchlistByType(type: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    suspend fun getWatchlistOnce(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE id = :id")
    suspend fun getWatchlistItem(id: String): WatchlistEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id)")
    suspend fun isInWatchlist(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id)")
    fun isInWatchlistFlow(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWatchlistItems(items: List<WatchlistEntity>)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun removeFromWatchlist(id: String)

    @Query("DELETE FROM watchlist")
    suspend fun clearWatchlist()

    // ── Series Next Up ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeriesNextUp(entry: SeriesNextUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeriesNextUpItems(items: List<SeriesNextUpEntity>)

    @Query("SELECT * FROM series_next_up ORDER BY updatedAt DESC")
    suspend fun getAllSeriesNextUpOnce(): List<SeriesNextUpEntity>

    @Query("SELECT * FROM series_next_up WHERE isComplete = 0 ORDER BY updatedAt DESC")
    fun getActiveSeriesNextUp(): Flow<List<SeriesNextUpEntity>>

    @Query("SELECT * FROM series_next_up WHERE seriesId = :seriesId")
    suspend fun getSeriesNextUp(seriesId: String): SeriesNextUpEntity?

    @Query("DELETE FROM series_next_up WHERE seriesId = :seriesId")
    suspend fun deleteSeriesNextUp(seriesId: String)

    @Query("DELETE FROM series_next_up")
    suspend fun clearSeriesNextUp()

    // ── Runtime State Replace ──

    @Transaction
    suspend fun replaceRuntimeState(
        addons: List<AddonEntity>,
        catalogConfigs: List<CatalogConfigEntity>,
        hubRows: List<HubRowEntity>,
        hubRowItems: List<HubRowItemEntity>,
        watchHistory: List<WatchHistoryEntity>,
        watchlist: List<WatchlistEntity>,
        seriesNextUp: List<SeriesNextUpEntity>,
        clearUserState: Boolean = false
    ) {
        clearHubRowItems()
        clearHubRows()
        clearCatalogConfigs()
        clearAddons()
        
        // Only clear user/activity state when explicitly requested.
        // Normal app restart/profile reload should preserve saves.
        if (clearUserState) {
            clearWatchHistory()
            clearWatchlist()
            clearSeriesNextUp()
        }
    
        if (addons.isNotEmpty()) insertAddons(addons)
        if (catalogConfigs.isNotEmpty()) saveCatalogConfigs(catalogConfigs)
        if (hubRows.isNotEmpty()) insertHubRows(hubRows)
        if (hubRowItems.isNotEmpty()) insertHubRowItems(hubRowItems)
        if (watchHistory.isNotEmpty()) upsertHistoryItems(watchHistory)
        if (watchlist.isNotEmpty()) addWatchlistItems(watchlist)
        if (seriesNextUp.isNotEmpty()) upsertSeriesNextUpItems(seriesNextUp)
    }
}
