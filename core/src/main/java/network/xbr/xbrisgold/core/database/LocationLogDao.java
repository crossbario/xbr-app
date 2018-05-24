///////////////////////////////////////////////////////////////////////////////
//
//   XBR is Gold - https://xbr.network
//
//   Copyright (c) Crossbar.io Technologies GmbH. All rights reserved
//   Licensed under the GPL 3.0 https://opensource.org/licenses/GPL-3.0
//
///////////////////////////////////////////////////////////////////////////////

package network.xbr.xbrisgold.core.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import network.xbr.xbrisgold.core.database.LocationLog;

@Dao
public interface LocationLogDao {
    @Query("SELECT * FROM location_log")
    List<LocationLog> getAll();

    @Insert
    void insert(LocationLog... log);
}
