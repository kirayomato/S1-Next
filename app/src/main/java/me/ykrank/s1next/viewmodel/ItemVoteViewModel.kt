package me.ykrank.s1next.viewmodel

import androidx.databinding.ObservableBoolean
import me.ykrank.s1next.data.api.model.Vote

class ItemVoteViewModel(
    private val voteVM: VoteViewModel,
    val option: Vote.VoteOption
) {
    val selected = ObservableBoolean()

    val isSingleVotable: Boolean
        get() = voteVM.isVoteable && !voteVM.isMultiple

    val isMultiVotable: Boolean
        get() = voteVM.isVoteable && voteVM.isMultiple
}
