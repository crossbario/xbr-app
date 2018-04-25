package network.xbr.xbrisgold;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    
    public static final int POLICY_DISCONNECT = 1;
    public static final int POLICY_NO_HEARTBEAT = 2;
    public static final int POLICY_KEEP_ALIVE = 3;
    public static final int POLICY_FREQUENT_HEARTBEAT = 4;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
