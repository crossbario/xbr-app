///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 1000;
    private static final int PERMISSION_GRANTED = 0;

    private ActivityTrackerService mActivityTrackerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(getApplicationContext(), ActivityTrackerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
//        Util.ensureLocationPermissionsAndBind(this, mConnection, LOCATION_REQUEST_CODE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE && grantResults[0] == PERMISSION_GRANTED) {
            Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mActivityTrackerService = ((ActivityTrackerService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mActivityTrackerService = null;
        }
    };
}
