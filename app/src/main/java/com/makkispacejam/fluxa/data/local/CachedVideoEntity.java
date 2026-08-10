package com.makkispacejam.fluxa.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_videos")
public class CachedVideoEntity {
    @PrimaryKey
    @NonNull
    public String videoId;
    public String title;
    public String channelName;
    public String channelId;
    public String channelAvatarUrl;
    public String imageUrl;
    public String videoUrl;
    public long timestamp;
    public long viewCount;
    public long cachedAt;

    public CachedVideoEntity(@NonNull String videoId) {
        this.videoId = videoId;
        this.cachedAt = System.currentTimeMillis();
    }
}
