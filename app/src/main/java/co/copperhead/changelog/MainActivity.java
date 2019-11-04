package co.copperhead.changelog;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.michaelflisar.changelog.ChangelogBuilder;
import com.michaelflisar.changelog.interfaces.IAutoVersionNameFormatter;
import com.michaelflisar.changelog.internal.ChangelogParserAsyncTask;
import com.michaelflisar.changelog.internal.ChangelogRecyclerViewAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static final String EXTRA_FORCE_SHOW = "co.copperhead.changelog.extra.FORCE_SHOW";

    private static final int MENU_ITEM_OPT_OUT = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!shouldShowChangelog(getIntent())) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        ArrayList<Integer> items = new ArrayList<>();
        String[] array = getResources().getStringArray(R.array.changelog_files);
        for (String value : array) {
            int id = getResources().getIdentifier(value, "raw", getPackageName());
            items.add(id);
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        ViewPager vp = findViewById(R.id.view_pager);
        SectionsPagerAdapter pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), items);
        vp.setAdapter(pagerAdapter);

        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (vp.getCurrentItem() == (pagerAdapter.getCount() - 1)) {
                    fab.setImageResource(R.drawable.ic_done_24px);
                } else {
                    fab.setImageResource(R.drawable.ic_forward_24px);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        findViewById(R.id.fab).setOnClickListener(v -> {
            if (vp.getCurrentItem() < (pagerAdapter.getCount() - 1)) {
                vp.setCurrentItem(vp.getCurrentItem() + 1, true);
            } else {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_OPT_OUT, 0, R.string.opt_out).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ITEM_OPT_OUT) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("opt_out", true).apply();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean shouldShowChangelog(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_FORCE_SHOW, false)) {
            return true;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("opt_out", false)) {
            return false;
        }

        String current = getProp("ro.copperhead.version", "1.0");
        String previous = PreferenceManager.getDefaultSharedPreferences(this).getString("previous_version", "0.0");

        if (Integer.parseInt(current.replace(".", "")) > Integer.parseInt(previous.replace(".", ""))) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("previous_version", current).apply();
            return true;
        }
        return false;
    }

    @SuppressWarnings("SameParameterValue")
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

    class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Integer> mItems = new ArrayList<>();

        SectionsPagerAdapter(FragmentManager fm, ArrayList<Integer> items) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mItems.addAll(items);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return ChangelogFragment.newInstance(mItems.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }
    }

    public static class ChangelogFragment extends Fragment {

        private static final String ARG_XML = "xml";

        private ChangelogBuilder mBuilder;
        private ChangelogParserAsyncTask mAsyncTask;

        static ChangelogFragment newInstance(int xml) {
            ChangelogFragment fragment = new ChangelogFragment();
            Bundle b = new Bundle();
            b.putInt(ARG_XML, xml);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedState) {
            super.onCreate(savedState);

            mBuilder = new ChangelogBuilder()
                    .withUseBulletList(true)
                    .withMinVersionToShow(-1)
                    .withManagedShowOnStart(false)
                    .withTitle("Changelog")
                    .withXmlFile(getArguments().getInt(ARG_XML, R.raw.changelog))
                    .withVersionNameFormatter(new IAutoVersionNameFormatter() {
                        @Override
                        public String deriveVersionName(int versionCode) {
                            return "";
                        }

                        @Override
                        public int describeContents() {
                            return 0;
                        }

                        @Override
                        public void writeToParcel(Parcel dest, int flags) {

                        }
                    });
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedState) {
            View v = inflater.inflate(R.layout.changelog_fragment, container, false);

            RecyclerView rv = v.findViewById(R.id.rvChangelog);
            ProgressBar pb = v.findViewById(R.id.pbLoading);
            ChangelogRecyclerViewAdapter adapter = mBuilder.setupEmptyRecyclerView(rv);

            mAsyncTask = new ChangelogParserAsyncTask(getActivity(), pb, adapter, mBuilder);
            mAsyncTask.execute();

            return v;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mAsyncTask != null) {
                mAsyncTask.cancel(true);
            }
        }

    }

}
