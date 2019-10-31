package com.michaelflisar.changelog.items;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.michaelflisar.changelog.R;
import com.michaelflisar.changelog.interfaces.IImage;
import com.michaelflisar.changelog.internal.ChangelogRecyclerViewAdapter;

public class ItemImage implements IImage {

    public String mImage;

    public ItemImage(String image) {
        mImage = image;
    }

    @Override
    public ChangelogRecyclerViewAdapter.Type getRecyclerViewType() {
        return ChangelogRecyclerViewAdapter.Type.Image;
    }

    public Drawable getDrawable(Context context) {
        int resId = context.getResources().getIdentifier(mImage, "drawable", context.getPackageName());
        Log.d("TEST", "resId - " + resId);
        return resId > 0 ? ContextCompat.getDrawable(context, resId) : null;
    }

    @Override
    public int getLayoutId() {
        return R.layout.changelog_image;
    }
}
