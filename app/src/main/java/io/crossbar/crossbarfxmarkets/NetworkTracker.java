package io.crossbar.crossbarfxmarkets;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class NetworkTracker {

    private static final String TAG = NetworkTracker.class.getName();

    private final List<StateListener> mListeners = new ArrayList<>();
    private final Context mContext;

    private ConnectivityManager mCManager;
    private boolean mTracking;
    private boolean mConnected;

    public NetworkTracker(Context context) {
        mContext = context;
    }

    public interface StateListener {
        void onConnect();
        void onDisconnect();
    }

    public void addStateListener(StateListener listener) {
        mListeners.add(listener);
    }

    public void removeStateListener(StateListener listener) {
        mListeners.remove(listener);
    }

    public void start() {
        if (isRunning()) {
            Log.i(TAG, "start: Already running, ignoring this request...");
            return;
        }
        mCManager = mContext.getSystemService(ConnectivityManager.class);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        mCManager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                mConnected = true;
                mListeners.forEach(StateListener::onConnect);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                mConnected = false;
                mListeners.forEach(StateListener::onDisconnect);
            }
        });
        mTracking = true;
    }

    public void stop() {
        mCManager = null;
        mTracking = false;
    }

    public boolean isRunning() {
        return mTracking;
    }

    public boolean isConntected() {
        return mConnected;
    }
}
