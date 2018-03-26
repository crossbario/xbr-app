package network.xbr.xbrisgold;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.SessionDetails;

public class LongRunningService extends Service {

    private static final String TAG = LongRunningService.class.getName();
    private static final long RECONNECT_INTERVAL = 10000;
    private static final long CALL_QUEUE_INTERVAL = 1000;

    private static boolean sIsRunning;

    private long mLastConnectRequestTime;
    private boolean mWasReconnectRequest;
    private Handler mHandler;
    private Runnable mLastCallback;
    private int mTick;

    private void appendCrashCount() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int currentCount = preferences.getInt("crash_count", 0);
        preferences.edit().putInt("crash_count", currentCount + 1).apply();
    }
1
    private int getCrashCount() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getInt("crash_count", 0);
    }


    public static boolean isRunning() {
        return sIsRunning;
    }

    private BroadcastReceiver mNetworkStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            connectToServer(context);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, String.format("Crash count: %s", getCrashCount()));
        sIsRunning = true;
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
        sIsRunning = false;
        appendCrashCount();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        appendCrashCount();
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
                Log.i(TAG, "REMOVE");
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
        Session wampSession = new Session();
        wampSession.addOnJoinListener(this::onJoin);
        wampSession.addOnLeaveListener((session, closeDetails) -> Log.i(TAG, "LEFT"));
        wampSession.addOnDisconnectListener((session, b) -> {
            Log.i(TAG, String.format("DISCONNECTED, clean=%s", b));
            connectToServer(getApplicationContext());
        });
        IAuthenticator auth = new CryptosignAuth(
                "test@crossbario.com",
                "ef83d35678742e01fa412d597cd3909c113b12a8a7dc101cba073c0423c9db41",
                "e5b0d24af05c77d644de885946147aeb4fa6897a5cf4eb14347c3d637664b117");
        Client client = new Client(wampSession, "ws://178.62.69.210:8080/ws", "realm1", auth);
        client.connect().whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });
    }

    private void onJoin(Session session, SessionDetails details) {
        if (!session.isConnected()) {
            return;
        }
        session.publish("io.xbr.gold.tick", mTick).whenComplete((publication, throwable) -> {
            if (throwable == null) {
                Log.i(TAG, String.format("Published io.xbr.gold.tick %s", mTick));
            }
        });
        mTick++;
        new Handler().postDelayed(() -> onJoin(session, details), 10000);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
