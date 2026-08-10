package com.makkispacejam.fluxa.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@SuppressWarnings("ALL")
@Database(
    entities = {
        SubscriptionEntity.class,
        PlaylistEntity.class,
        PlaylistItemEntity.class,
        VideoInteractionEntity.class,
        WatchedVideoEntity.class,
        CachedVideoEntity.class
    },
    version = 8,
    exportSchema = false
)
public abstract class FluxaDatabase extends RoomDatabase {

    public abstract FluxaDao fluxaDao();

    private static volatile FluxaDatabase INSTANCE;

    public static FluxaDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (FluxaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    FluxaDatabase.class, "fluxa_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
