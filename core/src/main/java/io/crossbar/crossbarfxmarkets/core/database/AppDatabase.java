///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets.core.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {DisconnectionStat.class, NetworkUsageStat.class, WAMPLatencyStat.class,
        LocationLog.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DisconnectStatDao getDCStatDao();
    public abstract NetworkUsageStatDao getNetworkStatDao();
    public abstract WAMPLatencyStatDao getWAMPLatencyStatDao();
    public abstract LocationLogDao getLocationLogDao();
}
