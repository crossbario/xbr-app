package network.xbr.xbrisgold;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    public static final int POLICY_DISCONNECT = 1;
    public static final int POLICY_NO_HEARTBEAT = 2;
    public static final int POLICY_KEEP_ALIVE = 3;
    public static final int POLICY_FREQUENT_HEARTBEAT = 4;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        System.out.println(String.format("Changed key %s", key));
        if (key.equals(getString(R.string.key_policy_wifi_foreground))
                || key.equals(getString(R.string.key_policy_wifi_background))
                || key.equals(getString(R.string.key_policy_wifi_doze))
                || key.equals(getString(R.string.key_policy_mobile_data_foreground))
                || key.equals(getString(R.string.key_policy_mobile_data_background))
                || key.equals(getString(R.string.key_policy_mobile_data_doze))) {

        }
        if (key.equals(getString(R.string.key_policy_wifi_foreground))) {
            System.out.println(parseInt(sharedPreferences, key));
        } else if (key.equals(getString(R.string.key_policy_wifi_background))) {
            System.out.println(parseInt(sharedPreferences, key));
        } else if (key.equals(getString(R.string.key_policy_wifi_doze))) {
            System.out.println(parseInt(sharedPreferences, key));
        } else if (key.equals(getString(R.string.key_policy_mobile_data_foreground))) {
            System.out.println(parseInt(sharedPreferences, key));
        } else if (key.equals(getString(R.string.key_policy_mobile_data_background))) {
            System.out.println(parseInt(sharedPreferences, key));
        } else if (key.equals(getString(R.string.key_policy_mobile_data_doze))) {
            System.out.println(parseInt(sharedPreferences, key));
        }
    }

    private int parseInt(SharedPreferences sharedPreferences, String key) {
        return Integer.parseInt(sharedPreferences.getString(key, null));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
