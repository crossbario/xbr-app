package network.xbr.xbrisgold;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    
    public static final String POLICY_DISCONNECT = "-1";
    public static final String POLICY_NO_HEARTBEAT = "0";
    public static final String POLICY_KEEP_ALIVE = "600";
    public static final String POLICY_FREQUENT_HEARTBEAT = "10";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
