package network.xbr.xbrisgold;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkStateChangeListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Helpers.isNetworkAvailable(context)) {
            System.out.println("Aafsdfasdasdas");
        } else {
            System.out.println("sknhdfckljsdkljfsadsdl");
        }
    }
}
