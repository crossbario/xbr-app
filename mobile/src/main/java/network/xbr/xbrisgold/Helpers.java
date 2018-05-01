package network.xbr.xbrisgold;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class Helpers {

    private static CompletableFuture<Boolean> sInternetFuture;
    private static Thread sInternetCheckerThread;
    private static boolean sIsLastRunComplete;

    private static NetworkInfo getNetworkInfo(Context ctx) {
        ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
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
        PowerManager pm = ctx.getSystemService(PowerManager.class);
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
                    return parseInt(prefs, ctx.getString(R.string.key_policy_wifi_foreground));
                } else if (Helpers.isDozeMode(ctx)) {
                    return parseInt(prefs, ctx.getString(R.string.key_policy_wifi_doze));
                } else {
                    return parseInt(prefs, ctx.getString(R.string.key_policy_wifi_background));
                }
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (app.isVisible()) {
                    return parseInt(prefs,
                            ctx.getString(R.string.key_policy_mobile_data_foreground));
                } else if (Helpers.isDozeMode(ctx)) {
                    return parseInt(prefs, ctx.getString(R.string.key_policy_mobile_data_doze));
                } else {
                    return parseInt(prefs,
                            ctx.getString(R.string.key_policy_mobile_data_background));
                }
            }
        }

        // Random default.
        return 66;
    }

    private static int parseInt(SharedPreferences sharedPreferences, String key) {
        return Integer.parseInt(sharedPreferences.getString(key, null));
    }
}
