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

@Entity(tableName = "wamp_latency_stat")
public class WAMPLatencyStat {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public long timeSent;

    public long timeReceived;

}
