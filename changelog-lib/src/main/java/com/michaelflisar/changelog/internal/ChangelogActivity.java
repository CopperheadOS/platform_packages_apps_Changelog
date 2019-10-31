package com.michaelflisar.changelog.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.michaelflisar.changelog.Changelog;
import com.michaelflisar.changelog.ChangelogBuilder;
import com.michaelflisar.changelog.ChangelogUtil;
import com.michaelflisar.changelog.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by flisar on 06.03.2018.
 */

public class ChangelogActivity extends AppCompatActivity {

    private static final int MENU_ITEM_RATE = 101;

    public static Intent createIntent(Context context, ChangelogBuilder changelogBuilder, Integer theme, boolean themeHasActionBar) {
        Intent intent = new Intent(context, ChangelogActivity.class);
        intent.putExtra("builder", changelogBuilder);
        intent.putExtra("theme", theme == null ? -1 : theme);
        intent.putExtra("themeHasActionBar", themeHasActionBar);
        return intent;
    }

    private ChangelogBuilder mBuilder;
    private ChangelogParserAsyncTask mAsyncTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int theme = getIntent().getIntExtra("theme", -1);
        if (theme > 0) {
            setTheme(theme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelog_activity);

        mBuilder = getIntent().getParcelableExtra("builder");

        Toolbar toolbar = findViewById(R.id.toolbar);
        boolean themeHasActionBar = getIntent().getBooleanExtra("themeHasActionBar", false);
        if (!themeHasActionBar) {
            setSupportActionBar(toolbar);
        } else {
            toolbar.setVisibility(View.GONE);
        }

        String title = mBuilder.getCustomTitle();
        if (title == null) {
            title = getString(R.string.changelog_dialog_title, ChangelogUtil.getAppVersionName(this));
        }

        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ProgressBar pb = findViewById(R.id.pbLoading);
        RecyclerView rv = findViewById(R.id.rvChangelog);
        ChangelogRecyclerViewAdapter adapter = mBuilder.setupEmptyRecyclerView(rv);

        mAsyncTask = new ChangelogParserAsyncTask(this, pb, adapter, mBuilder);
        mAsyncTask.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mBuilder.isUseRateButton()) {
            String rateLabel = mBuilder.getCustomRateLabel();
            if (TextUtils.isEmpty(rateLabel)) {
                rateLabel = getString(R.string.changelog_dialog_rate);
            }
            menu.add(0, MENU_ITEM_RATE, 0, rateLabel).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case MENU_ITEM_RATE:
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Changelog.RATE_CLICK_ACTION));
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
        super.onDestroy();
    }
}
