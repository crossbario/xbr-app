package network.xbr.xbrisgold;


import android.app.Service;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.TransportOptions;
import network.xbr.xbrisgold.database.AppDatabase;
import network.xbr.xbrisgold.database.DisconnectionStat;
import network.xbr.xbrisgold.database.StatsKeyValueStore;

public class LongRunningService extends Service {

    private static final String TAG = LongRunningService.class.getName();
    private static final long RECONNECT_INTERVAL = 20000;
    private static final long CALL_QUEUE_INTERVAL = 3000;

    private long mLastConnectRequestTime;
    private boolean mWasReconnectRequest;
    private Handler mHandler;
    private Runnable mLastCallback;

    private StatsKeyValueStore mStatsStore;
    private AppDatabase mStatsDB;

    private BroadcastReceiver mNetworkStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            connectToServer(context);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mStatsStore = StatsKeyValueStore.getInstance(getApplicationContext());
        mStatsDB = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,
                "connection-stats").build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, String.format("Crash count: %s", mStatsStore.getServiceCrashCount()));
        mHandler = new Handler();
        registerReceiver(mNetworkStateChangeListener,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        // Automatically restarts the service if killed by the OS.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mNetworkStateChangeListener);
        super.onDestroy();
        mStatsStore.appendServiceCrashCount();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        mStatsStore.appendServiceCrashCount();
    }

    private long getTimeSinceLastConnectRequest() {
        return System.currentTimeMillis() - mLastConnectRequestTime;
    }

    private void connectToServer(Context ctx) {
        if (Helpers.isNetworkAvailable(ctx.getApplicationContext())) {
            if (mWasReconnectRequest && getTimeSinceLastConnectRequest() < RECONNECT_INTERVAL) {
                mWasReconnectRequest = false;
                mHandler.removeCallbacks(mLastCallback);
            } else if (!mWasReconnectRequest
                    && getTimeSinceLastConnectRequest() < CALL_QUEUE_INTERVAL) {
                Log.i(TAG, "Remove previous connect request as new arrived before " +
                        "CALL_QUEUE_INTERVAL timeout");
                mHandler.removeCallbacks(mLastCallback);
            }

            Helpers.isInternetWorking().whenComplete((working, throwable) -> {
                mLastConnectRequestTime = System.currentTimeMillis();
                if (working) {
                    mLastCallback = this::actuallyConnect;
                    mHandler.postDelayed(mLastCallback, CALL_QUEUE_INTERVAL);
                } else {
                    // Network is available but we don't have a working internet
                    // lets schedule something to recheck if internet is working.
                    mLastCallback = () -> connectToServer(ctx);
                    mWasReconnectRequest = true;
                    mHandler.postDelayed(mLastCallback, RECONNECT_INTERVAL);
                }
            });
        } else {
            Log.i(TAG, "Network not available");
        }
    }

    private void actuallyConnect() {
        mStatsStore.appendConnectionAttemptsCount();

        Session wampSession = new Session();
        wampSession.addOnJoinListener(this::onJoin);
        wampSession.addOnLeaveListener((session, closeDetails) -> {
            Log.i(TAG, String.format("LEAVE, reason=%s", closeDetails.reason));
            DisconnectionStat stat = new DisconnectionStat();
            stat.reason = closeDetails.reason;
            stat.time = new Date(System.currentTimeMillis()).toString();
            stat.wasNetworkAvailable = Helpers.isNetworkAvailable(getApplicationContext());

            // Reading/Writing to databases need to be done on a separate thread.
            new Thread(() -> {
                mStatsDB.getDCStatDao().insert(stat);
                Log.i(TAG, "Insert new stat");
            }).start();
        });
        wampSession.addOnDisconnectListener((session, b) -> {
            Log.i(TAG, String.format("DISCONNECTED, clean=%s", b));
            connectToServer(getApplicationContext());
        });
        IAuthenticator auth = new CryptosignAuth(
                "test@crossbario.com",
                "ef83d35678742e01fa412d597cd3909c113b12a8a7dc101cba073c0423c9db41",
                "e5b0d24af05c77d644de885946147aeb4fa6897a5cf4eb14347c3d637664b117");
        Client client = new Client(wampSession, "ws://178.62.69.210:8080/ws", "realm1", auth);

        TransportOptions options = new TransportOptions();
        options.setAutoPingInterval(66);
        client.connect(options).whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });
    }

    private CompletableFuture<InvocationResult> stats() {
        CompletableFuture<InvocationResult> future = new CompletableFuture<>();
        Log.i(TAG, "Called stats");
        int crashCount = mStatsStore.getServiceCrashCount();
        int successCount = mStatsStore.getConnectionSuccessCount();
        int failureCount = mStatsStore.getConnectionFailureCount();
        int retriesCount = mStatsStore.getConnectionRetriesCount();

        new Thread(() -> {
            for (DisconnectionStat stat: mStatsDB.getDCStatDao().getAll()) {
                Log.i(TAG, stat.toString());
            }
            String res = String.format("crash: %s, success %s, failure %s, retries %s",
                    crashCount, successCount, failureCount, retriesCount);
            Log.i(TAG, res);
            future.complete(new InvocationResult(res));
        }).start();

        return future;
    }

    private void onJoin(Session session, SessionDetails details) {
        mStatsStore.appendConnectionSuccessCount();

        RegisterOptions options = new RegisterOptions(
                RegisterOptions.MATCH_EXACT, RegisterOptions.INVOKE_ROUNDROBIN);

        String proc2 = "network.xbr.connection_stats";
        session.register(proc2, this::stats, options).whenComplete((registration, throwable) -> {
            if (throwable == null) {
                Log.i(TAG, String.format("Registered procedure %s", proc2));
            } else {
                throwable.printStackTrace();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
