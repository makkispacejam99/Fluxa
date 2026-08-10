package com.makkispacejam.fluxa.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "video_interactions")
public class VideoInteractionEntity {
    @PrimaryKey
    @NonNull
    public String videoId;
    public String title;
    public String channelName;
    public boolean isLiked;
    public boolean isDisliked;
    public long progressMs;
    public long durationMs;
    public int viewCount;
    public long lastWatchedAt;
    public boolean isShort;

    public VideoInteractionEntity(@NonNull String videoId) {
        this.videoId = videoId;
        this.lastWatchedAt = System.currentTimeMillis();
    }
}
