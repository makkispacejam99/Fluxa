package com.makkispacejam.fluxa.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;

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
    version = 9,
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
                            .addMigrations(MIGRATION_8_9)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE cached_videos ADD COLUMN duration INTEGER NOT NULL DEFAULT 0");
        }
    };
}
