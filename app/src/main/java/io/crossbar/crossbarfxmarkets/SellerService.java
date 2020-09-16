///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets;

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

public class SellerService {

    private SimpleSeller mSeller;

    private final String mURI;
    private final String mRealm;
    private final String mDelegateEthKey;
    private final String mMemberEthKey;
    private final String mCryptoSignKey;

    public SellerService(String uri, String realm, String delegateEthKey, String memberEthKey,
                         String cryptoSignKey) {

        mURI = uri;
        mRealm = realm;
        mDelegateEthKey = delegateEthKey;
        mMemberEthKey = memberEthKey;
        mCryptoSignKey = cryptoSignKey;
    }

    public void sell() {
        Session session = new Session();
        session.addOnJoinListener(this::onJoin);

        ECKeyPair keyPair = ECKeyPair.create(Numeric.hexStringToByteArray(mMemberEthKey));
        String addressHex = Credentials.create(keyPair).getAddress();
        byte[] addressRaw = Numeric.hexStringToByteArray(addressHex);

        String pubkeyHex = CryptosignAuth.getPublicKey(AuthUtil.toBinary(mCryptoSignKey));

        Map<String, Object> extras = new HashMap<>();
        extras.put("wallet_address", addressRaw);
        extras.put("pubkey", pubkeyHex);

        MarketMemberLogin.sign(
                keyPair, addressHex, pubkeyHex
        ).thenCompose(signature -> {
            extras.put("signature", signature);

            CryptosignAuth auth = new CryptosignAuth("public", mCryptoSignKey, extras);
            Client client = new Client(session, mURI, mRealm, auth);

            return client.connect();
        }).whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("Exit...");
            }
        });
    }

    private void onJoin(Session session, SessionDetails details) {
        System.out.println("Joined...");

        byte[] apiID = new byte[16];
        String topic = "xbr.myapp.example";
        AtomicReference<BigInteger> balance = new AtomicReference<>();

        session.call(
                "xbr.marketmaker.get_config", Map.class
        ).thenCompose(config -> {
            String marketMaker = (String) config.get("marketmaker");
            mSeller = new SimpleSeller(marketMaker, mDelegateEthKey);
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
            System.out.println("BALANCE IS " + Util.toInt(balance.get()) + " XBR");
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }
}
