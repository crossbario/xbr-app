///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets.core.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface NetworkUsageStatDao {
    @Query("SELECT * FROM network_usage_stats")
    List<NetworkUsageStat> getAll();

    @Insert
    void insert(NetworkUsageStat... stats);
}
