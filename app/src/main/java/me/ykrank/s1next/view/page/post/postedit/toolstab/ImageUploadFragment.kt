package me.ykrank.s1next.view.page.post.postedit.toolstab

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidtools.extension.toast
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.EventBus
import com.github.ykrank.androidtools.widget.uploadimg.ImageUploadManager
import com.github.ykrank.androidtools.widget.uploadimg.LibImageUploadFragment
import com.github.ykrank.androidtools.widget.uploadimg.ModelImageUpload
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.model.PostEditor
import me.ykrank.s1next.databinding.ViewImageUploadSourceSwitchBinding
import me.ykrank.s1next.view.event.PostAddImageEvent
import me.ykrank.s1next.widget.net.Image
import me.ykrank.s1next.widget.uploadimg.FORUM_ATTACHMENT_REMOTE_PREFIX
import me.ykrank.s1next.widget.uploadimg.ForumAttachmentUploadManager
import me.ykrank.s1next.widget.uploadimg.ForumAttachmentUploadTarget
import me.ykrank.s1next.widget.uploadimg.ForumAttachmentUploadTargetProvider
import me.ykrank.s1next.widget.uploadimg.RIPImageUploadManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

class ImageUploadFragment : LibImageUploadFragment() {

    @Inject
    internal lateinit var mEventBus: EventBus

    @Inject
    internal lateinit var mUser: User

    @Inject
    internal lateinit var mS1Service: S1Service

    @Inject
    @Image
    internal lateinit var mOkHttpClient: OkHttpClient

