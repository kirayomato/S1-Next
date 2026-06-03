package me.ykrank.s1next.view.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.ykrank.androidtools.extension.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.app.AppService
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.data.api.runApiCatching
import me.ykrank.s1next.data.api.toastError
import me.ykrank.s1next.databinding.LayoutVoteBinding
import me.ykrank.s1next.viewmodel.ItemVoteViewModel
import me.ykrank.s1next.viewmodel.VoteViewModel
import javax.inject.Inject


/**
 * A dialog lets the user vote thread.
 */
@AndroidEntryPoint
class VoteDialogFragment : BaseDialogFragment(), VoteViewModel.VoteVmAction {
    @Inject
    internal lateinit var appService: AppService

    @Inject
    internal lateinit var s1Service: S1Service

    @Inject
    internal lateinit var mUser: User

    private lateinit var tid: String
    private lateinit var mVote: Vote
    private lateinit var binding: LayoutVoteBinding
    private lateinit var adapter: VoteOptionsAdapter

    private lateinit var data: List<ItemVoteViewModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tid = requireArguments().getString(ARG_THREAD_ID)!!
        mVote = requireArguments().getParcelable(ARG_VOTE)!!

        adapter = VoteOptionsAdapter(::refreshSelectedItem)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LayoutVoteBinding.inflate(inflater, container, false)

        val model = VoteViewModel(mVote, this)
        binding.model = model

        binding.recycleView.adapter = adapter
        binding.recycleView.layoutManager = LinearLayoutManager(requireContext())
        mVote.voteOptions?.let {
            data = it.values.map { ItemVoteViewModel(binding.model!!, it) }
            adapter.swapDataSet(data)
        }

        loadData()

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun loadData() {
        lifecycleScope.launch {
            flow {
                emit(runApiCatching {
                    appService.getPollInfo(
                        mUser.appSecureToken,
                        tid
                    )
                })
            }.zip(
                flow {
                    emit(runApiCatching {
                        appService.getPollOptions(
                            mUser.appSecureToken,
                            tid
                        )
                    })
                }
            ) { appVote, voteOptions -> appVote to voteOptions }
                .collect { (appVoteResult, voteOptionsResult) ->
                    appVoteResult.toastError(activity) {
                        val appVote = this.data
                        voteOptionsResult.toastError(activity) {
                            binding.model?.appVote?.set(appVote)
                            data?.let {
                                this@VoteDialogFragment.data.forEachIndexed { index, vm ->
                                    vm.option.mergeWithAppVoteOption(
                                        it[index], appVote?.voters
                                            ?: mVote.voteCount
                                    )
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
        }
    }

    private fun refreshSelectedItem(position: Int) {
        if (mVote.isMultiple) {
            val vm = data[position]
            if (vm.selected.get()) {
                vm.selected.set(false)
            } else {
                val selected = data.filter { it.selected.get() }
                if (selected.size < mVote.maxChoices) {
                    vm.selected.set(true)
                }
            }
        } else {
            data.forEachIndexed { index, vm -> vm.selected.set(index == position) }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onClickVote(view: View?) {
        val selected = data.filter { it.selected.get() }.map { it.option.optionId }
        lifecycleScope.launch {
            val voteRaw = runApiCatching {
                s1Service.vote(tid, mUser.authenticityToken, selected)
            }
            voteRaw.toastError(activity) {
                activity?.toast(R.string.vote_success)
                loadData()
            }
        }
    }

    companion object {

        const val ARG_VOTE = "vote"
        const val ARG_THREAD_ID = "thread_id"
        val TAG: String = VoteDialogFragment::class.java.simpleName

        fun newInstance(threadId: String, vote: Vote): VoteDialogFragment {
            val fragment = VoteDialogFragment()
            val bundle = Bundle()
            bundle.putString(ARG_THREAD_ID, threadId)
            bundle.putParcelable(ARG_VOTE, vote)
            fragment.arguments = bundle

            return fragment
        }
    }
}
