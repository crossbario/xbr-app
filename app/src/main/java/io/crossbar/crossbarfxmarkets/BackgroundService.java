package io.crossbar.crossbarfxmarkets;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.ArrayList;
import java.util.List;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;


public class BackgroundService extends Service {

    private static final String TAG = BackgroundService.class.getName();
    private FusedLocationProviderClient mLocationProvider;

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            connectAndSend(locationResult.getLocations());
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
        }
    };

    private LocationRequest getLocationRequest(int batchInterval, int granularity) {
        LocationRequest request = new LocationRequest();
        request.setMaxWaitTime(batchInterval * 1000);
        request.setInterval(granularity * 1000);
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return request;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: Starting...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, showNotification(intent));

        mLocationProvider = new FusedLocationProviderClient(getApplicationContext());
        if (hasLocationPermission()) {
            Log.i(TAG, "onStartCommand: We have the permissions....");
            mLocationProvider.requestLocationUpdates(getLocationRequest(2 * 60, 10),
                    mLocationCallback, Looper.getMainLooper());
        }
        return START_STICKY;
    }

    @RequiresApi(26)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,
                NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
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
        if (mLocationProvider != null) {
            mLocationProvider.removeLocationUpdates(mLocationCallback);

        }
        System.out.println("I am killed....");
    }

    private boolean hasLocationPermission() {
        return true;
    }

    private Notification showNotification(Intent intent) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_message_body))
                .setTicker(getString(R.string.app_name))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        return notificationBuilder.build();
    }

    private void connectAndSend(List<Location> locations) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            Log.i(TAG, "connectAndSend: Joined...");
            session.publish("io.crossbar.location", filterLocation(locations)).thenAccept(callResult -> {
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

    private List<RealLocation> filterLocation(List<Location> locations) {
        List<RealLocation> result = new ArrayList<>();
        for (Location location: locations) {
            result.add(new RealLocation(location.getLatitude(), location.getLongitude(),
                    location.getTime()));
        }
        return result;
    }

    private static class RealLocation {
        public double latitude;
        public double longitude;
        public long time;

        public RealLocation(double latitude, double longitude, long time) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
        }
    }
}
