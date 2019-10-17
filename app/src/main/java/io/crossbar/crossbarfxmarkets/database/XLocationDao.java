package io.crossbar.crossbarfxmarkets.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface XLocationDao {
    @Query("SELECT * FROM locations")
    List<XLocation> getAll();

    @Query("SELECT * from locations where batch_id = :batchID")
    public List<XLocation> getLocationsByBatchID(String batchID);

    @Insert
    void insert(XLocation... locations);
}
