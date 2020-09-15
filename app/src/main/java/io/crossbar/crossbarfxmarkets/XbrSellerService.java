package io.crossbar.crossbarfxmarkets;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class XbrSellerService extends Service {

    private SellerService mSeller;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String uri = intent.getExtras().getString("uri");
        String realm = intent.getExtras().getString("realm");
        String delegateKey = intent.getExtras().getString("delegate_eth_key");
        String memberKey = intent.getExtras().getString("member_eth_key");
        String cryptoSignKey = intent.getExtras().getString("cryptosign_key");

        mSeller = new SellerService(uri, realm, delegateKey, memberKey, cryptoSignKey);
        mSeller.sell();
        startForeground();
        return START_STICKY;
    }

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
