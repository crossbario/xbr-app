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
    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static CompletableFuture<Boolean> isInternetWorking() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress("www.google.com", 80);
                socket.connect(socketAddress, 5000);
                socket.close();
                future.complete(true);
            } catch (IOException ignore) {
                future.complete(false);
            }
        }).start();
        return future;
    }
}
