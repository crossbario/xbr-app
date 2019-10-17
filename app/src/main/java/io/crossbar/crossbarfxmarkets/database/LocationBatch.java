package io.crossbar.crossbarfxmarkets.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;


@Entity(tableName = "locations_batch")
public class LocationBatch {
    @PrimaryKey @NonNull public String uid;

    public LocationBatch(String uid) {
        this.uid = uid;
    }
}
