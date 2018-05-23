///////////////////////////////////////////////////////////////////////////////
//
//   XBR is Gold - https://xbr.network
//
//   Copyright (c) Crossbar.io Technologies GmbH. All rights reserved
//   Licensed under the GPL 3.0 https://opensource.org/licenses/GPL-3.0
//
///////////////////////////////////////////////////////////////////////////////

package network.xbr.xbrisgold.core.database;

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
