package com.github.ykrank.androidtools.binding;

import androidx.recyclerview.widget.RecyclerView;

import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter;


public final class LibRecyclerViewBindingAdapter {

    private LibRecyclerViewBindingAdapter() {
    }

    public static void setHasProgress(RecyclerView recyclerView, Boolean oldIsLoadingFirstTime, Boolean newIsLoadingFirstTime) {
        if (newIsLoadingFirstTime != oldIsLoadingFirstTime) {
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            if (adapter instanceof LibBaseRecyclerViewAdapter) {
                LibBaseRecyclerViewAdapter baseRecyclerViewAdapter = (LibBaseRecyclerViewAdapter) adapter;
                baseRecyclerViewAdapter.setHasProgress(newIsLoadingFirstTime);
            }
        }
    }
}
