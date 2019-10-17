package io.crossbar.crossbarfxmarkets.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {XLocation.class, LocationBatch.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract XLocationDao getLocationDao();
    public abstract BatchLocationDao getLocationBatchDao();
}
