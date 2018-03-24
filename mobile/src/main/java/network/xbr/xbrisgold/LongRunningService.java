package network.xbr.xbrisgold;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.SessionDetails;

public class LongRunningService extends Service {

    private BroadcastReceiver mNetworkStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            actuallyConnect(context);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(mNetworkStateChangeListener,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        // Automatically restarts the service if killed by the OS.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mNetworkStateChangeListener);
        super.onDestroy();
    }

    private void actuallyConnect(Context ctx) {
        if (Helpers.isNetworkAvailable(ctx.getApplicationContext())) {
            Helpers.isInternetWorking().whenComplete((working, throwable) -> {
                if (working) {
                    connectToServer();
                } else {
                    // Network is available but we don't have a working internet
                    // lets schedule something to recheck if internet is working.
                    new Handler().postDelayed(() -> actuallyConnect(ctx), 10000);
                }
            });
        }
    }

    private void connectToServer() {
        Session wampSession = new Session();
        wampSession.addOnJoinListener(this::onJoin);
        wampSession.addOnLeaveListener((session, closeDetails) -> System.out.println("LEFT"));
        wampSession.addOnDisconnectListener((session, b) -> {
            // Check if we have a working internet before attempting a reconnect.
            System.out.println("DISCONNECTED");
            actuallyConnect(getApplicationContext());
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
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final int[] i = {0};
        executorService.scheduleAtFixedRate(() -> {
            session.publish("io.xbr.gold.tick", i[0]).whenComplete((publication, throwable) -> {
                if (throwable == null) {
                    System.out.println(String.format("Published io.cbr.gold.tick %s", i[0]));
                }
            });
            i[0]++;
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
