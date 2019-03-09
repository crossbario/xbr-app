///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets.core.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "location_log")
public class LocationLog {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public double longitude;

    public double latitude;

    public float accuracy;

    @Override
    public String toString() {
        return String.format("Location: %s,%s Accuracy: %s", latitude, longitude, accuracy);
    }
}
