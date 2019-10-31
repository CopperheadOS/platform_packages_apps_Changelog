package com.michaelflisar.changelog.tags;

import android.content.Context;

public class ChangelogTagImage implements IChangelogTag {

    public static final String TAG = "image";

    @Override
    public String getXMLTagName() {
        return TAG;
    }

    @Override
    public String formatChangelogRow(Context context, String changeText) {
        return changeText;
    }
}
