package me.ykrank.s1next.view.adapter;

import android.app.Activity;

import me.ykrank.s1next.data.pref.ThemeManager;
import me.ykrank.s1next.view.adapter.delegate.ForumAdapterDelegate;

public final class ForumRecyclerViewAdapter extends BaseRecyclerViewAdapter {

    public ForumRecyclerViewAdapter(Activity activity, ThemeManager themeManager) {
        super(activity);

        addAdapterDelegate(new ForumAdapterDelegate(activity, themeManager));
    }
}
