package network.xbr.xbrisgold;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;
    private static final int SUCCESS_CODE = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String packageName = getPackageName();
        PowerManager pm = getSystemService(PowerManager.class);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            startBackgroundService();
        }

        // FIXME: move to a settings button's onClick.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == SUCCESS_CODE) {
            startBackgroundService();
        } else {
            // TODO: Show dialog that app may get killed in the background by the OS
            // and won't work once device enters doze mode.
            // https://developer.android.com/training/monitoring-device-state/doze-standby.html
            startBackgroundService();
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = getSystemService(ActivityManager.class);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LongRunningService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startBackgroundService() {
        if (!isServiceRunning()) {
            startService(new Intent(getApplicationContext(), LongRunningService.class));
        }
    }
}
