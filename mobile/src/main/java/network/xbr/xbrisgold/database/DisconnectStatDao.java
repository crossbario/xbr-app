package network.xbr.xbrisgold.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface DisconnectStatDao {
    @Query("SELECT * FROM disconnection_stats")
    List<DisconnectionStat> getAll();

    @Insert
    void insert(DisconnectionStat... stats);
}
