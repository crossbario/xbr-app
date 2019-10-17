package io.crossbar.crossbarfxmarkets;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class Util {
    public static boolean hasLocationPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void ensureServiceRunning(Context ctx) {
        if (!isBackgroundServiceRunning(ctx)) {
            ctx.startService(new Intent(ctx, BackgroundService.class));
        }
    }

    public static void ensureLocationPermissionsAndBind(Activity activity, ServiceConnection onBind,
                                                        int requestCode) {
        Context ctx = activity.getApplicationContext();
        if (hasLocationPermission(ctx)) {
            Intent intent = new Intent(ctx, BackgroundService.class);
            activity.bindService(intent, onBind, Context.BIND_AUTO_CREATE);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
        }
    }

    public static boolean isBackgroundServiceRunning(Context ctx) {
        ActivityManager manager = ctx.getSystemService(ActivityManager.class);
        if (manager == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo service:
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BackgroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
