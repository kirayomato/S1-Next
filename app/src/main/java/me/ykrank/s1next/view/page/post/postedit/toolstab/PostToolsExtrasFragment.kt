package me.ykrank.s1next.view.page.post.postedit.toolstab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import me.ykrank.s1next.databinding.FragmentPostToolsExtrasBinding
import me.ykrank.s1next.view.fragment.BaseFragment
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraBold
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraCreditPermission
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraImg
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraItalic
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraLink
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraQuote
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraStrikethrough
import me.ykrank.s1next.view.page.post.internal.PostToolsExtraUnderline

class PostToolsExtrasFragment : BaseFragment() {

    private lateinit var binding: FragmentPostToolsExtrasBinding

    private lateinit var provider: PostToolsExtrasContextProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider = parentFragment as PostToolsExtrasContextProvider
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPostToolsExtrasBinding.inflate(inflater, container, false)

        binding.recycleView.layoutManager = GridLayoutManager(requireContext(), 5, LinearLayoutManager.VERTICAL, false)
        val adapter = PostToolsExtrasAdapter(requireContext()) { provider.currentEditText }
        adapter.swapDataSet(listOf(PostToolsExtraBold(), PostToolsExtraItalic(), PostToolsExtraUnderline(),
                PostToolsExtraImg(), PostToolsExtraLink(), PostToolsExtraStrikethrough(), PostToolsExtraQuote(),
                PostToolsExtraCreditPermission()))
        binding.recycleView.adapter = adapter

        return binding.root
    }

    interface PostToolsExtrasContextProvider {
        val currentEditText: EditText
    }

    companion object {
        val TAG: String = PostToolsExtrasFragment::class.java.simpleName

        fun newInstance(): PostToolsExtrasFragment {
            return PostToolsExtrasFragment()
        }
    }
}
