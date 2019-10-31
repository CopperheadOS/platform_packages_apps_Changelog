package co.copperhead.changelog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.michaelflisar.changelog.Changelog;
import com.michaelflisar.changelog.ChangelogBuilder;
import com.michaelflisar.changelog.interfaces.IChangelogRateHandler;

import java.lang.reflect.Method;

public class MainActivity extends Activity implements IChangelogRateHandler {

    static final String EXTRA_FORCE_SHOW = "co.copperhead.changelog.extra.FORCE_SHOW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("TEST", "force - " + getIntent().getBooleanExtra(EXTRA_FORCE_SHOW, false));


        if (showShowChangelog(getIntent())) {

            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Changelog.RATE_CLICK_ACTION.equals(intent.getAction())) {
                        onRateButtonClicked();
                    }
                }
            }, new IntentFilter(Changelog.RATE_CLICK_ACTION));

            ChangelogBuilder builder = new ChangelogBuilder()
                    .withUseBulletList(true)
                    .withMinVersionToShow(-1)
                    .withManagedShowOnStart(false)
                    .withTitle("Changelog")
                    .withXmlFile(R.raw.changelog)
                    .withRateButton(true)
                    .withRateButtonLabel("Opt out");
            builder.buildAndStartActivity(MainActivity.this, true);
        }
        finish();
    }

    private boolean showShowChangelog(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_FORCE_SHOW, true)) {
            return true;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("opt_out", false)) {
            return false;
        }

        String current = getProp("ro.copperhead.version", "1.0");
        String previous = PreferenceManager.getDefaultSharedPreferences(this).getString("previous_version", "0.0");

        if (Double.parseDouble(current) > Double.parseDouble(previous)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("previous_version", current).apply();
            return true;
        }
        return false;
    }

    @Override
    public boolean onRateButtonClicked() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("opt_out", true).apply();
        return true;
    }

    private String getProp(String prop, String def) {
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method m = clazz.getDeclaredMethod("get", String.class);
            String s = (String) m.invoke(null, prop);
            return TextUtils.isEmpty(s) ? def : s;
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }
}
