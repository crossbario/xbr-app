package network.xbr.xbrisgold;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class MainApplication extends Application {

    public static final String INTENT_APP_VISIBILITY_CHANGED = "network.xbr.app_visibility_changed";

    private LocalBroadcastManager mBroadcaster;

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcaster = LocalBroadcastManager.getInstance(getApplicationContext());
        registerActivityLifecycleCallbacks(new AppLifecycleHandler());
    }

    private class AppLifecycleHandler implements Application.ActivityLifecycleCallbacks {

        private int mActivityStartedCount;

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            mActivityStartedCount++;
            if (mActivityStartedCount == 1) {
                Intent intent = new Intent(INTENT_APP_VISIBILITY_CHANGED);
                intent.putExtra("app_visible", true);
                mBroadcaster.sendBroadcast(intent);
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            mActivityStartedCount--;
            if (mActivityStartedCount == 0) {
                Intent intent = new Intent(INTENT_APP_VISIBILITY_CHANGED);
                intent.putExtra("app_visible", false);
                mBroadcaster.sendBroadcast(intent);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }
}
