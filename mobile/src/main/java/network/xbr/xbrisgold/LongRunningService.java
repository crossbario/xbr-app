package network.xbr.xbrisgold;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.ExitInfo;

public class LongRunningService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectToServer().whenComplete((exitInfo, throwable) -> {
            System.out.println(exitInfo);
        });
        return super.onStartCommand(intent, flags, startId);
    }

    private CompletableFuture<ExitInfo> connectToServer() {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, sessionDetails) -> {
            System.out.println("JOINED");
        });
        wampSession.addOnLeaveListener((session, closeDetails) -> {
            System.out.println("LEFT");
        });
        wampSession.addOnDisconnectListener((session, b) -> {
            System.out.println("DISCONNECTED");
        });
        IAuthenticator auth = new CryptosignAuth(
                "test@crossbario.com",
                "74PTVnh0LgH6QS1ZfNOQnBE7Eqin3BAcugc8BCPJ20E",
                "e5b0d24af05c77d644de885946147aeb4fa6897a5cf4eb14347c3d637664b117");
        Client client = new Client(wampSession, "ws://192.168.31.95:8080/ws", "realm1", auth);
        return client.connect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
