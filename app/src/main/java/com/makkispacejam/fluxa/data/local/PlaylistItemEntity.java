package com.makkispacejam.fluxa.data.local;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "playlist_items",
    foreignKeys = @ForeignKey(
        entity = PlaylistEntity.class,
        parentColumns = "id",
        childColumns = "playlistId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index(value = {"playlistId", "videoId"})
)
public class PlaylistItemEntity {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;
    public long playlistId;
    public String videoId;
    public String title;
    public String channelName;
    public String channelId;
    public String thumbnailUrl;
    public String uploaderAvatarUrl;
    public long durationSeconds;
    public long addedAt;

    public PlaylistItemEntity(long playlistId, String videoId, String title, String channelName, String channelId, String thumbnailUrl, String uploaderAvatarUrl, long durationSeconds) {
        this.playlistId = playlistId;
        this.videoId = videoId;
        this.title = title;
        this.channelName = channelName;
        this.channelId = channelId != null ? channelId : "";
        this.thumbnailUrl = thumbnailUrl;
        this.uploaderAvatarUrl = uploaderAvatarUrl;
        this.durationSeconds = durationSeconds;
        this.addedAt = System.currentTimeMillis();
    }
}
