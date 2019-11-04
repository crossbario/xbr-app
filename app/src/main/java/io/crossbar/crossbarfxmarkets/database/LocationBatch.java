package io.crossbar.crossbarfxmarkets.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "locations_batch")
public class LocationBatch {
    @PrimaryKey(autoGenerate = true) public int uid;
}
