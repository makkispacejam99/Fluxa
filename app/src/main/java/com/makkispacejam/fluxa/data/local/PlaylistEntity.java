package com.makkispacejam.fluxa.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;
    public String name;
    public boolean isSystemPlaylist;
    public long createdAt;

    public PlaylistEntity(String name, boolean isSystemPlaylist) {
        this.name = name;
        this.isSystemPlaylist = isSystemPlaylist;
        this.createdAt = System.currentTimeMillis();
    }
}
