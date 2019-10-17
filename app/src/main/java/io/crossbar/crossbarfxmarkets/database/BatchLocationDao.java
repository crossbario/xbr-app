package io.crossbar.crossbarfxmarkets.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface BatchLocationDao {
    @Query("SELECT * from locations_batch where uid = :uid LIMIT 1")
    public LocationBatch getBatchByID(String uid);

    @Insert
    public void insert(LocationBatch... batches);
}
