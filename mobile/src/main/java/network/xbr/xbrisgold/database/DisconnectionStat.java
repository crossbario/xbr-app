package network.xbr.xbrisgold.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "disconnection_stats")
public class DisconnectionStat {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public String time;

    public String reason;

    public boolean wasNetworkAvailable;

    @Override
    public String toString() {
        return String.format("Disconnected at %s, reason=%s", time, reason);
    }
}
