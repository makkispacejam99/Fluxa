package com.makkispacejam.fluxa.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface FluxaDao {

    // Subscripciones
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSubscription(SubscriptionEntity subscription);

    @Delete
    void deleteSubscription(SubscriptionEntity subscription);

    @Query("SELECT * FROM subscriptions WHERE isBlocked = 0 ORDER BY subscribedAt DESC")
    Flow<List<SubscriptionEntity>> getAllSubscriptionsFlow();

    @Query("SELECT * FROM subscriptions WHERE isBlocked = 0 ORDER BY subscribedAt DESC")
    List<SubscriptionEntity> getAllSubscriptions();

    @Query("SELECT * FROM subscriptions WHERE isBlocked = 0 ORDER BY subscribedAt DESC LIMIT 6")
    Flow<List<SubscriptionEntity>> getSubscriptionSummaryFlow();

    @Query("SELECT * FROM subscriptions")
    List<SubscriptionEntity> getAllSubscriptionsForBackup();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSubscriptions(List<SubscriptionEntity> subscriptions);

    @Query("DELETE FROM subscriptions")
    void clearAllSubscriptions();

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId AND isBlocked = 0)")
    Flow<Boolean> isSubscribed(String channelId);

    @Query("UPDATE subscriptions SET isBlocked = :isBlocked WHERE channelId = :channelId")
    void setBlockedStatus(String channelId, boolean isBlocked);

    @Query("UPDATE subscriptions SET avatarUrl = :avatarUrl WHERE channelId = :channelId")
    void updateSubscriptionAvatar(String channelId, String avatarUrl);

    @Query("SELECT channelId FROM subscriptions WHERE isBlocked = 1")
    List<String> getBlockedChannelIds();

    @Query("SELECT * FROM subscriptions WHERE isBlocked = 1 ORDER BY subscribedAt DESC")
    Flow<List<SubscriptionEntity>> getBlockedSubscriptionsFlow();

    @Query("SELECT * FROM subscriptions WHERE isBlocked = 1 ORDER BY subscribedAt DESC")
    List<SubscriptionEntity> getBlockedSubscriptions();

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId AND isBlocked = 1)")
    boolean isChannelBlocked(String channelId);


    // Playlists
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertPlaylist(PlaylistEntity playlist);

    @Query("SELECT * FROM playlists WHERE isSystemPlaylist = 0")
    Flow<List<PlaylistEntity>> getUserPlaylists();

    @Query("SELECT * FROM playlists WHERE name = :name AND isSystemPlaylist = 1 LIMIT 1")
    @SuppressWarnings("unused")
    Flow<PlaylistEntity> getSystemPlaylistFlowByName(String name);

    @Query("SELECT * FROM playlists WHERE name = :name AND isSystemPlaylist = 1 LIMIT 1")
    PlaylistEntity getSystemPlaylistByName(String name);

    @Delete
    void deletePlaylist(PlaylistEntity playlist);

    @Update
    void updatePlaylist(PlaylistEntity playlist);

    @Query("SELECT * FROM playlists")
    List<PlaylistEntity> getAllPlaylists();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylists(List<PlaylistEntity> playlists);

    @Query("DELETE FROM playlists")
    void clearAllPlaylists();


    // Elementos de la Playlist
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylistItem(PlaylistItemEntity item);

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    Flow<List<PlaylistItemEntity>> getItemsForPlaylist(long playlistId);

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    void removeItemFromPlaylist(long playlistId, String videoId);

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId)")
    boolean isVideoInPlaylist(long playlistId, String videoId);

    @Query("SELECT * FROM playlist_items")
    List<PlaylistItemEntity> getAllPlaylistItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylistItems(List<PlaylistItemEntity> playlistItems);

    @Query("DELETE FROM playlist_items")
    void clearAllPlaylistItems();


    // Interacciones de video
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateInteraction(VideoInteractionEntity interaction);

    @Query("SELECT * FROM video_interactions WHERE videoId = :videoId")
    VideoInteractionEntity getInteraction(String videoId);

    @Delete
    void deleteInteraction(VideoInteractionEntity interaction);

    @Query("SELECT * FROM video_interactions WHERE videoId = :videoId")
    Flow<VideoInteractionEntity> getInteractionFlow(String videoId);

    @Query("SELECT * FROM video_interactions WHERE isLiked = 1 ORDER BY lastWatchedAt DESC")
    @SuppressWarnings("unused")
    Flow<List<VideoInteractionEntity>> getLikedVideos();

    @Query("UPDATE video_interactions SET progressMs = :progress, durationMs = :duration, lastWatchedAt = :timestamp WHERE videoId = :videoId")
    void updateProgress(String videoId, long progress, long duration, long timestamp);

    @Query("UPDATE video_interactions SET viewCount = viewCount + 1, lastWatchedAt = :timestamp WHERE videoId = :videoId")
    void incrementViewCount(String videoId, long timestamp);

    @Query("SELECT * FROM video_interactions WHERE lastWatchedAt > 0 ORDER BY lastWatchedAt DESC LIMIT 100")
    Flow<List<VideoInteractionEntity>> getHistory();

    @Query("SELECT * FROM video_interactions")
    List<VideoInteractionEntity> getAllVideoInteractions();

    @Query("SELECT * FROM video_interactions ORDER BY lastWatchedAt DESC LIMIT :limit")
    List<VideoInteractionEntity> getRecentInteractions(int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVideoInteractions(List<VideoInteractionEntity> videoInteractions);

    @Query("DELETE FROM video_interactions")
    void clearAllVideoInteractions();

    @Query("DELETE FROM video_interactions WHERE videoId LIKE '%googlevideo.com%'")
    void deleteOrphanedStreamUrlEntries();

    @Query("DELETE FROM video_interactions WHERE videoId LIKE '%videoplayback%' OR length(videoId) > 20")
    void deleteGarbledVideoIdEntries();

    // Control de historial
    @Query("DELETE FROM video_interactions WHERE videoId NOT IN (SELECT videoId FROM video_interactions ORDER BY lastWatchedAt DESC LIMIT 100) AND isLiked = 0")
    void pruneOldHistory();

    @Transaction
    default void registrarHistorialYPrunar(VideoInteractionEntity interaction) {
        insertOrUpdateInteraction(interaction);
        pruneOldHistory();
    }

    // Videos ya vistos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWatchedVideo(WatchedVideoEntity watchedVideo);

    @Query("SELECT EXISTS(SELECT 1 FROM watched_videos WHERE videoId = :videoId)")
    boolean isVideoWatched(String videoId);

    @Query("SELECT videoId FROM watched_videos")
    List<String> getAllWatchedVideoIds();

    @Query("SELECT videoId FROM watched_videos")
    Flow<List<String>> getAllWatchedVideoIdsFlow();

    @Query("SELECT * FROM watched_videos")
    List<WatchedVideoEntity> getAllWatchedVideosForBackup();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWatchedVideos(List<WatchedVideoEntity> watchedVideos);

    @Query("DELETE FROM watched_videos")
    void clearAllWatchedVideos();

    @Query("DELETE FROM watched_videos WHERE videoId = :videoId")
    void removeWatchedVideo(String videoId);

    // Cacheo para shorts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCachedVideos(List<CachedVideoEntity> videos);

    @Query("SELECT * FROM cached_videos WHERE videoId IN (:videoIds)")
    List<CachedVideoEntity> getCachedVideos(List<String> videoIds);

    @Query("SELECT * FROM cached_videos ORDER BY cachedAt DESC LIMIT :limit")
    List<CachedVideoEntity> getRecentCachedVideos(int limit);

    @Query("DELETE FROM cached_videos WHERE cachedAt < :timestamp")
    void pruneOldCachedVideos(long timestamp);
}
