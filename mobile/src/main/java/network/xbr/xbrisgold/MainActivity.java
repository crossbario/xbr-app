package network.xbr.xbrisgold;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;

public class MainActivity extends AppCompatActivity {

    private Session mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSession = new Session();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getStarted();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSession.leave();
    }

    private void getStarted() {
        mSession.addOnJoinListener((session, details) -> {
            System.out.println(String.format("Joined session %s", details.sessionID));
        });
        mSession.addOnLeaveListener((session, details) -> {
            System.out.println("Left session.");
        });

        IAuthenticator authenticator = new CryptosignAuth(
                "test@crossbario.com",
                "ef83d35678742e01fa412d597cd3909c113b12a8a7dc101cba073c0423c9db41",
                "e5b0d24af05c77d644de885946147aeb4fa6897a5cf4eb14347c3d637664b117");
        Client client = new Client(mSession, "ws://192.168.31.95:8080/ws", "realm1", authenticator);
        client.connect().whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });
    }
}
