package io.crossbar.crossbarfxmarkets;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.crossbar.autobahn.utils.AuthUtil;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleSeller;
import xbr.network.Util;
import xbr.network.eip712.MarketMemberLogin;

public class XbrSellerService extends Service {

    private static String TAG = XbrSellerService.class.getName();

    private SimpleSeller mSeller;
    private Session mSession;

    private String mDelegateKey;

    public void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String NOTIFICATION_CHANNEL_ID = "io.crossbar.crossbarfxmarkets";

        Notification notification =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_seller_running))
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setTicker(getString(R.string.app_name))
                        .build();

        final int NOTIFICATION_ID = 1001;
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String uri = intent.getExtras().getString("uri");
        String realm = intent.getExtras().getString("realm");
        mDelegateKey = intent.getExtras().getString("delegate_eth_key");
        String memberKey = intent.getExtras().getString("member_eth_key");
        String cryptoSignKey = intent.getExtras().getString("cryptosign_key");

        connect(uri, realm, memberKey, cryptoSignKey);

        startForeground();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSession.leave();
        super.onDestroy();
    }

    public void connect(String uri, String realm, String memberEthKey, String cryptoSignKey) {
        mSession = new Session();
        mSession.addOnJoinListener(this::startSelling);

        ECKeyPair keyPair = ECKeyPair.create(Numeric.hexStringToByteArray(memberEthKey));
        String addressHex = Credentials.create(keyPair).getAddress();
        byte[] addressRaw = Numeric.hexStringToByteArray(addressHex);

        String pubkeyHex = CryptosignAuth.getPublicKey(AuthUtil.toBinary(cryptoSignKey));

        Map<String, Object> extras = new HashMap<>();
        extras.put("wallet_address", addressRaw);
        extras.put("pubkey", pubkeyHex);

        MarketMemberLogin.sign(
                keyPair, addressHex, pubkeyHex
        ).thenCompose(signature -> {
            extras.put("signature", signature);

            CryptosignAuth auth = new CryptosignAuth("public", cryptoSignKey, extras);
            Client client = new Client(mSession, uri, realm, auth);

            return client.connect();
        }).whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                Log.i(TAG, "connect: connection closed, existed.");
            }
        });
    }

    private void startSelling(Session session, SessionDetails details) {
        System.out.println("Joined...");

        byte[] apiID = new byte[16];
        String topic = "xbr.myapp.example";
        AtomicReference<BigInteger> balance = new AtomicReference<>();

        session.call(
                "xbr.marketmaker.get_config", Map.class
        ).thenCompose(config -> {

            String marketMaker = (String) config.get("marketmaker");
            mSeller = new SimpleSeller(marketMaker, mDelegateKey);
            BigInteger price = Util.toXBR(1);
            int intervalSeconds = 10;
            mSeller.add(apiID, topic, price, intervalSeconds);
            return mSeller.start(session);

        }).thenCompose(bigInteger -> {

            balance.set(bigInteger);
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "crossbario");
            payload.put("country", "DE");
            payload.put("level", "Crossbar is super cool!");
            return mSeller.wrap(apiID, topic, payload);

        }).thenCompose(enc -> {

            return session.publish(topic, new PublishOptions(true, true), enc.get("id"),
                    enc.get("serializer"), enc.get("ciphertext"));

        }).thenAccept(publication -> {
            Log.i(TAG, "startSelling: Balance IS " + Util.toInt(balance.get()) + " XBR");
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
