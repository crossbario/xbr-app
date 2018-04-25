package network.xbr.xbrisgold;


import android.app.Service;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.TransportOptions;
import network.xbr.xbrisgold.database.AppDatabase;
import network.xbr.xbrisgold.database.DisconnectionStat;
import network.xbr.xbrisgold.database.NetworkUsageStat;
import network.xbr.xbrisgold.database.StatsKeyValueStore;
import network.xbr.xbrisgold.database.WAMPLatencyStat;
import network.xbr.xbrisgold.database.WAMPLatencyStatDao;

public class LongRunningService extends Service implements OnSharedPreferenceChangeListener {

    private static final String TAG = LongRunningService.class.getName();
    private static final String NETWORK_STATE_CHANGE_INTENT =
            "android.net.conn.CONNECTIVITY_CHANGE";
    private static final long RECONNECT_INTERVAL = 20000;
    private static final long CALL_QUEUE_INTERVAL = 3000;

    private static final RegisterOptions REGISTER_OPTIONS = new RegisterOptions(
            RegisterOptions.MATCH_EXACT, RegisterOptions.INVOKE_ROUNDROBIN);
    private static final String PROC_NET_STATS = "network.xbr.connection_stats";

    private long mLastConnectRequestTime;
    private boolean mWasReconnectRequest;
    private Handler mHandler;
    private Runnable mLastCallback;
    private StatsKeyValueStore mStatsStore;
    private AppDatabase mStatsDB;
    private SharedPreferences mSharedPreferences;
    private LocalBroadcastManager mLocalBroadcaster;

    private BroadcastReceiver mStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(NETWORK_STATE_CHANGE_INTENT)) {
                connectToServer(context);
            } else if (action.equals(MainApplication.INTENT_APP_VISIBILITY_CHANGED)) {
                boolean isVisible = intent.getBooleanExtra("app_visible", true);
                System.out.println("Is visible: " + isVisible);
                if (Helpers.isWifiConnected(getApplicationContext())) {

                } else if (Helpers.isMobileDataConnected(getApplicationContext())) {

                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mStatsStore = StatsKeyValueStore.getInstance(getApplicationContext());
        mStatsDB = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,
                "connection-stats").build();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mLocalBroadcaster = LocalBroadcastManager.getInstance(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, String.format("Crash count: %s", mStatsStore.getServiceCrashCount()));
        mHandler = new Handler();
        registerReceiver(mStateChangeListener,
                new IntentFilter(NETWORK_STATE_CHANGE_INTENT));
        mLocalBroadcaster.registerReceiver(mStateChangeListener,
                new IntentFilter(MainApplication.INTENT_APP_VISIBILITY_CHANGED));
        // Automatically restarts the service if killed by the OS.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mStateChangeListener);
        mLocalBroadcaster.unregisterReceiver(mStateChangeListener);
        mStatsStore.appendServiceCrashCount();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        unregisterReceiver(mStateChangeListener);
        mLocalBroadcaster.unregisterReceiver(mStateChangeListener);
        mStatsStore.appendServiceCrashCount();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
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
        NetworkUsageStat networkUsageStat = new NetworkUsageStat();
        mStatsStore.appendConnectionAttemptsCount();

        int UID = android.os.Process.myUid();
        long bytesRxBefore = TrafficStats.getUidRxBytes(UID);
        long bytesTxBefore = TrafficStats.getUidTxBytes(UID);

        Session wampSession = new Session();
        wampSession.addOnConnectListener(session -> {
            networkUsageStat.connectTime = System.currentTimeMillis();
        });
        wampSession.addOnJoinListener(this::onJoin);
        wampSession.addOnLeaveListener((session, closeDetails) -> {
            Log.i(TAG, String.format("LEAVE, reason=%s", closeDetails.reason));
            DisconnectionStat stat = new DisconnectionStat();
            stat.reason = closeDetails.reason;
            stat.time = Helpers.getCurrentDate();
            stat.wasNetworkAvailable = Helpers.isNetworkAvailable(getApplicationContext());

            Helpers.callInThread(() -> {
                mStatsDB.getDCStatDao().insert(stat);
                Log.i(TAG, "Insert new stat");
            });
        });
        wampSession.addOnDisconnectListener((session, b) -> {
            networkUsageStat.disconnectTime = System.currentTimeMillis();
            long bytesRx = TrafficStats.getUidRxBytes(UID) - bytesRxBefore;
            long bytesTx = TrafficStats.getUidTxBytes(UID) - bytesTxBefore;
            long bytesTotal = bytesRx + bytesTx;
            mStatsStore.appendAppNetworkUsage(bytesTotal);
            networkUsageStat.bytesReceived = bytesRx;
            networkUsageStat.bytesSent = bytesTx;
            networkUsageStat.totalBytes = bytesTotal;
            Helpers.callInThread(() -> mStatsDB.getNetworkStatDao().insert(networkUsageStat));

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
        networkUsageStat.connectRequestTime = System.currentTimeMillis();
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

        String res = String.format("crash: %s, success %s, failure %s, retries %s",
                crashCount, successCount, failureCount, retriesCount);
        Helpers.callInThread(() -> {
            for (DisconnectionStat stat: mStatsDB.getDCStatDao().getAll()) {
                Log.i(TAG, stat.toString());
            }
            Log.i(TAG, res);
            future.complete(new InvocationResult(res));
        });

        return future;
    }

    private void onJoin(Session session, SessionDetails details) {
        mStatsStore.appendConnectionSuccessCount();
        CompletableFuture<Registration> regFuture = session.register(
                PROC_NET_STATS, this::stats, REGISTER_OPTIONS);
        regFuture.whenComplete((registration, throwable) -> {
            if (throwable == null) {
                Log.i(TAG, String.format("Registered procedure %s", PROC_NET_STATS));
            } else {
                throwable.printStackTrace();
            }
        });

        WAMPLatencyStatDao latencyStatDao = mStatsDB.getWAMPLatencyStatDao();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(() -> {
            WAMPLatencyStat stat = new WAMPLatencyStat();
            stat.timeSent = System.currentTimeMillis();
            CompletableFuture<Publication> pubFuture = session.publish("network.xbr.wamp_rtt");
            pubFuture.whenComplete((publication, throwable) -> {
                if (throwable == null) {
                    stat.timeReceived = System.currentTimeMillis();
                    Helpers.callInThread(() -> latencyStatDao.insert(stat));
                    System.out.println(String.format("RTT: %s ms",
                            stat.timeReceived - stat.timeSent));
                }
            });
            try {
                pubFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                pubFuture.completeExceptionally(e);
            }
        }, 0, 2, TimeUnit.MINUTES);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.key_policy_wifi_foreground))) {
            System.out.println();
        } else if (key.equals(getString(R.string.key_policy_mobile_data_foreground))) {
            System.out.println();
        }
    }
}
