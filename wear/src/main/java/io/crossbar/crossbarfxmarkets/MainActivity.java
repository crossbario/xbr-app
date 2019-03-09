///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import io.crossbar.crossbarfxmarkets.core.Helpers;
import io.crossbar.crossbarfxmarkets.core.MainApplication;
import io.crossbar.crossbarfxmarkets.core.SettingsFragment;


public class MainActivity extends WearableActivity {

    private static final int REQUEST_CODE_LOCATION = 1001;
    private static final int SUCCESS_CODE = -1;

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);
        mTextView.setOnClickListener(v -> {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment(), "settings")
                    .commit();
        });

        // Enables Always-on
        setAmbientEnabled();

        Helpers.startBackgroundService(getApplicationContext());
        Helpers.checkLocationPermissions(this, REQUEST_CODE_LOCATION);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        SettingsFragment fragment = (SettingsFragment) fm.findFragmentByTag("settings");
        if (fragment != null && fragment.isVisible()) {
            fm.beginTransaction().remove(fragment).commit();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (resultCode == SUCCESS_CODE) {
                LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(
                        getApplicationContext());
                Intent intent = new Intent(MainApplication.INTENT_LOCATION_ENABLED);
                intent.putExtra("enabled", true);
                broadcaster.sendBroadcast(intent);
            } else {
                // TODO: Tell the user somehow location is disabled.
            }
        }
    }
}
