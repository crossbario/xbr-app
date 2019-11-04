///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.crossbarfxmarkets.database.AppDatabase;
import io.crossbar.crossbarfxmarkets.database.LocationBatch;
import io.crossbar.crossbarfxmarkets.database.XLocation;


public class BackgroundService extends Service implements NetworkTracker.StateListener {

    private static final String TAG = BackgroundService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private FusedLocationProviderClient mLocationProvider;
    private NetworkTracker mNetworkTracker;

    private long mBatchStartTime;
    private long mBatchEndTime;
    private LocationBatch mCurrentBatch;
    private long mCurrentBatchID;
    private boolean mLocationRequested;

    private AppDatabase mDB;
    private Executor mExecutor;

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            mExecutor.execute(() -> {
                System.out.println(String.format("Got %s locations", locationResult.getLocations().size()));
                mDB.getLocationDao().insert(filterLocation(locationResult.getLocations()));
                if (mBatchEndTime <= mBatchStartTime) {
                    connectAndSend(mDB.getLocationDao().getLocationsByBatchID(mCurrentBatchID));
                    initBatch();
                }
            });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: Service started");
        mExecutor = Executors.newSingleThreadExecutor();
        mLocationProvider = new FusedLocationProviderClient(getApplicationContext());
        mDB = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,
                "crossbarfxmarkets").build();
        mNetworkTracker = new NetworkTracker(getApplicationContext());
        mNetworkTracker.addStateListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startLocationUpdates();
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: Starting...");
        startLocationUpdates();
        return START_STICKY;
    }

    private LocationRequest getLocationRequest(long batchIntervalSeconds, int granularitySeconds) {
        LocationRequest request = LocationRequest.create();
        request.setMaxWaitTime(batchIntervalSeconds);
        request.setInterval(granularitySeconds * 1000);
        request.setFastestInterval(granularitySeconds * 1000);
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        request.setSmallestDisplacement(1);
        return request;
    }

    private void initBatch() {
        mBatchStartTime = System.currentTimeMillis();
        mBatchEndTime = System.currentTimeMillis() + 30 * 1000;
        mCurrentBatch = new LocationBatch();
        mExecutor.execute(() -> mCurrentBatchID = mDB.getLocationBatchDao().insert(mCurrentBatch));
    }

    private void startLocationUpdates() {
        if (!mNetworkTracker.isRunning()) {
            mNetworkTracker.start();
        }
        if (mLocationRequested) {
            return;
        }
        if (hasLocationPermission()) {
            Log.i(TAG, "startLocationUpdates: We have the permissions....");
            initBatch();
            mLocationProvider.requestLocationUpdates(
                    getLocationRequest(mBatchEndTime - mBatchStartTime, 10),
                    mLocationCallback, Looper.getMainLooper());
            mLocationRequested = true;
        }
    }

    private void stopLocationUpdates() {
        if (mNetworkTracker.isRunning()) {
            mNetworkTracker.removeStateListener(this);
            mNetworkTracker.stop();
        }
        if (mLocationRequested) {
            Log.i(TAG, "stopLocationUpdates: Stopping location updates");
            mLocationProvider.removeLocationUpdates(mLocationCallback);
            mLocationRequested = false;
        }
    }

    @Override
    public void onConnect() {
        startLocationUpdates();
    }

    @Override
    public void onDisconnect() {
        stopLocationUpdates();
    }

    class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startForegroundNew(String channelID){
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(channelID, channelName,
                NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = getSystemService(NotificationManager.class);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        Notification notification = builder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    public void startForeground() {
        String NOTIFICATION_CHANNEL_ID = "io.crossbar.crossbarfxmarkets";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundNew(NOTIFICATION_CHANNEL_ID);
        else
            startForeground(1, showNotification(NOTIFICATION_CHANNEL_ID));
    }

    private boolean hasLocationPermission() {
        return true;
    }

    private Notification showNotification(String channelID) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_message_body))
                .setTicker(getString(R.string.app_name))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        return builder.build();
    }

    private void connectAndSend(List<XLocation> locations) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            Log.i(TAG, "connectAndSend: Joined...");
            session.publish("io.crossbar.location", locations).thenAccept(callResult -> {
                Log.i(TAG, "connectAndSend: Posted locations");
                session.leave();
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        });

        Client client = new Client(wampSession, "ws://68.183.216.125:8080/ws", "basepos");
        client.connect().whenComplete((exitInfo, throwable) -> {
            
        });
    }

    private XLocation[] filterLocation(List<Location> locations) {
        XLocation[] result = new XLocation[locations.size()];
        for (int i = 0; i < locations.size(); i++) {
            Location location = locations.get(i);
            XLocation xLocation = new XLocation(location.getLatitude(), location.getLongitude(),
                    location.getTime());
            xLocation.batchID = (int) mCurrentBatchID;
            result[i] = xLocation;
        }
        return result;
    }
}
