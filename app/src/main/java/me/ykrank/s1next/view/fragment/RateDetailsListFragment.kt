package me.ykrank.s1next.view.fragment

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.extension.dp2px
import com.github.ykrank.androidtools.ui.vm.LoadingViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import me.ykrank.s1next.data.api.model.Rate
import me.ykrank.s1next.view.adapter.RateDetailAdapter

/**
 * Created by ykrank on 2017/1/16.
 */

@AndroidEntryPoint
class RateDetailsListFragment : BaseRecyclerViewFragment<List<Rate>>() {

    private lateinit var rates: List<Rate>
    private lateinit var mRecyclerAdapter: RateDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val argRates = arguments?.getParcelableArrayList<Rate>(ARG_RATES)
        if (argRates != null) {
            rates = argRates
        } else {
            rates = listOf()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        mRecyclerAdapter = RateDetailAdapter(requireContext(), RateDetailAdapter.Mode.MULTI)
        recyclerView.adapter = mRecyclerAdapter
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            val d16 = 16.dp2px(requireContext())

            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.set(d16, 0, d16, 0)
            }
        })
    }

    override fun getSourceObservable(@LoadingViewModel.LoadingDef loading: Int): Single<List<Rate>> {
        return Single.just(rates)
    }

    override fun onNext(data: List<Rate>) {
        super.onNext(data)
        mRecyclerAdapter.diffNewDataSet(data, true)
    }

    companion object {
        val TAG = RateDetailsListFragment::class.java.simpleName
        private const val ARG_RATES = "rates"

        fun instance(rates: ArrayList<Rate>): RateDetailsListFragment {
            val fragment = RateDetailsListFragment()
            val bundle = Bundle()
            bundle.putParcelableArrayList(ARG_RATES, rates)
            fragment.arguments = bundle
            return fragment
        }
    }
}
