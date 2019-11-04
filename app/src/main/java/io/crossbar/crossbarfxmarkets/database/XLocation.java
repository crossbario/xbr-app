package io.crossbar.crossbarfxmarkets.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations",
        foreignKeys = @ForeignKey(entity = LocationBatch.class,
                parentColumns = "uid",
                childColumns = "batch_id"))
public class XLocation {
    @PrimaryKey(autoGenerate = true) public int uid;
    @ColumnInfo(name = "batch_id", index = true) public int batchID;

    public double latitude;
    public double longitude;
    public long time;

    public XLocation(double latitude, double longitude, long time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    @Override
    public String toString() {
        return String.format("Location: %s,%s time: %s", latitude, longitude, time);
    }
}
