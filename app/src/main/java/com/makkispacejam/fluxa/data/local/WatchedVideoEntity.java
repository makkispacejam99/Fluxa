package com.makkispacejam.fluxa.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "watched_videos")
public class WatchedVideoEntity {
    @PrimaryKey
    @NonNull
    public String videoId;
    public long watchedAt;

    public WatchedVideoEntity(@NonNull String videoId) {
        this.videoId = videoId;
        this.watchedAt = System.currentTimeMillis();
    }
}
