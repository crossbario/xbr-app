package network.xbr.xbrisgold.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "wamp_latency_stat")
public class WAMPLatencyStat {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public long timeSent;

    public long timeReceived;

}
