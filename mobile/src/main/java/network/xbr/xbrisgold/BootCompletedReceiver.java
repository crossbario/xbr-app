package network.xbr.xbrisgold;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (Helpers.isNetworkAvailable(ctx.getApplicationContext())) {
                ctx.startService(new Intent(ctx.getApplicationContext(), LongRunningService.class));
            } else {
                Toast.makeText(ctx.getApplicationContext(), "Network unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
