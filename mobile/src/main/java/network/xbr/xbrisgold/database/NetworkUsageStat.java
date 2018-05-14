///////////////////////////////////////////////////////////////////////////////
//
//   XBR is Gold - https://xbr.network
//
//   Copyright (c) Crossbar.io Technologies GmbH. All rights reserved
//   Licensed under the GPL 3.0 https://opensource.org/licenses/GPL-3.0
//
///////////////////////////////////////////////////////////////////////////////

package network.xbr.xbrisgold.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "network_usage_stats")
public class NetworkUsageStat {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public long connectRequestTime;

    public long connectTime;

    public long disconnectTime;

    public long bytesSent;

    public long bytesReceived;

    public long totalBytes;

    @Override
    public String toString() {
        return String.format("Connect time: %s, disconnect time: %s, bytesSent=%s, " +
                "bytesReceived=%s, totalBytes=%s", new Date(connectTime), new Date(disconnectTime),
                bytesSent, bytesReceived, totalBytes);
    }
}
