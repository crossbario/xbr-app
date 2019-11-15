package io.crossbar.crossbarfxmarkets;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.Task;

public class ActivityTrackerService extends Service {

    public static final String ACTIVITY_ACTION = "io.crossbar.crossbarfxmarkets.activty";
    private static final String TAG = ActivityTrackerService.class.getName();
    private static final int REQUEST_CODE = 1;

    private PendingIntent mPendingIntent;
    private ActivityRecognitionClient mClient;
    private final IBinder mBinder = new LocalBinder();
    private boolean mStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        mClient = new ActivityRecognitionClient(getApplicationContext());
        Intent intent = new Intent(getApplicationContext(), DeviceActivityReceiver.class);
        intent.setAction(ACTIVITY_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startUpdates();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startUpdates();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        stopUpdates();
        Log.d(TAG, "onDestroy: service is going down...");
        super.onDestroy();
    }

    private void startUpdates() {
        if (mStarted) {
            return;
        }
        // TODO: Make this time configurable
        Task<Void> task = mClient.requestActivityUpdates(3000, mPendingIntent);
        task.addOnSuccessListener(command -> mStarted = true);
        task.addOnFailureListener(command -> mStarted = false);
    }

    private void stopUpdates() {
        if (mStarted) {
            Task<Void> task = mClient.removeActivityUpdates(mPendingIntent);
            task.addOnSuccessListener(command -> mStarted = false);
            task.addOnFailureListener(command -> mStarted = false);
        }
    }

    class LocalBinder extends Binder {
        ActivityTrackerService getService() {
            return ActivityTrackerService.this;
        }
    }
}
