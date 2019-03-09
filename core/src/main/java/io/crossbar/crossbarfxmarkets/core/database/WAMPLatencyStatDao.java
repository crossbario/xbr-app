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
public interface WAMPLatencyStatDao {
    @Query("SELECT * FROM wamp_latency_stat")
    List<WAMPLatencyStat> getAll();

    @Insert
    void insert(WAMPLatencyStat... stats);
}
