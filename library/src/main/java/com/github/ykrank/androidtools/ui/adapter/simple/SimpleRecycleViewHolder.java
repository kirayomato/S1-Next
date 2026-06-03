package com.github.ykrank.androidtools.ui.adapter.simple;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public class SimpleRecycleViewHolder<T extends ViewBinding> extends RecyclerView.ViewHolder {
    final T binding;

    public SimpleRecycleViewHolder(T binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public T getBinding() {
        return binding;
    }
}
