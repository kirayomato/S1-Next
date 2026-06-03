package com.github.ykrank.androidtools.ui.internal

import androidx.databinding.Observable
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.ykrank.androidtools.R
import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter
import com.github.ykrank.androidtools.ui.vm.LoadingViewModel

class LoadingViewModelViewBinder(
    private val swipeRefreshLayout: SwipeRefreshLayout,
    private val recyclerView: RecyclerView,
) {
    private var loadingViewModel: LoadingViewModel? = null
    private val loadingCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            bindCurrentState()
        }
    }

    init {
        swipeRefreshLayout.setColorSchemeColors(
            *swipeRefreshLayout.resources.getIntArray(R.array.swipe_refresh_layout_color_scheme)
        )
    }

    fun setLoadingViewModel(loadingViewModel: LoadingViewModel) {
        if (this.loadingViewModel === loadingViewModel) {
            bindCurrentState()
            return
        }
        clear()
        this.loadingViewModel = loadingViewModel
        loadingViewModel.addOnPropertyChangedCallback(loadingCallback)
        bindCurrentState()
    }

    fun clear() {
        loadingViewModel?.removeOnPropertyChangedCallback(loadingCallback)
        loadingViewModel = null
    }

    private fun bindCurrentState() {
        val loadingViewModel = loadingViewModel ?: return
        swipeRefreshLayout.isEnabled = loadingViewModel.isSwipeRefreshLayoutEnabled
        swipeRefreshLayout.isRefreshing = loadingViewModel.isSwipeRefresh
        (recyclerView.adapter as? LibBaseRecyclerViewAdapter)?.setHasProgress(
            loadingViewModel.isLoadingFirstTime
        )
    }
}