    private var useForumAttachment = true
    private var useOriginalResolution = false
    private var uploadOptionsExpanded = false
    private var sourceBinding: ViewImageUploadSourceSwitchBinding? = null
    private val addedForumAttachmentIds = hashSetOf<String>()
    private var loadingForumAttachmentIds: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.appComponent.inject(this)
    }

    override val imageClickListener: ((View, ModelImageUpload) -> Unit)? =
        { _, model ->
            model.url?.also {
                mEventBus.postDefault(
                    PostAddImageEvent(
                        it,
                        model.insertText ?: "[img]$it[/img]",
                        model.forumAttachmentId(),
                    )
                )
            }
        }

    override fun provideImageUploadManager(): ImageUploadManager {
        val targetProvider = parentFragment as? ForumAttachmentUploadTargetProvider
        val target = targetProvider?.forumAttachmentUploadTarget
        if (useForumAttachment && target != null) {
            return ForumAttachmentUploadManager(
                target,
                mUser,
                mS1Service,
                { targetProvider.forumAttachmentUploadInfo },
                { targetProvider.forumAttachmentFormHash },
            )
        }
        return RIPImageUploadManager(_okHttpClient = mOkHttpClient)
    }

    override val maxCompressedImageSize: Size?
        get() = if (useOriginalResolution) null else MAX_COMPRESSED_IMAGE_SIZE

    override fun createUploadOptionsView(inflater: LayoutInflater, container: ViewGroup): View? {
        if ((parentFragment as? ForumAttachmentUploadTargetProvider)?.forumAttachmentUploadTarget == null) {
            return null
        }
        val binding = ViewImageUploadSourceSwitchBinding.inflate(inflater, container, false)
        sourceBinding = binding
        binding.forumAttachmentSwitch.isChecked = useForumAttachment
        binding.forumAttachmentSwitch.setOnCheckedChangeListener { _, checked ->
            useForumAttachment = checked
            imageUploadManager = provideImageUploadManager()
            updateUploadSourceText()
        }
        binding.originalResolutionSwitch.isChecked = useOriginalResolution
        binding.originalResolutionSwitch.setOnCheckedChangeListener { _, checked ->
            useOriginalResolution = checked
            updateUploadSourceText()
        }
        binding.optionsHeader.setOnClickListener {
            setUploadOptionsExpanded(!uploadOptionsExpanded)
        }
        updateUploadSourceText()
        setUploadOptionsExpanded(uploadOptionsExpanded)
        refreshForumAttachments()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        refreshForumAttachments()
    }

    fun refreshForumAttachments() {
        syncForumAttachments()
    }

    override fun delPickedImage(model: ModelImageUpload?) {
        val aid = model?.forumAttachmentId()
        if (aid != null && isForumAttachmentInserted(aid)) {
            context?.toast(R.string.image_upload_forum_attachment_used)
            return
        }
        super.delPickedImage(model)
    }

    private fun updateUploadSourceText() {
        val binding = sourceBinding ?: return
        binding.uploadSourceTitle.setText(
            if (useForumAttachment) {
                R.string.image_upload_source_forum_attachment
            } else {
                R.string.image_upload_source_external
            }
        )
        binding.uploadSourceHint.setText(
            if (useForumAttachment) {
                R.string.image_upload_source_forum_attachment_hint
            } else {
                R.string.image_upload_source_external_hint
            }
        )
        binding.uploadOptionsTitle.text = getString(
            R.string.image_upload_options_summary,
            getString(
                if (useForumAttachment) {
                    R.string.image_upload_source_forum_attachment
                } else {
                    R.string.image_upload_source_external
                }
            ),
            getString(
                if (useOriginalResolution) {
                    R.string.image_upload_original_resolution
                } else {
                    R.string.image_upload_limit_large_resolution
                }
            ),
        )
    }

    private fun setUploadOptionsExpanded(expanded: Boolean) {
        uploadOptionsExpanded = expanded
        val binding = sourceBinding ?: return
        binding.uploadOptionsDetail.visibility = if (expanded) View.VISIBLE else View.GONE
        binding.expandIcon.setImageResource(
            if (expanded) {
                R.drawable.ic_expand_less
            } else {
                R.drawable.ic_expand_more
            }
        )
    }

    private fun syncForumAttachments() {
        val targetProvider = parentFragment as? ForumAttachmentUploadTargetProvider ?: return
        val availableAttachments = targetProvider.forumAttachments
        val availableIds = availableAttachments.map { it.aid }.toSet()
        images.filter { model ->
            val aid = model.existingForumAttachmentId()
            aid != null && aid !in availableIds
        }.forEach { model ->
            model.existingForumAttachmentId()?.let { addedForumAttachmentIds.remove(it) }
            removeUploadedImage(model)
        }
        val models = availableAttachments.toModels()
        addUploadedImages(models)
        loadMissingForumAttachmentPreviews(targetProvider, availableAttachments)
    }

    private fun List<PostEditor.ForumAttachment>.toModels(): List<ModelImageUpload> {
        return mapNotNull { attachment ->
            val imageUrl = attachment.imageUrl ?: return@mapNotNull null
            if (!addedForumAttachmentIds.add(attachment.aid)) {
                return@mapNotNull null
            }
            ModelImageUpload(media = null, remoteId = "$FORUM_ATTACHMENT_REMOTE_PREFIX${attachment.aid}").apply {
                state = ModelImageUpload.STATE_DONE
                url = imageUrl
                deleteUrl = attachment.aid
                insertText = "[attachimg]${attachment.aid}[/attachimg]"
            }
        }
    }

    private fun loadMissingForumAttachmentPreviews(
        targetProvider: ForumAttachmentUploadTargetProvider,
        attachments: List<PostEditor.ForumAttachment>,
    ) {
        val missingIds = attachments.asSequence()
            .filter { it.imageUrl.isNullOrEmpty() }
            .map { it.aid }
            .filter { it !in addedForumAttachmentIds }
            .toSet()
        if (missingIds.isEmpty() || missingIds == loadingForumAttachmentIds) {
            return
        }
        val fid = targetProvider.forumAttachmentUploadInfo?.fid
            ?: targetProvider.forumAttachmentUploadTarget?.fid()
            ?: return
        loadingForumAttachmentIds = missingIds
        lifecycleScope.launch {
            val loadedAttachments = runCatching {
                withContext(LibImageUploadFragment.uploadDispatcher) {
                    PostEditor.fromAttachmentListHtml(
                        mS1Service.getForumAttachmentList(missingIds.toForumAttachmentAids(), fid, "attachlist")
                    )
                }
            }.getOrElse { error ->
                L.report(error)
                emptyList()
            }
            loadingForumAttachmentIds = emptySet()
            addUploadedImages(loadedAttachments.toModels())
        }
    }

    private fun Set<String>.toForumAttachmentAids(): String {
        return joinToString(separator = "") { "|$it" }
    }

    private fun ForumAttachmentUploadTarget.fid(): Int? {
        return when (this) {
            is ForumAttachmentUploadTarget.NewThread -> fid
            is ForumAttachmentUploadTarget.Reply -> fid
            is ForumAttachmentUploadTarget.EditPost -> fid
        }
    }

    private fun ModelImageUpload.existingForumAttachmentId(): String? {
        val id = remoteId?.removePrefix(FORUM_ATTACHMENT_REMOTE_PREFIX) ?: return null
        return id.takeIf { it != remoteId && it.matches(Regex("""\d+""")) }
    }

    private fun ModelImageUpload.forumAttachmentId(): String? {
        existingForumAttachmentId()?.let { return it }
        val aid = deleteUrl?.takeIf { it.matches(Regex("""\d+""")) } ?: return null
        return aid.takeIf {
            insertText?.contains("aid=$aid") == true || url?.contains("aid=$aid") == true
        }
    }

    private fun isForumAttachmentInserted(aid: String): Boolean {
        val content = (parentFragment as? PostToolsExtrasFragment.PostToolsExtrasContextProvider)
            ?.currentEditText
            ?.text
            ?.toString()
            .orEmpty()
        if (Regex("""(?i)\[attachimg]\s*$aid\s*\[/attachimg]""").containsMatchIn(content)) {
            return true
        }
        return Regex("""(?i)(?:[?&]|&amp;)aid=$aid(?:\D|$)""").containsMatchIn(content)
    }

    companion object {
        val TAG: String = ImageUploadFragment::class.java.simpleName
        private val MAX_COMPRESSED_IMAGE_SIZE = Size(2000, 2000)

        fun newInstance(): ImageUploadFragment {
            return ImageUploadFragment()
        }
    }
}
