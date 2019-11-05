package co.copperhead.changelog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.michaelflisar.changelog.ChangelogBuilder;
import com.michaelflisar.changelog.classes.ChangelogRenderer;
import com.michaelflisar.changelog.interfaces.IAutoVersionNameFormatter;
import com.michaelflisar.changelog.internal.ChangelogParserAsyncTask;
import com.michaelflisar.changelog.internal.ChangelogRecyclerViewAdapter;
import com.michaelflisar.changelog.items.ItemImage;
import com.michaelflisar.changelog.items.ItemRelease;
import com.michaelflisar.changelog.items.ItemRow;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

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

        String[] array = getResources().getStringArray(R.array.changelog_files);
        ArrayList<String> items = new ArrayList<>(Arrays.asList(array));

        ImageView fabForward = findViewById(R.id.fab_forward);
        ImageView fabBack = findViewById(R.id.fab_back);
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
                    fabForward.setImageResource(R.drawable.ic_done_24px);
                } else {
                    fabForward.setImageResource(R.drawable.ic_forward_24px);
                }
                fabBack.setVisibility(vp.getCurrentItem() == 0 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        findViewById(R.id.fab_forward).setOnClickListener(v -> {
            if (vp.getCurrentItem() < (pagerAdapter.getCount() - 1)) {
                vp.setCurrentItem(vp.getCurrentItem() + 1, true);
            } else {
                finish();
            }
        });
        findViewById(R.id.fab_back).setOnClickListener(v -> {
            if (vp.getCurrentItem() > 0) {
                vp.setCurrentItem( vp.getCurrentItem() - 1, true);
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
        if (Settings.Global.getInt(getContentResolver(), "messaging_opt_out", 0) == 1) {
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

        private ArrayList<String> mItems = new ArrayList<>();

        SectionsPagerAdapter(FragmentManager fm, ArrayList<String> items) {
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

        static ChangelogFragment newInstance(String xml) {
            ChangelogFragment fragment = new ChangelogFragment();
            Bundle b = new Bundle();
            b.putString(ARG_XML, xml);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedState) {
            super.onCreate(savedState);

            String xml = getArguments().getString(ARG_XML, "changelog");

            int xmlResId = getResources().getIdentifier(xml, "raw", getContext().getPackageName());

            mBuilder = new ChangelogBuilder()
                    .withUseBulletList(true)
                    .withMinVersionToShow(-1)
                    .withManagedShowOnStart(false)
                    .withTitle("Changelog")
                    .withXmlFile(xmlResId)
                    .withUseBulletList(xml.contains("changelog"))
                    .withRenderer(new Renderer(getContext()))
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

        private class Renderer extends ChangelogRenderer {

            private Animator currentAnimator;
            private int shortAnimationDuration;

            Renderer(Context context) {
                super();

                shortAnimationDuration = context.getResources().getInteger(
                        android.R.integer.config_shortAnimTime);
            }

            @Override
            public void bindHeader(ChangelogRecyclerViewAdapter adapter, Context context, ViewHolderHeader viewHolder, ItemRelease release, ChangelogBuilder builder) {
                super.bindHeader(adapter, context, viewHolder, release, builder);
                if (release != null) {
                    if (TextUtils.isEmpty(release.getDate())) {
                        viewHolder.getTvVersion().setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        viewHolder.getTvDate().setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void bindRow(ChangelogRecyclerViewAdapter adapter, Context context, ViewHolderRow viewHolder, ItemRow row, ChangelogBuilder builder) {
                super.bindRow(adapter, context, viewHolder, row, builder);
                if (row != null) {
                    if (!builder.isUseBulletList()) {
                        viewHolder.getTvText().setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    }
                }
            }

            @Override
            public void bindImage(ChangelogRecyclerViewAdapter adapter, Context context, ViewHolderImage viewHolder, ItemImage image, ChangelogBuilder builder) {
                super.bindImage(adapter, context, viewHolder, image, builder);
                viewHolder.imageView.setOnClickListener(v -> {
                    if (getView() == null) return;
                    ImageView expanded = getView().findViewById(R.id.expanded_image);
                    expanded.bringToFront();
                    zoomImageFromThumb(viewHolder.imageView, viewHolder.imageView.getDrawable());
                });
            }

            private void zoomImageFromThumb(final View thumbView, Drawable d) {
                // If there's an animation in progress, cancel it
                // immediately and proceed with this one.
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // Load the high-resolution "zoomed-in" image.
                final ImageView expandedImageView = (ImageView) getView().findViewById(
                        R.id.expanded_image);
                expandedImageView.setImageDrawable(d);

                // Calculate the starting and ending bounds for the zoomed-in image.
                // This step involves lots of math. Yay, math.
                final Rect startBounds = new Rect();
                final Rect finalBounds = new Rect();
                final Point globalOffset = new Point();

                // The start bounds are the global visible rectangle of the thumbnail,
                // and the final bounds are the global visible rectangle of the container
                // view. Also set the container view's offset as the origin for the
                // bounds, since that's the origin for the positioning animation
                // properties (X, Y).
                thumbView.getGlobalVisibleRect(startBounds);
                getView().findViewById(R.id.container)
                        .getGlobalVisibleRect(finalBounds, globalOffset);
                startBounds.offset(-globalOffset.x, -globalOffset.y);
                finalBounds.offset(-globalOffset.x, -globalOffset.y);

                // Adjust the start bounds to be the same aspect ratio as the final
                // bounds using the "center crop" technique. This prevents undesirable
                // stretching during the animation. Also calculate the start scaling
                // factor (the end scaling factor is always 1.0).
                float startScale;
                if ((float) finalBounds.width() / finalBounds.height()
                        > (float) startBounds.width() / startBounds.height()) {
                    // Extend start bounds horizontally
                    startScale = (float) startBounds.height() / finalBounds.height();
                    float startWidth = startScale * finalBounds.width();
                    float deltaWidth = (startWidth - startBounds.width()) / 2;
                    startBounds.left -= deltaWidth;
                    startBounds.right += deltaWidth;
                } else {
                    // Extend start bounds vertically
                    startScale = (float) startBounds.width() / finalBounds.width();
                    float startHeight = startScale * finalBounds.height();
                    float deltaHeight = (startHeight - startBounds.height()) / 2;
                    startBounds.top -= deltaHeight;
                    startBounds.bottom += deltaHeight;
                }

                // Hide the thumbnail and show the zoomed-in view. When the animation
                // begins, it will position the zoomed-in view in the place of the
                // thumbnail.
                thumbView.setAlpha(0f);
                expandedImageView.setVisibility(View.VISIBLE);

                // Set the pivot point for SCALE_X and SCALE_Y transformations
                // to the top-left corner of the zoomed-in view (the default
                // is the center of the view).
                expandedImageView.setPivotX(0f);
                expandedImageView.setPivotY(0f);

                // Construct and run the parallel animation of the four translation and
                // scale properties (X, Y, SCALE_X, and SCALE_Y).
                AnimatorSet set = new AnimatorSet();
                set
                        .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                                startBounds.left, finalBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                                startBounds.top, finalBounds.top))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                                startScale, 1f))
                        .with(ObjectAnimator.ofFloat(expandedImageView,
                                View.SCALE_Y, startScale, 1f));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;

                // Upon clicking the zoomed-in image, it should zoom back down
                // to the original bounds and show the thumbnail instead of
                // the expanded image.
                final float startScaleFinal = startScale;
                expandedImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (currentAnimator != null) {
                            currentAnimator.cancel();
                        }

                        // Animate the four positioning/sizing properties in parallel,
                        // back to their original values.
                        AnimatorSet set = new AnimatorSet();
                        set.play(ObjectAnimator
                                .ofFloat(expandedImageView, View.X, startBounds.left))
                                .with(ObjectAnimator
                                        .ofFloat(expandedImageView,
                                                View.Y,startBounds.top))
                                .with(ObjectAnimator
                                        .ofFloat(expandedImageView,
                                                View.SCALE_X, startScaleFinal))
                                .with(ObjectAnimator
                                        .ofFloat(expandedImageView,
                                                View.SCALE_Y, startScaleFinal));
                        set.setDuration(shortAnimationDuration);
                        set.setInterpolator(new DecelerateInterpolator());
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                thumbView.setAlpha(1f);
                                expandedImageView.setVisibility(View.GONE);
                                currentAnimator = null;
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                thumbView.setAlpha(1f);
                                expandedImageView.setVisibility(View.GONE);
                                currentAnimator = null;
                            }
                        });
                        set.start();
                        currentAnimator = set;
                    }
                });
            }
        }

    }

}
