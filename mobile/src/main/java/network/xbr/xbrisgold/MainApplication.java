package network.xbr.xbrisgold;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class MainApplication extends Application {

    private AppLifecycleHandler mAppLifecycleHandler;
    private LocalBroadcastManager mBroadcaster;

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcaster = LocalBroadcastManager.getInstance(getApplicationContext());
        mAppLifecycleHandler = new AppLifecycleHandler();
        registerActivityLifecycleCallbacks(mAppLifecycleHandler);
    }

    public boolean isForeground() {
        return mAppLifecycleHandler.isAppForeground();
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
                mBroadcaster.sendBroadcast(new Intent());
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
                mBroadcaster.sendBroadcast(new Intent());
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }

        public boolean isAppForeground() {
            return mActivityStartedCount > 0;
        }
    }
}
