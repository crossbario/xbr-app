package network.xbr.xbrisgold;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class Helpers {

    private static CompletableFuture<Boolean> sInternetFuture;
    private static boolean sIsLastRunComplete;

    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static CompletableFuture<Boolean> isInternetWorking() {
        if (sInternetFuture == null || sIsLastRunComplete) {
            sInternetFuture= new CompletableFuture<>();
        }

        new Thread(() -> {
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
        }).start();
        return sInternetFuture;
    }
}
