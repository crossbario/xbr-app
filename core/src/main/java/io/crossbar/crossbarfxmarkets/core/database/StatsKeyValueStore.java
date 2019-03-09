///////////////////////////////////////////////////////////////////////////////
//
//   XBR is Gold - https://xbr.network
//
//   Copyright (c) Crossbar.io Technologies GmbH. All rights reserved
//   Licensed under the GPL 3.0 https://opensource.org/licenses/GPL-3.0
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets.core.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class StatsKeyValueStore {

    private static final String KEY_CONN_ATTEMPT_COUNT = "connection_attempt_count";
    private static final String KEY_CONN_SUCCESS_COUNT = "connection_success_count";
    private static final String KEY_CONN_FAIL_COUNT = "connection_fail_count";
    private static final String KEY_CONN_RETRY_COUNT = "connection_retry_count";
    private static final String KEY_SERVICE_CRASH_COUNT = "service_crash_count";
    private static final String KEY_APP_NETWORK_USAGE = "network_usage_bytes";

    private static StatsKeyValueStore sInstance;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    private StatsKeyValueStore(Context ctx) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        mEditor = mPreferences.edit();
    }

    public static StatsKeyValueStore getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new StatsKeyValueStore(ctx);
        }
        return sInstance;
    }

    private int getCount(String key) {
        return mPreferences.getInt(key, 0);
    }

    private void appendCount(String key) {
        mEditor.putInt(key, getCount(key) + 1);
        mEditor.apply();
    }

    public int getConnectionAttemptsCount() {
        return getCount(KEY_CONN_ATTEMPT_COUNT);
    }

    public void appendConnectionAttemptsCount() {
        appendCount(KEY_CONN_ATTEMPT_COUNT);
    }

    public int getConnectionSuccessCount() {
        return getCount(KEY_CONN_SUCCESS_COUNT);
    }

    public void appendConnectionSuccessCount() {
        appendCount(KEY_CONN_SUCCESS_COUNT);
    }

    public int getConnectionFailureCount() {
        return getCount(KEY_CONN_FAIL_COUNT);
    }

    public void appendConnectionFailureCount() {
        appendCount(KEY_CONN_FAIL_COUNT);
    }

    public int getConnectionRetriesCount() {
        return getCount(KEY_CONN_RETRY_COUNT);
    }

    public void appendConnectionRetriesCount() {
        appendCount(KEY_CONN_RETRY_COUNT);
    }

    public int getServiceCrashCount() {
        return getCount(KEY_SERVICE_CRASH_COUNT);
    }

    public void appendServiceCrashCount() {
        appendCount(KEY_SERVICE_CRASH_COUNT);
    }

    public long getAppNetworkUsageBytes() {
        return mPreferences.getLong(KEY_APP_NETWORK_USAGE, 0);
    }

    public void appendAppNetworkUsage(long bytes) {
        mEditor.putLong(KEY_APP_NETWORK_USAGE, getAppNetworkUsageBytes() + bytes);
        mEditor.apply();
    }
}
