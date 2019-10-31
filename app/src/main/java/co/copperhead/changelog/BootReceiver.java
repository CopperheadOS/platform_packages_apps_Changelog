package co.copperhead.changelog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Context appContext = context.getApplicationContext();
        if (appContext == null) return;

        Intent main = new Intent(appContext, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        appContext.startActivity(main);
    }
}
