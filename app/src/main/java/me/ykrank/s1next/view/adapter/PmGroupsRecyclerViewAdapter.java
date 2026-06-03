package me.ykrank.s1next.view.adapter;

import android.app.Activity;

import com.github.ykrank.androidtools.widget.EventBus;

import me.ykrank.s1next.data.User;
import me.ykrank.s1next.view.adapter.delegate.PmGroupsAdapterDelegate;

public final class PmGroupsRecyclerViewAdapter extends BaseRecyclerViewAdapter {

    public PmGroupsRecyclerViewAdapter(Activity activity, EventBus eventBus, User user) {
        super(activity);

        addAdapterDelegate(new PmGroupsAdapterDelegate(activity, eventBus, user));
    }
}
