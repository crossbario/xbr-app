///////////////////////////////////////////////////////////////////////////////
//
//   XBR is Gold - https://xbr.network
//
//   Copyright (c) Crossbar.io Technologies GmbH. All rights reserved
//   Licensed under the GPL 3.0 https://opensource.org/licenses/GPL-3.0
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets.core;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class Helpers {

    private static CompletableFuture<Boolean> sInternetFuture;
    private static Thread sInternetCheckerThread;
    private static boolean sIsLastRunComplete;

    private static NetworkInfo getNetworkInfo(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        return cm.getActiveNetworkInfo();
    }

    public static boolean isNetworkAvailable(Context ctx) {
        NetworkInfo activeNetworkInfo = getNetworkInfo(ctx);
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static CompletableFuture<Boolean> isInternetWorking() {
        if (sInternetFuture == null || sIsLastRunComplete) {
            sInternetFuture = new CompletableFuture<>();
        }

        if (sInternetCheckerThread == null || !sInternetCheckerThread.isAlive()) {
            sInternetCheckerThread = new Thread(() -> {
                try {
                    Socket socket = new Socket();
                    SocketAddress socketAddress = new InetSocketAddress("www.google.com", 80);
                    socket.connect(socketAddress, 5000);
                    socket.close();
                    sInternetFuture.complete(true);
                } catch (IOException ignore) {
                    sInternetFuture.complete(false);
                }
                sIsLastRunComplete = true;
            });
        }

        if (!sInternetCheckerThread.isAlive()) {
            sInternetCheckerThread.start();
        }

        return sInternetFuture;
    }

    public static void callInThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    public static String getCurrentDate() {
        return new Date(System.currentTimeMillis()).toString();
    }

    private static boolean isDozeMode(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null
                && pm.isDeviceIdleMode();
    }

    public static int getProfilePingInterval(Application mainApp) {
        MainApplication app = (MainApplication) mainApp;
        Context ctx = app.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        NetworkInfo networkInfo = Helpers.getNetworkInfo(ctx);

        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (app.isVisible()) {
                    return parseInt(prefs, ctx.getString(R.string.key_policy_wifi_foreground),
                            String.valueOf(SettingsFragment.POLICY_FREQUENT_HEARTBEAT));
                } else if (Helpers.isDozeMode(ctx)) {
                    return parseInt(prefs, ctx.getString(R.string.key_policy_wifi_doze),
                            String.valueOf(SettingsFragment.POLICY_NO_HEARTBEAT));
                } else {
                    return parseInt(prefs, ctx.getString(R.string.key_policy_wifi_background),
                            String.valueOf(SettingsFragment.POLICY_KEEP_ALIVE));
                }
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (app.isVisible()) {
                    return parseInt(prefs,
                            ctx.getString(R.string.key_policy_mobile_data_foreground),
                            String.valueOf(SettingsFragment.POLICY_FREQUENT_HEARTBEAT));
                } else if (Helpers.isDozeMode(ctx)) {
                    return parseInt(prefs, ctx.getString(R.string.key_policy_mobile_data_doze),
                            String.valueOf(SettingsFragment.POLICY_NO_HEARTBEAT));
                } else {
                    return parseInt(prefs,
                            ctx.getString(R.string.key_policy_mobile_data_background),
                            String.valueOf(SettingsFragment.POLICY_KEEP_ALIVE));
                }
            }
        }

        // Random default.
        return 66;
    }

    private static int parseInt(SharedPreferences sharedPreferences, String key, String defValue) {
        return Integer.parseInt(sharedPreferences.getString(key, defValue));
    }

    public static boolean hasLocationPermission(Context ctx) {
        return ActivityCompat.checkSelfPermission(ctx, ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx, ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isServiceRunning(Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LongRunningService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void startBackgroundService(Context ctx) {
        if (!isServiceRunning(ctx)) {
            ctx.startService(new Intent(ctx, LongRunningService.class));
        }
    }

    public static void checkLocationPermissions(Activity activity, int requestCode) {
        if (!Helpers.hasLocationPermission(activity)) {
            ActivityCompat.requestPermissions(activity, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
        }
    }
}
