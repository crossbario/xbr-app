package network.xbr.xbrisgold;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import network.xbr.xbrisgold.core.Helpers;
import network.xbr.xbrisgold.core.SettingsFragment;

public class MainActivity extends WearableActivity {

    public static final String INTENT_LOCATION_ENABLED = "network.xbr.location_enabled";

    private static final int REQUEST_CODE_NO_BATTERY_OPTIMIZATIONS = 1000;
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
        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (!Helpers.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment(), "settings")
                    .commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                Intent intent = new Intent(INTENT_LOCATION_ENABLED);
                intent.putExtra("enabled", true);
                broadcaster.sendBroadcast(intent);
            } else {
                // TODO: Tell the user somehow location is disabled.
            }
        }
    }
}
